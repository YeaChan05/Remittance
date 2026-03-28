package org.yechan.remittance.transfer.config

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.yechan.remittance.transfer.TransferAccountClient
import org.yechan.remittance.transfer.TransferAccountLockCommand
import org.yechan.remittance.transfer.TransferAccountSnapshot
import org.yechan.remittance.transfer.TransferBalanceChangeCommand
import org.yechan.remittance.transfer.TransferLockedAccounts
import org.yechan.remittance.transfer.TransferMemberClient
import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Configuration
class TransferApplicationFakeClientBeanRegistrar :
    BeanRegistrarDsl({
        registerBean<TransferApplicationAccountStore> {
            TransferApplicationAccountStore()
        }

        registerBean<TransferApplicationMemberStore> {
            TransferApplicationMemberStore()
        }

        registerBean<TransferAccountClient>(primary = true) {
            TransferApplicationFakeAccountClient(bean())
        }

        registerBean<TransferMemberClient>(primary = true) {
            TransferApplicationFakeMemberClient(bean())
        }
    })

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
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                object : TransactionSynchronization {
                    override fun afterCommit() {
                        applyInternal(command)
                    }
                },
            )
            return
        }
        applyInternal(command)
    }

    private fun applyInternal(command: TransferBalanceChangeCommand) {
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

class TransferApplicationFakeAccountClient(
    private val store: TransferApplicationAccountStore,
) : TransferAccountClient {
    override fun get(accountId: Long): TransferAccountSnapshot? = store.find(accountId)

    override fun lock(command: TransferAccountLockCommand): TransferLockedAccounts? {
        val fromAccount = store.find(command.fromAccountId) ?: return null
        val toAccount = store.find(command.toAccountId) ?: return null
        return TransferLockedAccounts(fromAccount, toAccount)
    }

    override fun applyBalanceChange(command: TransferBalanceChangeCommand) {
        store.apply(command)
    }
}

class TransferApplicationFakeMemberClient(
    private val store: TransferApplicationMemberStore,
) : TransferMemberClient {
    override fun exists(memberId: Long): Boolean = store.exists(memberId)
}
