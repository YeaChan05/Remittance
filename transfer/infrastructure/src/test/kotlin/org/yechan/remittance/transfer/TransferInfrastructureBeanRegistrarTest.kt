package org.yechan.remittance.transfer

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.test.context.support.TestPropertySourceUtils
import org.yechan.remittance.InternalServiceAuthProperties
import java.math.BigDecimal

class TransferInfrastructureBeanRegistrarTest {
    @Test
    fun `자동 설정은 transfer consumer client 빈을 등록한다`() {
        val accountServer = MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse = when (request.requestUrl?.encodedPath) {
                    "/internal/accounts/query" -> MockResponse()
                        .setHeader("Content-Type", "application/json")
                        .setBody("""{"accountId":10,"memberId":7,"balance":1000}""")

                    "/internal/accounts/lock" -> MockResponse()
                        .setHeader("Content-Type", "application/json")
                        .setBody(
                            """
                            {
                              "fromAccount":{"accountId":10,"memberId":7,"balance":900},
                              "toAccount":{"accountId":20,"memberId":8,"balance":100}
                            }
                            """.trimIndent(),
                        )

                    "/internal/accounts/balance-change" -> MockResponse()
                        .setHeader("Content-Type", "application/json")
                        .setBody("""{"applied":true}""")

                    else -> MockResponse().setResponseCode(404)
                }
            }
            start()
        }
        val memberServer = MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse = when (request.requestUrl?.encodedPath) {
                    "/internal/members/existence" -> MockResponse()
                        .setHeader("Content-Type", "application/json")
                        .setBody("""{"exists":true}""")

                    else -> MockResponse().setResponseCode(404)
                }
            }
            start()
        }
        val context = AnnotationConfigApplicationContext().apply {
            beanFactory.registerSingleton(
                "internalServiceAuthProperties",
                InternalServiceAuthProperties("test-internal-token"),
            )
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                this,
                "spring.http.serviceclient.account-internal.base-url=${
                    accountServer.url("/").toString().removeSuffix("/")
                }",
                "spring.http.serviceclient.member-internal.base-url=${
                    memberServer.url("/").toString().removeSuffix("/")
                }",
            )
            register(TestConfiguration::class.java)
            refresh()
        }

        try {
            val accountClient = context.getBean(TransferAccountClient::class.java)
            val memberClient = context.getBean(TransferMemberClient::class.java)

            assertThat(accountClient.get(99L, 10L)?.memberId).isEqualTo(7L)
            assertThat(
                accountClient.lock(
                    TransferAccountLockCommand(
                        99L,
                        10L,
                        20L,
                    ),
                )?.toAccount?.memberId,
            ).isEqualTo(8L)
            accountClient.applyBalanceChange(
                TransferBalanceChangeCommand(
                    memberId = 99L,
                    fromAccountId = 10L,
                    toAccountId = 20L,
                    fromBalance = BigDecimal("890"),
                    toBalance = BigDecimal("110"),
                ),
            )
            assertThat(memberClient.exists(7L)).isTrue()
        } finally {
            context.close()
            accountServer.shutdown()
            memberServer.shutdown()
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Import(TransferInfrastructureBeanRegistrar::class)
    class TestConfiguration
}
