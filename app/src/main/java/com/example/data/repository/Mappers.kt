package com.example.data.repository

import com.example.data.local.BusinessProfileEntity
import com.example.data.local.CatalogItemEntity
import com.example.data.local.ClientEntity
import com.example.data.local.QuotationEntity
import com.example.data.local.QuotationItemEntity
import com.example.data.local.RecurringExpenseEntity
import com.example.domain.model.BusinessProfile
import com.example.domain.model.CatalogItem
import com.example.domain.model.Client
import com.example.domain.model.Quotation
import com.example.domain.model.QuotationItem
import com.example.domain.model.RecurringExpense

fun ClientEntity.toDomain() = Client(
    id = id,
    name = name,
    companyName = companyName,
    contactPerson = contactPerson,
    phone = phone,
    email = email,
    billingAddress = billingAddress,
    taxNumber = taxNumber,
    notes = notes,
    createdAt = createdAt
)

fun Client.toEntity() = ClientEntity(
    id = id,
    name = name,
    companyName = companyName,
    contactPerson = contactPerson,
    phone = phone,
    email = email,
    billingAddress = billingAddress,
    taxNumber = taxNumber,
    notes = notes,
    createdAt = createdAt
)

fun CatalogItemEntity.toDomain() = CatalogItem(
    id = id,
    name = name,
    description = description,
    sku = sku,
    category = category,
    unitType = unitType,
    defaultUnitPrice = defaultUnitPrice,
    defaultQuantity = defaultQuantity,
    costPrice = costPrice,
    taxRate = taxRate,
    isArchived = isArchived
)

fun CatalogItem.toEntity() = CatalogItemEntity(
    id = id,
    name = name,
    description = description,
    sku = sku,
    category = category,
    unitType = unitType,
    defaultUnitPrice = defaultUnitPrice,
    defaultQuantity = defaultQuantity,
    costPrice = costPrice,
    taxRate = taxRate,
    isArchived = isArchived
)

fun QuotationItemEntity.toDomain() = QuotationItem(
    id = id,
    quotationId = quotationId,
    catalogItemId = catalogItemId,
    name = name,
    description = description,
    unitType = unitType,
    quantity = quantity,
    unitPrice = unitPrice,
    discountPercent = discountPercent,
    discountFixed = discountFixed,
    taxPercent = taxPercent,
    grossAmount = grossAmount,
    lineDiscountAmount = lineDiscountAmount,
    taxableAmount = taxableAmount,
    taxAmount = taxAmount,
    lineTotal = lineTotal
)

fun QuotationItem.toEntity(quotationId: Long) = QuotationItemEntity(
    id = id,
    quotationId = quotationId,
    catalogItemId = catalogItemId,
    name = name,
    description = description,
    unitType = unitType,
    quantity = quantity,
    unitPrice = unitPrice,
    discountPercent = discountPercent,
    discountFixed = discountFixed,
    taxPercent = taxPercent,
    grossAmount = grossAmount,
    lineDiscountAmount = lineDiscountAmount,
    taxableAmount = taxableAmount,
    taxAmount = taxAmount,
    lineTotal = lineTotal
)

fun QuotationEntity.toDomain(items: List<QuotationItem> = emptyList()) = Quotation(
    id = id,
    quotationNumber = quotationNumber,
    clientId = clientId,
    clientSnapshotName = clientSnapshotName,
    clientSnapshotCompany = clientSnapshotCompany,
    clientSnapshotContact = clientSnapshotContact,
    clientSnapshotPhone = clientSnapshotPhone,
    clientSnapshotEmail = clientSnapshotEmail,
    clientSnapshotAddress = clientSnapshotAddress,
    clientSnapshotTaxNumber = clientSnapshotTaxNumber,
    businessSnapshotName = businessSnapshotName,
    businessSnapshotOwner = businessSnapshotOwner,
    businessSnapshotPhone = businessSnapshotPhone,
    businessSnapshotEmail = businessSnapshotEmail,
    businessSnapshotAddress = businessSnapshotAddress,
    businessSnapshotWebsite = businessSnapshotWebsite,
    businessSnapshotRegNumber = businessSnapshotRegNumber,
    businessSnapshotTaxNumber = businessSnapshotTaxNumber,
    businessSnapshotFooter = businessSnapshotFooter,
    issueDate = issueDate,
    validUntilDate = validUntilDate,
    currency = currency,
    subtotal = subtotal,
    lineDiscountsTotal = lineDiscountsTotal,
    overallDiscount = overallDiscount,
    taxableAmount = taxableAmount,
    taxTotal = taxTotal,
    additionalFee = additionalFee,
    grandTotal = grandTotal,
    paymentTerms = paymentTerms,
    deliveryTerms = deliveryTerms,
    notes = notes,
    status = status,
    revisionNumber = revisionNumber,
    createdAt = createdAt,
    modifiedAt = modifiedAt,
    items = items
)

