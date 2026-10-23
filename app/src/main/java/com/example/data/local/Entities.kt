package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.ExpenseFrequency
import com.example.domain.model.QuotationStatus
import java.math.BigDecimal

@Entity(
    tableName = "clients",
    indices = [Index(value = ["name"]), Index(value = ["company_name"])]
)
data class ClientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "company_name") val companyName: String = "",
    @ColumnInfo(name = "contact_person") val contactPerson: String = "",
    val phone: String = "",
    val email: String = "",
    @ColumnInfo(name = "billing_address") val billingAddress: String = "",
    @ColumnInfo(name = "tax_number") val taxNumber: String = "",
    val notes: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "catalog_items",
    indices = [Index(value = ["name"]), Index(value = ["category"])]
)
data class CatalogItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val sku: String = "",
    val category: String = "General",
    @ColumnInfo(name = "unit_type") val unitType: String = "units",
    @ColumnInfo(name = "default_unit_price") val defaultUnitPrice: BigDecimal = BigDecimal.ZERO,
    @ColumnInfo(name = "default_quantity") val defaultQuantity: BigDecimal = BigDecimal.ONE,
    @ColumnInfo(name = "cost_price") val costPrice: BigDecimal = BigDecimal.ZERO,
    @ColumnInfo(name = "tax_rate") val taxRate: BigDecimal = BigDecimal.ZERO,
    @ColumnInfo(name = "is_archived") val isArchived: Boolean = false
)

