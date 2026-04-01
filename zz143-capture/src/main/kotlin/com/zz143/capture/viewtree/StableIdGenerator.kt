package com.zz143.capture.viewtree

import android.view.View
import com.zz143.core.model.ElementId
import com.zz143.core.model.ScreenId

internal class StableIdGenerator {

    fun generate(
        view: View,
        screenId: ScreenId,
        resourceNameCache: Map<Int, String?>
    ): ElementId {
        // Layer 1: Resource ID (most stable)
        if (view.id != View.NO_ID) {
            val resourceName = resourceNameCache[view.id]
                ?: try { view.resources.getResourceEntryName(view.id) } catch (_: Exception) { null }
            if (resourceName != null) {
                return ElementId.fromResource(screenId, resourceName)
            }
        }

        // Layer 2: Content description
        val contentDesc = view.contentDescription?.toString()
        if (!contentDesc.isNullOrBlank()) {
            return ElementId.fromContentDescription(screenId, contentDesc)
        }

        // Layer 3: Structural XPath
        val xpath = computeXPath(view)
        return ElementId.fromXPath(screenId, xpath)
    }

    private fun computeXPath(view: View): String {
        val segments = mutableListOf<String>()
        var current: View? = view

        while (current != null) {
            val parent = current.parent
            if (parent is android.view.ViewGroup) {
                val index = countSameClassSiblingsBefore(parent, current)
                segments.add("${current.javaClass.simpleName}[$index]")
            } else {
                segments.add(current.javaClass.simpleName)
            }
            current = parent as? View
        }

        return segments.reversed().joinToString("/")
    }

    private fun countSameClassSiblingsBefore(parent: android.view.ViewGroup, child: View): Int {
        var count = 0
        val targetClass = child.javaClass
        for (i in 0 until parent.childCount) {
            val sibling = parent.getChildAt(i)
            if (sibling === child) break
            if (sibling.javaClass == targetClass) count++
        }
        return count
    }
}
