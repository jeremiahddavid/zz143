# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- **Analytics callbacks**: `AnalyticsListener` interface with 7 events (`onWorkflowDetected`, `onSuggestionShown/Accepted/Dismissed/Rejected`, `onReplayStarted/Completed`). Thread-safe `AnalyticsDispatcher` using `CopyOnWriteArrayList`.
- **Consent framework**: `ConsentLevel` enum (`FULL`, `ANALYTICS_ONLY`, `NONE`). `ConsentManager` with persistence to `SharedPreferences`. `ZZ143.setConsent()` / `getConsentLevel()` public API. Capture and analytics gated behind consent checks.
- **Maven Central publishing**: Switched to `com.vanniktech.maven.publish` plugin with `SonatypeHost.CENTRAL_PORTAL` for correct Central Portal upload.
- **30 new unit tests** for consent management and analytics dispatcher (205 total across 12 test classes)

### Changed

- `ZZ143Config` now includes `requireExplicitConsent: Boolean = false`
- `ZZ143.trackAction()` and `startCapturing()` are now gated behind consent — no-op when consent is `NONE` or `ANALYTICS_ONLY`

## [0.1.0-alpha01] - 2026-04-01

Initial alpha release. This is an early prototype — APIs will change, not production-ready.

### Added

- **zz143-core**: Event bus (SharedFlow), binary TLV encoder, crash-safe file queue (CRC32 checksums), SQLite storage (8 tables), workflow persistence, ULID ID generation, configuration DSL (30+ parameters), orchestration layer connecting all pipeline stages
- **zz143-capture**: View tree walker with stable element IDs, incremental delta computation, gesture interception, gesture classification
- **zz143-learn**: N-gram extraction, Smith-Waterman fuzzy alignment, temporal pattern detection, 5-signal confidence scoring, parameter-aware workflow learning (fixed vs variable)
- **zz143-suggest**: Suggestion engine with prefix matching and time-based triggers, throttling with exponential backoff, user preference tracking, Material 3 Compose bottom sheet
- **zz143-replay**: `@WatchAction`/`@WatchParam`/`@WatchGuard` annotations, reflection-based action registry with name-based parameter mapping, 9-state replay state machine, strategy pattern for execution
- **zz143-android**: App Startup initializer, Activity lifecycle tracking, session management, Compose extensions
- **demo-app**: 3-tab demo (Coffee Shop with customization, Expense Report with form pre-fill, Settings with preference learning)
- **153 unit tests** across 10 test classes covering core components

### Known Issues

- Manual `trackAction()` calls required (no automatic UI capture)
- Not published to Maven Central (local dependency only)
- Tested on one emulator only (API 36)
- No integration tests for full pipeline
- No performance benchmarks
- Reflection-based handler invocation may need tuning with R8/ProGuard
