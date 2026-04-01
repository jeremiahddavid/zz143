Now I have a comprehensive understanding of the environment and existing patterns. Here is the complete technical design document.

---

# WatchLearn SDK -- Complete Technical Design Document

## 1. Full Project Structure

```
watchlearn-sdk/
├── settings.gradle.kts
├── build.gradle.kts                          # Root build with version catalog, shared config
├── gradle.properties                         # Shared Gradle properties
├── gradle/
│   ├── wrapper/
│   │   ├── gradle-wrapper.jar
│   │   └── gradle-wrapper.properties
│   └── libs.versions.toml                    # Gradle version catalog
├── gradlew
├── gradlew.bat
├── buildSrc/
│   ├── build.gradle.kts                      # Convention plugin build
│   └── src/main/kotlin/
│       ├── watchlearn.android-library.gradle.kts   # Shared Android library config
│       └── watchlearn.publishing.gradle.kts        # Maven Central publishing config
├── LICENSE                                    # Apache 2.0
├── README.md
├── CHANGELOG.md
├── CONTRIBUTING.md
│
├── watchlearn-core/
│   ├── build.gradle.kts
│   ├── consumer-rules.pro
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   └── kotlin/com/watchlearn/core/
│       │       ├── WatchLearn.kt                         # Singleton SDK entry point
│       │       ├── WatchLearnConfig.kt                   # Configuration DSL
│       │       ├── model/
│       │       │   ├── WatchLearnEvent.kt                # Sealed class hierarchy for all events
│       │       │   ├── ViewNode.kt                       # Wireframe view tree node
│       │       │   ├── ScreenSnapshot.kt                 # Full snapshot model
│       │       │   ├── IncrementalDelta.kt               # Delta between snapshots
│       │       │   ├── GestureEvent.kt                   # Touch/gesture model
│       │       │   ├── NavigationEvent.kt                # Screen transition model
│       │       │   ├── TextInputEvent.kt                 # Text field change model
│       │       │   ├── SemanticAction.kt                 # High-level user action
│       │       │   ├── Workflow.kt                       # Learned workflow model
│       │       │   ├── WorkflowStep.kt                   # Single step within a workflow
│       │       │   ├── Suggestion.kt                     # Suggestion to display
│       │       │   ├── ReplayResult.kt                   # Result of executing a workflow
│       │       │   ├── ElementId.kt                      # Stable element identifier (value class)
│       │       │   ├── ScreenId.kt                       # Stable screen identifier (value class)
│       │       │   └── SessionId.kt                      # Session identifier (value class)
│       │       ├── event/
│       │       │   ├── EventBus.kt                       # Internal SharedFlow-based event bus
│       │       │   ├── EventEncoder.kt                   # Binary tuple encoder
│       │       │   ├── EventDecoder.kt                   # Binary tuple decoder
│       │       │   ├── EventBatch.kt                     # Batched events for persistence
│       │       │   └── EventFilter.kt                    # Configurable event filtering
│       │       ├── storage/
│       │       │   ├── StorageEngine.kt                  # Interface for storage backends
│       │       │   ├── SqliteStorageEngine.kt            # SQLite-backed storage
│       │       │   ├── FileQueue.kt                      # File-backed crash-safe queue
│       │       │   ├── FileQueueSegment.kt               # Single segment of the file queue
│       │       │   ├── DatabaseSchema.kt                 # SQLite table/index definitions
│       │       │   └── Migration.kt                      # Database migration helpers
│       │       ├── threading/
│       │       │   ├── WatchLearnDispatchers.kt          # Named dispatchers for SDK threads
│       │       │   └── CoroutineScopeProvider.kt         # Scoped coroutine lifecycle
│       │       ├── clock/
│       │       │   ├── Clock.kt                          # Interface for time (testability)
│       │       │   └── SystemClock.kt                    # Real implementation
│       │       ├── id/
│       │       │   ├── IdGenerator.kt                    # Interface for ID generation
│       │       │   └── UlidGenerator.kt                  # ULID-based ID generation
│       │       └── util/
│       │           ├── RingBuffer.kt                     # Fixed-size circular buffer
│       │           ├── Debouncer.kt                      # Debounce utility
│       │           ├── Checksums.kt                      # CRC32 for file queue integrity
│       │           └── Extensions.kt                     # Kotlin extension functions
│       └── test/
│           └── kotlin/com/watchlearn/core/
│               ├── event/
│               │   ├── EventBusTest.kt
│               │   ├── EventEncoderTest.kt
│               │   └── EventDecoderTest.kt
│               ├── storage/
│               │   ├── SqliteStorageEngineTest.kt
│               │   ├── FileQueueTest.kt
│               │   └── FileQueueCrashRecoveryTest.kt
│               ├── model/
│               │   └── ViewNodeTest.kt
│               └── util/
│                   ├── RingBufferTest.kt
│                   └── DebouncerTest.kt
│
├── watchlearn-capture/
│   ├── build.gradle.kts
│   ├── consumer-rules.pro
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   └── kotlin/com/watchlearn/capture/
│       │       ├── CaptureEngine.kt                      # Orchestrates all capture subsystems
│       │       ├── viewtree/
│       │       │   ├── ViewTreeWalker.kt                 # Recursive view tree traversal
│       │       │   ├── ViewNodeMapper.kt                 # Maps Android View -> ViewNode
│       │       │   ├── ComposeNodeMapper.kt              # Maps Compose semantics -> ViewNode
│       │       │   ├── ElementIdStrategy.kt              # Interface for element ID generation
│       │       │   ├── StableIdGenerator.kt              # Default stable ID generation
│       │       │   ├── ViewTreeHasher.kt                 # Structural hash of view trees
│       │       │   └── ViewPropertyExtractor.kt          # Extracts relevant view properties
│       │       ├── snapshot/
│       │       │   ├── SnapshotScheduler.kt              # Throttled snapshot scheduling
│       │       │   ├── FullSnapshotCapture.kt            # Complete wireframe capture
│       │       │   ├── DeltaComputer.kt                  # Tree diff algorithm
│       │       │   └── SnapshotPool.kt                   # Object pool for snapshot allocation
│       │       ├── gesture/
│       │       │   ├── GestureInterceptor.kt             # Window.Callback delegate for touches
│       │       │   ├── GestureClassifier.kt              # Classifies raw touch -> gesture type
│       │       │   └── ScrollTracker.kt                  # Tracks scroll position changes
│       │       ├── navigation/
│       │       │   ├── NavigationTracker.kt              # Activity/Fragment lifecycle tracking
│       │       │   ├── NavControllerObserver.kt          # Jetpack Navigation observer
│       │       │   └── ScreenIdentifier.kt               # Derives stable screen ID from route
│       │       ├── text/
│       │       │   ├── TextChangeTracker.kt              # Monitors EditText changes
│       │       │   └── InputRedactor.kt                  # Redacts sensitive input
│       │       └── sensitivity/
│       │           ├── SensitivityClassifier.kt          # Classifies views as sensitive
│       │           └── RedactionConfig.kt                # Developer-specified redaction rules
│       └── test/
│           └── kotlin/com/watchlearn/capture/
│               ├── viewtree/
│               │   ├── ViewTreeWalkerTest.kt
│               │   ├── StableIdGeneratorTest.kt
│               │   └── DeltaComputerTest.kt
│               ├── gesture/
│               │   └── GestureClassifierTest.kt
│               └── navigation/
│                   └── NavigationTrackerTest.kt
│
├── watchlearn-learn/
│   ├── build.gradle.kts
│   ├── consumer-rules.pro
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   └── kotlin/com/watchlearn/learn/
│       │       ├── PatternEngine.kt                      # Top-level pattern detection orchestrator
│       │       ├── sequence/
│       │       │   ├── ActionSequence.kt                 # Ordered list of SemanticActions
│       │       │   ├── NGramExtractor.kt                 # Extracts n-grams from action streams
│       │       │   ├── SequenceMatcher.kt                # Finds recurring subsequences
│       │       │   ├── SequenceAligner.kt                # Smith-Waterman alignment for fuzzy match
│       │       │   └── SequenceIndex.kt                  # Suffix array index for fast lookup
│       │       ├── frequency/
│       │       │   ├── FrequencyAnalyzer.kt              # Counts action/sequence occurrences
│       │       │   ├── TimeWindowAggregator.kt           # Aggregates events by time windows
│       │       │   └── DecayFunction.kt                  # Exponential decay for recency weighting
│       │       ├── temporal/
│       │       │   ├── TemporalPatternDetector.kt        # Detects time-based patterns
│       │       │   ├── PeriodicityEstimator.kt           # Estimates periodicity (daily/weekly/etc.)
│       │       │   ├── TimeSlot.kt                       # Discretized time bucket
│       │       │   └── CircularTimeHistogram.kt          # Circular histogram for recurring times
│       │       ├── context/
│       │       │   ├── ContextVector.kt                  # Feature vector for context matching
│       │       │   ├── ContextExtractor.kt               # Extracts context from event stream
│       │       │   └── ContextSimilarity.kt              # Cosine similarity for context vectors
│       │       ├── scoring/
│       │       │   ├── ConfidenceScorer.kt               # Computes overall confidence score
│       │       │   ├── ScoringWeights.kt                 # Configurable weights per signal
│       │       │   └── ConfidenceThresholds.kt           # Min thresholds for suggestions
│       │       ├── workflow/
│       │       │   ├── WorkflowExtractor.kt              # Extracts Workflow from sequences
│       │       │   ├── WorkflowMerger.kt                 # Merges similar workflows
│       │       │   ├── WorkflowParameterizer.kt          # Identifies variable vs fixed params
│       │       │   └── WorkflowPruner.kt                 # Prunes stale/unused workflows
│       │       └── ml/
│       │           ├── MLPatternDetector.kt              # Future: TFLite pattern detector
│       │           └── FeatureExtractor.kt               # Feature extraction for ML input
│       └── test/
│           └── kotlin/com/watchlearn/learn/
│               ├── sequence/
│               │   ├── NGramExtractorTest.kt
│               │   ├── SequenceMatcherTest.kt
│               │   └── SequenceAlignerTest.kt
│               ├── frequency/
│               │   ├── FrequencyAnalyzerTest.kt
│               │   └── TimeWindowAggregatorTest.kt
│               ├── temporal/
│               │   ├── TemporalPatternDetectorTest.kt
│               │   └── PeriodicityEstimatorTest.kt
│               ├── scoring/
│               │   └── ConfidenceScorerTest.kt
│               └── workflow/
│                   ├── WorkflowExtractorTest.kt
│                   └── WorkflowParameterizerTest.kt
│
├── watchlearn-suggest/
│   ├── build.gradle.kts
│   ├── consumer-rules.pro
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── res/
│       │   │   ├── layout/
│       │   │   │   ├── watchlearn_suggestion_bottom_sheet.xml
│       │   │   │   ├── watchlearn_suggestion_inline.xml
│       │   │   │   └── watchlearn_suggestion_item.xml
│       │   │   ├── drawable/
│       │   │   │   ├── watchlearn_ic_automation.xml
│       │   │   │   ├── watchlearn_bg_suggestion.xml
│       │   │   │   └── watchlearn_ripple.xml
│       │   │   ├── anim/
│       │   │   │   ├── watchlearn_slide_up.xml
│       │   │   │   └── watchlearn_fade_out.xml
│       │   │   └── values/
│       │   │       ├── watchlearn_strings.xml
│       │   │       ├── watchlearn_colors.xml
│       │   │       ├── watchlearn_dimens.xml
│       │   │       └── watchlearn_styles.xml
│       │   └── kotlin/com/watchlearn/suggest/
│       │       ├── SuggestionEngine.kt                   # Decides when to show suggestions
│       │       ├── ui/
│       │       │   ├── SuggestionBottomSheet.kt          # Bottom sheet UI
│       │       │   ├── SuggestionNotificationBuilder.kt  # Notification-based suggestion
│       │       │   ├── SuggestionInlineView.kt           # Inline banner view
│       │       │   └── SuggestionAdapter.kt              # RecyclerView adapter for listing
│       │       ├── compose/
│       │       │   ├── SuggestionBottomSheetCompose.kt   # Compose bottom sheet
│       │       │   └── SuggestionBannerCompose.kt        # Compose inline banner
│       │       ├── throttle/
│       │       │   ├── SuggestionThrottler.kt            # Rate-limiting suggestion display
│       │       │   └── CooldownTracker.kt                # Per-workflow cooldowns
│       │       ├── preference/
│       │       │   ├── UserPreferenceStore.kt            # Tracks accepted/dismissed prefs
│       │       │   └── PreferenceSignals.kt              # User's implicit preferences
│       │       └── trigger/
│       │           ├── SuggestionTrigger.kt              # Interface for trigger conditions
│       │           ├── ContextTrigger.kt                 # Triggers on matching context
│       │           └── TimeTrigger.kt                    # Triggers at learned times
│       └── test/
│           └── kotlin/com/watchlearn/suggest/
│               ├── SuggestionEngineTest.kt
│               ├── throttle/
│               │   └── SuggestionThrottlerTest.kt
│               └── preference/
│                   └── UserPreferenceStoreTest.kt
│
├── watchlearn-replay/
│   ├── build.gradle.kts
│   ├── consumer-rules.pro
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   └── kotlin/com/watchlearn/replay/
│       │       ├── ReplayEngine.kt                       # Top-level replay orchestrator
│       │       ├── registry/
│       │       │   ├── ActionRegistry.kt                 # Registry of @WatchAction annotated methods
│       │       │   ├── ActionDescriptor.kt               # Metadata about a registered action
│       │       │   ├── ActionParameter.kt                # Parameter descriptor for an action
│       │       │   └── RegistryScanner.kt                # Scans for @WatchAction at init time
│       │       ├── execution/
│       │       │   ├── ExecutionPlan.kt                  # Ordered execution plan
│       │       │   ├── StepExecutor.kt                   # Executes a single workflow step
│       │       │   ├── ParameterResolver.kt              # Resolves parameters from context
│       │       │   └── ExecutionContext.kt                # Mutable context for a replay run
│       │       ├── strategy/
│       │       │   ├── ReplayStrategy.kt                 # Interface for replay approaches
│       │       │   ├── DirectInvocationStrategy.kt       # Calls @WatchAction methods directly
│       │       │   ├── AccessibilityStrategy.kt          # Uses AccessibilityService for replay
│       │       │   ├── IntentStrategy.kt                 # Uses Intents/deep links for navigation
│       │       │   └── StrategySelector.kt               # Selects best strategy per step
│       │       ├── state/
│       │       │   ├── ReplayStateMachine.kt             # State machine for replay lifecycle
│       │       │   ├── ReplayState.kt                    # States: Idle, Running, Paused, etc.
│       │       │   └── StateTransition.kt                # Valid state transitions
│       │       ├── verification/
│       │       │   ├── PostStepVerifier.kt               # Verifies expected state after step
│       │       │   ├── ViewTreeMatcher.kt                # Matches current view tree to expected
│       │       │   └── VerificationResult.kt             # Pass/fail/partial result
│       │       ├── error/
│       │       │   ├── ReplayErrorHandler.kt             # Centralized error handling
│       │       │   ├── RetryPolicy.kt                    # Configurable retry policy
│       │       │   ├── RecoveryStrategy.kt               # Skip, retry, abort, rollback
│       │       │   └── ReplayException.kt                # Typed exceptions for replay failures
│       │       └── annotation/
│       │           ├── WatchAction.kt                    # @WatchAction annotation
│       │           ├── WatchParam.kt                     # @WatchParam annotation for parameters
│       │           └── WatchGuard.kt                     # @WatchGuard precondition annotation
│       └── test/
│           └── kotlin/com/watchlearn/replay/
│               ├── registry/
│               │   ├── ActionRegistryTest.kt
│               │   └── RegistryScannerTest.kt
│               ├── execution/
│               │   ├── StepExecutorTest.kt
│               │   └── ParameterResolverTest.kt
│               ├── state/
│               │   └── ReplayStateMachineTest.kt
│               └── strategy/
│                   └── StrategySelectorTest.kt
│
├── watchlearn-android/
│   ├── build.gradle.kts
│   ├── consumer-rules.pro
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   └── kotlin/com/watchlearn/android/
│       │       ├── WatchLearnInitializer.kt              # App Startup ContentProvider init
│       │       ├── lifecycle/
│       │       │   ├── ActivityLifecycleTracker.kt       # Application.ActivityLifecycleCallbacks
│       │       │   ├── FragmentLifecycleTracker.kt       # FragmentManager.FragmentLifecycleCallbacks
│       │       │   ├── ProcessLifecycleTracker.kt        # ProcessLifecycleOwner observer
│       │       │   └── SessionManager.kt                 # Session start/end detection
│       │       ├── compose/
│       │       │   ├── WatchLearnComposable.kt           # CompositionLocal for WatchLearn
│       │       │   ├── ComposeSnapshotInterceptor.kt     # Hooks into Compose snapshot system
│       │       │   ├── SemanticsTreeReader.kt            # Reads Compose semantics tree
│       │       │   └── Modifiers.kt                      # Modifier.watchAction() extension
│       │       ├── accessibility/
│       │       │   ├── WatchLearnAccessibilityService.kt # Accessibility service for replay
│       │       │   └── AccessibilityPermissionHelper.kt  # Permission request helpers
│       │       └── notification/
│       │           └── NotificationChannelSetup.kt       # Creates notification channel
│       └── test/
│           └── kotlin/com/watchlearn/android/
│               ├── lifecycle/
│               │   ├── ActivityLifecycleTrackerTest.kt
│               │   └── SessionManagerTest.kt
│               └── compose/
│                   └── SemanticsTreeReaderTest.kt
│
└── demo-app/
    ├── build.gradle.kts
    └── src/
        └── main/
            ├── AndroidManifest.xml
            ├── res/
            │   ├── layout/
            │   │   ├── activity_main.xml
            │   │   ├── fragment_product_list.xml
            │   │   ├── fragment_product_detail.xml
            │   │   ├── fragment_cart.xml
            │   │   ├── fragment_checkout.xml
            │   │   └── item_product.xml
            │   ├── navigation/
            │   │   └── nav_graph.xml
            │   └── values/
            │       ├── strings.xml
            │       ├── colors.xml
            │       └── themes.xml
            └── kotlin/com/watchlearn/demo/
                ├── DemoApplication.kt                    # WatchLearn.init() here
                ├── MainActivity.kt
                ├── ui/
                │   ├── ProductListFragment.kt
                │   ├── ProductDetailFragment.kt
                │   ├── CartFragment.kt
                │   └── CheckoutFragment.kt
                ├── data/
                │   ├── Product.kt
                │   ├── CartItem.kt
                │   └── SampleData.kt
                └── actions/
                    └── DemoActions.kt                    # @WatchAction annotated methods
```

