package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.QuotationEntity
import com.example.data.local.QuotationItemEntity
import com.example.domain.calculator.FinancialCalculator
import com.example.domain.model.BusinessProfile
import com.example.domain.model.CatalogItem
import com.example.domain.model.Client
import com.example.domain.model.ExpenseFrequency
import com.example.domain.model.Quotation
import com.example.domain.model.QuotationItem
import com.example.domain.model.QuotationStatus
import com.example.domain.model.RecurringExpense
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.util.Calendar

class QuotationRepository(private val db: AppDatabase) {
    private val clientDao = db.clientDao()
    private val catalogDao = db.catalogItemDao()
    private val quotationDao = db.quotationDao()
    private val quotationItemDao = db.quotationItemDao()
    private val recurringDao = db.recurringExpenseDao()
    private val profileDao = db.businessProfileDao()

    // --- Clients ---
    val allClients: Flow<List<Client>> = clientDao.getAllClients()
        .map { list -> list.map { it.toDomain() } }
        .flowOn(Dispatchers.IO)

    fun getClient(id: Long): Flow<Client?> = clientDao.getClientById(id)
        .map { it?.toDomain() }
        .flowOn(Dispatchers.IO)

    suspend fun saveClient(client: Client): Long = withContext(Dispatchers.IO) {
        if (client.id == 0L) {
            clientDao.insert(client.toEntity())
        } else {
            clientDao.update(client.toEntity())
            client.id
        }
    }

    suspend fun deleteClient(id: Long) = withContext(Dispatchers.IO) {
        clientDao.deleteById(id)
    }

    // --- Catalog Items ---
    val allCatalogItems: Flow<List<CatalogItem>> = catalogDao.getAllItems()
        .map { list -> list.map { it.toDomain() } }
        .flowOn(Dispatchers.IO)

    val activeCatalogItems: Flow<List<CatalogItem>> = catalogDao.getActiveItems()
        .map { list -> list.map { it.toDomain() } }
        .flowOn(Dispatchers.IO)

    suspend fun saveCatalogItem(item: CatalogItem): Long = withContext(Dispatchers.IO) {
        if (item.id == 0L) {
            catalogDao.insert(item.toEntity())
        } else {
            catalogDao.update(item.toEntity())
            item.id
        }
    }

    suspend fun deleteCatalogItem(item: CatalogItem) = withContext(Dispatchers.IO) {
        catalogDao.delete(item.toEntity())
    }

    // --- Recurring Expenses ---
    val allRecurringExpenses: Flow<List<RecurringExpense>> = recurringDao.getAllExpenses()
        .map { list -> list.map { it.toDomain() } }
        .flowOn(Dispatchers.IO)

    suspend fun saveRecurringExpense(expense: RecurringExpense): Long = withContext(Dispatchers.IO) {
        if (expense.id == 0L) {
            recurringDao.insert(expense.toEntity())
        } else {
            recurringDao.update(expense.toEntity())
            expense.id
        }
    }

    suspend fun deleteRecurringExpense(id: Long) = withContext(Dispatchers.IO) {
        recurringDao.deleteById(id)
    }

    // --- Business Profile ---
    val businessProfile: Flow<BusinessProfile> = profileDao.getProfile()
        .map { it?.toDomain() ?: BusinessProfile() }
        .flowOn(Dispatchers.IO)

    suspend fun saveBusinessProfile(profile: BusinessProfile) = withContext(Dispatchers.IO) {
        profileDao.insertOrUpdate(profile.toEntity())
    }

    suspend fun getBusinessProfileDirect(): BusinessProfile = withContext(Dispatchers.IO) {
        profileDao.getProfileDirect()?.toDomain() ?: BusinessProfile()
    }

    // --- Quotations ---
    val allQuotations: Flow<List<Quotation>> = quotationDao.getAllQuotations()
        .map { list ->
            list.map { qEntity ->
                val items = quotationItemDao.getItemsForQuotationDirect(qEntity.id).map { it.toDomain() }
                qEntity.toDomain(items)
            }
        }
        .flowOn(Dispatchers.IO)

    fun getQuotation(id: Long): Flow<Quotation?> {
        val qFlow = quotationDao.getQuotationById(id)
        val itemsFlow = quotationItemDao.getItemsForQuotation(id)
        return combine(qFlow, itemsFlow) { qEntity, itemEntities ->
            qEntity?.toDomain(itemEntities.map { it.toDomain() })
        }.flowOn(Dispatchers.IO)
    }

