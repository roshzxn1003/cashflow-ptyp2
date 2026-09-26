package com.example.data.upi

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log

/**
 * Parsed UPI payment information. Mirrors the NPCI UPI Linking Specification
 * query parameters used by every UPI app on Android.
 */
data class UpiPaymentInfo(
    val payeeAddress: String,      // pa  - VPA, e.g. "someone@okhdfcbank"
    val payeeName: String = "",    // pn  - display name / merchant
    val amount: String = "",       // am  - optional, only when the QR carries it
    val currency: String = "INR",  // cu
    val note: String = "",         // tn  - transaction note
    val txnRef: String = "",       // tr  - transaction reference id (only when provided)
    val merchantCode: String = ""  // mc  - optional merchant category code
)

/**
 * Result of returning from an external UPI payment app (Google Pay, PhonePe, Paytm, BHIM, etc.).
 * Parses both the NPCI query response string ("response" extra) and bundle extras.
 */
data class UpiIntentResult(
    val launched: Boolean,
    val isExplicitSuccess: Boolean,
    val isExplicitFailure: Boolean,
    val status: String,            // "SUCCESS", "SUBMITTED", "PENDING", "FAILURE", "CANCELLED", "UNKNOWN"
    val returnedTxnRef: String?,   // tr or fallback reference
    val txnId: String?,            // upitxnid / txnId / UTR
    val approvalRefNo: String?,    // ApprovalRefNo / RRN (Bank Retrieval Reference)
    val responseCode: String?,     // "00", "0", etc.
    val rawResponse: String?
) {
    val isSuccessOrPending: Boolean get() = isExplicitSuccess || status.equals("SUBMITTED", ignoreCase = true) || status.equals("PENDING", ignoreCase = true)
    val needsConfirmation: Boolean get() = launched && !isExplicitFailure
}

/**
 * Standard UPI deep-link integration (NPCI spec). Zenith initiates payments
 * to a VPA through installed UPI apps with full response parsing and error handling.
 */
object UpiService {

    private const val TAG = "Zenith_UPI"
    private const val UPI_SCHEME = "upi"
    private const val UPI_AUTHORITY = "pay"

    private val VPA_REGEX = Regex("^[a-zA-Z0-9][a-zA-Z0-9.\\-_]{1,}@[a-zA-Z]{2,}$")

    fun isValidVpa(vpa: String): Boolean = VPA_REGEX.matches(vpa.trim())

    /**
     * Builds the standard `upi://pay` deep-link URI from the NPCI parameter set.
     * Sanitizes payee name, transaction note, and ensures unverified merchant references
     * are not appended to personal P2P deep-links (preventing bank "UPI limit per bank" errors).
     */
    fun buildPaymentUri(info: UpiPaymentInfo): Uri {
        val cleanPa = info.payeeAddress.trim()
        val cleanPn = info.payeeName.trim().replace(Regex("[^a-zA-Z0-9 ._-]"), " ").take(60).trim()
        val cleanAm = info.amount.trim().replace(Regex("[^0-9.]"), "")
        val cleanCu = if (info.currency.isNotBlank()) info.currency.trim() else "INR"
        val cleanTn = info.note.trim().replace(Regex("[^a-zA-Z0-9 ._-]"), " ").take(50).trim()
        val cleanTr = info.txnRef.trim().replace(Regex("[^a-zA-Z0-9._-]"), "")
        val cleanMc = info.merchantCode.trim()

        val builder = Uri.Builder()
            .scheme(UPI_SCHEME)
            .authority(UPI_AUTHORITY)
            .appendQueryParameter("pa", cleanPa)

        if (cleanPn.isNotBlank()) builder.appendQueryParameter("pn", cleanPn)
        if (cleanAm.isNotBlank()) builder.appendQueryParameter("am", cleanAm)
        if (cleanCu.isNotBlank()) builder.appendQueryParameter("cu", cleanCu)
        if (cleanTn.isNotBlank()) builder.appendQueryParameter("tn", cleanTn.ifBlank { "Zenith Payment" })
        
        // NPCI Rule: Only append 'tr' for verified merchant transfers (when 'mc' is present)
        // or if 'tr' is a non-synthetic reference from an actual scanned merchant QR.
        if (cleanTr.isNotBlank() && cleanMc.isNotBlank()) {
            builder.appendQueryParameter("tr", cleanTr)
        }
        if (cleanMc.isNotBlank()) builder.appendQueryParameter("mc", cleanMc)

        val uri = builder.build()
        Log.d(TAG, "Generated UPI Deep Link URI: $uri")
        return uri
    }

