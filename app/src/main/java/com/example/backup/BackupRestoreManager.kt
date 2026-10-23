package com.example.backup

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.repository.toDomain
import com.example.data.repository.toEntity
import com.example.domain.model.BusinessProfile
import com.example.domain.model.CatalogItem
import com.example.domain.model.Client
import com.example.domain.model.Quotation
import com.example.domain.model.QuotationItem
import com.example.domain.model.RecurringExpense
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.math.BigDecimal

data class BackupMetadata(
    val app: String = "Quotation Pro",
    val schemaVersion: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val clientCount: Int,
    val quotationCount: Int,
    val catalogCount: Int,
    val recurringCount: Int
)

object BackupRestoreManager {
    private const val BACKUP_SCHEMA_VERSION = 1

    suspend fun exportBackup(db: AppDatabase, outputStream: OutputStream): BackupMetadata = withContext(Dispatchers.IO) {
        val clientDao = db.clientDao()
        val catalogDao = db.catalogItemDao()
        val quotationDao = db.quotationDao()
        val quotationItemDao = db.quotationItemDao()
        val recurringDao = db.recurringExpenseDao()
        val profileDao = db.businessProfileDao()

        val clients = clientDao.getAllClientsDirect().map { it.toDomain() }
        val catalog = catalogDao.getAllItemsDirect().map { it.toDomain() }
        val quotations = quotationDao.getAllQuotationsDirect()
        val allItems = quotationItemDao.getAllItemsDirect().map { it.toDomain() }
        val recurring = recurringDao.getAllExpensesDirect().map { it.toDomain() }
        val profile = profileDao.getProfileDirect()?.toDomain() ?: BusinessProfile()

        val root = JSONObject()
        root.put("app", "Quotation Pro")
        root.put("schemaVersion", BACKUP_SCHEMA_VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        // Business Profile
        val profObj = JSONObject().apply {
            put("businessName", profile.businessName)
            put("ownerName", profile.ownerName)
            put("phone", profile.phone)
            put("email", profile.email)
            put("address", profile.address)
            put("website", profile.website)
            put("regNumber", profile.regNumber)
            put("taxNumber", profile.taxNumber)
            put("defaultCurrency", profile.defaultCurrency)
            put("defaultTaxRate", profile.defaultTaxRate.toPlainString())
            put("defaultValidityDays", profile.defaultValidityDays)
            put("quotationPrefix", profile.quotationPrefix)
            put("defaultPaymentTerms", profile.defaultPaymentTerms)
            put("defaultDeliveryTerms", profile.defaultDeliveryTerms)
            put("defaultFooterText", profile.defaultFooterText)
        }
        root.put("businessProfile", profObj)

        // Clients
        val clientsArr = JSONArray()
        for (c in clients) {
            val cObj = JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("companyName", c.companyName)
                put("contactPerson", c.contactPerson)
                put("phone", c.phone)
                put("email", c.email)
                put("billingAddress", c.billingAddress)
                put("taxNumber", c.taxNumber)
                put("notes", c.notes)
                put("createdAt", c.createdAt)
            }
            clientsArr.put(cObj)
        }
        root.put("clients", clientsArr)

        // Catalog
        val catArr = JSONArray()
        for (cat in catalog) {
            val catObj = JSONObject().apply {
                put("id", cat.id)
                put("name", cat.name)
                put("description", cat.description)
                put("sku", cat.sku)
                put("category", cat.category)
                put("unitType", cat.unitType)
                put("defaultUnitPrice", cat.defaultUnitPrice.toPlainString())
                put("defaultQuantity", cat.defaultQuantity.toPlainString())
                put("costPrice", cat.costPrice.toPlainString())
                put("taxRate", cat.taxRate.toPlainString())
                put("isArchived", cat.isArchived)
            }
            catArr.put(catObj)
        }
        root.put("catalogItems", catArr)

        // Recurring Expenses
        val recArr = JSONArray()
        for (r in recurring) {
            val rObj = JSONObject().apply {
                put("id", r.id)
                put("name", r.name)
                put("category", r.category)
                put("amount", r.amount.toPlainString())
                put("currency", r.currency)
                put("frequency", r.frequency.name)
                put("billingStartDate", r.billingStartDate)
                if (r.billingEndDate != null) put("billingEndDate", r.billingEndDate)
                if (r.nextDueDate != null) put("nextDueDate", r.nextDueDate)
                put("isActive", r.isActive)
                put("notes", r.notes)
            }
            recArr.put(rObj)
        }
        root.put("recurringExpenses", recArr)

        // Quotations with items
        val quotesArr = JSONArray()
        for (q in quotations) {
            val qItems = allItems.filter { it.quotationId == q.id }
            val qObj = JSONObject().apply {
                put("id", q.id)
                put("quotationNumber", q.quotationNumber)
                if (q.clientId != null) put("clientId", q.clientId)
                put("clientSnapshotName", q.clientSnapshotName)
                put("clientSnapshotCompany", q.clientSnapshotCompany)
                put("clientSnapshotContact", q.clientSnapshotContact)
                put("clientSnapshotPhone", q.clientSnapshotPhone)
                put("clientSnapshotEmail", q.clientSnapshotEmail)
                put("clientSnapshotAddress", q.clientSnapshotAddress)
                put("clientSnapshotTaxNumber", q.clientSnapshotTaxNumber)
                put("businessSnapshotName", q.businessSnapshotName)
                put("businessSnapshotOwner", q.businessSnapshotOwner)
                put("businessSnapshotPhone", q.businessSnapshotPhone)
                put("businessSnapshotEmail", q.businessSnapshotEmail)
                put("businessSnapshotAddress", q.businessSnapshotAddress)
                put("businessSnapshotWebsite", q.businessSnapshotWebsite)
                put("businessSnapshotRegNumber", q.businessSnapshotRegNumber)
                put("businessSnapshotTaxNumber", q.businessSnapshotTaxNumber)
                put("businessSnapshotFooter", q.businessSnapshotFooter)
                put("issueDate", q.issueDate)
                put("validUntilDate", q.validUntilDate)
                put("currency", q.currency)
                put("subtotal", q.subtotal.toPlainString())
                put("lineDiscountsTotal", q.lineDiscountsTotal.toPlainString())
                put("overallDiscount", q.overallDiscount.toPlainString())
                put("taxableAmount", q.taxableAmount.toPlainString())
                put("taxTotal", q.taxTotal.toPlainString())
                put("additionalFee", q.additionalFee.toPlainString())
                put("grandTotal", q.grandTotal.toPlainString())
                put("paymentTerms", q.paymentTerms)
                put("deliveryTerms", q.deliveryTerms)
                put("notes", q.notes)
                put("status", q.status.name)
                put("revisionNumber", q.revisionNumber)
                put("createdAt", q.createdAt)
                put("modifiedAt", q.modifiedAt)

                val itemsArr = JSONArray()
                for (it in qItems) {
                    val itObj = JSONObject().apply {
                        put("id", it.id)
                        if (it.catalogItemId != null) put("catalogItemId", it.catalogItemId)
                        put("name", it.name)
                        put("description", it.description)
                        put("unitType", it.unitType)
                        put("quantity", it.quantity.toPlainString())
                        put("unitPrice", it.unitPrice.toPlainString())
                        put("discountPercent", it.discountPercent.toPlainString())
                        put("discountFixed", it.discountFixed.toPlainString())
                        put("taxPercent", it.taxPercent.toPlainString())
                        put("grossAmount", it.grossAmount.toPlainString())
                        put("lineDiscountAmount", it.lineDiscountAmount.toPlainString())
                        put("taxableAmount", it.taxableAmount.toPlainString())
                        put("taxAmount", it.taxAmount.toPlainString())
                        put("lineTotal", it.lineTotal.toPlainString())
                    }
                    itemsArr.put(itObj)
                }
                put("items", itemsArr)
            }
            quotesArr.put(qObj)
        }
        root.put("quotations", quotesArr)

        outputStream.bufferedWriter().use { writer ->
            writer.write(root.toString(2))
        }

        BackupMetadata(
            clientCount = clients.size,
            quotationCount = quotations.size,
            catalogCount = catalog.size,
            recurringCount = recurring.size
        )
    }

