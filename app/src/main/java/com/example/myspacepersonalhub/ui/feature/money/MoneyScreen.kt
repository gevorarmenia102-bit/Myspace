package com.example.myspacepersonalhub.ui.feature.money

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myspacepersonalhub.core.database.entity.TransactionEntity
import com.example.myspacepersonalhub.core.model.TransactionType
import com.example.myspacepersonalhub.core.util.CurrencyUtils
import com.example.myspacepersonalhub.core.util.IntentUtils
import com.example.myspacepersonalhub.theme.*
import com.example.myspacepersonalhub.ui.components.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyScreen(
    viewModel: MoneyViewModel,
    onAddTransactionClick: () -> Unit,
    onAddBudgetClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val currentBalance = state.currencyBalances[state.selectedCurrency]
    val activeCurrencies = state.currencyBalances.keys.toList().ifEmpty { listOf("USD") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Money Manager", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = {
                            val csv = viewModel.exportToCsv()
                            IntentUtils.shareText(context, csv, "Export Transactions CSV")
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Export CSV", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransactionClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(horizontal = 16.dp)
        ) {
            // Currency Selector Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                activeCurrencies.forEach { curr ->
                    FilterChip(
                        selected = state.selectedCurrency == curr,
                        onClick = { viewModel.selectCurrency(curr) },
                        label = { Text(curr, fontWeight = FontWeight.Bold, fontSize = 10.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Balance Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        "NET BALANCE (${state.selectedCurrency})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val net = currentBalance?.netMinorUnits ?: 0L
                    Text(
                        text = CurrencyUtils.formatAmount(net, state.selectedCurrency),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (net >= 0) AccentEmerald else AccentRed
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AccentEmerald))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Income", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = CurrencyUtils.formatAmount(currentBalance?.incomeMinorUnits ?: 0L, state.selectedCurrency),
                                fontWeight = FontWeight.Bold,
                                color = AccentEmerald,
                                fontSize = 13.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AccentRed))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Expenses", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = CurrencyUtils.formatAmount(currentBalance?.expenseMinorUnits ?: 0L, state.selectedCurrency),
                                fontWeight = FontWeight.Bold,
                                color = AccentRed,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar & Filter
            SearchBarField(
                query = state.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                placeholder = "Search transactions..."
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.selectedType == null,
                    onClick = { viewModel.selectType(null) },
                    label = { Text("All", fontSize = 10.sp) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = state.selectedType == TransactionType.INCOME,
                    onClick = { viewModel.selectType(TransactionType.INCOME) },
                    label = { Text("Income", fontSize = 10.sp) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = state.selectedType == TransactionType.EXPENSE,
                    onClick = { viewModel.selectType(TransactionType.EXPENSE) },
                    label = { Text("Expense", fontSize = 10.sp) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Transaction List
            if (state.isLoading) {
                LoadingStateView(message = "Loading finances...")
            } else if (state.filteredTransactions.isEmpty()) {
                EmptyStateView(
                    title = "No Transactions",
                    message = "Add your personal income and expenses to track cash flow per currency.",
                    icon = Icons.Default.ReceiptLong,
                    actionLabel = "Add Transaction",
                    onAction = onAddTransactionClick
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(state.filteredTransactions, key = { it.id }) { tx ->
                        TransactionItemCard(
                            transaction = tx,
                            onDelete = { viewModel.deleteTransaction(tx.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItemCard(
    transaction: TransactionEntity,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isIncome = transaction.type == TransactionType.INCOME
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isIncome) AccentEmerald.copy(alpha = 0.15f) else AccentRed.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = if (isIncome) AccentEmerald else AccentRed,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${transaction.categoryId} • ${dateFormat.format(Date(transaction.transactionDate))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isIncome) "+" else "-"} ${CurrencyUtils.formatAmount(transaction.amountMinorUnits, transaction.currencyCode)}",
                    fontWeight = FontWeight.Bold,
                    color = if (isIncome) AccentEmerald else AccentRed,
                    fontSize = 13.sp
                )
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                }
            }
        }
    }

    if (showDeleteConfirm) {
        ConfirmationDialog(
            title = "Delete Transaction",
            message = "Are you sure you want to remove this transaction?",
            confirmLabel = "Delete",
            isDestructive = true,
            onConfirm = onDelete,
            onDismiss = { showDeleteConfirm = false }
        )
    }
}
