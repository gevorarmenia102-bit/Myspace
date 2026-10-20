package com.example.myspacepersonalhub.navigation

sealed class Screen(val route: String) {
    // Top-level Bottom Bar Destinations
    object Dashboard : Screen("dashboard")
    object Contacts : Screen("contacts")
    object Notes : Screen("notes")
    object Tasks : Screen("tasks")
    object Money : Screen("money")

    // Drawer / More Destinations
    object Vault : Screen("vault")
    object Tracker : Screen("tracker")
    object QuickAccess : Screen("quick_access")
    object Settings : Screen("settings")

    // Detail & Edit Screens
    data class ContactDetail(val contactId: String) : Screen("contact_detail/$contactId")
    data class ContactEdit(val contactId: String? = null) : Screen("contact_edit/${contactId ?: "new"}")

    data class NoteEdit(val noteId: String? = null, val isJournal: Boolean = false) :
        Screen("note_edit/${noteId ?: "new"}?isJournal=$isJournal")

    data class TaskEdit(val taskId: String? = null) : Screen("task_edit/${taskId ?: "new"}")

    data class TransactionEdit(val transactionId: String? = null) :
        Screen("transaction_edit/${transactionId ?: "new"}")

    data class BudgetEdit(val budgetId: String? = null) :
        Screen("budget_edit/${budgetId ?: "new"}")

    data class VaultDetail(val itemId: String) : Screen("vault_detail/$itemId")
    data class VaultEdit(val itemId: String? = null) : Screen("vault_edit/${itemId ?: "new"}")
}
