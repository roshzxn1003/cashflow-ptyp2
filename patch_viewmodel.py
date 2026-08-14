import re

with open('/app/applet/app/src/main/java/com/example/ui/viewmodel/CashFlowViewModel.kt', 'r') as f:
    content = f.read()

# Replace currentUserId and currentUserName
content = re.sub(
    r'val currentUserId = "local_user_1"\n\s*val currentUserName = "You"',
    'val currentUserId: String\n        get() = authService.currentUser.value?.id ?: "local_user_1"\n    val currentUserName: String\n        get() = authService.currentUser.value?.email?.substringBefore("@") ?: "You"',
    content
)

# Replace userFamilies flow
content = re.sub(
    r'val userFamilies: Flow<List<FamilyEntity>> = repository\.getAllFamiliesForUser\(currentUserId\)',
    'val userFamilies: Flow<List<FamilyEntity>> = authService.currentUser.flatMapLatest { user ->\n        repository.getAllFamiliesForUser(user?.id ?: "local_user_1")\n    }',
    content
)

with open('/app/applet/app/src/main/java/com/example/ui/viewmodel/CashFlowViewModel.kt', 'w') as f:
    f.write(content)
