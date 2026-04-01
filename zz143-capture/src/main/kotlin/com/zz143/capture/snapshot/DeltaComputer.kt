package com.zz143.capture.snapshot

import com.zz143.core.model.ElementId
import com.zz143.core.model.Mutation
import com.zz143.core.model.ViewNode

internal class DeltaComputer {

    fun compute(previous: ViewNode, current: ViewNode): List<Mutation> {
        val mutations = mutableListOf<Mutation>()
        val prevMap = buildElementMap(previous)
        val currMap = buildElementMap(current)

        // Pass 1: Removed nodes
        for ((id, _) in prevMap) {
            if (id !in currMap) {
                mutations.add(Mutation.NodeRemoved(id))
            }
        }

        // Pass 2: Added nodes
        for ((id, node) in currMap) {
            if (id !in prevMap) {
                val parentId = findParentId(current, node) ?: continue
                val childIndex = findChildIndex(current, node)
                mutations.add(Mutation.NodeAdded(parentId, childIndex, node))
            }
        }

        // Pass 3: Changed properties
        for ((id, currNode) in currMap) {
            val prevNode = prevMap[id] ?: continue

            // Skip subtree comparison if structural hash matches
            if (currNode.structuralHash == prevNode.structuralHash &&
                currNode.text == prevNode.text &&
                currNode.contentDescription == prevNode.contentDescription &&
                currNode.isVisible == prevNode.isVisible &&
                currNode.isEnabled == prevNode.isEnabled &&
                currNode.isFocused == prevNode.isFocused &&
                currNode.bounds == prevNode.bounds) {
                continue
            }

            diffProperties(prevNode, currNode).forEach { mutations.add(it) }
        }

        return mutations
    }

    fun shouldEmitFullSnapshot(mutations: List<Mutation>, totalNodes: Int): Boolean {
        return mutations.size > totalNodes * 0.6
    }

    private fun diffProperties(prev: ViewNode, curr: ViewNode): List<Mutation> {
        val diffs = mutableListOf<Mutation>()
        val id = curr.elementId

        if (prev.text != curr.text)
            diffs.add(Mutation.PropertyChanged(id, "text", prev.text, curr.text))
        if (prev.isVisible != curr.isVisible)
            diffs.add(Mutation.PropertyChanged(id, "isVisible", prev.isVisible.toString(), curr.isVisible.toString()))
        if (prev.isEnabled != curr.isEnabled)
            diffs.add(Mutation.PropertyChanged(id, "isEnabled", prev.isEnabled.toString(), curr.isEnabled.toString()))
        if (prev.isFocused != curr.isFocused)
            diffs.add(Mutation.PropertyChanged(id, "isFocused", prev.isFocused.toString(), curr.isFocused.toString()))
        if (prev.bounds != curr.bounds)
            diffs.add(Mutation.PropertyChanged(id, "bounds", prev.bounds.toString(), curr.bounds.toString()))
        if (prev.contentDescription != curr.contentDescription)
            diffs.add(Mutation.PropertyChanged(id, "contentDescription", prev.contentDescription, curr.contentDescription))

        return diffs
    }

    private fun buildElementMap(root: ViewNode): Map<ElementId, ViewNode> {
        val map = mutableMapOf<ElementId, ViewNode>()
        fun traverse(node: ViewNode) {
            map[node.elementId] = node
            node.children.forEach { traverse(it) }
        }
        traverse(root)
        return map
    }

    private fun findParentId(root: ViewNode, target: ViewNode): ElementId? {
        fun search(node: ViewNode): ElementId? {
            for (child in node.children) {
                if (child.elementId == target.elementId) return node.elementId
                search(child)?.let { return it }
            }
            return null
        }
        return search(root)
    }

    private fun findChildIndex(root: ViewNode, target: ViewNode): Int {
        fun search(node: ViewNode): Int? {
            for ((index, child) in node.children.withIndex()) {
                if (child.elementId == target.elementId) return index
                search(child)?.let { return it }
            }
            return null
        }
        return search(root) ?: 0
    }
}
