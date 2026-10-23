package com.example.data.local

import androidx.room.TypeConverter
import com.example.domain.model.ExpenseFrequency
import com.example.domain.model.QuotationStatus
import java.math.BigDecimal

class Converters {
    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String {
        return value?.toPlainString() ?: "0.00"
    }

    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal {
        return try {
            if (value.isNullOrBlank()) BigDecimal.ZERO else BigDecimal(value)
        } catch (_: Exception) {
            BigDecimal.ZERO
        }
    }

    @TypeConverter
    fun fromQuotationStatus(status: QuotationStatus?): String {
        return (status ?: QuotationStatus.DRAFT).name
    }

    @TypeConverter
    fun toQuotationStatus(value: String?): QuotationStatus {
        return QuotationStatus.fromString(value ?: "DRAFT")
    }

    @TypeConverter
    fun fromExpenseFrequency(frequency: ExpenseFrequency?): String {
        return (frequency ?: ExpenseFrequency.MONTHLY).name
    }

    @TypeConverter
    fun toExpenseFrequency(value: String?): ExpenseFrequency {
        return ExpenseFrequency.fromString(value ?: "MONTHLY")
    }
}
