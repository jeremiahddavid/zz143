package com.zz143.learn.sequence

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NGramExtractorTest {

    private val extractor = NGramExtractor(minN = 2, maxN = 5)

    private fun token(action: String, screen: String, ts: Long = 0L) =
        ActionToken(actionType = action, screenId = screen, timestampMs = ts)

    @Test
    fun extractFromFiveTokenSequenceProducesCorrectNGramCount() {
        val tokens = listOf(
            token("tap", "screenA"),
            token("scroll", "screenA"),
            token("tap", "screenB"),
            token("type", "screenB"),
            token("tap", "screenC")
        )

        val ngrams = extractor.extract(tokens)

        // minN=2, maxN=5, sequence size=5
        // n=2: 4 ngrams, n=3: 3 ngrams, n=4: 2 ngrams, n=5: 1 ngram = 10 total
        assertThat(ngrams).hasSize(10)
    }

    @Test
    fun extractFromFiveTokenSequenceContainsExpectedSizes() {
        val tokens = listOf(
            token("tap", "s1"),
            token("scroll", "s1"),
            token("tap", "s2"),
            token("type", "s2"),
            token("tap", "s3")
        )

        val ngrams = extractor.extract(tokens)

        val sizes = ngrams.map { it.size }.distinct().sorted()
        assertThat(sizes).containsExactly(2, 3, 4, 5)
    }

    @Test
    fun extractFromFiveTokenSequencePreservesActionTypes() {
        val tokens = listOf(
            token("tap", "s1"),
            token("scroll", "s1"),
            token("tap", "s2"),
            token("type", "s2"),
            token("tap", "s3")
        )

        val ngrams = extractor.extract(tokens)

        // The first bigram should have the tokens for tap@s1 and scroll@s1
        val firstBigram = ngrams.first { it.size == 2 }
        assertThat(firstBigram.actionTypes).containsExactly("tap@s1", "scroll@s1").inOrder()
    }

    @Test
    fun extractFromFiveTokenSequenceTracksDistinctScreens() {
        val tokens = listOf(
            token("tap", "s1"),
            token("scroll", "s1"),
            token("tap", "s2"),
            token("type", "s2"),
            token("tap", "s3")
        )

        val ngrams = extractor.extract(tokens)

        // The full 5-gram should have screens s1, s2, s3 (distinct)
        val fullNgram = ngrams.first { it.size == 5 }
        assertThat(fullNgram.screens).containsExactly("s1", "s2", "s3")
    }

    @Test
    fun hashIsDeterministicForSameInput() {
        val tokens = listOf(
            token("tap", "screenA"),
            token("scroll", "screenB")
        )

        val ngrams1 = extractor.extract(tokens)
        val ngrams2 = extractor.extract(tokens)

        assertThat(ngrams1.map { it.hash }).isEqualTo(ngrams2.map { it.hash })
    }

    @Test
    fun hashIs16CharHexString() {
        val tokens = listOf(
            token("tap", "s1"),
            token("scroll", "s2"),
            token("type", "s3")
        )

        val ngrams = extractor.extract(tokens)

        for (ngram in ngrams) {
            assertThat(ngram.hash).hasLength(16)
            assertThat(ngram.hash).matches("[0-9a-f]{16}")
        }
    }

    @Test
    fun extractFromEmptyInputReturnsEmptyList() {
        val ngrams = extractor.extract(emptyList())

        assertThat(ngrams).isEmpty()
    }

    @Test
    fun extractFromSequenceSmallerThanMinNReturnsEmptyList() {
        // minN is 2, so a single token should produce no ngrams
        val tokens = listOf(token("tap", "s1"))

        val ngrams = extractor.extract(tokens)

        assertThat(ngrams).isEmpty()
    }

    @Test
    fun differentInputsProduceDifferentHashes() {
        val tokens1 = listOf(token("tap", "s1"), token("scroll", "s2"))
        val tokens2 = listOf(token("type", "s1"), token("swipe", "s2"))

        val hash1 = extractor.extract(tokens1).first().hash
        val hash2 = extractor.extract(tokens2).first().hash

        assertThat(hash1).isNotEqualTo(hash2)
    }
}
