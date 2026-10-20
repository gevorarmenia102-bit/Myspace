package com.example.myspacepersonalhub.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.myspacepersonalhub.MySpaceApplication
import com.example.myspacepersonalhub.ui.components.MySpaceScaffold
import com.example.myspacepersonalhub.ui.feature.contacts.*
import com.example.myspacepersonalhub.ui.feature.dashboard.*
import com.example.myspacepersonalhub.ui.feature.money.*
import com.example.myspacepersonalhub.ui.feature.notes.*
import com.example.myspacepersonalhub.ui.feature.quickaccess.*
import com.example.myspacepersonalhub.ui.feature.settings.*
import com.example.myspacepersonalhub.ui.feature.tasks.*
import com.example.myspacepersonalhub.ui.feature.tracker.*
import com.example.myspacepersonalhub.ui.feature.vault.*

@Composable
fun NavigationHost(app: MySpaceApplication) {
    var backStack by remember { mutableStateOf(listOf<Screen>(Screen.Dashboard)) }
    val currentScreen = backStack.lastOrNull() ?: Screen.Dashboard

    val navigateTo: (Screen) -> Unit = { screen ->
        // If navigating to a top-level tab, clear intermediate details
        if (screen in listOf(Screen.Dashboard, Screen.Contacts, Screen.Notes, Screen.Tasks, Screen.Money)) {
            backStack = listOf(screen)
        } else {
            backStack = backStack + screen
        }
    }

    val popBack: () -> Unit = {
        if (backStack.size > 1) {
            backStack = backStack.dropLast(1)
        }
    }

    BackHandler(enabled = backStack.size > 1) {
        popBack()
    }

    // ViewModels initialized from Application Service Locator
    val dashboardViewModel = remember {
        DashboardViewModel(
            contactRepository = app.contactRepository,
            noteRepository = app.noteRepository,
            taskRepository = app.taskRepository,
            moneyRepository = app.moneyRepository,
            preferencesRepository = app.preferencesRepository,
            syncManager = app.syncManager
        )
    }

    val contactsViewModel = remember { ContactsViewModel(app.contactRepository) }
    val notesViewModel = remember { NotesViewModel(app.noteRepository) }
    val tasksViewModel = remember { TasksViewModel(app.taskRepository) }
    val moneyViewModel = remember { MoneyViewModel(app.moneyRepository) }
    val vaultViewModel = remember { VaultViewModel(app.vaultRepository, app.preferencesRepository) }
    val trackerViewModel = remember {
        TrackerViewModel(
            trackerRepository = app.trackerRepository,
            taskRepository = app.taskRepository,
            contactRepository = app.contactRepository
        )
    }
    val quickAccessViewModel = remember { QuickAccessViewModel(app.quickAccessRepository) }
    val settingsViewModel = remember {
        SettingsViewModel(
            preferencesRepository = app.preferencesRepository,
            backupRepository = app.backupRepository,
            syncManager = app.syncManager
        )
    }

    val dashboardState by dashboardViewModel.uiState.collectAsState()

    // Determine if bottom navigation bar should be shown (shown for primary tabs)
    val isPrimaryScreen = currentScreen in listOf(
        Screen.Dashboard,
        Screen.Contacts,
        Screen.Notes,
        Screen.Tasks,
        Screen.Money,
        Screen.Vault,
        Screen.Tracker,
        Screen.QuickAccess,
        Screen.Settings
    )

    if (isPrimaryScreen) {
        MySpaceScaffold(
            currentScreen = currentScreen,
            onNavigate = navigateTo,
            syncStatus = dashboardState.syncStatus,
            onSyncClick = { dashboardViewModel.syncNow() }
        ) {
            when (currentScreen) {
                is Screen.Dashboard -> DashboardScreen(
                    viewModel = dashboardViewModel,
                    onNavigate = navigateTo,
                    onAddContact = { navigateTo(Screen.ContactEdit(null)) },
                    onAddNote = { navigateTo(Screen.NoteEdit(null, false)) },
                    onAddTask = { navigateTo(Screen.TaskEdit(null)) },
                    onAddTransaction = { navigateTo(Screen.TransactionEdit(null)) },
                    onAddVaultItem = { navigateTo(Screen.VaultEdit(null)) }
                )
                is Screen.Contacts -> ContactListScreen(
                    viewModel = contactsViewModel,
                    onContactClick = { id -> navigateTo(Screen.ContactDetail(id)) },
                    onAddContactClick = { navigateTo(Screen.ContactEdit(null)) }
                )
                is Screen.Notes -> NotesScreen(
                    viewModel = notesViewModel,
                    onNoteClick = { id -> navigateTo(Screen.NoteEdit(id, false)) },
                    onAddNoteClick = { isJournal -> navigateTo(Screen.NoteEdit(null, isJournal)) }
                )
                is Screen.Tasks -> TasksScreen(
                    viewModel = tasksViewModel,
                    onTaskClick = { id -> navigateTo(Screen.TaskEdit(id)) },
                    onAddTaskClick = { navigateTo(Screen.TaskEdit(null)) }
                )
                is Screen.Money -> MoneyScreen(
                    viewModel = moneyViewModel,
                    onAddTransactionClick = { navigateTo(Screen.TransactionEdit(null)) },
                    onAddBudgetClick = { navigateTo(Screen.BudgetEdit(null)) }
                )
                is Screen.Vault -> VaultScreen(
                    viewModel = vaultViewModel,
                    onItemClick = { id -> navigateTo(Screen.VaultDetail(id)) },
                    onAddItemClick = { navigateTo(Screen.VaultEdit(null)) }
                )
                is Screen.Tracker -> CalendarTrackerScreen(viewModel = trackerViewModel)
                is Screen.QuickAccess -> QuickAccessScreen(viewModel = quickAccessViewModel)
                is Screen.Settings -> SettingsScreen(viewModel = settingsViewModel)
                else -> {}
            }
        }
    } else {
        // Detail / Editor sub-screens (full screen without bottom navigation)
        when (currentScreen) {
            is Screen.ContactDetail -> ContactDetailScreen(
                contactId = currentScreen.contactId,
                viewModel = contactsViewModel,
                onBackClick = popBack,
                onEditClick = { id -> navigateTo(Screen.ContactEdit(id)) }
            )
            is Screen.ContactEdit -> ContactEditScreen(
                contactId = currentScreen.contactId,
                viewModel = contactsViewModel,
                onBackClick = popBack,
                onSaveSuccess = popBack
            )
            is Screen.NoteEdit -> NoteEditScreen(
                noteId = currentScreen.noteId,
                isJournal = currentScreen.isJournal,
                viewModel = notesViewModel,
                onBackClick = popBack,
                onSaveSuccess = popBack
            )
            is Screen.TaskEdit -> TaskEditScreen(
                taskId = currentScreen.taskId,
                viewModel = tasksViewModel,
                onBackClick = popBack,
                onSaveSuccess = popBack
            )
            is Screen.TransactionEdit -> TransactionEditScreen(
                viewModel = moneyViewModel,
                defaultCurrency = dashboardState.preferredCurrency,
                onBackClick = popBack,
                onSaveSuccess = popBack
            )
            is Screen.BudgetEdit -> BudgetEditScreen(
                viewModel = moneyViewModel,
                defaultCurrency = dashboardState.preferredCurrency,
                onBackClick = popBack,
                onSaveSuccess = popBack
            )
            is Screen.VaultDetail -> VaultDetailScreen(
                itemId = currentScreen.itemId,
                viewModel = vaultViewModel,
                onBackClick = popBack,
                onEditClick = { id -> navigateTo(Screen.VaultEdit(id)) }
            )
            is Screen.VaultEdit -> VaultEditScreen(
                itemId = currentScreen.itemId,
                viewModel = vaultViewModel,
                onBackClick = popBack,
                onSaveSuccess = popBack
            )
            else -> {}
        }
    }
}
