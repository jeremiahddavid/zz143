package com.zz143.capture.viewtree

import com.google.common.truth.Truth.assertThat
import com.zz143.capture.snapshot.DeltaComputer
import com.zz143.core.model.ElementId
import com.zz143.core.model.Mutation
import com.zz143.core.model.Rect
import com.zz143.core.testutil.ViewNodeBuilder
import com.zz143.core.testutil.viewNode
import org.junit.Before
import org.junit.Test

class DeltaComputerTest {

    private lateinit var deltaComputer: DeltaComputer

    @Before
    fun setUp() {
        deltaComputer = DeltaComputer()
    }

    // --- Identical trees produce no mutations ---

    @Test
    fun identicalSingleNodeTreesProduceNoMutations() {
        val tree = viewNode {
            elementId = ElementId("res:screen/root")
            className = "android.widget.FrameLayout"
            text = "Hello"
        }
        val mutations = deltaComputer.compute(tree, tree)
        assertThat(mutations).isEmpty()
    }

    @Test
    fun identicalTreesWithChildrenProduceNoMutations() {
        val tree = viewNode {
            elementId = ElementId("res:screen/root")
            className = "android.widget.LinearLayout"
            child {
                elementId = ElementId("res:screen/child1")
                className = "android.widget.TextView"
                text = "First"
            }
            child {
                elementId = ElementId("res:screen/child2")
                className = "android.widget.Button"
                text = "Click"
            }
        }
        val mutations = deltaComputer.compute(tree, tree)
        assertThat(mutations).isEmpty()
    }

    // --- NodeRemoved ---

    @Test
    fun removedChildNodeProducesNodeRemovedMutation() {
        val previous = viewNode {
            elementId = ElementId("res:screen/root")
            className = "android.widget.LinearLayout"
            child {
                elementId = ElementId("res:screen/child1")
                className = "android.widget.TextView"
            }
            child {
                elementId = ElementId("res:screen/child2")
                className = "android.widget.Button"
            }
        }
        val current = viewNode {
            elementId = ElementId("res:screen/root")
            className = "android.widget.LinearLayout"
            child {
                elementId = ElementId("res:screen/child1")
                className = "android.widget.TextView"
            }
            // child2 removed
        }

        val mutations = deltaComputer.compute(previous, current)

        val removals = mutations.filterIsInstance<Mutation.NodeRemoved>()
        assertThat(removals).hasSize(1)
        assertThat(removals[0].elementId).isEqualTo(ElementId("res:screen/child2"))
    }

    @Test
    fun removedNestedNodeProducesNodeRemovedMutation() {
        val previous = viewNode {
            elementId = ElementId("res:screen/root")
            className = "FrameLayout"
            child {
                elementId = ElementId("res:screen/parent")
                className = "LinearLayout"
                child {
                    elementId = ElementId("res:screen/grandchild")
                    className = "TextView"
                }
            }
        }
        val current = viewNode {
            elementId = ElementId("res:screen/root")
            className = "FrameLayout"
            child {
                elementId = ElementId("res:screen/parent")
                className = "LinearLayout"
                // grandchild removed
            }
        }

        val mutations = deltaComputer.compute(previous, current)

        val removals = mutations.filterIsInstance<Mutation.NodeRemoved>()
        assertThat(removals).hasSize(1)
        assertThat(removals[0].elementId).isEqualTo(ElementId("res:screen/grandchild"))
    }

    // --- NodeAdded ---

    @Test
    fun addedChildNodeProducesNodeAddedMutation() {
        val previous = viewNode {
            elementId = ElementId("res:screen/root")
            className = "android.widget.LinearLayout"
            child {
                elementId = ElementId("res:screen/child1")
                className = "android.widget.TextView"
            }
        }
        val current = viewNode {
            elementId = ElementId("res:screen/root")
            className = "android.widget.LinearLayout"
            child {
                elementId = ElementId("res:screen/child1")
                className = "android.widget.TextView"
            }
            child {
                elementId = ElementId("res:screen/child2")
                className = "android.widget.Button"
                text = "New"
            }
        }

        val mutations = deltaComputer.compute(previous, current)

        val additions = mutations.filterIsInstance<Mutation.NodeAdded>()
        assertThat(additions).hasSize(1)
        assertThat(additions[0].parentElementId).isEqualTo(ElementId("res:screen/root"))
        assertThat(additions[0].childIndex).isEqualTo(1)
        assertThat(additions[0].node.elementId).isEqualTo(ElementId("res:screen/child2"))
    }

