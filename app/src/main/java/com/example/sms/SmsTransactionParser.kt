package com.example.sms

import com.example.data.AccountType
import com.example.data.ExpenseTransaction
import com.example.data.TransactionType
import java.util.Locale
import java.util.regex.Pattern

data class ParsedSmsTransaction(
    val amount: Double,
    val type: TransactionType,
    val accountType: AccountType,
    val category: String,
    val title: String,
    val note: String,
    val originalSms: String,
    val timestamp: Long
)

object SmsTransactionParser {

    // Regex patterns for amounts: e.g. Rs 1,250.00, Rs. 50, INR 499.00, $15.50, USD 20, debited by 150.00
    private val AMOUNT_PATTERN = Pattern.compile(
        "(?:rs|inr|usd|\\$|\\u20b9)?\\.?\\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)",
        Pattern.CASE_INSENSITIVE
    )

    private val DEBIT_KEYWORDS = listOf("debited", "spent", "paid", "withdrawn", "purchase", "sent to", "txn of rs", "debit")
    private val CREDIT_KEYWORDS = listOf("credited", "received", "deposited", "added", "refunded", "credit")

    fun parseSms(messageBody: String, sender: String = "", timestamp: Long = System.currentTimeMillis()): ParsedSmsTransaction? {
        val lowerBody = messageBody.lowercase(Locale.ROOT)

        // Filter out non-financial SMS (must contain transaction keywords)
        val isDebit = DEBIT_KEYWORDS.any { lowerBody.contains(it) }
        val isCredit = CREDIT_KEYWORDS.any { lowerBody.contains(it) }

        if (!isDebit && !isCredit) {
            return null
        }

        // Extract amount
        val amount = extractAmount(messageBody) ?: return null
        if (amount <= 0.0) return null

        val type = if (isCredit && !isDebit) TransactionType.INCOME else TransactionType.EXPENSE

        // Determine Account Type
        val accountType = if (lowerBody.contains("wallet") || lowerBody.contains("paytm") || lowerBody.contains("gpay")) {
            AccountType.WALLET
        } else {
            AccountType.ONLINE_BANKING
        }

        // Infer merchant / payee and category
        val (title, category) = inferTitleAndCategory(messageBody, lowerBody, type, sender)

        val note = "Auto-detected from SMS: ${if (sender.isNotBlank()) "Sender ($sender)" else "Bank SMS"}"

        return ParsedSmsTransaction(
            amount = amount,
            type = type,
            accountType = accountType,
            category = category,
            title = title,
            note = note,
            originalSms = messageBody,
            timestamp = timestamp
        )
    }

    private fun extractAmount(text: String): Double? {
        // Look for explicit "rs 123", "inr 123", "$ 123", "debited by 123"
        val explicitMatcher = Pattern.compile(
            "(?:debited|credited|spent|paid|rs\\.?|inr|usd|\\$|\\u20b9|amount|val)\\s*:?\\s*(?:rs\\.?|inr|usd|\\$|\\u20b9)?\\s*([0-9,]+\\.[0-9]{1,2}|[0-9,]+)",
            Pattern.CASE_INSENSITIVE
        ).matcher(text)

        if (explicitMatcher.find()) {
            val numStr = explicitMatcher.group(1)?.replace(",", "")
            val parsed = numStr?.toDoubleOrNull()
            if (parsed != null && parsed > 0.0) return parsed
        }

        // Fallback general pattern
        val matcher = AMOUNT_PATTERN.matcher(text)
        while (matcher.find()) {
            val numStr = matcher.group(1)?.replace(",", "")
            val parsed = numStr?.toDoubleOrNull()
            if (parsed != null && parsed > 0.0 && parsed < 10000000.0) {
                return parsed
            }
        }
        return null
    }

    private fun inferTitleAndCategory(
        originalBody: String,
        lowerBody: String,
        type: TransactionType,
        sender: String
    ): Pair<String, String> {
        // Look for "at [Merchant]" or "to [Merchant]" or "info: [Merchant]"
        val atMatcher = Pattern.compile("(?:at|to|info|vpa|info\\*|towards)\\s+([A-Za-z0-9\\s&.-]{3,25})", Pattern.CASE_INSENSITIVE).matcher(originalBody)
        var merchantName = ""
        if (atMatcher.find()) {
            merchantName = atMatcher.group(1)?.trim() ?: ""
            // Clean trailing words like "on", "ref", "date", "via"
            merchantName = merchantName.split(" on ", " ref ", " date ", " via ", " avl ", " bal ")[0].trim()
        }

        if (merchantName.isBlank()) {
            merchantName = if (sender.isNotBlank()) "SMS Transaction ($sender)" else "Bank SMS Transaction"
        }

        // Categorize based on keywords
        val category = when {
            lowerBody.contains("grocery") || lowerBody.contains("supermarket") || lowerBody.contains("mart") || lowerBody.contains("bigbasket") || lowerBody.contains("blinkit") -> "Groceries"
            lowerBody.contains("swiggy") || lowerBody.contains("zomato") || lowerBody.contains("restaurant") || lowerBody.contains("cafe") || lowerBody.contains("food") || lowerBody.contains("bistro") -> "Dining"
            lowerBody.contains("uber") || lowerBody.contains("ola") || lowerBody.contains("metro") || lowerBody.contains("fuel") || lowerBody.contains("petrol") || lowerBody.contains("transport") -> "Transport"
            lowerBody.contains("amazon") || lowerBody.contains("flipkart") || lowerBody.contains("myntra") || lowerBody.contains("shopping") || lowerBody.contains("mall") -> "Shopping"
            lowerBody.contains("electricity") || lowerBody.contains("bill") || lowerBody.contains("wifi") || lowerBody.contains("broadband") || lowerBody.contains("water") || lowerBody.contains("recharge") -> "Utilities"
            lowerBody.contains("salary") || lowerBody.contains("deposit") || type == TransactionType.INCOME -> "Income/Deposit"
            else -> "Other"
        }

        val title = if (merchantName.length > 35) merchantName.substring(0, 35) else merchantName
        return Pair(title, category)
    }

    fun ParsedSmsTransaction.toExpenseTransaction(): ExpenseTransaction {
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
