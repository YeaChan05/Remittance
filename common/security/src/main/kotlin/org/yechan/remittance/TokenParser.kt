package org.yechan.remittance

import jakarta.servlet.http.HttpServletRequest

fun interface TokenParser {
    fun parse(request: HttpServletRequest): String?
}
