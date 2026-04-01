package com.zz143.learn.scoring

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ConfidenceScorerTest {

    private val scorer = ConfidenceScorer()

    @Test
    fun scoreIsAlwaysBetweenZeroAndOne() {
        val signals = listOf(
            PatternSignals(count = 0, lastSeenMs = 0L, intervalVariance = 0f, endsWithTerminal = false, temporalConfidence = 0f),
            PatternSignals(count = 100, lastSeenMs = System.currentTimeMillis(), intervalVariance = 0f, endsWithTerminal = true, temporalConfidence = 1f),
            PatternSignals(count = 1, lastSeenMs = System.currentTimeMillis() - 365 * 86400_000L, intervalVariance = 1f, endsWithTerminal = false, temporalConfidence = 0f),
            PatternSignals(count = 50, lastSeenMs = System.currentTimeMillis(), intervalVariance = 0.5f, endsWithTerminal = true, temporalConfidence = 0.5f)
        )

        for (signal in signals) {
            val score = scorer.score(signal)

            assertThat(score).isAtLeast(0f)
            assertThat(score).isAtMost(1f)
        }
    }

    @Test
    fun highFrequencyAndRecentPatternProducesHighScore() {
        val signals = PatternSignals(
            count = 100,
            lastSeenMs = System.currentTimeMillis(),
            intervalVariance = 0f,
            endsWithTerminal = true,
            temporalConfidence = 1.0f
        )

        val score = scorer.score(signals)

        assertThat(score).isGreaterThan(0.8f)
    }

    @Test
    fun oldPatternProducesLowScore() {
        // Pattern last seen 365 days ago with low frequency
        val signals = PatternSignals(
            count = 1,
            lastSeenMs = System.currentTimeMillis() - 365L * 86400_000L,
            intervalVariance = 1.0f,
            endsWithTerminal = false,
            temporalConfidence = 0.0f
        )

        val score = scorer.score(signals)

        assertThat(score).isLessThan(0.4f)
    }

    @Test
    fun terminalPatternScoresHigherThanNonTerminal() {
        val baseSignals = PatternSignals(
            count = 10,
            lastSeenMs = System.currentTimeMillis(),
            intervalVariance = 0.3f,
            endsWithTerminal = false,
            temporalConfidence = 0.5f
        )

        val terminalSignals = baseSignals.copy(endsWithTerminal = true)

        val nonTerminalScore = scorer.score(baseSignals)
        val terminalScore = scorer.score(terminalSignals)

        assertThat(terminalScore).isGreaterThan(nonTerminalScore)
    }

    @Test
    fun defaultWeightsSumToOne() {
        val weights = ScoringWeights.default()

        val sum = weights.frequency + weights.recency + weights.consistency +
            weights.completeness + weights.temporal

        assertThat(sum).isWithin(0.001f).of(1.0f)
    }

    @Test
    fun moreFrequentPatternScoresHigherAllElseEqual() {
        val now = System.currentTimeMillis()

        val lowFreq = PatternSignals(
            count = 1,
            lastSeenMs = now,
            intervalVariance = 0.5f,
            endsWithTerminal = true,
            temporalConfidence = 0.5f
        )

        val highFreq = lowFreq.copy(count = 50)

        val lowScore = scorer.score(lowFreq)
        val highScore = scorer.score(highFreq)

        assertThat(highScore).isGreaterThan(lowScore)
    }

    @Test
    fun moreRecentPatternScoresHigherAllElseEqual() {
        val now = System.currentTimeMillis()

        val oldSignals = PatternSignals(
            count = 10,
            lastSeenMs = now - 90L * 86400_000L,
            intervalVariance = 0.3f,
            endsWithTerminal = true,
            temporalConfidence = 0.5f
        )

        val recentSignals = oldSignals.copy(lastSeenMs = now)

        val oldScore = scorer.score(oldSignals)
        val recentScore = scorer.score(recentSignals)

        assertThat(recentScore).isGreaterThan(oldScore)
    }
}