    @Test
    fun addedNodeRecordsCorrectChildIndex() {
        val previous = viewNode {
            elementId = ElementId("res:screen/root")
            className = "LinearLayout"
        }
        val current = viewNode {
            elementId = ElementId("res:screen/root")
            className = "LinearLayout"
            child {
                elementId = ElementId("res:screen/new_child")
                className = "TextView"
            }
        }

        val mutations = deltaComputer.compute(previous, current)

        val additions = mutations.filterIsInstance<Mutation.NodeAdded>()
        assertThat(additions).hasSize(1)
        assertThat(additions[0].childIndex).isEqualTo(0)
    }

    // --- PropertyChanged ---

    @Test
    fun textChangeProducesPropertyChangedMutation() {
        val previous = viewNode {
            elementId = ElementId("res:screen/label")
            className = "android.widget.TextView"
            text = "Old text"
        }
        val current = viewNode {
            elementId = ElementId("res:screen/label")
            className = "android.widget.TextView"
            text = "New text"
        }

        val mutations = deltaComputer.compute(previous, current)

        val changes = mutations.filterIsInstance<Mutation.PropertyChanged>()
        assertThat(changes).isNotEmpty()
        val textChange = changes.find { it.property == "text" }
        assertThat(textChange).isNotNull()
        assertThat(textChange!!.oldValue).isEqualTo("Old text")
        assertThat(textChange.newValue).isEqualTo("New text")
    }

    @Test
    fun visibilityChangeProducesPropertyChangedMutation() {
        val previous = viewNode {
            elementId = ElementId("res:screen/view")
            className = "View"
            isVisible = true
        }
        val current = viewNode {
            elementId = ElementId("res:screen/view")
            className = "View"
            isVisible = false
        }

        val mutations = deltaComputer.compute(previous, current)

        val changes = mutations.filterIsInstance<Mutation.PropertyChanged>()
        val visibilityChange = changes.find { it.property == "isVisible" }
        assertThat(visibilityChange).isNotNull()
        assertThat(visibilityChange!!.oldValue).isEqualTo("true")
        assertThat(visibilityChange.newValue).isEqualTo("false")
    }

    @Test
    fun enabledChangeProducesPropertyChangedMutation() {
        val previous = viewNode {
            elementId = ElementId("res:screen/btn")
            className = "Button"
            isEnabled = true
        }
        val current = viewNode {
            elementId = ElementId("res:screen/btn")
            className = "Button"
            isEnabled = false
        }

        val mutations = deltaComputer.compute(previous, current)

        val changes = mutations.filterIsInstance<Mutation.PropertyChanged>()
        val enabledChange = changes.find { it.property == "isEnabled" }
        assertThat(enabledChange).isNotNull()
    }

    @Test
    fun focusChangeProducesPropertyChangedMutation() {
        val previous = viewNode {
            elementId = ElementId("res:screen/input")
            className = "EditText"
            isFocused = false
        }
        val current = viewNode {
            elementId = ElementId("res:screen/input")
            className = "EditText"
            isFocused = true
        }

        val mutations = deltaComputer.compute(previous, current)

        val changes = mutations.filterIsInstance<Mutation.PropertyChanged>()
        val focusChange = changes.find { it.property == "isFocused" }
        assertThat(focusChange).isNotNull()
    }

    @Test
    fun boundsChangeProducesPropertyChangedMutation() {
        val previous = viewNode {
            elementId = ElementId("res:screen/box")
            className = "View"
            bounds = Rect(0, 0, 100, 100)
        }
        val current = viewNode {
            elementId = ElementId("res:screen/box")
            className = "View"
            bounds = Rect(10, 10, 200, 200)
        }

        val mutations = deltaComputer.compute(previous, current)

        val changes = mutations.filterIsInstance<Mutation.PropertyChanged>()
        val boundsChange = changes.find { it.property == "bounds" }
        assertThat(boundsChange).isNotNull()
    }

