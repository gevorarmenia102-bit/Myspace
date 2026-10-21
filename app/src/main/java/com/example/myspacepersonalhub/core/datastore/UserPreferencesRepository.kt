package com.example.myspacepersonalhub.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.myspacepersonalhub.core.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore by preferencesDataStore(name = "user_preferences")

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val defaultCurrency: String = "USD",
    val biometricLockEnabled: Boolean = false,
    val autoLockTimeoutSeconds: Int = 60,
    val cloudSyncEnabled: Boolean = false,
    val lastSyncTimestamp: Long = 0L,
    val userEmail: String? = null,
    val demoDataSeeded: Boolean = false,
    val githubToken: String? = null,
    val githubOwner: String? = null,
    val githubRepo: String? = null,
    val githubBranch: String = "main",
    val firebaseDatabaseUrl: String? = null,
    val realtimeSyncEnabled: Boolean = true,
    val hubMigrationV2: Boolean = false
)

class UserPreferencesRepository(private val context: Context) {
    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_CURRENCY = stringPreferencesKey("default_currency")
        val BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock")
        val AUTO_LOCK_TIMEOUT = intPreferencesKey("auto_lock_timeout")
        val CLOUD_SYNC_ENABLED = booleanPreferencesKey("cloud_sync_enabled")
        val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val DEMO_DATA_SEEDED = booleanPreferencesKey("demo_data_seeded")
        val GITHUB_TOKEN = stringPreferencesKey("github_token")
        val GITHUB_OWNER = stringPreferencesKey("github_owner")
        val GITHUB_REPO = stringPreferencesKey("github_repo")
        val GITHUB_BRANCH = stringPreferencesKey("github_branch")
        val FIREBASE_DATABASE_URL = stringPreferencesKey("firebase_database_url")
        val REALTIME_SYNC_ENABLED = booleanPreferencesKey("realtime_sync_enabled")
        val HUB_MIGRATION_V2 = booleanPreferencesKey("hub_migration_v2")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val themeModeStr = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.DARK.name
            val themeMode = try {
                ThemeMode.valueOf(themeModeStr)
            } catch (_: Exception) {
                ThemeMode.DARK
            }
            UserPreferences(
                themeMode = themeMode,
                defaultCurrency = preferences[PreferencesKeys.DEFAULT_CURRENCY] ?: "USD",
                biometricLockEnabled = preferences[PreferencesKeys.BIOMETRIC_LOCK] ?: false,
                autoLockTimeoutSeconds = preferences[PreferencesKeys.AUTO_LOCK_TIMEOUT] ?: 60,
                cloudSyncEnabled = preferences[PreferencesKeys.CLOUD_SYNC_ENABLED] ?: false,
                lastSyncTimestamp = preferences[PreferencesKeys.LAST_SYNC_TIMESTAMP] ?: 0L,
                userEmail = preferences[PreferencesKeys.USER_EMAIL],
                demoDataSeeded = preferences[PreferencesKeys.DEMO_DATA_SEEDED] ?: false,
                githubToken = preferences[PreferencesKeys.GITHUB_TOKEN],
                githubOwner = preferences[PreferencesKeys.GITHUB_OWNER],
                githubRepo = preferences[PreferencesKeys.GITHUB_REPO],
                githubBranch = preferences[PreferencesKeys.GITHUB_BRANCH] ?: "main",
                firebaseDatabaseUrl = preferences[PreferencesKeys.FIREBASE_DATABASE_URL],
                realtimeSyncEnabled = preferences[PreferencesKeys.REALTIME_SYNC_ENABLED] ?: true,
                hubMigrationV2 = preferences[PreferencesKeys.HUB_MIGRATION_V2] ?: false
            )
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    suspend fun setDefaultCurrency(currency: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_CURRENCY] = currency.uppercase().trim()
        }
    }

    suspend fun setBiometricLockEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BIOMETRIC_LOCK] = enabled
        }
    }

    suspend fun setAutoLockTimeout(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_LOCK_TIMEOUT] = seconds
        }
    }

    suspend fun setCloudSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CLOUD_SYNC_ENABLED] = enabled
        }
    }

    suspend fun setLastSyncTimestamp(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_SYNC_TIMESTAMP] = timestamp
        }
    }

    suspend fun setUserEmail(email: String?) {
        context.dataStore.edit { preferences ->
            if (email != null) {
                preferences[PreferencesKeys.USER_EMAIL] = email
            } else {
                preferences.remove(PreferencesKeys.USER_EMAIL)
            }
        }
    }

    suspend fun setDemoDataSeeded(seeded: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEMO_DATA_SEEDED] = seeded
        }
    }

    suspend fun setGitHubConfig(token: String?, owner: String?, repo: String?, branch: String = "main") {
        context.dataStore.edit { preferences ->
            if (!token.isNullOrBlank()) preferences[PreferencesKeys.GITHUB_TOKEN] = token.trim() else preferences.remove(PreferencesKeys.GITHUB_TOKEN)
            if (!owner.isNullOrBlank()) preferences[PreferencesKeys.GITHUB_OWNER] = owner.trim() else preferences.remove(PreferencesKeys.GITHUB_OWNER)
            if (!repo.isNullOrBlank()) preferences[PreferencesKeys.GITHUB_REPO] = repo.trim() else preferences.remove(PreferencesKeys.GITHUB_REPO)
            preferences[PreferencesKeys.GITHUB_BRANCH] = branch.trim().ifBlank { "main" }
        }
    }

    suspend fun setFirebaseDatabaseUrl(url: String?) {
        context.dataStore.edit { preferences ->
            if (!url.isNullOrBlank()) preferences[PreferencesKeys.FIREBASE_DATABASE_URL] = url.trim() else preferences.remove(PreferencesKeys.FIREBASE_DATABASE_URL)
        }
    }

    suspend fun setRealtimeSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.REALTIME_SYNC_ENABLED] = enabled
        }
    }

    suspend fun setHubMigrationV2Done(done: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HUB_MIGRATION_V2] = done
        }
    }
}
