import re

with open('/app/applet/app/src/main/java/com/example/ui/viewmodel/CashFlowViewModel.kt', 'r') as f:
    content = f.read()

new_imports = """import kotlinx.coroutines.flow.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.UUID"""

content = re.sub(r'import kotlinx.coroutines.flow.\*', new_imports, content)

new_base = """
    private val _currentFinanceScope = MutableStateFlow(FinanceScope.PERSONAL)
    val currentFinanceScope: StateFlow<FinanceScope> = _currentFinanceScope.asStateFlow()

    private val _activeFamilyId = MutableStateFlow<String?>(null)
    val activeFamilyId: StateFlow<String?> = _activeFamilyId.asStateFlow()

    val currentUserId = "local_user_1"
    val currentUserName = "You"

    val activeFamily: Flow<FamilyEntity?> = _activeFamilyId.flatMapLatest { id ->
        if (id != null) flow { emit(repository.getFamilyById(id)) } else flowOf(null)
    }
    
    val userFamilies: Flow<List<FamilyEntity>> = repository.getAllFamiliesForUser(currentUserId)
    
    @OptIn(ExperimentalCoroutinesApi::class)
    val familyMembers: Flow<List<FamilyMemberEntity>> = _activeFamilyId.flatMapLatest { id ->
        if (id != null) repository.getMembersByFamilyId(id) else flowOf(emptyList())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val baseDataState: Flow<BaseData> = combine(
        _currentFinanceScope,
        _activeFamilyId
    ) { scope, familyId -> Pair(scope, familyId) }.flatMapLatest { (scope, familyId) ->
        val txFlow = if (scope == FinanceScope.FAMILY && familyId != null) {
            repository.getFamilyTransactions(familyId)
        } else {
            repository.getTransactionsByScope(FinanceScope.PERSONAL)
        }
        val budgetFlow = if (scope == FinanceScope.FAMILY && familyId != null) {
            repository.getBudgetsByFamilyId(familyId)
        } else {
            repository.getBudgetsByScope(FinanceScope.PERSONAL)
        }
        val goalFlow = if (scope == FinanceScope.FAMILY && familyId != null) {
            repository.getGoalsByFamilyId(familyId)
        } else {
            repository.getGoalsByScope(FinanceScope.PERSONAL)
        }

        combine(
            txFlow,
            repository.allCategories,
            budgetFlow,
            goalFlow,
            repository.allScannedItems
        ) { txList, catList, budgetList, goalList, scannedList ->
            BaseData(txList, catList, budgetList, goalList, scannedList)
        }
    }
"""

content = re.sub(r'    private val baseDataState: Flow<BaseData> = combine\(.*?BaseData\(txList, catList, budgetList, goalList, scannedList\)\n    }', new_base, content, flags=re.DOTALL)

with open('/app/applet/app/src/main/java/com/example/ui/viewmodel/CashFlowViewModel.kt', 'w') as f:
    f.write(content)
