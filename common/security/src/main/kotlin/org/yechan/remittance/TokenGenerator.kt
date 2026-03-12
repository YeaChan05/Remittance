package org.yechan.remittance

fun interface TokenGenerator {
    fun generate(memberId: Long?): AuthTokenValue
}
