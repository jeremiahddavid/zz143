package com.zz143.core.testutil

import com.zz143.core.threading.ZZ143Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher

fun testDispatchers(): ZZ143Dispatchers {
    val dispatcher = UnconfinedTestDispatcher()
    return ZZ143Dispatchers(
        main = dispatcher,
        io = dispatcher,
        computation = dispatcher
    )
}
