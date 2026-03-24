package org.yechan.remittance.api.transfer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.client.RestTestClient
import org.yechan.remittance.AggregateApplication
import org.yechan.remittance.IntegrationTestEnvironmentSetup
import org.yechan.remittance.TransferTestFixtures
import org.yechan.remittance.TransferTestFixtures.LedgerRow
import org.yechan.remittance.TransferTestFixturesConfig
import org.yechan.remittance.account.AccountIdentifier
import org.yechan.remittance.transfer.IdempotencyKeyProps
import org.yechan.remittance.transfer.TransferIdentifier
import org.yechan.remittance.transfer.TransferModel
import org.yechan.remittance.transfer.TransferProps
import org.yechan.remittance.transfer.TransferQueryCondition
import org.yechan.remittance.transfer.TransferRepository
import org.yechan.remittance.transfer.TransferRequestProps
import org.yechan.remittance.transfer.dto.DepositRequest
import org.yechan.remittance.transfer.dto.IdempotencyKeyCreateResponse
import org.yechan.remittance.transfer.dto.TransferRequest
import org.yechan.remittance.transfer.dto.WithdrawalRequest
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.util.Optional
import java.util.concurrent.atomic.AtomicBoolean

@SpringBootTest(classes = [AggregateApplication::class])
@Import(TransferTestFixturesConfig::class, PostSpecs.TransferFailureConfig::class)
class PostSpecs : IntegrationTestEnvironmentSetup() {
    @Autowired
    lateinit var restTestClient: RestTestClient

    @Autowired
    lateinit var fixtures: TransferTestFixtures

