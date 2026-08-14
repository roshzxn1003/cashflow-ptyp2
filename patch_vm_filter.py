import re

with open('/app/applet/app/src/main/java/com/example/ui/viewmodel/CashFlowViewModel.kt', 'r') as f:
    content = f.read()

# Add _filterMember
filter_decl = """    private val _filterCategory = MutableStateFlow<String?>(null)
    private val _filterMember = MutableStateFlow<String?>(null)"""
content = content.replace("    private val _filterCategory = MutableStateFlow<String?>(null)", filter_decl)

# Update FilterState
filter_state_old = """private data class FilterState(
    val search: String,
    val type: TransactionType?,
    val category: String?,
    val currency: String,
    val tab: Int
)"""
filter_state_new = """private data class FilterState(
    val search: String,
    val type: TransactionType?,
    val category: String?,
    val member: String?,
    val currency: String,
    val tab: Int
)"""
content = content.replace(filter_state_old, filter_state_new)

# Update combine call for uiState
combine_old = """            _filterCategory,
            _currencySymbol,
            _selectedTab
        ) { s, t, c, curr, tab -> FilterState(s, t, c, curr, tab) }"""
combine_new = """            _filterCategory,
            _filterMember,
            _currencySymbol,
            _selectedTab
        ) { s, t, c, m, curr, tab -> FilterState(s, t, c, m, curr, tab) }"""
content = content.replace(combine_old, combine_new)

# Update filtered logic
filtered_old = """            val filteredTxs = base.transactions.filter { tx ->
                val matchesSearch = tx.title.contains(filters.search, ignoreCase = true) ||
                        tx.category.contains(filters.search, ignoreCase = true) ||
                        tx.note.contains(filters.search, ignoreCase = true)
                val matchesType = filters.type == null || tx.type == filters.type
                val matchesCat = filters.category == null || tx.category == filters.category

                matchesSearch && matchesType && matchesCat
            }.sortedByDescending { it.dateMillis }"""

filtered_new = """            val filteredTxs = base.transactions.filter { tx ->
                val matchesSearch = tx.title.contains(filters.search, ignoreCase = true) ||
                        tx.category.contains(filters.search, ignoreCase = true) ||
                        tx.note.contains(filters.search, ignoreCase = true)
                val matchesType = filters.type == null || tx.type == filters.type
                val matchesCat = filters.category == null || tx.category == filters.category
                val matchesMember = filters.member == null || tx.createdByUserId == filters.member

                matchesSearch && matchesType && matchesCat && matchesMember
            }.sortedByDescending { it.dateMillis }"""
content = content.replace(filtered_old, filtered_new)

# Update UIState
ui_state_old = """    val selectedFilterCategory: String? = null,"""
ui_state_new = """    val selectedFilterCategory: String? = null,
    val selectedFilterMember: String? = null,"""
content = content.replace(ui_state_old, ui_state_new)

# Add setFilterMember
set_method = """    fun setFilterCategory(category: String?) {
        _filterCategory.value = category
    }

    fun setFilterMember(memberId: String?) {
        _filterMember.value = memberId
    }"""
content = content.replace("    fun setFilterCategory(category: String?) {\n        _filterCategory.value = category\n    }", set_method)

# Update CashFlowUiState constructor call
ui_state_cons_old = """                selectedFilterCategory = filters.category,"""
ui_state_cons_new = """                selectedFilterCategory = filters.category,
                selectedFilterMember = filters.member,"""
content = content.replace(ui_state_cons_old, ui_state_cons_new)

with open('/app/applet/app/src/main/java/com/example/ui/viewmodel/CashFlowViewModel.kt', 'w') as f:
    f.write(content)
