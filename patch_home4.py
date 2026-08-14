import re

with open('/app/applet/app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'r') as f:
    content = f.read()

sig = """fun HomeScreen(
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
    onNavigateToTransactions: () -> Unit,
    familyMembers: List<com.example.data.models.FamilyMemberEntity> = emptyList()
) {"""

content = re.sub(r'fun HomeScreen\([\s\S]*?\) \{', sig, content)

card = """                val creatorName = if (tx.financeScope == FinanceScope.FAMILY) familyMembers.find { it.userId == tx.createdByUserId }?.name else null
                TransactionItemCard(
                    transaction = tx,
                    currencySymbol = state.currencySymbol,
                    onDelete = onDeleteTransaction,
                    modifier = Modifier.animateItem(),
                    creatorName = creatorName
                )"""
content = re.sub(r'                TransactionItemCard\(\s*transaction = tx,\s*currencySymbol = state\.currencySymbol,\s*onDelete = onDeleteTransaction,\s*modifier = Modifier\.animateItem\(\)\s*\)', card, content)

with open('/app/applet/app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'w') as f:
    f.write(content)
