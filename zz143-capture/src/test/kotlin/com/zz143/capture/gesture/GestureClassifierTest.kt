package com.zz143.capture.gesture

import com.google.common.truth.Truth.assertThat
import com.zz143.core.model.GestureType
import org.junit.Before
import org.junit.Test

class GestureClassifierTest {

    private lateinit var classifier: GestureClassifier

    @Before
    fun setUp() {
        classifier = GestureClassifier()
    }

    // --- TAP ---

    @Test
    fun noMovementShortDurationClassifiesAsTap() {
        val result = classifier.classify(
            startX = 100f, startY = 200f,
            endX = 100f, endY = 200f,
            durationMs = 100
        )
        assertThat(result).isEqualTo(GestureType.TAP)
    }

    @Test
    fun smallMovementBelowThresholdShortDurationClassifiesAsTap() {
        // distance = sqrt(10^2 + 10^2) ~ 14.14 < 20 (tap threshold)
        val result = classifier.classify(
            startX = 100f, startY = 200f,
            endX = 110f, endY = 210f,
            durationMs = 50
        )
        assertThat(result).isEqualTo(GestureType.TAP)
    }

    @Test
    fun movementJustBelowTapThresholdClassifiesAsTap() {
        // distance ~ 19.79 < 20
        val result = classifier.classify(
            startX = 0f, startY = 0f,
            endX = 14f, endY = 14f,
            durationMs = 100
        )
        assertThat(result).isEqualTo(GestureType.TAP)
    }

    // --- LONG_PRESS ---

    @Test
    fun noMovementLongDurationClassifiesAsLongPress() {
        val result = classifier.classify(
            startX = 100f, startY = 200f,
            endX = 100f, endY = 200f,
            durationMs = 600
        )
        assertThat(result).isEqualTo(GestureType.LONG_PRESS)
    }

    @Test
    fun smallMovementWithDurationExactly500msClassifiesAsTap() {
        // durationMs = 500, threshold is > 500, so 500 is NOT long press
        val result = classifier.classify(
            startX = 100f, startY = 200f,
            endX = 100f, endY = 200f,
            durationMs = 500
        )
        assertThat(result).isEqualTo(GestureType.TAP)
    }

    @Test
    fun smallMovementWithDuration501msClassifiesAsLongPress() {
        val result = classifier.classify(
            startX = 100f, startY = 200f,
            endX = 100f, endY = 200f,
            durationMs = 501
        )
        assertThat(result).isEqualTo(GestureType.LONG_PRESS)
    }

    // --- SWIPE_RIGHT ---

    @Test
    fun largePositiveDxClassifiesAsSwipeRight() {
        val result = classifier.classify(
            startX = 0f, startY = 100f,
            endX = 200f, endY = 100f,
            durationMs = 200
        )
        assertThat(result).isEqualTo(GestureType.SWIPE_RIGHT)
    }

    // --- SWIPE_LEFT ---

    @Test
    fun largeNegativeDxClassifiesAsSwipeLeft() {
        val result = classifier.classify(
            startX = 200f, startY = 100f,
            endX = 0f, endY = 100f,
            durationMs = 200
        )
        assertThat(result).isEqualTo(GestureType.SWIPE_LEFT)
    }

    // --- SWIPE_DOWN ---

    @Test
    fun largePositiveDyClassifiesAsSwipeDown() {
        val result = classifier.classify(
            startX = 100f, startY = 0f,
            endX = 100f, endY = 200f,
            durationMs = 200
        )
        assertThat(result).isEqualTo(GestureType.SWIPE_DOWN)
    }

    // --- SWIPE_UP ---

    @Test
    fun largeNegativeDyClassifiesAsSwipeUp() {
        val result = classifier.classify(
            startX = 100f, startY = 200f,
            endX = 100f, endY = 0f,
            durationMs = 200
        )
        assertThat(result).isEqualTo(GestureType.SWIPE_UP)
    }

    // --- SCROLL (between tap threshold and swipe threshold) ---