    suspend fun restoreBackup(db: AppDatabase, inputStream: InputStream): BackupMetadata = withContext(Dispatchers.IO) {
        val jsonText = inputStream.bufferedReader().use { it.readText() }
        val root = JSONObject(jsonText)

        val app = root.optString("app", "")
        val schemaVer = root.optInt("schemaVersion", 0)

        if (!app.contains("Quotation Pro", ignoreCase = true) && !root.has("quotations")) {
            throw IllegalArgumentException("Incompatible or invalid Quotation Pro backup file.")
        }
        if (schemaVer > BACKUP_SCHEMA_VERSION) {
            throw IllegalArgumentException("Backup was created with a newer version of Quotation Pro (v$schemaVer). Please update the app.")
        }

        db.withTransaction {
            val clientDao = db.clientDao()
            val catalogDao = db.catalogItemDao()
            val quotationDao = db.quotationDao()
            val quotationItemDao = db.quotationItemDao()
            val recurringDao = db.recurringExpenseDao()
            val profileDao = db.businessProfileDao()

            // Clear current data
            quotationItemDao.deleteAll()
            quotationDao.deleteAll()
            clientDao.deleteAll()
            catalogDao.deleteAll()
            recurringDao.deleteAll()

            // Restore Profile
            if (root.has("businessProfile")) {
                val p = root.getJSONObject("businessProfile")
                val profile = BusinessProfile(
                    businessName = p.optString("businessName", "ProTech Solutions"),
                    ownerName = p.optString("ownerName", "Alex Morgan"),
                    phone = p.optString("phone", ""),
                    email = p.optString("email", ""),
                    address = p.optString("address", ""),
                    website = p.optString("website", ""),
                    regNumber = p.optString("regNumber", ""),
                    taxNumber = p.optString("taxNumber", ""),
                    defaultCurrency = p.optString("defaultCurrency", "USD"),
                    defaultTaxRate = BigDecimal(p.optString("defaultTaxRate", "8.50")),
                    defaultValidityDays = p.optInt("defaultValidityDays", 14),
                    quotationPrefix = p.optString("quotationPrefix", "QT-2026-"),
                    defaultPaymentTerms = p.optString("defaultPaymentTerms", ""),
                    defaultDeliveryTerms = p.optString("defaultDeliveryTerms", ""),
                    defaultFooterText = p.optString("defaultFooterText", "")
                )
                profileDao.insertOrUpdate(profile.toEntity())
            }

            // Restore Clients
            val cArr = root.optJSONArray("clients") ?: JSONArray()
            val clientsList = mutableListOf<Client>()
            for (i in 0 until cArr.length()) {
                val c = cArr.getJSONObject(i)
                clientsList.add(
                    Client(
                        id = c.optLong("id", 0),
                        name = c.optString("name", "Unnamed Client"),
                        companyName = c.optString("companyName", ""),
                        contactPerson = c.optString("contactPerson", ""),
                        phone = c.optString("phone", ""),
                        email = c.optString("email", ""),
                        billingAddress = c.optString("billingAddress", ""),
                        taxNumber = c.optString("taxNumber", ""),
                        notes = c.optString("notes", ""),
                        createdAt = c.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            if (clientsList.isNotEmpty()) {
                clientDao.insertAll(clientsList.map { it.toEntity() })
            }

            // Restore Catalog Items
            val catArr = root.optJSONArray("catalogItems") ?: JSONArray()
            val catalogList = mutableListOf<CatalogItem>()
            for (i in 0 until catArr.length()) {
                val cat = catArr.getJSONObject(i)
                catalogList.add(
                    CatalogItem(
                        id = cat.optLong("id", 0),
                        name = cat.optString("name", "Unnamed Item"),
                        description = cat.optString("description", ""),
                        sku = cat.optString("sku", ""),
                        category = cat.optString("category", "General"),
                        unitType = cat.optString("unitType", "units"),
                        defaultUnitPrice = BigDecimal(cat.optString("defaultUnitPrice", "0.00")),
                        defaultQuantity = BigDecimal(cat.optString("defaultQuantity", "1")),
                        costPrice = BigDecimal(cat.optString("costPrice", "0.00")),
                        taxRate = BigDecimal(cat.optString("taxRate", "0.00")),
                        isArchived = cat.optBoolean("isArchived", false)
                    )
                )
            }
            if (catalogList.isNotEmpty()) {
                catalogDao.insertAll(catalogList.map { it.toEntity() })
            }

            // Restore Recurring Expenses
            val recArr = root.optJSONArray("recurringExpenses") ?: JSONArray()
            val recurringList = mutableListOf<RecurringExpense>()
            for (i in 0 until recArr.length()) {
                val r = recArr.getJSONObject(i)
                recurringList.add(
                    RecurringExpense(
                        id = r.optLong("id", 0),
                        name = r.optString("name", "Expense"),
                        category = r.optString("category", "Software"),
                        amount = BigDecimal(r.optString("amount", "0.00")),
                        currency = r.optString("currency", "USD"),
                        frequency = com.example.domain.model.ExpenseFrequency.fromString(r.optString("frequency", "MONTHLY")),
                        billingStartDate = r.optLong("billingStartDate", System.currentTimeMillis()),
                        billingEndDate = if (r.has("billingEndDate")) r.optLong("billingEndDate") else null,
                        nextDueDate = if (r.has("nextDueDate")) r.optLong("nextDueDate") else null,
                        isActive = r.optBoolean("isActive", true),
                        notes = r.optString("notes", "")
                    )
                )
            }
            if (recurringList.isNotEmpty()) {
                recurringDao.insertAll(recurringList.map { it.toEntity() })
            }

            // Restore Quotations and Items
            val quotesArr = root.optJSONArray("quotations") ?: JSONArray()
            for (i in 0 until quotesArr.length()) {
                val q = quotesArr.getJSONObject(i)
                val qId = q.optLong("id", 0)
                val quote = Quotation(
                    id = qId,
                    quotationNumber = q.optString("quotationNumber", "QT-RESTORE-$i"),
                    clientId = if (q.has("clientId")) q.optLong("clientId") else null,
                    clientSnapshotName = q.optString("clientSnapshotName", ""),
                    clientSnapshotCompany = q.optString("clientSnapshotCompany", ""),
                    clientSnapshotContact = q.optString("clientSnapshotContact", ""),
                    clientSnapshotPhone = q.optString("clientSnapshotPhone", ""),
                    clientSnapshotEmail = q.optString("clientSnapshotEmail", ""),
                    clientSnapshotAddress = q.optString("clientSnapshotAddress", ""),
                    clientSnapshotTaxNumber = q.optString("clientSnapshotTaxNumber", ""),
                    businessSnapshotName = q.optString("businessSnapshotName", ""),
                    businessSnapshotOwner = q.optString("businessSnapshotOwner", ""),
                    businessSnapshotPhone = q.optString("businessSnapshotPhone", ""),
                    businessSnapshotEmail = q.optString("businessSnapshotEmail", ""),
                    businessSnapshotAddress = q.optString("businessSnapshotAddress", ""),
                    businessSnapshotWebsite = q.optString("businessSnapshotWebsite", ""),
                    businessSnapshotRegNumber = q.optString("businessSnapshotRegNumber", ""),
                    businessSnapshotTaxNumber = q.optString("businessSnapshotTaxNumber", ""),
                    businessSnapshotFooter = q.optString("businessSnapshotFooter", ""),
                    issueDate = q.optLong("issueDate", System.currentTimeMillis()),
                    validUntilDate = q.optLong("validUntilDate", System.currentTimeMillis()),
                    currency = q.optString("currency", "USD"),
                    subtotal = BigDecimal(q.optString("subtotal", "0.00")),
                    lineDiscountsTotal = BigDecimal(q.optString("lineDiscountsTotal", "0.00")),
                    overallDiscount = BigDecimal(q.optString("overallDiscount", "0.00")),
                    taxableAmount = BigDecimal(q.optString("taxableAmount", "0.00")),
                    taxTotal = BigDecimal(q.optString("taxTotal", "0.00")),
                    additionalFee = BigDecimal(q.optString("additionalFee", "0.00")),
                    grandTotal = BigDecimal(q.optString("grandTotal", "0.00")),
                    paymentTerms = q.optString("paymentTerms", ""),
                    deliveryTerms = q.optString("deliveryTerms", ""),
                    notes = q.optString("notes", ""),
                    status = com.example.domain.model.QuotationStatus.fromString(q.optString("status", "DRAFT")),
                    revisionNumber = q.optInt("revisionNumber", 1),
                    createdAt = q.optLong("createdAt", System.currentTimeMillis()),
                    modifiedAt = q.optLong("modifiedAt", System.currentTimeMillis())
                )
                val newQId = quotationDao.insert(quote.toEntity())

                val itArr = q.optJSONArray("items") ?: JSONArray()
                val itemsList = mutableListOf<QuotationItem>()
                for (j in 0 until itArr.length()) {
                    val it = itArr.getJSONObject(j)
                    itemsList.add(
                        QuotationItem(
                            id = 0,
                            quotationId = newQId,
                            catalogItemId = if (it.has("catalogItemId")) it.optLong("catalogItemId") else null,
                            name = it.optString("name", "Item"),
                            description = it.optString("description", ""),
                            unitType = it.optString("unitType", "units"),
                            quantity = BigDecimal(it.optString("quantity", "1")),
                            unitPrice = BigDecimal(it.optString("unitPrice", "0.00")),
                            discountPercent = BigDecimal(it.optString("discountPercent", "0.00")),
                            discountFixed = BigDecimal(it.optString("discountFixed", "0.00")),
                            taxPercent = BigDecimal(it.optString("taxPercent", "0.00")),
                            grossAmount = BigDecimal(it.optString("grossAmount", "0.00")),
                            lineDiscountAmount = BigDecimal(it.optString("lineDiscountAmount", "0.00")),
                            taxableAmount = BigDecimal(it.optString("taxableAmount", "0.00")),
                            taxAmount = BigDecimal(it.optString("taxAmount", "0.00")),
                            lineTotal = BigDecimal(it.optString("lineTotal", "0.00"))
                        )
                    )
                }
                if (itemsList.isNotEmpty()) {
                    quotationItemDao.insertAll(itemsList.map { it.toEntity(newQId) })
                }
            }

            BackupMetadata(
                clientCount = clientsList.size,
                quotationCount = quotesArr.length(),
                catalogCount = catalogList.size,
                recurringCount = recurringList.size
            )
        }
    }
}
