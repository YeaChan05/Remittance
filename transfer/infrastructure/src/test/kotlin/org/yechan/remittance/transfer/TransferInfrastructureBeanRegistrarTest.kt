package org.yechan.remittance.transfer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.yechan.remittance.account.internal.contract.AccountBalanceChangeRequest
import org.yechan.remittance.account.internal.contract.AccountBalanceChangeResponse
import org.yechan.remittance.account.internal.contract.AccountGetRequest
import org.yechan.remittance.account.internal.contract.AccountInternalApi
import org.yechan.remittance.account.internal.contract.AccountLockRequest
import org.yechan.remittance.account.internal.contract.AccountLockResponse
import org.yechan.remittance.account.internal.contract.AccountSnapshotResponse
import org.yechan.remittance.member.internal.contract.MemberExistenceInternalApi
import org.yechan.remittance.member.internal.contract.MemberExistsResponse
import java.math.BigDecimal

class TransferInfrastructureBeanRegistrarTest {
    @Test
    fun `자동 설정은 transfer consumer client 빈을 등록한다`() {
        val accountInternalApi = object : AccountInternalApi {
            override fun get(request: AccountGetRequest): AccountSnapshotResponse = AccountSnapshotResponse(request.accountId, 7L, BigDecimal("1000"))

            override fun lock(request: AccountLockRequest): AccountLockResponse = AccountLockResponse(
                AccountSnapshotResponse(request.fromAccountId, 7L, BigDecimal("900")),
                AccountSnapshotResponse(request.toAccountId, 8L, BigDecimal("100")),
            )

            override fun applyBalanceChange(request: AccountBalanceChangeRequest): AccountBalanceChangeResponse = AccountBalanceChangeResponse(true)
        }
        val memberExistenceInternalApi =
            MemberExistenceInternalApi { MemberExistsResponse(it.memberId == 7L) }
        val context = AnnotationConfigApplicationContext().apply {
            beanFactory.registerSingleton("accountInternalApi", accountInternalApi)
            beanFactory.registerSingleton("memberExistenceInternalApi", memberExistenceInternalApi)
            register(TestConfiguration::class.java)
            refresh()
        }

        val accountClient = context.getBean(TransferAccountClient::class.java)
        val memberClient = context.getBean(TransferMemberClient::class.java)

        assertThat(accountClient.get(10L)?.memberId).isEqualTo(7L)
        assertThat(memberClient.exists(7L)).isTrue()

        context.close()
    }

    @Configuration(proxyBeanMethods = false)
    @Import(TransferInfrastructureBeanRegistrar::class)
    class TestConfiguration
}
