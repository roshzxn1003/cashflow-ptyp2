package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.TransactionType
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.CashFlowUiState
import java.util.*

@Composable
fun AnalyticsScreen(
    state: CashFlowUiState,
    totalIncome: Double,
    totalExpense: Double,
    onGenerateAiAdvice: (income: Double, expense: Double, topCat: String) -> Unit,
    currentFinanceScope: com.example.data.models.FinanceScope = com.example.data.models.FinanceScope.PERSONAL,
    familyMembers: List<com.example.data.models.FamilyMemberEntity> = emptyList()
) {
    // Group expenses by category
    val expensesByCategory = remember(state.transactions) {
        state.transactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    val topCategory = remember(expensesByCategory) {
        expensesByCategory.maxByOrNull { it.value }?.key ?: "Food & Dining"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
    ) {
        // Title Header
        item {
            Text(
                text = "Analytics & Reports",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Spending breakdown and AI insights",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Section 1: Custom Bar Chart (Monthly Income vs Expense)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().testTag("income_vs_expense_chart_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Income vs. Expenses",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val maxVal = maxOf(totalIncome, totalExpense, 1.0)

                        // Income Bar
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            val incomeHeightRatio = (totalIncome / maxVal).toFloat().coerceIn(0.1f, 1.0f)
                            Text(
                                text = "${state.currencySymbol}${totalIncome.toInt()}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(48.dp)
                                    .fillMaxHeight(incomeHeightRatio)
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    .background(IncomeGreen)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Income", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }

                        // Expense Bar
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            val expenseHeightRatio = (totalExpense / maxVal).toFloat().coerceIn(0.1f, 1.0f)
                            Text(
                                text = "${state.currencySymbol}${totalExpense.toInt()}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(48.dp)
                                    .fillMaxHeight(expenseHeightRatio)
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    .background(ExpenseRed)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Expenses", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Section 2: Category Breakdown Donut Chart
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().testTag("category_breakdown_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Expense Categories",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (expensesByCategory.isEmpty()) {
                        Text(
                            text = "No expenses logged to display chart.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val palette = listOf(
                            ExpenseRed, GoldAccent, Color(0xFF3B82F6),
                            Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF10B981)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Donut Canvas
                            val totalCatExpense = expensesByCategory.values.sum().coerceAtLeast(1.0)

                            Canvas(modifier = Modifier.size(130.dp)) {
                                var startAngle = -90f
                                expensesByCategory.entries.forEachIndexed { idx, entry ->
                                    val sweep = ((entry.value / totalCatExpense) * 360f).toFloat()
                                    val color = palette[idx % palette.size]

                                    drawArc(
                                        color = color,
                                        startAngle = startAngle,
                                        sweepAngle = sweep,
                                        useCenter = false,
                                        style = Stroke(width = 24.dp.toPx())
                                    )
                                    startAngle += sweep
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Legend List
                            Column(modifier = Modifier.weight(1f)) {
                                expensesByCategory.entries.take(4).forEachIndexed { idx, entry ->
                                    val color = palette[idx % palette.size]
                                    val percent = ((entry.value / totalCatExpense) * 100).toInt()

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 3.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = entry.key,
                                            fontSize = 12.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "$percent%",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Section 3: Gemini Financial Coach Box
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth().testTag("ai_financial_coach_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Gemini Financial Coach",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Button(
                            onClick = { onGenerateAiAdvice(totalIncome, totalExpense, topCategory) },
                            modifier = Modifier.testTag("ask_ai_coach_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Analyze", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (state.isAiCoachLoading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    } else if (state.aiCoachAdvice != null) {
                        Text(
                            text = state.aiCoachAdvice,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Text(
                            text = "Tap 'Analyze' to generate AI financial tips based on your real spending metrics!",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}