fun Quotation.toEntity() = QuotationEntity(
    id = id,
    quotationNumber = quotationNumber,
    clientId = clientId,
    clientSnapshotName = clientSnapshotName,
    clientSnapshotCompany = clientSnapshotCompany,
    clientSnapshotContact = clientSnapshotContact,
    clientSnapshotPhone = clientSnapshotPhone,
    clientSnapshotEmail = clientSnapshotEmail,
    clientSnapshotAddress = clientSnapshotAddress,
    clientSnapshotTaxNumber = clientSnapshotTaxNumber,
    businessSnapshotName = businessSnapshotName,
    businessSnapshotOwner = businessSnapshotOwner,
    businessSnapshotPhone = businessSnapshotPhone,
    businessSnapshotEmail = businessSnapshotEmail,
    businessSnapshotAddress = businessSnapshotAddress,
    businessSnapshotWebsite = businessSnapshotWebsite,
    businessSnapshotRegNumber = businessSnapshotRegNumber,
    businessSnapshotTaxNumber = businessSnapshotTaxNumber,
    businessSnapshotFooter = businessSnapshotFooter,
    issueDate = issueDate,
    validUntilDate = validUntilDate,
    currency = currency,
    subtotal = subtotal,
    lineDiscountsTotal = lineDiscountsTotal,
    overallDiscount = overallDiscount,
    taxableAmount = taxableAmount,
    taxTotal = taxTotal,
    additionalFee = additionalFee,
    grandTotal = grandTotal,
    paymentTerms = paymentTerms,
    deliveryTerms = deliveryTerms,
    notes = notes,
    status = status,
    revisionNumber = revisionNumber,
    createdAt = createdAt,
    modifiedAt = modifiedAt
)

fun RecurringExpenseEntity.toDomain() = RecurringExpense(
    id = id,
    name = name,
    category = category,
    amount = amount,
    currency = currency,
    frequency = frequency,
    billingStartDate = billingStartDate,
    billingEndDate = billingEndDate,
    nextDueDate = nextDueDate,
    isActive = isActive,
    notes = notes
)

fun RecurringExpense.toEntity() = RecurringExpenseEntity(
    id = id,
    name = name,
    category = category,
    amount = amount,
    currency = currency,
    frequency = frequency,
    billingStartDate = billingStartDate,
    billingEndDate = billingEndDate,
    nextDueDate = nextDueDate,
    isActive = isActive,
    notes = notes
)

fun BusinessProfileEntity.toDomain() = BusinessProfile(
    id = id,
    businessName = businessName,
    ownerName = ownerName,
    phone = phone,
    email = email,
    address = address,
    website = website,
    regNumber = regNumber,
    taxNumber = taxNumber,
    defaultCurrency = defaultCurrency,
    defaultTaxRate = defaultTaxRate,
    defaultValidityDays = defaultValidityDays,
    quotationPrefix = quotationPrefix,
    defaultPaymentTerms = defaultPaymentTerms,
    defaultDeliveryTerms = defaultDeliveryTerms,
    defaultFooterText = defaultFooterText
)

fun BusinessProfile.toEntity() = BusinessProfileEntity(
    id = id,
    businessName = businessName,
    ownerName = ownerName,
    phone = phone,
    email = email,
    address = address,
    website = website,
    regNumber = regNumber,
    taxNumber = taxNumber,
    defaultCurrency = defaultCurrency,
    defaultTaxRate = defaultTaxRate,
    defaultValidityDays = defaultValidityDays,
    quotationPrefix = quotationPrefix,
    defaultPaymentTerms = defaultPaymentTerms,
    defaultDeliveryTerms = defaultDeliveryTerms,
    defaultFooterText = defaultFooterText
)
