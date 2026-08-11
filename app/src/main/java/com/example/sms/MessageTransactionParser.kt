package com.example.sms

import com.example.data.AccountType
import com.example.data.ExpenseTransaction
import com.example.data.TransactionType
import java.util.Locale
import java.util.regex.Pattern

data class ParsedMessageTransaction(
    val amount: Double,
    val currencyFound: String, // e.g. "AED", "BDT", "USD", "INR", etc.
    val type: TransactionType,
    val accountType: AccountType,
    val category: String,
    val title: String,
    val note: String,
    val originalText: String,
    val source: String, // "SMS", "EMAIL", "NOTIFICATION"
    val timestamp: Long
)

object MessageTransactionParser {

    private val DEBIT_KEYWORDS = listOf(
        "debited", "spent", "paid", "withdrawn", "purchase", "sent to", "debit",
        "charged", "sent", "payment of", "payment to", "txn of"
    )
    private val CREDIT_KEYWORDS = listOf(
        "credited", "received", "deposited", "added", "refunded", "credit",
        "salary", "cashback", "inward"
    )

    fun parseMessage(
        text: String,
        targetCurrencyCode: String, // e.g. "AED", "BDT", "USD", "EUR", "INR"
        senderOrSource: String = "Auto Detector",
        sourceType: String = "SMS/Email",
        timestamp: Long = System.currentTimeMillis()
    ): ParsedMessageTransaction? {
        if (text.isBlank()) return null
        val lowerText = text.lowercase(Locale.ROOT)

        // Must contain a financial keyword
        val isDebit = DEBIT_KEYWORDS.any { lowerText.contains(it) }
        val isCredit = CREDIT_KEYWORDS.any { lowerText.contains(it) }

        if (!isDebit && !isCredit) {
            return null
        }

        // Detect currency in message
        val detectedCurrency = detectCurrency(lowerText)

        // Perform strict currency filtering based on targetCurrencyCode
        if (!isCurrencyMatching(detectedCurrency, targetCurrencyCode, lowerText)) {
            // Rejects transactions in non-matching currencies
            return null
        }

        val amount = extractAmount(text) ?: return null
        if (amount <= 0.0) return null

        val type = if (isCredit && !isDebit) TransactionType.INCOME else TransactionType.EXPENSE

        val accountType = when {
            lowerText.contains("wallet") || lowerText.contains("paytm") || lowerText.contains("gpay") ||
                    lowerText.contains("bkash") || lowerText.contains("nagad") || lowerText.contains("rocket") -> AccountType.WALLET
            else -> AccountType.ONLINE_BANKING
        }

        val (title, category) = inferTitleAndCategory(text, lowerText, type, senderOrSource)
        val note = "Auto-detected ($sourceType): $senderOrSource • Currency: ${detectedCurrency.ifBlank { targetCurrencyCode.uppercase(Locale.ROOT) }}"

        return ParsedMessageTransaction(
            amount = amount,
            currencyFound = if (detectedCurrency.isNotBlank()) detectedCurrency else targetCurrencyCode.uppercase(Locale.ROOT),
            type = type,
            accountType = accountType,
            category = category,
            title = title,
            note = note,
            originalText = text,
            source = sourceType,
            timestamp = timestamp
        )
    }

    /**
     * Identifies currency indicators in the message body.
     */
    fun detectCurrency(lowerText: String): String {
        return when {
            lowerText.contains("aed") || lowerText.contains("dhs") || lowerText.contains("dirham") -> "AED"
            lowerText.contains("bdt") || lowerText.contains("৳") || lowerText.contains("tk") || lowerText.contains("taka") -> "BDT"
            lowerText.contains("usd") || lowerText.contains("dollar") || lowerText.contains("$") -> "USD"
            lowerText.contains("eur") || lowerText.contains("euro") || lowerText.contains("€") -> "EUR"
            lowerText.contains("inr") || lowerText.contains("₹") || lowerText.contains("rs") || lowerText.contains("rupee") -> "INR"
            lowerText.contains("gbp") || lowerText.contains("£") || lowerText.contains("pound") -> "GBP"
            lowerText.contains("sar") || lowerText.contains("riyal") -> "SAR"
            lowerText.contains("qar") -> "QAR"
            else -> ""
        }
    }

