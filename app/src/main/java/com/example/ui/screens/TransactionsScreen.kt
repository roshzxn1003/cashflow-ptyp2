package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.TransactionEntity
import com.example.data.models.TransactionType
import com.example.ui.components.TransactionItemCard
import com.example.ui.viewmodel.CashFlowUiState
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    state: CashFlowUiState,
    onSearchQueryChange: (String) -> Unit,
    onFilterTypeChange: (TransactionType?) -> Unit,
    onFilterCategoryChange: (String?) -> Unit,
    onFilterMemberChange: (String?) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onOpenAddTransaction: () -> Unit,
    familyMembers: List<com.example.data.models.FamilyMemberEntity> = emptyList()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title and Add Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Transaction History",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            FloatingActionButton(
                onClick = onOpenAddTransaction,
                modifier = Modifier.size(44.dp).testTag("fab_add_transaction"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search title, category, or note...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("search_transactions_input"),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Type Filter Chips (All, Expense, Income)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = state.selectedFilterType == null,
                onClick = { onFilterTypeChange(null) },
                label = { Text("All") },
                modifier = Modifier.testTag("filter_all_chip")
            )
            FilterChip(
                selected = state.selectedFilterType == TransactionType.EXPENSE,
                onClick = { onFilterTypeChange(TransactionType.EXPENSE) },
                label = { Text("Expenses") },
                modifier = Modifier.testTag("filter_expense_chip")
            )
            FilterChip(
                selected = state.selectedFilterType == TransactionType.INCOME,
                onClick = { onFilterTypeChange(TransactionType.INCOME) },
                label = { Text("Income") },
                modifier = Modifier.testTag("filter_income_chip")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Category Filter Horizontal Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = state.selectedFilterCategory == null,
                    onClick = { onFilterCategoryChange(null) },
                    label = { Text("All Categories", fontSize = 11.sp) }
                )
            }
            items(state.categories, key = { "cat_${it.id}" }) { cat ->
                FilterChip(
                    selected = state.selectedFilterCategory == cat.name,
                    onClick = {
                        if (state.selectedFilterCategory == cat.name) onFilterCategoryChange(null)
                        else onFilterCategoryChange(cat.name)
                    },
                    label = { Text(cat.name, fontSize = 11.sp) },
                    modifier = Modifier.animateItem()
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Transactions List
        if (state.transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No matching transactions found.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(state.transactions, key = { "tx_${it.id}" }) { tx ->
                    TransactionItemCard(
                        transaction = tx,
                        currencySymbol = state.currencySymbol,
                        onDelete = onDeleteTransaction,
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}
