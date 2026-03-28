package org.yechan.remittance

import java.util.UUID

object EmailGenerator {
    fun generate(): String = "${UUID.randomUUID()}@test.local"
}