    suspend fun getQuotationDirect(id: Long): Quotation? = withContext(Dispatchers.IO) {
        val qEntity = quotationDao.getQuotationByIdDirect(id) ?: return@withContext null
        val items = quotationItemDao.getItemsForQuotationDirect(id).map { it.toDomain() }
        qEntity.toDomain(items)
    }

    suspend fun saveQuotation(quotation: Quotation, items: List<QuotationItem>): Long = withContext(Dispatchers.IO) {
        db.withTransaction {
            // Re-calculate all line items and totals
            val recalculatedItems = items.map { item ->
                val calc = FinancialCalculator.calculateLineItem(
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    discountPercent = item.discountPercent,
                    discountFixed = item.discountFixed,
                    taxPercent = item.taxPercent
                )
                item.copy(
                    grossAmount = calc.grossAmount,
                    lineDiscountAmount = calc.discountAmount,
                    taxableAmount = calc.taxableAmount,
                    taxAmount = calc.taxAmount,
                    lineTotal = calc.lineTotal
                )
            }

            val totals = FinancialCalculator.calculateQuotationTotals(
                items = recalculatedItems,
                overallDiscount = quotation.overallDiscount,
                additionalFee = quotation.additionalFee
            )

            val finalQuotation = quotation.copy(
                subtotal = totals.subtotal,
                lineDiscountsTotal = totals.totalLineDiscounts,
                overallDiscount = totals.overallDiscount,
                taxableAmount = totals.taxableAmount,
                taxTotal = totals.totalTax,
                additionalFee = totals.additionalFee,
                grandTotal = totals.grandTotal,
                modifiedAt = System.currentTimeMillis()
            )

            val quotationId: Long
            if (finalQuotation.id == 0L) {
                quotationId = quotationDao.insert(finalQuotation.toEntity())
            } else {
                quotationId = finalQuotation.id
                quotationDao.update(finalQuotation.toEntity())
                quotationItemDao.deleteByQuotationId(quotationId)
            }

            val itemEntities = recalculatedItems.map { it.toEntity(quotationId) }
            quotationItemDao.insertAll(itemEntities)

            quotationId
        }
    }

    suspend fun updateQuotationStatus(id: Long, status: QuotationStatus) = withContext(Dispatchers.IO) {
        quotationDao.updateStatus(id, status)
    }

    suspend fun deleteQuotation(id: Long) = withContext(Dispatchers.IO) {
        db.withTransaction {
            quotationItemDao.deleteByQuotationId(id)
            quotationDao.deleteById(id)
        }
    }

    suspend fun duplicateQuotation(id: Long): Long = withContext(Dispatchers.IO) {
        val original = getQuotationDirect(id) ?: return@withContext -1L
        val profile = getBusinessProfileDirect()
        val nextNumber = generateNextQuotationNumber(profile.quotationPrefix)

        val newQuote = original.copy(
            id = 0,
            quotationNumber = nextNumber,
            status = QuotationStatus.DRAFT,
            issueDate = System.currentTimeMillis(),
            validUntilDate = System.currentTimeMillis() + (profile.defaultValidityDays.toLong() * 24 * 3600 * 1000),
            revisionNumber = 1,
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis()
        )

        val newItems = original.items.map { it.copy(id = 0, quotationId = 0) }
        saveQuotation(newQuote, newItems)
    }

    suspend fun generateNextQuotationNumber(prefix: String): String = withContext(Dispatchers.IO) {
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val cleanPrefix = if (prefix.contains("{YYYY}")) {
            prefix.replace("{YYYY}", year.toString())
        } else if (prefix.isNotBlank()) {
            prefix
        } else {
            "QT-$year-"
        }

        val allQuotes = quotationDao.getAllQuotationsDirect()
        var maxSeq = 0
        for (q in allQuotes) {
            val num = q.quotationNumber
            if (num.startsWith(cleanPrefix)) {
                val suffix = num.removePrefix(cleanPrefix)
                val seq = suffix.toIntOrNull()
                if (seq != null && seq > maxSeq) {
                    maxSeq = seq
                }
            }
        }
        val nextSeq = maxSeq + 1
        String.format("%s%04d", cleanPrefix, nextSeq)
    }

