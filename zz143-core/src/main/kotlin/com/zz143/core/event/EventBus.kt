package com.zz143.core.event

import com.zz143.core.model.ZZ143Event
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

internal class EventBus(
    private val filter: EventFilter = EventFilter.default()
) {
    private val _events = MutableSharedFlow<ZZ143Event>(
        replay = 0,
        extraBufferCapacity = 512,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<ZZ143Event> = _events.asSharedFlow()

    suspend fun emit(event: ZZ143Event) {
        if (filter.shouldInclude(event)) {
            _events.emit(event)
        }
    }

    fun tryEmit(event: ZZ143Event): Boolean {
        if (filter.shouldInclude(event)) {
            return _events.tryEmit(event)
        }
        return false
    }
}
