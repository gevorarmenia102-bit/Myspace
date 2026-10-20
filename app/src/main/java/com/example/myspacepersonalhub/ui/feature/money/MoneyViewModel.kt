package com.example.myspacepersonalhub.ui.feature.money

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myspacepersonalhub.core.database.entity.BudgetEntity
import com.example.myspacepersonalhub.core.database.entity.TransactionEntity
import com.example.myspacepersonalhub.core.model.TransactionType
import com.example.myspacepersonalhub.core.util.CurrencyBalance
import com.example.myspacepersonalhub.core.util.CurrencyUtils
import com.example.myspacepersonalhub.data.repository.MoneyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class MoneyUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val filteredTransactions: List<TransactionEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val currencyBalances: Map<String, CurrencyBalance> = emptyMap(),
    val selectedCurrency: String = "USD",
    val selectedType: TransactionType? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false
)

class MoneyViewModel(private val moneyRepository: MoneyRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(MoneyUiState())
    val uiState: StateFlow<MoneyUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            combine(
                moneyRepository.getAllTransactions(),
                moneyRepository.getAllBudgets()
            ) { txs, budgets ->
                Pair(txs, budgets)
            }.collect { (txs, budgets) ->
                val balances = CurrencyUtils.calculateTotalsByCurrency(txs)
                val currencies = balances.keys.toList()
                val currentCurr = if (currencies.contains(_uiState.value.selectedCurrency)) {
                    _uiState.value.selectedCurrency
                } else {
                    currencies.firstOrNull() ?: "USD"
                }

                _uiState.update { state ->
                    val filtered = applyFilter(txs, currentCurr, state.selectedType, state.searchQuery)
                    state.copy(
                        transactions = txs,
                        filteredTransactions = filtered,
                        budgets = budgets,
                        currencyBalances = balances,
                        selectedCurrency = currentCurr,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun selectCurrency(currencyCode: String) {
        _uiState.update { state ->
            val filtered = applyFilter(state.transactions, currencyCode, state.selectedType, state.searchQuery)
            state.copy(selectedCurrency = currencyCode, filteredTransactions = filtered)
        }
    }

    fun selectType(type: TransactionType?) {
        _uiState.update { state ->
            val newType = if (state.selectedType == type) null else type
            val filtered = applyFilter(state.transactions, state.selectedCurrency, newType, state.searchQuery)
            state.copy(selectedType = newType, filteredTransactions = filtered)
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { state ->
            val filtered = applyFilter(state.transactions, state.selectedCurrency, state.selectedType, query)
            state.copy(searchQuery = query, filteredTransactions = filtered)
        }
    }

    fun saveTransaction(
        transaction: TransactionEntity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = moneyRepository.saveTransaction(transaction)
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "Save failed")
            }
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            moneyRepository.deleteTransaction(id)
        }
    }

    fun saveBudget(
        budget: BudgetEntity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = moneyRepository.saveBudget(budget)
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "Save failed")
            }
        }
    }

    fun deleteBudget(id: String) {
        viewModelScope.launch {
            moneyRepository.deleteBudget(id)
        }
    }

    fun exportToCsv(): String {
        return moneyRepository.exportToCsv(_uiState.value.transactions)
    }

    private fun applyFilter(
        txs: List<TransactionEntity>,
        currency: String,
        type: TransactionType?,
        query: String
    ): List<TransactionEntity> {
        var result = txs.filter { it.currencyCode.equals(currency, ignoreCase = true) }

        if (type != null) {
            result = result.filter { it.type == type }
        }

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            result = result.filter {
                it.title.lowercase().contains(q) ||
                it.categoryId.lowercase().contains(q) ||
                (it.notes?.lowercase()?.contains(q) == true)
            }
        }

        return result
    }
}
