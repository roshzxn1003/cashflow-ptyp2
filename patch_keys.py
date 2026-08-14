import os
import re

files_to_patch = [
    '/app/applet/app/src/main/java/com/example/ui/screens/BudgetsAndGoalsScreen.kt',
    '/app/applet/app/src/main/java/com/example/ui/screens/HomeScreen.kt',
    '/app/applet/app/src/main/java/com/example/ui/screens/TransactionsScreen.kt'
]

for file in files_to_patch:
    with open(file, 'r') as f:
        content = f.read()

    # BudgetsAndGoalsScreen
    content = content.replace('items(filteredBudgets, key = { it.id })', 'items(filteredBudgets, key = { "budget_${it.id}" })')
    content = content.replace('items(state.savingsGoals, key = { it.id })', 'items(state.savingsGoals, key = { "goal_${it.id}" })')

    # HomeScreen
    content = content.replace('items(recentTxs, key = { it.id })', 'items(recentTxs, key = { "tx_${it.id}" })')

    # TransactionsScreen
    content = content.replace('items(state.categories, key = { it.id })', 'items(state.categories, key = { "cat_${it.id}" })')
    content = content.replace('items(state.transactions, key = { it.id })', 'items(state.transactions, key = { "tx_${it.id}" })')

    with open(file, 'w') as f:
        f.write(content)
