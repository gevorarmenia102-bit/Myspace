package com.example.myspacepersonalhub.ui.feature.money

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myspacepersonalhub.core.database.entity.TransactionEntity
import com.example.myspacepersonalhub.core.model.TransactionType
import com.example.myspacepersonalhub.core.util.CurrencyUtils
import com.example.myspacepersonalhub.theme.AccentEmerald
import com.example.myspacepersonalhub.theme.AccentRed
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditScreen(
    viewModel: MoneyViewModel,
    defaultCurrency: String = "USD",
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amountInput by remember { mutableStateOf("") }
    var currencyCode by remember { mutableStateOf(defaultCurrency) }
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("General") }
    var paymentMethod by remember { mutableStateOf("Card") }
    var notes by remember { mutableStateOf("") }

    var amountError by remember { mutableStateOf<String?>(null) }
    var titleError by remember { mutableStateOf<String?>(null) }

    val incomeCategories = listOf("Salary", "Freelance", "Investment", "Gift", "Refund", "General")
    val expenseCategories = listOf("Food & Dining", "Groceries", "Transport", "Utilities", "Shopping", "Entertainment", "Health", "General")
    val currentCategories = if (type == TransactionType.INCOME) incomeCategories else expenseCategories

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Transaction", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            val minorUnits = CurrencyUtils.parseToMinorUnits(amountInput, currencyCode)
                            if (minorUnits == null || minorUnits <= 0) {
                                amountError = "Please enter a valid amount greater than zero"
                                return@Button
                            } else {
                                amountError = null
                            }

                            if (title.trim().isBlank()) {
                                titleError = "Title is required"
                                return@Button
                            } else {
                                titleError = null
                            }

                            val tx = TransactionEntity(
                                id = UUID.randomUUID().toString(),
                                type = type,
                                amountMinorUnits = minorUnits,
                                currencyCode = currencyCode.uppercase().trim(),
                                categoryId = category.trim(),
                                title = title.trim(),
                                transactionDate = System.currentTimeMillis(),
                                paymentMethod = paymentMethod.trim().ifBlank { null },
                                notes = notes.trim().ifBlank { null }
                            )

                            viewModel.saveTransaction(
                                transaction = tx,
                                onSuccess = onSaveSuccess,
                                onError = { amountError = it }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Save", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Type Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = type == TransactionType.EXPENSE,
                    onClick = { type = TransactionType.EXPENSE },
                    label = { Text("Expense", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentRed.copy(alpha = 0.2f),
                        selectedLabelColor = AccentRed
                    )
                )
                FilterChip(
                    selected = type == TransactionType.INCOME,
                    onClick = { type = TransactionType.INCOME },
                    label = { Text("Income", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentEmerald.copy(alpha = 0.2f),
                        selectedLabelColor = AccentEmerald
                    )
                )
            }

            // Amount & Currency
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = {
                        amountInput = it
                        if (amountError != null) amountError = null
                    },
                    label = { Text("Amount *") },
                    isError = amountError != null,
                    supportingText = amountError?.let { { Text(it, color = AccentRed) } },
                    singleLine = true,
                    modifier = Modifier.weight(1.5f),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = currencyCode,
                    onValueChange = { currencyCode = it.uppercase() },
                    label = { Text("Currency") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Quick Currencies
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CurrencyUtils.SUPPORTED_CURRENCIES.forEach { curr ->
                    FilterChip(
                        selected = currencyCode == curr,
                        onClick = { currencyCode = curr },
                        label = { Text(curr, fontSize = 9.sp) }
                    )
                }
            }

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (titleError != null) titleError = null
                },
                label = { Text("Title / Payee *") },
                isError = titleError != null,
                supportingText = titleError?.let { { Text(it, color = AccentRed) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Category Chips
            Text("CATEGORY", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                currentCategories.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat, fontSize = 10.sp) }
                    )
                }
            }

            // Payment Method
            Text("PAYMENT METHOD", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Cash", "Card", "Bank Transfer", "Other").forEach { method ->
                    FilterChip(
                        selected = paymentMethod == method,
                        onClick = { paymentMethod = method },
                        label = { Text(method, fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (Optional)") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}
