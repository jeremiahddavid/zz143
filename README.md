<p align="center">
  <h1 align="center">zz143</h1>
  <p align="center">
    <strong>Session replay that replays back.</strong>
    <br />
    The SDK that watches how users interact with your app, learns their patterns, and automates repetitive workflows.
  </p>
</p>

<p align="center">
  <a href="#"><img src="https://img.shields.io/badge/platform-Android-brightgreen" alt="Platform" /></a>
  <a href="#"><img src="https://img.shields.io/badge/min%20SDK-24-blue" alt="Min SDK" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-Apache%202.0-orange" alt="License" /></a>
  <a href="#"><img src="https://img.shields.io/badge/version-0.1.0--alpha01-purple" alt="Version" /></a>
</p>

<p align="center">
  <!-- TODO: Replace with actual demo GIF -->
  <img src="docs/demo.gif" alt="zz143 demo" width="300" />
</p>

---

## What is zz143?

**20+ SDKs** can record what users do in your app (FullStory, PostHog, UXCam). They replay sessions on web dashboards for analytics.

**zz143 replays actions back *inside* your app.** It observes user behavior, detects recurring patterns, and offers to automate them.

```
Your app user orders a latte every Monday morning:
  1. Open menu
  2. Tap "Latte"
  3. Select "Large"
  4. Tap "Express delivery"
  5. Confirm order

After 3 repetitions, zz143 suggests:
  "Repeat your usual order? (5 steps, ~30 seconds saved)"
  [Yes] [Not now] [Don't ask again]
```

## Quick Start

### 1. Add the dependency

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.zz143:zz143-android:0.1.0-alpha01")
}
```

### 2. Initialize (3 lines)

```kotlin
// Application.onCreate()
ZZ143.init(this) {
    suggestionsEnabled(true)
    debugLogging(true)
}
ZZ143.startCapturing()
```

### 3. Register automatable actions

```kotlin
class OrderService {
    @WatchAction(type = "add_to_cart", description = "Add item to cart")
    suspend fun addToCart(
        @WatchParam(name = "itemId") itemId: String,
        @WatchParam(name = "quantity") quantity: Int = 1
    ): CartResult {
        return cartApi.add(itemId, quantity)
    }

    @WatchAction(type = "checkout", description = "Complete checkout")
    suspend fun checkout(
        @WatchParam(name = "deliveryMethod") delivery: String
    ): OrderResult {
        return cartApi.checkout(delivery)
    }
}

// Register in your Activity/Fragment
ZZ143.registerActions(orderService)
```

That's it. zz143 will automatically:
- Record user interactions (wireframe snapshots, not screenshots)
- Detect when action sequences repeat
- Suggest automating the workflow
- Execute it on confirmation

## How It Works

```
OBSERVE          LEARN            SUGGEST          EXECUTE
  |                |                |                |
  v                v                v                v
Record user     Detect            Show bottom     Call your
actions as      recurring         sheet: "Do      @WatchAction
wireframe       n-gram            this again?"    methods
snapshots +     sequences                         directly
semantic        with temporal
events          analysis
```

### Architecture

```
+-----------------------------------------------+
|                YOUR APP                         |
|                                                 |
|  @WatchAction fun addToCart(...)                |
|  @WatchAction fun checkout(...)                 |
|                                                 |
+------------------+----------------------------+
                   |
          +--------v--------+
          |   zz143 SDK     |
          |                 |
          | [Capture]       |  Wireframe snapshots + gestures
          | [Learn]         |  N-gram patterns + temporal detection
          | [Suggest]       |  Throttled suggestion UI
          | [Replay]        |  Direct invocation + state machine
          |                 |
          | [Event Bus]     |  SharedFlow, binary encoding
          | [File Queue]    |  Crash-safe persistence
          | [SQLite]        |  Pattern storage + analysis
          +-----------------+
```

## Features

| Feature | Description |
|---------|-------------|
| **Wireframe capture** | Records view tree structure, not screenshots. Privacy-safe by default. |
| **Incremental deltas** | Only records what changed between snapshots (like rrweb). |
| **N-gram pattern detection** | Finds recurring action sequences using frequency analysis. |
| **Temporal patterns** | Detects daily/weekly/monthly workflows via autocorrelation. |
| **Smith-Waterman alignment** | Fuzzy matching for workflows with minor variations. |
| **Confidence scoring** | 5-signal composite score (frequency, recency, consistency, completeness, temporal). |
| **Suggestion throttling** | 3/hour, 10/day limits with exponential backoff on dismissal. |
| **Replay state machine** | 9-state FSM with retry, skip, and error recovery. |
| **Strategy pattern** | Direct invocation, Accessibility Service, or Intent-based replay. |
| **@WatchAction annotations** | Type-safe action registration with parameter schemas. |
| **On-device only** | All data stays on the phone. No cloud dependency. |
| **Binary encoding** | 3-5x smaller than JSON event payloads. |
| **Crash-safe storage** | File-backed queue with CRC32 checksums survives app crashes. |
| **Compose support** | `Modifier.watchAction()` and semantics tree capture. |

## Modules

| Module | Description |
|--------|-------------|
| `zz143-core` | Data models, event bus, storage engine, configuration |
| `zz143-capture` | View tree walker, gesture interceptor, delta computation |
| `zz143-learn` | Pattern detection, sequence analysis, temporal detection |
| `zz143-suggest` | Suggestion UI (bottom sheet, notification, inline) |
| `zz143-replay` | Action registry, execution engine, state machine |
| `zz143-android` | Android lifecycle hooks, Compose integration |

## Privacy

zz143 is privacy-first by design:

- **Wireframes, not screenshots.** We capture the view tree structure, never pixel data.
- **Text masked by default.** `captureTextValues = false` means only hashed values are stored.
- **Passwords auto-redacted.** Secure text fields are never captured.
- **On-device only.** No data leaves the phone by default. No cloud, no analytics server.
- **Sensitive screens.** Mark entire screens to skip capture completely.
- **User control.** Users can dismiss, snooze, or permanently reject any suggestion.

## Use Cases

- **E-commerce:** "Reorder your usual" for any app, without building it from scratch
- **Enterprise:** Automate the 8-step data entry workflow your sales reps do 50x/day
- **Healthcare:** Pre-fill the same patient intake form filled identically each visit
- **Finance:** Automate weekly transfers to the same accounts
- **Food delivery:** "Same lunch order as yesterday?"

## Roadmap

- [x] Core event bus and storage
- [x] View tree capture with stable element IDs
- [x] N-gram pattern detection engine
- [x] Replay state machine with strategy pattern
- [ ] Compose semantics tree integration
- [ ] iOS SDK (Swift)
- [ ] React Native wrapper
- [ ] Flutter wrapper
- [ ] Cloud dashboard (pattern analytics)
- [ ] MCP server integration

## Contributing

We welcome contributions! See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## License

Apache 2.0. See [LICENSE](LICENSE).
