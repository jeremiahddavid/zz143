package com.zz143.core.storage

import android.content.ContentValues
import com.zz143.core.ZZ143Config
import com.zz143.core.event.EventEncoder
import com.zz143.core.threading.ZZ143Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.DataInputStream

/**
 * Periodically drains events from the FileQueue into SQLite.
 * Reads batched binary payloads, decodes the envelope (not individual events),
 * and stores them in the events table for pattern analysis.
 */
internal class FileQueueDrainer(
    private val fileQueue: FileQueue,
    private val database: ZZ143Database,
    private val config: ZZ143Config,
    private val scope: CoroutineScope,
    private val dispatchers: ZZ143Dispatchers
) {
    private var drainJob: Job? = null
    private val drainIntervalMs = 10_000L // Every 10 seconds

    fun start() {
        drainJob = scope.launch(dispatchers.io) {
            while (isActive) {
                drain()
                delay(drainIntervalMs)
            }
        }
    }

    fun stop() {
        drainJob?.cancel()
        drainJob = null
    }

    /**
     * Drain one pass: peek batches from FileQueue, insert into SQLite, acknowledge.
     */
    internal suspend fun drain() {
        val maxBatchesPerDrain = 20
        val rawBatches = fileQueue.peek(maxBatchesPerDrain)
        if (rawBatches.isEmpty()) return

        val db = database.writableDatabase
        var acknowledged = 0

        for (rawBatch in rawBatches) {
            try {
                val events = decodeBatchEnvelope(rawBatch)
                db.beginTransaction()
                try {
                    for (event in events) {
                        val values = ContentValues().apply {
                            put("event_id", event.eventId)
                            put("session_id", event.sessionId)
                            put("event_type", event.eventType)
                            put("screen_id", event.screenId)
                            put("timestamp_ms", event.timestampMs)
                            put("uptime_ms", event.uptimeMs)
                            put("encoded_payload", event.payload)
                        }
                        db.insertWithOnConflict("events", null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE)

                        // Also insert semantic actions for pattern analysis
                        if (event.actionType != null) {
                            val actionValues = ContentValues().apply {
                                put("action_id", "${event.eventId}_a")
                                put("event_id", event.eventId)
                                put("session_id", event.sessionId)
                                put("action_type", event.actionType)
                                put("screen_id", event.screenId)
                                put("timestamp_ms", event.timestampMs)
                                put("parameters_json", event.parametersJson)
                                put("action_source", event.actionSource)
                            }
                            db.insertWithOnConflict("semantic_actions", null, actionValues, android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE)
                        }
                    }
                    db.setTransactionSuccessful()
                    acknowledged++
                } finally {
                    db.endTransaction()
                }
            } catch (e: Exception) {
                // Skip corrupt batches, acknowledge to avoid infinite retry
                acknowledged++
            }
        }

        if (acknowledged > 0) {
            fileQueue.acknowledge(acknowledged)
        }
    }

    /**
     * Decode the batch envelope written by EventBatchCollector.
     * Format: [4-byte count][4-byte size][payload]...[4-byte size][payload]
     * We extract minimal metadata from each event's TLV payload for indexing.
     */
    private fun decodeBatchEnvelope(rawBatch: ByteArray): List<EventRecord> {
        val records = mutableListOf<EventRecord>()
        try {
            val input = DataInputStream(ByteArrayInputStream(rawBatch))
            val count = input.readInt()

            repeat(count) {
                val size = input.readInt()
                val payload = ByteArray(size)
                input.readFully(payload)
                val record = extractMetadata(payload)
                if (record != null) {
                    records.add(record)
                }
            }
        } catch (_: Exception) {
            // Partial decode is OK — return what we got
        }
        return records
    }

    /**
     * Extract minimal indexable fields from a TLV-encoded event without full decode.
     * Reads tags sequentially until we have what we need.
     */
    private fun extractMetadata(payload: ByteArray): EventRecord? {
        if (payload.size < 4) return null

        val input = DataInputStream(ByteArrayInputStream(payload))
        var eventType = -1
        var eventId = ""
        var sessionId = ""
        var timestampMs = 0L
        var uptimeMs = 0L
        var screenId = ""
        var actionType: String? = null
        var parametersJson: String? = null
        var actionSource = 0

        try {
            while (input.available() > 0) {
                val tag = input.readByte()
                when (tag) {
                    EventEncoder.TAG_EVENT_TYPE -> eventType = input.readByte().toInt()
                    EventEncoder.TAG_EVENT_ID -> eventId = readTaggedString(input)
                    EventEncoder.TAG_SESSION_ID -> sessionId = readTaggedString(input)
                    EventEncoder.TAG_TIMESTAMP -> { timestampMs = input.readLong() }
                    EventEncoder.TAG_SCREEN_ID -> screenId = readTaggedString(input)
                    EventEncoder.TAG_ACTION_TYPE -> {
                        actionType = readTaggedString(input)
                        actionSource = input.readByte().toInt()
                    }
                    EventEncoder.TAG_PARAM -> {
                        val param = readTaggedString(input)
                        parametersJson = if (parametersJson == null) param else "$parametersJson,$param"
                    }
                    EventEncoder.TAG_END -> break
                    else -> {
                        // Skip unknown tags — try to read and discard
                        skipTag(input, tag)
                    }
                }
            }
        } catch (_: Exception) {
            // Best-effort extraction
        }

        if (eventId.isEmpty()) return null

        return EventRecord(
            eventId = eventId,
            sessionId = sessionId,
            eventType = eventType,
            screenId = screenId,
            timestampMs = timestampMs,
            uptimeMs = uptimeMs,
            payload = payload,
            actionType = actionType,
            parametersJson = parametersJson,
            actionSource = actionSource
        )
    }

    private fun readTaggedString(input: DataInputStream): String {
        val length = readVarInt(input)
        val bytes = ByteArray(length)
        input.readFully(bytes)
        return String(bytes, Charsets.UTF_8)
    }

    private fun readVarInt(input: DataInputStream): Int {
        var result = 0
        var shift = 0
        var byte: Int
        do {
            byte = input.readByte().toInt() and 0xFF
            result = result or ((byte and 0x7F) shl shift)
            shift += 7
        } while (byte and 0x80 != 0)
        return result
    }

    private fun skipTag(input: DataInputStream, tag: Byte) {
        when (tag) {
            EventEncoder.TAG_GESTURE_TYPE -> input.readByte()
            EventEncoder.TAG_NAV_TYPE -> input.readByte()
            EventEncoder.TAG_LIFECYCLE -> input.readByte()
            EventEncoder.TAG_COORDINATES -> { input.readShort(); input.readShort() }
            EventEncoder.TAG_DURATION -> input.readLong()
            EventEncoder.TAG_ELEMENT_ID, EventEncoder.TAG_TEXT_HASH,
            EventEncoder.TAG_PROPERTY -> readTaggedString(input)
            else -> {} // Unknown tag — skip nothing and hope for the best
        }
    }

    internal data class EventRecord(
        val eventId: String,
        val sessionId: String,
        val eventType: Int,
        val screenId: String,
        val timestampMs: Long,
        val uptimeMs: Long,
        val payload: ByteArray,
        val actionType: String?,
        val parametersJson: String?,
        val actionSource: Int
    )
}
