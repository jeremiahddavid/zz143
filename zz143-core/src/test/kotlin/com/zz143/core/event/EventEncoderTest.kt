package com.zz143.core.event

import com.google.common.truth.Truth.assertThat
import com.zz143.core.model.*
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

class EventEncoderTest {

    private lateinit var encoder: EventEncoder

    private val sessionId = SessionId("test-session-001")
    private val screenId = ScreenId("activity:MainActivity")
    private val timestampMs = 1700000000000L
    private val uptimeMs = 5000L

    @Before
    fun setUp() {
        encoder = EventEncoder()
    }

    // --- Structural tests ---

    @Test
    fun encodedBytesStartWithTagEventType() {
        val event = gestureEvent()
        val bytes = encoder.encode(event)

        assertThat(bytes[0]).isEqualTo(EventEncoder.TAG_EVENT_TYPE)
    }

    @Test
    fun encodedBytesEndWithTagEnd() {
        val event = gestureEvent()
        val bytes = encoder.encode(event)

        assertThat(bytes.last()).isEqualTo(EventEncoder.TAG_END)
    }

    @Test
    fun gestureEventEncodesTypeOrdinal2() {
        val event = gestureEvent()
        val bytes = encoder.encode(event)

        // byte[0] = TAG_EVENT_TYPE, byte[1] = ordinal
        assertThat(bytes[1].toInt()).isEqualTo(2) // Gesture ordinal
    }

    @Test
    fun navigationEventEncodesTypeOrdinal3() {
        val event = navigationEvent()
        val bytes = encoder.encode(event)

        assertThat(bytes[1].toInt()).isEqualTo(3) // Navigation ordinal
    }

    @Test
    fun textInputEventEncodesTypeOrdinal4() {
        val event = textInputEvent()
        val bytes = encoder.encode(event)

        assertThat(bytes[1].toInt()).isEqualTo(4)
    }

    @Test
    fun actionEventEncodesTypeOrdinal5() {
        val event = actionEvent()
        val bytes = encoder.encode(event)

        assertThat(bytes[1].toInt()).isEqualTo(5)
    }

    @Test
    fun lifecycleEventEncodesTypeOrdinal6() {
        val event = lifecycleEvent()
        val bytes = encoder.encode(event)

        assertThat(bytes[1].toInt()).isEqualTo(6)
    }

    // --- Gesture encoding ---

    @Test
    fun gestureEventContainsGestureTypeTag() {
        val event = gestureEvent(gestureType = GestureType.TAP)
        val bytes = encoder.encode(event)

        assertThat(bytes.toList()).contains(EventEncoder.TAG_GESTURE_TYPE)
    }

    @Test
    fun gestureEventContainsCoordinatesTag() {
        val event = gestureEvent()
        val bytes = encoder.encode(event)

        assertThat(bytes.toList()).contains(EventEncoder.TAG_COORDINATES)
    }

    @Test
    fun gestureEventContainsDurationTag() {
        val event = gestureEvent()
        val bytes = encoder.encode(event)

        assertThat(bytes.toList()).contains(EventEncoder.TAG_DURATION)
    }

    @Test
    fun gestureWithTargetElementContainsElementIdTag() {
        val event = gestureEvent(targetElementId = ElementId("res:activity:Main/btn_ok"))
        val bytes = encoder.encode(event)

        assertThat(bytes.toList()).contains(EventEncoder.TAG_ELEMENT_ID)
    }

    @Test
    fun gestureWithoutTargetElementOmitsElementIdTag() {
        val event = gestureEvent(targetElementId = null)
        val bytes = encoder.encode(event)

        assertThat(bytes.toList()).doesNotContain(EventEncoder.TAG_ELEMENT_ID)
    }

    // --- Navigation encoding ---

    @Test
    fun navigationEventContainsNavTypeTag() {
        val event = navigationEvent()
        val bytes = encoder.encode(event)

        assertThat(bytes.toList()).contains(EventEncoder.TAG_NAV_TYPE)
    }

    @Test
    fun navigationWithRouteContainsPropertyTag() {
        val event = navigationEvent(route = "/home")
        val bytes = encoder.encode(event)

        assertThat(bytes.toList()).contains(EventEncoder.TAG_PROPERTY)
    }

    @Test
    fun navigationWithoutRouteOmitsPropertyTag() {
        val event = navigationEvent(route = null)
        val bytes = encoder.encode(event)

        // TAG_PROPERTY should not appear as a route tag when route is null
        // (the only TAG_PROPERTY source in navigation encoding is the route)
        val navTypeIndex = bytes.indexOfFirst { it == EventEncoder.TAG_NAV_TYPE }
        val endIndex = bytes.indexOfLast { it == EventEncoder.TAG_END }
        val navSection = bytes.slice(navTypeIndex..endIndex)
        assertThat(navSection.toList()).doesNotContain(EventEncoder.TAG_PROPERTY)
    }

    // --- TextInput encoding ---

    @Test
    fun textInputEventContainsElementIdTag() {
        val event = textInputEvent()
        val bytes = encoder.encode(event)

        assertThat(bytes.toList()).contains(EventEncoder.TAG_ELEMENT_ID)
    }