    @Test
    fun mediumDistanceBetweenTapAndSwipeThresholdClassifiesAsScroll() {
        // distance = 50, which is >= 20 (tap) but <= 100 (swipe)
        val result = classifier.classify(
            startX = 100f, startY = 100f,
            endX = 150f, endY = 100f,
            durationMs = 200
        )
        assertThat(result).isEqualTo(GestureType.SCROLL)
    }

    @Test
    fun distanceExactlyAtSwipeThresholdClassifiesAsScroll() {
        // distance = 100, threshold is > 100, so 100 is NOT a swipe
        val result = classifier.classify(
            startX = 0f, startY = 0f,
            endX = 100f, endY = 0f,
            durationMs = 200
        )
        assertThat(result).isEqualTo(GestureType.SCROLL)
    }

    @Test
    fun distanceJustAboveSwipeThresholdClassifiesAsSwipe() {
        // distance = 101 > 100 (swipe threshold), horizontal right
        val result = classifier.classify(
            startX = 0f, startY = 0f,
            endX = 101f, endY = 0f,
            durationMs = 200
        )
        assertThat(result).isEqualTo(GestureType.SWIPE_RIGHT)
    }

    // --- Boundary: tap threshold edge ---

    @Test
    fun distanceExactlyAtTapThresholdClassifiesAsScroll() {
        // distance = 20, which is NOT < 20, so it's not a tap.
        // distance = 20 <= 100, so it's a scroll.
        val result = classifier.classify(
            startX = 0f, startY = 0f,
            endX = 20f, endY = 0f,
            durationMs = 50
        )
        assertThat(result).isEqualTo(GestureType.SCROLL)
    }

    @Test
    fun distanceJustBelowTapThresholdClassifiesAsTap() {
        // distance ~ 19.9 < 20 => tap
        val result = classifier.classify(
            startX = 0f, startY = 0f,
            endX = 19.9f, endY = 0f,
            durationMs = 50
        )
        assertThat(result).isEqualTo(GestureType.TAP)
    }

    // --- Swipe direction: diagonal favoring horizontal ---

    @Test
    fun diagonalSwipeFavoringHorizontalClassifiesAsSwipeRight() {
        // dx = 150, dy = 50 => abs(dx) > abs(dy), dx > 0 => SWIPE_RIGHT
        val result = classifier.classify(
            startX = 0f, startY = 0f,
            endX = 150f, endY = 50f,
            durationMs = 200
        )
        assertThat(result).isEqualTo(GestureType.SWIPE_RIGHT)
    }

    @Test
    fun diagonalSwipeFavoringVerticalClassifiesAsSwipeDown() {
        // dx = 50, dy = 150 => abs(dy) > abs(dx), dy > 0 => SWIPE_DOWN
        val result = classifier.classify(
            startX = 0f, startY = 0f,
            endX = 50f, endY = 150f,
            durationMs = 200
        )
        assertThat(result).isEqualTo(GestureType.SWIPE_DOWN)
    }

    // --- Custom thresholds ---

    @Test
    fun customTapThresholdChangesClassification() {
        val customClassifier = GestureClassifier(tapThresholdPx = 50f)
        // distance = 30, which is < 50 (custom tap threshold)
        val result = customClassifier.classify(
            startX = 0f, startY = 0f,
            endX = 30f, endY = 0f,
            durationMs = 50
        )
        assertThat(result).isEqualTo(GestureType.TAP)
    }

    @Test
    fun customLongPressThresholdChangesClassification() {
        val customClassifier = GestureClassifier(longPressThresholdMs = 200L)
        // durationMs = 300, which is > 200 (custom threshold)
        val result = customClassifier.classify(
            startX = 0f, startY = 0f,
            endX = 0f, endY = 0f,
            durationMs = 300
        )
        assertThat(result).isEqualTo(GestureType.LONG_PRESS)
    }

    @Test
    fun customSwipeThresholdChangesClassification() {
        val customClassifier = GestureClassifier(swipeThresholdPx = 50f)
        // distance = 60, > 50 (custom swipe threshold), horizontal right
        val result = customClassifier.classify(
            startX = 0f, startY = 0f,
            endX = 60f, endY = 0f,
            durationMs = 200
        )
        assertThat(result).isEqualTo(GestureType.SWIPE_RIGHT)
    }
}
