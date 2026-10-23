package com.example.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class QuotationStatus(val displayName: String, val sinhalaName: String) {
    DRAFT("Draft", "කෙටුම්පත"),
    SENT("Sent", "යවන ලද"),
    ACCEPTED("Accepted", "පිළිගත්"),
    REJECTED("Rejected", "ප්‍රතික්ෂේපිත"),
    EXPIRED("Expired", "කල් ඉකුත් වූ"),
    CANCELLED("Cancelled", "අවලංගු කළ");

    companion object {
        fun fromString(value: String): QuotationStatus {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: DRAFT
        }
    }
}

enum class ExpenseFrequency(val displayName: String, val sinhalaName: String, val factorToMonthly: Double) {
    DAILY("Daily", "දිනපතා", 365.0 / 12.0),
    WEEKLY("Weekly", "සතිපතා", 52.0 / 12.0),
    MONTHLY("Monthly", "මාසිකව", 1.0),
    QUARTERLY("Quarterly", "කාර්තුමය", 1.0 / 3.0),
    YEARLY("Yearly", "වාර්ෂිකව", 1.0 / 12.0);

    companion object {
        fun fromString(value: String): ExpenseFrequency {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: MONTHLY
        }
    }
}

enum class PricingStrategy {
    PROFIT_MARGIN,
    MARKUP
}

data class AppCurrency(
    val code: String,
    val symbol: String,
    val name: String
) {
    companion object {
        val USD = AppCurrency("USD", "$", "US Dollar")
        val LKR = AppCurrency("LKR", "Rs", "Sri Lankan Rupee")
        val EUR = AppCurrency("EUR", "€", "Euro")
        val GBP = AppCurrency("GBP", "£", "British Pound")
        val AUD = AppCurrency("AUD", "A$", "Australian Dollar")
        val CAD = AppCurrency("CAD", "C$", "Canadian Dollar")
        val INR = AppCurrency("INR", "₹", "Indian Rupee")
        val SGD = AppCurrency("SGD", "S$", "Singapore Dollar")
        val AED = AppCurrency("AED", "AED", "UAE Dirham")

        val ALL = listOf(USD, LKR, EUR, GBP, AUD, CAD, INR, SGD, AED)

        fun find(code: String): AppCurrency {
            return ALL.find { it.code.equals(code, ignoreCase = true) } ?: USD
        }

        fun formatAmount(amount: BigDecimal, currencyCode: String = "USD"): String {
            val curr = find(currencyCode)
            val format = NumberFormat.getNumberInstance(Locale.US).apply {
                minimumFractionDigits = 2
                maximumFractionDigits = 2
            }
            return "${curr.symbol} ${format.format(amount.setScale(2, RoundingMode.HALF_UP))}"
        }

        fun formatAmount(amount: Double, currencyCode: String = "USD"): String {
            return formatAmount(BigDecimal.valueOf(amount), currencyCode)
        }
    }
}

data class Client(
    val id: Long = 0,
    val name: String,
    val companyName: String = "",
    val contactPerson: String = "",
    val phone: String = "",
    val email: String = "",
    val billingAddress: String = "",
    val taxNumber: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val displayName: String
        get() = if (companyName.isNotBlank() && name.isNotBlank() && companyName != name) {
            "$name ($companyName)"
        } else if (name.isNotBlank()) {
            name
        } else if (companyName.isNotBlank()) {
            companyName
        } else {
            "Unnamed Client"
        }
}

data class CatalogItem(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val sku: String = "",
    val category: String = "General",
    val unitType: String = "units",
    val defaultUnitPrice: BigDecimal = BigDecimal.ZERO,
    val defaultQuantity: BigDecimal = BigDecimal.ONE,
    val costPrice: BigDecimal = BigDecimal.ZERO,
    val taxRate: BigDecimal = BigDecimal.ZERO,
    val isArchived: Boolean = false
)

data class QuotationItem(
    val id: Long = 0,
    val quotationId: Long = 0,
    val catalogItemId: Long? = null,
    val name: String,
    val description: String = "",
    val unitType: String = "units",
    val quantity: BigDecimal = BigDecimal.ONE,
    val unitPrice: BigDecimal = BigDecimal.ZERO,
    val discountPercent: BigDecimal = BigDecimal.ZERO,
    val discountFixed: BigDecimal = BigDecimal.ZERO,
    val taxPercent: BigDecimal = BigDecimal.ZERO,
    val grossAmount: BigDecimal = BigDecimal.ZERO,
    val lineDiscountAmount: BigDecimal = BigDecimal.ZERO,
    val taxableAmount: BigDecimal = BigDecimal.ZERO,
    val taxAmount: BigDecimal = BigDecimal.ZERO,
    val lineTotal: BigDecimal = BigDecimal.ZERO
)

