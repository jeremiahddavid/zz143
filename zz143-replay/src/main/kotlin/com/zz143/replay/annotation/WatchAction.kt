package com.zz143.replay.annotation

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class WatchAction(
    val type: String,
    val description: String = "",
    val screen: String = "",
    val idempotent: Boolean = false
)

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class WatchParam(
    val name: String,
    val description: String = "",
    val sensitive: Boolean = false
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class WatchGuard(
    val expression: String
)
