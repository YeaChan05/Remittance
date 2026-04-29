package org.yechan.remittance

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class P6SpySqlFormatterTest {

    private val formatter = P6SpySqlFormatter()

    @Test
    fun `바인딩 값이 반영된 SELECT SQL을 읽기 좋게 줄바꿈한다`() {
        val message = formatter.formatMessage(
            connectionId = 7,
            now = "2026-04-26 12:00:00.000",
            elapsed = 13L,
            category = "statement",
            prepared = "select * from account where id = ?",
            sql = """
            select *
            from account
            where id = 1
            """.trimIndent(),
            url = "jdbc:mysql://localhost:3306/core",
        )

        assertThat(message).isEqualTo(
            """
            SQL category=statement elapsed=13ms connection=7
            select
              *
            from account
            where id = 1
            """.trimIndent(),
        )
    }

    @Test
    fun `WHERE 조건과 정렬 구문을 절 단위로 줄바꿈한다`() {
        val message = formatter.formatMessage(
            connectionId = 166,
            now = "2026-04-26 12:00:00.000",
            elapsed = 1L,
            category = "statement",
            prepared = "",
            sql = """
            select 
              o.id,
              o.status,
              o.created_at 
            from integration.outbox_events o
            where o.status='NEW' and (NULL is null or o.created_at<=NULL)
            order by o.created_at limit 100
            """.trimIndent(),
            url = "jdbc:mysql://localhost:3306/core",
        )

        assertThat(message).isEqualTo(
            """
            SQL category=statement elapsed=1ms connection=166
            select
              o.id,
              o.status,
              o.created_at
            from integration.outbox_events
              o
            where o.status='NEW'
              and (NULL is null
              or o.created_at<=NULL)
            order by o.created_at
            limit 100
            """.trimIndent(),
        )
    }

    @Test
    fun `SELECT SQL의 FROM alias를 다음 줄로 출력한다`() {
        val message = formatter.formatMessage(
            connectionId = 6,
            now = "2026-04-26 12:00:00.000",
            elapsed = 7L,
            category = "statement",
            prepared = "",
            sql = """
            select ae1_0.id,ae1_0.account_name,ae1_0.balance
            from core.account ae1_0
            where ae1_0.id=1
            """.trimIndent(),
            url = "jdbc:mysql://localhost:3306/core",
        )

        assertThat(message).isEqualTo(
            """
            SQL category=statement elapsed=7ms connection=6
            select
              ae1_0.id,
              ae1_0.account_name,
              ae1_0.balance
            from core.account
              ae1_0
            where ae1_0.id=1
            """.trimIndent(),
        )
    }

    @Test
    fun `INSERT SQL의 컬럼과 값을 줄 단위로 출력한다`() {
        val message = formatter.formatMessage(
            connectionId = 2,
            now = "2026-04-26 12:00:00.000",
            elapsed = 3L,
            category = "statement",
            prepared = "",
            sql = """
                insert into core.ledger (account_id,amount,balance_after,created_at,side,transfer_id,id)
                values (1,1000,9000,'2026-04-26 03:50:00','DEBIT',10,99)
            """.trimIndent(),
            url = "jdbc:mysql://localhost:3306/core",
        )

        assertThat(message).isEqualTo(
            """
                SQL category=statement elapsed=3ms connection=2
                insert into core.ledger (
                  account_id,
                  amount,
                  balance_after,
                  created_at,
                  side,
                  transfer_id,
                  id
                )
                values (
                  1,
                  1000,
                  9000,
                  '2026-04-26 03:50:00',
                  'DEBIT',
                  10,
                  99
                )
            """.trimIndent(),
        )
    }

    @Test
    fun `INSERT IGNORE SQL도 줄 단위로 출력한다`() {
        val message = formatter.formatMessage(
            connectionId = 3,
            now = "2026-04-26 12:00:00.000",
            elapsed = 4L,
            category = "statement",
            prepared = "",
            sql = "insert ignore into core.ledger (transfer_id,account_id,side,id) values (10,1,'DEBIT',99)",
            url = "jdbc:mysql://localhost:3306/core",
        )

        assertThat(message).isEqualTo(
            """
                SQL category=statement elapsed=4ms connection=3
                insert ignore into core.ledger (
                  transfer_id,
                  account_id,
                  side,
                  id
                )
                values (
                  10,
                  1,
                  'DEBIT',
                  99
                )
            """.trimIndent(),
        )
    }

    @Test
    fun `UPDATE SQL의 SET 항목과 WHERE 조건을 줄 단위로 출력한다`() {
        val message = formatter.formatMessage(
            connectionId = 4,
            now = "2026-04-26 12:00:00.000",
            elapsed = 5L,
            category = "statement",
            prepared = "",
            sql = """
                update core.account set balance=9000,updated_at='2026-04-26 03:50:00'
                where id=1 and member_id=10
            """.trimIndent(),
            url = "jdbc:mysql://localhost:3306/core",
        )

        assertThat(message).isEqualTo(
            """
                SQL category=statement elapsed=5ms connection=4
                update core.account
                set
                  balance=9000,
                  updated_at='2026-04-26 03:50:00'
                where id=1
                  and member_id=10
            """.trimIndent(),
        )
    }

    @Test
    fun `UPDATE SQL의 문자열 내부 쉼표는 SET 항목 분리 기준에서 제외한다`() {
        val message = formatter.formatMessage(
            connectionId = 5,
            now = "2026-04-26 12:00:00.000",
            elapsed = 6L,
            category = "statement",
            prepared = "",
            sql = "update core.account set account_name='main, savings',updated_at='2026-04-26 03:50:00' where id=1",
            url = "jdbc:mysql://localhost:3306/core",
        )

        assertThat(message).isEqualTo(
            """
                SQL category=statement elapsed=6ms connection=5
                update core.account
                set
                  account_name='main, savings',
                  updated_at='2026-04-26 03:50:00'
                where id=1
            """.trimIndent(),
        )
    }

    @Test
    fun `UPDATE SQL의 테이블 alias를 다음 줄로 출력한다`() {
        val message = formatter.formatMessage(
            connectionId = 7,
            now = "2026-04-29 06:53:13.926",
            elapsed = 8L,
            category = "statement",
            prepared = "",
            sql = """
                update integration.idempotency_key ike1_0
                set status='IN_PROGRESS',request_hash='hash',started_at='2026-04-29T06:53:13.926+0900'
                where ike1_0.client_id=837219962013607836 and ike1_0.scope='DEPOSIT'
            """.trimIndent(),
            url = "jdbc:mysql://localhost:3306/integration",
        )

        assertThat(message).isEqualTo(
            """
                SQL category=statement elapsed=8ms connection=7
                update integration.idempotency_key
                  ike1_0
                set
                  status='IN_PROGRESS',
                  request_hash='hash',
                  started_at='2026-04-29T06:53:13.926+0900'
                where ike1_0.client_id=837219962013607836
                  and ike1_0.scope='DEPOSIT'
            """.trimIndent(),
        )
    }

    @Test
    fun `SQL이 없으면 빈 로그를 반환한다`() {
        val message = formatter.formatMessage(
            connectionId = 1,
            now = "2026-04-26 12:00:00.000",
            elapsed = 0L,
            category = "commit",
            prepared = "",
            sql = "",
            url = "jdbc:mysql://localhost:3306/core",
        )

        assertThat(message).isBlank()
    }
}