---

## 2. All Data Models with Exact Field Types

### 2.1 Core Identifiers

```kotlin
// ElementId.kt
@JvmInline
value class ElementId(val value: String) {
    companion object {
        fun from(className: String, resourceId: String?, contentDesc: String?,
                 xpath: String, screenId: ScreenId): ElementId
    }
}

// ScreenId.kt
@JvmInline
value class ScreenId(val value: String) {
    companion object {
        fun fromActivity(activityName: String): ScreenId
        fun fromFragment(activityName: String, fragmentTag: String): ScreenId
        fun fromRoute(route: String): ScreenId
    }
}

// SessionId.kt
@JvmInline
value class SessionId(val value: String) // ULID
```

### 2.2 View Tree Model

```kotlin
// ViewNode.kt
data class ViewNode(
    val elementId: ElementId,
    val className: String,             // e.g., "android.widget.Button"
    val resourceIdName: String?,       // e.g., "btn_checkout" (null if not set)
    val contentDescription: String?,
    val text: String?,                 // Redacted if sensitive
    val isVisible: Boolean,
    val isEnabled: Boolean,
    val isClickable: Boolean,
    val isFocused: Boolean,
    val isEditable: Boolean,
    val bounds: Rect,                  // Absolute screen coordinates
    val relativePosition: RelativePosition, // Position within parent
    val scrollOffset: ScrollOffset?,   // null if not scrollable
    val treeDepth: Int,                // Depth from root
    val childIndex: Int,               // Index within parent
    val children: List<ViewNode>,
    val extraProperties: Map<String, String>, // Extensible properties
    val isSensitive: Boolean,          // Password fields, etc.
    val testTag: String?               // Compose test tag
)

data class Rect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

data class RelativePosition(
    val xFraction: Float,  // 0.0..1.0 within parent
    val yFraction: Float
)

data class ScrollOffset(
    val scrollX: Int,
    val scrollY: Int,
    val maxScrollX: Int,
    val maxScrollY: Int
)
```

### 2.3 Snapshot and Delta Models

```kotlin
// ScreenSnapshot.kt
data class ScreenSnapshot(
    val snapshotId: String,            // ULID
    val sessionId: SessionId,
    val screenId: ScreenId,
    val timestampMs: Long,             // System.currentTimeMillis()
    val uptimeMs: Long,                // SystemClock.elapsedRealtime()
    val rootNode: ViewNode,
    val screenWidth: Int,
    val screenHeight: Int,
    val orientation: Int,              // Configuration.ORIENTATION_*
    val isFullSnapshot: Boolean        // true for first capture of a screen
)

// IncrementalDelta.kt
data class IncrementalDelta(
    val deltaId: String,               // ULID
    val baseSnapshotId: String,        // Reference to the ScreenSnapshot
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
        val property: String,          // e.g., "text", "isVisible", "bounds"
        val oldValue: String?,
        val newValue: String?
    ) : Mutation()

    data class NodeMoved(
        val elementId: ElementId,
        val newParentElementId: ElementId,
        val newChildIndex: Int
    ) : Mutation()

    data class ScrollChanged(
        val elementId: ElementId,
        val scrollOffset: ScrollOffset
    ) : Mutation()
}
```

### 2.4 Event Models

```kotlin
// WatchLearnEvent.kt
sealed class WatchLearnEvent {
    abstract val eventId: String          // ULID
    abstract val sessionId: SessionId
    abstract val timestampMs: Long
    abstract val uptimeMs: Long
    abstract val screenId: ScreenId

    data class Snapshot(
        override val eventId: String,
        override val sessionId: SessionId,
        override val timestampMs: Long,
        override val uptimeMs: Long,
        override val screenId: ScreenId,
        val snapshot: ScreenSnapshot
    ) : WatchLearnEvent()

    data class Delta(
        override val eventId: String,
        override val sessionId: SessionId,
        override val timestampMs: Long,
        override val uptimeMs: Long,
        override val screenId: ScreenId,
        val delta: IncrementalDelta
    ) : WatchLearnEvent()

    data class Gesture(
        override val eventId: String,
        override val sessionId: SessionId,
        override val timestampMs: Long,
        override val uptimeMs: Long,
        override val screenId: ScreenId,
        val gesture: GestureEvent
    ) : WatchLearnEvent()

    data class Navigation(
        override val eventId: String,
        override val sessionId: SessionId,
        override val timestampMs: Long,
        override val uptimeMs: Long,
        override val screenId: ScreenId,
        val navigation: NavigationEvent
    ) : WatchLearnEvent()

    data class TextInput(
        override val eventId: String,
        override val sessionId: SessionId,
        override val timestampMs: Long,
        override val uptimeMs: Long,
        override val screenId: ScreenId,
        val input: TextInputEvent
    ) : WatchLearnEvent()

    data class Action(
        override val eventId: String,
        override val sessionId: SessionId,
        override val timestampMs: Long,
        override val uptimeMs: Long,
        override val screenId: ScreenId,
        val action: SemanticAction
    ) : WatchLearnEvent()

    data class Lifecycle(
        override val eventId: String,
        override val sessionId: SessionId,
        override val timestampMs: Long,
        override val uptimeMs: Long,
        override val screenId: ScreenId,
        val type: LifecycleType,  // SESSION_START, SESSION_END, APP_BACKGROUNDED, APP_FOREGROUNDED
        val metadata: Map<String, String>
    ) : WatchLearnEvent()
}

// GestureEvent.kt
data class GestureEvent(
    val type: GestureType,
    val targetElementId: ElementId?,     // null if gesture hits no identifiable view
    val startX: Float,
    val startY: Float,
    val endX: Float?,                    // null for tap
    val endY: Float?,
    val durationMs: Long,
    val pressure: Float,
    val pointerCount: Int
)

enum class GestureType {
    TAP, DOUBLE_TAP, LONG_PRESS,
    SWIPE_UP, SWIPE_DOWN, SWIPE_LEFT, SWIPE_RIGHT,
    SCROLL, FLING, PINCH, CUSTOM
}

// NavigationEvent.kt
data class NavigationEvent(
    val type: NavigationType,
    val fromScreenId: ScreenId?,
    val toScreenId: ScreenId,
    val route: String?,                   // Deep link or nav route
    val transitionType: String?,          // "push", "pop", "replace"
    val arguments: Map<String, String>    // Redacted nav arguments
)

enum class NavigationType {
    ACTIVITY_CREATED, ACTIVITY_DESTROYED,
    FRAGMENT_ATTACHED, FRAGMENT_DETACHED,
    COMPOSE_NAVIGATION, DEEP_LINK, BACK_PRESS
}

// TextInputEvent.kt
data class TextInputEvent(
    val targetElementId: ElementId,
    val fieldType: TextFieldType,
    val isRedacted: Boolean,
    val textLength: Int,                  // Length even if redacted
    val hashedValue: String?              // SHA-256 hash for matching, null if sensitive
)

enum class TextFieldType {
    PLAIN, EMAIL, PASSWORD, PHONE, NUMBER, URL, SEARCH, MULTILINE
}

// SemanticAction.kt
data class SemanticAction(
    val actionType: String,              // "add_to_cart", "checkout", "search", etc.
    val actionSource: ActionSource,      // ANNOTATION, INFERRED, GESTURE
    val targetElementId: ElementId?,
    val parameters: Map<String, String>, // Key-value pairs describing the action
    val preconditions: Map<String, String>, // Expected state before action
    val postconditions: Map<String, String> // Expected state after action
)

enum class ActionSource {
    ANNOTATION,    // From @WatchAction
    INFERRED,      // Inferred from gesture + context
    GESTURE        // Raw gesture elevated to action
}
```

### 2.5 Workflow and Suggestion Models

