package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.models.BudgetEntity
import com.example.data.models.SavingsGoalEntity
import com.example.data.models.TransactionType
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.CashFlowUiState
import java.util.*

@Composable
fun BudgetsAndGoalsScreen(
    state: CashFlowUiState,
    onSaveBudget: (categoryName: String, limit: Double, periodType: String, customPeriodName: String, budgetId: Long) -> Unit,
    onDeleteBudget: (BudgetEntity) -> Unit,
    onSaveSavingsGoal: (SavingsGoalEntity) -> Unit,
    onDeleteSavingsGoal: (SavingsGoalEntity) -> Unit,
    onDepositToGoal: (SavingsGoalEntity, Double) -> Unit
) {
    var showAddBudgetModal by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<BudgetEntity?>(null) }
    var budgetToDelete by remember { mutableStateOf<BudgetEntity?>(null) }

    var showAddGoalModal by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var goalToDelete by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var selectedGoalForDeposit by remember { mutableStateOf<SavingsGoalEntity?>(null) }

    var selectedPeriodFilter by remember { mutableStateOf("ALL") } // ALL, MONTHLY, WEEKLY, CUSTOM

    val filteredBudgets = remember(state.budgets, selectedPeriodFilter) {
        when (selectedPeriodFilter) {
            "MONTHLY" -> state.budgets.filter { it.periodType == "MONTHLY" }
            "WEEKLY" -> state.budgets.filter { it.periodType == "WEEKLY" }
            "CUSTOM" -> state.budgets.filter { it.periodType == "CUSTOM" || it.periodType == "YEARLY" }
            else -> state.budgets
        }
    }

    val sdfMonth = remember { java.text.SimpleDateFormat("yyyy-MM", Locale.getDefault()) }
    val sdfYear = remember { java.text.SimpleDateFormat("yyyy", Locale.getDefault()) }

    // Helper to calculate spent for a specific budget
    val calculateSpent = { budget: BudgetEntity, txs: List<com.example.data.models.TransactionEntity> ->
        txs.filter { it.type == TransactionType.EXPENSE && it.category == budget.categoryName }
            .filter { tx ->
                val date = Date(tx.dateMillis)
                when (budget.periodType) {
                    "MONTHLY" -> sdfMonth.format(date) == budget.monthYear
                    "YEARLY" -> sdfYear.format(date) == budget.monthYear.substring(0, 4)
                    else -> true // Treat custom/weekly as all-time for simplicity in this prototype
                }
            }
            .sumOf { it.amount }
    }

    // Calculated totals for budgets
    val totalBudgeted = remember(filteredBudgets) { filteredBudgets.sumOf { it.monthlyLimit } }
    val totalSpentOnBudgets = remember(filteredBudgets, state.transactions) {
        filteredBudgets.sumOf { budget ->
            calculateSpent(budget, state.transactions)
        }
    }
    val totalRemaining = totalBudgeted - totalSpentOnBudgets
    val overallProgress = if (totalBudgeted > 0) (totalSpentOnBudgets / totalBudgeted).toFloat().coerceIn(0f, 1f) else 0f
    val overbudgetCount = remember(filteredBudgets, state.transactions) {
        filteredBudgets.count { budget ->
            val spent = calculateSpent(budget, state.transactions)
            spent > budget.monthlyLimit
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
    ) {
        // --- OVERVIEW SUMMARY CARD ---
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Budget Health Overview",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Tracking ${state.budgets.size} active spending limits",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }

                        AnimatedVisibility(visible = overbudgetCount > 0) {
                            Surface(
                                color = ExpenseRed,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "$overbudgetCount Over",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Total Spent",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "${state.currencySymbol}${String.format(Locale.US, "%.2f", totalSpentOnBudgets)}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (totalSpentOnBudgets > totalBudgeted && totalBudgeted > 0) ExpenseRed else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Total Budgeted",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "${state.currencySymbol}${String.format(Locale.US, "%.2f", totalBudgeted)}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { overallProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape),
                        color = when {
                            overallProgress >= 1.0f -> ExpenseRed
                            overallProgress >= 0.8f -> GoldAccent
                            else -> IncomeGreen
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (totalRemaining >= 0) "Remaining: ${state.currencySymbol}${String.format(Locale.US, "%.2f", totalRemaining)}"
                            else "Over budget by ${state.currencySymbol}${String.format(Locale.US, "%.2f", -totalRemaining)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (totalRemaining < 0) ExpenseRed else IncomeGreen
                        )

                        Text(
                            text = "${(overallProgress * 100).toInt()}% Used",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // --- SECTION 1: BUDGETS HEADER & PERIOD FILTERS ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Category Budgets",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Set spending limits for monthly or custom periods",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = {
                        editingBudget = null
                        showAddBudgetModal = true
                    },
                    modifier = Modifier.testTag("add_budget_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Budget", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Period Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedPeriodFilter == "ALL",
                    onClick = { selectedPeriodFilter = "ALL" },
                    label = { Text("All (${state.budgets.size})", fontSize = 12.sp) }
                )
                FilterChip(
                    selected = selectedPeriodFilter == "MONTHLY",
                    onClick = { selectedPeriodFilter = "MONTHLY" },
                    label = { Text("Monthly", fontSize = 12.sp) }
                )
                FilterChip(
                    selected = selectedPeriodFilter == "WEEKLY",
                    onClick = { selectedPeriodFilter = "WEEKLY" },
                    label = { Text("Weekly", fontSize = 12.sp) }
                )
                FilterChip(
                    selected = selectedPeriodFilter == "CUSTOM",
                    onClick = { selectedPeriodFilter = "CUSTOM" },
                    label = { Text("Custom", fontSize = 12.sp) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        if (filteredBudgets.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.PieChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No budgets found",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Tap 'New Budget' to set limits and control your expenses!",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else {
            items(filteredBudgets, key = { it.id }) { budget ->
                val categorySpent = remember(state.transactions, budget) {
                    calculateSpent(budget, state.transactions)
                }

                val remaining = budget.monthlyLimit - categorySpent
                val progress = if (budget.monthlyLimit > 0) (categorySpent / budget.monthlyLimit).toFloat().coerceIn(0f, 1f) else 0f
                val isOver = categorySpent > budget.monthlyLimit
                val percentInt = (progress * 100).toInt()

                val periodLabel = when (budget.periodType) {
                    "WEEKLY" -> "Weekly Budget"
                    "YEARLY" -> "Yearly Budget"
                    "CUSTOM" -> if (budget.customPeriodName.isNotBlank()) budget.customPeriodName else "Custom Period"
                    else -> "Monthly Budget (${budget.monthYear})"
                }

                Card(
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .animateItem(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isOver) ExpenseRed.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = budget.categoryName,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = periodLabel,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                if (isOver) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = ExpenseRed,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Over budget by ${state.currencySymbol}${String.format(Locale.US, "%.2f", categorySpent - budget.monthlyLimit)}!",
                                            fontSize = 12.sp,
                                            color = ExpenseRed,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "${state.currencySymbol}${String.format(Locale.US, "%.2f", remaining)} left of ${state.currencySymbol}${String.format(Locale.US, "%.2f", budget.monthlyLimit)} limit",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        editingBudget = budget
                                        showAddBudgetModal = true
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit Budget",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { budgetToDelete = budget },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete Budget",
                                        tint = ExpenseRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Spent: ${state.currencySymbol}${String.format(Locale.US, "%.2f", categorySpent)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isOver) ExpenseRed else MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = "$percentInt% used",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    percentInt >= 100 -> ExpenseRed
                                    percentInt >= 80 -> GoldAccent
                                    else -> IncomeGreen
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = when {
                                progress >= 1.0f -> ExpenseRed
                                progress >= 0.8f -> GoldAccent
                                else -> IncomeGreen
                            },
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(28.dp))
        }

        // --- SECTION 2: SAVINGS GOALS ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Savings Goals",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Track target funds and dream milestones",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = {
                        editingGoal = null
                        showAddGoalModal = true
                    },
                    modifier = Modifier.testTag("add_goal_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Goal", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (state.savingsGoals.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = "No savings goals added yet. Click 'New Goal' to set target savings milestones!",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(state.savingsGoals, key = { it.id }) { goal ->
                val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
                val percentInt = (progress * 100).toInt()

                Card(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .animateItem(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Savings,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(goal.title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        "$percentInt% Achieved",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = { selectedGoalForDeposit = goal },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("+ Deposit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                IconButton(
                                    onClick = {
                                        editingGoal = goal
                                        showAddGoalModal = true
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit Goal",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { goalToDelete = goal },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete Goal",
                                        tint = ExpenseRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Saved: ${state.currencySymbol}${String.format(Locale.US, "%.2f", goal.currentAmount)}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Target: ${state.currencySymbol}${String.format(Locale.US, "%.2f", goal.targetAmount)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    }

    // --- MODALS & DIALOGS ---

    // Add or Edit Budget Modal
    if (showAddBudgetModal) {
        AddOrEditBudgetModal(
            categories = state.categories,
            existingBudget = editingBudget,
            currencySymbol = state.currencySymbol,
            onDismiss = {
                showAddBudgetModal = false
                editingBudget = null
            },
            onSave = { category, limit, periodType, customName ->
                onSaveBudget(
                    category,
                    limit,
                    periodType,
                    customName,
                    editingBudget?.id ?: 0L
                )
                showAddBudgetModal = false
                editingBudget = null
            }
        )
    }

    // Confirm Delete Budget
    budgetToDelete?.let { budget ->
        AlertDialog(
            onDismissRequest = { budgetToDelete = null },
            title = { Text("Delete Budget") },
            text = { Text("Are you sure you want to delete the budget for '${budget.categoryName}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteBudget(budget)
                        budgetToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { budgetToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add or Edit Savings Goal Modal
    if (showAddGoalModal) {
        AddOrEditGoalModal(
            currencySymbol = state.currencySymbol,
            existingGoal = editingGoal,
            onDismiss = {
                showAddGoalModal = false
                editingGoal = null
            },
            onSave = { title, target, current ->
                val goal = editingGoal?.copy(
                    title = title,
                    targetAmount = target,
                    currentAmount = current
                ) ?: SavingsGoalEntity(
                    title = title,
                    targetAmount = target,
                    currentAmount = current
                )
                onSaveSavingsGoal(goal)
                showAddGoalModal = false
                editingGoal = null
            }
        )
    }

    // Confirm Delete Goal
    goalToDelete?.let { goal ->
        AlertDialog(
            onDismissRequest = { goalToDelete = null },
            title = { Text("Delete Goal") },
            text = { Text("Are you sure you want to delete the goal '${goal.title}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSavingsGoal(goal)
                        goalToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { goalToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Deposit to Goal Modal
    selectedGoalForDeposit?.let { goal ->
        DepositGoalModal(
            goal = goal,
            currencySymbol = state.currencySymbol,
            onDismiss = { selectedGoalForDeposit = null },
            onDeposit = { amount ->
                onDepositToGoal(goal, amount)
                selectedGoalForDeposit = null
            }
        )
    }
}

@Composable
fun AddOrEditBudgetModal(
    categories: List<com.example.data.models.CategoryEntity>,
    existingBudget: BudgetEntity?,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (categoryName: String, limit: Double, periodType: String, customPeriodName: String) -> Unit
) {
    var selectedCat by remember {
        mutableStateOf(existingBudget?.categoryName ?: categories.firstOrNull()?.name ?: "Food & Dining")
    }
    var limitText by remember {
        mutableStateOf(existingBudget?.monthlyLimit?.let { if (it > 0) it.toString() else "" } ?: "")
    }
    var selectedPeriodType by remember {
        mutableStateOf(existingBudget?.periodType ?: "MONTHLY")
    }
    var customPeriodTitle by remember {
        mutableStateOf(existingBudget?.customPeriodName ?: "")
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = if (existingBudget == null) "Create Budget Limit" else "Edit Budget Limit",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Track and limit spending for a category",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Budget Limit Input
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = { Text("Budget Limit ($currencySymbol)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_limit_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Budgeting Period Type Selector
                Text("Budgeting Period", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("MONTHLY" to "Monthly", "WEEKLY" to "Weekly", "CUSTOM" to "Custom").forEach { (typeKey, label) ->
                        FilterChip(
                            selected = selectedPeriodType == typeKey,
                            onClick = { selectedPeriodType = typeKey },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }

                if (selectedPeriodType == "CUSTOM") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customPeriodTitle,
                        onValueChange = { customPeriodTitle = it },
                        label = { Text("Custom Period Title (e.g. Vacation, Q3)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Category Selection
                Text("Select Category", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))

                Column {
                    categories.take(6).forEach { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCat = cat.name }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedCat == cat.name,
                                onClick = { selectedCat = cat.name }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(cat.name, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val limit = limitText.toDoubleOrNull() ?: 0.0
                        if (limit > 0) {
                            onSave(selectedCat, limit, selectedPeriodType, customPeriodTitle)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_budget_modal_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (existingBudget == null) "Save Budget" else "Update Budget", fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun AddOrEditGoalModal(
    currencySymbol: String,
    existingGoal: SavingsGoalEntity?,
    onDismiss: () -> Unit,
    onSave: (title: String, targetAmount: Double, currentAmount: Double) -> Unit
) {
    var title by remember { mutableStateOf(existingGoal?.title ?: "") }
    var targetText by remember { mutableStateOf(existingGoal?.targetAmount?.toString() ?: "") }
    var currentText by remember { mutableStateOf(existingGoal?.currentAmount?.toString() ?: "0") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    if (existingGoal == null) "Create Savings Goal" else "Edit Savings Goal",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Goal Title (e.g. Emergency Fund, Trip)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("goal_title_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it },
                    label = { Text("Target Amount ($currencySymbol)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("goal_target_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = currentText,
                    onValueChange = { currentText = it },
                    label = { Text("Current Saved ($currencySymbol)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val target = targetText.toDoubleOrNull() ?: 0.0
                        val current = currentText.toDoubleOrNull() ?: 0.0
                        if (title.isNotBlank() && target > 0) onSave(title, target, current)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_goal_modal_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (existingGoal == null) "Create Goal" else "Update Goal", fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun DepositGoalModal(
    goal: SavingsGoalEntity,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onDeposit: (Double) -> Unit
) {
    var depositText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Deposit to ${goal.title}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Current Saved: $currencySymbol${String.format(Locale.US, "%.2f", goal.currentAmount)} / $currencySymbol${String.format(Locale.US, "%.2f", goal.targetAmount)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = depositText,
                    onValueChange = { depositText = it },
                    label = { Text("Deposit Amount ($currencySymbol)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("deposit_amount_input")
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val amount = depositText.toDoubleOrNull() ?: 0.0
                        if (amount > 0) onDeposit(amount)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("confirm_deposit_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Add Funds", fontSize = 15.sp)
                }
            }
        }
    }
}
