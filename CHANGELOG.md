# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0-alpha01] - 2026-04-01

### Added

- **zz143-core**: Event bus (SharedFlow-based), binary TLV event encoder, crash-safe file queue with CRC32 checksums, SQLite storage engine (8 tables), ULID-based ID generation, configuration DSL with 30+ parameters
- **zz143-capture**: Recursive view tree walker with stable element IDs (resource ID / content description / XPath fallback), incremental delta computation with structural hash optimization, gesture interception via Window.Callback delegation, gesture classification (tap, swipe, scroll, long press)
- **zz143-learn**: N-gram sequence extraction (2-10 grams), Smith-Waterman local alignment for fuzzy workflow matching with Jaccard pre-filter, temporal pattern detection via hour/day histograms, 5-signal composite confidence scoring (frequency, recency, consistency, completeness, temporal)
- **zz143-suggest**: Suggestion engine with prefix matching and time-based triggers, throttling with exponential backoff (3/hour, 10/day limits), user preference tracking with dismissal/rejection handling, Material 3 Compose bottom sheet UI
- **zz143-replay**: `@WatchAction`, `@WatchParam`, `@WatchGuard` annotations, reflection-based action registry, 9-state replay state machine with validated transitions, strategy pattern (direct invocation, accessibility, intent), execution context with parameter resolution, retry with exponential backoff
- **zz143-android**: App Startup auto-initialization, Activity lifecycle tracking with automatic capture attachment, session management via ProcessLifecycleOwner, Compose extensions (`Modifier.watchAction()`, `Modifier.watchSensitive()`)
- **demo-app**: Coffee shop sample app with product list, cart, checkout flow, and 4 `@WatchAction` annotated methods
