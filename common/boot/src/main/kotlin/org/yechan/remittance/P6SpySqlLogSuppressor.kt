package org.yechan.remittance

import org.slf4j.MDC

object P6SpySqlLogSuppressor {

    const val MDC_KEY: String = "p6spy.sql.suppressed"
    private const val SUPPRESSED_VALUE = "true"

    fun isSuppressed(): Boolean = MDC.get(MDC_KEY).equals(SUPPRESSED_VALUE, ignoreCase = true)

    fun <T> suppress(block: () -> T): T {
        val previous = MDC.get(MDC_KEY)
        MDC.put(MDC_KEY, SUPPRESSED_VALUE)
        return try {
            block()
        } finally {
            if (previous == null) {
                MDC.remove(MDC_KEY)
            } else {
                MDC.put(MDC_KEY, previous)
            }
        }
    }
}
