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
import java.math.BigDecimal

class AccountInternalApiBeanRegistrarTest {
    @Test
    fun `자동 설정은 계좌 내부 controller 빈을 등록한다`() {
        val capturedMemberIds = mutableListOf<Long>()
        val queryUseCase = object : AccountInternalQueryUseCase {
            override fun get(
                memberId: Long,
                accountId: Long,
            ): AccountInternalSnapshotValue {
                capturedMemberIds += memberId
                return AccountInternalSnapshotValue(accountId, 3L, BigDecimal("1000"))
            }

            override fun lock(
                memberId: Long,
                fromAccountId: Long,
                toAccountId: Long,
            ): AccountInternalLockValue {
                capturedMemberIds += memberId
                return AccountInternalLockValue(
                    AccountInternalSnapshotValue(fromAccountId, 3L, BigDecimal("900")),
                    AccountInternalSnapshotValue(toAccountId, 4L, BigDecimal("100")),
                )
            }
        }
        val updateUseCase = AccountInternalUpdateUseCase { memberId, _ ->
            capturedMemberIds += memberId
            true
        }
        val context = AnnotationConfigApplicationContext().apply {
            beanFactory.registerSingleton("accountInternalQueryUseCase", queryUseCase)
            beanFactory.registerSingleton("accountInternalUpdateUseCase", updateUseCase)
            register(TestConfiguration::class.java)
            refresh()
        }

        val controller = context.getBean(AccountInternalController::class.java)

        assertThat(
            controller.applyBalanceChange(
                7L,
                AccountBalanceChangeRequest(
                    fromAccountId = 1L,
                    toAccountId = 2L,
                    fromBalance = BigDecimal("900"),
                    toBalance = BigDecimal("100"),
                ),
            ).applied,
        ).isTrue()
        assertThat(controller.get(7L, AccountGetRequest(10L))?.memberId).isEqualTo(3L)
        assertThat(capturedMemberIds).containsOnly(7L)

        context.close()
    }

    @Configuration(proxyBeanMethods = false)
    @Import(AccountInternalApiBeanRegistrar::class, AccountInternalController::class)
    class TestConfiguration
}
