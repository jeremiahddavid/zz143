package com.zz143.core.event

import com.zz143.core.model.*
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

internal class EventEncoder {

    companion object Tags {
        const val TAG_EVENT_TYPE: Byte = 0x01
        const val TAG_EVENT_ID: Byte = 0x02
        const val TAG_SESSION_ID: Byte = 0x03
        const val TAG_TIMESTAMP: Byte = 0x04
        const val TAG_SCREEN_ID: Byte = 0x05
        const val TAG_ELEMENT_ID: Byte = 0x06
        const val TAG_GESTURE_TYPE: Byte = 0x07
        const val TAG_COORDINATES: Byte = 0x08
        const val TAG_TEXT_HASH: Byte = 0x09
        const val TAG_SNAPSHOT_ROOT: Byte = 0x0A
        const val TAG_MUTATION: Byte = 0x0B
        const val TAG_NAV_TYPE: Byte = 0x0C
        const val TAG_PROPERTY: Byte = 0x0D
        const val TAG_PARAM: Byte = 0x0E
        const val TAG_ACTION_TYPE: Byte = 0x0F
        const val TAG_LIFECYCLE: Byte = 0x10
        const val TAG_DURATION: Byte = 0x11
        const val TAG_END: Byte = 0x7F
    }

    fun encode(event: ZZ143Event): ByteArray {
        val buffer = ByteArrayOutputStream(256)
        val out = DataOutputStream(buffer)

        out.writeByte(TAG_EVENT_TYPE.toInt())
        out.writeByte(eventTypeOrdinal(event))

        writeString(out, TAG_EVENT_ID, event.eventId)
        writeString(out, TAG_SESSION_ID, event.sessionId.value)
        writeLong(out, TAG_TIMESTAMP, event.timestampMs)
        writeString(out, TAG_SCREEN_ID, event.screenId.value)

        when (event) {
            is ZZ143Event.Gesture -> encodeGesture(out, event.gesture)
            is ZZ143Event.Navigation -> encodeNavigation(out, event.navigation)
            is ZZ143Event.TextInput -> encodeTextInput(out, event.input)
            is ZZ143Event.Action -> encodeAction(out, event.action)
            is ZZ143Event.Lifecycle -> encodeLifecycle(out, event)
            is ZZ143Event.Snapshot -> {} // Snapshots encoded separately (large)
            is ZZ143Event.Delta -> {}    // Deltas encoded separately (large)
        }

        out.writeByte(TAG_END.toInt())
        return buffer.toByteArray()
    }

    private fun encodeGesture(out: DataOutputStream, gesture: GestureEvent) {
        out.writeByte(TAG_GESTURE_TYPE.toInt())
        out.writeByte(gesture.type.ordinal)
        gesture.targetElementId?.let { writeString(out, TAG_ELEMENT_ID, it.value) }
        writeCoordinates(out, gesture.startX, gesture.startY)
        gesture.endX?.let { writeCoordinates(out, it, gesture.endY!!) }
        writeLong(out, TAG_DURATION, gesture.durationMs)
    }

    private fun encodeNavigation(out: DataOutputStream, nav: NavigationEvent) {
        out.writeByte(TAG_NAV_TYPE.toInt())
        out.writeByte(nav.type.ordinal)
        nav.fromScreenId?.let { writeString(out, TAG_SCREEN_ID, it.value) }
        writeString(out, TAG_SCREEN_ID, nav.toScreenId.value)
        nav.route?.let { writeString(out, TAG_PROPERTY, it) }
    }

    private fun encodeTextInput(out: DataOutputStream, input: TextInputEvent) {
        writeString(out, TAG_ELEMENT_ID, input.targetElementId.value)
        out.writeByte(TAG_PROPERTY.toInt())
        out.writeByte(input.fieldType.ordinal)
        writeVarInt(out, input.textLength)
        input.hashedValue?.let { writeString(out, TAG_TEXT_HASH, it) }
    }

    private fun encodeAction(out: DataOutputStream, action: SemanticAction) {
        writeString(out, TAG_ACTION_TYPE, action.actionType)
        out.writeByte(action.actionSource.ordinal)
        action.targetElementId?.let { writeString(out, TAG_ELEMENT_ID, it.value) }
        for ((key, value) in action.parameters) {
            writeString(out, TAG_PARAM, "$key=$value")
        }
    }

    private fun encodeLifecycle(out: DataOutputStream, event: ZZ143Event.Lifecycle) {
        out.writeByte(TAG_LIFECYCLE.toInt())
        out.writeByte(event.type.ordinal)
    }

    private fun eventTypeOrdinal(event: ZZ143Event): Int = when (event) {
        is ZZ143Event.Snapshot -> 0
        is ZZ143Event.Delta -> 1
        is ZZ143Event.Gesture -> 2
        is ZZ143Event.Navigation -> 3
        is ZZ143Event.TextInput -> 4
        is ZZ143Event.Action -> 5
        is ZZ143Event.Lifecycle -> 6
    }

    private fun writeString(out: DataOutputStream, tag: Byte, value: String) {
        out.writeByte(tag.toInt())
        val bytes = value.toByteArray(Charsets.UTF_8)
        writeVarInt(out, bytes.size)
        out.write(bytes)
    }

    private fun writeLong(out: DataOutputStream, tag: Byte, value: Long) {
        out.writeByte(tag.toInt())
        out.writeLong(value)
    }

    private fun writeCoordinates(out: DataOutputStream, x: Float, y: Float) {
        out.writeByte(TAG_COORDINATES.toInt())
        out.writeShort(x.toInt())
        out.writeShort(y.toInt())
    }

    internal fun writeVarInt(out: DataOutputStream, value: Int) {
        var v = value
        while (v and 0x7F.inv() != 0) {
            out.writeByte((v and 0x7F) or 0x80)
            v = v ushr 7
        }
        out.writeByte(v and 0x7F)
    }
}
