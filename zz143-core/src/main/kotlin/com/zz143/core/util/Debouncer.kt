package com.zz143.core.util

import kotlinx.coroutines.*

internal class Debouncer(
    private val scope: CoroutineScope,
    private val delayMs: Long
) {
    private var job: Job? = null

    fun debounce(action: suspend () -> Unit) {
        job?.cancel()
        job = scope.launch {
            delay(delayMs)
            action()
        }
    }

    fun cancel() {
        job?.cancel()
    }
}
