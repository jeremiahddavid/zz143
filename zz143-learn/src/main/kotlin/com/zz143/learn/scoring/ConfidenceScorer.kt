package com.zz143.learn.scoring

import kotlin.math.exp

class ConfidenceScorer(
    private val weights: ScoringWeights = ScoringWeights.default()
) {
    fun score(signals: PatternSignals): Float {
        val frequencyScore = sigmoid(signals.count.toFloat(), center = 5f, steepness = 0.5f)

        val daysSinceLastSeen = (System.currentTimeMillis() - signals.lastSeenMs) / 86400000.0f
        val recencyScore = exp(-0.05f * daysSinceLastSeen)

        val consistencyScore = 1.0f - signals.intervalVariance.coerceIn(0f, 1f)

        val completenessScore = if (signals.endsWithTerminal) 1.0f else 0.5f

        val temporalScore = signals.temporalConfidence

        val composite = weights.frequency * frequencyScore +
            weights.recency * recencyScore +
            weights.consistency * consistencyScore +
            weights.completeness * completenessScore +
            weights.temporal * temporalScore

        return composite.coerceIn(0f, 1f)
    }

    private fun sigmoid(x: Float, center: Float, steepness: Float): Float {
        return 1f / (1f + exp(-steepness * (x - center)))
    }
}

data class PatternSignals(
    val count: Int,
    val lastSeenMs: Long,
    val intervalVariance: Float,
    val endsWithTerminal: Boolean,
    val temporalConfidence: Float
)

data class ScoringWeights(
    val frequency: Float = 0.30f,
    val recency: Float = 0.20f,
    val consistency: Float = 0.20f,
    val completeness: Float = 0.15f,
    val temporal: Float = 0.15f
) {
    companion object {
        fun default() = ScoringWeights()
    }
}
