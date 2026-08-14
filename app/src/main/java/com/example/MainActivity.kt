package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AddTransactionDialog
import com.example.ui.components.ReceiptScanModal
import com.example.ui.components.VoiceAiModal
import com.example.ui.components.FamilyMembersDialog
import com.example.ui.screens.*
import com.example.ui.theme.CashFlowTheme
import com.example.ui.viewmodel.CashFlowViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: CashFlowViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CashFlowTheme {
                CashFlowMainApp(viewModel = viewModel)
            }
        }
    }
}

enum class NavigationItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HOME("Home", Icons.Default.Home, Icons.Outlined.Home, "nav_home"),
    TRANSACTIONS("Activity", Icons.Default.ReceiptLong, Icons.Outlined.ReceiptLong, "nav_transactions"),
    BUDGETS("Budgets", Icons.Default.PieChart, Icons.Outlined.PieChart, "nav_budgets"),
    ANALYTICS("Analytics", Icons.Default.BarChart, Icons.Outlined.BarChart, "nav_analytics"),
    PROFILE("Profile", Icons.Default.Person, Icons.Outlined.Person, "nav_profile")
}

@Composable
fun CashFlowMainApp(viewModel: CashFlowViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentFinanceScope by viewModel.currentFinanceScope.collectAsStateWithLifecycle()
    val activeFamilyId by viewModel.activeFamilyId.collectAsStateWithLifecycle()
    val familyMembers by viewModel.familyMembers.collectAsStateWithLifecycle(initialValue = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var showFamilyMembersDialog by remember { mutableStateOf(false) }

    val totalIncome = remember(uiState.transactions) { viewModel.getTotalIncome(uiState.transactions) }
    val totalExpense = remember(uiState.transactions) { viewModel.getTotalExpense(uiState.transactions) }
    val netBalance = remember(uiState.transactions) { viewModel.getNetBalance(uiState.transactions) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            if (uiState.selectedTab == 0) {
                FloatingActionButton(
                    onClick = { viewModel.openVoiceDialog() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(imageVector = Icons.Default.Mic, contentDescription = "Voice Input")
                }
            }
        },
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationItem.entries.forEachIndexed { index, item ->
                    val isSelected = uiState.selectedTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.setTab(index) },
                        label = { Text(item.title, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title
                            )
                        },
                        modifier = Modifier.testTag(item.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = uiState.selectedTab,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally(animationSpec = tween(300)) { width -> width } + fadeIn(animationSpec = tween(300))).togetherWith(slideOutHorizontally(animationSpec = tween(300)) { width -> -width } + fadeOut(animationSpec = tween(300)))
                    } else {
                        (slideInHorizontally(animationSpec = tween(300)) { width -> -width } + fadeIn(animationSpec = tween(300))).togetherWith(slideOutHorizontally(animationSpec = tween(300)) { width -> width } + fadeOut(animationSpec = tween(300)))
                    }
                },
                label = "tab_transition"
            ) { targetTab ->
                when (targetTab) {
                    0 -> HomeScreen(
                        familyMembers = familyMembers,
                        state = uiState,
                        totalIncome = totalIncome,
                        totalExpense = totalExpense,
                        netBalance = netBalance,
                        currentFinanceScope = currentFinanceScope,
                        onChangeFinanceScope = { viewModel.setFinanceScope(it) },
                        activeFamilyId = activeFamilyId,
                        onOpenCreateFamily = { viewModel.createFamily("My Family") },
                        onManageMembers = { showFamilyMembersDialog = true },
                        onOpenAddTransaction = { showAddDialog = true },
                        onOpenVoiceAi = { viewModel.openVoiceDialog() },
                        onOpenReceiptScan = { viewModel.openReceiptDialog() },
                        onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) },
                        onNavigateToTransactions = { viewModel.setTab(1) }
                    )
                    1 -> TransactionsScreen(
                        familyMembers = familyMembers,
                        state = uiState,
                        onSearchQueryChange = { q -> viewModel.setSearchQuery(q) },
                        onFilterTypeChange = { type -> viewModel.setFilterType(type) },
                        onFilterCategoryChange = { cat -> viewModel.setFilterCategory(cat) },
                        onFilterMemberChange = { memberId -> viewModel.setFilterMember(memberId) },
                        onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) },
                        onOpenAddTransaction = { showAddDialog = true }
                    )
                    2 -> BudgetsAndGoalsScreen(
                        state = uiState,
                        onSaveBudget = { cat, limit, period, customName, id ->
                            viewModel.saveBudget(cat, limit, period, customName, id)
                        },
                        onDeleteBudget = { budget -> viewModel.deleteBudget(budget) },
                        onSaveSavingsGoal = { goal ->
                            viewModel.saveSavingsGoalEntity(goal)
                        },
                        onDeleteSavingsGoal = { goal -> viewModel.deleteSavingsGoal(goal) },
                        onDepositToGoal = { goal, amount -> viewModel.updateGoalDeposit(goal, amount) }
                    )
                    3 -> AnalyticsScreen(
                        state = uiState,
                        totalIncome = totalIncome,
                        totalExpense = totalExpense,
                        onGenerateAiAdvice = { inc, exp, cat -> viewModel.generateAiCoachAdvice(inc, exp, cat) },
                        currentFinanceScope = currentFinanceScope,
                        familyMembers = familyMembers
                    )
                    4 -> ProfileScreen(
                        state = uiState,
                        viewModel = viewModel,
                        onCurrencySelect = { curr -> viewModel.setCurrency(curr) },
                        onDeleteScannedItem = { item -> viewModel.deleteScannedItem(item) }
                    )
                }
            }
        }

        if (showFamilyMembersDialog && currentFinanceScope == com.example.data.models.FinanceScope.FAMILY) {
            FamilyMembersDialog(
                familyMembers = familyMembers,
                onDismiss = { showFamilyMembersDialog = false },
                onAddMember = { name, role -> viewModel.addFamilyMember(name, role) }
            )
        }

        // Global Modals
        if (showAddDialog) {
            AddTransactionDialog(
                categories = uiState.categories,
                currencySymbol = uiState.currencySymbol,
                onDismiss = { showAddDialog = false },
                onSave = { title, amount, type, category, pm, note ->
                    viewModel.addTransaction(title, amount, type, category, pm, note)
                }
            )
        }

        if (uiState.isVoiceDialogShowing) {
            VoiceAiModal(
                isProcessing = uiState.isVoiceProcessing,
                parsedExpense = uiState.parsedVoiceExpense,
                currencySymbol = uiState.currencySymbol,
                onDismiss = { viewModel.closeVoiceDialog() },
                onProcessPrompt = { prompt -> viewModel.processVoicePrompt(prompt) },
                onConfirmSave = { viewModel.confirmVoiceExpense() }
            )
        }

        if (uiState.isReceiptDialogShowing) {
            ReceiptScanModal(
                isProcessing = uiState.isReceiptProcessing,
                parsedReceipt = uiState.parsedReceipt,
                currencySymbol = uiState.currencySymbol,
                onDismiss = { viewModel.closeReceiptDialog() },
                onProcessReceipt = { rawText -> viewModel.processReceiptText(rawText) },
                onConfirmSave = { viewModel.confirmReceiptExpense() },
                onBarcodeScanned = { barcode ->
                    viewModel.closeReceiptDialog()
                    viewModel.openScannedBarcodeSheet(barcode)
                }
            )
        }

        val barcode = uiState.scannedBarcodeValue
        if (uiState.isScannedBarcodeSheetShowing && barcode != null) {
            ScannedItemBottomSheet(
                barcodeValue = barcode,
                onDismiss = { viewModel.closeScannedBarcodeSheet() },
                onAddToList = { productName ->
                    viewModel.addScannedItem(barcode, productName)
                    viewModel.closeScannedBarcodeSheet()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannedItemBottomSheet(
    barcodeValue: String,
    onDismiss: () -> Unit,
    onAddToList: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var productName by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text("Product Scanned", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Barcode: $barcodeValue", color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = productName,
                onValueChange = { productName = it },
                label = { Text("Product Name") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { onAddToList(if (productName.isBlank()) "Unknown Product" else productName) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add to List")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