    /**
     * Launch intent for [UpiPaymentInfo]. When [targetPackage] is blank,
     * it generates a generic `upi://pay` Intent wrapped in `Intent.createChooser`
     * so the Android system displays the native app selector tray for any installed UPI app.
     */
    fun buildPayIntent(info: UpiPaymentInfo, targetPackage: String? = null, useChooser: Boolean = true): Intent? {
        val uri = buildPaymentUri(info)
        Log.i(TAG, "Building UPI Pay Intent: targetPackage=$targetPackage, VPA=${info.payeeAddress}, Amount=${info.amount}")
        return if (targetPackage.isNullOrBlank()) {
            val baseIntent = Intent(Intent.ACTION_VIEW, uri)
            if (useChooser) Intent.createChooser(baseIntent, "Pay with any UPI App") else baseIntent
        } else {
            Intent(Intent.ACTION_VIEW, uri).setPackage(targetPackage)
        }
    }

    /**
     * Explicit generic intent chooser builder for NPCI `upi://pay` requests.
     */
    fun buildGenericChooserIntent(info: UpiPaymentInfo, title: String = "Pay with any UPI App"): Intent {
        val uri = buildPaymentUri(info)
        return Intent.createChooser(Intent(Intent.ACTION_VIEW, uri), title)
    }

    /**
     * Installed apps that can handle `upi://pay` (requires `<queries>` in AndroidManifest).
     */
    fun installedUpiApps(context: Context): List<UpiApp> {
        val probe = Intent(Intent.ACTION_VIEW).setData(Uri.parse("upi://pay"))
        return try {
            val resolves = context.packageManager.queryIntentActivities(probe, PackageManager.MATCH_DEFAULT_ONLY)
            val apps = resolves.sortedBy { it.activityInfo.packageName }
                .map { resolve ->
                    val label = try {
                        context.packageManager.getApplicationLabel(
                            context.packageManager.getApplicationInfo(resolve.activityInfo.packageName, 0)
                        ).toString()
                    } catch (e: Exception) {
                        resolve.activityInfo.packageName
                    }
                    UpiApp(packageName = resolve.activityInfo.packageName, label = label)
                }
            Log.d(TAG, "Found ${apps.size} installed UPI apps: ${apps.joinToString { it.label }}")
            apps
        } catch (e: Exception) {
            Log.e(TAG, "Error discovering installed UPI apps", e)
            emptyList()
        }
    }

    fun isAnyUpiAppInstalled(context: Context): Boolean = installedUpiApps(context).isNotEmpty()

    /**
     * Parses a raw QR payload into [UpiPaymentInfo]. Supports full
     * `upi://pay?pa=...&pn=...` URI, lowercase/uppercase schemes, or a bare VPA.
     */
    fun parseQrPayload(raw: String): UpiPaymentInfo? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return null
        Log.d(TAG, "Parsing QR payload: $trimmed")

        if (trimmed.startsWith("upi://", ignoreCase = true)) {
            val uri = Uri.parse(trimmed)
            val pa = uri.getQueryParameter("pa") ?: return null
            return UpiPaymentInfo(
                payeeAddress = pa,
                payeeName = uri.getQueryParameter("pn") ?: "",
                amount = uri.getQueryParameter("am") ?: "",
                currency = uri.getQueryParameter("cu") ?: "INR",
                note = uri.getQueryParameter("tn") ?: "",
                txnRef = uri.getQueryParameter("tr") ?: "",
                merchantCode = uri.getQueryParameter("mc") ?: ""
            )
        }

