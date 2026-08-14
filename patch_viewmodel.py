import re

with open('/app/applet/app/src/main/java/com/example/ui/viewmodel/CashFlowViewModel.kt', 'r') as f:
    content = f.read()

init_old = """    init {
        val database = CashFlowDatabase.getDatabase(application)
        repository = CashFlowRepository("""
        
init_new = """    private val syncEngine: com.example.data.network.SyncEngine
    val authService = com.example.data.network.SupabaseAuthService()

    init {
        val database = CashFlowDatabase.getDatabase(application)
        repository = CashFlowRepository("""

content = content.replace(init_old, init_new)

repo_init = """            database.familyDao(),
            database.familyMemberDao()
        )"""
        
sync_init = """            database.familyDao(),
            database.familyMemberDao()
        )
        syncEngine = com.example.data.network.SyncEngine(
            database.transactionDao(),
            database.familyDao(),
            authService
        )
        
        viewModelScope.launch {
            authService.restoreSession()
            authService.currentUser.collect { user ->
                if (user != null) {
                    syncEngine.syncTransactions()
                }
            }
        }"""
        
content = content.replace(repo_init, sync_init)

with open('/app/applet/app/src/main/java/com/example/ui/viewmodel/CashFlowViewModel.kt', 'w') as f:
    f.write(content)
