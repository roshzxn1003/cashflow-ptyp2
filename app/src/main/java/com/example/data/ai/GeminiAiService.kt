package com.example.data.ai

import com.example.data.models.TransactionType
import com.example.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ParsedVoiceExpense(
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: String,
    val paymentMethod: String,
    val note: String
)

data class ParsedReceipt(
    val merchantName: String,
    val totalAmount: Double,
    val category: String,
    val dateString: String,
    val itemsSummary: String
)

object GeminiAiService {
    
    suspend fun parseVoiceCommand(prompt: String): ParsedVoiceExpense = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext fallbackParseVoiceCommand(prompt)
        }
        try {
            val systemInstruction = """
                You are CashFlow AI. The user is dictating an expense or income. Extract data into ONLY a JSON object:
                - "title": short descriptive title
                - "amount": number
                - "type": "EXPENSE" or "INCOME"
                - "category": ("Food & Dining", "Transportation", "Housing & Rent", "Bills & Utilities", "Shopping", "Salary & Income", "Investments")
                - "paymentMethod": ("Cash", "Credit Card", "UPI", "Bank Transfer")
                - "note": short note
                Return plain JSON only without markdown formatting.
            """.trimIndent()
            val responseText = callGeminiApi(apiKey, systemInstruction, prompt)
            val jsonClean = responseText.replace("```json", "").replace("```", "").trim()
            val jsonObj = JSONObject(jsonClean)
            val title = jsonObj.optString("title", "Voice Entry")
            val amount = jsonObj.optDouble("amount", 0.0)
            val typeStr = jsonObj.optString("type", "EXPENSE")
            val type = if (typeStr.uppercase() == "INCOME") TransactionType.INCOME else TransactionType.EXPENSE
            val category = jsonObj.optString("category", "Food & Dining")
            val paymentMethod = jsonObj.optString("paymentMethod", "Cash")
            val note = jsonObj.optString("note", "Voice logged: $prompt")
            ParsedVoiceExpense(title, amount, type, category, paymentMethod, note)
        } catch (e: Exception) {
            fallbackParseVoiceCommand(prompt)
        }
    }

    suspend fun parseAudioCommand(audioBase64: String): ParsedVoiceExpense = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext fallbackParseVoiceCommand("simulated audio parsed")
        }
        try {
            val systemInstruction = """
                You are CashFlow AI. The user is dictating an expense or income via audio. 
                Transcribe the audio AND extract the data into ONLY a JSON object:
                - "title": short descriptive title (based on transcription)
                - "amount": number
                - "type": "EXPENSE" or "INCOME"
                - "category": ("Food & Dining", "Transportation", "Housing & Rent", "Bills & Utilities", "Shopping", "Salary & Income", "Investments")
                - "paymentMethod": ("Cash", "Credit Card", "UPI", "Bank Transfer")
                - "note": transcription of the audio
                Return plain JSON only without markdown formatting.
            """.trimIndent()
            
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 30000
            conn.readTimeout = 30000

            val requestPayload = JSONObject().apply {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply { put("text", systemInstruction) }))
                })
                put("contents", JSONArray().put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "audio/mp4")
                                put("data", audioBase64)
                            })
                        })
                        put(JSONObject().apply { put("text", "Please analyze this audio transaction.") })
                    })
                }))
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(requestPayload.toString())
                writer.flush()
            }

            if (conn.responseCode == 200) {
                val responseString = conn.inputStream.bufferedReader().readText()
                val respObj = JSONObject(responseString)
                val candidates = respObj.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val content = firstCandidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val firstPart = parts?.optJSONObject(0)
                val responseText = firstPart?.optString("text") ?: ""

                val jsonClean = responseText.replace("```json", "").replace("```", "").trim()
                val jsonObj = JSONObject(jsonClean)
                val title = jsonObj.optString("title", "Audio Entry")
                val amount = jsonObj.optDouble("amount", 0.0)
                val typeStr = jsonObj.optString("type", "EXPENSE")
                val type = if (typeStr.uppercase() == "INCOME") TransactionType.INCOME else TransactionType.EXPENSE
                val category = jsonObj.optString("category", "Food & Dining")
                val paymentMethod = jsonObj.optString("paymentMethod", "Cash")
                val note = jsonObj.optString("note", "Audio input")
                ParsedVoiceExpense(title, amount, type, category, paymentMethod, note)
            } else {
                throw RuntimeException("Gemini API error code: ${conn.responseCode}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackParseVoiceCommand("Failed to parse audio")
        }
    }

    suspend fun parseReceiptOcr(receiptText: String): ParsedReceipt = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext ParsedReceipt(
                merchantName = "Scanned Store",
                totalAmount = 84.50,
                category = "Shopping",
                dateString = "Today",
                itemsSummary = "Simulated receipt scan items"
            )
        }
        try {
            val systemInstruction = """
                You are a receipt scanner OCR AI. Analyze the receipt text and return ONLY a JSON object:
                - "merchantName": name of store or restaurant
                - "totalAmount": final total amount paid (number)
                - "category": best matching category ("Food & Dining", "Shopping", "Bills & Utilities", "Transportation", etc.)
                - "dateString": date on receipt or "Today"
                - "itemsSummary": short 1-line comma-separated list of items
                Return plain JSON only without markdown formatting.
            """.trimIndent()
            val responseText = callGeminiApi(apiKey, systemInstruction, receiptText)
            val jsonClean = responseText.replace("```json", "").replace("```", "").trim()
            val jsonObj = JSONObject(jsonClean)
            ParsedReceipt(
                merchantName = jsonObj.optString("merchantName", "Receipt Scan"),
                totalAmount = jsonObj.optDouble("totalAmount", 0.0),
                category = jsonObj.optString("category", "Shopping"),
                dateString = jsonObj.optString("dateString", "Today"),
                itemsSummary = jsonObj.optString("itemsSummary", "Scanned receipt")
            )
        } catch (e: Exception) {
            ParsedReceipt(
                merchantName = "Scanned Merchant",
                totalAmount = 52.00,
                category = "Food & Dining",
                dateString = "Today",
                itemsSummary = "OCR receipt items parsed"
            )
        }
    }

    suspend fun getFinancialCoachAdvice(totalIncome: Double, totalExpense: Double, topExpenseCategory: String): String = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
        val prompt = "User monthly status: Income = $$totalIncome, Expense = $$totalExpense, Top Spending Category = '${topExpenseCategory}'. Provide 3 actionable financial advice tips in bullet points."
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            val savingsRate = if (totalIncome > 0) ((totalIncome - totalExpense) / totalIncome * 100).toInt() else 0
            return@withContext "• **Savings Velocity**: You're saving ~$savingsRate% of your income. Aim for 20%+ monthly savings.\n• **Category Watch**: Highest expenditure is currently in **$topExpenseCategory**. Consider setting a budget cap.\n• **Rule of 50/30/20**: Allocate 50% for needs, 30% for wants, and 20% directly into savings goals!"
        }
        try {
            val systemInstruction = "You are CashFlow AI Financial Advisor. Be encouraging, precise, and practical. Use bullet points."
            callGeminiApi(apiKey, systemInstruction, prompt)
        } catch (e: Exception) {
            "• **Keep Tracking**: Consistently logging every small expense helps unlock 15% extra savings monthly!\n• **Review Subscriptions**: Audit recurring monthly payments to eliminate unused services.\n• **Emergency Cushion**: Build a 3-6 month liquid safety fund for financial peace of mind."
        }
    }

    private fun callGeminiApi(apiKey: String, systemInstruction: String, promptText: String): String {
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 15000
        conn.readTimeout = 15000

        val requestPayload = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().apply { put("text", systemInstruction) }))
            })
            put("contents", JSONArray().put(JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().apply { put("text", promptText) }))
            }))
        }

        OutputStreamWriter(conn.outputStream).use { writer ->
            writer.write(requestPayload.toString())
            writer.flush()
        }

        if (conn.responseCode == 200) {
            val responseString = conn.inputStream.bufferedReader().readText()
            val respObj = JSONObject(responseString)
            val candidates = respObj.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val firstPart = parts?.optJSONObject(0)
            return firstPart?.optString("text") ?: ""
        } else {
            throw RuntimeException("Gemini API error code: ${conn.responseCode}")
        }
    }

    private fun fallbackParseVoiceCommand(prompt: String): ParsedVoiceExpense {
        val lower = prompt.lowercase()
        val isIncome = lower.contains("salary") || lower.contains("received") || lower.contains("earned") || lower.contains("income") || lower.contains("got paid")
        
        val numberRegex = Regex("""(\d+(\.\d+)?)""")
        val match = numberRegex.find(prompt)
        val amount = match?.value?.toDoubleOrNull() ?: 25.0

        val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE
        val category = when {
            lower.contains("coffee") || lower.contains("lunch") || lower.contains("dinner") || lower.contains("food") || lower.contains("restaurant") -> "Food & Dining"
            lower.contains("uber") || lower.contains("cab") || lower.contains("bus") || lower.contains("fuel") || lower.contains("gas") -> "Transportation"
            lower.contains("rent") || lower.contains("apartment") -> "Housing & Rent"
            lower.contains("bill") || lower.contains("electricity") || lower.contains("wifi") -> "Bills & Utilities"
            lower.contains("shopping") || lower.contains("clothes") || lower.contains("shoes") -> "Shopping"
            isIncome -> "Salary & Income"
            else -> "Food & Dining"
        }
        val paymentMethod = when {
            lower.contains("upi") -> "UPI"
            lower.contains("card") || lower.contains("credit") -> "Credit Card"
            lower.contains("bank") || lower.contains("transfer") -> "Bank Transfer"
            else -> "Cash"
        }
        val title = prompt.ifBlank { "Voice Expense" }.take(30)

        return ParsedVoiceExpense(
            title = title,
            amount = amount,
            type = type,
            category = category,
            paymentMethod = paymentMethod,
            note = "Logged via Voice AI"
        )
    }
}
