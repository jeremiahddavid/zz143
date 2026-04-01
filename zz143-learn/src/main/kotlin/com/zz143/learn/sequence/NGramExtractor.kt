package com.zz143.learn.sequence

import com.zz143.core.model.SemanticAction
import java.security.MessageDigest

class NGramExtractor(
    private val minN: Int = 2,
    private val maxN: Int = 10
) {
    data class NGram(
        val hash: String,
        val actionTypes: List<String>,
        val size: Int,
        val screens: List<String>
    )

    fun extract(actions: List<ActionToken>): List<NGram> {
        val ngrams = mutableListOf<NGram>()

        for (n in minN..minOf(maxN, actions.size)) {
            for (i in 0..(actions.size - n)) {
                val window = actions.subList(i, i + n)
                val types = window.map { it.token }
                val screens = window.map { it.screenId }.distinct()
                val hash = hashNGram(types)

                ngrams.add(NGram(
                    hash = hash,
                    actionTypes = types,
                    size = n,
                    screens = screens
                ))
            }
        }

        return ngrams
    }

    private fun hashNGram(types: List<String>): String {
        val joined = types.joinToString("|")
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(joined.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.take(16)
    }
}

data class ActionToken(
    val actionType: String,
    val screenId: String,
    val timestampMs: Long
) {
    val token: String get() = "$actionType@$screenId"
}
