package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.models.CategoryEntity
import com.example.data.models.TransactionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    categories: List<CategoryEntity>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Double, type: TransactionType, category: String, paymentMethod: String, note: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()?.name ?: "Food & Dining") }
    var selectedPaymentMethod by remember { mutableStateOf("Cash") }
    var note by remember { mutableStateOf("") }

    val paymentMethods = listOf("Cash", "Credit Card", "UPI", "Debit Card", "Bank Transfer")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Transaction",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_add_dialog")) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Expense / Income Segmented Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    Button(
                        onClick = { selectedType = TransactionType.EXPENSE },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == TransactionType.EXPENSE) MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (selectedType == TransactionType.EXPENSE) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("type_expense_button")
                    ) {
                        Text("Expense", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = { selectedType = TransactionType.INCOME },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == TransactionType.INCOME) MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (selectedType == TransactionType.INCOME) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("type_income_button")
                    ) {
                        Text("Income", fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Amount Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount ($currencySymbol)") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("amount_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Title Field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title / Merchant") },
                    placeholder = { Text("e.g., Target, Starbucks, Salary") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("title_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category Selector Chips
                Text(
                    text = "Category",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column {
                    val filteredCats = categories.filter { it.type == selectedType || it.type == TransactionType.EXPENSE }
                    filteredCats.chunked(2).forEach { rowCats ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            rowCats.forEach { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat.name,
                                    onClick = { selectedCategory = cat.name },
                                    label = { Text(cat.name, fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Payment Method Chips
                Text(
                    text = "Payment Method",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    paymentMethods.take(3).forEach { pm ->
                        FilterChip(
                            selected = selectedPaymentMethod == pm,
                            onClick = { selectedPaymentMethod = pm },
                            label = { Text(pm, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Note Field
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Save Button
                Button(
                    onClick = {
                        val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
                        if (parsedAmount > 0 && title.isNotBlank()) {
                            onSave(title, parsedAmount, selectedType, selectedCategory, selectedPaymentMethod, note)
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_transaction_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Transaction", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
