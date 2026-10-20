package com.example.myspacepersonalhub.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class SyncState {
    PENDING_CREATE,
    PENDING_UPDATE,
    PENDING_DELETE,
    SYNCED,
    CONFLICT,
    ERROR
}

@Serializable
enum class Priority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}

@Serializable
enum class TaskStatus {
    TODO,
    IN_PROGRESS,
    DONE,
    CANCELLED
}

@Serializable
enum class TransactionType {
    INCOME,
    EXPENSE
}

@Serializable
enum class VaultItemType {
    SECURE_NOTE,
    DOCUMENT,
    IMAGE,
    FILE
}

@Serializable
enum class GoalStatus {
    IN_PROGRESS,
    ACHIEVED,
    ABANDONED
}

@Serializable
enum class QuickAccessType {
    URL,
    PHONE,
    EMAIL,
    ADDRESS,
    TEXT
}

@Serializable
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}
