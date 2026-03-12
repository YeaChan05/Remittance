package org.yechan.remittance

import org.springframework.security.core.Authentication

fun interface TokenVerifier {
    fun verify(token: String): Authentication
}