    // --- Seed Realistic Demo Data ---
    suspend fun seedDemoData() = withContext(Dispatchers.IO) {
        db.withTransaction {
            // Business Profile
            val profile = BusinessProfile(
                id = 1,
                businessName = "Apex Digital Studio",
                ownerName = "Kavinda Perera",
                phone = "+94 77 123 4567",
                email = "hello@apexdigital.io",
                address = "Level 8, World Trade Center, Colombo 01, Sri Lanka",
                website = "https://apexdigital.io",
                regNumber = "PV-0028194",
                taxNumber = "VAT-102938475",
                defaultCurrency = "USD",
                defaultTaxRate = BigDecimal("15.00"),
                defaultValidityDays = 14,
                quotationPrefix = "QT-2026-",
                defaultPaymentTerms = "50% upfront deposit upon acceptance. 25% upon design approval. 25% on final launch.",
                defaultDeliveryTerms = "Estimated timeline: 4-6 weeks from initial kickoff.",
                defaultFooterText = "Thank you for partnering with Apex Digital Studio! All proposals are confidential."
            )
            profileDao.insertOrUpdate(profile.toEntity())

            // Clients
            val c1Id = clientDao.insert(
                Client(
                    name = "Nimali Rathnayake",
                    companyName = "Horizon Ceylon Exports",
                    contactPerson = "Nimali Rathnayake",
                    phone = "+94 71 889 9123",
                    email = "nimali@horizonexports.lk",
                    billingAddress = "45 Galle Road, Mount Lavinia, Sri Lanka",
                    taxNumber = "TIN-92837411",
                    notes = "Premium tea and spices exporter looking for international e-commerce site."
                ).toEntity()
            )

            val c2Id = clientDao.insert(
                Client(
                    name = "Marcus Vance",
                    companyName = "Vance Health Tech",
                    contactPerson = "Marcus Vance",
                    phone = "+1 (415) 890-3412",
                    email = "m.vance@vancehealth.co",
                    billingAddress = "500 Market St, Suite 210, San Francisco, CA",
                    taxNumber = "US-EIN-9481029",
                    notes = "Health monitoring SaaS platform client."
                ).toEntity()
            )

            val c3Id = clientDao.insert(
                Client(
                    name = "Dr. Samantha Silva",
                    companyName = "Lanka Wellness Center",
                    contactPerson = "Dr. Samantha Silva",
                    phone = "+94 11 234 5678",
                    email = "info@lankawellness.lk",
                    billingAddress = "12 Kynsey Road, Colombo 07",
                    taxNumber = "VAT-8839201",
                    notes = "Regular maintenance and brand identity client."
                ).toEntity()
            )

            // Catalog Items
            val cat1Id = catalogDao.insert(
                CatalogItem(
                    name = "E-Commerce Website Development",
                    description = "Custom responsive storefront, shopping cart, payment gateway integration, and CMS admin.",
                    sku = "WEB-ECOM",
                    category = "Web Development",
                    unitType = "project",
                    defaultUnitPrice = BigDecimal("2800.00"),
                    defaultQuantity = BigDecimal.ONE,
                    costPrice = BigDecimal("1200.00"),
                    taxRate = BigDecimal("15.00")
                ).toEntity()
            )

            val cat2Id = catalogDao.insert(
                CatalogItem(
                    name = "UI/UX Mobile App Design",
                    description = "Figma wireframes, high-fidelity prototypes, component design system, and user testing.",
                    sku = "DSGN-UIUX",
                    category = "Design",
                    unitType = "screens",
                    defaultUnitPrice = BigDecimal("120.00"),
                    defaultQuantity = BigDecimal("15"),
                    costPrice = BigDecimal("50.00"),
                    taxRate = BigDecimal("15.00")
                ).toEntity()
            )

            val cat3Id = catalogDao.insert(
                CatalogItem(
                    name = "Cloud Architecture & DevOps Consulting",
                    description = "AWS/GCP infrastructure provisioning, automated CI/CD pipelines, Docker containerization.",
                    sku = "CNS-CLOUD",
                    category = "Consulting",
                    unitType = "hrs",
                    defaultUnitPrice = BigDecimal("85.00"),
                    defaultQuantity = BigDecimal("20"),
                    costPrice = BigDecimal("35.00"),
                    taxRate = BigDecimal("15.00")
                ).toEntity()
            )

            val cat4Id = catalogDao.insert(
                CatalogItem(
                    name = "Annual Maintenance & Support Plan",
                    description = "24/7 uptime monitoring, security patches, bi-weekly backups, and 10 support hours/month.",
                    sku = "SUP-ANNUAL",
                    category = "Maintenance",
                    unitType = "months",
                    defaultUnitPrice = BigDecimal("350.00"),
                    defaultQuantity = BigDecimal("12"),
                    costPrice = BigDecimal("150.00"),
                    taxRate = BigDecimal("15.00")
                ).toEntity()
            )

            // Recurring Expenses
            recurringDao.insert(
                RecurringExpense(
                    name = "GitHub Enterprise & Copilot",
                    category = "Software",
                    amount = BigDecimal("42.00"),
                    currency = "USD",
                    frequency = ExpenseFrequency.MONTHLY,
                    isActive = true,
                    notes = "Team code repositories and AI tool licenses."
                ).toEntity()
            )

            recurringDao.insert(
                RecurringExpense(
                    name = "Google Workspace & Cloud Storage",
                    category = "Software",
                    amount = BigDecimal("72.00"),
                    currency = "USD",
                    frequency = ExpenseFrequency.MONTHLY,
                    isActive = true,
                    notes = "Business email, drive storage, and workspace apps."
                ).toEntity()
            )

            recurringDao.insert(
                RecurringExpense(
                    name = "AWS Production Servers & CDN",
                    category = "Hosting",
                    amount = BigDecimal("185.00"),
                    currency = "USD",
                    frequency = ExpenseFrequency.MONTHLY,
                    isActive = true,
                    notes = "Cloud staging and demo environment hosting."
                ).toEntity()
            )

            recurringDao.insert(
                RecurringExpense(
                    name = "Figma Professional Team Plan",
                    category = "Design Tools",
                    amount = BigDecimal("45.00"),
                    currency = "USD",
                    frequency = ExpenseFrequency.MONTHLY,
                    isActive = true,
                    notes = "3 design editor seats."
                ).toEntity()
            )

            recurringDao.insert(
                RecurringExpense(
                    name = "High-Speed Fiber Business Internet",
                    category = "Office & Utilities",
                    amount = BigDecimal("65.00"),
                    currency = "USD",
                    frequency = ExpenseFrequency.MONTHLY,
                    isActive = true,
                    notes = "Office leased line 200Mbps."
                ).toEntity()
            )

            recurringDao.insert(
                RecurringExpense(
                    name = "Annual Corporate Legal & Accounting Audit",
                    category = "Legal & Accounting",
                    amount = BigDecimal("1200.00"),
                    currency = "USD",
                    frequency = ExpenseFrequency.YEARLY,
                    isActive = true,
                    notes = "Annual tax filing and legal audit."
                ).toEntity()
            )

            // Quotation 1: Sent (Horizon Ceylon Exports)
            val q1Number = "QT-2026-0001"
            val q1Id = quotationDao.insert(
                QuotationEntity(
                    quotationNumber = q1Number,
                    clientId = c1Id,
                    clientSnapshotName = "Nimali Rathnayake",
                    clientSnapshotCompany = "Horizon Ceylon Exports",
                    clientSnapshotContact = "Nimali Rathnayake",
                    clientSnapshotPhone = "+94 71 889 9123",
                    clientSnapshotEmail = "nimali@horizonexports.lk",
                    clientSnapshotAddress = "45 Galle Road, Mount Lavinia, Sri Lanka",
                    clientSnapshotTaxNumber = "TIN-92837411",
                    businessSnapshotName = profile.businessName,
                    businessSnapshotOwner = profile.ownerName,
                    businessSnapshotPhone = profile.phone,
                    businessSnapshotEmail = profile.email,
                    businessSnapshotAddress = profile.address,
                    businessSnapshotWebsite = profile.website,
                    businessSnapshotRegNumber = profile.regNumber,
                    businessSnapshotTaxNumber = profile.taxNumber,
                    businessSnapshotFooter = profile.defaultFooterText,
                    issueDate = System.currentTimeMillis() - (2L * 24 * 3600 * 1000),
                    validUntilDate = System.currentTimeMillis() + (12L * 24 * 3600 * 1000),
                    currency = "USD",
                    subtotal = BigDecimal("4600.00"),
                    lineDiscountsTotal = BigDecimal("200.00"),
                    overallDiscount = BigDecimal("100.00"),
                    taxableAmount = BigDecimal("4300.00"),
                    taxTotal = BigDecimal("645.00"),
                    additionalFee = BigDecimal("50.00"),
                    grandTotal = BigDecimal("4995.00"),
                    paymentTerms = profile.defaultPaymentTerms,
                    deliveryTerms = profile.defaultDeliveryTerms,
                    notes = "Includes 3 months of complimentary warranty support.",
                    status = QuotationStatus.SENT,
                    revisionNumber = 1,
                    createdAt = System.currentTimeMillis() - (2L * 24 * 3600 * 1000),
                    modifiedAt = System.currentTimeMillis() - (2L * 24 * 3600 * 1000)
                )
            )

            quotationItemDao.insertAll(
                listOf(
                    QuotationItemEntity(
                        quotationId = q1Id,
                        catalogItemId = cat1Id,
                        name = "E-Commerce Website Development",
                        description = "Full responsive online store with payment gateway, catalog, and inventory sync.",
                        unitType = "project",
                        quantity = BigDecimal.ONE,
                        unitPrice = BigDecimal("2800.00"),
                        discountPercent = BigDecimal.ZERO,
                        discountFixed = BigDecimal("200.00"),
                        taxPercent = BigDecimal("15.00"),
                        grossAmount = BigDecimal("2800.00"),
                        lineDiscountAmount = BigDecimal("200.00"),
                        taxableAmount = BigDecimal("2600.00"),
                        taxAmount = BigDecimal("390.00"),
                        lineTotal = BigDecimal("2990.00")
                    ),
                    QuotationItemEntity(
                        quotationId = q1Id,
                        catalogItemId = cat2Id,
                        name = "UI/UX Mobile App Design",
                        description = "Design of 15 core screens with responsive tablet and mobile views.",
                        unitType = "screens",
                        quantity = BigDecimal("15"),
                        unitPrice = BigDecimal("120.00"),
                        discountPercent = BigDecimal.ZERO,
                        discountFixed = BigDecimal.ZERO,
                        taxPercent = BigDecimal("15.00"),
                        grossAmount = BigDecimal("1800.00"),
                        lineDiscountAmount = BigDecimal.ZERO,
                        taxableAmount = BigDecimal("1800.00"),
                        taxAmount = BigDecimal("270.00"),
                        lineTotal = BigDecimal("2070.00")
                    )
                )
            )

            // Quotation 2: Accepted (Vance Health Tech)
            val q2Number = "QT-2026-0002"
            val q2Id = quotationDao.insert(
                QuotationEntity(
                    quotationNumber = q2Number,
                    clientId = c2Id,
                    clientSnapshotName = "Marcus Vance",
                    clientSnapshotCompany = "Vance Health Tech",
                    clientSnapshotContact = "Marcus Vance",
                    clientSnapshotPhone = "+1 (415) 890-3412",
                    clientSnapshotEmail = "m.vance@vancehealth.co",
                    clientSnapshotAddress = "500 Market St, Suite 210, San Francisco, CA",
                    clientSnapshotTaxNumber = "US-EIN-9481029",
                    businessSnapshotName = profile.businessName,
                    businessSnapshotOwner = profile.ownerName,
                    businessSnapshotPhone = profile.phone,
                    businessSnapshotEmail = profile.email,
                    businessSnapshotAddress = profile.address,
                    businessSnapshotWebsite = profile.website,
                    businessSnapshotRegNumber = profile.regNumber,
                    businessSnapshotTaxNumber = profile.taxNumber,
                    businessSnapshotFooter = profile.defaultFooterText,
                    issueDate = System.currentTimeMillis() - (8L * 24 * 3600 * 1000),
                    validUntilDate = System.currentTimeMillis() + (6L * 24 * 3600 * 1000),
                    currency = "USD",
                    subtotal = BigDecimal("3400.00"),
                    lineDiscountsTotal = BigDecimal.ZERO,
                    overallDiscount = BigDecimal.ZERO,
                    taxableAmount = BigDecimal("3400.00"),
                    taxTotal = BigDecimal("510.00"),
                    additionalFee = BigDecimal.ZERO,
                    grandTotal = BigDecimal("3910.00"),
                    paymentTerms = "Net 30. Wire transfer or ACH.",
                    deliveryTerms = "Milestone delivery via GitHub pull requests.",
                    notes = "Approved via formal SOW on Oct 18.",
                    status = QuotationStatus.ACCEPTED,
                    revisionNumber = 1,
                    createdAt = System.currentTimeMillis() - (8L * 24 * 3600 * 1000),
                    modifiedAt = System.currentTimeMillis() - (5L * 24 * 3600 * 1000)
                )
            )

            quotationItemDao.insertAll(
                listOf(
                    QuotationItemEntity(
                        quotationId = q2Id,
                        catalogItemId = cat3Id,
                        name = "Cloud Architecture & DevOps Consulting",
                        description = "Production Kubernetes cluster deployment and automated canary release pipeline.",
                        unitType = "hrs",
                        quantity = BigDecimal("40"),
                        unitPrice = BigDecimal("85.00"),
                        discountPercent = BigDecimal.ZERO,
                        discountFixed = BigDecimal.ZERO,
                        taxPercent = BigDecimal("15.00"),
                        grossAmount = BigDecimal("3400.00"),
                        lineDiscountAmount = BigDecimal.ZERO,
                        taxableAmount = BigDecimal("3400.00"),
                        taxAmount = BigDecimal("510.00"),
                        lineTotal = BigDecimal("3910.00")
                    )
                )
            )

            // Quotation 3: Draft (Lanka Wellness Center)
            val q3Number = "QT-2026-0003"
            val q3Id = quotationDao.insert(
                QuotationEntity(
                    quotationNumber = q3Number,
                    clientId = c3Id,
                    clientSnapshotName = "Dr. Samantha Silva",
                    clientSnapshotCompany = "Lanka Wellness Center",
                    clientSnapshotContact = "Dr. Samantha Silva",
                    clientSnapshotPhone = "+94 11 234 5678",
                    clientSnapshotEmail = "info@lankawellness.lk",
                    clientSnapshotAddress = "12 Kynsey Road, Colombo 07",
                    clientSnapshotTaxNumber = "VAT-8839201",
                    businessSnapshotName = profile.businessName,
                    businessSnapshotOwner = profile.ownerName,
                    businessSnapshotPhone = profile.phone,
                    businessSnapshotEmail = profile.email,
                    businessSnapshotAddress = profile.address,
                    businessSnapshotWebsite = profile.website,
                    businessSnapshotRegNumber = profile.regNumber,
                    businessSnapshotTaxNumber = profile.taxNumber,
                    businessSnapshotFooter = profile.defaultFooterText,
                    issueDate = System.currentTimeMillis(),
                    validUntilDate = System.currentTimeMillis() + (14L * 24 * 3600 * 1000),
                    currency = "USD",
                    subtotal = BigDecimal("4200.00"),
                    lineDiscountsTotal = BigDecimal("420.00"),
                    overallDiscount = BigDecimal.ZERO,
                    taxableAmount = BigDecimal("3780.00"),
                    taxTotal = BigDecimal("567.00"),
                    additionalFee = BigDecimal.ZERO,
                    grandTotal = BigDecimal("4347.00"),
                    paymentTerms = "Monthly billing on the 1st of each calendar month.",
                    deliveryTerms = "Continuous ongoing maintenance.",
                    notes = "10% annual commitment discount applied.",
                    status = QuotationStatus.DRAFT,
                    revisionNumber = 1,
                    createdAt = System.currentTimeMillis(),
                    modifiedAt = System.currentTimeMillis()
                )
            )

            quotationItemDao.insertAll(
                listOf(
                    QuotationItemEntity(
                        quotationId = q3Id,
                        catalogItemId = cat4Id,
                        name = "Annual Maintenance & Support Plan",
                        description = "12 months of uptime monitoring, security updates, and 10 support hours monthly.",
                        unitType = "months",
                        quantity = BigDecimal("12"),
                        unitPrice = BigDecimal("350.00"),
                        discountPercent = BigDecimal("10.00"),
                        discountFixed = BigDecimal.ZERO,
                        taxPercent = BigDecimal("15.00"),
                        grossAmount = BigDecimal("4200.00"),
                        lineDiscountAmount = BigDecimal("420.00"),
                        taxableAmount = BigDecimal("3780.00"),
                        taxAmount = BigDecimal("567.00"),
                        lineTotal = BigDecimal("4347.00")
                    )
                )
            )
        }
    }
}
