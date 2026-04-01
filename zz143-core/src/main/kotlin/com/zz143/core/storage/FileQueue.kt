package com.zz143.core.storage

import com.zz143.core.id.UlidGenerator
import com.zz143.core.threading.ZZ143Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.zip.CRC32

class FileQueue internal constructor(
    private val directory: File,
    private val maxSegmentSize: Long = 512 * 1024L,
    private val maxTotalSize: Long = 10 * 1024 * 1024L,
    private val maxSegmentCount: Int = 50,
    private val dispatchers: ZZ143Dispatchers
) {
    private val segments = ConcurrentLinkedDeque<Segment>()
    private val writeLock = Mutex()
    private var activeSegment: Segment? = null

    suspend fun initialize() = withContext(dispatchers.io) {
        directory.mkdirs()
        val deadDir = File(directory, "dead")
        deadDir.mkdirs()

        val existingFiles = directory.listFiles()
            ?.filter { it.name.startsWith("zz_") && it.name.endsWith(".seg") }
            ?.sortedBy { it.name }
            ?: emptyList()

        for (file in existingFiles) {
            val segment = Segment.recover(file)
            if (segment != null) {
                segments.addLast(segment)
            } else {
                file.renameTo(File(deadDir, file.name))
            }
        }

        activeSegment = if (segments.isNotEmpty() && segments.peekLast().canWrite) {
            segments.peekLast()
        } else {
            rollNewSegment()
        }
    }

    suspend fun enqueue(data: ByteArray) = writeLock.withLock {
        withContext(dispatchers.io) {
            var segment = activeSegment!!
            if (!segment.canWrite || segment.size + data.size + 8 > maxSegmentSize) {
                segment.finalize()
                segment = rollNewSegment()
            }
            segment.append(data)
            enforceQuota()
        }
    }

    suspend fun peek(maxCount: Int): List<ByteArray> = withContext(dispatchers.io) {
        val result = mutableListOf<ByteArray>()
        for (segment in segments) {
            if (result.size >= maxCount) break
            result.addAll(segment.readAll().take(maxCount - result.size))
        }
        result
    }

    suspend fun acknowledge(count: Int) = withContext(dispatchers.io) {
        var remaining = count
        while (remaining > 0 && segments.isNotEmpty()) {
            val oldest = segments.peekFirst()
            val segmentSize = oldest.entryCount
            if (segmentSize <= remaining) {
                segments.pollFirst()
                oldest.delete()
                remaining -= segmentSize
            } else {
                remaining = 0
            }
        }
    }

    val totalEntries: Int get() = segments.sumOf { it.entryCount }

    private fun enforceQuota() {
        while (segments.size > maxSegmentCount || segments.sumOf { it.size } > maxTotalSize) {
            val oldest = segments.pollFirst() ?: break
            oldest.delete()
        }
    }

    private fun rollNewSegment(): Segment {
        val name = "zz_${UlidGenerator.next()}.seg"
        val segment = Segment.create(File(directory, name))
        segments.addLast(segment)
        activeSegment = segment
        return segment
    }

    internal class Segment private constructor(
        private val file: File,
        var canWrite: Boolean,
        var entryCount: Int,
        var size: Long
    ) {
        companion object {
            private const val MAGIC_HEADER = 0x5A5A3134 // "ZZ14"
            private const val MAGIC_FOOTER = 0x5A5A4546 // "ZZEF"
            private const val VERSION = 1

            fun create(file: File): Segment {
                val out = DataOutputStream(BufferedOutputStream(FileOutputStream(file)))
                out.writeInt(MAGIC_HEADER)
                out.writeInt(VERSION)
                out.writeInt(0) // entry count placeholder
                out.flush()
                out.close()
                return Segment(file, canWrite = true, entryCount = 0, size = 12L)
            }

            fun recover(file: File): Segment? {
                if (file.length() < 12) return null
                return try {
                    val input = DataInputStream(BufferedInputStream(FileInputStream(file)))
                    val magic = input.readInt()
                    if (magic != MAGIC_HEADER) { input.close(); return null }
                    val version = input.readInt()
                    if (version != VERSION) { input.close(); return null }
                    val count = input.readInt()
                    input.close()
                    // Check if footer exists (finalized segment)
                    val raf = RandomAccessFile(file, "r")
                    raf.seek(file.length() - 4)
                    val footer = raf.readInt()
                    raf.close()
                    val finalized = footer == MAGIC_FOOTER
                    Segment(file, canWrite = !finalized, entryCount = count, size = file.length())
                } catch (e: Exception) {
                    null
                }
            }
        }

        fun append(data: ByteArray) {
            val raf = RandomAccessFile(file, "rw")
            raf.seek(file.length())
            raf.writeInt(data.size)
            raf.write(data)
            val crc = CRC32()
            crc.update(data)
            raf.writeInt(crc.value.toInt())
            entryCount++
            // Update entry count in header
            raf.seek(8)
            raf.writeInt(entryCount)
            raf.close()
            size = file.length()
        }

        fun readAll(): List<ByteArray> {
            val entries = mutableListOf<ByteArray>()
            try {
                val input = DataInputStream(BufferedInputStream(FileInputStream(file)))
                input.skipBytes(12) // header
                repeat(entryCount) {
                    val length = input.readInt()
                    val data = ByteArray(length)
                    input.readFully(data)
                    val storedCrc = input.readInt()
                    val crc = CRC32()
                    crc.update(data)
                    if (crc.value.toInt() == storedCrc) {
                        entries.add(data)
                    }
                }
                input.close()
            } catch (_: Exception) {}
            return entries
        }

        fun finalize() {
            val raf = RandomAccessFile(file, "rw")
            raf.seek(file.length())
            raf.writeInt(MAGIC_FOOTER)
            raf.close()
            canWrite = false
            size = file.length()
        }

        fun delete() {
            file.delete()
        }
    }
}
