import re

with open('/app/applet/app/src/main/java/com/example/ui/viewmodel/CashFlowViewModel.kt', 'r') as f:
    content = f.read()

# Elevate userProfileRepository to a class property
content = content.replace(
    'private val syncEngine: com.example.data.network.SyncEngine',
    'private val syncEngine: com.example.data.network.SyncEngine\n    private val userProfileRepository: com.example.data.repository.UserProfileRepository'
)

# Fix init block
content = content.replace(
    'val userProfileRepository = com.example.data.repository.UserProfileRepository(database.userProfileDao())',
    'userProfileRepository = com.example.data.repository.UserProfileRepository(database.userProfileDao())'
)

# Add localUserProfile property
user_profile_flow = """
    @OptIn(ExperimentalCoroutinesApi::class)
    val localUserProfile: StateFlow<UserProfileEntity?> = authService.currentUser.flatMapLatest { user ->
        if (user != null) {
            userProfileRepository.getProfileFlow(user.id)
        } else {
            flowOf(null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
"""

content = content.replace(
    'val currentUser = authService.currentUser',
    'val currentUser = authService.currentUser' + user_profile_flow
)

with open('/app/applet/app/src/main/java/com/example/ui/viewmodel/CashFlowViewModel.kt', 'w') as f:
    f.write(content)
