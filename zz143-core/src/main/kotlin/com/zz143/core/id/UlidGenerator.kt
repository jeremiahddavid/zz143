package com.zz143.core.id

import java.security.SecureRandom

object UlidGenerator {
    private val random = SecureRandom()
    private val encoding = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"

    fun next(): String {
        val timestamp = System.currentTimeMillis()
        val sb = StringBuilder(26)

        // Encode timestamp (10 chars, 48 bits)
        var ts = timestamp
        val timeChars = CharArray(10)
        for (i in 9 downTo 0) {
            timeChars[i] = encoding[(ts and 0x1F).toInt()]
            ts = ts shr 5
        }
        sb.append(timeChars)

        // Encode randomness (16 chars, 80 bits)
        val randomBytes = ByteArray(10)
        random.nextBytes(randomBytes)
        var bits = 0L
        var bitCount = 0
        var byteIndex = 0
        repeat(16) {
            while (bitCount < 5 && byteIndex < randomBytes.size) {
                bits = (bits shl 8) or (randomBytes[byteIndex++].toLong() and 0xFF)
                bitCount += 8
            }
            bitCount -= 5
            sb.append(encoding[((bits shr bitCount) and 0x1F).toInt()])
        }

        return sb.toString()
    }
}
