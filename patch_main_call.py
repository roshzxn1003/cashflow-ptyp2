import re

with open('/app/applet/app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

tx_screen_old = """                    1 -> TransactionsScreen(
                        familyMembers = familyMembers,
                        state = uiState,
                        onSearchQueryChange = { q -> viewModel.setSearchQuery(q) },
                        onFilterTypeChange = { type -> viewModel.setFilterType(type) },
                        onFilterCategoryChange = { cat -> viewModel.setFilterCategory(cat) },
                        onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) },
                        onOpenAddTransaction = { showAddDialog = true }
                    )"""
tx_screen_new = """                    1 -> TransactionsScreen(
                        familyMembers = familyMembers,
                        state = uiState,
                        onSearchQueryChange = { q -> viewModel.setSearchQuery(q) },
                        onFilterTypeChange = { type -> viewModel.setFilterType(type) },
                        onFilterCategoryChange = { cat -> viewModel.setFilterCategory(cat) },
                        onFilterMemberChange = { memberId -> viewModel.setFilterMember(memberId) },
                        onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) },
                        onOpenAddTransaction = { showAddDialog = true }
                    )"""
content = content.replace(tx_screen_old, tx_screen_new)

with open('/app/applet/app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
