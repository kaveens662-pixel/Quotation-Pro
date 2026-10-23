package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.backup.BackupMetadata
import com.example.backup.BackupRestoreManager
import com.example.data.local.AppDatabase
import com.example.data.preferences.AppLanguage
import com.example.data.preferences.AppThemeMode
import com.example.data.preferences.PreferencesManager
import com.example.data.preferences.UserPreferences
import com.example.data.repository.QuotationRepository
import com.example.domain.calculator.FinancialCalculator
import com.example.domain.calculator.PricingCalculatorEngine
import com.example.domain.calculator.PricingInput
import com.example.domain.calculator.PricingResult
import com.example.domain.calculator.RecurringCostCalculator
import com.example.domain.calculator.RecurringCostSummary
import com.example.domain.model.AppCurrency
import com.example.domain.model.BusinessProfile
import com.example.domain.model.CatalogItem
import com.example.domain.model.Client
import com.example.domain.model.ExpenseFrequency
import com.example.domain.model.PricingStrategy
import com.example.domain.model.Quotation
import com.example.domain.model.QuotationItem
import com.example.domain.model.QuotationStatus
import com.example.domain.model.RecurringExpense
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.math.BigDecimal
import java.math.RoundingMode

data class DashboardUiState(
    val totalQuotationsCount: Int = 0,
    val draftCount: Int = 0,
    val sentCount: Int = 0,
    val acceptedCount: Int = 0,
    val rejectedCount: Int = 0,
    val expiredCount: Int = 0,
    val totalQuotedValue: BigDecimal = BigDecimal.ZERO,
    val acceptedRevenueValue: BigDecimal = BigDecimal.ZERO,
    val currencyCode: String = "USD",
    val acceptanceRate: Double = 0.0,
    val recentQuotations: List<Quotation> = emptyList(),
    val upcomingExpiries: List<Quotation> = emptyList(),
    val isLoading: Boolean = false
)

enum class QuotationSortOption(val displayName: String) {
    NEWEST("Newest First"),
    OLDEST("Oldest First"),
    AMOUNT_HIGH("Highest Amount"),
    AMOUNT_LOW("Lowest Amount"),
    EXPIRING_SOON("Expiring Soon")
}

data class QuotationDraftState(
    val id: Long = 0,
    val quotationNumber: String = "",
    val selectedClient: Client? = null,
    val items: List<QuotationItem> = emptyList(),
    val issueDate: Long = System.currentTimeMillis(),
    val validUntilDate: Long = System.currentTimeMillis() + (14L * 24 * 3600 * 1000),
    val currency: String = "USD",
    val overallDiscount: BigDecimal = BigDecimal.ZERO,
    val additionalFee: BigDecimal = BigDecimal.ZERO,
    val paymentTerms: String = "",
    val deliveryTerms: String = "",
    val notes: String = "",
    val status: QuotationStatus = QuotationStatus.DRAFT,
    val currentStep: Int = 0 // 0: Client, 1: Items, 2: Terms, 3: Review
)

class QuotationViewModel(application: Application) : AndroidViewModel(application) {
    val database = AppDatabase.getDatabase(application)
    val repository = QuotationRepository(database)
    val preferencesManager = PreferencesManager(application)

