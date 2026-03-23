package org.yechan.remittance

import java.util.UUID

object EmailGenerator {
    fun generate(): String = "${UUID.randomUUID()}@example.com"
}
