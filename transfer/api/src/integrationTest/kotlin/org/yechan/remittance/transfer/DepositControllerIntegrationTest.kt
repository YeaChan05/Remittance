package org.yechan.remittance.transfer

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpOutputMessage
import org.springframework.http.MediaType
import org.springframework.http.converter.AbstractHttpMessageConverter
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import org.yechan.remittance.GlobalExceptionHandler
import org.yechan.remittance.LoginUserIdArgumentResolver
import org.yechan.remittance.transfer.dto.DepositRequest
import java.math.BigDecimal

class DepositControllerIntegrationTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var transferCreateUseCase: StubTransferCreateUseCase
    private val objectMapper = ObjectMapper()

    @BeforeEach
    fun setUp() {
        transferCreateUseCase = StubTransferCreateUseCase()
        val validator = LocalValidatorFactoryBean().apply { afterPropertiesSet() }
        val depositRequestConverter = DepositRequestMessageConverter(objectMapper)
        val transferResultConverter = TransferResultMessageConverter(objectMapper)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(DepositController(transferCreateUseCase))
                .setCustomArgumentResolvers(LoginUserIdArgumentResolver())
                .setControllerAdvice(GlobalExceptionHandler())
                .setValidator(validator)
                .setMessageConverters(
                    depositRequestConverter,
                    transferResultConverter,
                    StringHttpMessageConverter(),
                )
                .build()
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
        transferCreateUseCase.clear()
    }

    @Test
    fun `입금 요청은 로그인 사용자와 경로 멱등키를 use case로 전달한다`() {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                "7",
                "credentials",
                listOf(SimpleGrantedAuthority("ROLE_USER")),
            )

        mockMvc.perform(
            post("/deposits/deposit-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(DepositRequest(10L, BigDecimal("50.00")))),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SUCCEEDED"))
            .andExpect(jsonPath("$.transferId").value(99))
            .andExpect(jsonPath("$.errorCode").doesNotExist())

        transferCreateUseCase.assertCalled(
            memberId = 7L,
            idempotencyKey = "deposit-key",
            accountId = 10L,
            amount = BigDecimal("50.00"),
            scope = TransferProps.TransferScopeValue.DEPOSIT,
        )
    }

    @Test
    fun `인증 정보가 없으면 unauthorized를 반환한다`() {
        mockMvc.perform(
            post("/deposits/deposit-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(DepositRequest(10L, BigDecimal("50.00")))),
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `잘못된 요청 본문이면 bad request를 반환한다`() {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                "7",
                "credentials",
                listOf(SimpleGrantedAuthority("ROLE_USER")),
            )

        mockMvc.perform(
            post("/deposits/deposit-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"accountId":10,"amount":0}"""),
        )
            .andExpect(status().isBadRequest)
    }

    class StubTransferCreateUseCase : TransferCreateUseCase {
        private var lastMemberId: Long? = null
        private var lastIdempotencyKey: String? = null
        private var lastProps: TransferRequestProps? = null

        override fun transfer(
            memberId: Long,
            idempotencyKey: String,
            props: TransferRequestProps,
        ): TransferResult {
            lastMemberId = memberId
            lastIdempotencyKey = idempotencyKey
            lastProps = props
            return TransferResult.succeeded(99L)
        }

        fun assertCalled(
            memberId: Long,
            idempotencyKey: String,
            accountId: Long,
            amount: BigDecimal,
            scope: TransferProps.TransferScopeValue,
        ) {
            assertThat(lastMemberId).isEqualTo(memberId)
            assertThat(lastIdempotencyKey).isEqualTo(idempotencyKey)
            assertThat(lastProps).isNotNull
            assertThat(lastProps!!.fromAccountId).isEqualTo(accountId)
            assertThat(lastProps!!.toAccountId).isEqualTo(accountId)
            assertThat(lastProps!!.amount).isEqualByComparingTo(amount)
            assertThat(lastProps!!.scope).isEqualTo(scope)
        }

        fun clear() {
            lastMemberId = null
            lastIdempotencyKey = null
            lastProps = null
        }
    }

    private class DepositRequestMessageConverter(
        private val objectMapper: ObjectMapper,
    ) : AbstractHttpMessageConverter<DepositRequest>(MediaType.APPLICATION_JSON) {
        override fun supports(clazz: Class<*>): Boolean = clazz == DepositRequest::class.java

        override fun readInternal(
            clazz: Class<out DepositRequest>,
            inputMessage: HttpInputMessage,
        ): DepositRequest {
            val node = objectMapper.readTree(inputMessage.body)
            val accountId = node.get("accountId")?.longValue()
            val amountNode = node.get("amount")
            val amount = amountNode?.decimalValue()
            return DepositRequest(accountId, amount)
        }

        override fun writeInternal(
            t: DepositRequest,
            outputMessage: HttpOutputMessage,
        ): Unit = throw UnsupportedOperationException("write not supported")
    }

    private class TransferResultMessageConverter(
        private val objectMapper: ObjectMapper,
    ) : AbstractHttpMessageConverter<TransferResult>(MediaType.APPLICATION_JSON) {
        override fun supports(clazz: Class<*>): Boolean = clazz == TransferResult::class.java

        override fun readInternal(
            clazz: Class<out TransferResult>,
            inputMessage: HttpInputMessage,
        ): TransferResult = throw UnsupportedOperationException("read not supported")

        override fun writeInternal(
            t: TransferResult,
            outputMessage: HttpOutputMessage,
        ): Unit = objectMapper.run { writeValue(outputMessage.body, t) }
    }
}