        return if (isValidVpa(trimmed)) UpiPaymentInfo(payeeAddress = trimmed) else null
    }

    /**
     * Comprehensive parsing of the ActivityResult after a UPI app returns.
     * Extracts NPCI key-values from "response" string, URI data, or Intent extras.
     */
    fun mapResult(resultCode: Int, data: Intent?): UpiIntentResult {
        Log.i(TAG, "Mapping UPI Activity Result -> resultCode=$resultCode, data=$data")

        val responseMap = mutableMapOf<String, String>()

        // 1. Check raw "response" string in extras (Standard NPCI format: txnId=...&responseCode=00&Status=SUCCESS&txnRef=...)
        val rawResponseString = data?.getStringExtra("response")
            ?: data?.extras?.getString("response")
            ?: data?.dataString
            ?: ""

        if (rawResponseString.isNotBlank()) {
            Log.d(TAG, "Raw UPI Response String: $rawResponseString")
            // Parse query string pairs
            val cleanStr = if (rawResponseString.startsWith("upi://") || rawResponseString.contains("?")) {
                rawResponseString.substringAfter("?")
            } else {
                rawResponseString
            }
            cleanStr.split("&").forEach { pair ->
                val parts = pair.split("=", limit = 2)
                if (parts.size == 2) {
                    responseMap[parts[0].trim().lowercase()] = parts[1].trim()
                }
            }
        }

        // 2. Also inspect all bundle extras directly
        data?.extras?.let { bundle ->
            for (key in bundle.keySet()) {
                val value = bundle.get(key)?.toString()
                if (value != null) {
                    responseMap.putIfAbsent(key.lowercase(), value)
                    Log.d(TAG, "UPI Intent Extra: $key = $value")
                }
            }
        }

        // 3. Inspect Uri query params if present
        data?.data?.let { uri ->
            try {
                uri.queryParameterNames.forEach { param ->
                    uri.getQueryParameter(param)?.let { valStr ->
                        responseMap.putIfAbsent(param.lowercase(), valStr)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not parse Uri query params", e)
            }
        }

        val statusVal = responseMap["status"]?.uppercase() ?: ""
        val responseCode = responseMap["responsecode"] ?: responseMap["response_code"]
        val txnId = responseMap["txnid"] ?: responseMap["upitxnid"] ?: responseMap["transaction_id"]
        val txnRef = responseMap["txnref"] ?: responseMap["tr"] ?: responseMap["transactionref"]
        val approvalRefNo = responseMap["approvalrefno"] ?: responseMap["approval_ref_no"] ?: responseMap["rrn"]

        val isExplicitSuccess = statusVal == "SUCCESS" || responseCode == "00" || responseCode == "0"
        val isExplicitFailure = statusVal == "FAILURE" || statusVal == "FAILED" || (responseCode != null && responseCode != "00" && responseCode != "0" && responseCode != "SUCCESS" && responseCode != "PENDING")

        val normalizedStatus = when {
            isExplicitSuccess -> "SUCCESS"
            statusVal == "SUBMITTED" || statusVal == "PENDING" -> "SUBMITTED"
            isExplicitFailure -> "FAILURE"
            resultCode == Activity.RESULT_CANCELED && responseMap.isEmpty() -> "CANCELLED"
            resultCode == Activity.RESULT_OK -> "SUCCESS"
            else -> "UNKNOWN"
        }

        val finalTxnRef = txnId ?: approvalRefNo ?: txnRef

        Log.i(TAG, "UPI Parsed Result -> Status: $normalizedStatus, TxnId: $txnId, TxnRef: $finalTxnRef, ApprovalRefNo: $approvalRefNo, ResponseCode: $responseCode")

        return UpiIntentResult(
            launched = true,
            isExplicitSuccess = isExplicitSuccess || (resultCode == Activity.RESULT_OK && !isExplicitFailure),
            isExplicitFailure = isExplicitFailure,
            status = normalizedStatus,
            returnedTxnRef = finalTxnRef?.takeIf { it.isNotBlank() },
            txnId = txnId?.takeIf { it.isNotBlank() },
            approvalRefNo = approvalRefNo?.takeIf { it.isNotBlank() },
            responseCode = responseCode?.takeIf { it.isNotBlank() },
            rawResponse = rawResponseString.takeIf { it.isNotBlank() }
        )
    }

    const val GOOGLE_PAY_PACKAGE = "com.google.android.apps.nbu.paisa.user"
    const val PHONEPE_PACKAGE = "com.phonepe.app"
    const val PAYTM_PACKAGE = "net.one97.paytm"
    const val BHIM_PACKAGE = "in.org.npci.upiapp"
    const val CRED_PACKAGE = "com.dreamplug.androidapp"
    const val AMAZON_PAY_PACKAGE = "in.amazon.mShop.android.shopping"

    fun isGooglePayInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(GOOGLE_PAY_PACKAGE, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Builds a direct Intent for Google Pay (Tez) if installed, or falls back to system UPI chooser.
     */
    fun buildGooglePayIntent(info: UpiPaymentInfo, context: Context? = null): Intent {
        val uri = buildPaymentUri(info)
        val isGPayAvailable = context?.let { isGooglePayInstalled(it) } ?: true
        return if (isGPayAvailable) {
            Intent(Intent.ACTION_VIEW, uri).setPackage(GOOGLE_PAY_PACKAGE)
        } else {
            Intent(Intent.ACTION_VIEW, uri)
        }
    }

    /** Play Store fallback URI for a UPI app, when none is installed. */
    fun marketUri(packageName: String): Uri =
        Uri.parse("market://details?id=$packageName")

    /** System settings page to grant access to the app (manifest <queries>). */
    fun systemSettingsIntent(): Intent = Intent(Settings.ACTION_SETTINGS)
}

data class UpiApp(
    val packageName: String,
    val label: String
)
