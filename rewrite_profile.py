import re

with open('/app/applet/app/src/main/java/com/example/ui/screens/ProfileScreen.kt', 'r') as f:
    content = f.read()

# I will just write a completely new file for ProfileScreen.kt to be robust.
new_content = """package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.CashFlowViewModel
import io.github.jan.supabase.auth.user.UserInfo
import com.example.data.models.UserProfileEntity
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    state: com.example.ui.viewmodel.CashFlowUiState,
    viewModel: CashFlowViewModel,
    onCurrencySelect: (String) -> Unit,
    onDeleteScannedItem: (com.example.data.models.ScannedItemEntity) -> Unit
) {
    val context = LocalContext.current
    val currencies = listOf("$", "₹", "€", "£", "¥")
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle(initialValue = null)
    val localUserProfile by viewModel.localUserProfile.collectAsStateWithLifecycle(initialValue = null)
    var showAuthDialog by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // It's loading if we have a user logged in, but the local profile hasn't been emitted yet
    val isProfileLoading = currentUser != null && localUserProfile == null

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
    ) {
        item {
            UserProfileCard(
                currentUser = currentUser,
                userProfile = localUserProfile,
                isLoading = isProfileLoading,
                onAuthAction = {
                    if (currentUser == null) showAuthDialog = true else viewModel.signOut()
                }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Text(
                text = "Preferences",
                style = MaterialTheme.typography.titleMedium,                
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Currency Symbol", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        currencies.forEach { curr ->
                            val isSelected = state.currencySymbol == curr
                            FilterChip(
                                selected = isSelected,
                                onClick = { onCurrencySelect(curr) },
                                label = { Text(curr) }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            if (currentUser != null) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isSyncing = true
                            viewModel.syncNow()
                            isSyncing = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isSyncing
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Syncing...")
                    } else {
                        Icon(Icons.Default.Sync, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sync Now")
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showAuthDialog) {
        AuthDialog(
            onDismiss = { showAuthDialog = false },
            onSignIn = { email, pass ->
                viewModel.signIn(email, pass) { success ->
                    if (success) {
                        showAuthDialog = false
                    } else {
                        Toast.makeText(context, "Sign in failed. Check credentials.", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onSignUp = { email, pass, name ->
                viewModel.signUp(email, pass, name) { success ->
                    if (success) {
                        showAuthDialog = false
                    } else {
                        Toast.makeText(context, "Sign up failed. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

@Composable
fun UserProfileCard(
    currentUser: UserInfo?, 
    userProfile: UserProfileEntity?, 
    isLoading: Boolean, 
    onAuthAction: () -> Unit
) {
    val initial = userProfile?.fullName?.firstOrNull()?.uppercase() 
        ?: currentUser?.email?.firstOrNull()?.uppercase() 
        ?: "?"
        
    val displayName = userProfile?.fullName 
        ?: currentUser?.email 
        ?: "Not logged in"
        
    val statusText = if (currentUser != null) "Account active" else "Local mode (Guest)"

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().testTag("profile_header_card"),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLoading) {
                    // Loading Avatar
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        // Loading text placeholders
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height(20.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.4f)
                                .height(16.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                        )
                    }
                } else {
                    // Actual Profile Content
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onAuthAction,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text(
                    text = if (currentUser == null) "Sign In or Create Account" else "Sign Out",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun AuthDialog(
    onDismiss: () -> Unit,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String, String) -> Unit
) {
    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    fun validateAndSubmit() {
        errorText = null
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errorText = "Please enter a valid email address."
            return
        }
        if (password.length < 6) {
            errorText = "Password must be at least 6 characters."
            return
        }
        if (isSignUp && name.isBlank()) {
            errorText = "Please enter your full name."
            return
        }

        isLoading = true
        if (isSignUp) {
            onSignUp(email, password, name)
        } else {
            onSignIn(email, password)
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text(
                text = if (isSignUp) "Create Account" else "Sign In",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                if (errorText != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorText!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (isSignUp) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isLoading
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = {
                        errorText = null
                        isSignUp = !isSignUp
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isSignUp) "Already have an account? Sign In" else "Don't have an account? Sign Up")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { validateAndSubmit() },
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (isSignUp) "Sign Up" else "Sign In")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
"""

with open('/app/applet/app/src/main/java/com/example/ui/screens/ProfileScreen.kt', 'w') as f:
    f.write(new_content)
