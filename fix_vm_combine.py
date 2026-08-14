import re

with open('/app/applet/app/src/main/java/com/example/ui/viewmodel/CashFlowViewModel.kt', 'r') as f:
    content = f.read()

# We have 5 flows combined currently, plus one new flow _filterMember.
# Kotlin combine supports up to 5 flows. We can combine _filterType, _filterCategory, _filterMember into a Pair or Triple or just use array combine.
# Better yet, just use a state class for all filters!
# But for now, let's just do: combine(combine(_searchQuery, _filterType, _filterCategory, _filterMember) { ... }, _currencySymbol, _selectedTab)
# Wait, let's just use the varargs combine: `combine(flow1, flow2, flow3...) { args -> }` 

combine_block_old = """    private val filterState: Flow<FilterState> = combine(
        _searchQuery,
        _filterType,
        _filterCategory,
        _currencySymbol,
        _selectedTab
    ) { search, type, cat, curr, tab ->
        FilterState(search, type, cat, curr, tab)
    }"""

combine_block_new = """    private val filterState: Flow<FilterState> = combine(
        _searchQuery,
        _filterType,
        _filterCategory,
        _filterMember
    ) { search, type, cat, member ->
        object { val s = search; val t = type; val c = cat; val m = member }
    }.combine(combine(_currencySymbol, _selectedTab, ::Pair)) { f1, f2 ->
        FilterState(f1.s, f1.t, f1.c, f1.m, f2.first, f2.second)
    }"""

content = content.replace(combine_block_old, combine_block_new)

with open('/app/applet/app/src/main/java/com/example/ui/viewmodel/CashFlowViewModel.kt', 'w') as f:
    f.write(content)
