package com.example.myspacepersonalhub.ui.feature.quickaccess

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myspacepersonalhub.core.database.entity.QuickAccessEntity
import com.example.myspacepersonalhub.core.model.QuickAccessType
import com.example.myspacepersonalhub.data.repository.QuickAccessRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class QuickAccessUiState(
    val items: List<QuickAccessEntity> = emptyList(),
    val filteredItems: List<QuickAccessEntity> = emptyList(),
    val selectedType: QuickAccessType? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false
)

class QuickAccessViewModel(private val repository: QuickAccessRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(QuickAccessUiState())
    val uiState: StateFlow<QuickAccessUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getAllItems().collect { list ->
                _uiState.update { state ->
                    val filtered = applyFilter(list, state.selectedType, state.searchQuery)
                    state.copy(items = list, filteredItems = filtered, isLoading = false)
                }
            }
        }
    }

    fun selectType(type: QuickAccessType?) {
        _uiState.update { state ->
            val newType = if (state.selectedType == type) null else type
            val filtered = applyFilter(state.items, newType, state.searchQuery)
            state.copy(selectedType = newType, filteredItems = filtered)
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { state ->
            val filtered = applyFilter(state.items, state.selectedType, query)
            state.copy(searchQuery = query, filteredItems = filtered)
        }
    }

    fun saveItem(item: QuickAccessEntity, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.saveItem(item)
            if (result.isSuccess) onSuccess() else onError(result.exceptionOrNull()?.message ?: "Save failed")
        }
    }

    fun deleteItem(id: String) {
        _uiState.update { state ->
            val updated = state.items.filterNot { it.id == id }
            state.copy(items = updated, filteredItems = applyFilter(updated, state.selectedType, state.searchQuery))
        }
        viewModelScope.launch {
            repository.deleteItem(id)
        }
    }

    private fun applyFilter(list: List<QuickAccessEntity>, type: QuickAccessType?, query: String): List<QuickAccessEntity> {
        var res = list
        if (type != null) {
            res = res.filter { it.itemType == type }
        }
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            res = res.filter { it.title.lowercase().contains(q) || it.value.lowercase().contains(q) }
        }
        return res
    }
}
