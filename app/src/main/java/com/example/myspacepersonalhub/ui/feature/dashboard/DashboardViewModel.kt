package com.example.myspacepersonalhub.ui.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myspacepersonalhub.core.database.entity.ContactEntity
import com.example.myspacepersonalhub.core.database.entity.NoteEntity
import com.example.myspacepersonalhub.core.database.entity.TaskEntity
import com.example.myspacepersonalhub.core.datastore.UserPreferencesRepository
import com.example.myspacepersonalhub.core.sync.CloudSyncStatus
import com.example.myspacepersonalhub.core.sync.SyncManager
import com.example.myspacepersonalhub.core.util.CurrencyBalance
import com.example.myspacepersonalhub.core.util.CurrencyUtils
import com.example.myspacepersonalhub.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DashboardUiState(
    val greeting: String = "Hello",
    val dateDisplay: String = "",
    val todayTasks: List<TaskEntity> = emptyList(),
    val pinnedNotes: List<NoteEntity> = emptyList(),
    val recentContacts: List<ContactEntity> = emptyList(),
    val preferredCurrency: String = "USD",
    val currencyBalance: CurrencyBalance? = null,
    val contactCount: Int = 0,
    val activeTasksCount: Int = 0,
    val notesCount: Int = 0,
    val syncStatus: CloudSyncStatus = CloudSyncStatus.Unconfigured,
    val isLoading: Boolean = false
)

class DashboardViewModel(
    private val contactRepository: ContactRepository,
    private val noteRepository: NoteRepository,
    private val taskRepository: TaskRepository,
    private val moneyRepository: MoneyRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        updateDateAndGreeting()
        observeDashboardData()
        checkSyncStatus()
    }

    private fun updateDateAndGreeting() {
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val greeting = when (hour) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..21 -> "Good Evening"
            else -> "Good Night"
        }
        val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        _uiState.update { it.copy(greeting = greeting, dateDisplay = sdf.format(Date())) }
    }

    private fun checkSyncStatus() {
        viewModelScope.launch {
            syncManager.checkStatus()
            syncManager.syncStatus.collect { status ->
                _uiState.update { it.copy(syncStatus = status) }
            }
        }
    }

    private fun observeDashboardData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            combine(
                taskRepository.getTodayTasks(),
                noteRepository.getPinnedNotes(),
                contactRepository.getAllContacts(),
                moneyRepository.getAllTransactions(),
                preferencesRepository.userPreferencesFlow
            ) { tasks, pinned, contacts, transactions, prefs ->
                val currencyBalances = CurrencyUtils.calculateTotalsByCurrency(transactions)
                val curr = prefs.defaultCurrency
                val balance = currencyBalances[curr] ?: currencyBalances.values.firstOrNull()

                DashboardSummary(
                    todayTasks = tasks,
                    pinnedNotes = pinned,
                    recentContacts = contacts.take(6),
                    preferredCurrency = curr,
                    currencyBalance = balance,
                    contactCount = contacts.size,
                    activeTasksCount = tasks.size,
                    notesCount = pinned.size
                )
            }.collect { summary ->
                _uiState.update {
                    it.copy(
                        todayTasks = summary.todayTasks,
                        pinnedNotes = summary.pinnedNotes,
                        recentContacts = summary.recentContacts,
                        preferredCurrency = summary.preferredCurrency,
                        currencyBalance = summary.currencyBalance,
                        contactCount = summary.contactCount,
                        activeTasksCount = summary.activeTasksCount,
                        notesCount = summary.notesCount,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            syncManager.syncNow()
        }
    }

    private data class DashboardSummary(
        val todayTasks: List<TaskEntity>,
        val pinnedNotes: List<NoteEntity>,
        val recentContacts: List<ContactEntity>,
        val preferredCurrency: String,
        val currencyBalance: CurrencyBalance?,
        val contactCount: Int,
        val activeTasksCount: Int,
        val notesCount: Int
    )
}
