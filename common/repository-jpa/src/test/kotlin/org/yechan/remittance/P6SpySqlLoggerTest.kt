package org.yechan.remittance

import com.p6spy.engine.logging.Category
import com.p6spy.engine.spy.appender.MessageFormattingStrategy
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class P6SpySqlLoggerTest {

    @Test
    fun `suppress 구간에서는 SQL 로그 포매팅과 출력을 건너뛴다`() {
        val logger = P6SpySqlLogger()
        val strategy = CountingFormattingStrategy()
        logger.setStrategy(strategy)

        P6SpySqlLogSuppressor.suppress {
            logger.logSQL(
                1,
                "2026-04-26 12:00:00.000",
                1L,
                Category.STATEMENT,
                "select 1",
                "select 1",
                "jdbc:mysql://localhost:3306/core",
            )
        }

        assertThat(strategy.formatCount).isZero()
    }

    @Test
    fun `suppress 구간이 아니면 SQL 로그 포매팅을 수행한다`() {
        val logger = P6SpySqlLogger()
        val strategy = CountingFormattingStrategy()
        logger.setStrategy(strategy)

        logger.logSQL(
            1,
            "2026-04-26 12:00:00.000",
            1L,
            Category.STATEMENT,
            "select 1",
            "select 1",
            "jdbc:mysql://localhost:3306/core",
        )

        assertThat(strategy.formatCount).isOne()
    }

    private class CountingFormattingStrategy : MessageFormattingStrategy {
        var formatCount: Int = 0
            private set

        override fun formatMessage(
            connectionId: Int,
            now: String?,
            elapsed: Long,
            category: String?,
            prepared: String?,
            sql: String?,
            url: String?,
        ): String {
            formatCount += 1
            return "sql"
        }
    }
}