@Entity(
    tableName = "quotations",
    indices = [
        Index(value = ["quotation_number"], unique = true),
        Index(value = ["status"]),
        Index(value = ["client_id"]),
        Index(value = ["issue_date"])
    ]
)
data class QuotationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "quotation_number") val quotationNumber: String,
    @ColumnInfo(name = "client_id") val clientId: Long? = null,
    @ColumnInfo(name = "client_snapshot_name") val clientSnapshotName: String = "",
    @ColumnInfo(name = "client_snapshot_company") val clientSnapshotCompany: String = "",
    @ColumnInfo(name = "client_snapshot_contact") val clientSnapshotContact: String = "",
    @ColumnInfo(name = "client_snapshot_phone") val clientSnapshotPhone: String = "",
    @ColumnInfo(name = "client_snapshot_email") val clientSnapshotEmail: String = "",
    @ColumnInfo(name = "client_snapshot_address") val clientSnapshotAddress: String = "",
    @ColumnInfo(name = "client_snapshot_tax_number") val clientSnapshotTaxNumber: String = "",
    @ColumnInfo(name = "business_snapshot_name") val businessSnapshotName: String = "",
    @ColumnInfo(name = "business_snapshot_owner") val businessSnapshotOwner: String = "",
    @ColumnInfo(name = "business_snapshot_phone") val businessSnapshotPhone: String = "",
    @ColumnInfo(name = "business_snapshot_email") val businessSnapshotEmail: String = "",
    @ColumnInfo(name = "business_snapshot_address") val businessSnapshotAddress: String = "",
    @ColumnInfo(name = "business_snapshot_website") val businessSnapshotWebsite: String = "",
    @ColumnInfo(name = "business_snapshot_reg_number") val businessSnapshotRegNumber: String = "",
    @ColumnInfo(name = "business_snapshot_tax_number") val businessSnapshotTaxNumber: String = "",
    @ColumnInfo(name = "business_snapshot_footer") val businessSnapshotFooter: String = "",
    @ColumnInfo(name = "issue_date") val issueDate: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "valid_until_date") val validUntilDate: Long = System.currentTimeMillis(),
    val currency: String = "USD",
    val subtotal: BigDecimal = BigDecimal.ZERO,
    @ColumnInfo(name = "line_discounts_total") val lineDiscountsTotal: BigDecimal = BigDecimal.ZERO,
    @ColumnInfo(name = "overall_discount") val overallDiscount: BigDecimal = BigDecimal.ZERO,
    @ColumnInfo(name = "taxable_amount") val taxableAmount: BigDecimal = BigDecimal.ZERO,
    @ColumnInfo(name = "tax_total") val taxTotal: BigDecimal = BigDecimal.ZERO,
    @ColumnInfo(name = "additional_fee") val additionalFee: BigDecimal = BigDecimal.ZERO,
    @ColumnInfo(name = "grand_total") val grandTotal: BigDecimal = BigDecimal.ZERO,
    @ColumnInfo(name = "payment_terms") val paymentTerms: String = "",
    @ColumnInfo(name = "delivery_terms") val deliveryTerms: String = "",
    val notes: String = "",
    val status: QuotationStatus = QuotationStatus.DRAFT,
    @ColumnInfo(name = "revision_number") val revisionNumber: Int = 1,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "modified_at") val modifiedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "quotation_items",
    indices = [
        Index(value = ["quotation_id"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = QuotationEntity::class,
            parentColumns = ["id"],
            childColumns = ["quotation_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class QuotationItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "quotation_id") val quotationId: Long,
    @ColumnInfo(name = "catalog_item_id") val catalogItemId: Long? = null,
    val name: String,
    val description: String = "",
    @ColumnInfo(name = "unit_type") val unitType: String = "units",
    val quantity: BigDecimal = BigDecimal.ONE,
    @ColumnInfo(name = "unit_price") val unitPrice: BigDecimal = BigDecimal.ZERO,
    @ColumnInfo(name = "discount_percent") val discountPercent: BigDecimal = BigDecimal.ZERO,
    @ColumnInfo(name = "discount_fixed") val discountFixed: BigDecimal = BigDecimal.ZERO,
    @ColumnInfo(name = "tax_percent") val taxPercent: BigDecimal = BigDecimal.ZERO,
    @ColumnInfo(name = "gross_amount") val grossAmount: BigDecimal = BigDecimal.ZERO,
    @ColumnInfo(name = "line_discount_amount") val lineDiscountAmount: BigDecimal = BigDecimal.ZERO,
    @ColumnInfo(name = "taxable_amount") val taxableAmount: BigDecimal = BigDecimal.ZERO,
    @ColumnInfo(name = "tax_amount") val taxAmount: BigDecimal = BigDecimal.ZERO,
    @ColumnInfo(name = "line_total") val lineTotal: BigDecimal = BigDecimal.ZERO
)

@Entity(
    tableName = "recurring_expenses",
    indices = [Index(value = ["is_active"]), Index(value = ["category"])]
)
data class RecurringExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String = "Software",
    val amount: BigDecimal = BigDecimal.ZERO,
    val currency: String = "USD",
    val frequency: ExpenseFrequency = ExpenseFrequency.MONTHLY,
    @ColumnInfo(name = "billing_start_date") val billingStartDate: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "billing_end_date") val billingEndDate: Long? = null,
    @ColumnInfo(name = "next_due_date") val nextDueDate: Long? = null,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    val notes: String = ""
)

@Entity(tableName = "business_profile")
data class BusinessProfileEntity(
    @PrimaryKey val id: Long = 1,
    @ColumnInfo(name = "business_name") val businessName: String = "ProTech Solutions",
    @ColumnInfo(name = "owner_name") val ownerName: String = "Alex Morgan",
    val phone: String = "+1 (555) 019-2834",
    val email: String = "contact@protechsolutions.com",
    val address: String = "100 Innovation Way, Suite 400\nSan Francisco, CA 94107",
    val website: String = "https://protechsolutions.com",
    @ColumnInfo(name = "reg_number") val regNumber: String = "BRN-8492048",
    @ColumnInfo(name = "tax_number") val taxNumber: String = "VAT-US928374",
    @ColumnInfo(name = "default_currency") val defaultCurrency: String = "USD",
    @ColumnInfo(name = "default_tax_rate") val defaultTaxRate: BigDecimal = BigDecimal("8.50"),
    @ColumnInfo(name = "default_validity_days") val defaultValidityDays: Int = 14,
    @ColumnInfo(name = "quotation_prefix") val quotationPrefix: String = "QT-2026-",
    @ColumnInfo(name = "default_payment_terms") val defaultPaymentTerms: String = "Payment due within 14 days of invoice.",
    @ColumnInfo(name = "default_delivery_terms") val defaultDeliveryTerms: String = "Delivery per project schedule.",
    @ColumnInfo(name = "default_footer_text") val defaultFooterText: String = "Thank you for your business!"
)