data class Quotation(
    val id: Long = 0,
    val quotationNumber: String,
    val clientId: Long? = null,
    val clientSnapshotName: String = "",
    val clientSnapshotCompany: String = "",
    val clientSnapshotContact: String = "",
    val clientSnapshotPhone: String = "",
    val clientSnapshotEmail: String = "",
    val clientSnapshotAddress: String = "",
    val clientSnapshotTaxNumber: String = "",
    val businessSnapshotName: String = "",
    val businessSnapshotOwner: String = "",
    val businessSnapshotPhone: String = "",
    val businessSnapshotEmail: String = "",
    val businessSnapshotAddress: String = "",
    val businessSnapshotWebsite: String = "",
    val businessSnapshotRegNumber: String = "",
    val businessSnapshotTaxNumber: String = "",
    val businessSnapshotFooter: String = "",
    val issueDate: Long = System.currentTimeMillis(),
    val validUntilDate: Long = System.currentTimeMillis() + (14L * 24 * 3600 * 1000),
    val currency: String = "USD",
    val subtotal: BigDecimal = BigDecimal.ZERO,
    val lineDiscountsTotal: BigDecimal = BigDecimal.ZERO,
    val overallDiscount: BigDecimal = BigDecimal.ZERO,
    val taxableAmount: BigDecimal = BigDecimal.ZERO,
    val taxTotal: BigDecimal = BigDecimal.ZERO,
    val additionalFee: BigDecimal = BigDecimal.ZERO,
    val grandTotal: BigDecimal = BigDecimal.ZERO,
    val paymentTerms: String = "Payment due within 14 days of quotation acceptance.",
    val deliveryTerms: String = "Work commences upon receipt of initial deposit.",
    val notes: String = "Thank you for considering our services.",
    val status: QuotationStatus = QuotationStatus.DRAFT,
    val revisionNumber: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val items: List<QuotationItem> = emptyList()
) {
    val isExpired: Boolean
        get() = status != QuotationStatus.ACCEPTED && 
                status != QuotationStatus.REJECTED && 
                status != QuotationStatus.CANCELLED && 
                System.currentTimeMillis() > validUntilDate

    val effectiveStatus: QuotationStatus
        get() = if (isExpired && status == QuotationStatus.SENT) QuotationStatus.EXPIRED else status

    fun formattedIssueDate(): String = formatDate(issueDate)
    fun formattedValidUntil(): String = formatDate(validUntilDate)

    companion object {
        fun formatDate(millis: Long): String {
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            return sdf.format(Date(millis))
        }
    }
}

data class RecurringExpense(
    val id: Long = 0,
    val name: String,
    val category: String = "Software",
    val amount: BigDecimal = BigDecimal.ZERO,
    val currency: String = "USD",
    val frequency: ExpenseFrequency = ExpenseFrequency.MONTHLY,
    val billingStartDate: Long = System.currentTimeMillis(),
    val billingEndDate: Long? = null,
    val nextDueDate: Long? = null,
    val isActive: Boolean = true,
    val notes: String = ""
) {
    val monthlyEquivalent: BigDecimal
        get() = amount.multiply(BigDecimal.valueOf(frequency.factorToMonthly)).setScale(2, RoundingMode.HALF_UP)

    val annualEquivalent: BigDecimal
        get() = monthlyEquivalent.multiply(BigDecimal.valueOf(12)).setScale(2, RoundingMode.HALF_UP)
}

data class BusinessProfile(
    val id: Long = 1,
    val businessName: String = "ProTech Solutions",
    val ownerName: String = "Alex Morgan",
    val phone: String = "+1 (555) 019-2834",
    val email: String = "contact@protechsolutions.com",
    val address: String = "100 Innovation Way, Suite 400\nSan Francisco, CA 94107",
    val website: String = "https://protechsolutions.com",
    val regNumber: String = "BRN-8492048",
    val taxNumber: String = "VAT-US928374",
    val defaultCurrency: String = "USD",
    val defaultTaxRate: BigDecimal = BigDecimal("8.50"),
    val defaultValidityDays: Int = 14,
    val quotationPrefix: String = "QT-2026-",
    val defaultPaymentTerms: String = "Payment due within 14 days of invoice. 50% deposit required upon quotation approval.",
    val defaultDeliveryTerms: String = "Delivery within agreed milestone schedule.",
    val defaultFooterText: String = "Thank you for your business! All quotations are subject to our standard terms of service."
)
