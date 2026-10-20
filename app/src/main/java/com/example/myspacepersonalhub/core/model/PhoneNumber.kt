package com.example.myspacepersonalhub.core.model

import kotlinx.serialization.Serializable

@Serializable
data class PhoneNumber(
    val id: String = java.util.UUID.randomUUID().toString(),
    val number: String,
    val label: String = "Mobile", // Mobile, Home, Work, Other
    val isPrimary: Boolean = false
)
