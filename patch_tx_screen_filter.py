import re

with open('/app/applet/app/src/main/java/com/example/ui/screens/TransactionsScreen.kt', 'r') as f:
    content = f.read()

sig = """fun TransactionsScreen(
    state: CashFlowUiState,
    onSearchQueryChange: (String) -> Unit,
    onFilterTypeChange: (TransactionType?) -> Unit,
    onFilterCategoryChange: (String?) -> Unit,
    onFilterMemberChange: (String?) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onOpenAddTransaction: () -> Unit,
    familyMembers: List<com.example.data.models.FamilyMemberEntity> = emptyList()
) {"""
content = re.sub(r'fun TransactionsScreen\([\s\S]*?familyMembers: List<com.example.data.models.FamilyMemberEntity> = emptyList\(\)\n\) \{', sig, content)

category_row_regex = r'        // Category Filter Horizontal Row.*?\}'
member_row = """        }

        if (familyMembers.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = state.selectedFilterMember == null,
                        onClick = { onFilterMemberChange(null) },
                        label = { Text("All Members", fontSize = 11.sp) }
                    )
                }
                items(familyMembers) { member ->
                    FilterChip(
                        selected = state.selectedFilterMember == member.userId,
                        onClick = { onFilterMemberChange(member.userId) },
                        label = { Text(member.name, fontSize = 11.sp) }
                    )
                }
            }
        }"""
content = re.sub(r'        \}\n\n        Spacer\(modifier = Modifier.height\(12.dp\)\)\n\n        // Transaction List', member_row + '\n\n        Spacer(modifier = Modifier.height(12.dp))\n\n        // Transaction List', content, flags=re.DOTALL)

with open('/app/applet/app/src/main/java/com/example/ui/screens/TransactionsScreen.kt', 'w') as f:
    f.write(content)
