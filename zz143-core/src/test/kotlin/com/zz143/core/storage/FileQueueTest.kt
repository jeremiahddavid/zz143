package com.zz143.core.storage

import com.google.common.truth.Truth.assertThat
import com.zz143.core.testutil.testDispatchers
import com.zz143.core.threading.ZZ143Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

class FileQueueTest {

    @get:Rule
    val tempDir = TemporaryFolder()

    private lateinit var dispatchers: ZZ143Dispatchers
    private lateinit var queueDir: File

    @Before
    fun setUp() {
        dispatchers = testDispatchers()
        queueDir = tempDir.newFolder("queue")
    }

    // --- Write / Read roundtrip ---

    @Test
    fun enqueueAndPeekReturnsSameData() = runTest {
        val queue = createQueue()
        queue.initialize()

        val data = "hello world".toByteArray()
        queue.enqueue(data)

        val peeked = queue.peek(10)
        assertThat(peeked).hasSize(1)
        assertThat(peeked[0]).isEqualTo(data)
    }

    @Test
    fun enqueueMultipleItemsAndPeekReturnsAll() = runTest {
        val queue = createQueue()
        queue.initialize()

        val items = listOf("alpha", "beta", "gamma").map { it.toByteArray() }
        for (item in items) queue.enqueue(item)

        val peeked = queue.peek(10)
        assertThat(peeked).hasSize(3)
        assertThat(String(peeked[0])).isEqualTo("alpha")
        assertThat(String(peeked[1])).isEqualTo("beta")
        assertThat(String(peeked[2])).isEqualTo("gamma")
    }

    @Test
    fun peekWithMaxCountLimitsResults() = runTest {
        val queue = createQueue()
        queue.initialize()

        for (i in 1..5) queue.enqueue("item-$i".toByteArray())

        val peeked = queue.peek(2)
        assertThat(peeked).hasSize(2)
    }

    @Test
    fun totalEntriesReflectsEnqueuedCount() = runTest {
        val queue = createQueue()
        queue.initialize()

        queue.enqueue("one".toByteArray())
        queue.enqueue("two".toByteArray())
        queue.enqueue("three".toByteArray())

        assertThat(queue.totalEntries).isEqualTo(3)
    }

    // --- Acknowledge ---

    @Test
    fun acknowledgeRemovesEntries() = runTest {
        val queue = createQueue()
        queue.initialize()

        queue.enqueue("a".toByteArray())
        queue.enqueue("b".toByteArray())
        queue.enqueue("c".toByteArray())

        // Force segment finalization by rolling a new segment
        // then acknowledge from the oldest segment
        queue.acknowledge(3)

        val peeked = queue.peek(10)
        assertThat(peeked).isEmpty()
    }

    // --- Segment rollover ---

    @Test
    fun largeDataCausesSegmentRollover() = runTest {
        // Small max segment size to trigger rollover
        val queue = createQueue(maxSegmentSize = 100L)
        queue.initialize()

        // Each entry is 50 bytes of payload + 8 bytes overhead = 58 bytes
        val payload = ByteArray(50) { 0x42 }
        queue.enqueue(payload) // first goes into segment 1
        queue.enqueue(payload) // should trigger rollover to segment 2

        val segFiles = queueDir.listFiles()?.filter {
            it.name.startsWith("zz_") && it.name.endsWith(".seg")
        } ?: emptyList()

        assertThat(segFiles.size).isAtLeast(2)
    }

    @Test
    fun dataAcrossMultipleSegmentsIsReadableViaPeek() = runTest {
        val queue = createQueue(maxSegmentSize = 100L)
        queue.initialize()

        val payload = ByteArray(50) { it.toByte() }
        queue.enqueue(payload)
        queue.enqueue(payload)
        queue.enqueue(payload)

        val peeked = queue.peek(10)
        assertThat(peeked.size).isAtLeast(2)
    }

    // --- Quota enforcement ---

    @Test
    fun exceedingMaxTotalSizeDropsOldestSegments() = runTest {
        // Tiny quota: only ~200 bytes total allowed
        val queue = createQueue(maxSegmentSize = 80L, maxTotalSize = 200L)
        queue.initialize()

        val payload = ByteArray(30) { 0x41 }
        // Each enqueue adds ~42 bytes (30 data + 4 length + 4 crc + header).
        // With maxSegmentSize=80 and header=12, each segment holds ~1-2 entries before rolling.
        // Writing many entries should trigger quota enforcement.
        for (i in 1..10) queue.enqueue(payload)

        val totalBytes = queueDir.listFiles()
            ?.filter { it.name.startsWith("zz_") && it.name.endsWith(".seg") }
            ?.sumOf { it.length() } ?: 0L

        assertThat(totalBytes).isAtMost(200L + 100L) // allow some slack for active segment
    }