```kotlin
// Workflow.kt
data class Workflow(
    val workflowId: String,            // ULID
    val name: String,                  // Human-readable generated name
    val description: String,
    val steps: List<WorkflowStep>,
    val triggerContext: ContextVector,  // When this workflow typically starts
    val frequency: WorkflowFrequency,
    val confidenceScore: Float,        // 0.0..1.0
    val firstSeenMs: Long,
    val lastSeenMs: Long,
    val executionCount: Int,           // Times user performed manually
    val automationCount: Int,          // Times SDK executed automatically
    val successRate: Float,            // Success rate of automated executions
    val status: WorkflowStatus,
    val version: Int                   // Incremented when workflow is updated
)

enum class WorkflowStatus {
    DETECTED,      // Pattern detected, not yet suggested
    SUGGESTED,     // Suggestion shown to user
    ACCEPTED,      // User accepted automation
    REJECTED,      // User rejected suggestion
    ACTIVE,        // Currently being automated
    PAUSED,        // User paused automation
    DEPRECATED     // Pattern no longer occurring
}

data class WorkflowFrequency(
    val type: FrequencyType,
    val intervalMs: Long?,             // Estimated interval between occurrences
    val dayOfWeek: Int?,               // 1=Monday..7=Sunday
    val hourOfDay: Int?,               // 0..23
    val minuteOfHour: Int?,
    val confidence: Float              // Confidence in frequency estimate
)

enum class FrequencyType {
    MULTIPLE_DAILY, DAILY, WEEKLY, BIWEEKLY, MONTHLY, IRREGULAR, ON_DEMAND
}

// WorkflowStep.kt
data class WorkflowStep(
    val stepIndex: Int,
    val action: SemanticAction,
    val expectedScreenId: ScreenId,
    val replayStrategy: ReplayStrategyType,
    val parameters: List<StepParameter>,
    val timeoutMs: Long,               // Max wait for this step
    val isOptional: Boolean,           // Can be skipped without failing
    val retryPolicy: RetryPolicy,
    val verificationCriteria: VerificationCriteria?
)

data class StepParameter(
    val name: String,
    val type: ParameterType,           // STRING, INT, FLOAT, BOOLEAN, ELEMENT_REF
    val isVariable: Boolean,           // true if value changes between executions
    val defaultValue: String?,
    val sourceExpression: String?      // How to resolve this value at runtime
)

enum class ParameterType {
    STRING, INT, FLOAT, BOOLEAN, ELEMENT_REF, SCREEN_REF, TIMESTAMP
}

data class VerificationCriteria(
    val expectedScreenId: ScreenId?,
    val expectedElements: List<ElementId>,
    val expectedProperties: Map<ElementId, Map<String, String>>,
    val timeoutMs: Long
)

// Suggestion.kt
data class Suggestion(
    val suggestionId: String,          // ULID
    val workflow: Workflow,
    val displayType: SuggestionDisplayType,
    val title: String,
    val description: String,
    val estimatedTimeSavedMs: Long,
    val createdAtMs: Long,
    val expiresAtMs: Long,            // Suggestion auto-expires
    val priority: Int                  // Lower = higher priority
)

enum class SuggestionDisplayType {
    BOTTOM_SHEET, NOTIFICATION, INLINE_BANNER
}

// ReplayResult.kt
data class ReplayResult(
    val workflowId: String,
    val executionId: String,           // ULID
    val startedAtMs: Long,
    val completedAtMs: Long,
    val status: ReplayStatus,
    val stepsCompleted: Int,
    val totalSteps: Int,
    val failedStepIndex: Int?,
    val error: ReplayError?,
    val stateChanges: List<StateChange>
)

enum class ReplayStatus {
    SUCCESS, PARTIAL_SUCCESS, FAILED, ABORTED_BY_USER, TIMED_OUT
}

data class ReplayError(
    val type: ReplayErrorType,
    val message: String,
    val stepIndex: Int,
    val elementId: ElementId?,
    val expectedState: String?,
    val actualState: String?
)

enum class ReplayErrorType {
    ELEMENT_NOT_FOUND, SCREEN_MISMATCH, ACTION_FAILED,
    TIMEOUT, PRECONDITION_FAILED, PERMISSION_DENIED,
    APP_CRASHED, NETWORK_ERROR, UNKNOWN
}

data class StateChange(
    val stepIndex: Int,
    val screenId: ScreenId,
    val changedElements: List<ElementId>,
    val timestampMs: Long
)
```

---

## 3. The Event System

### 3.1 Event Emission

Events flow through a SharedFlow-based internal bus. Every capture subsystem (view tree walker, gesture interceptor, navigation tracker) emits events into the bus. The bus is the single point of collection.

```kotlin
// EventBus.kt
internal class EventBus(
    private val config: WatchLearnConfig,
    private val dispatchers: WatchLearnDispatchers
) {
    // Hot flow -- events are never replayed to late collectors
    private val _events = MutableSharedFlow<WatchLearnEvent>(
        replay = 0,
        extraBufferCapacity = 512,      // Buffer 512 events before suspending
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<WatchLearnEvent> = _events.asSharedFlow()

    suspend fun emit(event: WatchLearnEvent) {
        if (config.eventFilter.shouldInclude(event)) {
            _events.emit(event)
        }
    }

    // Non-suspending version for Java interop / callbacks
    fun tryEmit(event: WatchLearnEvent): Boolean {
        if (config.eventFilter.shouldInclude(event)) {
            return _events.tryEmit(event)
        }
        return false
    }
}
```

### 3.2 Event Batching

A coroutine collects events from the bus and batches them before encoding and persisting. Batching reduces I/O operations.

```kotlin
// EventBatch.kt -- Collector that batches events
internal class EventBatchCollector(
    private val eventBus: EventBus,
    private val encoder: EventEncoder,
    private val storageEngine: StorageEngine,
    private val config: WatchLearnConfig,
    private val scope: CoroutineScope,
    private val dispatchers: WatchLearnDispatchers
) {
    private val maxBatchSize: Int = config.batchSize        // default 50
    private val maxBatchWindowMs: Long = config.batchWindowMs // default 5000ms

    fun start() {
        scope.launch(dispatchers.io) {
            eventBus.events
                .chunkedByTimeOrSize(maxBatchWindowMs, maxBatchSize)
                .collect { batch ->
                    persistBatch(batch)
                }
        }
    }

    private suspend fun persistBatch(events: List<WatchLearnEvent>) {
        val encoded = events.map { encoder.encode(it) }
        val batchPayload = EventBatchPayload(
            batchId = UlidGenerator.next(),
            eventCount = events.size,
            firstTimestampMs = events.first().timestampMs,
            lastTimestampMs = events.last().timestampMs,
            encodedEvents = encoded,
            checksum = Checksums.crc32(encoded)
        )
        storageEngine.writeBatch(batchPayload)
    }
}
```

The `chunkedByTimeOrSize` is a custom Flow operator that emits a list when either the size threshold is reached or the time window elapses, whichever comes first. This is implemented using `channelFlow` with a timer coroutine.

### 3.3 Binary Tuple Encoding

Events are encoded as binary tuples inspired by OpenReplay/Clarity. Each event is a sequence of (tag, length, value) tuples where the tag is a single byte identifying the field type.

```kotlin
// EventEncoder.kt
internal class EventEncoder {

    // Tag constants
    companion object {
        const val TAG_EVENT_TYPE: Byte = 0x01
        const val TAG_EVENT_ID: Byte = 0x02
        const val TAG_SESSION_ID: Byte = 0x03
        const val TAG_TIMESTAMP: Byte = 0x04
        const val TAG_SCREEN_ID: Byte = 0x05
        const val TAG_ELEMENT_ID: Byte = 0x06
        const val TAG_GESTURE_TYPE: Byte = 0x07
        const val TAG_COORDINATES: Byte = 0x08
        const val TAG_TEXT_HASH: Byte = 0x09
        const val TAG_SNAPSHOT_ROOT: Byte = 0x0A
        const val TAG_MUTATION: Byte = 0x0B
        const val TAG_NAV_TYPE: Byte = 0x0C
        const val TAG_PROPERTY: Byte = 0x0D
        const val TAG_PARAM: Byte = 0x0E
        const val TAG_END: Byte = 0x7F
        // ... more tags up to 0x7E
    }

    fun encode(event: WatchLearnEvent): ByteArray {
        val buffer = ByteArrayOutputStream(256)
        val out = DataOutputStream(buffer)

        // Write event type tag
        out.writeByte(TAG_EVENT_TYPE.toInt())
        out.writeByte(eventTypeOrdinal(event))

        // Write common fields
        writeString(out, TAG_EVENT_ID, event.eventId)
        writeString(out, TAG_SESSION_ID, event.sessionId.value)
        writeLong(out, TAG_TIMESTAMP, event.timestampMs)
        writeString(out, TAG_SCREEN_ID, event.screenId.value)

        // Write type-specific fields
        when (event) {
            is WatchLearnEvent.Gesture -> encodeGesture(out, event.gesture)
            is WatchLearnEvent.Snapshot -> encodeSnapshot(out, event.snapshot)
            is WatchLearnEvent.Delta -> encodeDelta(out, event.delta)
            is WatchLearnEvent.Navigation -> encodeNavigation(out, event.navigation)
            is WatchLearnEvent.TextInput -> encodeTextInput(out, event.input)
            is WatchLearnEvent.Action -> encodeAction(out, event.action)
            is WatchLearnEvent.Lifecycle -> encodeLifecycle(out, event)
        }

        out.writeByte(TAG_END.toInt())
        return buffer.toByteArray()
    }

    // View tree nodes are encoded recursively with depth-first pre-order traversal.
    // Each node is: TAG_SNAPSHOT_ROOT, childCount, [properties...], [children...]
    private fun encodeViewNode(out: DataOutputStream, node: ViewNode) {
        out.writeByte(TAG_SNAPSHOT_ROOT.toInt())
        writeVarInt(out, node.children.size)
        writeString(out, TAG_ELEMENT_ID, node.elementId.value)
        writeString(out, TAG_PROPERTY, node.className)
        // ... encode all properties with their tags
        for (child in node.children) {
            encodeViewNode(out, child)
        }
    }

    // Helper: variable-length integer encoding (protobuf-style varint)
    private fun writeVarInt(out: DataOutputStream, value: Int) { /* ... */ }
    private fun writeString(out: DataOutputStream, tag: Byte, value: String) { /* ... */ }
    private fun writeLong(out: DataOutputStream, tag: Byte, value: Long) { /* ... */ }
}
```

The encoding format:
- **Header**: 1 byte event type tag + 1 byte event type ordinal
- **Fields**: Each field is `[1-byte tag][VarInt length][value bytes]`
- **Strings**: UTF-8 encoded, prefixed with VarInt length
- **Integers**: VarInt encoding (1-5 bytes for Int, 1-10 bytes for Long)
- **Floats**: IEEE 754, 4 bytes
- **Coordinates**: Packed as 2 x Int16 (2 bytes each, 4 bytes total)
- **View tree**: Recursive depth-first encoding with child count prefix
- **Terminator**: 0x7F byte marks end of event

Decoding is the mirror: read tag, dispatch on tag to appropriate parser, read length, read value.

### 3.4 Event Persistence

Events flow: EventBus -> EventBatchCollector -> EventEncoder -> FileQueue -> (periodic flush) -> SqliteStorageEngine.

The FileQueue is the primary crash-safe buffer. SQLite is the long-term indexed store for pattern analysis.

---

## 4. The Capture Layer Algorithm

### 4.1 View Tree Walking

The view tree walker runs on the main thread (necessarily, since it accesses View properties) but must be extremely fast. Budget: under 8ms per snapshot (half a frame at 60fps).

**Algorithm for traditional View system:**

```
function walkViewTree(root: View) -> ViewNode:
    if root is not visible and config.skipInvisibleViews:
        return null

    node = ViewNode(
        elementId = generateStableId(root),
        className = root.javaClass.name,
        resourceIdName = resolveResourceName(root.id),
        contentDescription = root.contentDescription?.toString(),
        text = extractText(root),
        bounds = getAbsoluteBounds(root),
        ...
    )

    if root is ViewGroup:
        for i in 0 until root.childCount:
            child = root.getChildAt(i)
            childNode = walkViewTree(child)
            if childNode != null:
                node.children.add(childNode)

    return node
```

**Performance optimizations:**

1. **Pre-allocated node pool**: A `SnapshotPool` maintains pre-allocated `ViewNode` objects to avoid GC pressure. Pool size: 500 nodes (typical complex screen). Nodes are cleared and returned to the pool after encoding.

2. **Skip invisible branches**: If a ViewGroup is `GONE` or `INVISIBLE` and `clipChildren=true`, skip all children. This prunes large subtrees.

3. **Depth limit**: Default max depth of 30. Deeper trees are truncated. Configurable.

4. **Child limit**: Default max 100 children per ViewGroup. RecyclerView and similar containers are special-cased to only capture visible items.

5. **Property extraction caching**: `resourceIdName` resolution (calling `resources.getResourceEntryName()`) is expensive. Results are cached in an LRU cache keyed by `view.id`.

6. **Sampling for complex trees**: If the tree exceeds 2000 nodes, the walker switches to a "simplified" mode that only captures semantically meaningful nodes (clickable, editable, has content description, has resource ID).

**Algorithm for Compose:**

Compose does not expose a traditional View tree. Instead, WatchLearn reads the Semantics tree via `SemanticsOwner`.

```
function walkComposeTree(semanticsOwner: SemanticsOwner) -> ViewNode:
    root = semanticsOwner.rootSemanticsNode
    return mapSemanticsNode(root)

function mapSemanticsNode(node: SemanticsNode) -> ViewNode:
    config = node.config
    viewNode = ViewNode(
        elementId = generateComposeElementId(node),
        className = config.getOrNull(SemanticsProperties.ClassName) ?: "ComposeNode",
        testTag = config.getOrNull(SemanticsProperties.TestTag),
        text = config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text,
        contentDescription = config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull(),
        isClickable = config.contains(SemanticsActions.OnClick),
        bounds = node.boundsInRoot,
        ...
    )

    for child in node.children:
        viewNode.children.add(mapSemanticsNode(child))

    return viewNode
```

### 4.2 Stable Element ID Generation

Element IDs must be stable across app sessions so that the same button generates the same ID each time. This is critical for pattern matching and replay.

**Strategy (layered fallback):**

```
function generateStableId(view: View, screenId: ScreenId) -> ElementId:
    // Layer 1: Resource ID (most stable)
    if view.id != View.NO_ID:
        resourceName = resources.getResourceEntryName(view.id)
        if resourceName != null:
            return ElementId("res:${screenId.value}/${resourceName}")

    // Layer 2: Content description
    if view.contentDescription != null:
        return ElementId("cd:${screenId.value}/${hash(view.contentDescription)}")

    // Layer 3: Compose test tag
    if view has testTag:
        return ElementId("tag:${screenId.value}/${testTag}")

    // Layer 4: Structural XPath
    xpath = computeXPath(view)
    return ElementId("xpath:${screenId.value}/${xpath}")
```

**XPath computation:**

```
function computeXPath(view: View) -> String:
    segments = []
    current = view
    while current != null:
        parent = current.parent
        if parent is ViewGroup:
            // Index among siblings of the same class
            index = countSameClassSiblingsBefore(parent, current)
            segments.add("${current.javaClass.simpleName}[${index}]")
        else:
            segments.add(current.javaClass.simpleName)
        current = parent as? View
    return segments.reversed().joinToString("/")
```

