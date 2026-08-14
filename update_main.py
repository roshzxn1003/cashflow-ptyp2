import re

with open('/app/applet/app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

states = """    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentFinanceScope by viewModel.currentFinanceScope.collectAsStateWithLifecycle()
    val activeFamilyId by viewModel.activeFamilyId.collectAsStateWithLifecycle()
    val familyMembers by viewModel.familyMembers.collectAsStateWithLifecycle(initialValue = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }"""

content = content.replace("    val uiState by viewModel.uiState.collectAsStateWithLifecycle()\n    val currentFinanceScope by viewModel.currentFinanceScope.collectAsStateWithLifecycle()\n    val activeFamilyId by viewModel.activeFamilyId.collectAsStateWithLifecycle()\n    var showAddDialog by remember { mutableStateOf(false) }", states)

content = content.replace("                    0 -> HomeScreen(", "                    0 -> HomeScreen(\n                        familyMembers = familyMembers,")
content = content.replace("                    1 -> TransactionsScreen(", "                    1 -> TransactionsScreen(\n                        familyMembers = familyMembers,")

with open('/app/applet/app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