    @Test
    fun exceedingMaxSegmentCountDropsOldestSegments() = runTest {
        val queue = createQueue(maxSegmentSize = 50L, maxSegmentCount = 3)
        queue.initialize()

        val payload = ByteArray(20) { 0x41 }
        for (i in 1..20) queue.enqueue(payload)

        val segFiles = queueDir.listFiles()?.filter {
            it.name.startsWith("zz_") && it.name.endsWith(".seg")
        } ?: emptyList()

        assertThat(segFiles.size).isAtMost(3)
    }

    // --- Crash recovery ---

    @Test
    fun initializeRecoversExistingSegments() = runTest {
        // Write data with one queue instance
        val queue1 = createQueue()
        queue1.initialize()
        queue1.enqueue("persistent".toByteArray())

        // Create a new queue on the same directory (simulates restart)
        val queue2 = createQueue()
        queue2.initialize()

        val peeked = queue2.peek(10)
        assertThat(peeked).hasSize(1)
        assertThat(String(peeked[0])).isEqualTo("persistent")
    }

    @Test
    fun initializeIgnoresCorruptedSegments() = runTest {
        // Create a corrupt file in the queue directory
        val corruptFile = File(queueDir, "zz_00000CORRUPT00000000000000.seg")
        corruptFile.writeBytes(byteArrayOf(0x00, 0x01, 0x02)) // Too short, no valid header

        val queue = createQueue()
        queue.initialize()

        // Should not crash; corrupt file moved to dead dir
        queue.enqueue("after-corrupt".toByteArray())
        val peeked = queue.peek(10)
        assertThat(peeked).hasSize(1)
        assertThat(String(peeked[0])).isEqualTo("after-corrupt")
    }

    @Test
    fun initializeMovesCorruptFilesToDeadDirectory() = runTest {
        val corruptFile = File(queueDir, "zz_00000CORRUPT00000000000000.seg")
        corruptFile.writeBytes(byteArrayOf(0x00, 0x01, 0x02))

        val queue = createQueue()
        queue.initialize()

        val deadDir = File(queueDir, "dead")
        val deadFiles = deadDir.listFiles() ?: emptyArray()
        assertThat(deadFiles.map { it.name }).contains("zz_00000CORRUPT00000000000000.seg")
    }

    @Test
    fun initializeCreatesFreshSegmentWhenDirectoryIsEmpty() = runTest {
        val queue = createQueue()
        queue.initialize()

        val segFiles = queueDir.listFiles()?.filter {
            it.name.startsWith("zz_") && it.name.endsWith(".seg")
        } ?: emptyList()

        assertThat(segFiles).isNotEmpty()
    }

    @Test
    fun recoveredFinalizedSegmentIsNotWritable() = runTest {
        // Create a finalized segment manually
        val queue1 = createQueue()
        queue1.initialize()
        queue1.enqueue("before-finalize".toByteArray())

        // Find the segment file and manually write the footer to simulate finalization
        val segFiles = queueDir.listFiles()?.filter {
            it.name.startsWith("zz_") && it.name.endsWith(".seg")
        } ?: emptyList()

        assertThat(segFiles).isNotEmpty()
        val segFile = segFiles.first()
        val raf = RandomAccessFile(segFile, "rw")
        raf.seek(segFile.length())
        raf.writeInt(0x5A5A4546) // MAGIC_FOOTER
        raf.close()

        // Recover: should see the finalized segment plus a new writable one
        val queue2 = createQueue()
        queue2.initialize()

        val peeked = queue2.peek(10)
        assertThat(peeked).hasSize(1)
        assertThat(String(peeked[0])).isEqualTo("before-finalize")

        // Should be able to write new data (into a new segment)
        queue2.enqueue("after-recover".toByteArray())
        val allPeeked = queue2.peek(10)
        assertThat(allPeeked).hasSize(2)
    }

    // --- Empty peek ---

    @Test
    fun peekOnEmptyQueueReturnsEmptyList() = runTest {
        val queue = createQueue()
        queue.initialize()

        val peeked = queue.peek(10)
        assertThat(peeked).isEmpty()
    }

    // --- Helpers ---

    private fun createQueue(
        maxSegmentSize: Long = 512 * 1024L,
        maxTotalSize: Long = 10 * 1024 * 1024L,
        maxSegmentCount: Int = 50
    ): FileQueue = FileQueue(
        directory = queueDir,
        maxSegmentSize = maxSegmentSize,
        maxTotalSize = maxTotalSize,
        maxSegmentCount = maxSegmentCount,
        dispatchers = dispatchers
    )
}
