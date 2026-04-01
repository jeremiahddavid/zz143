package com.zz143.capture.viewtree

import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import com.zz143.core.ZZ143Config
import com.zz143.core.model.ElementId
import com.zz143.core.model.Rect
import com.zz143.core.model.ScreenId
import com.zz143.core.model.ViewNode

internal class ViewTreeWalker(
    private val config: ZZ143Config,
    private val idGenerator: StableIdGenerator = StableIdGenerator()
) {
    private val locationBuffer = IntArray(2)
    private val resourceNameCache = HashMap<Int, String?>(256)

    fun walk(rootView: View, screenId: ScreenId): ViewNode? {
        return walkRecursive(rootView, screenId, depth = 0, childIndex = 0)
    }

    private fun walkRecursive(
        view: View,
        screenId: ScreenId,
        depth: Int,
        childIndex: Int
    ): ViewNode? {
        if (depth > config.maxSnapshotDepth) return null
        if (config.skipInvisibleViews && view.visibility != View.VISIBLE) return null

        val elementId = idGenerator.generate(view, screenId, resourceNameCache)

        view.getLocationOnScreen(locationBuffer)
        val bounds = Rect(
            left = locationBuffer[0],
            top = locationBuffer[1],
            right = locationBuffer[0] + view.width,
            bottom = locationBuffer[1] + view.height
        )

        val isSensitive = view is EditText && (
            view.inputType and android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD != 0 ||
            view.inputType and android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD != 0 ||
            view.inputType and android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD != 0
        )

        val text = when {
            isSensitive -> null
            !config.captureTextValues -> null
            view is TextView -> view.text?.toString()?.take(200)
            else -> null
        }

        val children = if (view is ViewGroup) {
            val childList = mutableListOf<ViewNode>()
            val childCount = minOf(view.childCount, 100) // cap at 100 children
            for (i in 0 until childCount) {
                val child = view.getChildAt(i)
                walkRecursive(child, screenId, depth + 1, i)?.let { childList.add(it) }
            }
            childList
        } else {
            emptyList()
        }

        return ViewNode(
            elementId = elementId,
            className = view.javaClass.simpleName,
            resourceIdName = resolveResourceName(view),
            contentDescription = view.contentDescription?.toString(),
            text = text,
            isVisible = view.visibility == View.VISIBLE,
            isEnabled = view.isEnabled,
            isClickable = view.isClickable,
            isFocused = view.isFocused,
            isEditable = view is EditText,
            bounds = bounds,
            treeDepth = depth,
            childIndex = childIndex,
            children = children,
            isSensitive = isSensitive,
            testTag = null,
            scrollOffsetX = view.scrollX,
            scrollOffsetY = view.scrollY
        )
    }

    private fun resolveResourceName(view: View): String? {
        if (view.id == View.NO_ID) return null
        return resourceNameCache.getOrPut(view.id) {
            try {
                view.resources.getResourceEntryName(view.id)
            } catch (_: Exception) {
                null
            }
        }
    }

    fun clearCaches() {
        resourceNameCache.clear()
    }
}
