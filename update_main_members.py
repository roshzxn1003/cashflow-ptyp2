import re

with open('/app/applet/app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace("import com.example.ui.components.VoiceAiModal", "import com.example.ui.components.VoiceAiModal\nimport com.example.ui.components.FamilyMembersDialog")

states = """    val activeFamilyId by viewModel.activeFamilyId.collectAsStateWithLifecycle()
    val familyMembers by viewModel.familyMembers.collectAsStateWithLifecycle(initialValue = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var showFamilyMembersDialog by remember { mutableStateOf(false) }"""

content = content.replace("    val activeFamilyId by viewModel.activeFamilyId.collectAsStateWithLifecycle()\n    val familyMembers by viewModel.familyMembers.collectAsStateWithLifecycle(initialValue = emptyList())\n    var showAddDialog by remember { mutableStateOf(false) }", states)

homescreen_call_old = """                        onOpenCreateFamily = { viewModel.createFamily("My Family") },"""
homescreen_call_new = """                        onOpenCreateFamily = { viewModel.createFamily("My Family") },
                        onManageMembers = { showFamilyMembersDialog = true },"""
content = content.replace(homescreen_call_old, homescreen_call_new)

dialog_block = """        if (showFamilyMembersDialog && currentFinanceScope == com.example.data.models.FinanceScope.FAMILY) {
            FamilyMembersDialog(
                familyMembers = familyMembers,
                onDismiss = { showFamilyMembersDialog = false },
                onAddMember = { name, role -> viewModel.addFamilyMember(name, role) }
            )
        }

        // Global Modals"""
content = content.replace("        // Global Modals", dialog_block)

with open('/app/applet/app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
