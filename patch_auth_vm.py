import re

with open('/app/applet/app/src/main/java/com/example/ui/viewmodel/CashFlowViewModel.kt', 'r') as f:
    content = f.read()

auth_methods = """
    val currentUser = authService.currentUser

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
"""

content = content.replace("    fun addTransaction(", auth_methods + "\n    fun addTransaction(")

with open('/app/applet/app/src/main/java/com/example/ui/viewmodel/CashFlowViewModel.kt', 'w') as f:
    f.write(content)