The XPath is the least stable strategy (breaks when siblings are added/removed). The ID generator records which strategy was used so the replay engine knows the confidence level of element identification.

**For Compose:** Test tags are the primary strategy. If unavailable, the semantics node's merge descriptor + position index serves as fallback. Developers are strongly encouraged to add `Modifier.testTag()` to actionable elements.

### 4.3 Delta Computation

Full snapshots are expensive. After the first full snapshot of a screen, subsequent captures compute only deltas.

**Algorithm (tree diffing):**

```
function computeDelta(previous: ViewNode, current: ViewNode) -> List<Mutation>:
    mutations = []

    // Build lookup maps from previous tree
    prevMap = buildElementIdMap(previous)  // ElementId -> ViewNode
    currMap = buildElementIdMap(current)

    // Pass 1: Find removed nodes
    for (id, node) in prevMap:
        if id not in currMap:
            mutations.add(NodeRemoved(id))

    // Pass 2: Find added nodes
    for (id, node) in currMap:
        if id not in prevMap:
            parentId = findParentId(current, node)
            childIndex = findChildIndex(current, node)
            mutations.add(NodeAdded(parentId, childIndex, node))

    // Pass 3: Find changed properties on existing nodes
    for (id, currNode) in currMap:
        if id in prevMap:
            prevNode = prevMap[id]
            propertyDiffs = diffProperties(prevNode, currNode)
            for (prop, oldVal, newVal) in propertyDiffs:
                mutations.add(PropertyChanged(id, prop, oldVal, newVal))

            // Check if parent changed (moved)
            if parentOf(prevNode) != parentOf(currNode):
                mutations.add(NodeMoved(id, parentOf(currNode), childIndexOf(currNode)))

    return mutations
```

**Optimizations:**

