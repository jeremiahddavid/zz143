package com.zz143.core.util

internal class RingBuffer<T>(private val capacity: Int) : Iterable<T> {
    private val buffer = arrayOfNulls<Any>(capacity)
    private var head = 0
    private var _size = 0

    val size: Int get() = _size

    fun add(element: T) {
        buffer[(head + _size) % capacity] = element
        if (_size == capacity) {
            head = (head + 1) % capacity
        } else {
            _size++
        }
    }

    @Suppress("UNCHECKED_CAST")
    operator fun get(index: Int): T {
        require(index in 0 until _size) { "Index $index out of bounds for size $_size" }
        return buffer[(head + index) % capacity] as T
    }

    fun toList(): List<T> = (0 until _size).map { get(it) }

    fun clear() {
        buffer.fill(null)
        head = 0
        _size = 0
    }

    override fun iterator(): Iterator<T> = object : Iterator<T> {
        private var index = 0
        override fun hasNext() = index < _size
        override fun next() = get(index++)
    }
}
