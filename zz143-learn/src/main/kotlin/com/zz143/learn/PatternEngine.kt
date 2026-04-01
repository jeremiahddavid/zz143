package com.zz143.learn

import com.zz143.core.model.*
import com.zz143.core.id.UlidGenerator
import com.zz143.learn.scoring.ConfidenceScorer
import com.zz143.learn.scoring.PatternSignals
import com.zz143.learn.sequence.ActionToken
import com.zz143.learn.sequence.NGramExtractor
import com.zz143.learn.sequence.SequenceAligner
import com.zz143.learn.temporal.TemporalPatternDetector

class PatternEngine(
    private val minOccurrences: Int = 3,
    private val minConfidence: Float = 0.6f
) {
    private val ngramExtractor = NGramExtractor()
    private val aligner = SequenceAligner()
    private val temporalDetector = TemporalPatternDetector()
    private val confidenceScorer = ConfidenceScorer()

    // In-memory ngram counts (flushed to DB periodically)
    private val ngramCounts = mutableMapOf<String, NGramRecord>()

    data class NGramRecord(
        val hash: String,
        val actionTypes: List<String>,
        val size: Int,
        val screens: List<String>,
        var count: Int = 1,
        var firstSeenMs: Long = System.currentTimeMillis(),
        var lastSeenMs: Long = System.currentTimeMillis(),
        val timestamps: MutableList<Long> = mutableListOf()
    )

    fun processActions(actions: List<SemanticAction>, screenIds: List<ScreenId>) {
        if (actions.isEmpty()) return

        val tokens = actions.mapIndexed { i, action ->
            ActionToken(
                actionType = action.actionType,
                screenId = screenIds.getOrElse(i) { ScreenId("unknown") }.value,
                timestampMs = System.currentTimeMillis()
            )
        }

        val ngrams = ngramExtractor.extract(tokens)
        val now = System.currentTimeMillis()

        for (ngram in ngrams) {
            val existing = ngramCounts[ngram.hash]
            if (existing != null) {
                existing.count++
                existing.lastSeenMs = now
                existing.timestamps.add(now)
            } else {
                ngramCounts[ngram.hash] = NGramRecord(
                    hash = ngram.hash,
                    actionTypes = ngram.actionTypes,
                    size = ngram.size,
                    screens = ngram.screens,
                    firstSeenMs = now,
                    lastSeenMs = now,
                    timestamps = mutableListOf(now)
                )
            }
        }
    }

    fun findWorkflows(): List<Workflow> {
        // Filter by minimum occurrences, prefer longer sequences
        val candidates = ngramCounts.values
            .filter { it.count >= minOccurrences }
            .sortedWith(compareByDescending<NGramRecord> { it.count }.thenByDescending { it.size })

        // Remove subsequences (keep longer patterns)
        val filtered = removeSubsequences(candidates)

        return filtered.mapNotNull { record ->
            val frequency = temporalDetector.detect(record.timestamps)
                ?: WorkflowFrequency(FrequencyType.IRREGULAR, null, null, null, 0f)

            val intervalVariance = computeIntervalVariance(record.timestamps)

            val terminals = setOf("checkout", "submit", "confirm", "save", "send", "complete", "done", "pay")
            val endsWithTerminal = record.actionTypes.lastOrNull()?.let { last ->
                terminals.any { last.contains(it, ignoreCase = true) }
            } ?: false

            val confidence = confidenceScorer.score(PatternSignals(
                count = record.count,
                lastSeenMs = record.lastSeenMs,
                intervalVariance = intervalVariance,
                endsWithTerminal = endsWithTerminal,
                temporalConfidence = frequency.confidence
            ))

            if (confidence < minConfidence) return@mapNotNull null

            val steps = record.actionTypes.mapIndexed { index, actionType ->
                val screen = record.screens.getOrElse(index) { record.screens.firstOrNull() ?: "unknown" }
                WorkflowStep(
                    stepIndex = index,
                    action = SemanticAction(
                        actionType = actionType.substringBefore("@"),
                        actionSource = ActionSource.INFERRED,
                        targetElementId = null,
                        parameters = emptyMap()
                    ),
                    expectedScreenId = ScreenId(screen),
                    parameters = emptyList()
                )
            }

            val name = generateWorkflowName(record.actionTypes)

            Workflow(
                workflowId = UlidGenerator.next(),
                name = name,
                description = "${record.count} occurrences, ${record.size} steps",
                steps = steps,
                frequency = frequency,
                confidenceScore = confidence,
                firstSeenMs = record.firstSeenMs,
                lastSeenMs = record.lastSeenMs,
                executionCount = record.count,
                automationCount = 0,
                successRate = 0f,
                status = WorkflowStatus.DETECTED
            )
        }
    }

    fun matchesPrefix(recentActions: List<String>, workflow: Workflow): PrefixMatch? {
        val workflowTypes = workflow.steps.map { it.action.actionType }
        if (recentActions.size > workflowTypes.size) return null

        val matchLength = recentActions.indices.count { i ->
            i < workflowTypes.size && recentActions[i] == workflowTypes[i]
        }

        if (matchLength < 2) return null

        return PrefixMatch(
            matchLength = matchLength,
            remainingSteps = workflowTypes.size - matchLength,
            workflow = workflow
        )
    }

    data class PrefixMatch(
        val matchLength: Int,
        val remainingSteps: Int,
        val workflow: Workflow
    )

    private fun removeSubsequences(candidates: List<NGramRecord>): List<NGramRecord> {
        val result = mutableListOf<NGramRecord>()
        val processed = mutableSetOf<String>()

        for (candidate in candidates) {
            if (candidate.hash in processed) continue

            val isSubsequence = candidates.any { other ->
                other.hash != candidate.hash &&
                other.size > candidate.size &&
                other.count >= minOccurrences &&
                candidate.actionTypes.joinToString("|") in other.actionTypes.joinToString("|")
            }

            if (!isSubsequence) {
                result.add(candidate)
            }
            processed.add(candidate.hash)
        }

        return result
    }

    private fun computeIntervalVariance(timestamps: List<Long>): Float {
        if (timestamps.size < 2) return 1f
        val sorted = timestamps.sorted()
        val intervals = (1 until sorted.size).map { (sorted[it] - sorted[it - 1]).toFloat() }
        val mean = intervals.average().toFloat()
        if (mean == 0f) return 0f
        val variance = intervals.map { (it - mean) * (it - mean) }.average().toFloat()
        val cv = kotlin.math.sqrt(variance) / mean // coefficient of variation
        return cv.coerceIn(0f, 1f)
    }

    private fun generateWorkflowName(actionTypes: List<String>): String {
        val simplified = actionTypes.map { it.substringBefore("@").replace("_", " ") }
        return when {
            simplified.size <= 3 -> simplified.joinToString(" → ")
            else -> "${simplified.first()} → ... → ${simplified.last()} (${simplified.size} steps)"
        }
    }
}
