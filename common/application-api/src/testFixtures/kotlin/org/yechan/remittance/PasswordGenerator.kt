package org.yechan.remittance

import java.util.UUID

object PasswordGenerator {
    fun generate(): String = "Pw1!${UUID.randomUUID().toString().take(8)}"
}
