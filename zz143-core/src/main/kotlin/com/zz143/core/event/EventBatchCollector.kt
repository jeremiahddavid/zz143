package com.zz143.core.event

import com.zz143.core.ZZ143Config
import com.zz143.core.storage.FileQueue
import com.zz143.core.threading.ZZ143Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import com.zz143.core.model.ZZ143Event

internal class EventBatchCollector(
    private val eventBus: EventBus,
    private val encoder: EventEncoder,
    private val fileQueue: FileQueue,
    private val config: ZZ143Config,
    private val scope: CoroutineScope,
    private val dispatchers: ZZ143Dispatchers
) {
    private var collectJob: Job? = null
    private val channel = Channel<ZZ143Event>(capacity = Channel.BUFFERED)

    fun start() {
        collectJob = scope.launch(dispatchers.computation) {
            // Forward SharedFlow events into a channel so we can consume with
            // timeout-based batching (channels support suspending receive).
            val forwardJob = eventBus.events
                .onEach { channel.send(it) }
                .launchIn(this)

            try {
                while (true) {
                    val batch = mutableListOf<ZZ143Event>()

                    // Wait for at least one event before starting the batch window.
                    val first = channel.receive()
                    batch.add(first)

                    // Collect up to batchSize or until batchWindowMs elapses,
                    // whichever comes first.
                    val deadline = config.batchWindowMs
                    val remaining = config.batchSize - 1

                    if (remaining > 0) {
                        withTimeoutOrNull(deadline) {
                            repeat(remaining) {
                                val event = channel.receive()
                                batch.add(event)
                            }
                        }
                    }

                    // Encode the batch and enqueue the combined payload.
                    val encoded = encodeBatch(batch)
                    fileQueue.enqueue(encoded)
                }
            } finally {
                forwardJob.cancel()
            }
        }
    }

    fun stop() {
        collectJob?.cancel()
        collectJob = null
    }

    private fun encodeBatch(events: List<ZZ143Event>): ByteArray {
        val buffers = events.map { encoder.encode(it) }
        val totalSize = buffers.sumOf { it.size } + (buffers.size * 4) + 4
        val output = java.io.ByteArrayOutputStream(totalSize)
        val dataOut = java.io.DataOutputStream(output)

        // Write the number of events in this batch as a 4-byte header.
        dataOut.writeInt(buffers.size)

        for (buffer in buffers) {
            dataOut.writeInt(buffer.size)
            dataOut.write(buffer)
        }

        dataOut.flush()
        return output.toByteArray()
    }
}
