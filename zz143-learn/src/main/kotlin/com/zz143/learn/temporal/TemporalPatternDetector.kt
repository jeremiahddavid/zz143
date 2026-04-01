package com.zz143.learn.temporal

import com.zz143.core.model.FrequencyType
import com.zz143.core.model.WorkflowFrequency
import java.util.Calendar
import kotlin.math.max

class TemporalPatternDetector {

    fun detect(timestamps: List<Long>): WorkflowFrequency? {
        if (timestamps.size < 3) return null

        val hourHistogram = IntArray(24)
        val dayHistogram = IntArray(7)

        for (ts in timestamps) {
            val cal = Calendar.getInstance().apply { timeInMillis = ts }
            hourHistogram[cal.get(Calendar.HOUR_OF_DAY)]++
            dayHistogram[(cal.get(Calendar.DAY_OF_WEEK) + 5) % 7]++ // Mon=0..Sun=6
        }

        val peakHour = hourHistogram.indices.maxByOrNull { hourHistogram[it] } ?: 0
        val hourConcentration = hourHistogram[peakHour].toFloat() / timestamps.size
        val hasTimePeak = hourConcentration > 0.6f

        val peakDay = dayHistogram.indices.maxByOrNull { dayHistogram[it] } ?: 0
        val dayConcentration = dayHistogram[peakDay].toFloat() / timestamps.size
        val hasDayPeak = dayConcentration > 0.5f

        // Compute intervals
        val sorted = timestamps.sorted()
        val intervals = (1 until sorted.size).map { sorted[it] - sorted[it - 1] }
        val avgIntervalMs = if (intervals.isNotEmpty()) intervals.average().toLong() else 0L

        val frequencyType = classifyFrequency(avgIntervalMs)
        val confidence = max(hourConcentration, dayConcentration)

        return WorkflowFrequency(
            type = frequencyType,
            intervalMs = avgIntervalMs,
            dayOfWeek = if (hasDayPeak) peakDay + 1 else null,
            hourOfDay = if (hasTimePeak) peakHour else null,
            confidence = confidence
        )
    }

    private fun classifyFrequency(avgIntervalMs: Long): FrequencyType {
        val hours = avgIntervalMs / 3600_000.0
        return when {
            hours < 4 -> FrequencyType.MULTIPLE_DAILY
            hours < 36 -> FrequencyType.DAILY
            hours < 10 * 24 -> FrequencyType.WEEKLY
            hours < 21 * 24 -> FrequencyType.BIWEEKLY
            hours < 45 * 24 -> FrequencyType.MONTHLY
            else -> FrequencyType.IRREGULAR
        }
    }
}
