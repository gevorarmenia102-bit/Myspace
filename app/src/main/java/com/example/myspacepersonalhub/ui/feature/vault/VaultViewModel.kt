package com.example.myspacepersonalhub.ui.feature.vault

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myspacepersonalhub.core.database.entity.VaultItemEntity
import com.example.myspacepersonalhub.core.datastore.UserPreferencesRepository
import com.example.myspacepersonalhub.core.model.VaultItemType
import com.example.myspacepersonalhub.data.repository.VaultRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

data class VaultUiState(
    val items: List<VaultItemEntity> = emptyList(),
    val isUnlocked: Boolean = false,
    val biometricEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val searchQuery: String = ""
)

class VaultViewModel(
    private val vaultRepository: VaultRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    init {
        observePreferences()
        loadItems()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesRepository.userPreferencesFlow.collect { prefs ->
                _uiState.update { state ->
                    // If biometric lock is disabled in settings, unlock automatically
                    state.copy(
                        biometricEnabled = prefs.biometricLockEnabled,
                        isUnlocked = if (!prefs.biometricLockEnabled) true else state.isUnlocked
                    )
                }
            }
        }
    }

    private fun loadItems() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            vaultRepository.getAllVaultItems().collect { itemList ->
                _uiState.update { it.copy(items = itemList, isLoading = false) }
            }
        }
    }

    fun unlockVault() {
        _uiState.update { it.copy(isUnlocked = true) }
    }

    fun lockVault() {
        if (_uiState.value.biometricEnabled) {
            _uiState.update { it.copy(isUnlocked = false) }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun saveSecureNote(
        title: String,
        secretText: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = vaultRepository.saveSecureNote(title, secretText)
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "Save failed")
            }
        }
    }

    fun updateSecureNote(
        id: String,
        title: String,
        secretText: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = vaultRepository.updateSecureNote(id, title, secretText)
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "Update failed")
            }
        }
    }

    fun decryptNote(item: VaultItemEntity): String {
        return vaultRepository.decryptSecureNote(item)
    }

    fun deleteItem(item: VaultItemEntity) {
        viewModelScope.launch {
            vaultRepository.deleteVaultItem(item)
        }
    }

    fun getVaultItemById(id: String): Flow<VaultItemEntity?> {
        return vaultRepository.getVaultItemById(id)
    }
}
