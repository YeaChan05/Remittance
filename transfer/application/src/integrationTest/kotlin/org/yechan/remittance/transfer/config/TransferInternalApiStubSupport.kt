package org.yechan.remittance.transfer.config

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.yechan.remittance.InternalServiceAuthenticationFilter
import org.yechan.remittance.transfer.TransferAccountSnapshot
import org.yechan.remittance.transfer.TransferBalanceChangeCommand
import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

abstract class TransferInternalApiStubSupport {
    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            TransferInternalApiStubEnvironment.startIfNeeded()
            registry.add("spring.http.serviceclient.account-internal.base-url") {
                TransferInternalApiStubEnvironment.accountBaseUrl()
            }
            registry.add("spring.http.serviceclient.member-internal.base-url") {
                TransferInternalApiStubEnvironment.memberBaseUrl()
            }
        }
    }
}

object TransferInternalApiStubEnvironment {
    private val objectMapper = ObjectMapper()
    private val started = AtomicBoolean(false)
    private val accountServer = MockWebServer()
    private val memberServer = MockWebServer()

    val accountStore = TransferApplicationAccountStore()
    val memberStore = TransferApplicationMemberStore()

    fun startIfNeeded() {
        if (started.compareAndSet(false, true)) {
            accountServer.dispatcher = AccountInternalDispatcher(accountStore, objectMapper)
            memberServer.dispatcher = MemberInternalDispatcher(memberStore, objectMapper)
            accountServer.start()
            memberServer.start()
        }
    }

    fun accountBaseUrl(): String {
        startIfNeeded()
        return accountServer.url("/").toString().removeSuffix("/")
    }

    fun memberBaseUrl(): String {
        startIfNeeded()
        return memberServer.url("/").toString().removeSuffix("/")
    }
}

class TransferApplicationAccountStore {
    private val nextId = AtomicLong(1)
    private val accounts = ConcurrentHashMap<Long, TransferAccountSnapshot>()

    fun create(
        memberId: Long,
        balance: BigDecimal,
    ): TransferAccountSnapshot {
        val accountId = nextId.getAndIncrement()
        val snapshot = TransferAccountSnapshot(accountId, memberId, balance)
        accounts[accountId] = snapshot
        return snapshot
    }

    fun find(accountId: Long): TransferAccountSnapshot? = accounts[accountId]

    fun apply(command: TransferBalanceChangeCommand) {
        val fromAccount = accounts[command.fromAccountId] ?: return
        accounts[command.fromAccountId] = fromAccount.copy(balance = command.fromBalance)

        if (command.toAccountId == command.fromAccountId) {
            return
        }

        val toAccount = accounts[command.toAccountId] ?: return
        accounts[command.toAccountId] = toAccount.copy(balance = command.toBalance)
    }

    fun clear() {
        accounts.clear()
        nextId.set(1)
    }
}

class TransferApplicationMemberStore {
    private val members = ConcurrentHashMap.newKeySet<Long>()

    fun register(memberId: Long) {
        members += memberId
    }

    fun exists(memberId: Long): Boolean = members.contains(memberId)

    fun clear() {
        members.clear()
    }
}

private class AccountInternalDispatcher(
    private val store: TransferApplicationAccountStore,
    private val objectMapper: ObjectMapper,
) : Dispatcher() {
    override fun dispatch(request: RecordedRequest): MockResponse = when (request.requestUrl?.encodedPath) {
        "/internal/accounts/query" -> handleQuery(request)
        "/internal/accounts/lock" -> handleLock(request)
        "/internal/accounts/balance-change" -> handleBalanceChange(request)
        else -> MockResponse().setResponseCode(404)
    }

    private fun handleQuery(request: RecordedRequest): MockResponse {
        val accountId = body(request).get("accountId").longValue()
        val snapshot = store.find(accountId) ?: return MockResponse().setResponseCode(204)
        return json(
            mapOf(
                "accountId" to snapshot.accountId,
                "memberId" to snapshot.memberId,
                "balance" to snapshot.balance,
            ),
        )
    }

    private fun handleLock(request: RecordedRequest): MockResponse {
        val payload = body(request)
        val fromAccountId = payload.get("fromAccountId").longValue()
        val toAccountId = payload.get("toAccountId").longValue()
        val fromAccount = store.find(fromAccountId) ?: return MockResponse().setResponseCode(204)
        val toAccount = store.find(toAccountId) ?: return MockResponse().setResponseCode(204)
        return json(
            mapOf(
                "fromAccount" to snapshotResponse(fromAccount),
                "toAccount" to snapshotResponse(toAccount),
            ),
        )
    }

    private fun handleBalanceChange(request: RecordedRequest): MockResponse {
        val payload = body(request)
        store.apply(
            TransferBalanceChangeCommand(
                memberId = request.getHeader(InternalServiceAuthenticationFilter.INTERNAL_USER_ID_HEADER)
                    ?.toLongOrNull() ?: 0L,
                fromAccountId = payload.get("fromAccountId").longValue(),
                toAccountId = payload.get("toAccountId").longValue(),
                fromBalance = payload.get("fromBalance").decimalValue(),
                toBalance = payload.get("toBalance").decimalValue(),
            ),
        )
        return json(mapOf("applied" to true))
    }

    private fun snapshotResponse(snapshot: TransferAccountSnapshot): Map<String, Any> = mapOf(
        "accountId" to snapshot.accountId,
        "memberId" to snapshot.memberId,
        "balance" to snapshot.balance,
    )

    private fun body(request: RecordedRequest) = objectMapper.readTree(request.body.readUtf8())

    private fun json(payload: Any): MockResponse = MockResponse()
        .setHeader("Content-Type", "application/json")
        .setBody(objectMapper.writeValueAsString(payload))
}

private class MemberInternalDispatcher(
    private val store: TransferApplicationMemberStore,
    private val objectMapper: ObjectMapper,
) : Dispatcher() {
    override fun dispatch(request: RecordedRequest): MockResponse = when (request.requestUrl?.encodedPath) {
        "/internal/members/existence" -> {
            val memberId =
                objectMapper.readTree(request.body.readUtf8()).get("memberId").longValue()
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(mapOf("exists" to store.exists(memberId))))
        }

        else -> MockResponse().setResponseCode(404)
    }
}
