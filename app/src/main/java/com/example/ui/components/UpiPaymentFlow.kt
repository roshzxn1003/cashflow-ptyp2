package com.example.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.upi.UpiApp
import com.example.data.upi.UpiPaymentInfo
import com.example.data.upi.UpiService
import com.example.ui.theme.*
import java.util.Locale
import java.util.UUID

/**
 * Request to start a UPI payment. The parent form sets this after validation.
 */
data class UpiPayRequest(
    val amount: Double,
    val purpose: String,
    val vpa: String,
    val targetPackage: String? = null
)

/**
 * Shared "pay via UPI" flow used by every UPI entry point: app picker,
 * direct intent launch (such as Google Pay), and the explicit "Payment completed?" confirmation.
 * Never assumes success — [onSaveConfirmed] is only called after the user
 * confirms in the UPI app. The returned txn reference is passed through when
 * a supported UPI app echoes one back; otherwise null.
 */
@Composable
fun UpiPaymentFlow(
    payRequest: UpiPayRequest?,
    currencySymbol: String,
    onSaveConfirmed: (upiTransactionId: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showAppPicker by remember { mutableStateOf(false) }
    var showPaymentConfirm by remember { mutableStateOf(false) }
    var upiResultState by remember { mutableStateOf<com.example.data.upi.UpiIntentResult?>(null) }
    var returnedTxnRef by remember { mutableStateOf<String?>(null) }
    var userEditableTxnRef by remember { mutableStateOf("") }

    val upiApps = remember { UpiService.installedUpiApps(context) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val mapped = UpiService.mapResult(result.resultCode, result.data)
        android.util.Log.i("Zenith_UPI", "UPI Activity result mapped: $mapped")
        upiResultState = mapped
        returnedTxnRef = mapped.returnedTxnRef
        userEditableTxnRef = mapped.returnedTxnRef ?: ""
        // Always open confirmation so user can save or discard payment
        showPaymentConfirm = true
    }

    fun launchPayment(targetPackage: String?) {
        val request = payRequest ?: return
        val generatedRef = "ZNTH-" + UUID.randomUUID().toString().take(8).uppercase(Locale.US)
        val info = UpiPaymentInfo(
            payeeAddress = request.vpa.trim(),
            payeeName = request.purpose.trim().ifBlank { "Zenith Payee" },
            amount = String.format(Locale.US, "%.2f", request.amount),
            currency = "INR",
            note = request.purpose.trim().ifBlank { "Zenith Payment" },
            txnRef = generatedRef
        )
        returnedTxnRef = generatedRef
        userEditableTxnRef = generatedRef
        
        val intent = UpiService.buildPayIntent(info, targetPackage)
        if (intent == null) {
            Toast.makeText(context, "No UPI app found on this device.", Toast.LENGTH_SHORT).show()
            return
        }
        showAppPicker = false
        try {
            launcher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            android.util.Log.e("Zenith_UPI", "ActivityNotFoundException when launching UPI", e)
            Toast.makeText(context, "No UPI app available to handle this payment.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.util.Log.e("Zenith_UPI", "Error launching UPI intent", e)
            Toast.makeText(context, "Could not open UPI app: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(payRequest) {
        if (payRequest != null) {
            returnedTxnRef = null
            userEditableTxnRef = ""
            upiResultState = null
            if (!payRequest.targetPackage.isNullOrBlank()) {
                showAppPicker = false
                launchPayment(payRequest.targetPackage)
            } else {
                showAppPicker = true
            }
        }
    }

    // --- UPI APP PICKER ---
    if (showAppPicker) {
        val request = payRequest
        if (request != null) {
            Dialog(onDismissRequest = { 
                showAppPicker = false
                onDismiss()
            }) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SlateDarkSurface,
                    border = BorderStroke(1.dp, GlassBorderColor),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Pay with UPI",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateDarkTextPrimary
                        )
                        Text(
                            text = "Choose an installed UPI application to transfer money.",
                            fontSize = 12.sp,
                            color = SlateDarkTextSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        if (upiApps.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = GoalAmber.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, GoalAmber.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "No UPI app detected",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SlateDarkTextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Install Google Pay, PhonePe, Paytm, or BHIM. You can also record this payment manually.",
                                        fontSize = 11.sp,
                                        color = SlateDarkTextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                val store = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=upi%20payment"))
                                                try { context.startActivity(store) } catch (e: Exception) { }
                                            },
                                            modifier = Modifier.weight(1f).height(40.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4))
                                        ) {
                                            Text("Play Store", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                showAppPicker = false
                                                showPaymentConfirm = true
                                            },
                                            modifier = Modifier.weight(1f).height(40.dp),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Record Manually", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF06B6D4).copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, Color(0xFF06B6D4).copy(alpha = 0.45f)),
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { launchPayment(null) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color(0xFF06B6D4), modifier = Modifier.size(22.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "System UPI Chooser",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SlateDarkTextPrimary
                                            )
                                            Text("Open standard Android app selection tray", fontSize = 11.sp, color = SlateDarkTextSecondary)
                                        }
                                    }
                                }
                                upiApps.forEach { app: UpiApp ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = SlateDarkSurfaceVariant,
                                        border = BorderStroke(1.dp, GlassBorderColor),
                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { launchPayment(app.packageName) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = EmeraldDarkPrimary, modifier = Modifier.size(22.dp))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = app.label,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = SlateDarkTextPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- PAYMENT CONFIRMATION ---
    if (showPaymentConfirm) {
        val request = payRequest
        if (request != null) {
            val status = upiResultState?.status ?: "PENDING"
            val isExplicitSuccess = upiResultState?.isExplicitSuccess == true
            val isExplicitFailure = upiResultState?.isExplicitFailure == true

            Dialog(onDismissRequest = { }) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = SlateDarkSurface,
                    border = BorderStroke(1.dp, GlassBorderColor),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Status Badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                isExplicitSuccess -> IncomeGreen.copy(alpha = 0.2f)
                                isExplicitFailure -> ExpenseRed.copy(alpha = 0.2f)
                                else -> GoalAmber.copy(alpha = 0.2f)
                            },
                            border = BorderStroke(
                                1.dp,
                                when {
                                    isExplicitSuccess -> IncomeGreen.copy(alpha = 0.5f)
                                    isExplicitFailure -> ExpenseRed.copy(alpha = 0.5f)
                                    else -> GoalAmber.copy(alpha = 0.5f)
                                }
                            )
                        ) {
                            Text(
                                text = when {
                                    isExplicitSuccess -> "✓ UPI Payment Verified"
                                    isExplicitFailure -> "⚠️ UPI App Reported Failure"
                                    else -> "UPI Payment Handshake"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isExplicitSuccess -> IncomeGreen
                                    isExplicitFailure -> ExpenseRed
                                    else -> GoalAmber
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "$currencySymbol${String.format(Locale.US, "%.2f", request.amount)}",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isExplicitSuccess) IncomeGreen else Color(0xFF06B6D4)
                        )
                        if (request.purpose.isNotBlank()) {
                            Text(
                                text = request.purpose,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SlateDarkTextPrimary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        if (request.vpa.isNotBlank()) {
                            Text(
                                text = "To: ${request.vpa}",
                                fontSize = 12.sp,
                                color = SlateDarkTextSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Transaction ID / Reference
                        OutlinedTextField(
                            value = userEditableTxnRef,
                            onValueChange = { userEditableTxnRef = it },
                            label = { Text("UPI Txn ID / Ref", fontSize = 11.sp) },
                            placeholder = { Text("e.g. ZNTH-123456 or UTR") },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF06B6D4),
                                unfocusedBorderColor = GlassBorderColor,
                                focusedContainerColor = SlateDarkSurfaceVariant,
                                unfocusedContainerColor = SlateDarkSurfaceVariant,
                                focusedTextColor = SlateDarkTextPrimary,
                                unfocusedTextColor = SlateDarkTextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                showPaymentConfirm = false
                                onSaveConfirmed(userEditableTxnRef.ifBlank { returnedTxnRef })
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_upi_save"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldDarkPrimary)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Transaction to Zenith", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = {
                                showPaymentConfirm = false
                                showAppPicker = false
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Discard", fontSize = 13.sp, color = SlateDarkTextSecondary)
                        }
                    }
                }
            }
        }
    }
}