1. **Structural hash**: Each ViewNode has a lazily-computed structural hash (hash of className + children's hashes). If a subtree's hash is unchanged, skip diffing its children entirely.

2. **Delta suppression**: If the delta contains more than 60% of the total nodes as mutations, emit a full snapshot instead (it is cheaper to encode).

3. **Snapshot scheduling**: Full snapshots are forced every 30 seconds regardless of deltas, as a recovery checkpoint. Deltas reference their base snapshot by ID.

---

## 5. The Storage Layer

### 5.1 SQLite Schema

```sql
-- Database: watchlearn.db
-- Version: 1

CREATE TABLE sessions (
    session_id       TEXT PRIMARY KEY,
    started_at_ms    INTEGER NOT NULL,
    ended_at_ms      INTEGER,
    app_version      TEXT NOT NULL,
    sdk_version      TEXT NOT NULL,
    device_model     TEXT NOT NULL,
    os_version       TEXT NOT NULL,
    is_active        INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE events (
    event_id         TEXT PRIMARY KEY,
    session_id       TEXT NOT NULL REFERENCES sessions(session_id),
    event_type       INTEGER NOT NULL,   -- ordinal of WatchLearnEvent subclass
    screen_id        TEXT NOT NULL,
    timestamp_ms     INTEGER NOT NULL,
    uptime_ms        INTEGER NOT NULL,
    encoded_payload  BLOB NOT NULL,      -- Binary-encoded event data
    batch_id         TEXT,               -- Which batch this came from
    FOREIGN KEY (session_id) REFERENCES sessions(session_id)
);

CREATE INDEX idx_events_session_time ON events(session_id, timestamp_ms);
CREATE INDEX idx_events_screen ON events(screen_id, timestamp_ms);
CREATE INDEX idx_events_type ON events(event_type, timestamp_ms);

CREATE TABLE semantic_actions (
    action_id        TEXT PRIMARY KEY,
    event_id         TEXT NOT NULL REFERENCES events(event_id),
    session_id       TEXT NOT NULL,
    action_type      TEXT NOT NULL,       -- e.g., "add_to_cart"
    screen_id        TEXT NOT NULL,
    timestamp_ms     INTEGER NOT NULL,
    target_element_id TEXT,
    parameters_json  TEXT,                -- JSON map of parameters
    action_source    INTEGER NOT NULL     -- ActionSource ordinal
);

CREATE INDEX idx_actions_type ON semantic_actions(action_type, timestamp_ms);
CREATE INDEX idx_actions_session ON semantic_actions(session_id, timestamp_ms);

CREATE TABLE workflows (
    workflow_id      TEXT PRIMARY KEY,
    name             TEXT NOT NULL,
    description      TEXT NOT NULL,
    steps_json       TEXT NOT NULL,       -- JSON-serialized List<WorkflowStep>
    trigger_context  BLOB,               -- Serialized ContextVector
    frequency_type   INTEGER NOT NULL,
    frequency_json   TEXT NOT NULL,       -- JSON-serialized WorkflowFrequency
    confidence_score REAL NOT NULL,
    first_seen_ms    INTEGER NOT NULL,
    last_seen_ms     INTEGER NOT NULL,
    execution_count  INTEGER NOT NULL DEFAULT 0,
    automation_count INTEGER NOT NULL DEFAULT 0,
    success_rate     REAL NOT NULL DEFAULT 0.0,
    status           INTEGER NOT NULL,   -- WorkflowStatus ordinal
    version          INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX idx_workflows_status ON workflows(status);
CREATE INDEX idx_workflows_confidence ON workflows(confidence_score DESC);

CREATE TABLE workflow_instances (
    instance_id      TEXT PRIMARY KEY,
    workflow_id      TEXT NOT NULL REFERENCES workflows(workflow_id),
    session_id       TEXT NOT NULL,
    started_at_ms    INTEGER NOT NULL,
    completed_at_ms  INTEGER,
    status           INTEGER NOT NULL,   -- ReplayStatus ordinal
    steps_completed  INTEGER NOT NULL DEFAULT 0,
    error_json       TEXT                -- JSON-serialized ReplayError
);

CREATE TABLE suggestions (
    suggestion_id    TEXT PRIMARY KEY,
    workflow_id      TEXT NOT NULL REFERENCES workflows(workflow_id),
    display_type     INTEGER NOT NULL,
    title            TEXT NOT NULL,
    description      TEXT NOT NULL,
    created_at_ms    INTEGER NOT NULL,
    expires_at_ms    INTEGER NOT NULL,
    shown_at_ms      INTEGER,
    responded_at_ms  INTEGER,
    response         INTEGER,            -- null=pending, 0=dismissed, 1=accepted, 2=snoozed
    priority         INTEGER NOT NULL
);

CREATE INDEX idx_suggestions_workflow ON suggestions(workflow_id);

CREATE TABLE user_preferences (
    preference_key   TEXT PRIMARY KEY,   -- "workflow:{id}:enabled", "global:max_daily", etc.
    value_text       TEXT NOT NULL,
    updated_at_ms    INTEGER NOT NULL
);

CREATE TABLE action_registry (
    action_type      TEXT PRIMARY KEY,
    class_name       TEXT NOT NULL,
    method_name      TEXT NOT NULL,
    parameters_json  TEXT NOT NULL,       -- JSON-serialized List<ActionParameter>
    guard_expression TEXT,
    registered_at_ms INTEGER NOT NULL
);

CREATE TABLE pattern_ngrams (
    ngram_hash       TEXT PRIMARY KEY,    -- Hash of the n-gram sequence
    action_types     TEXT NOT NULL,       -- Pipe-separated action types
    ngram_size       INTEGER NOT NULL,
    count            INTEGER NOT NULL DEFAULT 1,
    first_seen_ms    INTEGER NOT NULL,
    last_seen_ms     INTEGER NOT NULL,
    avg_interval_ms  REAL,               -- Average time between occurrences
    screens_json     TEXT                 -- JSON array of screen IDs involved
);

CREATE INDEX idx_ngrams_count ON pattern_ngrams(count DESC);
CREATE INDEX idx_ngrams_size ON pattern_ngrams(ngram_size);
```

### 5.2 File Queue Implementation

The file queue is the crash-safe buffer between event emission and SQLite persistence. Inspired by PostHog's approach.

**Design:**

The queue uses append-only segment files. Each segment is a flat binary file with a simple format:

```
Segment file format:
  [4 bytes: magic number 0x574C5151 ("WLQQ")]
  [4 bytes: segment version (1)]
  [4 bytes: entry count]
  [4 bytes: CRC32 of header]
  Entry[]:
    [4 bytes: entry length (N)]
    [N bytes: encoded event data]
    [4 bytes: CRC32 of entry data]
  [4 bytes: segment footer magic 0x574C5146 ("WLQF")]
```

```kotlin
// FileQueue.kt
internal class FileQueue(
    private val directory: File,         // Context.filesDir/watchlearn/queue/
    private val maxSegmentSize: Long = 512 * 1024,  // 512 KB per segment
    private val maxTotalSize: Long = 10 * 1024 * 1024, // 10 MB total
    private val maxSegmentCount: Int = 50,
    private val dispatchers: WatchLearnDispatchers
) {
    private val segments = ConcurrentLinkedDeque<FileQueueSegment>()
    private val writeLock = Mutex()
    private var activeSegment: FileQueueSegment? = null

    suspend fun initialize() {
        withContext(dispatchers.io) {
            directory.mkdirs()
            // Recover existing segments from disk
            val existingFiles = directory.listFiles()
                ?.filter { it.name.startsWith("wlq_") && it.name.endsWith(".seg") }
                ?.sortedBy { it.name } // ULID-named, so lexicographic = chronological
                ?: emptyList()

            for (file in existingFiles) {
                val segment = FileQueueSegment.recover(file)
                if (segment != null) {
                    segments.addLast(segment)
                } else {
                    // Corrupted segment -- move to dead-letter directory
                    file.renameTo(File(directory, "dead/${file.name}"))
                }
            }

            // Open or create active segment
            activeSegment = if (segments.isNotEmpty() && segments.peekLast().canWrite()) {
                segments.peekLast()
            } else {
                rollNewSegment()
            }
        }
    }

    suspend fun enqueue(data: ByteArray) {
        writeLock.withLock {
            var segment = activeSegment!!
            if (!segment.canWrite() || segment.size() + data.size + 8 > maxSegmentSize) {
                segment.finalize()
                segment = rollNewSegment()
            }
            segment.append(data)
            enforceQuota()
        }
    }

    suspend fun peek(maxCount: Int): List<ByteArray> {
        // Read from oldest segment first (FIFO)
        val result = mutableListOf<ByteArray>()
        for (segment in segments) {
            if (result.size >= maxCount) break
            result.addAll(segment.readAll().take(maxCount - result.size))
        }
        return result
    }

    suspend fun acknowledge(count: Int) {
        // Remove acknowledged entries from the front of the queue
        var remaining = count
        while (remaining > 0 && segments.isNotEmpty()) {
            val oldest = segments.peekFirst()
            val segmentSize = oldest.entryCount()
            if (segmentSize <= remaining) {
                segments.pollFirst()
                oldest.delete()
                remaining -= segmentSize
            } else {
                oldest.trimFront(remaining)
                remaining = 0
            }
        }
    }

    private fun enforceQuota() {
        // Remove oldest segments if over quota
        while (segments.size > maxSegmentCount ||
               segments.sumOf { it.size() } > maxTotalSize) {
            val oldest = segments.pollFirst() ?: break
            oldest.delete()
        }
    }

    private fun rollNewSegment(): FileQueueSegment {
        val name = "wlq_${UlidGenerator.next()}.seg"
        val segment = FileQueueSegment.create(File(directory, name))
        segments.addLast(segment)
        activeSegment = segment
        return segment
    }
}
```

**Crash recovery:** On startup, `initialize()` iterates all segment files. Each segment validates its header CRC32 and walks entries, validating per-entry CRC32s. Entries with invalid checksums are skipped (partial write due to crash). The footer magic confirms the segment was properly closed. Segments without a footer are treated as the "active" segment with potential partial last entry.

**Flush to SQLite:** A periodic coroutine (every 10 seconds) peeks from the FileQueue, decodes, inserts into SQLite, and acknowledges. If SQLite write fails, entries stay in the queue for retry.

---

## 6. The Pattern Detection Algorithm

### 6.1 N-Gram Sequence Matching

The core pattern detection is based on n-gram extraction from semantic action sequences.

**Definitions:**
- An **action token** is a string representing a semantic action type + screen ID: `"add_to_cart@product_detail"`, `"tap_checkout@cart"`, etc.
- An **action sequence** is a time-ordered list of action tokens within a session.
- An **n-gram** is a contiguous subsequence of n action tokens.

**Algorithm:**

```
PHASE 1: N-GRAM EXTRACTION (runs after each session ends or every 100 actions)

function extractNGrams(actions: List<SemanticAction>, minN: Int = 2, maxN: Int = 10):
    for n in minN..maxN:
        for i in 0..(actions.size - n):
            ngram = actions[i..i+n].map { "${it.actionType}@${it.screenId}" }
            ngramHash = sha256(ngram.joinToString("|"))

            // Upsert into pattern_ngrams table
            existing = db.query(pattern_ngrams, ngramHash)
            if existing != null:
                existing.count += 1
                existing.last_seen_ms = now()
                existing.avg_interval_ms = updateRunningAverage(
                    existing.avg_interval_ms, existing.count,
                    now() - existing.last_seen_ms
                )
                db.update(existing)
            else:
                db.insert(ngramHash, ngram, n, count=1, first_seen=now(), last_seen=now())


PHASE 2: SEQUENCE MATCHING (runs periodically, e.g., every 6 hours or on app startup)

function findRecurringSequences(minCount: Int = 3, minConfidence: Float = 0.6):
    candidates = db.query(
        "SELECT * FROM pattern_ngrams WHERE count >= ? ORDER BY count DESC, ngram_size DESC",
        minCount
    )

    // Filter overlapping n-grams: prefer longer ones
    filtered = removeSubsequences(candidates)

    // Score each candidate
    scoredWorkflows = []
    for candidate in filtered:
        score = computeConfidence(candidate)
        if score >= minConfidence:
            workflow = extractWorkflow(candidate)
            scoredWorkflows.add((workflow, score))

    return scoredWorkflows
```

**Removing overlapping subsequences:** If n-gram `[A, B, C, D]` has count >= threshold AND `[A, B, C]` also has count >= threshold, we prefer the longer one. The shorter one is only kept if it occurs significantly more often independently (count ratio > 2x).

### 6.2 Fuzzy Sequence Matching (Smith-Waterman)

Exact n-gram matching misses workflows that are mostly the same but have minor variations (an extra scroll, an optional step). Smith-Waterman local alignment finds the best local alignment between two sequences.

```
function alignSequences(seq1: List<String>, seq2: List<String>) -> AlignmentResult:
    // Scoring: match = +2, mismatch = -1, gap = -1
    m = seq1.size
    n = seq2.size
    H = Matrix(m+1, n+1, init=0)  // score matrix
    T = Matrix(m+1, n+1, init=NONE) // traceback matrix
    maxScore = 0
    maxPos = (0, 0)

    for i in 1..m:
        for j in 1..n:
            match = H[i-1][j-1] + (if seq1[i-1] == seq2[j-1] then 2 else -1)
            delete = H[i-1][j] - 1
            insert = H[i][j-1] - 1
            H[i][j] = max(0, match, delete, insert)

            if H[i][j] == match: T[i][j] = DIAG
            if H[i][j] == delete: T[i][j] = UP
            if H[i][j] == insert: T[i][j] = LEFT
            if H[i][j] == 0: T[i][j] = NONE

            if H[i][j] > maxScore:
                maxScore = H[i][j]
                maxPos = (i, j)

    // Traceback from maxPos to build alignment
    alignment = traceback(T, seq1, seq2, maxPos)
    similarity = maxScore / (2.0 * max(m, n))  // Normalized to 0..1

    return AlignmentResult(alignment, similarity)
```

**Usage:** When a new action sequence is observed, it is aligned against all existing workflow step sequences. If similarity > 0.7, it is considered a match (potentially with parameter variations). Alignment runs on the computation dispatcher (background thread pool).

**Performance guard:** Smith-Waterman is O(m*n). For sequences longer than 50 steps, the algorithm falls back to a faster heuristic: compare the set of action types (Jaccard similarity) and only run full alignment if Jaccard > 0.5.

### 6.3 Time-Based Pattern Detection

Many user workflows are time-periodic ("check email every morning", "weekly grocery order").

**Algorithm: Periodicity estimation**

```
function detectPeriodicity(timestamps: List<Long>) -> WorkflowFrequency?:
    if timestamps.size < 3: return null

    // Compute inter-event intervals
    intervals = []
    for i in 1 until timestamps.size:
        intervals.add(timestamps[i] - timestamps[i-1])

    // Build circular time histogram
    // Bucket by hour-of-day (24 buckets) and day-of-week (7 buckets)
    hourHistogram = IntArray(24)
    dayHistogram = IntArray(7)
    for ts in timestamps:
        calendar = Calendar.getInstance().apply { timeInMillis = ts }
        hourHistogram[calendar.get(HOUR_OF_DAY)] += 1
        dayHistogram[calendar.get(DAY_OF_WEEK) - 1] += 1

    // Detect dominant hour
    peakHour = hourHistogram.indexOfMax()
    hourConcentration = hourHistogram[peakHour].toFloat() / timestamps.size
    // If > 60% of occurrences are in the same hour, strong time signal
    hasTimePeak = hourConcentration > 0.6

    // Detect dominant day
    peakDay = dayHistogram.indexOfMax()
    dayConcentration = dayHistogram[peakDay].toFloat() / timestamps.size
    hasDayPeak = dayConcentration > 0.5

    // Check for periodicity using autocorrelation
    // Discretize timestamps to hourly buckets
    bucketedSignal = discretizeToHourlyBuckets(timestamps)
    autocorrelation = computeAutocorrelation(bucketedSignal)

    // Find peaks in autocorrelation at expected periods
    dailyPeak = autocorrelation[24]      // 24 hours
    weeklyPeak = autocorrelation[168]     // 168 hours
    biweeklyPeak = autocorrelation[336]
    monthlyPeak = autocorrelation[720]    // ~30 days

    bestPeriod = findStrongestPeriod(dailyPeak, weeklyPeak, biweeklyPeak, monthlyPeak)

    return WorkflowFrequency(
        type = classifyPeriod(bestPeriod),
        intervalMs = bestPeriod * 3600_000L,
        dayOfWeek = if hasDayPeak then peakDay + 1 else null,
        hourOfDay = if hasTimePeak then peakHour else null,
        confidence = max(hourConcentration, dayConcentration, autocorrelationPeak)
    )
```

### 6.4 Confidence Scoring

Each potential workflow gets a composite confidence score:

```
function computeConfidence(candidate: NGramCandidate) -> Float:
    weights = ScoringWeights.default()  // Configurable

    // Signal 1: Frequency (how often the sequence occurs)
    frequencyScore = sigmoid(candidate.count, center=5, steepness=0.5)
    // 3 occurrences => ~0.27, 5 => 0.5, 10 => ~0.92

    // Signal 2: Recency (recent patterns are more relevant)
    daysSinceLastSeen = (now() - candidate.last_seen_ms) / 86400000.0
    recencyScore = exp(-0.05 * daysSinceLastSeen)
    // 0 days => 1.0, 14 days => 0.5, 30 days => 0.22

    // Signal 3: Consistency (how similar the sequences are across occurrences)
    // Measured by the variance of the sequence lengths and timing
    consistencyScore = 1.0 - normalizedVariance(candidate.intervalVariance)

    // Signal 4: Completeness (does the pattern represent a full task?)
    // Heuristic: sequences that end with a "terminal" action (submit, confirm, save)
    completenessScore = if endsWithTerminal(candidate) then 1.0 else 0.5

    // Signal 5: Temporal regularity (does it happen at regular times?)
    temporalScore = candidate.frequency?.confidence ?: 0.0

    // Weighted sum
    composite = weights.frequency * frequencyScore +
                weights.recency * recencyScore +
                weights.consistency * consistencyScore +
                weights.completeness * completenessScore +
                weights.temporal * temporalScore

    return composite.coerceIn(0.0f, 1.0f)
```

**Default weights:** frequency=0.30, recency=0.20, consistency=0.20, completeness=0.15, temporal=0.15.

---

## 7. The Suggestion Engine

### 7.1 When to Show Suggestions

The `SuggestionEngine` subscribes to the EventBus and maintains a running buffer of recent actions. When the current action sequence matches the beginning of a known workflow (prefix matching), it triggers a suggestion.

```
function onNewAction(action: SemanticAction):
    recentActions.add(action)
    if recentActions.size > 20: recentActions.removeFirst()

    for workflow in activeWorkflows:
        prefixMatch = matchesPrefix(recentActions, workflow.steps)
        if prefixMatch.matchLength >= 2 and prefixMatch.remainingSteps >= 1:
            // User is in the middle of a known workflow
            contextMatch = cosineSimilarity(currentContext, workflow.triggerContext)
            if contextMatch > 0.5:
                candidate = Suggestion(
                    workflow = workflow,
                    title = "Complete '${workflow.name}'?",
                    description = "${prefixMatch.remainingSteps} steps remaining",
                    estimatedTimeSavedMs = estimateTimeSaved(workflow, prefixMatch)
                )
                if throttler.canShow(candidate):
                    show(candidate)

    // Also check time-triggered suggestions
    for workflow in timeTriggeredWorkflows:
        if isWithinTimeWindow(workflow.frequency, now()):
            contextMatch = cosineSimilarity(currentContext, workflow.triggerContext)
            if contextMatch > 0.3:
                // Time-based suggestion (proactive)
                candidate = Suggestion(
                    workflow = workflow,
                    title = "Time for '${workflow.name}'?",
                    displayType = NOTIFICATION
                )
                if throttler.canShow(candidate):
                    showNotification(candidate)
```

### 7.2 Throttling

The `SuggestionThrottler` prevents suggestion fatigue:

```kotlin
internal class SuggestionThrottler(
    private val preferenceStore: UserPreferenceStore,
    private val clock: Clock
) {
    // Hard limits
    private val maxSuggestionsPerHour: Int = 3
    private val maxSuggestionsPerDay: Int = 10
    private val cooldownAfterDismissalMs: Long = 4 * 3600_000L  // 4 hours
    private val cooldownAfterRejectionMs: Long = 7 * 86400_000L // 7 days
    private val backoffMultiplier: Float = 2.0f

    private val recentSuggestions = ArrayDeque<Long>() // timestamps

    fun canShow(suggestion: Suggestion): Boolean {
        val now = clock.currentTimeMillis()

        // Global rate limit
        val suggestionsLastHour = recentSuggestions.count { now - it < 3600_000L }
        if (suggestionsLastHour >= maxSuggestionsPerHour) return false

        val suggestionsLastDay = recentSuggestions.count { now - it < 86400_000L }
        if (suggestionsLastDay >= maxSuggestionsPerDay) return false

        // Per-workflow cooldown
        val workflowPrefs = preferenceStore.getWorkflowPreferences(suggestion.workflow.workflowId)
        val lastShownMs = workflowPrefs.lastSuggestionShownMs
        val dismissCount = workflowPrefs.consecutiveDismissals

        val cooldown = when {
            workflowPrefs.isRejected -> cooldownAfterRejectionMs
            dismissCount > 0 -> cooldownAfterDismissalMs * backoffMultiplier.pow(dismissCount - 1)
            else -> 0L
        }

        if (now - lastShownMs < cooldown) return false

        // Never show if user explicitly turned off
        if (workflowPrefs.isExplicitlyDisabled) return false

        return true
    }

    fun recordShown(suggestion: Suggestion) {
        recentSuggestions.addLast(clock.currentTimeMillis())
        // Trim old entries
        while (recentSuggestions.isNotEmpty() &&
               clock.currentTimeMillis() - recentSuggestions.first() > 86400_000L) {
            recentSuggestions.removeFirst()
        }
    }
}
```

### 7.3 User Preference Tracking

Implicit signals:
- **Accepted**: Reset cooldown, increase confidence weight for this workflow
- **Dismissed (X button)**: Increment consecutive dismissal counter, apply exponential backoff
- **Snoozed**: Defer for user-specified or default period (2 hours)
- **Rejected ("Don't suggest again")**: Mark workflow as rejected; 7-day cooldown minimum; after 3 rejections, permanently suppress
- **Completed manually after seeing suggestion**: Treat as soft positive signal (user saw it but preferred manual; slight confidence boost)
- **Automation succeeded**: Strong positive signal; reduce future suggestion priority (already automated)
- **Automation failed**: Record failure; after 3 consecutive failures, pause automation and re-suggest with lower priority

---

## 8. The Replay/Execution Engine

### 8.1 Action Registry

```kotlin
// WatchAction.kt
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class WatchAction(
    val type: String,                    // Unique action type identifier
    val description: String = "",
    val screen: String = "",             // Expected screen for this action
    val idempotent: Boolean = false      // Safe to retry?
)

// WatchParam.kt
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class WatchParam(
    val name: String,
    val description: String = "",
    val sensitive: Boolean = false       // Will be redacted in logs
)

// WatchGuard.kt
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class WatchGuard(
    val expression: String               // e.g., "screen == 'cart' && cartNotEmpty"
)
```

**Registry scanning at init time:**

```kotlin
// RegistryScanner.kt
internal class RegistryScanner {

    fun scan(vararg targets: Any): List<ActionDescriptor> {
        val descriptors = mutableListOf<ActionDescriptor>()
        for (target in targets) {
            val klass = target::class
            for (function in klass.memberFunctions) {
                val watchAction = function.findAnnotation<WatchAction>() ?: continue
                val params = function.parameters
                    .filter { it.findAnnotation<WatchParam>() != null }
                    .map { param ->
                        val annotation = param.findAnnotation<WatchParam>()!!
                        ActionParameter(
                            name = annotation.name,
                            type = mapKotlinType(param.type),
                            isSensitive = annotation.sensitive,
                            description = annotation.description
                        )
                    }
                val guards = function.findAnnotations<WatchGuard>()
                    .map { it.expression }

                descriptors.add(ActionDescriptor(
                    actionType = watchAction.type,
                    description = watchAction.description,
                    expectedScreen = watchAction.screen,
                    isIdempotent = watchAction.idempotent,
                    targetInstance = WeakReference(target),
                    method = function,
                    parameters = params,
                    guardExpressions = guards
                ))
            }
        }
        return descriptors
    }
}
```

### 8.2 Parameter Substitution

The `ParameterResolver` resolves step parameters at replay time:

```kotlin
internal class ParameterResolver(
    private val executionContext: ExecutionContext
) {
    fun resolve(parameter: StepParameter): Any? {
        return when {
            // Fixed value
            !parameter.isVariable -> parameter.defaultValue?.let { cast(it, parameter.type) }

            // Source expression (mini expression language)
            parameter.sourceExpression != null -> evaluateExpression(
                parameter.sourceExpression, executionContext
            )

            // Prompt user for value
            else -> executionContext.requestUserInput(parameter.name, parameter.type)
        }
    }

    // Expression examples:
    // "context.lastProductName" - from execution context
    // "step[0].result.orderId" - from previous step result
    // "input.quantity"         - from user input
    private fun evaluateExpression(expression: String, context: ExecutionContext): Any? {
        val parts = expression.split(".")
        return when (parts[0]) {
            "context" -> context.getVariable(parts.drop(1).joinToString("."))
            "step" -> {
                val stepIndex = parts[1].removeSurrounding("[", "]").toInt()
                val stepResult = context.getStepResult(stepIndex)
                resolveNestedPath(stepResult, parts.drop(2))
            }
            "input" -> context.getUserInput(parts[1])
            else -> null
        }
    }
}
```

### 8.3 State Machine

```kotlin
// ReplayState.kt
enum class ReplayState {
    IDLE,            // No replay in progress
    PREPARING,       // Building execution plan, resolving parameters
    WAITING_SCREEN,  // Waiting for expected screen to appear
    EXECUTING_STEP,  // Currently executing a step
    VERIFYING,       // Post-step verification
    PAUSED,          // User paused or awaiting input
    RECOVERING,      // Error recovery in progress
    COMPLETED,       // All steps completed successfully
    FAILED,          // Unrecoverable failure
    ABORTED          // User aborted
}

// ReplayStateMachine.kt
internal class ReplayStateMachine {
    private val _state = MutableStateFlow(ReplayState.IDLE)
    val state: StateFlow<ReplayState> = _state.asStateFlow()

    private val validTransitions: Map<ReplayState, Set<ReplayState>> = mapOf(
        IDLE to setOf(PREPARING),
        PREPARING to setOf(WAITING_SCREEN, FAILED, ABORTED),
        WAITING_SCREEN to setOf(EXECUTING_STEP, FAILED, ABORTED),
        EXECUTING_STEP to setOf(VERIFYING, RECOVERING, PAUSED, FAILED, ABORTED),
        VERIFYING to setOf(WAITING_SCREEN, EXECUTING_STEP, COMPLETED, RECOVERING, FAILED),
        PAUSED to setOf(EXECUTING_STEP, ABORTED),
        RECOVERING to setOf(EXECUTING_STEP, WAITING_SCREEN, FAILED, ABORTED),
        COMPLETED to setOf(IDLE),
        FAILED to setOf(IDLE),
        ABORTED to setOf(IDLE)
    )

    fun transition(newState: ReplayState) {
        val current = _state.value
        require(newState in (validTransitions[current] ?: emptySet())) {
            "Invalid transition: $current -> $newState"
        }
        _state.value = newState
    }
}
```

### 8.4 Replay Strategies (Strategy Pattern)

```kotlin
// ReplayStrategy.kt
interface ReplayStrategy {
    val name: String
    val priority: Int  // Lower = preferred

    suspend fun canExecute(step: WorkflowStep, context: ExecutionContext): Boolean
    suspend fun execute(step: WorkflowStep, context: ExecutionContext): StepResult
    suspend fun verify(step: WorkflowStep, context: ExecutionContext): VerificationResult
}

// DirectInvocationStrategy.kt -- calls @WatchAction methods directly
internal class DirectInvocationStrategy(
    private val registry: ActionRegistry
) : ReplayStrategy {
    override val name = "direct_invocation"
    override val priority = 0  // Most preferred

    override suspend fun canExecute(step: WorkflowStep, context: ExecutionContext): Boolean {
        val descriptor = registry.find(step.action.actionType) ?: return false
        // Check guard conditions
        return descriptor.guardExpressions.all { evaluateGuard(it, context) }
    }

    override suspend fun execute(step: WorkflowStep, context: ExecutionContext): StepResult {
        val descriptor = registry.find(step.action.actionType)!!
        val target = descriptor.targetInstance.get()
            ?: return StepResult.Failed(ReplayErrorType.ACTION_FAILED, "Target GC'd")

        val resolvedParams = step.parameters.map { paramResolver.resolve(it) }

        return try {
            // Must invoke on main thread if it touches UI
            val result = withContext(Dispatchers.Main) {
                descriptor.method.call(target, *resolvedParams.toTypedArray())
            }
            StepResult.Success(result)
        } catch (e: Exception) {
            StepResult.Failed(ReplayErrorType.ACTION_FAILED, e.message)
        }
    }
}

// AccessibilityStrategy.kt -- uses AccessibilityService for UI automation
internal class AccessibilityStrategy(
    private val accessibilityService: WatchLearnAccessibilityService?
) : ReplayStrategy {
    override val name = "accessibility"
    override val priority = 10  // Fallback

    override suspend fun canExecute(step: WorkflowStep, context: ExecutionContext): Boolean {
        return accessibilityService != null &&
               step.action.targetElementId != null
    }

    override suspend fun execute(step: WorkflowStep, context: ExecutionContext): StepResult {
        val service = accessibilityService!!
        val targetId = step.action.targetElementId!!

        // Find node in accessibility tree
        val node = service.findNodeById(targetId)
            ?: return StepResult.Failed(ReplayErrorType.ELEMENT_NOT_FOUND,
                "Element $targetId not found in accessibility tree")

        return when (step.action.actionType) {
            "tap", "click" -> {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                StepResult.Success(null)
            }
            "set_text" -> {
                val text = step.parameters.find { it.name == "text" }?.let {
                    paramResolver.resolve(it) as? String
                } ?: return StepResult.Failed(ReplayErrorType.ACTION_FAILED, "No text param")
                val args = Bundle().apply { putString(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text) }
                node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                StepResult.Success(null)
            }
            "scroll" -> {
                node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                StepResult.Success(null)
            }
            else -> StepResult.Failed(ReplayErrorType.ACTION_FAILED, "Unsupported action for accessibility")
        }
    }
}

// IntentStrategy.kt -- uses Intents for navigation
internal class IntentStrategy(
    private val context: Context
) : ReplayStrategy {
    override val name = "intent"
    override val priority = 5

    override suspend fun canExecute(step: WorkflowStep, context: ExecutionContext): Boolean {
        return step.action.actionType.startsWith("navigate") &&
               step.action.parameters.containsKey("route")
    }

    override suspend fun execute(step: WorkflowStep, ctx: ExecutionContext): StepResult {
        val route = step.action.parameters["route"] ?: return StepResult.Failed(
            ReplayErrorType.ACTION_FAILED, "No route specified")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(route))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return StepResult.Success(null)
    }
}

// StrategySelector.kt
internal class StrategySelector(
    private val strategies: List<ReplayStrategy>
) {
    suspend fun selectStrategy(step: WorkflowStep, context: ExecutionContext): ReplayStrategy? {
        return strategies
            .sortedBy { it.priority }
            .firstOrNull { it.canExecute(step, context) }
    }
}
```

### 8.5 Error Handling and Recovery

```kotlin
// ReplayErrorHandler.kt
internal class ReplayErrorHandler(
    private val stateMachine: ReplayStateMachine,
    private val strategySelector: StrategySelector
) {
    suspend fun handle(
        error: ReplayError,
        step: WorkflowStep,
        context: ExecutionContext
    ): RecoveryAction {
        return when (error.type) {
            ELEMENT_NOT_FOUND -> {
                // Wait and retry -- element might not have loaded yet
                if (context.retryCount(step.stepIndex) < step.retryPolicy.maxRetries) {
                    delay(step.retryPolicy.retryDelayMs *
                          step.retryPolicy.backoffMultiplier.pow(context.retryCount(step.stepIndex)))
                    context.incrementRetry(step.stepIndex)
                    RecoveryAction.RETRY
                } else if (step.isOptional) {
                    RecoveryAction.SKIP
                } else {
                    // Try alternative strategy
                    val altStrategy = strategySelector.selectAlternative(step, context)
                    if (altStrategy != null) {
                        context.setStrategy(step.stepIndex, altStrategy)
                        RecoveryAction.RETRY_WITH_ALTERNATIVE
                    } else {
                        RecoveryAction.ABORT
                    }
                }
            }

            SCREEN_MISMATCH -> {
                // We are on the wrong screen -- try navigating
                val targetScreen = step.expectedScreenId
                val navStep = synthesizeNavigationStep(targetScreen)
                if (navStep != null && context.retryCount(step.stepIndex) < 2) {
                    context.insertStep(step.stepIndex, navStep)
                    context.incrementRetry(step.stepIndex)
                    RecoveryAction.RETRY
                } else {
                    RecoveryAction.ABORT
                }
            }

            TIMEOUT -> {
                if (step.isOptional) RecoveryAction.SKIP
                else RecoveryAction.ABORT
            }

            APP_CRASHED -> RecoveryAction.ABORT

            PRECONDITION_FAILED -> RecoveryAction.ABORT

            else -> {
                if (step.retryPolicy.maxRetries > 0 &&
                    context.retryCount(step.stepIndex) < step.retryPolicy.maxRetries) {
                    context.incrementRetry(step.stepIndex)
                    RecoveryAction.RETRY
                } else {
                    RecoveryAction.ABORT
                }
            }
        }
    }
}

enum class RecoveryAction {
    RETRY, RETRY_WITH_ALTERNATIVE, SKIP, ABORT, ROLLBACK
}

data class RetryPolicy(
    val maxRetries: Int = 3,
    val retryDelayMs: Long = 1000,
    val backoffMultiplier: Float = 2.0f,
    val retryOnTypes: Set<ReplayErrorType> = setOf(ELEMENT_NOT_FOUND, TIMEOUT)
)
```

### 8.6 Full Execution Loop

```kotlin
// ReplayEngine.kt
class ReplayEngine internal constructor(
    private val registry: ActionRegistry,
    private val strategySelector: StrategySelector,
    private val errorHandler: ReplayErrorHandler,
    private val stateMachine: ReplayStateMachine,
    private val captureEngine: CaptureEngine,
    private val dispatchers: WatchLearnDispatchers
) {
    suspend fun execute(workflow: Workflow): ReplayResult {
        val executionId = UlidGenerator.next()
        val context = ExecutionContext(workflow, executionId)
        val startMs = System.currentTimeMillis()

        stateMachine.transition(PREPARING)

        // Build execution plan
        val plan = ExecutionPlan(workflow.steps.toMutableList())
        var currentStepIndex = 0

        try {
            while (currentStepIndex < plan.steps.size) {
                val step = plan.steps[currentStepIndex]

                // Wait for correct screen
                stateMachine.transition(WAITING_SCREEN)
                val screenReady = waitForScreen(step.expectedScreenId, step.timeoutMs)
                if (!screenReady) {
                    val error = ReplayError(SCREEN_MISMATCH, "Expected ${step.expectedScreenId}",
                        currentStepIndex, null, step.expectedScreenId.value, getCurrentScreenId().value)
                    val recovery = errorHandler.handle(error, step, context)
                    when (recovery) {
                        RETRY -> continue
                        SKIP -> { currentStepIndex++; continue }
                        ABORT -> {
                            stateMachine.transition(FAILED)
                            return buildResult(executionId, startMs, FAILED, currentStepIndex, error)
                        }
                        else -> { currentStepIndex++; continue }
                    }
                }

                // Select and execute strategy
                stateMachine.transition(EXECUTING_STEP)
                val strategy = context.getStrategy(currentStepIndex)
                    ?: strategySelector.selectStrategy(step, context)
                    ?: run {
                        if (step.isOptional) { currentStepIndex++; continue }
                        stateMachine.transition(FAILED)
                        return buildResult(executionId, startMs, FAILED, currentStepIndex,
                            ReplayError(ACTION_FAILED, "No strategy available", currentStepIndex, null, null, null))
                    }

                val stepResult = strategy.execute(step, context)

                when (stepResult) {
                    is StepResult.Success -> {
                        context.recordStepResult(currentStepIndex, stepResult)

                        // Verify post-conditions
                        if (step.verificationCriteria != null) {
                            stateMachine.transition(VERIFYING)
                            val verification = strategy.verify(step, context)
                            if (verification !is VerificationResult.Pass) {
                                // Verification failed -- attempt recovery
                                val error = ReplayError(PRECONDITION_FAILED,
                                    "Post-step verification failed", currentStepIndex, null, null, null)
                                val recovery = errorHandler.handle(error, step, context)
                                if (recovery == ABORT) {
                                    stateMachine.transition(FAILED)
                                    return buildResult(executionId, startMs, PARTIAL_SUCCESS,
                                        currentStepIndex, error)
                                }
                                continue
                            }
                        }

                        currentStepIndex++
                    }

                    is StepResult.Failed -> {
                        val error = ReplayError(stepResult.errorType, stepResult.message,
                            currentStepIndex, step.action.targetElementId, null, null)
                        val recovery = errorHandler.handle(error, step, context)
                        when (recovery) {
                            RETRY, RETRY_WITH_ALTERNATIVE -> continue
                            SKIP -> currentStepIndex++
                            ABORT -> {
                                stateMachine.transition(FAILED)
                                return buildResult(executionId, startMs, FAILED, currentStepIndex, error)
                            }
                            ROLLBACK -> {
                                // Future: implement rollback
                                stateMachine.transition(FAILED)
                                return buildResult(executionId, startMs, FAILED, currentStepIndex, error)
                            }
                        }
                    }
                }
            }

            stateMachine.transition(COMPLETED)
            return buildResult(executionId, startMs, SUCCESS, plan.steps.size, null)

        } catch (e: CancellationException) {
            stateMachine.transition(ABORTED)
            return buildResult(executionId, startMs, ABORTED_BY_USER, currentStepIndex, null)
        } catch (e: Exception) {
            stateMachine.transition(FAILED)
            return buildResult(executionId, startMs, FAILED, currentStepIndex,
                ReplayError(UNKNOWN, e.message ?: "Unknown error", currentStepIndex, null, null, null))
        } finally {
            if (stateMachine.state.value !in setOf(IDLE)) {
                stateMachine.transition(IDLE)
            }
        }
    }

    private suspend fun waitForScreen(screenId: ScreenId, timeoutMs: Long): Boolean {
        if (getCurrentScreenId() == screenId) return true
        return withTimeoutOrNull(timeoutMs) {
            captureEngine.screenChanges
                .filter { it == screenId }
                .first()
            true
        } ?: false
    }
}
```

---

## 9. The Public Developer API

### 9.1 Main Entry Point

```kotlin
// WatchLearn.kt
object WatchLearn {
    // Initialization
    fun init(context: Context, config: WatchLearnConfig = WatchLearnConfig.default())
    fun init(context: Context, block: WatchLearnConfig.Builder.() -> Unit)

    // State
    val isInitialized: Boolean
    val isCapturing: Boolean
    val sessionId: SessionId?

    // Capture control
    fun startCapturing()
    fun stopCapturing()
    fun pauseCapturing()
    fun resumeCapturing()

    // Action registration
    fun registerActions(vararg targets: Any)  // Scans for @WatchAction
    fun unregisterActions(vararg targets: Any)

    // Manual event emission
    fun trackAction(actionType: String, parameters: Map<String, String> = emptyMap())
    fun trackNavigation(screenName: String, route: String? = null)
    fun trackScreen(screenId: String)       // Mark current screen explicitly

    // Workflow management
    fun getWorkflows(): List<Workflow>
    fun getWorkflow(workflowId: String): Workflow?
    fun enableWorkflow(workflowId: String)
    fun disableWorkflow(workflowId: String)
    fun deleteWorkflow(workflowId: String)

    // Replay
    suspend fun executeWorkflow(workflowId: String): ReplayResult
    fun getReplayState(): StateFlow<ReplayState>
    fun abortReplay()

    // Suggestions
    fun getSuggestions(): List<Suggestion>
    fun acceptSuggestion(suggestionId: String)
    fun dismissSuggestion(suggestionId: String)
    fun rejectSuggestion(suggestionId: String)  // "Don't suggest again"

    // Callbacks
    fun setOnSuggestionListener(listener: OnSuggestionListener?)
    fun setOnReplayListener(listener: OnReplayListener?)
    fun setOnPatternDetectedListener(listener: OnPatternDetectedListener?)

    // Privacy
    fun addSensitiveScreen(screenId: String)    // Never capture this screen
    fun addSensitiveElement(elementId: String)   // Always redact this element
    fun clearAllData()                           // Delete all stored data

    // Debug
    fun enableDebugLogging(enabled: Boolean)
    fun exportDiagnostics(): File               // Exports diagnostic report
}
```

### 9.2 Configuration DSL

```kotlin
// WatchLearnConfig.kt
data class WatchLearnConfig(
    // Capture settings
    val snapshotIntervalMs: Long = 1000L,       // Min interval between snapshots
    val maxSnapshotDepth: Int = 30,
    val maxNodesPerSnapshot: Int = 2000,
    val captureGestures: Boolean = true,
    val captureNavigation: Boolean = true,
    val captureTextInput: Boolean = true,
    val skipInvisibleViews: Boolean = true,

    // Event settings
    val batchSize: Int = 50,
    val batchWindowMs: Long = 5000L,

    // Storage settings
    val maxStorageMb: Int = 50,
    val maxQueueSizeMb: Int = 10,
    val eventRetentionDays: Int = 90,

    // Pattern detection settings
    val minPatternOccurrences: Int = 3,
    val minConfidenceScore: Float = 0.6f,
    val patternAnalysisIntervalMs: Long = 6 * 3600_000L,  // 6 hours
    val maxWorkflows: Int = 50,

    // Suggestion settings
    val suggestionsEnabled: Boolean = true,
    val maxSuggestionsPerDay: Int = 10,
    val suggestionDisplayType: SuggestionDisplayType = SuggestionDisplayType.BOTTOM_SHEET,
    val suggestionAutoExpireMs: Long = 300_000L,  // 5 minutes

    // Replay settings
    val defaultStepTimeoutMs: Long = 10_000L,
    val defaultRetryPolicy: RetryPolicy = RetryPolicy(),
    val requireUserConfirmation: Boolean = true,  // Ask before each replay

    // Privacy settings
    val redactPasswords: Boolean = true,
    val redactEmails: Boolean = true,
    val redactPhoneNumbers: Boolean = true,
    val sensitiveScreens: Set<String> = emptySet(),
    val sensitiveElements: Set<String> = emptySet(),
    val captureTextValues: Boolean = false,  // false = hash only

    // Event filtering
    val eventFilter: EventFilter = EventFilter.default(),

    // Compose support
    val composeEnabled: Boolean = true
) {
    class Builder {
        // All fields with defaults matching the data class
        // Builder methods for each field, returning `this`
        fun build(): WatchLearnConfig
    }
}
```

### 9.3 Callback Interfaces

```kotlin
interface OnSuggestionListener {
    fun onSuggestionReady(suggestion: Suggestion)
    fun onSuggestionExpired(suggestion: Suggestion)
}

interface OnReplayListener {
    fun onReplayStarted(workflow: Workflow)
    fun onReplayStepCompleted(workflow: Workflow, stepIndex: Int, totalSteps: Int)
    fun onReplayCompleted(result: ReplayResult)
    fun onReplayFailed(result: ReplayResult)
    fun onReplayAborted(result: ReplayResult)
}

interface OnPatternDetectedListener {
    fun onNewWorkflowDetected(workflow: Workflow)
    fun onWorkflowUpdated(workflow: Workflow)
    fun onWorkflowDeprecated(workflow: Workflow)
}
```

### 9.4 Annotations (already shown in section 8.1)

### 9.5 Compose Extensions

```kotlin
// Modifiers.kt
fun Modifier.watchAction(
    actionType: String,
    parameters: Map<String, String> = emptyMap()
): Modifier

fun Modifier.watchSensitive(): Modifier  // Marks element as sensitive

// WatchLearnComposable.kt
val LocalWatchLearn = staticCompositionLocalOf<WatchLearn> { error("WatchLearn not provided") }

@Composable
fun WatchLearnProvider(content: @Composable () -> Unit)

@Composable
fun rememberWatchLearnState(): WatchLearnState  // Observe suggestions, replay state
```

---

## 10. Threading Model

| Work | Thread/Dispatcher | Rationale |
|---|---|---|
| View tree walking | `Dispatchers.Main` | Must access View properties on main thread |
| Compose semantics reading | `Dispatchers.Main` | Compose UI thread requirement |
| Gesture interception | `Dispatchers.Main` | `Window.Callback` runs on main thread |
| Event emission to bus | Caller's context | `tryEmit` is non-blocking; `emit` suspends if buffer full |
| Event batching | `WatchLearnDispatchers.io` | I/O-bound; dedicated single thread |
| Binary encoding | `WatchLearnDispatchers.computation` | CPU-bound; limited parallelism pool |
| File queue writes | `WatchLearnDispatchers.io` | Disk I/O |
| SQLite writes | `WatchLearnDispatchers.io` | Single-writer; serialized via Mutex |
| SQLite reads | `WatchLearnDispatchers.io` | Can parallelize reads |
| Pattern detection | `WatchLearnDispatchers.computation` | CPU-intensive; runs in background |
| Smith-Waterman alignment | `WatchLearnDispatchers.computation` | CPU-intensive |
| Suggestion evaluation | `WatchLearnDispatchers.computation` | Quick but should not block UI |
| Suggestion UI display | `Dispatchers.Main` | UI updates |
| Replay execution | `Dispatchers.Main` + `io` | UI actions on Main; waits on IO |
| Accessibility queries | `Dispatchers.Main` | Accessibility API requirement |

```kotlin
// WatchLearnDispatchers.kt
internal class WatchLearnDispatchers {
    // Single-threaded for sequential I/O (file queue, SQLite writes)
    val io: CoroutineDispatcher = Executors.newSingleThreadExecutor { r ->
        Thread(r, "WatchLearn-IO").apply { isDaemon = true }
    }.asCoroutineDispatcher()

    // Limited parallelism for CPU work (pattern detection, encoding)
    val computation: CoroutineDispatcher = Executors.newFixedThreadPool(2) { r ->
        Thread(r, "WatchLearn-Compute").apply { isDaemon = true; priority = Thread.MIN_PRIORITY }
    }.asCoroutineDispatcher()

    fun shutdown() {
        (io as ExecutorCoroutineDispatcher).close()
        (computation as ExecutorCoroutineDispatcher).close()
    }
}
```

**Key design decisions:**

1. **View tree walking budget**: The walker must complete within 8ms on the main thread. If it exceeds this (monitored via `SystemClock.elapsedRealtime()`), it aborts and logs a warning. The next snapshot will be a full snapshot since the delta baseline is lost.

2. **Pattern detection is deferred**: Never runs in real-time. It runs either on a periodic schedule (default: every 6 hours), on app cold start (after a 30-second delay), or when explicitly triggered.

3. **Replay holds a wake lock**: During active replay, a partial wake lock is held to prevent the CPU from sleeping mid-workflow. Released immediately after replay completes or fails.

4. **All coroutines are scoped**: Tied to `ProcessLifecycleOwner` scope. When the app process dies, all coroutines are cancelled. The file queue ensures no data loss.

---

## 11. Testing Strategy

### 11.1 Unit Tests

**Core module (highest priority):**
- `EventEncoderTest` / `EventDecoderTest`: Round-trip encoding of every event type. Property-based tests with random event generation. Verify binary format stability (encode with v1, decode with v1).
- `FileQueueTest`: Write N entries, read back, verify order. Fill to capacity, verify oldest entries evicted. Concurrent read/write stress test.
- `FileQueueCrashRecoveryTest`: Write entries, truncate file at random points (simulating crash), recover. Verify that complete entries before truncation point are recoverable and partial entries are discarded.
- `RingBufferTest`: Full buffer overwrites oldest. Empty buffer returns empty.
- `DebouncerTest`: Events within window are collapsed. Events after window pass through.

**Capture module:**
- `ViewTreeWalkerTest`: Construct mock View hierarchies using Robolectric. Verify node count, depth, properties. Test RecyclerView special-casing (only visible items captured).
- `StableIdGeneratorTest`: Same view produces same ID across calls. Different views produce different IDs. Verify fallback chain: resource ID -> content description -> test tag -> XPath.
- `DeltaComputerTest`: Two identical trees produce empty delta. Add a node, verify `NodeAdded`. Remove a node, verify `NodeRemoved`. Change text, verify `PropertyChanged`. Move a node, verify `NodeMoved`. Verify that structural hash short-circuits unchanged subtrees.
- `GestureClassifierTest`: Tap at (100,100) classified as TAP. Swipe from (100,100) to (100,500) classified as SWIPE_DOWN. Pinch gestures classified as PINCH.

**Learn module:**
- `NGramExtractorTest`: 5 actions with n=2 produces 4 bigrams. Verify hash stability. Empty input produces empty output.
- `SequenceMatcherTest`: Feed 10 sessions with a common 3-action subsequence occurring in 7 of them. Verify it is detected with count=7.
- `SequenceAlignerTest`: Identical sequences produce similarity=1.0. Completely different sequences produce similarity near 0. Sequences with one insertion produce similarity > 0.8.
- `PeriodicityEstimatorTest`: Timestamps every 7 days produce WEEKLY. Timestamps every 24 hours produce DAILY. Random timestamps produce IRREGULAR.
- `ConfidenceScorerTest`: Workflow with count=10, recent, consistent, terminal action, and weekly periodicity scores > 0.8. Workflow with count=2, old, inconsistent scores < 0.4.

**Suggest module:**
- `SuggestionThrottlerTest`: First suggestion passes. 4th suggestion in same hour is blocked. Dismissed workflow has 4-hour cooldown. Rejected workflow has 7-day cooldown.
- `UserPreferenceStoreTest`: Accept, dismiss, reject cycles update preferences correctly.

**Replay module:**
- `ReplayStateMachineTest`: All valid transitions succeed. Invalid transitions throw. Verify no reachable state has no exit (no deadlocks).
- `StepExecutorTest`: Mock @WatchAction method, verify invocation with correct parameters. Verify timeout handling.
- `ParameterResolverTest`: Fixed parameters resolve to default value. Expression "step[0].result.orderId" resolves from previous step result. Missing value returns null.
- `StrategySelectorTest`: When DirectInvocation can handle, it is selected. When it cannot, AccessibilityStrategy is tried next.

### 11.2 Integration Tests (androidTest)

- **End-to-end capture**: Launch a real Activity with a known View hierarchy. Initialize WatchLearn, trigger a snapshot, verify the stored event contains the expected view tree.
- **Gesture capture integration**: Programmatically inject touch events using `Instrumentation.sendPointerSync()`. Verify GestureEvent is captured with correct coordinates and type.
- **Navigation tracking**: Start ActivityA, navigate to ActivityB, verify NavigationEvent emitted.
- **Full workflow test**: Perform a 3-step sequence 4 times. Trigger pattern detection. Verify workflow is created. Accept suggestion. Execute replay. Verify all 3 steps are executed.
- **Crash recovery test**: Write events to file queue, kill the process (using `android.os.Process.killProcess`), restart, verify events are recovered.

### 11.3 UI Tests (Espresso / Compose Testing)

- Suggestion bottom sheet appears when triggered, displays correct title/description, and dismiss/accept buttons work.
- Compose `SuggestionBannerCompose` renders with correct content.
- Inline suggestion view animates in and out.

### 11.4 Performance Tests

- **Snapshot speed**: Benchmark `ViewTreeWalker.walk()` on hierarchies of 100, 500, 1000, 2000 nodes. Assert < 8ms for 500 nodes on a mid-range device.
- **Encoding speed**: Benchmark `EventEncoder.encode()` for snapshot events. Assert < 2ms per event.
- **File queue throughput**: Write 10,000 events, read them back. Measure MB/s.
- **Memory**: Capture 1000 events, verify heap increase < 5MB.
- **Pattern detection**: Run `findRecurringSequences` on 10,000 n-grams. Assert < 500ms.

### 11.5 Test Infrastructure

- Robolectric for unit tests that need Android framework classes without device/emulator
- `FakeClock` for deterministic time in tests
- `FakeStorageEngine` in-memory implementation for testing upper layers without SQLite
- `TestEventBus` that records all emitted events for assertions
- `TestCoroutineScope` and `TestDispatcher` for coroutine testing

---

## 12. Build Configuration

### 12.1 Version Catalog (`gradle/libs.versions.toml`)

```toml
[versions]
kotlin = "1.9.22"
agp = "8.2.2"
coroutines = "1.7.3"
compose-bom = "2024.02.00"
compose-compiler = "1.5.8"
lifecycle = "2.7.0"
room = "2.6.1"
navigation = "2.7.7"
appstartup = "1.1.1"
material = "1.11.0"
junit = "4.13.2"
junit5 = "5.10.1"
mockk = "1.13.8"
robolectric = "4.11.1"
turbine = "1.0.0"
espresso = "3.5.1"
benchmark = "1.2.3"
minSdk = "24"
compileSdk = "34"
targetSdk = "34"

[libraries]
kotlin-stdlib = { group = "org.jetbrains.kotlin", name = "kotlin-stdlib", version.ref = "kotlin" }
kotlin-reflect = { group = "org.jetbrains.kotlin", name = "kotlin-reflect", version.ref = "kotlin" }
coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "coroutines" }
coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }

compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-runtime = { group = "androidx.compose.runtime", name = "runtime" }
compose-foundation = { group = "androidx.compose.foundation", name = "foundation" }

lifecycle-runtime = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }
lifecycle-process = { group = "androidx.lifecycle", name = "lifecycle-process", version.ref = "lifecycle" }
lifecycle-viewmodel = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-ktx", version.ref = "lifecycle" }

navigation-fragment = { group = "androidx.navigation", name = "navigation-fragment-ktx", version.ref = "navigation" }
navigation-ui = { group = "androidx.navigation", name = "navigation-ui-ktx", version.ref = "navigation" }

appstartup = { group = "androidx.startup", name = "startup-runtime", version.ref = "appstartup" }

material = { group = "com.google.android.material", name = "material", version.ref = "material" }

# Testing
junit = { group = "junit", name = "junit", version.ref = "junit" }
junit5-api = { group = "org.junit.jupiter", name = "junit-jupiter-api", version.ref = "junit5" }
junit5-engine = { group = "org.junit.jupiter", name = "junit-jupiter-engine", version.ref = "junit5" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
mockk-android = { group = "io.mockk", name = "mockk-android", version.ref = "mockk" }
robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
coroutines-test-lib = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espresso" }

[plugins]
android-library = { id = "com.android.library", version.ref = "agp" }
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-kapt = { id = "org.jetbrains.kotlin.kapt", version.ref = "kotlin" }
maven-publish = { id = "com.vanniktech.maven.publish", version = "0.28.0" }
```

### 12.2 Root `build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}

allprojects {
    group = "com.watchlearn"
    version = "0.1.0-SNAPSHOT"
}

subprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
            freeCompilerArgs += listOf(
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                "-opt-in=kotlinx.coroutines.FlowPreview"
            )
        }
    }
}
```

### 12.3 Convention Plugin (`buildSrc/src/main/kotlin/watchlearn.android-library.gradle.kts`)

```kotlin
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true // For Robolectric
        }
    }
}
```

### 12.4 Module `build.gradle.kts` Examples

**`watchlearn-core/build.gradle.kts`:**
```kotlin
plugins {
    id("watchlearn.android-library")
    id("watchlearn.publishing")
}

