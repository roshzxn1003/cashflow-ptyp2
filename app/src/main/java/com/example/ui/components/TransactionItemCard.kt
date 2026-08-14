package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.TransactionEntity
import com.example.data.models.TransactionType
import com.example.data.models.FinanceScope
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionItemCard(
    transaction: TransactionEntity,
    currencySymbol: String,
    onDelete: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier,
    creatorName: String? = null
) {
    val isExpense = transaction.type == TransactionType.EXPENSE
    val icon = getCategoryIcon(transaction.categoryIconName)
    val formattedDate = rememberFormattedDate(transaction.dateMillis)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .testTag("transaction_card_${transaction.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon background
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isExpense) ExpenseRed.copy(alpha = 0.15f) else IncomeGreen.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = transaction.category,
                    tint = if (isExpense) ExpenseRed else IncomeGreen,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transaction.category,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " • ${transaction.paymentMethod}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Amount and date
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isExpense) "-" else "+"}$currencySymbol${String.format(Locale.US, "%.2f", transaction.amount)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isExpense) ExpenseRed else IncomeGreen
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formattedDate,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { onDelete(transaction) },
                modifier = Modifier
                    .size(32.dp)
                    .testTag("delete_tx_${transaction.id}")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete transaction",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun rememberFormattedDate(dateMillis: Long): String {
    val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
    return sdf.format(Date(dateMillis))
}

fun getCategoryIcon(iconName: String): ImageVector {
    return when (iconName) {
        "Restaurant" -> Icons.Default.Restaurant
        "ShoppingBag" -> Icons.Default.ShoppingBag
        "Home" -> Icons.Default.Home
        "DirectionsCar" -> Icons.Default.DirectionsCar
        "Receipt" -> Icons.Default.Receipt
        "Movie" -> Icons.Default.Movie
        "MedicalServices" -> Icons.Default.MedicalServices
        "Payments" -> Icons.Default.Payments
        "Work" -> Icons.Default.Work
        "TrendingUp" -> Icons.Default.TrendingUp
        else -> Icons.Default.Category
    }
}
