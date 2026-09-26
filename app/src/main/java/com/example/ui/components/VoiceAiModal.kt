package com.example.ui.components

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.data.ai.AssistantChatMessage
import com.example.data.ai.ParsedVoiceExpense
import com.example.data.models.TransactionType
import com.example.ui.theme.*
import java.util.*

enum class VoiceModalState {
    IDLE,
    LISTENING,
    PROCESSING,
    RESULT,
    SUCCESS,
    ERROR
}

data class VoiceLanguageOption(
    val code: String,
    val sttLocale: String,
    val label: String,
    val flag: String,
    val description: String
)

@Composable
fun VoiceAiModal(
    isProcessing: Boolean,
    parsedExpense: ParsedVoiceExpense?,
    currencySymbol: String,
    assistantMessages: List<AssistantChatMessage> = emptyList(),
    isAssistantLoading: Boolean = false,
    onDismiss: () -> Unit,
    onProcessPrompt: (String) -> Unit,
    onProcessAudio: (String) -> Unit,
    onAskAssistant: (String) -> Unit = {},
    onClearChat: () -> Unit = {},
    onConfirmSave: (title: String, amount: Double, category: String, paymentMethod: String) -> Unit,
    onOpenManualAdd: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val chatListState = rememberLazyListState()

    // Tab 0: AI Assistant Chat (full history access), Tab 1: Single-Turn Voice Entry
    var activeTab by remember { mutableIntStateOf(0) }

    val languageOptions = remember {
        listOf(
            VoiceLanguageOption("ta-IN", "ta-IN", "தமிழ்", "🇮🇳", "Tamil"),
            VoiceLanguageOption("en-IN", "en-IN", "English", "🇺🇸", "English")
        )
    }

    var selectedLanguageCode by remember { mutableStateOf("ta-IN") }
    var modalState by remember { mutableStateOf(VoiceModalState.IDLE) }
    var recognizedSpokenText by remember { mutableStateOf("") }
    var rawInputText by remember { mutableStateOf("") }
    var assistantInputText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPermissionDenied by remember { mutableStateOf(false) }
    var liveAudioLevel by remember { mutableFloatStateOf(0f) }

    // Is speech recognizer listening for Assistant query or Voice Entry?
    var isListeningForAssistant by remember { mutableStateOf(false) }

    // Editable fields for confirmation
    var editTitle by remember { mutableStateOf("") }
    var editAmount by remember { mutableStateOf("") }
    var editType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var editCategory by remember { mutableStateOf("Food & Dining") }
    var editPaymentMethod by remember { mutableStateOf("UPI") }

    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    // Pulse animation for listening state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Stop and cleanup speech recognizer safely without firing stale error callbacks
    fun stopListeningSafely() {
        try {
            speechRecognizer?.setRecognitionListener(null)
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        speechRecognizer = null
        liveAudioLevel = 0f
        isListeningForAssistant = false
    }

    // Process spoken or entered query for Voice Entry
    fun handleProcessVoiceQuery(query: String) {
        if (query.isBlank()) return
        stopListeningSafely()
        modalState = VoiceModalState.PROCESSING
        onProcessPrompt(query)
    }

    // Handle Assistant query
    fun handleAskAssistant(query: String) {
        val clean = query.trim()
        if (clean.isBlank()) return
        stopListeningSafely()
        assistantInputText = ""
        onAskAssistant(clean)
    }

    // Auto-scroll chat to latest message
    LaunchedEffect(assistantMessages.size, isAssistantLoading) {
        if (assistantMessages.isNotEmpty()) {
            chatListState.animateScrollToItem(assistantMessages.size - 1)
        }
    }

    // System Voice Dialog Launcher (fallback / standard intent)
    val systemVoiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenResults = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val topSpoken = spokenResults?.firstOrNull()
            if (!topSpoken.isNullOrBlank()) {
                if (isListeningForAssistant) {
                    handleAskAssistant(topSpoken)
                } else {
                    recognizedSpokenText = topSpoken
                    handleProcessVoiceQuery(topSpoken)
                }
            } else {
                errorMessage = "Could not hear clearly, please try again."
                modalState = VoiceModalState.ERROR
                Toast.makeText(context, "Could not hear clearly, please try again.", Toast.LENGTH_SHORT).show()
            }
        } else {
            errorMessage = "Could not hear clearly, please try again."
            modalState = VoiceModalState.ERROR
        }
    }

    fun launchSystemVoiceIntent(langCode: String = selectedLanguageCode, forAssistant: Boolean = false) {
        isListeningForAssistant = forAssistant
        val targetOption = languageOptions.find { it.code == langCode } ?: languageOptions.first()
        val sttLocale = targetOption.sttLocale
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, sttLocale)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, sttLocale)
                putExtra(RecognizerIntent.EXTRA_PROMPT, if (forAssistant) "Ask Zenith AI in ${targetOption.label}..." else "Speak in ${targetOption.label}...")
            }
            systemVoiceLauncher.launch(intent)
        } catch (e: Exception) {
            errorMessage = "Voice recognition unavailable: ${e.localizedMessage}"
            modalState = VoiceModalState.ERROR
        }
    }

    // Permission launcher for audio recording
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isPermissionDenied = false
            errorMessage = null
            modalState = VoiceModalState.IDLE
        } else {
            isPermissionDenied = true
            errorMessage = "Microphone permission is required for voice commands."
            modalState = VoiceModalState.ERROR
        }
    }

    fun startListening(langCode: String = selectedLanguageCode, forAssistant: Boolean = false) {
        stopListeningSafely()
        errorMessage = null
        isPermissionDenied = false
        recognizedSpokenText = ""
        isListeningForAssistant = forAssistant

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            isPermissionDenied = true
            errorMessage = "Microphone permission is required for voice commands."
            modalState = VoiceModalState.ERROR
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            launchSystemVoiceIntent(langCode, forAssistant)
            return
        }

        val targetOption = languageOptions.find { it.code == langCode } ?: languageOptions.first()
        val sttLocale = targetOption.sttLocale

        try {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, sttLocale)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, sttLocale)
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("ta-IN", "en-IN", "en-US"))
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 300L)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_PROMPT, if (forAssistant) "Ask Zenith AI in ${targetOption.label}..." else "Speak in ${targetOption.label}...")
            }

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    modalState = VoiceModalState.LISTENING
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {
                    liveAudioLevel = (rmsdB / 10f).coerceIn(0.1f, 1f)
                }
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    modalState = VoiceModalState.PROCESSING
                }
                override fun onError(error: Int) {
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "Could not hear clearly, please try again."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening timed out. Please tap mic and try again."
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Please retry."
                        SpeechRecognizer.ERROR_NETWORK -> "Network issue. Please retry or type."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                            isPermissionDenied = true
                            "Microphone permission is required."
                        }
                        else -> "Could not hear clearly, please try again."
                    }
                    errorMessage = msg
                    modalState = VoiceModalState.ERROR
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    if (text.isNotBlank()) {
                        if (isListeningForAssistant) {
                            handleAskAssistant(text)
                        } else {
                            recognizedSpokenText = text
                            handleProcessVoiceQuery(text)
                        }
                    } else {
                        errorMessage = "Could not hear clearly, please try again."
                        modalState = VoiceModalState.ERROR
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    if (text.isNotBlank()) {
                        recognizedSpokenText = text
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            speechRecognizer = recognizer
            recognizer.startListening(intent)
            modalState = VoiceModalState.LISTENING
        } catch (e: Exception) {
            e.printStackTrace()
            launchSystemVoiceIntent(langCode, forAssistant)
        }
    }

    // Sync state with ViewModel
    LaunchedEffect(isProcessing) {
        if (isProcessing) {
            modalState = VoiceModalState.PROCESSING
        }
    }

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
            }
        }
    }

    fun speakAloud(text: String) {
        val ttsEngine = tts ?: return
        if (!isTtsReady) return
        val cleanText = text.replace("*", "").replace("#", "").replace("•", "").trim()
        val targetLocale = if (selectedLanguageCode == "ta-IN" || text.any { it in '\u0B80'..'\u0BFF' }) Locale("ta", "IN") else Locale.US
        ttsEngine.language = targetLocale
        ttsEngine.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "ZENITH_VOICE_OUT")
    }

    fun readOutTransaction(title: String, amount: Double, type: TransactionType, paymentMethod: String) {
        val isTamil = selectedLanguageCode == "ta-IN"
        val amtInt = if (amount % 1.0 == 0.0) amount.toInt().toString() else String.format(Locale.US, "%.2f", amount)
        val textToSpeak = if (isTamil) {
            if (type == TransactionType.EXPENSE) {
                "$title $amtInt ரூபாய் $paymentMethod மூலம் செலவு பதிவு செய்யப்பட்டது."
            } else {
                "$title $amtInt ரூபாய் வருமானம் பதிவு செய்யப்பட்டது."
            }
        } else {
            if (type == TransactionType.EXPENSE) {
                "Added $title for $amtInt rupees via $paymentMethod."
            } else {
                "Recorded $title income of $amtInt rupees via $paymentMethod."
            }
        }
        speakAloud(textToSpeak)
    }

    LaunchedEffect(parsedExpense) {
        if (parsedExpense != null) {
            activeTab = 1 // Switch to Voice Entry review tab
            editTitle = parsedExpense.title
            editAmount = if (parsedExpense.amount > 0) {
                if (parsedExpense.amount % 1.0 == 0.0) String.format(Locale.US, "%.0f", parsedExpense.amount)
                else String.format(Locale.US, "%.2f", parsedExpense.amount)
            } else ""
            editType = parsedExpense.type
            editCategory = parsedExpense.category
            editPaymentMethod = parsedExpense.paymentMethod
            modalState = VoiceModalState.RESULT

            // Automatically Read Aloud the parsed transaction result
            readOutTransaction(parsedExpense.title, parsedExpense.amount, parsedExpense.type, parsedExpense.paymentMethod)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            stopListeningSafely()
            try {
                tts?.stop()
                tts?.shutdown()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val examplePhrases = remember(selectedLanguageCode) {
        when (selectedLanguageCode) {
            "ta-IN" -> listOf(
                "இன்று படத்திற்கு 250 செலவு",
                "10 தக்காளி 50 ரூபாய்",
                "2 கிலோ வெங்காயம் 60 ரூபாய்",
                "ஒரு டீ 15 ரூபாய்",
                "பெட்ரோல் 500 ரூபாய்",
                "சம்பளம் 35000 வந்தது"
            )
            else -> listOf(
                "Innaiku movie ki 250 selavu aachu",
                "Spent 250 on lunch",
                "Add 5 apples",
                "500 petrol via UPI",
                "Paid 1200 for groceries",
                "Got salary 35000"
            )
        }
    }

    val assistantSuggestionChips = remember {
        listOf(
            "📊 Monthly Overview",
            "🍔 Food & Dining Spend",
            "⚡ UPI Transactions",
            "🎯 Budget Health",
            "💡 Savings Advice",
            "📈 Highest Expense",
            "📜 Recent Transactions",
            "💰 Net Balance"
        )
    }

    val categoryList = listOf(
        "Food & Dining",
        "Shopping",
        "Transportation",
        "Bills & Utilities",
        "Entertainment",
        "Healthcare",
        "Salary & Income",
        "Education",
        "Housing & Rent",
        "Other"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SlateDarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ZenithLogo(size = 32.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Zenith AI Assistant",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateDarkTextPrimary
                            )
                            Text(
                                text = "Full Transaction History & Voice Engine",
                                fontSize = 11.sp,
                                color = EmeraldDarkPrimary
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            stopListeningSafely()
                            onDismiss()
                        },
                        modifier = Modifier.size(32.dp).testTag("close_voice_modal")
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = SlateDarkTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Mode Tabs: [ 💬 AI Assistant ] vs [ 🎙️ Quick Voice Add ]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SlateDarkSurfaceVariant, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Tab 0: AI Assistant Chat
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (activeTab == 0) EmeraldDarkPrimary else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clickable {
                                stopListeningSafely()
                                activeTab = 0
                            }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Chat,
                                contentDescription = null,
                                tint = if (activeTab == 0) Color.White else SlateDarkTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AI Assistant",
                                fontSize = 13.sp,
                                fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (activeTab == 0) Color.White else SlateDarkTextSecondary
                            )
                        }
                    }

                    // Tab 1: Voice Entry
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (activeTab == 1) EmeraldDarkPrimary else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clickable {
                                stopListeningSafely()
                                activeTab = 1
                            }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = null,
                                tint = if (activeTab == 1) Color.White else SlateDarkTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Voice Entry",
                                fontSize = 13.sp,
                                fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (activeTab == 1) Color.White else SlateDarkTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- TAB 0: CONVERSATIONAL AI ASSISTANT WITH FULL TRANSACTION HISTORY ---
                if (activeTab == 0) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Suggestion Chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(assistantSuggestionChips) { prompt ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = SlateDarkSurfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderColor),
                                    modifier = Modifier.clickable {
                                        handleAskAssistant(prompt.substringAfter(" "))
                                    }
                                ) {
                                    Text(
                                        text = prompt,
                                        fontSize = 11.sp,
                                        color = SlateDarkTextPrimary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Message Stream
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SlateDarkSurfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                        ) {
                            LazyColumn(
                                state = chatListState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(assistantMessages) { msg ->
                                    if (msg.isUser) {
                                        // User Message
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(14.dp, 14.dp, 2.dp, 14.dp),
                                                color = EmeraldDarkPrimary,
                                                modifier = Modifier.widthIn(max = 270.dp)
                                            ) {
                                                Text(
                                                    text = msg.text,
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    modifier = Modifier.padding(10.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        // AI Assistant Message
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Start
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(14.dp, 14.dp, 14.dp, 2.dp),
                                                color = SlateDarkSurfaceVariant,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderColor),
                                                modifier = Modifier.widthIn(max = 290.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Box(
                                                                modifier = Modifier.size(18.dp).clip(CircleShape).background(EmeraldDarkPrimary.copy(alpha = 0.2f)),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = EmeraldDarkPrimary, modifier = Modifier.size(12.dp))
                                                            }
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text("Zenith AI", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldDarkPrimary)
                                                        }

                                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                            // Read Aloud Button
                                                            IconButton(
                                                                onClick = { speakAloud(msg.text) },
                                                                modifier = Modifier.size(24.dp)
                                                            ) {
                                                                Icon(Icons.Default.VolumeUp, contentDescription = "Listen", tint = CyanDarkSecondary, modifier = Modifier.size(14.dp))
                                                            }
                                                            // Copy Button
                                                            IconButton(
                                                                onClick = {
                                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                                    clipboard.setPrimaryClip(ClipData.newPlainText("Zenith AI", msg.text))
                                                                    Toast.makeText(context, "Copied response to clipboard", Toast.LENGTH_SHORT).show()
                                                                },
                                                                modifier = Modifier.size(24.dp)
                                                            ) {
                                                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = SlateDarkTextSecondary, modifier = Modifier.size(14.dp))
                                                            }
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(4.dp))

                                                    Text(
                                                        text = msg.text,
                                                        color = SlateDarkTextPrimary,
                                                        fontSize = 12.sp,
                                                        lineHeight = 17.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (isAssistantLoading) {
                                    item {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Start
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = SlateDarkSurfaceVariant,
                                                modifier = Modifier.padding(4.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    CircularProgressIndicator(strokeWidth = 2.dp, color = EmeraldDarkPrimary, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Analyzing financial records...", fontSize = 11.sp, color = SlateDarkTextSecondary)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Assistant Input Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SlateDarkSurfaceVariant, RoundedCornerShape(14.dp))
                                .border(1.dp, GlassBorderColor, RoundedCornerShape(14.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = assistantInputText,
                                onValueChange = { assistantInputText = it },
                                placeholder = { Text("Ask about expenses, UPI, budgets...", fontSize = 12.sp, color = SlateDarkTextSecondary) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = {
                                    keyboardController?.hide()
                                    handleAskAssistant(assistantInputText)
                                }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            // Mic Button for voice question
                            IconButton(
                                onClick = { startListening(selectedLanguageCode, forAssistant = true) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = "Speak Question",
                                    tint = if (modalState == VoiceModalState.LISTENING && isListeningForAssistant) CyanDarkSecondary else SlateDarkTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Send Button
                            IconButton(
                                onClick = {
                                    keyboardController?.hide()
                                    handleAskAssistant(assistantInputText)
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Send",
                                    tint = EmeraldDarkPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Clear Chat option
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = onClearChat,
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(13.dp), tint = SlateDarkTextMuted)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear Chat", fontSize = 11.sp, color = SlateDarkTextMuted)
                            }
                        }
                    }
                }

                // --- TAB 1: SINGLE-TURN VOICE ENTRY ---
                if (activeTab == 1) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Language Switcher Row (50/50 Split: Tamil & English)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SlateDarkSurfaceVariant, RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            languageOptions.forEach { lang ->
                                val isSelected = selectedLanguageCode == lang.code
                                val isListening = isSelected && modalState == VoiceModalState.LISTENING && !isListeningForAssistant

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) EmeraldDarkPrimary else Color.Transparent,
                                    border = if (isListening) {
                                        androidx.compose.foundation.BorderStroke(1.5.dp, CyanDarkSecondary)
                                    } else null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .clickable {
                                            if (selectedLanguageCode != lang.code) {
                                                selectedLanguageCode = lang.code
                                                if (modalState == VoiceModalState.LISTENING) {
                                                    startListening(lang.code, forAssistant = false)
                                                }
                                            }
                                        }
                                        .testTag("btn_lang_${lang.code}")
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "${lang.flag} ${lang.label}",
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isSelected) Color.White else SlateDarkTextSecondary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        when (modalState) {
                            VoiceModalState.IDLE, VoiceModalState.LISTENING, VoiceModalState.ERROR -> {
                                // Microphone Button
                                Box(
                                    modifier = Modifier
                                        .size(86.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (modalState == VoiceModalState.LISTENING) {
                                                Brush.radialGradient(
                                                    colors = listOf(CyanDarkSecondary.copy(alpha = 0.4f), Color.Transparent)
                                                )
                                            } else {
                                                Brush.radialGradient(
                                                    colors = listOf(EmeraldDarkPrimary.copy(alpha = 0.2f), Color.Transparent)
                                                )
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(66.dp)
                                            .scale(if (modalState == VoiceModalState.LISTENING) pulseScale else 1f)
                                            .clip(CircleShape)
                                            .background(
                                                if (modalState == VoiceModalState.LISTENING) {
                                                    Brush.linearGradient(listOf(CyanDarkSecondary, Color(0xFF6366F1)))
                                                } else {
                                                    Brush.linearGradient(listOf(EmeraldDarkPrimary, Color(0xFF059669)))
                                                }
                                            )
                                            .clickable {
                                                if (modalState == VoiceModalState.LISTENING) {
                                                    if (recognizedSpokenText.isNotBlank()) {
                                                        handleProcessVoiceQuery(recognizedSpokenText)
                                                    } else {
                                                        stopListeningSafely()
                                                        modalState = VoiceModalState.IDLE
                                                    }
                                                } else {
                                                    startListening(selectedLanguageCode, forAssistant = false)
                                                }
                                            }
                                            .testTag("mic_listen_button"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (modalState == VoiceModalState.LISTENING) Icons.Default.Stop else Icons.Default.Mic,
                                            contentDescription = "Microphone",
                                            tint = Color.White,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                }

                                if (modalState == VoiceModalState.LISTENING) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.height(24.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val multipliers = listOf(0.4f, 0.7f, 1.0f, 1.3f, 1.0f, 0.7f, 0.4f)
                                        multipliers.forEach { factor ->
                                            val barHeight = (6.dp + (16.dp * liveAudioLevel * factor)).coerceIn(4.dp, 22.dp)
                                            Box(
                                                modifier = Modifier
                                                    .width(4.dp)
                                                    .height(barHeight)
                                                    .clip(CircleShape)
                                                    .background(
                                                        Brush.verticalGradient(listOf(CyanDarkSecondary, EmeraldDarkPrimary))
                                                    )
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = when (modalState) {
                                        VoiceModalState.LISTENING -> "Listening in ${languageOptions.find { it.code == selectedLanguageCode }?.label ?: "Tamil"}..."
                                        VoiceModalState.ERROR -> errorMessage ?: "Could not hear clearly, please try again."
                                        else -> if (selectedLanguageCode == "ta-IN") "Tap mic to speak (எ.கா. \"10 தக்காளி\" அல்லது \"150 டீ\")" else "Tap mic to speak (e.g. \"Spent 250 on lunch via UPI\")"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (modalState == VoiceModalState.LISTENING) CyanDarkSecondary else if (modalState == VoiceModalState.ERROR) ExpenseRed else SlateDarkTextPrimary
                                )

                                if (recognizedSpokenText.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = SlateDarkSurfaceVariant,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "\"$recognizedSpokenText\"",
                                            fontSize = 12.sp,
                                            color = Color.White,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Direct Text Input
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(SlateDarkSurfaceVariant, RoundedCornerShape(12.dp))
                                        .border(1.dp, GlassBorderColor, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = rawInputText,
                                        onValueChange = { rawInputText = it },
                                        placeholder = { Text("or type e.g. Pathu thakkali / 5 apples", fontSize = 11.sp, color = SlateDarkTextSecondary) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(onDone = {
                                            keyboardController?.hide()
                                            if (rawInputText.isNotBlank()) {
                                                handleProcessVoiceQuery(rawInputText)
                                            }
                                        }),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        modifier = Modifier.weight(1f).testTag("voice_manual_text_input")
                                    )

                                    IconButton(
                                        onClick = {
                                            keyboardController?.hide()
                                            if (rawInputText.isNotBlank()) {
                                                handleProcessVoiceQuery(rawInputText)
                                            }
                                        },
                                        modifier = Modifier.size(34.dp).testTag("btn_parse_text")
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Parse", tint = EmeraldDarkPrimary)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(examplePhrases) { prompt ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = SlateDarkSurfaceVariant,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderColor),
                                            modifier = Modifier.clickable { handleProcessVoiceQuery(prompt) }
                                        ) {
                                            Text(
                                                text = prompt,
                                                fontSize = 11.sp,
                                                color = SlateDarkTextPrimary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            VoiceModalState.PROCESSING -> {
                                Spacer(modifier = Modifier.height(16.dp))
                                CircularProgressIndicator(color = EmeraldDarkPrimary, strokeWidth = 3.dp, modifier = Modifier.size(38.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Extracting Item, Quantity & Amount...", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SlateDarkTextPrimary)
                                Text("Processing transaction with Zenith NLP Engine", fontSize = 11.sp, color = SlateDarkTextSecondary)
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            VoiceModalState.RESULT -> {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = SlateDarkSurfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderColor),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("PARSED TRANSACTION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldDarkPrimary)
                                            if (parsedExpense?.quantity != null || !parsedExpense?.item.isNullOrBlank()) {
                                                Surface(shape = RoundedCornerShape(6.dp), color = EmeraldDarkPrimary.copy(alpha = 0.2f)) {
                                                    val qVal = parsedExpense?.quantity ?: 1.0
                                                    val qStr = if (qVal % 1.0 == 0.0) "${qVal.toInt()}" else "$qVal"
                                                    Text("Qty: $qStr ${parsedExpense?.unit ?: "pcs"}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmeraldDarkPrimary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Type Switcher (Expense / Income)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(SlateDarkSurface, RoundedCornerShape(10.dp))
                                                .padding(3.dp)
                                        ) {
                                            val isExp = editType == TransactionType.EXPENSE
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(32.dp)
                                                    .background(if (isExp) ExpenseRed else Color.Transparent, RoundedCornerShape(8.dp))
                                                    .clickable { editType = TransactionType.EXPENSE },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("Expense", fontWeight = FontWeight.Bold, color = if (isExp) Color.White else SlateDarkTextSecondary, fontSize = 12.sp)
                                            }

                                            val isInc = editType == TransactionType.INCOME
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(32.dp)
                                                    .background(if (isInc) IncomeGreen else Color.Transparent, RoundedCornerShape(8.dp))
                                                    .clickable { editType = TransactionType.INCOME },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("Income", fontWeight = FontWeight.Bold, color = if (isInc) Color.White else SlateDarkTextSecondary, fontSize = 12.sp)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Amount Field
                                        OutlinedTextField(
                                            value = editAmount,
                                            onValueChange = { editAmount = it },
                                            label = { Text("Amount ($currencySymbol)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth().testTag("voice_edit_amount")
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Title Field
                                        OutlinedTextField(
                                            value = editTitle,
                                            onValueChange = { editTitle = it },
                                            label = { Text("Title / Description") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth().testTag("voice_edit_title")
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Category Chips
                                        Text("Category", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SlateDarkTextSecondary)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LazyRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            items(categoryList) { cat ->
                                                FilterChip(
                                                    selected = editCategory == cat,
                                                    onClick = { editCategory = cat },
                                                    label = { Text(text = cat, fontSize = 11.sp) }
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        PaymentMethodDropdown(
                                            selectedMethod = editPaymentMethod,
                                            onMethodSelected = { editPaymentMethod = it },
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        OutlinedButton(
                                            onClick = {
                                                val amt = editAmount.toDoubleOrNull() ?: 0.0
                                                readOutTransaction(editTitle, amt, editType, editPaymentMethod)
                                            },
                                            modifier = Modifier.fillMaxWidth().height(38.dp),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp), tint = CyanDarkSecondary)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("🔊 Read Aloud", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyanDarkSecondary)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                val parsedAmount = editAmount.toDoubleOrNull() ?: 0.0
                                val isSaveEnabled = parsedAmount > 0 && editTitle.isNotBlank()

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            modalState = VoiceModalState.IDLE
                                            startListening(selectedLanguageCode, forAssistant = false)
                                        },
                                        modifier = Modifier.weight(1f).height(44.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Speak Again", fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = {
                                            if (isSaveEnabled) {
                                                val finalTitle = editTitle.trim()
                                                onConfirmSave(finalTitle, parsedAmount, editCategory, editPaymentMethod)
                                                Toast.makeText(context, "Added $currencySymbol$parsedAmount for $finalTitle", Toast.LENGTH_SHORT).show()
                                                onDismiss()
                                            }
                                        },
                                        modifier = Modifier.weight(1.3f).height(44.dp).testTag("confirm_voice_save_btn"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldDarkPrimary),
                                        enabled = isSaveEnabled
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Save Entry", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }

                            VoiceModalState.SUCCESS -> {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Transaction Saved!", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SlateDarkTextPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}
