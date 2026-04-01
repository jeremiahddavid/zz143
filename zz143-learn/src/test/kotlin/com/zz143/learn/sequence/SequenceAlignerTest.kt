package com.zz143.learn.sequence

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SequenceAlignerTest {

    private val aligner = SequenceAligner()

    @Test
    fun identicalSequencesHaveSimilarityCloseToOne() {
        val seq = listOf("tap@home", "scroll@home", "tap@detail", "submit@detail")

        val result = aligner.align(seq, seq)

        assertThat(result.similarity).isWithin(0.01f).of(1.0f)
    }

    @Test
    fun completelyDifferentSequencesHaveSimilarityCloseToZero() {
        val seq1 = listOf("aaa", "bbb", "ccc", "ddd")
        val seq2 = listOf("xxx", "yyy", "zzz", "www")

        val result = aligner.align(seq1, seq2)

        assertThat(result.similarity).isWithin(0.1f).of(0.0f)
    }

    @Test
    fun bothEmptySequencesReturnZeroSimilarity() {
        val result = aligner.align(emptyList(), emptyList())

        assertThat(result.similarity).isEqualTo(0f)
        assertThat(result.score).isEqualTo(0)
        assertThat(result.alignedLength).isEqualTo(0)
    }

    @Test
    fun oneEmptySequenceReturnsZeroSimilarity() {
        val seq = listOf("tap@home", "scroll@list")

        val result = aligner.align(seq, emptyList())

        assertThat(result.similarity).isEqualTo(0f)
        assertThat(result.score).isEqualTo(0)
    }

    @Test
    fun knownPartialMatchReturnsIntermediateSimilarity() {
        val seq1 = listOf("A", "B", "C", "D")
        val seq2 = listOf("A", "B", "X", "Y")

        val result = aligner.align(seq1, seq2)

        // 2 out of 4 match, so similarity should be around 0.5 but the exact
        // value depends on Smith-Waterman scoring with gap/mismatch penalties
        assertThat(result.similarity).isGreaterThan(0.0f)
        assertThat(result.similarity).isLessThan(1.0f)
    }

    @Test
    fun similarityIsAlwaysBetweenZeroAndOneInclusive() {
        val testCases = listOf(
            Pair(listOf("A"), listOf("A")),
            Pair(listOf("A", "B"), listOf("B", "A")),
            Pair(listOf("A", "B", "C"), listOf("A", "X", "C")),
            Pair(listOf("X"), listOf("Y")),
            Pair(emptyList(), listOf("A")),
            Pair(listOf("A", "B", "C", "D", "E"), listOf("A", "C", "E"))
        )

        for ((seq1, seq2) in testCases) {
            val result = aligner.align(seq1, seq2)

            assertThat(result.similarity).isAtLeast(0.0f)
            assertThat(result.similarity).isAtMost(1.0f)
        }
    }

    @Test
    fun alignmentScoreIsNonNegative() {
        val seq1 = listOf("A", "B", "C")
        val seq2 = listOf("X", "Y", "Z")

        val result = aligner.align(seq1, seq2)

        assertThat(result.score).isAtLeast(0)
    }

    @Test
    fun alignedLengthDoesNotExceedShorterSequence() {
        val seq1 = listOf("A", "B", "C", "D", "E")
        val seq2 = listOf("A", "B", "C")

        val result = aligner.align(seq1, seq2)

        assertThat(result.alignedLength).isAtMost(seq2.size)
    }
}
