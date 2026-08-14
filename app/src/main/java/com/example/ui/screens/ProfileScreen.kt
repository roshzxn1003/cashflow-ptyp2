package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.CashFlowUiState
import com.example.ui.viewmodel.CashFlowViewModel

@Composable
fun ProfileScreen(
    state: CashFlowUiState,
    viewModel: CashFlowViewModel,
    onCurrencySelect: (String) -> Unit,
    onDeleteScannedItem: (com.example.data.models.ScannedItemEntity) -> Unit
) {
    val context = LocalContext.current
    val currencies = listOf("$", "₹", "€", "£", "¥")
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle(initialValue = null)

    var showAuthDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
    ) {
        // User Profile Header Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().testTag("profile_header_card")
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (currentUser != null) currentUser!!.email?.firstOrNull()?.uppercase() ?: "A" else "A",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (currentUser != null) currentUser!!.email ?: "User" else "Guest User",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (currentUser != null) "Logged in" else "Not logged in",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                
                if (currentUser == null) {
                    TextButton(
                        onClick = { showAuthDialog = true },
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    ) {
                        Text("Sign In or Create Account")
                    }
                } else {
                    TextButton(
                        onClick = { viewModel.signOut() },
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    ) {
                        Text("Sign Out")
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Currencies
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
        
        // Sync Data
        item {
            if (currentUser != null) {
                Button(
                    onClick = { viewModel.syncNow() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sync Now")
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
                        Toast.makeText(context, "Sign in failed", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onSignUp = { email, pass, name ->
                viewModel.signUp(email, pass, name) { success ->
                    if (success) {
                        showAuthDialog = false
                    } else {
                        Toast.makeText(context, "Sign up failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isSignUp) "Create Account" else "Sign In") },
        text = {
            Column {
                if (isSignUp) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { isSignUp = !isSignUp }) {
                    Text(if (isSignUp) "Already have an account? Sign In" else "Don't have an account? Sign Up")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (isSignUp) {
                    onSignUp(email, password, name)
                } else {
                    onSignIn(email, password)
                }
            }) {
                Text(if (isSignUp) "Sign Up" else "Sign In")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
