import re

with open('/app/applet/app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

ana_old = """                    3 -> AnalyticsScreen(
                        state = uiState,
                        totalIncome = totalIncome,
                        totalExpense = totalExpense,
                        onGenerateAiAdvice = { inc, exp, cat -> viewModel.generateAiCoachAdvice(inc, exp, cat) }
                    )"""
ana_new = """                    3 -> AnalyticsScreen(
                        state = uiState,
                        totalIncome = totalIncome,
                        totalExpense = totalExpense,
                        onGenerateAiAdvice = { inc, exp, cat -> viewModel.generateAiCoachAdvice(inc, exp, cat) },
                        currentFinanceScope = currentFinanceScope,
                        familyMembers = familyMembers
                    )"""

content = content.replace(ana_old, ana_new)

with open('/app/applet/app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
