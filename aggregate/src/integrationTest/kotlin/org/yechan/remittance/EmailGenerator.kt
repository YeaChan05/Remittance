package org.yechan.remittance

import java.util.UUID

object EmailGenerator {
    fun generate(): String {
        return "${UUID.randomUUID()}@example.com"
    }
}
