package com.example.myspacepersonalhub.ui.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myspacepersonalhub.core.datastore.UserPreferences
import com.example.myspacepersonalhub.core.datastore.UserPreferencesRepository
import com.example.myspacepersonalhub.core.model.ThemeMode
import com.example.myspacepersonalhub.core.security.BackupCrypto
import com.example.myspacepersonalhub.core.storage.GitHubRepoInfo
import com.example.myspacepersonalhub.core.sync.CloudSyncStatus
import com.example.myspacepersonalhub.core.sync.SyncManager
import com.example.myspacepersonalhub.data.repository.BackupRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val syncStatus: CloudSyncStatus = CloudSyncStatus.Unconfigured,
    val isTestingGitHub: Boolean = false,
    val gitHubConnectionMessage: String? = null,
    val isGitHubConnected: Boolean? = null,
    val isUploadingBackupToCloud: Boolean = false,
    val cloudBackupMessage: String? = null
)

class SettingsViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val backupRepository: BackupRepository,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            combine(
                preferencesRepository.userPreferencesFlow,
                syncManager.syncStatus
            ) { prefs, sync ->
                Pair(prefs, sync)
            }.collect { (prefs, sync) ->
                _uiState.update { it.copy(preferences = prefs, syncStatus = sync) }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun setDefaultCurrency(currency: String) {
        viewModelScope.launch {
            preferencesRepository.setDefaultCurrency(currency)
        }
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setBiometricLockEnabled(enabled)
        }
    }

    fun setGitHubConfig(token: String?, owner: String?, repo: String?, branch: String = "main") {
        viewModelScope.launch {
            preferencesRepository.setGitHubConfig(token, owner, repo, branch)
        }
    }

    fun setFirebaseDatabaseUrl(url: String?) {
        viewModelScope.launch {
            preferencesRepository.setFirebaseDatabaseUrl(url)
        }
    }

    fun setRealtimeSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setRealtimeSyncEnabled(enabled)
            if (enabled) {
                syncManager.startRealtimeSync()
            } else {
                syncManager.stopRealtimeSync()
            }
        }
    }

    fun testGitHubConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingGitHub = true, gitHubConnectionMessage = null, isGitHubConnected = null) }
            val result = syncManager.testGitHubConnection()
            if (result.isSuccess) {
                val info = result.getOrNull()
                val privStatus = if (info?.isPrivate == true) "Private Repo" else "Public Repo"
                _uiState.update {
                    it.copy(
                        isTestingGitHub = false,
                        isGitHubConnected = true,
                        gitHubConnectionMessage = "Connected to ${info?.fullName} ($privStatus, branch: ${info?.defaultBranch})"
                    )
                }
            } else {
                val err = result.exceptionOrNull()?.message ?: "Connection failed"
                _uiState.update {
                    it.copy(
                        isTestingGitHub = false,
                        isGitHubConnected = false,
                        gitHubConnectionMessage = err
                    )
                }
            }
        }
    }

    fun uploadBackupToGitHub(passphrase: String, onComplete: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            if (passphrase.length < 6) {
                onError("Passphrase must be at least 6 characters")
                return@launch
            }
            _uiState.update { it.copy(isUploadingBackupToCloud = true, cloudBackupMessage = null) }
            val result = syncManager.uploadEncryptedBackupToGitHub(passphrase)
            if (result.isSuccess) {
                val msg = result.getOrNull() ?: "Backup successfully uploaded to GitHub"
                _uiState.update { it.copy(isUploadingBackupToCloud = false, cloudBackupMessage = msg) }
                onComplete(msg)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Upload failed"
                _uiState.update { it.copy(isUploadingBackupToCloud = false, cloudBackupMessage = err) }
                onError(err)
            }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            syncManager.syncNow()
        }
    }

    fun createEncryptedBackup(passphrase: String, onComplete: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                if (passphrase.length < 6) {
                    onError("Passphrase must be at least 6 characters")
                    return@launch
                }
                val backup = backupRepository.createEncryptedBackup(passphrase.toCharArray())
                val jsonPayload = Json.encodeToString(backup)
                onComplete(jsonPayload)
            } catch (e: Exception) {
                onError(e.message ?: "Backup failed")
            }
        }
    }

    fun restoreEncryptedBackup(
        backupJson: String,
        passphrase: String,
        onSuccess: (Int) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val backup = Json.decodeFromString<BackupCrypto.EncryptedBackup>(backupJson)
                val result = backupRepository.restoreEncryptedBackup(backup, passphrase.toCharArray())
                if (result.isSuccess) {
                    onSuccess(result.getOrNull() ?: 0)
                } else {
                    onError("Failed to decrypt: Incorrect passphrase or corrupted backup file.")
                }
            } catch (e: Exception) {
                onError("Invalid backup format or incorrect passphrase.")
            }
        }
    }
}
