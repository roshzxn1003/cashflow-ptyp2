import re

with open('/app/applet/app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'r') as f:
    content = f.read()

new_imports = """import com.example.data.models.FinanceScope
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButtonDefaults"""

content = content.replace("import com.example.data.models.TransactionType", new_imports + "\nimport com.example.data.models.TransactionType")

signature = """fun HomeScreen(
    state: CashFlowUiState,
    totalIncome: Double,
    totalExpense: Double,
    netBalance: Double,
    currentFinanceScope: FinanceScope,
    onChangeFinanceScope: (FinanceScope) -> Unit,
    activeFamilyId: String?,
    onOpenCreateFamily: () -> Unit,
    onOpenAddTransaction: () -> Unit,
    onOpenVoiceAi: () -> Unit,
    onOpenReceiptScan: () -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onNavigateToTransactions: () -> Unit
) {"""

content = re.sub(r'fun HomeScreen\([\s\S]*?\) \{', signature, content)

welcome_header = """        // Welcome Header
        item {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                SegmentedButton(
                    selected = currentFinanceScope == FinanceScope.PERSONAL,
                    onClick = { onChangeFinanceScope(FinanceScope.PERSONAL) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("Personal")
                }
                SegmentedButton(
                    selected = currentFinanceScope == FinanceScope.FAMILY,
                    onClick = { onChangeFinanceScope(FinanceScope.FAMILY) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Family")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (currentFinanceScope == FinanceScope.FAMILY && activeFamilyId == null) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("👨👩👧👦", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Family Finance",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Manage your family's income,\\nexpenses, budgets and savings\\ntogether.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = onOpenCreateFamily) {
                        Text("+ Create Family")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = { /* Join Family - Phase 3 */ }, enabled = false) {
                        Text("Join Family (Coming Soon)")
                    }
                }
            }
        } else {
            // Welcome Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (currentFinanceScope == FinanceScope.FAMILY) "Family Finance" else "Hello, Finance Master! 👋",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (currentFinanceScope == FinanceScope.FAMILY) "Family Overview" else "CashFlow Overview",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = "Wallet",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }"""

content = re.sub(r'        // Welcome Header\n        item \{\n            Row\([\s\S]*?Spacer\(modifier = Modifier.height\(16.dp\)\)\n        \}', welcome_header, content)

content = content.replace("        val recentTxs = state.transactions.take(5)", "        } // end of else block\n        val recentTxs = state.transactions.take(5)")

with open('/app/applet/app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'w') as f:
    f.write(content)
