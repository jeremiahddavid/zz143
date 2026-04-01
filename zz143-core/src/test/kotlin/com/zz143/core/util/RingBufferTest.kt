package com.zz143.core.util

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class RingBufferTest {

    private lateinit var buffer: RingBuffer<Int>

    @Before
    fun setUp() {
        buffer = RingBuffer(5)
    }

    // --- add and size ---

    @Test
    fun newBufferIsEmpty() {
        assertThat(buffer.size).isEqualTo(0)
    }

    @Test
    fun addSingleElementIncreasesSize() {
        buffer.add(10)
        assertThat(buffer.size).isEqualTo(1)
    }

    @Test
    fun addMultipleElementsIncreasesSizeUpToCapacity() {
        repeat(5) { buffer.add(it) }
        assertThat(buffer.size).isEqualTo(5)
    }

    @Test
    fun sizeNeverExceedsCapacity() {
        repeat(10) { buffer.add(it) }
        assertThat(buffer.size).isEqualTo(5)
    }

    // --- get ---

    @Test
    fun getReturnsElementsInInsertionOrder() {
        buffer.add(10)
        buffer.add(20)
        buffer.add(30)

        assertThat(buffer[0]).isEqualTo(10)
        assertThat(buffer[1]).isEqualTo(20)
        assertThat(buffer[2]).isEqualTo(30)
    }

    @Test
    fun getAfterOverflowReturnsNewestElements() {
        // capacity = 5, add 7 elements => oldest two are evicted
        for (i in 0 until 7) buffer.add(i)

        assertThat(buffer[0]).isEqualTo(2) // oldest surviving
        assertThat(buffer[4]).isEqualTo(6) // newest
    }

    @Test(expected = IllegalArgumentException::class)
    fun getNegativeIndexThrows() {
        buffer.add(1)
        buffer[-1]
    }

    @Test(expected = IllegalArgumentException::class)
    fun getIndexAtSizeThrows() {
        buffer.add(1)
        buffer.add(2)
        buffer[2]
    }

    @Test(expected = IllegalArgumentException::class)
    fun getOnEmptyBufferThrows() {
        buffer[0]
    }

    // --- overflow behavior ---

    @Test
    fun overflowEvictsOldestElement() {
        for (i in 1..6) buffer.add(i * 10)
        // Elements: 20, 30, 40, 50, 60 (10 was evicted)
        assertThat(buffer[0]).isEqualTo(20)
    }

    @Test
    fun multipleOverflowCyclesWorkCorrectly() {
        // Add 15 elements to a buffer of capacity 5
        for (i in 1..15) buffer.add(i)
        // Should contain 11, 12, 13, 14, 15
        assertThat(buffer.toList()).containsExactly(11, 12, 13, 14, 15).inOrder()
    }

    @Test
    fun overflowByExactlyOneEvictsOneElement() {
        for (i in 1..5) buffer.add(i)
        buffer.add(99)
        // 1 evicted, 2..5, 99 remain
        assertThat(buffer.toList()).containsExactly(2, 3, 4, 5, 99).inOrder()
    }

    // --- toList ---

    @Test
    fun toListOnEmptyBufferReturnsEmptyList() {
        assertThat(buffer.toList()).isEmpty()
    }

    @Test
    fun toListReturnsElementsInInsertionOrder() {
        buffer.add(1)
        buffer.add(2)
        buffer.add(3)
        assertThat(buffer.toList()).containsExactly(1, 2, 3).inOrder()
    }

    @Test
    fun toListAfterOverflowReturnsCorrectOrder() {
        for (i in 0 until 8) buffer.add(i)
        assertThat(buffer.toList()).containsExactly(3, 4, 5, 6, 7).inOrder()
    }

    // --- clear ---

    @Test
    fun clearResetsSizeToZero() {
        buffer.add(1)
        buffer.add(2)
        buffer.clear()
        assertThat(buffer.size).isEqualTo(0)
    }

    @Test
    fun clearMakesToListEmpty() {
        buffer.add(1)
        buffer.add(2)
        buffer.clear()
        assertThat(buffer.toList()).isEmpty()
    }

    @Test
    fun addAfterClearWorksCorrectly() {
        for (i in 1..5) buffer.add(i)
        buffer.clear()
        buffer.add(100)
        buffer.add(200)
        assertThat(buffer.toList()).containsExactly(100, 200).inOrder()
    }

    // --- iterator ---

    @Test
    fun iteratorReturnsAllElements() {
        buffer.add(10)
        buffer.add(20)
        buffer.add(30)

        val collected = mutableListOf<Int>()
        for (item in buffer) collected.add(item)
        assertThat(collected).containsExactly(10, 20, 30).inOrder()
    }

    @Test
    fun iteratorOnEmptyBufferHasNoElements() {
        val collected = mutableListOf<Int>()
        for (item in buffer) collected.add(item)
        assertThat(collected).isEmpty()
    }

    @Test
    fun iteratorAfterOverflowReturnsNewestElements() {
        for (i in 0 until 8) buffer.add(i)

        val collected = mutableListOf<Int>()
        for (item in buffer) collected.add(item)
        assertThat(collected).containsExactly(3, 4, 5, 6, 7).inOrder()
    }

    // --- with different types ---

    @Test
    fun bufferWorksWithStringType() {
        val strBuffer = RingBuffer<String>(3)
        strBuffer.add("a")
        strBuffer.add("b")
        strBuffer.add("c")
        strBuffer.add("d")

        assertThat(strBuffer.toList()).containsExactly("b", "c", "d").inOrder()
    }

    // --- capacity 1 edge case ---

    @Test
    fun capacityOneBufferAlwaysHoldsLatest() {
        val tiny = RingBuffer<Int>(1)
        tiny.add(1)
        tiny.add(2)
        tiny.add(3)

        assertThat(tiny.size).isEqualTo(1)
        assertThat(tiny[0]).isEqualTo(3)
    }
}
