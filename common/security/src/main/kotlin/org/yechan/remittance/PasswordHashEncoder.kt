package org.yechan.remittance

interface PasswordHashEncoder {
    fun encode(password: String): String

    fun matches(
        password: String,
        encodedPassword: String
    ): Boolean
}