    @Test
    fun contentDescriptionChangeProducesPropertyChangedMutation() {
        val previous = viewNode {
            elementId = ElementId("res:screen/img")
            className = "ImageView"
            contentDescription = "Old description"
        }
        val current = viewNode {
            elementId = ElementId("res:screen/img")
            className = "ImageView"
            contentDescription = "New description"
        }

        val mutations = deltaComputer.compute(previous, current)

        val changes = mutations.filterIsInstance<Mutation.PropertyChanged>()
        val descChange = changes.find { it.property == "contentDescription" }
        assertThat(descChange).isNotNull()
    }

    @Test
    fun multiplePropertyChangesProduceMultipleMutations() {
        val previous = viewNode {
            elementId = ElementId("res:screen/multi")
            className = "View"
            text = "old"
            isVisible = true
            isFocused = false
        }
        val current = viewNode {
            elementId = ElementId("res:screen/multi")
            className = "View"
            text = "new"
            isVisible = false
            isFocused = true
        }

        val mutations = deltaComputer.compute(previous, current)

        val changes = mutations.filterIsInstance<Mutation.PropertyChanged>()
        val changedProps = changes.map { it.property }.toSet()
        assertThat(changedProps).containsAtLeast("text", "isVisible", "isFocused")
    }

    // --- Mixed mutations ---

    @Test
    fun addAndRemoveInSameComputeProducesBothMutationTypes() {
        val previous = viewNode {
            elementId = ElementId("res:screen/root")
            className = "LinearLayout"
            child {
                elementId = ElementId("res:screen/old_child")
                className = "TextView"
            }
        }
        val current = viewNode {
            elementId = ElementId("res:screen/root")
            className = "LinearLayout"
            child {
                elementId = ElementId("res:screen/new_child")
                className = "Button"
            }
        }

        val mutations = deltaComputer.compute(previous, current)

        val removals = mutations.filterIsInstance<Mutation.NodeRemoved>()
        val additions = mutations.filterIsInstance<Mutation.NodeAdded>()
        assertThat(removals).hasSize(1)
        assertThat(removals[0].elementId).isEqualTo(ElementId("res:screen/old_child"))
        assertThat(additions).hasSize(1)
        assertThat(additions[0].node.elementId).isEqualTo(ElementId("res:screen/new_child"))
    }

    // --- shouldEmitFullSnapshot ---

    @Test
    fun shouldEmitFullSnapshotReturnsFalseWhenFewMutations() {
        val mutations = listOf(
            Mutation.PropertyChanged(ElementId("a"), "text", "x", "y")
        )
        val result = deltaComputer.shouldEmitFullSnapshot(mutations, totalNodes = 10)
        assertThat(result).isFalse()
    }

    @Test
    fun shouldEmitFullSnapshotReturnsTrueWhenManyMutations() {
        val mutations = (1..7).map {
            Mutation.PropertyChanged(ElementId("e$it"), "text", "old", "new")
        }
        // 7 mutations, 10 nodes => 7 > 10 * 0.6 = 6 => true
        val result = deltaComputer.shouldEmitFullSnapshot(mutations, totalNodes = 10)
        assertThat(result).isTrue()
    }

    @Test
    fun shouldEmitFullSnapshotReturnsFalseAtExactThreshold() {
        val mutations = (1..6).map {
            Mutation.PropertyChanged(ElementId("e$it"), "text", "old", "new")
        }
        // 6 mutations, 10 nodes => 6 > 6.0 is false
        val result = deltaComputer.shouldEmitFullSnapshot(mutations, totalNodes = 10)
        assertThat(result).isFalse()
    }

    @Test
    fun shouldEmitFullSnapshotReturnsTrueWhenAllNodesChanged() {
        val mutations = (1..10).map {
            Mutation.PropertyChanged(ElementId("e$it"), "text", "old", "new")
        }
        val result = deltaComputer.shouldEmitFullSnapshot(mutations, totalNodes = 10)
        assertThat(result).isTrue()
    }

    @Test
    fun shouldEmitFullSnapshotReturnsFalseForEmptyMutations() {
        val result = deltaComputer.shouldEmitFullSnapshot(emptyList(), totalNodes = 10)
        assertThat(result).isFalse()
    }
}
