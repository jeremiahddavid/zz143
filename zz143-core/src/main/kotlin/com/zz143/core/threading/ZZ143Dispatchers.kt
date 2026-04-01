package com.zz143.core.threading

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

class ZZ143Dispatchers(
    val main: CoroutineDispatcher = Dispatchers.Main.immediate,
    val io: CoroutineDispatcher = Executors.newSingleThreadExecutor { r ->
        Thread(r, "zz143-io").apply { isDaemon = true }
    }.asCoroutineDispatcher(),
    val computation: CoroutineDispatcher = Executors.newFixedThreadPool(2) { r ->
        Thread(r, "zz143-compute").apply { isDaemon = true }
    }.asCoroutineDispatcher()
)
