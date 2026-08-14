import re

with open('/app/applet/app/src/main/java/com/example/ui/screens/TransactionsScreen.kt', 'r') as f:
    content = f.read()

sig = """fun TransactionsScreen(
    state: CashFlowUiState,
    onSearchQueryChange: (String) -> Unit,
    onFilterTypeChange: (TransactionType?) -> Unit,
    onFilterCategoryChange: (String?) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onOpenAddTransaction: () -> Unit,
    familyMembers: List<com.example.data.models.FamilyMemberEntity> = emptyList()
) {"""

content = re.sub(r'fun TransactionsScreen\([\s\S]*?onOpenAddTransaction: \(\) -> Unit\n\) \{', sig, content)

card = """                        val creatorName = if (tx.financeScope == com.example.data.models.FinanceScope.FAMILY) familyMembers.find { it.userId == tx.createdByUserId }?.name else null
                        TransactionItemCard(
                            transaction = tx,
                            currencySymbol = state.currencySymbol,
                            onDelete = onDeleteTransaction,
                            modifier = Modifier.animateItem(),
                            creatorName = creatorName
                        )"""
content = re.sub(r'                        TransactionItemCard\(\s*transaction = tx,\s*currencySymbol = state\.currencySymbol,\s*onDelete = onDeleteTransaction,\s*modifier = Modifier\.animateItem\(\)\s*\)', card, content)

with open('/app/applet/app/src/main/java/com/example/ui/screens/TransactionsScreen.kt', 'w') as f:
    f.write(content)
