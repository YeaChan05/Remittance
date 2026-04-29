package org.yechan.remittance

import com.p6spy.engine.logging.Category
import com.p6spy.engine.spy.appender.Slf4JLogger

class P6SpySqlLogger : Slf4JLogger() {

    override fun logSQL(
        connectionId: Int,
        now: String?,
        elapsed: Long,
        category: Category?,
        prepared: String?,
        sql: String?,
        url: String?,
    ) {
        if (P6SpySqlLogSuppressor.isSuppressed()) {
            return
        }
        super.logSQL(connectionId, now, elapsed, category, prepared, sql, url)
    }
}