android {
    namespace = "com.watchlearn.core"
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.process)
    implementation(libs.appstartup)

    testImplementation(libs.junit)
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test.lib)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
}
```

**`watchlearn-capture/build.gradle.kts`:**
```kotlin
plugins {
    id("watchlearn.android-library")
    id("watchlearn.publishing")
}

android {
    namespace = "com.watchlearn.capture"
}

dependencies {
    api(project(":watchlearn-core"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.coroutines.test.lib)
}
```

**`watchlearn-suggest/build.gradle.kts`:**
```kotlin
plugins {
    id("watchlearn.android-library")
    id("watchlearn.publishing")
}

android {
    namespace = "com.watchlearn.suggest"

    buildFeatures {
        viewBinding = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }
}

dependencies {
    api(project(":watchlearn-core"))
    implementation(project(":watchlearn-learn"))
    implementation(libs.material)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
}
```

**`demo-app/build.gradle.kts`:**
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.watchlearn.demo"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.watchlearn.demo"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }
}

dependencies {
    implementation(project(":watchlearn-core"))
    implementation(project(":watchlearn-capture"))
    implementation(project(":watchlearn-learn"))
    implementation(project(":watchlearn-suggest"))
    implementation(project(":watchlearn-replay"))
    implementation(project(":watchlearn-android"))

    implementation(libs.material)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
}
```

### 12.5 `settings.gradle.kts`

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolution {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "watchlearn-sdk"

include(":watchlearn-core")
include(":watchlearn-capture")
include(":watchlearn-learn")
include(":watchlearn-suggest")
include(":watchlearn-replay")
include(":watchlearn-android")
include(":demo-app")
```

### 12.6 Publishing Convention Plugin (`watchlearn.publishing.gradle.kts`)

```kotlin
plugins {
    id("com.vanniktech.maven.publish")
}

mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    coordinates(
        groupId = "com.watchlearn",
        artifactId = project.name,
        version = project.version.toString()
    )

    pom {
        name.set(project.name)
        description.set("WatchLearn SDK - On-device behavioral automation for Android")
        url.set("https://github.com/watchlearn/watchlearn-sdk")
        licenses {
            license {
                name.set("Apache License 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0")
            }
        }
        developers {
            developer {
                id.set("watchlearn")
                name.set("WatchLearn Team")
            }
        }
        scm {
            url.set("https://github.com/watchlearn/watchlearn-sdk")
            connection.set("scm:git:git://github.com/watchlearn/watchlearn-sdk.git")
        }
    }
}
```

### 12.7 ProGuard Rules (`consumer-rules.pro` shared by all modules)

```proguard
# Keep @WatchAction annotated methods
-keep @com.watchlearn.replay.annotation.WatchAction class * {
    @com.watchlearn.replay.annotation.WatchAction <methods>;
}
-keepclassmembers class * {
    @com.watchlearn.replay.annotation.WatchAction *;
}

# Keep @WatchParam annotations (needed for reflection)
-keep @com.watchlearn.replay.annotation.WatchParam class * { *; }

# Keep all public API classes
-keep class com.watchlearn.core.WatchLearn { *; }
-keep class com.watchlearn.core.WatchLearnConfig { *; }
-keep class com.watchlearn.core.WatchLearnConfig$Builder { *; }
-keep class com.watchlearn.core.model.** { *; }

# Keep callback interfaces
-keep interface com.watchlearn.core.On* { *; }
```

---

## Edge Cases and Failure Modes

### App Crashes Mid-Capture

The file queue uses per-entry CRC32 checksums and an append-only segment format. When the app crashes:
1. The current segment file may have a partial last entry (no CRC32 footer for the last bytes written).
2. On recovery, `FileQueueSegment.recover()` walks the segment: each entry is read by first reading its 4-byte length, then the payload, then the 4-byte CRC32. If the CRC32 does not match, that entry and all subsequent bytes in the segment are discarded.
3. The SQLite WAL journal provides crash safety for the database. Any uncommitted transaction is rolled back on next open.
4. In-flight events in the `EventBus` SharedFlow buffer are lost. This is acceptable because they had not been persisted yet, and the loss of a few hundred milliseconds of events is negligible.

### User Dismisses a Suggestion

1. `SuggestionThrottler` increments `consecutiveDismissals` for that workflow.
2. Cooldown is applied: `4 hours * 2^(dismissals - 1)`. So: 4h, 8h, 16h, 32h, 64h...
3. After 5 consecutive dismissals without any acceptance, the workflow status changes to `DEPRECATED` and is no longer suggested.
4. The workflow remains in the database for analysis but will not generate new suggestions unless the user manually re-enables it.

### Replay Fails Halfway Through

1. The `ReplayStateMachine` transitions to `RECOVERING`.
2. The `ReplayErrorHandler` evaluates the error type and step's retry policy.
3. For `ELEMENT_NOT_FOUND`: The engine waits (with exponential backoff: 1s, 2s, 4s) and retries up to `maxRetries`. If the element appears after a layout change, the retry succeeds. If retries are exhausted and the step is `isOptional=true`, the step is skipped. Otherwise, the replay aborts.
4. For `SCREEN_MISMATCH`: The engine attempts to navigate to the expected screen (synthesizing a navigation step using deep links or back navigation). If this fails, the replay aborts.
5. The `ReplayResult` records the exact step that failed, the error details, and the number of steps completed. This is stored in the `workflow_instances` table.
6. The workflow's `successRate` is updated. After 3 consecutive failures, the workflow is automatically paused and the user is notified.
7. Partial state changes from completed steps are NOT rolled back (rollback is listed as a future enhancement since it would require recording inverse actions).

### View Tree Changes Between Capture and Replay

This is the most common replay failure mode. Mitigations:

1. **Multi-strategy element matching**: The `ViewTreeMatcher` used during verification and replay does not require an exact tree match. It tries, in order:
   - Exact `ElementId` match (resource ID-based IDs are very stable)
   - Fuzzy match: same resource ID name even if XPath changed (e.g., button moved to different parent)
   - Content-based match: find an element with the same text/content description on the current screen
   - Structural similarity: find the closest element by Levenshtein distance on the XPath

2. **Workflow versioning**: When a workflow is re-detected with a slightly different step sequence (similarity > 0.7 but not identical), the workflow is updated (version incremented) with the new step definitions. Old steps that no longer match are flagged for re-verification.

3. **@WatchAction methods are immune**: Direct invocation strategy calls developer-defined methods, which handle their own element finding internally. This is the most robust replay approach and the primary reason the annotation system exists.

### Session/Lifecycle Edge Cases

- **Split-screen / multi-window**: The SDK only captures the foreground window. `ActivityLifecycleTracker` uses `Activity.isInMultiWindowMode()` to detect this and annotates events with a flag.
- **Configuration changes (rotation)**: A full snapshot is forced after any configuration change, since the entire view tree is rebuilt.
- **Process death and restart**: `SessionManager` detects cold starts by checking for a running session in the database. If the previous session was never closed (crash), it is closed with `ended_at_ms = last event timestamp + 1` and a new session is started.
- **Background to foreground**: A new session is NOT started. The existing session continues, with a `Lifecycle` event of type `APP_FOREGROUNDED` recording the gap. Pattern detection accounts for background gaps by excluding them from interval calculations.

### Memory Pressure

- The `SnapshotPool` has a hard cap of 500 pre-allocated nodes. If the tree exceeds this, nodes are allocated normally (and GC'd normally).
- The `RingBuffer` used for recent actions in the suggestion engine has a fixed size of 100 entries. Oldest are overwritten.
- SQLite connections use a single connection pool (size=1 for writes, size=4 for reads).
- If `ActivityManager.getMemoryInfo()` reports `lowMemory = true`, the SDK pauses capture temporarily and logs a warning.

---

## Module Dependency Graph

```
watchlearn-android
    ├── watchlearn-capture
    │     └── watchlearn-core
    ├── watchlearn-suggest
    │     ├── watchlearn-core
    │     └── watchlearn-learn
    │           └── watchlearn-core
    ├── watchlearn-replay
    │     └── watchlearn-core
    └── watchlearn-core

demo-app
    ├── watchlearn-android
    ├── watchlearn-core
    ├── watchlearn-capture
    ├── watchlearn-learn
    ├── watchlearn-suggest
    └── watchlearn-replay
```

`watchlearn-core` has zero dependencies on other WatchLearn modules. Each higher-level module depends only on `watchlearn-core` (and potentially one peer module). `watchlearn-android` is the integration layer that wires everything together for an Android app.

---

## Implementation Sequencing

**Phase 1 (Weeks 1-3): Foundation**
1. `watchlearn-core`: Models, EventBus, EventEncoder/Decoder, FileQueue, SqliteStorageEngine
2. Unit tests for all core components

**Phase 2 (Weeks 4-6): Capture**
3. `watchlearn-capture`: ViewTreeWalker (traditional Views), StableIdGenerator, DeltaComputer, GestureInterceptor, NavigationTracker
4. `watchlearn-android`: ActivityLifecycleTracker, SessionManager, WatchLearnInitializer
5. Integration tests with a real Activity

**Phase 3 (Weeks 7-9): Learning**
6. `watchlearn-learn`: NGramExtractor, SequenceMatcher, FrequencyAnalyzer, TemporalPatternDetector, ConfidenceScorer, WorkflowExtractor
7. End-to-end test: capture actions -> detect pattern -> extract workflow

**Phase 4 (Weeks 10-11): Suggestions**
8. `watchlearn-suggest`: SuggestionEngine, SuggestionThrottler, UserPreferenceStore, SuggestionBottomSheet
9. UI tests for suggestion display

**Phase 5 (Weeks 12-14): Replay**
10. `watchlearn-replay`: ActionRegistry, RegistryScanner, DirectInvocationStrategy, ReplayStateMachine, ReplayEngine, ReplayErrorHandler
11. AccessibilityStrategy and IntentStrategy
12. Full end-to-end test: capture -> learn -> suggest -> accept -> replay

**Phase 6 (Weeks 15-16): Compose and Polish**
13. Compose support: SemanticsTreeReader, ComposeNodeMapper, Compose suggestion UI, Modifier extensions
14. Demo app
15. Performance benchmarks
16. Documentation, README, publishing setup

---

### Critical Files for Implementation

- `/watchlearn-core/src/main/kotlin/com/watchlearn/core/WatchLearn.kt` — The singleton entry point that wires all modules together; defines the entire public API surface
- `/watchlearn-core/src/main/kotlin/com/watchlearn/core/storage/FileQueue.kt` — The crash-safe persistence layer; correctness here is critical for data integrity across all failure modes
- `/watchlearn-capture/src/main/kotlin/com/watchlearn/capture/viewtree/ViewTreeWalker.kt` — The core capture algorithm; performance budget of 8ms on main thread makes this the most performance-sensitive file
- `/watchlearn-learn/src/main/kotlin/com/watchlearn/learn/sequence/SequenceMatcher.kt` — The n-gram pattern detection algorithm; the intelligence of the entire SDK depends on this working correctly
- `/watchlearn-replay/src/main/kotlin/com/watchlearn/replay/ReplayEngine.kt` — The execution orchestrator with its state machine, strategy selection, error recovery, and timeout handling; the most complex control flow in the project