# Contributing to zz143

Thank you for your interest in contributing to zz143! This guide will help you get started.

zz143 is an Android SDK that observes user behavior, detects recurring patterns, and offers to automate repetitive workflows -- all on-device. We welcome bug reports, feature requests, documentation improvements, and code contributions.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Project Structure](#project-structure)
- [Coding Style](#coding-style)
- [Testing](#testing)
- [Pull Request Process](#pull-request-process)
- [Commit Conventions](#commit-conventions)
- [Issue Templates](#issue-templates)
- [Code of Conduct](#code-of-conduct)

## Prerequisites

Before you begin, ensure you have the following installed:

- **JDK 17** (Eclipse Temurin or Azul Zulu recommended)
- **Android SDK 34** (API level 34) with build tools
- **Kotlin 2.0** (managed via the Gradle version catalog)
- **Android Studio** Ladybug or later (recommended but not required)
- **Git** 2.30+

## Getting Started

1. **Fork** the repository on GitHub.
2. **Clone** your fork:
   ```bash
   git clone https://github.com/<your-username>/zz143.git
   cd zz143
   ```
3. **Build** the project:
   ```bash
   ./gradlew build
   ```
4. **Run tests**:
   ```bash
   ./gradlew test
   ```
5. **Run a specific module's tests**:
   ```bash
   ./gradlew :zz143-core:test
   ./gradlew :zz143-capture:test
   ./gradlew :zz143-learn:test
   ```
6. **Run the demo app** (requires an emulator or connected device):
   ```bash
   ./gradlew :demo-app:installDebug
   ```

## Project Structure

zz143 is organized into 7 Gradle modules:

| Module | Purpose |
|--------|---------|
| `zz143-core` | Data models, event bus (SharedFlow), binary encoding, storage engine (SQLite + file queue), configuration DSL, threading utilities |
| `zz143-capture` | View tree walker, Compose semantics mapper, gesture interceptor, snapshot scheduler, delta computation, sensitivity classification |
| `zz143-learn` | N-gram pattern extraction, sequence alignment (Smith-Waterman), temporal pattern detection (periodicity, circular histograms), confidence scoring, workflow extraction |
| `zz143-suggest` | Suggestion UI (bottom sheet, notification, inline banner), throttling (3/hour, 10/day), cooldown tracking, user preference store, trigger conditions |
| `zz143-replay` | Action registry (`@WatchAction`), execution engine, replay state machine (9-state FSM), strategy pattern (direct invocation, accessibility, intent), error handling and retry |
| `zz143-android` | Android lifecycle hooks (Activity, Fragment, Process), Compose integration (`Modifier.watchAction()`), accessibility service, session management |
| `demo-app` | Sample app demonstrating SDK integration |

Dependencies flow in one direction: `android` -> `suggest` / `replay` -> `learn` -> `capture` -> `core`.

## Coding Style

This project follows the official Kotlin coding conventions:

- **`kotlin.code.style=official`** is set in `gradle.properties`.
- **4-space indentation** for all Kotlin, Java, and XML files.
- **No wildcard imports.** Use explicit imports only.
- **Trailing commas** are encouraged in multi-line parameter lists.
- **Explicit return types** on all public API functions and properties.
- **`internal` visibility** for anything not part of the public SDK API.
- Follow existing patterns in the codebase. When in doubt, look at neighboring files.

Resource naming in the `zz143-suggest` module uses the `zz143_` prefix to avoid collisions with host app resources.

## Testing

### Directory Structure

Tests mirror the main source structure:

```
zz143-<module>/
  src/
    main/kotlin/com/zz143/<module>/       # Production code
    test/kotlin/com/zz143/<module>/       # Unit tests
    androidTest/kotlin/com/zz143/<module>/  # Instrumented tests (if any)
```

### Naming Conventions

- Test classes: `<ClassName>Test.kt` (e.g., `EventBusTest.kt`)
- Test methods: use backtick-quoted descriptive names:
  ```kotlin
  @Test
  fun `emits events to all active subscribers`() { ... }
  ```

### Assertions

Use [Google Truth](https://truth.dev/) for assertions:

```kotlin
import com.google.common.truth.Truth.assertThat

assertThat(result).isEqualTo(expected)
assertThat(list).hasSize(3)
assertThat(value).isNull()
```

### Running Tests

```bash
# All tests
./gradlew test

# Single module
./gradlew :zz143-learn:test

# Single test class
./gradlew :zz143-core:test --tests "com.zz143.core.event.EventBusTest"
```

## Pull Request Process

1. **Fork** the repository and create a feature branch from `main`:
   ```bash
   git checkout -b feat/your-feature-name
   ```
2. **Make your changes.** Keep PRs focused on a single concern.
3. **Add or update tests** for any new or changed behavior.
4. **Run the full test suite** and ensure it passes:
   ```bash
   ./gradlew test
   ```
5. **Follow the commit conventions** described below.
6. **Push** your branch and open a pull request against `main`.
7. **Fill out the PR template.** Describe what changed, why, and how to test it.
8. **Respond to review feedback.** Maintainers may request changes before merging.

### PR Guidelines

- Keep changes small and incremental when possible.
- Do not mix refactoring with feature work in the same PR.
- Ensure no new warnings are introduced.
- Public API changes require a discussion in a GitHub issue first.

## Commit Conventions

This project uses [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<optional scope>): <description>

[optional body]

[optional footer(s)]
```

### Types

| Type | When to Use |
|------|-------------|
| `feat:` | A new feature or capability |
| `fix:` | A bug fix |
| `docs:` | Documentation-only changes |
| `test:` | Adding or updating tests (no production code change) |
| `chore:` | Build scripts, CI config, dependency updates, tooling |
| `refactor:` | Code restructuring without behavior change |
| `perf:` | Performance improvements |

### Examples

```
feat(learn): add Smith-Waterman sequence alignment for fuzzy matching
fix(replay): prevent state machine from skipping the Verifying state
docs: update README quick start example
test(core): add crash recovery tests for FileQueue
chore: bump Kotlin to 2.0.10
```

### Rules

- Use the **imperative mood** in the description ("add", not "added" or "adds").
- Keep the first line under **72 characters**.
- Reference related issues in the footer: `Closes #42`.

## Issue Templates

When reporting bugs or requesting features, please use the provided GitHub issue templates:

- **Bug Report** -- for crashes, incorrect behavior, or regressions.
- **Feature Request** -- for new capabilities or enhancements.

These templates are located in `.github/ISSUE_TEMPLATE/` and will appear automatically when you create a new issue.

## Code of Conduct

This project is governed by the [Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md). By participating, you agree to uphold this code. Please report unacceptable behavior to the project maintainers.

---

Thank you for helping make zz143 better!
