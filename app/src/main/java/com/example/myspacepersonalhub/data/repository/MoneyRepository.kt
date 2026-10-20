package com.example.myspacepersonalhub.data.repository

import com.example.myspacepersonalhub.core.database.dao.MoneyDao
import com.example.myspacepersonalhub.core.database.entity.BudgetEntity
import com.example.myspacepersonalhub.core.database.entity.TransactionEntity
import com.example.myspacepersonalhub.core.model.SyncState
import com.example.myspacepersonalhub.core.util.CurrencyUtils
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MoneyRepository(
    private val moneyDao: MoneyDao,
    private val onDataChanged: (() -> Unit)? = null
) {

    fun getAllTransactions(): Flow<List<TransactionEntity>> = moneyDao.getAllTransactions()

    fun getRecentTransactions(limit: Int = 10): Flow<List<TransactionEntity>> = moneyDao.getRecentTransactions(limit)

    fun getTransactionsByCurrency(currencyCode: String): Flow<List<TransactionEntity>> =
        moneyDao.getTransactionsByCurrency(currencyCode)

    suspend fun saveTransaction(transaction: TransactionEntity): Result<Unit> {
        if (transaction.amountMinorUnits <= 0) {
            return Result.failure(IllegalArgumentException("Amount must be greater than zero"))
        }
        val trimmedTitle = transaction.title.trim()
        if (trimmedTitle.isEmpty()) {
            return Result.failure(IllegalArgumentException("Title cannot be blank"))
        }

        val updated = transaction.copy(
            title = trimmedTitle,
            currencyCode = transaction.currencyCode.uppercase().trim(),
            categoryId = transaction.categoryId.trim().ifBlank { "General" },
            updatedAt = System.currentTimeMillis(),
            syncState = if (transaction.syncState == SyncState.SYNCED) SyncState.PENDING_UPDATE else transaction.syncState
        )
        moneyDao.insertTransaction(updated)
        onDataChanged?.invoke()
        return Result.success(Unit)
    }

    suspend fun deleteTransaction(id: String) {
        moneyDao.softDeleteTransaction(id)
        onDataChanged?.invoke()
    }

    // Budgets
    fun getAllBudgets(): Flow<List<BudgetEntity>> = moneyDao.getAllBudgets()

    suspend fun saveBudget(budget: BudgetEntity): Result<Unit> {
        if (budget.limitAmountMinorUnits <= 0) {
            return Result.failure(IllegalArgumentException("Budget limit must be greater than zero"))
        }
        moneyDao.insertBudget(budget.copy(updatedAt = System.currentTimeMillis()))
        onDataChanged?.invoke()
        return Result.success(Unit)
    }

    suspend fun deleteBudget(id: String) {
        moneyDao.softDeleteBudget(id)
        onDataChanged?.invoke()
    }

    fun exportToCsv(transactions: List<TransactionEntity>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val sb = StringBuilder()
        sb.appendLine("Date,Title,Type,Amount,Currency,Category,PaymentMethod,Notes")
        for (tx in transactions) {
            val dateStr = dateFormat.format(Date(tx.transactionDate))
            val amountFormatted = CurrencyUtils.formatAmount(tx.amountMinorUnits, tx.currencyCode)
            sb.appendLine("\"$dateStr\",\"${tx.title}\",\"${tx.type.name}\",\"$amountFormatted\",\"${tx.currencyCode}\",\"${tx.categoryId}\",\"${tx.paymentMethod ?: ""}\",\"${(tx.notes ?: "").replace("\"", "\"\"")}\"")
        }
        return sb.toString()
    }
}
