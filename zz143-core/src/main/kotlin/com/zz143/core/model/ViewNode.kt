package com.zz143.core.model

data class ViewNode(
    val elementId: ElementId,
    val className: String,
    val resourceIdName: String?,
    val contentDescription: String?,
    val text: String?,
    val isVisible: Boolean,
    val isEnabled: Boolean,
    val isClickable: Boolean,
    val isFocused: Boolean,
    val isEditable: Boolean,
    val bounds: Rect,
    val treeDepth: Int,
    val childIndex: Int,
    val children: List<ViewNode>,
    val isSensitive: Boolean,
    val testTag: String?,
    val scrollOffsetX: Int = 0,
    val scrollOffsetY: Int = 0,
) {
    val nodeCount: Int get() = 1 + children.sumOf { it.nodeCount }

    fun flatten(): List<ViewNode> = listOf(this) + children.flatMap { it.flatten() }

    fun findById(id: ElementId): ViewNode? {
        if (elementId == id) return this
        return children.firstNotNullOfOrNull { it.findById(id) }
    }

    // Structural hash for fast subtree comparison
    val structuralHash: Int by lazy {
        var h = className.hashCode()
        h = 31 * h + (resourceIdName?.hashCode() ?: 0)
        h = 31 * h + children.size
        h = 31 * h + children.fold(0) { acc, child -> 31 * acc + child.structuralHash }
        h
    }
}

data class Rect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val centerX: Int get() = (left + right) / 2
    val centerY: Int get() = (top + bottom) / 2
}
