package org.yechan.remittance.transfer.repository

import jakarta.persistence.LockModeType
import java.time.LocalDate
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.yechan.remittance.transfer.TransferProps

interface DailyLimitUsageJpaRepository : JpaRepository<DailyLimitUsageEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        select d from DailyLimitUsageEntity d
        where d.accountId = :accountId
          and d.scope = :scope
          and d.usageDate = :usageDate
        """
    )
    fun findForUpdate(
        @Param("accountId") accountId: Long,
        @Param("scope") scope: TransferProps.TransferScopeValue,
        @Param("usageDate") usageDate: LocalDate
    ): DailyLimitUsageEntity?
}
