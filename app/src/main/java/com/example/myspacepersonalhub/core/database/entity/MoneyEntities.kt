package com.example.myspacepersonalhub.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.myspacepersonalhub.core.model.SyncState
import com.example.myspacepersonalhub.core.model.TransactionType
import java.util.UUID

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val type: TransactionType,
    val amountMinorUnits: Long, // e.g. 1500 for $15.00, integer minor units avoids floating point errors
    val currencyCode: String = "USD",
    val categoryId: String = "General",
    val title: String,
    val transactionDate: Long = System.currentTimeMillis(),
    val paymentMethod: String? = "Cash",
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncState: SyncState = SyncState.PENDING_CREATE,
    val ownerUid: String? = null
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val currencyCode: String = "USD",
    val categoryId: String? = null, // null means total budget
    val limitAmountMinorUnits: Long,
    val period: String = "MONTHLY", // MONTHLY, WEEKLY, YEARLY
    val startDate: Long = 0L,
    val endDate: Long = Long.MAX_VALUE,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncState: SyncState = SyncState.PENDING_CREATE,
    val ownerUid: String? = null
)