    /**
     * Strict check comparing detected currency against selected target currency.
     */
    private fun isCurrencyMatching(detectedCurrency: String, targetCurrencyCode: String, lowerText: String): Boolean {
        val normalizedTarget = targetCurrencyCode.uppercase(Locale.ROOT).trim()

        val targetFamily = when {
            normalizedTarget == "AED" || normalizedTarget.contains("DHS") || normalizedTarget.contains("DIRHAM") -> "AED"
            normalizedTarget == "BDT" || normalizedTarget.contains("TAKA") || normalizedTarget == "TK" || normalizedTarget.contains("৳") -> "BDT"
            normalizedTarget == "USD" || normalizedTarget.contains("$") -> "USD"
            normalizedTarget == "EUR" || normalizedTarget.contains("€") -> "EUR"
            normalizedTarget == "INR" || normalizedTarget.contains("₹") || normalizedTarget.contains("RS") -> "INR"
            normalizedTarget == "GBP" || normalizedTarget.contains("£") -> "GBP"
            normalizedTarget == "SAR" -> "SAR"
            normalizedTarget == "QAR" -> "QAR"
            else -> normalizedTarget
        }

        // If an explicit currency was detected in text
        if (detectedCurrency.isNotBlank()) {
            return detectedCurrency.equals(targetFamily, ignoreCase = true)
        }

        // If no explicit currency symbol was detected in text:
        // Check if competing currency keywords exist in text
        val competingCurrencies = mapOf(
            "AED" to listOf("aed", "dhs", "dirham"),
            "BDT" to listOf("bdt", "taka", "tk", "৳"),
            "USD" to listOf("usd", "$", "dollar"),
            "EUR" to listOf("eur", "€", "euro"),
            "INR" to listOf("inr", "₹", "rs"),
            "GBP" to listOf("gbp", "£")
        )

        val targetKeywords = competingCurrencies[targetFamily] ?: listOf(targetFamily.lowercase(Locale.ROOT))

        for ((family, keywords) in competingCurrencies) {
            if (family != targetFamily) {
                if (keywords.any { lowerText.contains(it) }) {
                    return false // Message explicitly contains a competing currency, reject it!
                }
            }
        }

        return true
    }

    private fun extractAmount(text: String): Double? {
        val explicitMatcher = Pattern.compile(
            "(?:debited|credited|spent|paid|amount|val|txn|aed|bdt|usd|eur|inr|rs\\.?|tk\\.?|৳|\\$|\\u20b9|\\u20ac)\\s*:?\\s*(?:aed|bdt|usd|eur|inr|rs\\.?|tk\\.?|৳|\\$|\\u20b9|\\u20ac)?\\s*([0-9,]+\\.[0-9]{1,2}|[0-9,]+)",
            Pattern.CASE_INSENSITIVE
        ).matcher(text)

        if (explicitMatcher.find()) {
            val numStr = explicitMatcher.group(1)?.replace(",", "")
            val parsed = numStr?.toDoubleOrNull()
            if (parsed != null && parsed > 0.0) return parsed
        }

        val generalMatcher = Pattern.compile(
            "([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)"
        ).matcher(text)

        while (generalMatcher.find()) {
            val numStr = generalMatcher.group(1)?.replace(",", "")
            val parsed = numStr?.toDoubleOrNull()
            if (parsed != null && parsed > 0.0 && parsed < 10000000.0) {
                return parsed
            }
        }
        return null
    }

    private fun inferTitleAndCategory(
        originalText: String,
        lowerText: String,
        type: TransactionType,
        source: String
    ): Pair<String, String> {
        val atMatcher = Pattern.compile("(?:at|to|info|vpa|info\\*|towards|merchant|from)\\s+([A-Za-z0-9\\s&.-]{3,25})", Pattern.CASE_INSENSITIVE).matcher(originalText)
        var merchantName = ""
        if (atMatcher.find()) {
            merchantName = atMatcher.group(1)?.trim() ?: ""
            merchantName = merchantName.split(" on ", " ref ", " date ", " via ", " avl ", " bal ", " for ")[0].trim()
        }

        if (merchantName.isBlank()) {
            merchantName = if (source.isNotBlank()) "Transaction ($source)" else "Bank Notification"
        }

        val category = when {
            lowerText.contains("grocery") || lowerText.contains("supermarket") || lowerText.contains("mart") ||
                    lowerText.contains("bigbasket") || lowerText.contains("blinkit") || lowerText.contains("carrefour") ||
                    lowerText.contains("lulu") || lowerText.contains("meena") || lowerText.contains("swapno") || lowerText.contains("agora") -> "Groceries"

            lowerText.contains("swiggy") || lowerText.contains("zomato") || lowerText.contains("restaurant") ||
                    lowerText.contains("cafe") || lowerText.contains("food") || lowerText.contains("talabat") ||
                    lowerText.contains("bistro") || lowerText.contains("kfc") || lowerText.contains("dominos") -> "Dining"

            lowerText.contains("uber") || lowerText.contains("careem") || lowerText.contains("pathao") ||
                    lowerText.contains("ola") || lowerText.contains("metro") || lowerText.contains("fuel") ||
                    lowerText.contains("petrol") || lowerText.contains("transport") -> "Transport"

            lowerText.contains("amazon") || lowerText.contains("noon") || lowerText.contains("flipkart") ||
                    lowerText.contains("daraz") || lowerText.contains("myntra") || lowerText.contains("shopping") || lowerText.contains("mall") -> "Shopping"

            lowerText.contains("electricity") || lowerText.contains("dewa") || lowerText.contains("desco") ||
                    lowerText.contains("bill") || lowerText.contains("wifi") || lowerText.contains("broadband") ||
                    lowerText.contains("water") || lowerText.contains("recharge") -> "Utilities"

            lowerText.contains("salary") || lowerText.contains("deposit") || type == TransactionType.INCOME -> "Income/Deposit"
            else -> "Other"
        }

        val title = if (merchantName.length > 35) merchantName.substring(0, 35) else merchantName
        return Pair(title, category)
    }

    fun ParsedMessageTransaction.toExpenseTransaction(): ExpenseTransaction {
        return ExpenseTransaction(
            type = this.type,
            amount = this.amount,
            category = this.category,
            accountType = this.accountType,
            title = this.title,
            note = this.note,
            timestamp = this.timestamp,
            isCleared = false
        )
    }
}
