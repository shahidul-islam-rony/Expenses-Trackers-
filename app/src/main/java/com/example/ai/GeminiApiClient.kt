package com.example.ai

import com.example.BuildConfig
import com.example.data.BalanceSummary
import com.example.data.ExpenseTransaction
import com.example.data.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageSender {
    USER,
    GEMINI,
    SYSTEM
}

object GeminiApiClient {

    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    suspend fun generateChatResponse(
        userPrompt: String,
        conversationHistory: List<ChatMessage>,
        balanceSummary: BalanceSummary,
        transactions: List<ExpenseTransaction>
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is missing or not configured. Please add your GEMINI_API_KEY in the AI Studio Secrets panel.")
            )
        }

        try {
            val systemInstructionText = buildSystemInstruction(balanceSummary, transactions)

            val contentsArray = JSONArray()

            // Include past turns for context (up to last 10 messages)
            val recentHistory = conversationHistory.takeLast(10)
            for (msg in recentHistory) {
                if (msg.sender == MessageSender.SYSTEM) continue
                val role = if (msg.sender == MessageSender.USER) "user" else "model"
                val turnObj = JSONObject()
                turnObj.put("role", role)
                val partsArr = JSONArray()
                val partObj = JSONObject()
                partObj.put("text", msg.text)
                partsArr.put(partObj)
                turnObj.put("parts", partsArr)
                contentsArray.put(turnObj)
            }

            // Current user turn if not already appended
            if (recentHistory.lastOrNull()?.text != userPrompt) {
                val currentTurn = JSONObject()
                currentTurn.put("role", "user")
                val partsArr = JSONArray()
                val partObj = JSONObject()
                partObj.put("text", userPrompt)
                partsArr.put(partObj)
                currentTurn.put("parts", partsArr)
                contentsArray.put(currentTurn)
            }

            val rootJson = JSONObject()
            rootJson.put("contents", contentsArray)

            // System instruction
            val sysInstObj = JSONObject()
            val sysParts = JSONArray()
            val sysPartObj = JSONObject()
            sysPartObj.put("text", systemInstructionText)
            sysParts.put(sysPartObj)
            sysInstObj.put("parts", sysParts)
            rootJson.put("systemInstruction", sysInstObj)

            val url = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = rootJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = try {
                    val errJson = JSONObject(responseBodyString)
                    errJson.optJSONObject("error")?.optString("message") ?: "API call failed (${response.code})"
                } catch (e: Exception) {
                    "HTTP ${response.code}: ${response.message}"
                }
                return@withContext Result.failure(Exception(errorMsg))
            }

            val respJson = JSONObject(responseBodyString)
            val candidates = respJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCand = candidates.getJSONObject(0)
                val contentObj = firstCand.optJSONObject("content")
                val parts = contentObj?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val text = parts.getJSONObject(0).optString("text")
                    if (text.isNotBlank()) {
                        return@withContext Result.success(text)
                    }
                }
            }

            Result.failure(Exception("No response text returned from Gemini API."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildSystemInstruction(
        summary: BalanceSummary,
        transactions: List<ExpenseTransaction>
    ): String {
        val todayStr = SimpleDateFormat("EEEE, MMMM dd, yyyy (yyyy-MM-dd)", Locale.getDefault()).format(Date())

        val sb = StringBuilder()
        sb.appendLine("You are an intelligent, friendly AI financial assistant built inside the Expense Tracker Android application.")
        sb.appendLine("You have direct, real-time access to the user's complete transaction database and balance records.")
        sb.appendLine("Today's Date: $todayStr")
        sb.appendLine()
        sb.appendLine("=== CURRENT FUND SUMMARY ===")
        sb.appendLine("Currency Symbol: ${summary.currencySymbol}")
        sb.appendLine("Wallet Balance: ${summary.currencySymbol}${String.format(Locale.US, "%.2f", summary.walletBalance)} (Initial: ${summary.currencySymbol}${String.format(Locale.US, "%.2f", summary.initialWallet)}, Expenses: ${summary.currencySymbol}${String.format(Locale.US, "%.2f", summary.walletExpenses)}, Income: ${summary.currencySymbol}${String.format(Locale.US, "%.2f", summary.walletIncome)})")
        sb.appendLine("Online Banking Balance: ${summary.currencySymbol}${String.format(Locale.US, "%.2f", summary.onlineBankBalance)} (Initial: ${summary.currencySymbol}${String.format(Locale.US, "%.2f", summary.initialOnlineBank)}, Expenses: ${summary.currencySymbol}${String.format(Locale.US, "%.2f", summary.onlineBankExpenses)}, Income: ${summary.currencySymbol}${String.format(Locale.US, "%.2f", summary.onlineBankIncome)})")
        sb.appendLine("Total Net Available Balance: ${summary.currencySymbol}${String.format(Locale.US, "%.2f", summary.totalNetBalance)}")
        sb.appendLine("Total Outstanding Dues: ${summary.currencySymbol}${String.format(Locale.US, "%.2f", summary.totalDue)}")
        sb.appendLine()
        sb.appendLine("=== TRANSACTION RECORDS (${transactions.size} TOTAL) ===")

        if (transactions.isEmpty()) {
            sb.appendLine("No transactions recorded yet.")
        } else {
            for (tx in transactions) {
                val dateStr = dateFormat.format(Date(tx.timestamp))
                val clearedStatus = if (tx.type == TransactionType.DUE) " (Cleared: ${tx.isCleared})" else ""
                sb.appendLine("- ID:${tx.id} | Date: $dateStr | Type: ${tx.type} | Amount: ${summary.currencySymbol}${String.format(Locale.US, "%.2f", tx.amount)} | Category: ${tx.category} | Account: ${tx.accountType} | Title: \"${tx.title}\" | Note: \"${tx.note}\"$clearedStatus")
            }
        }

        sb.appendLine()
        sb.appendLine("=== INSTRUCTIONS FOR ANSWERING ===")
        sb.appendLine("1. When asked about 'last month expenses' or specific time periods (e.g., last month, this month, today, past week), compute the exact total expenses from the timestamp dates of the transactions provided above.")
        sb.appendLine("2. Be precise with currency numbers and categories.")
        sb.appendLine("3. Present numerical breakdowns clearly using bullet points and bold text.")
        sb.appendLine("4. Maintain a warm, encouraging tone while providing actionable financial insights.")

        return sb.toString()
    }
}