    @Autowired
    lateinit var transferFailureSwitch: TransferFailureSwitch

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.clearContext()
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
        transferFailureSwitch.disable()
    }

    @Test
    fun `정상 이체는 ledger outbox idempotency snapshot을 생성한다`() {
        val result = fixtures.setupAuthentication()
        val memberId = result.authentication.name.toLong()
        val fromAccountBalance = BigDecimal.valueOf(100_000L)
        val toAccountBalance = BigDecimal.valueOf(50_000L)
        val transferAmount = BigDecimal.valueOf(30_000L)
        val fee = feeFor(transferAmount)

        val fromAccount =
            fixtures.createAccountWithBalance(memberId, "from-account", fromAccountBalance)
        val toAccount = fixtures.createAccountWithBalance(memberId, "to-account", toAccountBalance)
        val idempotencyKey = issueIdempotencyKey(
            result.auth.accessToken,
            IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER,
        )

        val before = LocalDateTime.now()
        val response = transfer(
            result.auth.accessToken,
            idempotencyKey,
            fromAccount.accountId,
            toAccount.accountId,
            transferAmount,
        )
        val after = LocalDateTime.now()

        assertTransferSucceeded(response)
        assertBalance(
            fromAccount.accountId,
            BigDecimal.valueOf(100_000L).subtract(transferAmount).subtract(fee),
        )
        assertBalance(toAccount.accountId, BigDecimal.valueOf(80_000L))
        assertLedgers(
            requireNotNull(response.transferId),
            fromAccount.accountId,
            toAccount.accountId,
            transferAmount.add(fee),
            transferAmount,
            before,
            after,
        )
        assertOutbox(
            requireNotNull(response.transferId),
            fromAccount.accountId,
            toAccount.accountId,
            transferAmount,
        )
        assertIdempotency(memberId, idempotencyKey)
    }

    @Test
    fun `동일 멱등키 재시도는 기존 snapshot을 재사용한다`() {
        val result = fixtures.setupAuthentication()
        val memberId = result.authentication.name.toLong()
        val fromAccountBalance = BigDecimal.valueOf(100_000L)
        val toAccountBalance = BigDecimal.valueOf(50_000L)
        val transferAmount = BigDecimal.valueOf(30_000L)

        val fromAccount =
            fixtures.createAccountWithBalance(memberId, "from-account", fromAccountBalance)
        val toAccount = fixtures.createAccountWithBalance(memberId, "to-account", toAccountBalance)
        val idempotencyKey = issueIdempotencyKey(
            result.auth.accessToken,
            IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER,
        )
        val fee = feeFor(transferAmount)

        val firstResponse = transfer(
            result.auth.accessToken,
            idempotencyKey,
            fromAccount.accountId,
            toAccount.accountId,
            transferAmount,
        )

        assertTransferSucceeded(firstResponse)
        assertBalance(
            fromAccount.accountId,
            fromAccountBalance.subtract(transferAmount).subtract(fee),
        )
        assertBalance(toAccount.accountId, BigDecimal.valueOf(80_000L))

        val firstSnapshot = fixtures.loadIdempotencyKey(memberId, idempotencyKey).responseSnapshot
        val firstLedgers = fixtures.loadLedgers(requireNotNull(firstResponse.transferId))
        val firstOutboxes = fixtures.loadOutboxEvents(requireNotNull(firstResponse.transferId))

        val secondResponse = transfer(
            result.auth.accessToken,
            idempotencyKey,
            fromAccount.accountId,
            toAccount.accountId,
            transferAmount,
        )

        assertThat(secondResponse.status).isEqualTo("SUCCEEDED")
        assertThat(secondResponse.transferId).isEqualTo(firstResponse.transferId)
        assertThat(secondResponse.errorCode).isNull()
        assertBalance(
            fromAccount.accountId,
            fromAccountBalance.subtract(transferAmount).subtract(fee),
        )
        assertBalance(toAccount.accountId, BigDecimal.valueOf(80_000L))

        val secondSnapshot = fixtures.loadIdempotencyKey(memberId, idempotencyKey).responseSnapshot
        assertThat(secondSnapshot).isEqualTo(firstSnapshot)

        val secondLedgers = fixtures.loadLedgers(requireNotNull(firstResponse.transferId))
        val secondOutboxes = fixtures.loadOutboxEvents(requireNotNull(firstResponse.transferId))
        assertThat(secondLedgers).containsExactlyInAnyOrderElementsOf(firstLedgers)
        assertThat(secondOutboxes).containsExactlyInAnyOrderElementsOf(firstOutboxes)
    }

    @Test
    fun `같은 멱등키로 다른 body를 보내면 거부한다`() {
        val result = fixtures.setupAuthentication()
        val memberId = result.authentication.name.toLong()
        val fromAccountBalance = BigDecimal.valueOf(100_000L)
        val toAccountBalance = BigDecimal.valueOf(50_000L)
        val transferAmount = BigDecimal.valueOf(30_000L)
        val differentAmount = BigDecimal.valueOf(10_000L)
        val fee = feeFor(transferAmount)

        val fromAccount =
            fixtures.createAccountWithBalance(memberId, "from-account", fromAccountBalance)
        val toAccount = fixtures.createAccountWithBalance(memberId, "to-account", toAccountBalance)
        val idempotencyKey = issueIdempotencyKey(
            result.auth.accessToken,
            IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER,
        )

        val firstResponse = transfer(
            result.auth.accessToken,
            idempotencyKey,
            fromAccount.accountId,
            toAccount.accountId,
            transferAmount,
        )

        assertTransferSucceeded(firstResponse)
        assertBalance(
            fromAccount.accountId,
            fromAccountBalance.subtract(transferAmount).subtract(fee),
        )
        assertBalance(toAccount.accountId, BigDecimal.valueOf(80_000L))

        val firstSnapshot = fixtures.loadIdempotencyKey(memberId, idempotencyKey).responseSnapshot
        val firstLedgers = fixtures.loadLedgers(requireNotNull(firstResponse.transferId))
        val firstOutboxes = fixtures.loadOutboxEvents(requireNotNull(firstResponse.transferId))

        restTestClient.post()
            .uri { uriBuilder -> uriBuilder.path("/transfers/$idempotencyKey").build() }
            .body(TransferRequest(fromAccount.accountId, toAccount.accountId, differentAmount))
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${result.auth.accessToken}")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isBadRequest

        assertBalance(
            fromAccount.accountId,
            fromAccountBalance.subtract(transferAmount).subtract(fee),
        )
        assertBalance(toAccount.accountId, BigDecimal.valueOf(80_000L))

        val secondSnapshot = fixtures.loadIdempotencyKey(memberId, idempotencyKey).responseSnapshot
        assertThat(secondSnapshot).isEqualTo(firstSnapshot)

        val secondLedgers = fixtures.loadLedgers(requireNotNull(firstResponse.transferId))
        val secondOutboxes = fixtures.loadOutboxEvents(requireNotNull(firstResponse.transferId))
        assertThat(secondLedgers).containsExactlyInAnyOrderElementsOf(firstLedgers)
        assertThat(secondOutboxes).containsExactlyInAnyOrderElementsOf(firstOutboxes)
    }

    @Test
    fun `잔액이 부족하면 이체에 실패한다`() {
        val result = fixtures.setupAuthentication()
        val memberId = result.authentication.name.toLong()
        val fromAccountBalance = BigDecimal.valueOf(10_000L)
        val toAccountBalance = BigDecimal.valueOf(50_000L)
        val transferAmount = BigDecimal.valueOf(50_000L)

        val fromAccount =
            fixtures.createAccountWithBalance(memberId, "from-account", fromAccountBalance)
        val toAccount = fixtures.createAccountWithBalance(memberId, "to-account", toAccountBalance)
        val idempotencyKey = issueIdempotencyKey(
            result.auth.accessToken,
            IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER,
        )

        val transferCountBefore = fixtures.countTransfers()
        val outboxCountBefore = fixtures.countOutboxEvents()
        val ledgerCountBefore = fixtures.countLedgers()

        val response = transfer(
            result.auth.accessToken,
            idempotencyKey,
            fromAccount.accountId,
            toAccount.accountId,
            transferAmount,
        )

        assertThat(response.status).isEqualTo("FAILED")
        assertThat(response.transferId).isNull()
        assertThat(response.errorCode).isEqualTo("INSUFFICIENT_BALANCE")
        assertBalance(fromAccount.accountId, BigDecimal.valueOf(10_000L))
        assertBalance(toAccount.accountId, BigDecimal.valueOf(50_000L))

        assertThat(fixtures.countTransfers()).isEqualTo(transferCountBefore)
        assertThat(fixtures.countOutboxEvents()).isEqualTo(outboxCountBefore)
        assertThat(fixtures.countLedgers()).isEqualTo(ledgerCountBefore)

        val idempotency = fixtures.loadIdempotencyKey(memberId, idempotencyKey)
        assertThat(idempotency.status).isEqualTo("FAILED")
        assertThat(idempotency.responseSnapshot).contains("FAILED", "INSUFFICIENT_BALANCE")
    }

    @Test
    fun `진행 중인 멱등키는 IN_PROGRESS를 반환한다`() {
        val result = fixtures.setupAuthentication()
        val memberId = result.authentication.name.toLong()
        val fromAccountBalance = BigDecimal.valueOf(100_000L)
        val toAccountBalance = BigDecimal.valueOf(50_000L)
        val transferAmount = BigDecimal.valueOf(30_000L)

        val fromAccount =
            fixtures.createAccountWithBalance(memberId, "from-account", fromAccountBalance)
        val toAccount = fixtures.createAccountWithBalance(memberId, "to-account", toAccountBalance)
        val idempotencyKey = issueIdempotencyKey(
            result.auth.accessToken,
            IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER,
        )

        fixtures.markIdempotencyInProgress(memberId, idempotencyKey, LocalDateTime.now())

        val transferCountBefore = fixtures.countTransfers()
        val outboxCountBefore = fixtures.countOutboxEvents()
        val ledgerCountBefore = fixtures.countLedgers()

        val response = transfer(
            result.auth.accessToken,
            idempotencyKey,
            fromAccount.accountId,
            toAccount.accountId,
            transferAmount,
        )

        assertThat(response.status).isEqualTo("IN_PROGRESS")
        assertThat(response.transferId).isNull()
        assertThat(response.errorCode).isNull()
        assertBalance(fromAccount.accountId, BigDecimal.valueOf(100_000L))
        assertBalance(toAccount.accountId, BigDecimal.valueOf(50_000L))
        assertThat(fixtures.countTransfers()).isEqualTo(transferCountBefore)
        assertThat(fixtures.countOutboxEvents()).isEqualTo(outboxCountBefore)
        assertThat(fixtures.countLedgers()).isEqualTo(ledgerCountBefore)

        val idempotency = fixtures.loadIdempotencyKey(memberId, idempotencyKey)
        assertThat(idempotency.status).isEqualTo("IN_PROGRESS")
        assertThat(idempotency.responseSnapshot).isNullOrEmpty()
    }

    @Test
    fun `일일 이체 한도를 초과하면 실패한다`() {
        val result = fixtures.setupAuthentication()
        val memberId = result.authentication.name.toLong()
        val fromAccountBalance = BigDecimal.valueOf(10_000_000L)
        val toAccountBalance = BigDecimal.ZERO
        val firstAmount = BigDecimal.valueOf(2_000_000L)
        val secondAmount = BigDecimal.valueOf(1_200_000L)
        val firstFee = feeFor(firstAmount)

        val fromAccount =
            fixtures.createAccountWithBalance(memberId, "from-account", fromAccountBalance)
        val toAccount = fixtures.createAccountWithBalance(memberId, "to-account", toAccountBalance)

        val firstKey = issueIdempotencyKey(
            result.auth.accessToken,
            IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER,
        )
        val firstResponse = transfer(
            result.auth.accessToken,
            firstKey,
            fromAccount.accountId,
            toAccount.accountId,
            firstAmount,
        )

        assertTransferSucceeded(firstResponse)
        assertBalance(
            fromAccount.accountId,
            fromAccountBalance.subtract(firstAmount).subtract(firstFee),
        )
        assertBalance(toAccount.accountId, firstAmount)

        val secondKey = issueIdempotencyKey(
            result.auth.accessToken,
            IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER,
        )
        val transferCountBefore = fixtures.countTransfers()
        val outboxCountBefore = fixtures.countOutboxEvents()
        val ledgerCountBefore = fixtures.countLedgers()

        val secondResponse = transfer(
            result.auth.accessToken,
            secondKey,
            fromAccount.accountId,
            toAccount.accountId,
            secondAmount,
        )

        assertThat(secondResponse.status).isEqualTo("FAILED")
        assertThat(secondResponse.transferId).isNull()
        assertThat(secondResponse.errorCode).isEqualTo("DAILY_LIMIT_EXCEEDED")
        assertBalance(
            fromAccount.accountId,
            fromAccountBalance.subtract(firstAmount).subtract(firstFee),
        )
        assertBalance(toAccount.accountId, firstAmount)

        assertThat(fixtures.countTransfers()).isEqualTo(transferCountBefore)
        assertThat(fixtures.countOutboxEvents()).isEqualTo(outboxCountBefore)
        assertThat(fixtures.countLedgers()).isEqualTo(ledgerCountBefore)

        val idempotency = fixtures.loadIdempotencyKey(
            memberId,
            secondKey,
            IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER,
        )
        assertThat(idempotency.status).isEqualTo("FAILED")
        assertThat(idempotency.responseSnapshot).contains("FAILED", "DAILY_LIMIT_EXCEEDED")
    }

    @Test
    fun `일일 출금 한도를 초과하면 실패한다`() {
        val result = fixtures.setupAuthentication()
        val memberId = result.authentication.name.toLong()
        val fromAccountBalance = BigDecimal.valueOf(2_000_000L)
        val firstAmount = BigDecimal.valueOf(600_000L)
        val secondAmount = BigDecimal.valueOf(500_000L)

        val fromAccount =
            fixtures.createAccountWithBalance(memberId, "from-account", fromAccountBalance)
        val firstKey = issueIdempotencyKey(
            result.auth.accessToken,
            IdempotencyKeyProps.IdempotencyScopeValue.WITHDRAW,
        )
        val firstResponse =
            withdraw(result.auth.accessToken, firstKey, fromAccount.accountId, firstAmount)

        assertTransferSucceeded(firstResponse)
        assertBalance(fromAccount.accountId, fromAccountBalance.subtract(firstAmount))

        val secondKey = issueIdempotencyKey(
            result.auth.accessToken,
            IdempotencyKeyProps.IdempotencyScopeValue.WITHDRAW,
        )
        val transferCountBefore = fixtures.countTransfers()
        val outboxCountBefore = fixtures.countOutboxEvents()
        val ledgerCountBefore = fixtures.countLedgers()

        val secondResponse =
            withdraw(result.auth.accessToken, secondKey, fromAccount.accountId, secondAmount)

        assertThat(secondResponse.status).isEqualTo("FAILED")
        assertThat(secondResponse.transferId).isNull()
        assertThat(secondResponse.errorCode).isEqualTo("DAILY_LIMIT_EXCEEDED")
        assertBalance(fromAccount.accountId, fromAccountBalance.subtract(firstAmount))

        assertThat(fixtures.countTransfers()).isEqualTo(transferCountBefore)
        assertThat(fixtures.countOutboxEvents()).isEqualTo(outboxCountBefore)
        assertThat(fixtures.countLedgers()).isEqualTo(ledgerCountBefore)

        val idempotency = fixtures.loadIdempotencyKey(
            memberId,
            secondKey,
            IdempotencyKeyProps.IdempotencyScopeValue.WITHDRAW,
        )
        assertThat(idempotency.status).isEqualTo("FAILED")
        assertThat(idempotency.responseSnapshot).contains("FAILED", "DAILY_LIMIT_EXCEEDED")
    }

    @Test
    fun `출금은 성공적으로 처리된다`() {
        val result = fixtures.setupAuthentication()
        val memberId = result.authentication.name.toLong()
        val accountBalance = BigDecimal.valueOf(10_000L)
        val withdrawAmount = BigDecimal.valueOf(4_000L)

        val account = fixtures.createAccountWithBalance(memberId, "withdraw-account", accountBalance)
        val idempotencyKey = issueIdempotencyKey(
            result.auth.accessToken,
            IdempotencyKeyProps.IdempotencyScopeValue.WITHDRAW,
        )

        val transferCountBefore = fixtures.countTransfers()
        val outboxCountBefore = fixtures.countOutboxEvents()
        val ledgerCountBefore = fixtures.countLedgers()

        val response =
            withdraw(result.auth.accessToken, idempotencyKey, account.accountId, withdrawAmount)

        assertTransferSucceeded(response)
        assertBalance(account.accountId, accountBalance.subtract(withdrawAmount))
        assertThat(fixtures.countTransfers()).isEqualTo(transferCountBefore + 1)
        assertThat(fixtures.countOutboxEvents()).isEqualTo(outboxCountBefore)
        assertThat(fixtures.countLedgers()).isEqualTo(ledgerCountBefore + 1)

        val idempotency = fixtures.loadIdempotencyKey(
            memberId,
            idempotencyKey,
            IdempotencyKeyProps.IdempotencyScopeValue.WITHDRAW,
        )
        assertThat(idempotency.status).isEqualTo("SUCCEEDED")
        assertThat(idempotency.responseSnapshot).contains("SUCCEEDED")
    }

    @Test
    fun `입금은 성공적으로 처리된다`() {
        val result = fixtures.setupAuthentication()
        val memberId = result.authentication.name.toLong()
        val accountBalance = BigDecimal.valueOf(10_000L)
        val depositAmount = BigDecimal.valueOf(5_000L)

        val account = fixtures.createAccountWithBalance(memberId, "deposit-account", accountBalance)
        val idempotencyKey = issueIdempotencyKey(
            result.auth.accessToken,
            IdempotencyKeyProps.IdempotencyScopeValue.DEPOSIT,
        )

        val transferCountBefore = fixtures.countTransfers()
        val outboxCountBefore = fixtures.countOutboxEvents()
        val ledgerCountBefore = fixtures.countLedgers()

        val response =
            deposit(result.auth.accessToken, idempotencyKey, account.accountId, depositAmount)

        assertTransferSucceeded(response)
        assertBalance(account.accountId, accountBalance.add(depositAmount))
        assertThat(fixtures.countTransfers()).isEqualTo(transferCountBefore + 1)
        assertThat(fixtures.countOutboxEvents()).isEqualTo(outboxCountBefore)
        assertThat(fixtures.countLedgers()).isEqualTo(ledgerCountBefore + 1)

        val idempotency = fixtures.loadIdempotencyKey(
            memberId,
            idempotencyKey,
            IdempotencyKeyProps.IdempotencyScopeValue.DEPOSIT,
        )
        assertThat(idempotency.status).isEqualTo("SUCCEEDED")
        assertThat(idempotency.responseSnapshot).contains("SUCCEEDED")
    }

    @Test
    fun `입금 요청은 인증이 없으면 401을 반환한다`() {
        restTestClient.post()
            .uri("/deposits/invalid-key")
            .body(
                mapOf(
                    "accountId" to 1L,
                    "amount" to 1000,
                ),
            )
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `출금 요청은 인증이 없으면 401을 반환한다`() {
        restTestClient.post()
            .uri("/withdrawals/invalid-key")
            .body(
                mapOf(
                    "accountId" to 1L,
                    "amount" to 1000,
                ),
            )
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `입금 요청 body가 잘못되면 400을 반환한다`() {
        val result = fixtures.setupAuthentication()
        val memberId = result.authentication.name.toLong()
        val account = fixtures.createAccountWithBalance(memberId, "deposit-account", BigDecimal.valueOf(10_000L))
        val idempotencyKey = issueIdempotencyKey(
            result.auth.accessToken,
            IdempotencyKeyProps.IdempotencyScopeValue.DEPOSIT,
        )
        val transferCountBefore = fixtures.countTransfers()
        val outboxCountBefore = fixtures.countOutboxEvents()
        val ledgerCountBefore = fixtures.countLedgers()

        restTestClient.post()
            .uri("/deposits/$idempotencyKey")
            .body(
                mapOf(
                    "accountId" to account.accountId,
                    "amount" to 0,
                ),
            )
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${result.auth.accessToken}")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isBadRequest

        assertThat(fixtures.countTransfers()).isEqualTo(transferCountBefore)
        assertThat(fixtures.countOutboxEvents()).isEqualTo(outboxCountBefore)
        assertThat(fixtures.countLedgers()).isEqualTo(ledgerCountBefore)

        val idempotency = fixtures.loadIdempotencyKey(
            memberId,
            idempotencyKey,
            IdempotencyKeyProps.IdempotencyScopeValue.DEPOSIT,
        )
        assertThat(idempotency.status).isEqualTo("BEFORE_START")
        assertThat(idempotency.responseSnapshot).isNull()
    }

    @Test
    fun `출금 요청 body가 잘못되면 400을 반환한다`() {
        val result = fixtures.setupAuthentication()
        val memberId = result.authentication.name.toLong()
        val account = fixtures.createAccountWithBalance(memberId, "withdraw-account", BigDecimal.valueOf(10_000L))
        val idempotencyKey = issueIdempotencyKey(
            result.auth.accessToken,
            IdempotencyKeyProps.IdempotencyScopeValue.WITHDRAW,
        )
        val transferCountBefore = fixtures.countTransfers()
        val outboxCountBefore = fixtures.countOutboxEvents()
        val ledgerCountBefore = fixtures.countLedgers()

        restTestClient.post()
            .uri("/withdrawals/$idempotencyKey")
            .body(
                mapOf(
                    "accountId" to account.accountId,
                    "amount" to 0,
                ),
            )
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${result.auth.accessToken}")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isBadRequest

        assertThat(fixtures.countTransfers()).isEqualTo(transferCountBefore)
        assertThat(fixtures.countOutboxEvents()).isEqualTo(outboxCountBefore)
        assertThat(fixtures.countLedgers()).isEqualTo(ledgerCountBefore)

        val idempotency = fixtures.loadIdempotencyKey(
            memberId,
            idempotencyKey,
            IdempotencyKeyProps.IdempotencyScopeValue.WITHDRAW,
        )
        assertThat(idempotency.status).isEqualTo("BEFORE_START")
        assertThat(idempotency.responseSnapshot).isNull()
    }

    @Test
    fun `watchdog로 timeout 처리된 멱등키는 FAILED TIMEOUT을 반환한다`() {
        val result = fixtures.setupAuthentication()
        val memberId = result.authentication.name.toLong()
        val fromAccountBalance = BigDecimal.valueOf(100_000L)
        val toAccountBalance = BigDecimal.valueOf(50_000L)
        val transferAmount = BigDecimal.valueOf(30_000L)

        val fromAccount =
            fixtures.createAccountWithBalance(memberId, "from-account", fromAccountBalance)
        val toAccount = fixtures.createAccountWithBalance(memberId, "to-account", toAccountBalance)
        val idempotencyKey = issueIdempotencyKey(
            result.auth.accessToken,
            IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER,
        )

        val now = LocalDateTime.now()
        fixtures.markIdempotencyInProgress(memberId, idempotencyKey, now.minusMinutes(10))

        val transferCountBefore = fixtures.countTransfers()
        val outboxCountBefore = fixtures.countOutboxEvents()
        val ledgerCountBefore = fixtures.countLedgers()

        val timeoutSnapshot = """{"status":"FAILED","errorCode":"TIMEOUT"}"""
        fixtures.markIdempotencyTimeoutBefore(now.minusMinutes(5), timeoutSnapshot, now)

        val response = transfer(
            result.auth.accessToken,
            idempotencyKey,
            fromAccount.accountId,
            toAccount.accountId,
            transferAmount,
        )

        assertThat(response.status).isEqualTo("FAILED")
        assertThat(response.transferId).isNull()
        assertThat(response.errorCode).isEqualTo("TIMEOUT")
        assertBalance(fromAccount.accountId, BigDecimal.valueOf(100_000L))
        assertBalance(toAccount.accountId, BigDecimal.valueOf(50_000L))

        assertThat(fixtures.countTransfers()).isEqualTo(transferCountBefore)
        assertThat(fixtures.countOutboxEvents()).isEqualTo(outboxCountBefore)
        assertThat(fixtures.countLedgers()).isEqualTo(ledgerCountBefore)

        val idempotency = fixtures.loadIdempotencyKey(memberId, idempotencyKey)
        assertThat(idempotency.status).isEqualTo("TIMEOUT")
        assertThat(idempotency.responseSnapshot).isEqualTo(timeoutSnapshot)
    }

    @Test
    fun `이체 성공 시 outbox 이벤트를 생성한다`() {
        val result = fixtures.setupAuthentication()
        val memberId = result.authentication.name.toLong()
        val fromAccountBalance = BigDecimal.valueOf(100_000L)
        val toAccountBalance = BigDecimal.valueOf(50_000L)
        val transferAmount = BigDecimal.valueOf(30_000L)

        val fromAccount =
            fixtures.createAccountWithBalance(memberId, "from-account", fromAccountBalance)
        val toAccount = fixtures.createAccountWithBalance(memberId, "to-account", toAccountBalance)
        val idempotencyKey = issueIdempotencyKey(
            result.auth.accessToken,
            IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER,
        )

        val outboxCountBefore = fixtures.countOutboxEvents()

        val response = transfer(
            result.auth.accessToken,
            idempotencyKey,
            fromAccount.accountId,
            toAccount.accountId,
            transferAmount,
        )

        assertTransferSucceeded(response)
        assertThat(fixtures.countOutboxEvents()).isEqualTo(outboxCountBefore + 1)
        assertOutbox(
            requireNotNull(response.transferId),
            fromAccount.accountId,
            toAccount.accountId,
            transferAmount,
        )
    }

    @Test
    fun `outbox 이벤트는 발행 후 SENT로 마킹할 수 있다`() {
        val result = fixtures.setupAuthentication()
        val memberId = result.authentication.name.toLong()
        val fromAccountBalance = BigDecimal.valueOf(100_000L)
        val toAccountBalance = BigDecimal.valueOf(50_000L)
        val transferAmount = BigDecimal.valueOf(30_000L)

        val fromAccount =
            fixtures.createAccountWithBalance(memberId, "from-account", fromAccountBalance)
        val toAccount = fixtures.createAccountWithBalance(memberId, "to-account", toAccountBalance)
        val idempotencyKey = issueIdempotencyKey(
            result.auth.accessToken,
            IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER,
        )

        val response = transfer(
            result.auth.accessToken,
            idempotencyKey,
            fromAccount.accountId,
            toAccount.accountId,
            transferAmount,
        )

        assertTransferSucceeded(response)
        val outboxIds = fixtures.loadOutboxEventIds(requireNotNull(response.transferId))
        assertThat(outboxIds).hasSize(1)

        fixtures.markOutboxSent(outboxIds.first())

        val outboxes = fixtures.loadOutboxEvents(requireNotNull(response.transferId))
        assertThat(outboxes).hasSize(1)
        assertThat(outboxes.first().status).isEqualTo("SENT")
    }

    @Test
    fun `publisher crash 이후에도 outbox는 다시 발행 가능하다`() {
        val result = fixtures.setupAuthentication()
        val memberId = result.authentication.name.toLong()
        val fromAccountBalance = BigDecimal.valueOf(100_000L)
        val toAccountBalance = BigDecimal.valueOf(50_000L)
        val transferAmount = BigDecimal.valueOf(30_000L)

        val fromAccount =
            fixtures.createAccountWithBalance(memberId, "from-account", fromAccountBalance)
        val toAccount = fixtures.createAccountWithBalance(memberId, "to-account", toAccountBalance)
        val idempotencyKey = issueIdempotencyKey(
            result.auth.accessToken,
            IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER,
        )

        val response = transfer(
            result.auth.accessToken,
            idempotencyKey,
            fromAccount.accountId,
            toAccount.accountId,
            transferAmount,
        )

        assertTransferSucceeded(response)
        val outboxIds = fixtures.loadOutboxEventIds(requireNotNull(response.transferId))
        assertThat(outboxIds).hasSize(1)

        val outboxesBefore = fixtures.loadOutboxEvents(requireNotNull(response.transferId))
        assertThat(outboxesBefore.first().status).isEqualTo("NEW")

        fixtures.markOutboxSent(outboxIds.first())

        val outboxesAfter = fixtures.loadOutboxEvents(requireNotNull(response.transferId))
        assertThat(outboxesAfter).hasSize(1)
        assertThat(outboxesAfter.first().status).isEqualTo("SENT")
    }

    @Test
    fun `영속화 실패 시 이체는 롤백된다`() {
        val result = fixtures.setupAuthentication()
        val memberId = result.authentication.name.toLong()
        val fromAccountBalance = BigDecimal.valueOf(100_000L)
        val toAccountBalance = BigDecimal.valueOf(50_000L)
        val transferAmount = BigDecimal.valueOf(30_000L)

        val fromAccount =
            fixtures.createAccountWithBalance(memberId, "from-account", fromAccountBalance)
        val toAccount = fixtures.createAccountWithBalance(memberId, "to-account", toAccountBalance)
        val idempotencyKey = issueIdempotencyKey(
            result.auth.accessToken,
            IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER,
        )

        val transferCountBefore = fixtures.countTransfers()
        val outboxCountBefore = fixtures.countOutboxEvents()
        val ledgerCountBefore = fixtures.countLedgers()

        transferFailureSwitch.enable()

        restTestClient.post()
            .uri { uriBuilder -> uriBuilder.path("/transfers/$idempotencyKey").build() }
            .body(TransferRequest(fromAccount.accountId, toAccount.accountId, transferAmount))
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${result.auth.accessToken}")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().is5xxServerError

        assertBalance(fromAccount.accountId, BigDecimal.valueOf(100_000L))
        assertBalance(toAccount.accountId, BigDecimal.valueOf(50_000L))
        assertThat(fixtures.countTransfers()).isEqualTo(transferCountBefore)
        assertThat(fixtures.countOutboxEvents()).isEqualTo(outboxCountBefore)
        assertThat(fixtures.countLedgers()).isEqualTo(ledgerCountBefore)

        val idempotency = fixtures.loadIdempotencyKey(memberId, idempotencyKey)
        assertThat(idempotency.status).isEqualTo("IN_PROGRESS")
        assertThat(idempotency.responseSnapshot).isNullOrEmpty()
    }

    private fun issueIdempotencyKey(
        accessToken: String,
        scope: IdempotencyKeyProps.IdempotencyScopeValue,
    ): String {
        val response = restTestClient.post()
            .uri { uriBuilder ->
                uriBuilder.path("/idempotency-keys")
                    .queryParamIfPresent("scope", Optional.ofNullable(scope))
                    .build()
            }
            .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
            .exchange()
            .expectStatus().isOk
            .expectBody(IdempotencyKeyCreateResponse::class.java)
            .returnResult()
            .responseBody ?: throw IllegalStateException("Idempotency key response is null")

        return response.idempotencyKey
    }

    private fun transfer(
        accessToken: String,
        idempotencyKey: String,
        fromAccountId: Long,
        toAccountId: Long,
        amount: BigDecimal,
    ): TransferResponse = restTestClient.post()
        .uri { uriBuilder -> uriBuilder.path("/transfers/$idempotencyKey").build() }
        .body(TransferRequest(fromAccountId, toAccountId, amount))
        .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk
        .expectBody(TransferResponse::class.java)
        .returnResult()
        .responseBody ?: throw IllegalStateException("Transfer response is null")

    private fun withdraw(
        accessToken: String,
        idempotencyKey: String,
        accountId: Long,
        amount: BigDecimal,
    ): TransferResponse = restTestClient.post()
        .uri { uriBuilder -> uriBuilder.path("/withdrawals/$idempotencyKey").build() }
        .body(WithdrawalRequest(accountId, amount))
        .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk
        .expectBody(TransferResponse::class.java)
        .returnResult()
        .responseBody ?: throw IllegalStateException("Withdrawal response is null")

    private fun deposit(
        accessToken: String,
        idempotencyKey: String,
        accountId: Long,
        amount: BigDecimal,
    ): TransferResponse = restTestClient.post()
        .uri { uriBuilder -> uriBuilder.path("/deposits/$idempotencyKey").build() }
        .body(DepositRequest(accountId, amount))
        .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk
        .expectBody(TransferResponse::class.java)
        .returnResult()
        .responseBody ?: throw IllegalStateException("Deposit response is null")

    private fun feeFor(amount: BigDecimal): BigDecimal = amount.multiply(TRANSFER_FEE_RATE).setScale(2, RoundingMode.DOWN)

    private fun assertLedger(
        ledgers: List<LedgerRow>,
        accountId: Long,
        side: String,
        amount: BigDecimal,
        before: LocalDateTime,
        after: LocalDateTime,
    ) {
        val ledger = ledgers.firstOrNull { it.accountId == accountId && it.side == side }
            ?: throw IllegalStateException("Ledger not found for account $accountId and side $side")

        assertThat(ledger.amount).isEqualByComparingTo(amount)
        assertThat(ledger.createdAt).isAfterOrEqualTo(before)
        assertThat(ledger.createdAt).isBeforeOrEqualTo(after)
    }

    private fun assertTransferSucceeded(response: TransferResponse) {
        assertThat(response.status).isEqualTo("SUCCEEDED")
        assertThat(response.transferId).isNotNull()
    }

    private fun assertBalance(
        accountId: Long,
        expected: BigDecimal,
    ) {
        assertThat(fixtures.loadBalance(accountId)).isEqualByComparingTo(expected)
    }

    private fun assertLedgers(
        transferId: Long,
        fromAccountId: Long,
        toAccountId: Long,
        debitAmount: BigDecimal,
        creditAmount: BigDecimal,
        before: LocalDateTime,
        after: LocalDateTime,
    ) {
        val ledgers = fixtures.loadLedgers(transferId)
        assertThat(ledgers).hasSize(2)
        assertLedger(ledgers, fromAccountId, "DEBIT", debitAmount, before, after)
        assertLedger(ledgers, toAccountId, "CREDIT", creditAmount, before, after)
    }

    private fun assertOutbox(
        transferId: Long,
        fromAccountId: Long,
        toAccountId: Long,
        amount: BigDecimal,
    ) {
        val outboxes = fixtures.loadOutboxEvents(transferId)
        assertThat(outboxes).hasSize(1)
        val outbox = outboxes.first()
        assertThat(outbox.status).isEqualTo("NEW")
        assertThat(outbox.payload).contains(
            "\"fromAccountId\":$fromAccountId",
            "\"toAccountId\":$toAccountId",
            "\"amount\":$amount",
        )
    }

    private fun assertIdempotency(
        memberId: Long,
        idempotencyKey: String,
    ) {
        val idempotency = fixtures.loadIdempotencyKey(memberId, idempotencyKey)
        assertThat(idempotency.status).isEqualTo("SUCCEEDED")
        assertThat(idempotency.responseSnapshot).isNotBlank()
    }

    data class TransferResponse(
        val status: String,
        val transferId: Long?,
        val errorCode: String?,
    )

    @TestConfiguration
    class TransferFailureConfig {
        @Bean
        fun transferFailureSwitch(): TransferFailureSwitch = TransferFailureSwitch()

        @Bean
        @Primary
        fun failureTransferRepository(
            delegate: TransferRepository,
            transferFailureSwitch: TransferFailureSwitch,
        ): TransferRepository = FailureTransferRepository(delegate, transferFailureSwitch)
    }

    class TransferFailureSwitch {
        private val enabled = AtomicBoolean(false)

        fun enable() {
            enabled.set(true)
        }

        fun disable() {
            enabled.set(false)
        }

        fun shouldFail(): Boolean = enabled.get()
    }

    private class FailureTransferRepository(
        private val delegate: TransferRepository,
        private val failureSwitch: TransferFailureSwitch,
    ) : TransferRepository {
        override fun save(props: TransferRequestProps): TransferModel {
            if (failureSwitch.shouldFail()) {
                throw IllegalStateException("Transfer save failed")
            }
            return delegate.save(props)
        }

        override fun findById(identifier: TransferIdentifier): TransferModel? = delegate.findById(identifier)

        override fun findCompletedByAccountId(
            identifier: AccountIdentifier,
            condition: TransferQueryCondition,
        ): List<TransferModel> = delegate.findCompletedByAccountId(identifier, condition)

        override fun sumAmountByFromAccountIdAndScopeBetween(
            identifier: AccountIdentifier,
            scope: TransferProps.TransferScopeValue,
            from: LocalDateTime,
            to: LocalDateTime,
        ): BigDecimal = delegate.sumAmountByFromAccountIdAndScopeBetween(identifier, scope, from, to)
    }

    private companion object {
        val TRANSFER_FEE_RATE: BigDecimal = BigDecimal("0.01")
    }
}
