package com.zz143.core.model

@JvmInline
value class ElementId(val value: String) {
    companion object {
        fun fromResource(screenId: ScreenId, resourceName: String): ElementId =
            ElementId("res:${screenId.value}/$resourceName")

        fun fromContentDescription(screenId: ScreenId, description: String): ElementId =
            ElementId("cd:${screenId.value}/${description.hashCode().toUInt()}")

        fun fromTestTag(screenId: ScreenId, tag: String): ElementId =
            ElementId("tag:${screenId.value}/$tag")

        fun fromXPath(screenId: ScreenId, xpath: String): ElementId =
            ElementId("xpath:${screenId.value}/$xpath")
    }

    val strategy: String get() = value.substringBefore(":")
}
