import re

with open('/app/applet/app/src/main/java/com/example/ui/viewmodel/CashFlowViewModel.kt', 'r') as f:
    content = f.read()

add_tx = """    fun addTransaction(
        title: String,
        amount: Double,
        type: TransactionType,
        category: String,
        paymentMethod: String,
        note: String
    ) {
        viewModelScope.launch {
            val iconName = when (category) {
                "Food & Dining" -> "Restaurant"
                "Shopping" -> "ShoppingBag"
                "Housing & Rent" -> "Home"
                "Transportation" -> "DirectionsCar"
                "Bills & Utilities" -> "Receipt"
                "Entertainment" -> "Movie"
                "Healthcare" -> "MedicalServices"
                "Salary & Income" -> "Payments"
                "Freelance / Business" -> "Work"
                else -> "Category"
            }
            repository.addTransaction(
                TransactionEntity(
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    categoryIconName = iconName,
                    paymentMethod = paymentMethod,
                    note = note,
                    financeScope = _currentFinanceScope.value,
                    familyId = if (_currentFinanceScope.value == FinanceScope.FAMILY) _activeFamilyId.value else null,
                    createdByUserId = if (_currentFinanceScope.value == FinanceScope.FAMILY) currentUserId else null
                )
            )
        }
    }"""
content = re.sub(r'    fun addTransaction\([\s\S]*?TransactionEntity\([\s\S]*?\n                \)\n            \)\n        }\n    }', add_tx, content)

save_budget = """    fun saveBudget(
        categoryName: String,
        limit: Double,
        periodType: String = "MONTHLY",
        customPeriodName: String = "",
        budgetId: Long = 0
    ) {
        viewModelScope.launch {
            repository.saveBudget(
                BudgetEntity(
                    id = budgetId,
                    categoryName = categoryName,
                    monthlyLimit = limit,
                    monthYear = currentMonthYear,
                    periodType = periodType,
                    customPeriodName = customPeriodName,
                    financeScope = _currentFinanceScope.value,
                    familyId = if (_currentFinanceScope.value == FinanceScope.FAMILY) _activeFamilyId.value else null
                )
            )
        }
    }"""
content = re.sub(r'    fun saveBudget\([\s\S]*?BudgetEntity\([\s\S]*?\n                \)\n            \)\n        }\n    }', save_budget, content)

save_goal = """    fun saveSavingsGoal(
        title: String,
        targetAmount: Double,
        currentAmount: Double,
        goalId: Long = 0
    ) {
        viewModelScope.launch {
            repository.saveSavingsGoal(
                SavingsGoalEntity(
                    id = goalId,
                    title = title,
                    targetAmount = targetAmount,
                    currentAmount = currentAmount,
                    financeScope = _currentFinanceScope.value,
                    familyId = if (_currentFinanceScope.value == FinanceScope.FAMILY) _activeFamilyId.value else null
                )
            )
        }
    }"""
content = re.sub(r'    fun saveSavingsGoal\([\s\S]*?SavingsGoalEntity\([\s\S]*?\n                \)\n            \)\n        }\n    }', save_goal, content)

family_methods = """
    fun setFinanceScope(scope: FinanceScope) {
        _currentFinanceScope.value = scope
    }

    fun setActiveFamily(familyId: String?) {
        _activeFamilyId.value = familyId
    }

    fun createFamily(name: String) {
        viewModelScope.launch {
            val newFamilyId = UUID.randomUUID().toString()
            val family = FamilyEntity(
                id = newFamilyId,
                name = name,
                createdByUserId = currentUserId,
                createdAt = System.currentTimeMillis()
            )
            repository.insertFamily(family)
            
            val member = FamilyMemberEntity(
                id = UUID.randomUUID().toString(),
                familyId = newFamilyId,
                userId = currentUserId,
                name = currentUserName,
                role = FamilyRole.ADMIN,
                joinedAt = System.currentTimeMillis()
            )
            repository.insertMember(member)
            
            setActiveFamily(newFamilyId)
            setFinanceScope(FinanceScope.FAMILY)
        }
    }

    fun addFamilyMember(name: String, role: FamilyRole) {
        viewModelScope.launch {
            val fId = _activeFamilyId.value ?: return@launch
            val member = FamilyMemberEntity(
                id = UUID.randomUUID().toString(),
                familyId = fId,
                userId = UUID.randomUUID().toString(), // Mock user ID for new member locally
                name = name,
                role = role,
                joinedAt = System.currentTimeMillis()
            )
            repository.insertMember(member)
        }
    }

    fun openVoiceDialog() {"""
content = content.replace("    fun openVoiceDialog() {", family_methods)

with open('/app/applet/app/src/main/java/com/example/ui/viewmodel/CashFlowViewModel.kt', 'w') as f:
    f.write(content)
