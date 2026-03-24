package org.yechan.remittance.account.internal.adapter

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.yechan.remittance.account.AccountInternalLockValue
import org.yechan.remittance.account.AccountInternalQueryUseCase
import org.yechan.remittance.account.AccountInternalSnapshotValue
import org.yechan.remittance.account.AccountInternalUpdateUseCase
import org.yechan.remittance.account.internal.contract.AccountBalanceChangeRequest
import org.yechan.remittance.account.internal.contract.AccountGetRequest
import org.yechan.remittance.account.internal.contract.AccountInternalApi
import java.math.BigDecimal

class AccountInternalApiBeanRegistrarTest {
    @Test
    fun `자동 설정은 계좌 내부 API 빈을 등록한다`() {
        val queryUseCase = object : AccountInternalQueryUseCase {
            override fun get(accountId: Long): AccountInternalSnapshotValue = AccountInternalSnapshotValue(accountId, 3L, BigDecimal("1000"))

            override fun lock(fromAccountId: Long, toAccountId: Long): AccountInternalLockValue = AccountInternalLockValue(
                AccountInternalSnapshotValue(fromAccountId, 3L, BigDecimal("900")),
                AccountInternalSnapshotValue(toAccountId, 4L, BigDecimal("100")),
            )
        }
        val updateUseCase = AccountInternalUpdateUseCase { true }
        val context = AnnotationConfigApplicationContext().apply {
            beanFactory.registerSingleton("accountInternalQueryUseCase", queryUseCase)
            beanFactory.registerSingleton("accountInternalUpdateUseCase", updateUseCase)
            register(TestConfiguration::class.java)
            refresh()
        }

        val api = context.getBean(AccountInternalApi::class.java)

        assertThat(api.get(AccountGetRequest(10L))?.memberId).isEqualTo(3L)
        assertThat(
            api.applyBalanceChange(
                AccountBalanceChangeRequest(
                    fromAccountId = 1L,
                    toAccountId = 2L,
                    fromBalance = BigDecimal("900"),
                    toBalance = BigDecimal("100"),
                ),
            ).applied,
        ).isTrue()

        context.close()
    }

    @Configuration(proxyBeanMethods = false)
    @Import(AccountInternalApiBeanRegistrar::class)
    class TestConfiguration
}
