package org.yechan.remittance

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.math.BigDecimal

@Converter(autoApply = true)
class MoneyConverter : AttributeConverter<Money, BigDecimal> {

    override fun convertToDatabaseColumn(attribute: Money?): BigDecimal? = attribute?.amount

    override fun convertToEntityAttribute(dbData: BigDecimal?): Money? = dbData?.let { Money.of(it) }
}
