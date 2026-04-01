package com.zz143.core.event

import com.zz143.core.model.ZZ143Event

class EventFilter(
    private val excludeTypes: Set<Class<out ZZ143Event>> = emptySet(),
    private val excludeScreens: Set<String> = emptySet()
) {
    fun shouldInclude(event: ZZ143Event): Boolean {
        if (event::class.java in excludeTypes) return false
        if (event.screenId.value in excludeScreens) return false
        return true
    }

    companion object {
        fun default(): EventFilter = EventFilter()
    }
}