    val userPreferences: StateFlow<UserPreferences> = preferencesManager.userPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    val businessProfile: StateFlow<BusinessProfile> = repository.businessProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BusinessProfile())

    val allQuotations: StateFlow<List<Quotation>> = repository.allQuotations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allClients: StateFlow<List<Client>> = repository.allClients
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCatalogItems: StateFlow<List<CatalogItem>> = repository.allCatalogItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeCatalogItems: StateFlow<List<CatalogItem>> = repository.activeCatalogItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRecurringExpenses: StateFlow<List<RecurringExpense>> = repository.allRecurringExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Search & Filter for Quotations ---
    private val _quotationSearchQuery = MutableStateFlow("")
    val quotationSearchQuery = _quotationSearchQuery.asStateFlow()

    private val _quotationStatusFilter = MutableStateFlow<QuotationStatus?>(null)
    val quotationStatusFilter = _quotationStatusFilter.asStateFlow()

    private val _quotationSortOption = MutableStateFlow(QuotationSortOption.NEWEST)
    val quotationSortOption = _quotationSortOption.asStateFlow()

    val filteredQuotations = combine(
        allQuotations,
        _quotationSearchQuery,
        _quotationStatusFilter,
        _quotationSortOption
    ) { list, query, filter, sort ->
        var res = list
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            res = res.filter {
                it.quotationNumber.lowercase().contains(q) ||
                it.clientSnapshotName.lowercase().contains(q) ||
                it.clientSnapshotCompany.lowercase().contains(q)
            }
        }
        if (filter != null) {
            res = res.filter { it.effectiveStatus == filter }
        }
        when (sort) {
            QuotationSortOption.NEWEST -> res.sortedByDescending { it.issueDate }
            QuotationSortOption.OLDEST -> res.sortedBy { it.issueDate }
            QuotationSortOption.AMOUNT_HIGH -> res.sortedByDescending { it.grandTotal }
            QuotationSortOption.AMOUNT_LOW -> res.sortedBy { it.grandTotal }
            QuotationSortOption.EXPIRING_SOON -> res.sortedBy { it.validUntilDate }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Dashboard UI State ---
    val dashboardUiState: StateFlow<DashboardUiState> = combine(
        allQuotations,
        businessProfile
    ) { quotes, profile ->
        var totalQuoted = BigDecimal.ZERO
        var acceptedRevenue = BigDecimal.ZERO
        var draft = 0
        var sent = 0
        var accepted = 0
        var rejected = 0
        var expired = 0

        val now = System.currentTimeMillis()
        for (q in quotes) {
            totalQuoted = totalQuoted.add(q.grandTotal)
            when (q.effectiveStatus) {
                QuotationStatus.ACCEPTED -> {
                    accepted++
                    acceptedRevenue = acceptedRevenue.add(q.grandTotal)
                }
                QuotationStatus.DRAFT -> draft++
                QuotationStatus.SENT -> sent++
                QuotationStatus.REJECTED -> rejected++
                QuotationStatus.EXPIRED -> expired++
                QuotationStatus.CANCELLED -> {}
            }
        }

        val decidedCount = accepted + rejected
        val winRate = if (decidedCount > 0) (accepted.toDouble() / decidedCount.toDouble()) * 100.0 else 0.0

        val recent = quotes.sortedByDescending { it.createdAt }.take(5)
        val upcoming = quotes.filter { 
            it.effectiveStatus == QuotationStatus.SENT && it.validUntilDate >= now 
        }.sortedBy { it.validUntilDate }.take(4)

        DashboardUiState(
            totalQuotationsCount = quotes.size,
            draftCount = draft,
            sentCount = sent,
            acceptedCount = accepted,
            rejectedCount = rejected,
            expiredCount = expired,
            totalQuotedValue = totalQuoted,
            acceptedRevenueValue = acceptedRevenue,
            currencyCode = profile.defaultCurrency,
            acceptanceRate = winRate,
            recentQuotations = recent,
            upcomingExpiries = upcoming
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    // --- Recurring Cost Summary ---
    val recurringCostSummary: StateFlow<RecurringCostSummary> = combine(
        allRecurringExpenses,
        businessProfile
    ) { expenses, profile ->
        RecurringCostCalculator.summarize(expenses, profile.defaultCurrency)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RecurringCostSummary(0, BigDecimal.ZERO, BigDecimal.ZERO, emptyList(), "USD"))

    // --- Pricing Calculator State ---
    private val _pricingInput = MutableStateFlow(
        PricingInput(
            materialsCost = BigDecimal("500.00"),
            laborHours = BigDecimal("24.0"),
            hourlyRate = BigDecimal("65.00"),
            outsourcingCost = BigDecimal("200.00"),
            deliveryCost = BigDecimal("50.00"),
            softwareOverhead = BigDecimal("60.00"),
            otherOverhead = BigDecimal("40.00"),
            contingencyPercent = BigDecimal("10.0"),
            strategy = PricingStrategy.PROFIT_MARGIN,
            targetPercent = BigDecimal("25.0"),
            taxPercent = BigDecimal("15.0")
        )
    )
    val pricingInput = _pricingInput.asStateFlow()

    private val _pricingResult = MutableStateFlow(PricingCalculatorEngine.calculate(_pricingInput.value))
    val pricingResult = _pricingResult.asStateFlow()

    fun updatePricingInput(newInput: PricingInput) {
        _pricingInput.value = newInput
        _pricingResult.value = PricingCalculatorEngine.calculate(newInput)
    }

    // --- Active Quotation Draft for Creation/Editing ---
    private val _draftState = MutableStateFlow<QuotationDraftState?>(null)
    val draftState = _draftState.asStateFlow()

    fun startNewQuotation() {
        viewModelScope.launch {
            val profile = businessProfile.value
            val nextNum = repository.generateNextQuotationNumber(profile.quotationPrefix)
            val issue = System.currentTimeMillis()
            val valid = issue + (profile.defaultValidityDays.toLong() * 24 * 3600 * 1000)

            _draftState.value = QuotationDraftState(
                id = 0,
                quotationNumber = nextNum,
                currency = profile.defaultCurrency,
                issueDate = issue,
                validUntilDate = valid,
                paymentTerms = profile.defaultPaymentTerms,
                deliveryTerms = profile.defaultDeliveryTerms,
                notes = profile.defaultFooterText,
                currentStep = 0
            )
        }
    }

    fun startEditQuotation(quotation: Quotation) {
        viewModelScope.launch {
            val client = allClients.value.find { it.id == quotation.clientId } ?: Client(
                name = quotation.clientSnapshotName,
                companyName = quotation.clientSnapshotCompany,
                contactPerson = quotation.clientSnapshotContact,
                phone = quotation.clientSnapshotPhone,
                email = quotation.clientSnapshotEmail,
                billingAddress = quotation.clientSnapshotAddress,
                taxNumber = quotation.clientSnapshotTaxNumber
            )

            _draftState.value = QuotationDraftState(
                id = quotation.id,
                quotationNumber = quotation.quotationNumber,
                selectedClient = client,
                items = quotation.items,
                issueDate = quotation.issueDate,
                validUntilDate = quotation.validUntilDate,
                currency = quotation.currency,
                overallDiscount = quotation.overallDiscount,
                additionalFee = quotation.additionalFee,
                paymentTerms = quotation.paymentTerms,
                deliveryTerms = quotation.deliveryTerms,
                notes = quotation.notes,
                status = quotation.status,
                currentStep = 0
            )
        }
    }

    fun setDraftStep(step: Int) {
        _draftState.value = _draftState.value?.copy(currentStep = step)
    }

    fun selectClientForDraft(client: Client) {
        _draftState.value = _draftState.value?.copy(selectedClient = client)
    }

    fun addLineItemToDraft(item: QuotationItem) {
        val current = _draftState.value ?: return
        val updated = current.items + item
        _draftState.value = current.copy(items = updated)
    }

    fun updateLineItemInDraft(index: Int, item: QuotationItem) {
        val current = _draftState.value ?: return
        if (index in current.items.indices) {
            val list = current.items.toMutableList()
            list[index] = item
            _draftState.value = current.copy(items = list)
        }
    }

    fun removeLineItemFromDraft(index: Int) {
        val current = _draftState.value ?: return
        if (index in current.items.indices) {
            val list = current.items.toMutableList()
            list.removeAt(index)
            _draftState.value = current.copy(items = list)
        }
    }

    fun updateDraftTerms(
        issueDate: Long,
        validUntilDate: Long,
        currency: String,
        overallDiscount: BigDecimal,
        additionalFee: BigDecimal,
        paymentTerms: String,
        deliveryTerms: String,
        notes: String
    ) {
        _draftState.value = _draftState.value?.copy(
            issueDate = issueDate,
            validUntilDate = validUntilDate,
            currency = currency,
            overallDiscount = overallDiscount,
            additionalFee = additionalFee,
            paymentTerms = paymentTerms,
            deliveryTerms = deliveryTerms,
            notes = notes
        )
    }

    fun saveDraftQuotation(asStatus: QuotationStatus = QuotationStatus.DRAFT, onSaved: (Long) -> Unit) {
        val draft = _draftState.value ?: return
        viewModelScope.launch {
            val profile = businessProfile.value
            val client = draft.selectedClient

            val quotation = Quotation(
                id = draft.id,
                quotationNumber = draft.quotationNumber,
                clientId = client?.id,
                clientSnapshotName = client?.name ?: "Valued Client",
                clientSnapshotCompany = client?.companyName ?: "",
                clientSnapshotContact = client?.contactPerson ?: "",
                clientSnapshotPhone = client?.phone ?: "",
                clientSnapshotEmail = client?.email ?: "",
                clientSnapshotAddress = client?.billingAddress ?: "",
                clientSnapshotTaxNumber = client?.taxNumber ?: "",
                businessSnapshotName = profile.businessName,
                businessSnapshotOwner = profile.ownerName,
                businessSnapshotPhone = profile.phone,
                businessSnapshotEmail = profile.email,
                businessSnapshotAddress = profile.address,
                businessSnapshotWebsite = profile.website,
                businessSnapshotRegNumber = profile.regNumber,
                businessSnapshotTaxNumber = profile.taxNumber,
                businessSnapshotFooter = profile.defaultFooterText,
                issueDate = draft.issueDate,
                validUntilDate = draft.validUntilDate,
                currency = draft.currency,
                overallDiscount = draft.overallDiscount,
                additionalFee = draft.additionalFee,
                paymentTerms = draft.paymentTerms,
                deliveryTerms = draft.deliveryTerms,
                notes = draft.notes,
                status = asStatus
            )

            val savedId = repository.saveQuotation(quotation, draft.items)
            _draftState.value = null
            onSaved(savedId)
        }
    }

    fun cancelDraft() {
        _draftState.value = null
    }

    // --- Search & Filter Actions ---
    fun setQuotationSearch(query: String) {
        _quotationSearchQuery.value = query
    }

    fun setQuotationStatusFilter(status: QuotationStatus?) {
        _quotationStatusFilter.value = status
    }

    fun setQuotationSort(sort: QuotationSortOption) {
        _quotationSortOption.value = sort
    }

    // --- Quotation Actions ---
    fun updateQuotationStatus(id: Long, status: QuotationStatus) {
        viewModelScope.launch {
            repository.updateQuotationStatus(id, status)
        }
    }

    fun duplicateQuotation(id: Long, onDuplicated: (Long) -> Unit) {
        viewModelScope.launch {
            val newId = repository.duplicateQuotation(id)
            if (newId > 0) onDuplicated(newId)
        }
    }

    fun deleteQuotation(id: Long) {
        viewModelScope.launch {
            repository.deleteQuotation(id)
        }
    }

    // --- Client Operations ---
    fun saveClient(client: Client, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.saveClient(client)
            onDone?.invoke()
        }
    }

    fun deleteClient(id: Long, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.deleteClient(id)
            onDone?.invoke()
        }
    }

    // --- Catalog Operations ---
    fun saveCatalogItem(item: CatalogItem, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.saveCatalogItem(item)
            onDone?.invoke()
        }
    }

    fun deleteCatalogItem(item: CatalogItem, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.deleteCatalogItem(item)
            onDone?.invoke()
        }
    }

    // --- Recurring Expense Operations ---
    fun saveRecurringExpense(expense: RecurringExpense, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.saveRecurringExpense(expense)
            onDone?.invoke()
        }
    }

    fun deleteRecurringExpense(id: Long, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.deleteRecurringExpense(id)
            onDone?.invoke()
        }
    }

    // --- Business Profile Operations ---
    fun saveBusinessProfile(profile: BusinessProfile, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.saveBusinessProfile(profile)
            onDone?.invoke()
        }
    }

    // --- Preferences Operations ---
    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            preferencesManager.setThemeMode(mode)
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            preferencesManager.setLanguage(language)
        }
    }

    // --- Seed Demo Data ---
    fun seedDemoData(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.seedDemoData()
            onComplete()
        }
    }

    // --- Backup & Restore ---
    fun exportBackup(outputStream: OutputStream, onResult: (Result<BackupMetadata>) -> Unit) {
        viewModelScope.launch {
            try {
                val meta = BackupRestoreManager.exportBackup(database, outputStream)
                onResult(Result.success(meta))
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    fun restoreBackup(inputStream: InputStream, onResult: (Result<BackupMetadata>) -> Unit) {
        viewModelScope.launch {
            try {
                val meta = BackupRestoreManager.restoreBackup(database, inputStream)
                onResult(Result.success(meta))
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }
}
