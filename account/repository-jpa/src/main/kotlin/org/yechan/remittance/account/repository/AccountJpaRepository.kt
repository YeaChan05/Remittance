package org.yechan.remittance.account.repository

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AccountJpaRepository : JpaRepository<AccountEntity, Long> {
    fun findByMemberIdAndBankCodeAndAccountNumber(
        memberId: Long,
        bankCode: String,
        accountNumber: String,
    ): AccountEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        select a from AccountEntity a
        where a.id = :accountId
        """,
    )
    fun findByIdForUpdate(
        @Param("accountId") accountId: Long,
    ): AccountEntity?
}
