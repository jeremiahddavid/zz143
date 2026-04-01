package com.zz143.capture.viewtree

import com.zz143.core.model.ElementId
import com.zz143.core.model.Rect
import com.zz143.core.model.ViewNode

class ViewNodeBuilder {
    var elementId: ElementId = ElementId("test:element/default")
    var className: String = "android.view.View"
    var resourceIdName: String? = null
    var contentDescription: String? = null
    var text: String? = null
    var isVisible: Boolean = true
    var isEnabled: Boolean = true
    var isClickable: Boolean = false
    var isFocused: Boolean = false
    var isEditable: Boolean = false
    var bounds: Rect = Rect(0, 0, 100, 100)
    var treeDepth: Int = 0
    var childIndex: Int = 0
    var isSensitive: Boolean = false
    var testTag: String? = null
    var scrollOffsetX: Int = 0
    var scrollOffsetY: Int = 0
    private val children = mutableListOf<ViewNode>()

    fun child(init: ViewNodeBuilder.() -> Unit) {
        val builder = ViewNodeBuilder()
        builder.treeDepth = treeDepth + 1
        builder.childIndex = children.size
        builder.init()
        children.add(builder.build())
    }

    fun build(): ViewNode = ViewNode(
        elementId = elementId,
        className = className,
        resourceIdName = resourceIdName,
        contentDescription = contentDescription,
        text = text,
        isVisible = isVisible,
        isEnabled = isEnabled,
        isClickable = isClickable,
        isFocused = isFocused,
        isEditable = isEditable,
        bounds = bounds,
        treeDepth = treeDepth,
        childIndex = childIndex,
        children = children.toList(),
        isSensitive = isSensitive,
        testTag = testTag,
        scrollOffsetX = scrollOffsetX,
        scrollOffsetY = scrollOffsetY
    )
}

fun viewNode(init: ViewNodeBuilder.() -> Unit): ViewNode {
    val builder = ViewNodeBuilder()
    builder.init()
    return builder.build()
}
