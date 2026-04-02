package com.zz143.core.consent

/**
 * Controls what data the SDK is allowed to capture and process.
 *
 * - [FULL] — Capture events, learn patterns, show suggestions, fire analytics callbacks.
 * - [ANALYTICS_ONLY] — Fire analytics callbacks only. No event capture or pattern learning.
 * - [NONE] — SDK is completely dormant. No data is captured, processed, or reported.
 */
enum class ConsentLevel {
    FULL,
    ANALYTICS_ONLY,
    NONE
}
