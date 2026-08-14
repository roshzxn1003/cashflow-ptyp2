import re

with open('/app/applet/app/src/main/java/com/example/ui/viewmodel/CashFlowViewModel.kt', 'r') as f:
    content = f.read()

# Instantiate the repository and call syncProfile
sync_engine_init = """        syncEngine = com.example.data.network.SyncEngine(
            database.transactionDao(),
            database.familyDao(),
            authService
        )
"""
repo_init = sync_engine_init + "\n        val userProfileRepository = com.example.data.repository.UserProfileRepository(database.userProfileDao())\n"
content = content.replace(sync_engine_init, repo_init)

sync_call = """            authService.currentUser.collect { user ->
                if (user != null) {
                    syncEngine.syncTransactions()
                }
            }"""
sync_call_updated = """            authService.currentUser.collect { user ->
                if (user != null) {
                    userProfileRepository.syncProfile(user.id)
                    syncEngine.syncTransactions()
                }
            }"""
content = content.replace(sync_call, sync_call_updated)

with open('/app/applet/app/src/main/java/com/example/ui/viewmodel/CashFlowViewModel.kt', 'w') as f:
    f.write(content)
