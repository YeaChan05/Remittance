package org.yechan.remittance.transfer.repository

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.yechan.remittance.BaseEntity
import org.yechan.remittance.transfer.DailyLimitUsageModel
import org.yechan.remittance.transfer.DailyLimitUsageProps
import org.yechan.remittance.transfer.TransferProps
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(
    name = "daily_limit_usage",
    catalog = "core",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_daily_limit_usage_account_scope_date",
            columnNames = ["account_id", "scope", "usage_date"],
        ),
    ],
)
class DailyLimitUsageEntity() :
    BaseEntity(),
    DailyLimitUsageModel {
    @field:Column(name = "account_id", nullable = false)
    final override var accountId: Long = 0
        private set

    @field:Enumerated(EnumType.STRING)
    @field:Column(nullable = false)
    final override var scope: TransferProps.TransferScopeValue = TransferProps.TransferScopeValue.TRANSFER
        private set

    @field:Column(name = "usage_date", nullable = false)
    final override var usageDate: LocalDate = LocalDate.MIN
        private set

    @field:Column(name = "used_amount", nullable = false)
    final override var usedAmount: BigDecimal = BigDecimal.ZERO
        private set

    private constructor(
        accountId: Long,
        scope: TransferProps.TransferScopeValue,
        usageDate: LocalDate,
        usedAmount: BigDecimal,
    ) : this() {
        this.accountId = accountId
        this.scope = scope
        this.usageDate = usageDate
        this.usedAmount = usedAmount
    }

    override val dailyLimitUsageId: Long?
        get() = id

    override fun updateUsedAmount(usedAmount: BigDecimal) {
        this.usedAmount = usedAmount
    }

    companion object {
        fun create(props: DailyLimitUsageProps): DailyLimitUsageEntity = DailyLimitUsageEntity(props.accountId, props.scope, props.usageDate, props.usedAmount)
    }
}
