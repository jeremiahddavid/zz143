package com.zz143.core.model

@JvmInline
value class ScreenId(val value: String) {
    companion object {
        fun fromActivity(activityName: String): ScreenId =
            ScreenId("activity:${activityName.substringAfterLast('.')}")

        fun fromFragment(activityName: String, fragmentTag: String): ScreenId =
            ScreenId("fragment:${activityName.substringAfterLast('.')}/$fragmentTag")

        fun fromRoute(route: String): ScreenId =
            ScreenId("route:$route")
    }
}

@JvmInline
value class SessionId(val value: String)
