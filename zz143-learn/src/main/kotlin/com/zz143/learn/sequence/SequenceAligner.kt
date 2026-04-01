package com.zz143.learn.sequence

import kotlin.math.max

class SequenceAligner {

    data class AlignmentResult(
        val score: Int,
        val similarity: Float,
        val alignedLength: Int
    )

    fun align(seq1: List<String>, seq2: List<String>): AlignmentResult {
        // Fast pre-filter: Jaccard similarity check for long sequences
        if (seq1.size > 50 || seq2.size > 50) {
            val set1 = seq1.toSet()
            val set2 = seq2.toSet()
            val intersection = set1.intersect(set2).size
            val union = set1.union(set2).size
            val jaccard = if (union == 0) 0f else intersection.toFloat() / union
            if (jaccard < 0.5f) {
                return AlignmentResult(0, jaccard, 0)
            }
        }

        return smithWaterman(seq1, seq2)
    }

    private fun smithWaterman(
        seq1: List<String>,
        seq2: List<String>,
        matchScore: Int = 2,
        mismatchPenalty: Int = -1,
        gapPenalty: Int = -1
    ): AlignmentResult {
        val m = seq1.size
        val n = seq2.size
        val H = Array(m + 1) { IntArray(n + 1) }
        var maxScore = 0
        var alignedLength = 0

        for (i in 1..m) {
            for (j in 1..n) {
                val matchVal = H[i - 1][j - 1] + if (seq1[i - 1] == seq2[j - 1]) matchScore else mismatchPenalty
                val deleteVal = H[i - 1][j] + gapPenalty
                val insertVal = H[i][j - 1] + gapPenalty

                H[i][j] = max(0, max(matchVal, max(deleteVal, insertVal)))

                if (H[i][j] > maxScore) {
                    maxScore = H[i][j]
                }
            }
        }

        // Count aligned positions by traceback
        alignedLength = maxScore / matchScore

        val maxLen = max(m, n)
        val similarity = if (maxLen == 0) 0f else maxScore.toFloat() / (matchScore * maxLen)

        return AlignmentResult(
            score = maxScore,
            similarity = similarity.coerceIn(0f, 1f),
            alignedLength = alignedLength
        )
    }
}