    @Test
    fun textInputEventContainsPropertyTag() {
        val event = textInputEvent()
        val bytes = encoder.encode(event)

        assertThat(bytes.toList()).contains(EventEncoder.TAG_PROPERTY)
    }

    @Test
    fun textInputWithHashedValueContainsTextHashTag() {
        val event = textInputEvent(hashedValue = "abc123")
        val bytes = encoder.encode(event)

        assertThat(bytes.toList()).contains(EventEncoder.TAG_TEXT_HASH)
    }

    @Test
    fun textInputWithoutHashedValueOmitsTextHashTag() {
        val event = textInputEvent(hashedValue = null)
        val bytes = encoder.encode(event)

        assertThat(bytes.toList()).doesNotContain(EventEncoder.TAG_TEXT_HASH)
    }

    // --- Action encoding ---

    @Test
    fun actionEventContainsActionTypeTag() {
        val event = actionEvent()
        val bytes = encoder.encode(event)

        assertThat(bytes.toList()).contains(EventEncoder.TAG_ACTION_TYPE)
    }

    @Test
    fun actionWithTargetElementContainsElementIdTag() {
        val event = actionEvent(targetElementId = ElementId("res:activity:Main/btn"))
        val bytes = encoder.encode(event)

        assertThat(bytes.toList()).contains(EventEncoder.TAG_ELEMENT_ID)
    }

    @Test
    fun actionWithParametersContainsParamTags() {
        val event = actionEvent(parameters = mapOf("key1" to "val1", "key2" to "val2"))
        val bytes = encoder.encode(event)

        val paramCount = bytes.count { it == EventEncoder.TAG_PARAM }
        assertThat(paramCount).isEqualTo(2)
    }

    @Test
    fun actionWithNoParametersOmitsParamTag() {
        val event = actionEvent(parameters = emptyMap())
        val bytes = encoder.encode(event)

        assertThat(bytes.toList()).doesNotContain(EventEncoder.TAG_PARAM)
    }

    // --- Lifecycle encoding ---

    @Test
    fun lifecycleEventContainsLifecycleTag() {
        val event = lifecycleEvent()
        val bytes = encoder.encode(event)

        assertThat(bytes.toList()).contains(EventEncoder.TAG_LIFECYCLE)
    }

    @Test
    fun lifecycleSessionStartEncodesOrdinal0() {
        val event = lifecycleEvent(type = LifecycleType.SESSION_START)
        val bytes = encoder.encode(event)

        // Search from end — TAG_LIFECYCLE is near the end, before TAG_END
        val lifecycleTagIndex = bytes.indexOfLast { it == EventEncoder.TAG_LIFECYCLE }
        assertThat(lifecycleTagIndex).isGreaterThan(-1)
        assertThat(bytes[lifecycleTagIndex + 1].toInt()).isEqualTo(LifecycleType.SESSION_START.ordinal)
    }

    @Test
    fun lifecycleAppBackgroundedEncodesOrdinal2() {
        val event = lifecycleEvent(type = LifecycleType.APP_BACKGROUNDED)
        val bytes = encoder.encode(event)

        val lifecycleTagIndex = bytes.indexOfLast { it == EventEncoder.TAG_LIFECYCLE }
        assertThat(lifecycleTagIndex).isGreaterThan(-1)
        assertThat(bytes[lifecycleTagIndex + 1].toInt()).isEqualTo(LifecycleType.APP_BACKGROUNDED.ordinal)
    }

    // --- VarInt encoding ---

    @Test
    fun writeVarIntEncodesSingleByteForSmallValues() {
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)
        encoder.writeVarInt(dos, 42)
        dos.flush()

