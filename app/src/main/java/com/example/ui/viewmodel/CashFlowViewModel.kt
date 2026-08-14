package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiAiService
import com.example.data.ai.ParsedReceipt
import com.example.data.ai.ParsedVoiceExpense
import com.example.data.database.CashFlowDatabase
import com.example.data.models.*
import com.example.data.repository.CashFlowRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.UUID
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class CashFlowUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val savingsGoals: List<SavingsGoalEntity> = emptyList(),
    val scannedItems: List<ScannedItemEntity> = emptyList(),
    val currencySymbol: String = "₹",
    val searchQuery: String = "",
    val selectedFilterType: TransactionType? = null,
    val selectedFilterCategory: String? = null,
    val selectedFilterMember: String? = null,
    val isVoiceDialogShowing: Boolean = false,
    val isVoiceProcessing: Boolean = false,
    val parsedVoiceExpense: ParsedVoiceExpense? = null,
    val isReceiptDialogShowing: Boolean = false,
    val isReceiptProcessing: Boolean = false,
    val parsedReceipt: ParsedReceipt? = null,
    val aiCoachAdvice: String? = null,
    val isAiCoachLoading: Boolean = false,
    val selectedTab: Int = 0,
    val scannedBarcodeValue: String? = null,
    val isScannedBarcodeSheetShowing: Boolean = false
)

class CashFlowViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CashFlowRepository

    private val _searchQuery = MutableStateFlow("")
    private val _filterType = MutableStateFlow<TransactionType?>(null)
    private val _filterCategory = MutableStateFlow<String?>(null)
    private val _filterMember = MutableStateFlow<String?>(null)
    private val _currencySymbol = MutableStateFlow("₹")
    private val _selectedTab = MutableStateFlow(0)

    private val _voiceDialogShowing = MutableStateFlow(false)
    private val _voiceProcessing = MutableStateFlow(false)
    private val _parsedVoice = MutableStateFlow<ParsedVoiceExpense?>(null)

    private val _receiptDialogShowing = MutableStateFlow(false)
    private val _receiptProcessing = MutableStateFlow(false)
    private val _parsedReceipt = MutableStateFlow<ParsedReceipt?>(null)

    private val _aiCoachAdvice = MutableStateFlow<String?>(null)
    private val _aiCoachLoading = MutableStateFlow(false)

    private val _scannedBarcodeValue = MutableStateFlow<String?>(null)
    private val _isScannedBarcodeSheetShowing = MutableStateFlow(false)

    private val syncEngine: com.example.data.network.SyncEngine
    private val userProfileRepository: com.example.data.repository.UserProfileRepository
    val authService = com.example.data.network.SupabaseAuthService()

    init {
        val database = CashFlowDatabase.getDatabase(application)
        repository = CashFlowRepository(
            database.transactionDao(),
            database.categoryDao(),
            database.budgetDao(),
            database.savingsGoalDao(),
            database.scannedItemDao(),
            database.familyDao(),
            database.familyMemberDao()
        )
        syncEngine = com.example.data.network.SyncEngine(
            database.transactionDao(),
            database.familyDao(),
            authService
        )

        userProfileRepository = com.example.data.repository.UserProfileRepository(database.userProfileDao())
        
        viewModelScope.launch {
            authService.restoreSession()
            authService.currentUser.collect { user ->
                if (user != null) {
                    userProfileRepository.syncProfile(user.id)
                    syncEngine.syncTransactions()
                }
            }
        }
    }

    val currentMonthYear: String
        get() {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            return sdf.format(Date())
        }


    private val _currentFinanceScope = MutableStateFlow(FinanceScope.PERSONAL)
    val currentFinanceScope: StateFlow<FinanceScope> = _currentFinanceScope.asStateFlow()

    private val _activeFamilyId = MutableStateFlow<String?>(null)
    val activeFamilyId: StateFlow<String?> = _activeFamilyId.asStateFlow()

    val currentUserId: String
        get() = authService.currentUser.value?.id ?: "local_user_1"
    val currentUserName: String
        get() = authService.currentUser.value?.email?.substringBefore("@") ?: "You"

    val activeFamily: Flow<FamilyEntity?> = _activeFamilyId.flatMapLatest { id ->
        if (id != null) flow { emit(repository.getFamilyById(id)) } else flowOf(null)
    }
    
    val userFamilies: Flow<List<FamilyEntity>> = authService.currentUser.flatMapLatest { user ->
        repository.getAllFamiliesForUser(user?.id ?: "local_user_1")
    }
    
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


    private val filterState: Flow<FilterState> = combine(
        _searchQuery,
        _filterType,
        _filterCategory,
        _filterMember
    ) { search, type, cat, member ->
        object { val s = search; val t = type; val c = cat; val m = member }
    }.combine(combine(_currencySymbol, _selectedTab, ::Pair)) { f1, f2 ->
        FilterState(f1.s, f1.t, f1.c, f1.m, f2.first, f2.second)
    }

    private val voiceState: Flow<VoiceState> = combine(
        _voiceDialogShowing,
        _voiceProcessing,
        _parsedVoice
    ) { voiceShow, voiceProc, voiceParsed ->
        VoiceState(voiceShow, voiceProc, voiceParsed)
    }

    private val aiState: Flow<AiState> = combine(
        _receiptDialogShowing,
        _receiptProcessing,
        _parsedReceipt,
        _aiCoachAdvice,
        _aiCoachLoading
    ) { receiptShow, receiptProc, receiptParsed, coachAdvice, coachLoading ->
        AiState(receiptShow, receiptProc, receiptParsed, coachAdvice, coachLoading)
    }

    private val scannedItemState: Flow<ScannedItemState> = combine(
        _scannedBarcodeValue,
        _isScannedBarcodeSheetShowing
    ) { barcodeValue, sheetShowing ->
        ScannedItemState(barcodeValue, sheetShowing)
    }

    val uiState: StateFlow<CashFlowUiState> = combine(
        baseDataState,
        filterState,
        voiceState,
        aiState,
        scannedItemState
    ) { base, filter, voice, ai, scannedItem ->
        val filteredTx = base.transactions.filter { tx ->
            val matchesSearch = filter.search.isBlank() ||
                    tx.title.contains(filter.search, ignoreCase = true) ||
                    tx.category.contains(filter.search, ignoreCase = true) ||
                    tx.note.contains(filter.search, ignoreCase = true)

            val matchesType = filter.type == null || tx.type == filter.type
            val matchesCat = filter.category == null || tx.category == filter.category

            matchesSearch && matchesType && matchesCat
        }

        CashFlowUiState(
            transactions = filteredTx,
            categories = base.categories,
            budgets = base.budgets,
            savingsGoals = base.savingsGoals,
            scannedItems = base.scannedItems,
            currencySymbol = filter.currency,
            searchQuery = filter.search,
            selectedFilterType = filter.type,
            selectedFilterCategory = filter.category,
            isVoiceDialogShowing = voice.voiceShow,
            isVoiceProcessing = voice.voiceProc,
            parsedVoiceExpense = voice.voiceParsed,
            isReceiptDialogShowing = ai.receiptShow,
            isReceiptProcessing = ai.receiptProc,
            parsedReceipt = ai.receiptParsed,
            aiCoachAdvice = ai.coachAdvice,
            isAiCoachLoading = ai.coachLoading,
            selectedTab = filter.tab,
            scannedBarcodeValue = scannedItem.barcodeValue,
            isScannedBarcodeSheetShowing = scannedItem.sheetShowing
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CashFlowUiState()
    )

    fun getTotalIncome(transactions: List<TransactionEntity>): Double {
        return transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }

    fun getTotalExpense(transactions: List<TransactionEntity>): Double {
        return transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }

    fun getNetBalance(transactions: List<TransactionEntity>): Double {
        return getTotalIncome(transactions) - getTotalExpense(transactions)
    }

    fun setTab(index: Int) {
        _selectedTab.value = index
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterType(type: TransactionType?) {
        _filterType.value = type
    }

    fun setFilterCategory(category: String?) {
        _filterCategory.value = category
    }

    fun setFilterMember(memberId: String?) {
        _filterMember.value = memberId
    }

    fun setCurrency(symbol: String) {
        _currencySymbol.value = symbol
    }


    val currentUser = authService.currentUser
    @OptIn(ExperimentalCoroutinesApi::class)
    val localUserProfile: StateFlow<UserProfileEntity?> = authService.currentUser.flatMapLatest { user ->
        if (user != null) {
            userProfileRepository.getProfileFlow(user.id)
        } else {
            flowOf(null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)


    fun signIn(email: String, pass: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = authService.signIn(email, pass)
            onResult(success)
        }
    }

    fun signUp(email: String, pass: String, name: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = authService.signUp(email, pass, name)
            onResult(success)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authService.signOut()
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            syncEngine.syncTransactions()
        }
    }

    fun addTransaction(
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
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun saveBudget(
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
    }

    fun saveBudgetEntity(budget: BudgetEntity) {
        viewModelScope.launch {
            repository.saveBudget(budget)
        }
    }

    fun deleteBudget(budget: BudgetEntity) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
        }
    }

    fun saveSavingsGoal(
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
    }

    fun saveSavingsGoalEntity(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            repository.saveSavingsGoal(goal)
        }
    }

    fun deleteSavingsGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(goal)
        }
    }

    fun updateGoalDeposit(goal: SavingsGoalEntity, addedAmount: Double) {
        viewModelScope.launch {
            val updated = goal.copy(currentAmount = goal.currentAmount + addedAmount)
            repository.saveSavingsGoal(updated)
        }
    }

    fun addCategory(name: String, colorHex: String) {
        viewModelScope.launch {
            repository.addCategory(
                CategoryEntity(
                    name = name,
                    iconName = "Star",
                    colorHex = colorHex,
                    type = TransactionType.EXPENSE,
                    isDefault = false
                )
            )
        }
    }


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

    fun openVoiceDialog() {
        _voiceDialogShowing.value = true
        _parsedVoice.value = null
    }

    fun closeVoiceDialog() {
        _voiceDialogShowing.value = false
        _voiceProcessing.value = false
        _parsedVoice.value = null
    }

    fun processVoicePrompt(promptText: String) {
        viewModelScope.launch {
            _voiceProcessing.value = true
            val parsed = GeminiAiService.parseVoiceCommand(promptText)
            _parsedVoice.value = parsed
            _voiceProcessing.value = false
        }
    }

    
    fun processAudioPrompt(audioBase64: String) {
        viewModelScope.launch {
            _voiceProcessing.value = true
            val parsed = GeminiAiService.parseAudioCommand(audioBase64)
            _parsedVoice.value = parsed
            _voiceProcessing.value = false
        }
    }

    fun confirmVoiceExpense() {
        val parsed = _parsedVoice.value ?: return
        addTransaction(
            title = parsed.title,
            amount = parsed.amount,
            type = parsed.type,
            category = parsed.category,
            paymentMethod = parsed.paymentMethod,
            note = parsed.note
        )
        closeVoiceDialog()
    }

    fun openReceiptDialog() {
        _receiptDialogShowing.value = true
        _parsedReceipt.value = null
    }

    fun closeReceiptDialog() {
        _receiptDialogShowing.value = false
        _receiptProcessing.value = false
        _parsedReceipt.value = null
    }

    fun processReceiptText(sampleReceiptText: String) {
        viewModelScope.launch {
            _receiptProcessing.value = true
            val parsed = GeminiAiService.parseReceiptOcr(sampleReceiptText)
            _parsedReceipt.value = parsed
            _receiptProcessing.value = false
        }
    }

    fun confirmReceiptExpense() {
        val parsed = _parsedReceipt.value ?: return
        addTransaction(
            title = parsed.merchantName,
            amount = parsed.totalAmount,
            type = TransactionType.EXPENSE,
            category = parsed.category,
            paymentMethod = "Credit Card",
            note = "Receipt OCR: ${parsed.itemsSummary}"
        )
        closeReceiptDialog()
    }

    fun generateAiCoachAdvice(totalIncome: Double, totalExpense: Double, topCat: String) {
        viewModelScope.launch {
            _aiCoachLoading.value = true
            val advice = GeminiAiService.getFinancialCoachAdvice(totalIncome, totalExpense, topCat)
            _aiCoachAdvice.value = advice
            _aiCoachLoading.value = false
        }
    }

    fun addScannedItem(barcodeValue: String, productName: String) {
        viewModelScope.launch {
            repository.addScannedItem(
                ScannedItemEntity(
                    barcodeValue = barcodeValue,
                    productName = productName
                )
            )
        }
    }

    fun deleteScannedItem(item: ScannedItemEntity) {
        viewModelScope.launch {
            repository.deleteScannedItem(item)
        }
    }

    fun openScannedBarcodeSheet(barcodeValue: String) {
        _scannedBarcodeValue.value = barcodeValue
        _isScannedBarcodeSheetShowing.value = true
    }

    fun closeScannedBarcodeSheet() {
        _isScannedBarcodeSheetShowing.value = false
        _scannedBarcodeValue.value = null
    }
}

private data class BaseData(
    val transactions: List<TransactionEntity>,
    val categories: List<CategoryEntity>,
    val budgets: List<BudgetEntity>,
    val savingsGoals: List<SavingsGoalEntity>,
    val scannedItems: List<ScannedItemEntity>
)

private data class FilterState(
    val search: String,
    val type: TransactionType?,
    val category: String?,
    val member: String?,
    val currency: String,
    val tab: Int
)

private data class VoiceState(
    val voiceShow: Boolean,
    val voiceProc: Boolean,
    val voiceParsed: ParsedVoiceExpense?
)

private data class AiState(
    val receiptShow: Boolean,
    val receiptProc: Boolean,
    val receiptParsed: ParsedReceipt?,
    val coachAdvice: String?,
    val coachLoading: Boolean
)

private data class ScannedItemState(
    val barcodeValue: String?,
    val sheetShowing: Boolean
)
