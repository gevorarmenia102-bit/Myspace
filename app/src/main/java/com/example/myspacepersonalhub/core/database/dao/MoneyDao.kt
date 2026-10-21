package com.example.myspacepersonalhub.core.database.dao

import androidx.room.*
import com.example.myspacepersonalhub.core.database.entity.BudgetEntity
import com.example.myspacepersonalhub.core.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MoneyDao {
    @Query("SELECT * FROM transactions WHERE deletedAt IS NULL ORDER BY transactionDate DESC, createdAt DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE deletedAt IS NULL ORDER BY transactionDate DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int = 10): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE deletedAt IS NULL AND currencyCode = :currencyCode ORDER BY transactionDate DESC")
    fun getTransactionsByCurrency(currencyCode: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE deletedAt IS NULL AND transactionDate >= :startDate AND transactionDate <= :endDate ORDER BY transactionDate DESC")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id AND deletedAt IS NULL")
    fun getTransactionById(id: String): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionByIdDirect(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE syncState != 'SYNCED'")
    suspend fun getPendingSyncTransactions(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("UPDATE transactions SET deletedAt = :timestamp, syncState = 'PENDING_DELETE' WHERE id = :id")
    suspend fun softDeleteTransaction(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE transactions SET syncState = 'PENDING_UPDATE' WHERE syncState = 'SYNCED'")
    suspend fun markAllSyncedAsPending()

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    // Budgets
    @Query("SELECT * FROM budgets WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE id = :id AND deletedAt IS NULL")
    fun getBudgetById(id: String): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE id = :id LIMIT 1")
    suspend fun getBudgetByIdDirect(id: String): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE syncState != 'SYNCED'")
    suspend fun getPendingSyncBudgets(): List<BudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Query("UPDATE budgets SET deletedAt = :timestamp, syncState = 'PENDING_DELETE' WHERE id = :id")
    suspend fun softDeleteBudget(id: String, timestamp: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)
}