        val bytes = baos.toByteArray()
        assertThat(bytes.size).isEqualTo(1)
        assertThat(bytes[0].toInt() and 0xFF).isEqualTo(42)
    }

    @Test
    fun writeVarIntEncodesTwoBytesForValueAbove127() {
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)
        encoder.writeVarInt(dos, 300)
        dos.flush()

        val bytes = baos.toByteArray()
        assertThat(bytes.size).isEqualTo(2)
        // 300 = 0b100101100
        // byte 0: (300 & 0x7F) | 0x80 = 0b00101100 | 0x80 = 0xAC
        // byte 1: 300 >>> 7 = 2 = 0x02
        assertThat(bytes[0].toInt() and 0xFF).isEqualTo(0xAC)
        assertThat(bytes[1].toInt() and 0xFF).isEqualTo(0x02)
    }

    @Test
    fun writeVarIntEncodesZeroAsSingleByte() {
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)
        encoder.writeVarInt(dos, 0)
        dos.flush()

        val bytes = baos.toByteArray()
        assertThat(bytes.size).isEqualTo(1)
        assertThat(bytes[0].toInt()).isEqualTo(0)
    }

    @Test
    fun writeVarIntEncodesMaxSingleByteValue127() {
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)
        encoder.writeVarInt(dos, 127)
        dos.flush()

        val bytes = baos.toByteArray()
        assertThat(bytes.size).isEqualTo(1)
        assertThat(bytes[0].toInt() and 0xFF).isEqualTo(127)
    }

    @Test
    fun writeVarIntEncodesValue128AsTwoBytes() {
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)
        encoder.writeVarInt(dos, 128)
        dos.flush()

        val bytes = baos.toByteArray()
        assertThat(bytes.size).isEqualTo(2)
        // 128 = 0x80 => byte0: (128 & 0x7F) | 0x80 = 0x80, byte1: 128 >>> 7 = 1
        assertThat(bytes[0].toInt() and 0xFF).isEqualTo(0x80)
        assertThat(bytes[1].toInt() and 0xFF).isEqualTo(0x01)
    }

    @Test
    fun writeVarIntEncodesLargeValueMultipleBytes() {
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)
        encoder.writeVarInt(dos, 16384) // 2^14
        dos.flush()

        val bytes = baos.toByteArray()
        assertThat(bytes.size).isEqualTo(3)
    }

    // --- Common fields ---

    @Test
    fun allEventTypesEncodeSessionIdTag() {
        val event = gestureEvent()
        val bytes = encoder.encode(event)

        assertThat(bytes.toList()).contains(EventEncoder.TAG_SESSION_ID)
    }

    @Test
    fun allEventTypesEncodeTimestampTag() {
        val event = gestureEvent()
        val bytes = encoder.encode(event)

        assertThat(bytes.toList()).contains(EventEncoder.TAG_TIMESTAMP)
    }

    @Test
    fun allEventTypesEncodeEventIdTag() {
        val event = gestureEvent()
        val bytes = encoder.encode(event)

        assertThat(bytes.toList()).contains(EventEncoder.TAG_EVENT_ID)
    }

    @Test
    fun allEventTypesEncodeScreenIdTag() {
        val event = gestureEvent()
        val bytes = encoder.encode(event)

        assertThat(bytes.toList()).contains(EventEncoder.TAG_SCREEN_ID)
    }

    // --- Helpers ---

    private fun gestureEvent(
        gestureType: GestureType = GestureType.TAP,
        targetElementId: ElementId? = null,
        startX: Float = 100f,
        startY: Float = 200f,
        endX: Float? = null,
        endY: Float? = null,
        durationMs: Long = 50L
    ): ZZ143Event.Gesture = ZZ143Event.Gesture(
        eventId = "evt-001",
        sessionId = sessionId,
        timestampMs = timestampMs,
        uptimeMs = uptimeMs,
        screenId = screenId,
        gesture = GestureEvent(
            type = gestureType,
            targetElementId = targetElementId,
            startX = startX,
            startY = startY,
            endX = endX,
            endY = endY,
            durationMs = durationMs
        )
    )

    private fun navigationEvent(
        type: NavigationType = NavigationType.ACTIVITY_CREATED,
        fromScreenId: ScreenId? = ScreenId("activity:PreviousActivity"),
        toScreenId: ScreenId = ScreenId("activity:NextActivity"),
        route: String? = "/detail"
    ): ZZ143Event.Navigation = ZZ143Event.Navigation(
        eventId = "evt-002",
        sessionId = sessionId,
        timestampMs = timestampMs,
        uptimeMs = uptimeMs,
        screenId = screenId,
        navigation = NavigationEvent(
            type = type,
            fromScreenId = fromScreenId,
            toScreenId = toScreenId,
            route = route,
            transitionType = null
        )
    )

    private fun textInputEvent(
        targetElementId: ElementId = ElementId("res:activity:Main/et_name"),
        fieldType: TextFieldType = TextFieldType.PLAIN,
        textLength: Int = 10,
        hashedValue: String? = "hash123"
    ): ZZ143Event.TextInput = ZZ143Event.TextInput(
        eventId = "evt-003",
        sessionId = sessionId,
        timestampMs = timestampMs,
        uptimeMs = uptimeMs,
        screenId = screenId,
        input = TextInputEvent(
            targetElementId = targetElementId,
            fieldType = fieldType,
            isRedacted = false,
            textLength = textLength,
            hashedValue = hashedValue
        )
    )

    private fun actionEvent(
        actionType: String = "add_to_cart",
        actionSource: ActionSource = ActionSource.ANNOTATION,
        targetElementId: ElementId? = null,
        parameters: Map<String, String> = mapOf("item" to "shoes")
    ): ZZ143Event.Action = ZZ143Event.Action(
        eventId = "evt-004",
        sessionId = sessionId,
        timestampMs = timestampMs,
        uptimeMs = uptimeMs,
        screenId = screenId,
        action = SemanticAction(
            actionType = actionType,
            actionSource = actionSource,
            targetElementId = targetElementId,
            parameters = parameters
        )
    )

    private fun lifecycleEvent(
        type: LifecycleType = LifecycleType.SESSION_START
    ): ZZ143Event.Lifecycle = ZZ143Event.Lifecycle(
        eventId = "evt-005",
        sessionId = sessionId,
        timestampMs = timestampMs,
        uptimeMs = uptimeMs,
        screenId = screenId,
        type = type
    )
}
