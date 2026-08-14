import re

with open('/app/applet/app/src/main/java/com/example/ui/screens/AnalyticsScreen.kt', 'r') as f:
    content = f.read()

sig = """fun AnalyticsScreen(
    state: CashFlowUiState,
    totalIncome: Double,
    totalExpense: Double,
    onGenerateAiAdvice: (income: Double, expense: Double, topCat: String) -> Unit,
    currentFinanceScope: com.example.data.models.FinanceScope = com.example.data.models.FinanceScope.PERSONAL,
    familyMembers: List<com.example.data.models.FamilyMemberEntity> = emptyList()
) {"""

content = re.sub(r'fun AnalyticsScreen\([\s\S]*?onGenerateAiAdvice: \([\s\S]*?-> Unit\n\) \{', sig, content)

content = content.replace("                // List of Categories", """                if (currentFinanceScope == com.example.data.models.FinanceScope.FAMILY && familyMembers.isNotEmpty()) {
                    val spendingByMember = state.transactions
                        .filter { it.type == TransactionType.EXPENSE }
                        .groupBy { it.createdByUserId }
                        .mapValues { entry -> entry.value.sumOf { it.amount } }
                        .toList()
                        .sortedByDescending { it.second }
                    
                    if (spendingByMember.isNotEmpty()) {
                        Text("Spending by Member", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        spendingByMember.forEach { (userId, amount) ->
                            val memberName = familyMembers.find { it.userId == userId }?.name ?: "Unknown"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = memberName, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                }
                                Text(
                                    text = "${state.currencySymbol}$amount",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed
                                )
                            }
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
                
                // List of Categories""")

with open('/app/applet/app/src/main/java/com/example/ui/screens/AnalyticsScreen.kt', 'w') as f:
    f.write(content)
