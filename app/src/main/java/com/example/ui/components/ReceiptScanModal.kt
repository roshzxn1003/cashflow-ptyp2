package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.example.data.ai.ParsedReceipt
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.launch

@Composable
fun ReceiptScanModal(
    isProcessing: Boolean,
    parsedReceipt: ParsedReceipt?,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onProcessReceipt: (String) -> Unit,
    onConfirmSave: () -> Unit,
    onBarcodeScanned: (String) -> Unit
) {
    var rawReceiptInput by remember {
        mutableStateOf(
            "TARGO SUPERSTORE #4102\nDate: 07/26/2026 Time: 14:22\nOrganic Milk 1Gal     $5.49\nWhole Wheat Bread     $3.99\nFresh Espresso Beans  $14.50\nSubtotal:             $23.98\nTOTAL PAID:           $25.89\nCard Ending: ****4109"
        )
    }
    
    var isExtractingText by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isExtractingText = true
            try {
                val image = InputImage.fromFilePath(context, uri)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        rawReceiptInput = visionText.text
                        isExtractingText = false
                        onProcessReceipt(visionText.text)
                    }
                    .addOnFailureListener { e ->
                        rawReceiptInput = "Error extracting text: ${e.message}"
                        isExtractingText = false
                    }
            } catch (e: Exception) {
                rawReceiptInput = "Error loading image: ${e.message}"
                isExtractingText = false
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Receipt OCR Scanner",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_receipt_modal")) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isProcessing) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Scanning & Extracting Receipt Data...",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else if (parsedReceipt != null) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = parsedReceipt.merchantName,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$currencySymbol${parsedReceipt.totalAmount}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Category: ${parsedReceipt.category} • Date: ${parsedReceipt.dateString}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Items: ${parsedReceipt.itemsSummary}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onConfirmSave,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("confirm_receipt_save_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Scanned Expense", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(
                        text = "Receipt OCR Text Input:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = rawReceiptInput,
                        onValueChange = { inputStr -> rawReceiptInput = inputStr },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .testTag("receipt_ocr_text_field"),
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (isExtractingText) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Receipt Image", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val scanner = GmsBarcodeScanning.getClient(context)
                            scanner.startScan()
                                .addOnSuccessListener { barcode ->
                                    val rawValue = barcode.rawValue
                                    if (rawValue != null) {
                                        onBarcodeScanned(rawValue)
                                    }
                                }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scan Barcode / QR Code", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onProcessReceipt(rawReceiptInput) },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("scan_receipt_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.DocumentScanner, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Extract Receipt Data", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
