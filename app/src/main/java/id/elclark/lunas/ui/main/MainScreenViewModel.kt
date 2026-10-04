package id.elclark.lunas.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.elclark.lunas.data.LunasRepository
import id.elclark.lunas.model.*
import id.elclark.lunas.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface MainUiState {
    data object Loading : MainUiState
    data class Success(
        val overview: MonthlyOverview,
        val services: List<PaylaterService>,
        val items: List<BillItem>,
        val payments: List<PaymentStatus>,
        val settings: AppSettings,
        val isLocked: Boolean
    ) : MainUiState
}

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LunasRepository.getInstance(application.applicationContext)

    private val _selectedYm = MutableStateFlow(DateUtils.currentYearMonth())
    val selectedYm: StateFlow<String> = _selectedYm.asStateFlow()

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val lockStateFlow = combine(repository.settings, _isUnlocked) { s, u -> s to u }
            combine(
                _selectedYm,
                repository.services,
                repository.items,
                repository.payments,
                lockStateFlow
            ) { ym, services, items, payments, lockState ->
                val (settings, unlocked) = lockState
                val overview = repository.computeMonthlyOverview(ym, services, items, payments)
                val isLocked = settings.biometricEnabled && !unlocked
                MainUiState.Success(
                    overview = overview,
                    services = services,
                    items = items,
                    payments = payments,
                    settings = settings,
                    isLocked = isLocked
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            repository.reloadAllNow()
        }
    }

    fun unlock() {
        _isUnlocked.value = true
    }

    fun lock() {
        _isUnlocked.value = false
    }

    fun setYearMonth(ym: String) {
        _selectedYm.value = ym
    }

    fun previousMonth() {
        _selectedYm.value = DateUtils.plusMonths(_selectedYm.value, -1)
    }

    fun nextMonth() {
        _selectedYm.value = DateUtils.plusMonths(_selectedYm.value, 1)
    }

    fun togglePayment(serviceId: String, ym: String = _selectedYm.value) {
        viewModelScope.launch {
            repository.togglePaymentStatus(serviceId, ym)
        }
    }

    fun computeOverview(ym: String): MonthlyOverview {
        return repository.computeMonthlyOverview(
            targetYm = ym,
            allServices = repository.services.value,
            allItems = repository.items.value,
            allPayments = repository.payments.value
        )
    }

    fun computeTotalOwed(): TotalOwedSummary {
        return repository.computeTotalOwed(
            currentYm = DateUtils.currentYearMonth(),
            allServices = repository.services.value,
            allItems = repository.items.value,
            allPayments = repository.payments.value
        )
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            repository.deleteBillItem(itemId)
        }
    }

    fun addCicilan(
        serviceId: String,
        title: String,
        amountPerMonth: Long,
        tenor: Int,
        startMonth: String
    ) {
        viewModelScope.launch {
            repository.addBillItem(
                serviceId = serviceId,
                title = title.ifBlank { "Cicilan ${tenor}x" },
                amountPerMonth = amountPerMonth,
                totalTenor = tenor,
                startYearMonth = startMonth
            )
        }
    }

    fun addScratchpad(
        serviceId: String,
        title: String,
        rawExpression: String,
        computedSum: Long,
        targetMonth: String
    ) {
        viewModelScope.launch {
            repository.addBillItem(
                serviceId = serviceId,
                title = title.ifBlank { "Tagihan $targetMonth" },
                amountPerMonth = computedSum,
                totalTenor = 1,
                startYearMonth = targetMonth,
                rawMathExpression = rawExpression
            )
        }
    }

    fun updateCicilan(
        id: String,
        serviceId: String,
        title: String,
        amountPerMonth: Long,
        tenor: Int,
        startMonth: String
    ) {
        viewModelScope.launch {
            repository.updateBillItem(
                id = id,
                serviceId = serviceId,
                title = title.ifBlank { "Cicilan ${tenor}x" },
                amountPerMonth = amountPerMonth,
                totalTenor = tenor,
                startYearMonth = startMonth,
                rawMathExpression = null
            )
        }
    }

    fun updateScratchpad(
        id: String,
        serviceId: String,
        title: String,
        rawExpression: String,
        computedSum: Long,
        targetMonth: String
    ) {
        viewModelScope.launch {
            repository.updateBillItem(
                id = id,
                serviceId = serviceId,
                title = title.ifBlank { "Tagihan $targetMonth" },
                amountPerMonth = computedSum,
                totalTenor = 1,
                startYearMonth = targetMonth,
                rawMathExpression = rawExpression
            )
        }
    }

    fun saveService(service: PaylaterService) {
        viewModelScope.launch {
            repository.saveService(service)
        }
    }

    fun deleteService(serviceId: String) {
        viewModelScope.launch {
            repository.deleteService(serviceId)
        }
    }

    fun updateSettings(settings: AppSettings) {
        viewModelScope.launch {
            repository.updateSettings(settings)
        }
    }

    suspend fun exportBackup(): String {
        return repository.exportBackupJson()
    }

    suspend fun importBackup(jsonString: String): Boolean {
        return repository.importBackupJson(jsonString)
    }

    suspend fun importJsonData(rawText: String, merge: Boolean = true): Result<Int> {
        return repository.importJsonData(rawText, merge)
    }

    fun getAiExtractionPrompt(): String {
        return id.elclark.lunas.util.AiPromptTemplates.generateExtractionPrompt(repository.services.value)
    }
}
