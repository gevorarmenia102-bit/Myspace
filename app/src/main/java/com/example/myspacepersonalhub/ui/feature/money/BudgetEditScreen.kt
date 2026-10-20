package com.example.myspacepersonalhub.ui.feature.money

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
import com.example.myspacepersonalhub.core.database.entity.BudgetEntity
import com.example.myspacepersonalhub.core.util.CurrencyUtils
import com.example.myspacepersonalhub.theme.AccentRed
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetEditScreen(
    viewModel: MoneyViewModel,
    defaultCurrency: String = "USD",
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var limitAmountInput by remember { mutableStateOf("") }
    var currencyCode by remember { mutableStateOf(defaultCurrency) }
    var period by remember { mutableStateOf("MONTHLY") }
    var limitError by remember { mutableStateOf<String?>(null) }
    var nameError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Set Monthly Budget", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            val minorUnits = CurrencyUtils.parseToMinorUnits(limitAmountInput, currencyCode)
                            if (minorUnits == null || minorUnits <= 0) {
                                limitError = "Enter a valid positive amount"
                                return@Button
                            }
                            if (name.trim().isBlank()) {
                                nameError = "Budget name is required"
                                return@Button
                            }

                            val budget = BudgetEntity(
                                id = UUID.randomUUID().toString(),
                                name = name.trim(),
                                currencyCode = currencyCode.uppercase().trim(),
                                limitAmountMinorUnits = minorUnits,
                                period = period
                            )

                            viewModel.saveBudget(
                                budget = budget,
                                onSuccess = onSaveSuccess,
                                onError = { limitError = it }
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
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (nameError != null) nameError = null
                },
                label = { Text("Budget Name (e.g. Monthly Total, Dining)") },
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it, color = AccentRed) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = limitAmountInput,
                    onValueChange = {
                        limitAmountInput = it
                        if (limitError != null) limitError = null
                    },
                    label = { Text("Limit Amount *") },
                    isError = limitError != null,
                    supportingText = limitError?.let { { Text(it, color = AccentRed) } },
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

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}
