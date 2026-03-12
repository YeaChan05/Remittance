package org.yechan.remittance

import java.security.SecureRandom

object PasswordGenerator {
    private const val LETTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val DIGITS = "0123456789"
    private const val SPECIALS = "!@#$%^&*"
    private const val ALL = LETTERS + DIGITS + SPECIALS
    private val random = SecureRandom()

    fun generate(): String {
        val chars = mutableListOf<Char>()
        chars += LETTERS[random.nextInt(LETTERS.length)]
        chars += DIGITS[random.nextInt(DIGITS.length)]
        chars += SPECIALS[random.nextInt(SPECIALS.length)]

        while (chars.size < 10) {
            chars += ALL[random.nextInt(ALL.length)]
        }

        chars.shuffle(random)
        return chars.joinToString(separator = "")
    }
}
