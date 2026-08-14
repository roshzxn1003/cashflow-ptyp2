import re

with open('/app/applet/app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

# We need to collect active scope from viewModel
# val currentFinanceScope by viewModel.currentFinanceScope.collectAsStateWithLifecycle()
# val activeFamilyId by viewModel.activeFamilyId.collectAsStateWithLifecycle()
# val userFamilies by viewModel.userFamilies.collectAsStateWithLifecycle(initialValue = emptyList())

states = """    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentFinanceScope by viewModel.currentFinanceScope.collectAsStateWithLifecycle()
    val activeFamilyId by viewModel.activeFamilyId.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }"""

content = content.replace("    val uiState by viewModel.uiState.collectAsStateWithLifecycle()\n    var showAddDialog by remember { mutableStateOf(false) }", states)

homescreen_call = """                    0 -> HomeScreen(
                        state = uiState,
                        totalIncome = totalIncome,
                        totalExpense = totalExpense,
                        netBalance = netBalance,
                        currentFinanceScope = currentFinanceScope,
                        onChangeFinanceScope = { viewModel.setFinanceScope(it) },
                        activeFamilyId = activeFamilyId,
                        onOpenCreateFamily = { viewModel.createFamily("My Family") },
                        onOpenAddTransaction = { showAddDialog = true },
                        onOpenVoiceAi = { viewModel.openVoiceDialog() },
                        onOpenReceiptScan = { viewModel.openReceiptDialog() },
                        onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) },
                        onNavigateToTransactions = { viewModel.setTab(1) }
                    )"""

content = re.sub(r'                    0 -> HomeScreen\([\s\S]*?\n                    \)', homescreen_call, content)

with open('/app/applet/app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
