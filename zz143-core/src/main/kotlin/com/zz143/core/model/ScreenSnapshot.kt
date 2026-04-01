package com.zz143.core.model

data class ScreenSnapshot(
    val snapshotId: String,
    val sessionId: SessionId,
    val screenId: ScreenId,
    val timestampMs: Long,
    val uptimeMs: Long,
    val rootNode: ViewNode,
    val screenWidth: Int,
    val screenHeight: Int,
    val orientation: Int,
    val isFullSnapshot: Boolean
)

data class IncrementalDelta(
    val deltaId: String,
    val baseSnapshotId: String,
    val sessionId: SessionId,
    val timestampMs: Long,
    val mutations: List<Mutation>
)

sealed class Mutation {
    data class NodeAdded(
        val parentElementId: ElementId,
        val childIndex: Int,
        val node: ViewNode
    ) : Mutation()

    data class NodeRemoved(
        val elementId: ElementId
    ) : Mutation()

    data class PropertyChanged(
        val elementId: ElementId,
        val property: String,
        val oldValue: String?,
        val newValue: String?
    ) : Mutation()

    data class NodeMoved(
        val elementId: ElementId,
        val newParentElementId: ElementId,
        val newChildIndex: Int
    ) : Mutation()
}
