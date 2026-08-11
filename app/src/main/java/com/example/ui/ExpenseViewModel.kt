package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.ChatMessage
import com.example.ai.GeminiApiClient
import com.example.ai.MessageSender
import com.example.data.AccountType
import com.example.data.AppDatabase
import com.example.data.BalanceSummary
import com.example.data.ExpenseRepository
import com.example.data.ExpenseTransaction
import com.example.data.TransactionType
import com.example.sms.SmsScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExpenseRepository

    val balanceSummary: StateFlow<BalanceSummary>
    val rawTransactions: StateFlow<List<ExpenseTransaction>>

    val selectedCategoryFilter = MutableStateFlow("All")
    val selectedAccountFilter = MutableStateFlow("All")
    val searchQuery = MutableStateFlow("")

    val filteredTransactions: StateFlow<List<ExpenseTransaction>>

    // Gemini Chatbot State
    val chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = MessageSender.GEMINI,
                text = "Hello! I am your AI Expense Assistant. Ask me anything about your balances, spending trends, last month expenses, or dues!"
            )
        )
    )
    val isGeminiThinking = MutableStateFlow(false)
    val geminiError = MutableStateFlow<String?>(null)

    // SMS Scanning State
    val isScanningSms = MutableStateFlow(false)
    val detectedSmsTransactions = MutableStateFlow<List<ExpenseTransaction>>(emptyList())
    val smsScanStatusMessage = MutableStateFlow<String?>(null)

    init {
        val dao = AppDatabase.getDatabase(application).appDao()
        repository = ExpenseRepository(dao)

        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
        }

        balanceSummary = repository.balanceSummaryFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = BalanceSummary()
        )

        rawTransactions = repository.allTransactionsFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        filteredTransactions = combine(
            rawTransactions,
            selectedCategoryFilter,
            selectedAccountFilter,
            searchQuery
        ) { list, category, account, query ->
            list.filter { tx ->
                val matchesCategory = when (category) {
                    "All" -> true
                    "Dues" -> tx.type == TransactionType.DUE
                    else -> tx.category.equals(category, ignoreCase = true)
                }

                val matchesAccount = when (account) {
                    "All" -> true
                    "Wallet" -> tx.accountType == AccountType.WALLET
                    "Online Banking" -> tx.accountType == AccountType.ONLINE_BANKING
                    else -> true
                }

                val matchesQuery = query.isBlank() ||
                        tx.title.contains(query, ignoreCase = true) ||
                        tx.category.contains(query, ignoreCase = true) ||
                        tx.note.contains(query, ignoreCase = true)

                matchesCategory && matchesAccount && matchesQuery
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun addTransaction(
        type: TransactionType,
        amount: Double,
        category: String,
        accountType: AccountType,
        title: String,
        note: String
    ) {
        viewModelScope.launch {
            val tx = ExpenseTransaction(
                type = type,
                amount = amount,
                category = category,
                accountType = accountType,
                title = title,
                note = note,
                timestamp = System.currentTimeMillis(),
                isCleared = false
            )
            repository.addTransaction(tx)
        }
    }

    fun updateInitialBalances(walletAmount: Double, bankAmount: Double, symbol: String = "$") {
        viewModelScope.launch {
            repository.setInitialBalances(walletAmount, bankAmount, symbol)
        }
    }

    fun toggleDueCleared(transaction: ExpenseTransaction) {
        if (transaction.type != TransactionType.DUE) return
        viewModelScope.launch {
            val updated = transaction.copy(isCleared = !transaction.isCleared)
            repository.updateTransaction(updated)
        }
    }

    fun deleteTransaction(transaction: ExpenseTransaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun setCategoryFilter(category: String) {
        selectedCategoryFilter.value = category
    }

    fun setAccountFilter(account: String) {
        selectedAccountFilter.value = account
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    // --- Gemini Chatbot Actions ---
    fun sendChatMessage(userText: String) {
        if (userText.isBlank() || isGeminiThinking.value) return

        val userMsg = ChatMessage(sender = MessageSender.USER, text = userText.trim())
        chatMessages.value = chatMessages.value + userMsg

        isGeminiThinking.value = true
        geminiError.value = null

        viewModelScope.launch {
            val result = GeminiApiClient.generateChatResponse(
                userPrompt = userText.trim(),
                conversationHistory = chatMessages.value,
                balanceSummary = balanceSummary.value,
                transactions = rawTransactions.value
            )

            isGeminiThinking.value = false
            result.fold(
                onSuccess = { reply ->
                    chatMessages.value = chatMessages.value + ChatMessage(sender = MessageSender.GEMINI, text = reply)
                },
                onFailure = { err ->
                    val errorDetail = err.message ?: "Failed to reach Gemini API"
                    geminiError.value = errorDetail
                    chatMessages.value = chatMessages.value + ChatMessage(
                        sender = MessageSender.SYSTEM,
                        text = "⚠️ $errorDetail"
                    )
                }
            )
        }
    }

    fun clearChat() {
        chatMessages.value = listOf(
            ChatMessage(
                sender = MessageSender.GEMINI,
                text = "Chat history cleared. How can I assist with your expenses or bank balances today?"
            )
        )
        geminiError.value = null
    }

    // --- SMS & Email Detection Actions ---
    fun scanSmsInbox(context: Context) {
        viewModelScope.launch {
            isScanningSms.value = true
            val activeCurrency = balanceSummary.value.currencySymbol.ifBlank { "BDT" }
            smsScanStatusMessage.value = "Scanning SMS inbox for $activeCurrency transactions..."

            val detected = SmsScanner.scanInboxForTransactions(context, targetCurrencyCode = activeCurrency)

            // Deduplicate against existing database records (by checking title + amount + timestamp proximity)
            val existing = rawTransactions.value
            val newUnsaved = detected.filter { det ->
                existing.none { ext ->
                    ext.amount == det.amount &&
                            (ext.title.equals(det.title, ignoreCase = true) || Math.abs(ext.timestamp - det.timestamp) < 60000)
                }
            }

            detectedSmsTransactions.value = newUnsaved
            isScanningSms.value = false

            if (newUnsaved.isEmpty()) {
                smsScanStatusMessage.value = if (detected.isEmpty()) {
                    "No $activeCurrency debit/credit transactions found in SMS inbox. Messages in other currencies were automatically ignored."
                } else {
                    "All ${detected.size} $activeCurrency SMS transactions found in inbox have already been added!"
                }
            } else {
                smsScanStatusMessage.value = "Found ${newUnsaved.size} new $activeCurrency debit/credit transaction(s)!"
            }
        }
    }

    fun autoScanAndImportSms(context: Context) {
        viewModelScope.launch {
            val activeCurrency = balanceSummary.value.currencySymbol.ifBlank { "BDT" }
            val detected = SmsScanner.scanInboxForTransactions(context, targetCurrencyCode = activeCurrency)

            val existing = rawTransactions.value
            val newUnsaved = detected.filter { det ->
                existing.none { ext ->
                    ext.amount == det.amount &&
                            (ext.title.equals(det.title, ignoreCase = true) || Math.abs(ext.timestamp - det.timestamp) < 60000)
                }
            }

            if (newUnsaved.isNotEmpty()) {
                for (tx in newUnsaved) {
                    repository.addTransaction(tx)
                }
                smsScanStatusMessage.value = "Auto-added ${newUnsaved.size} new $activeCurrency transaction(s) from SMS inbox!"
            }
        }
    }

    fun scanEmailAlertText(emailText: String, sender: String = "Email Notification") {
        val activeCurrency = balanceSummary.value.currencySymbol.ifBlank { "BDT" }
        val tx = com.example.sms.EmailScanner.parseEmailText(emailText, activeCurrency, sender)

        if (tx != null) {
            val existing = rawTransactions.value
            val isDuplicate = existing.any { ext ->
                ext.amount == tx.amount && (ext.title.equals(tx.title, ignoreCase = true) || Math.abs(ext.timestamp - tx.timestamp) < 60000)
            }
            if (!isDuplicate) {
                detectedSmsTransactions.value = detectedSmsTransactions.value + tx
                smsScanStatusMessage.value = "Successfully detected 1 new $activeCurrency transaction from Email!"
            } else {
                smsScanStatusMessage.value = "This email transaction is already in your database."
            }
        } else {
            smsScanStatusMessage.value = "No $activeCurrency transaction detected in email text. (Non-matching currencies like USD/AED are automatically ignored)."
        }
    }

    fun importDetectedSmsTransactions(transactionsToImport: List<ExpenseTransaction>) {
        viewModelScope.launch {
            for (tx in transactionsToImport) {
                repository.addTransaction(tx)
            }
            detectedSmsTransactions.value = emptyList()
            smsScanStatusMessage.value = "Successfully imported ${transactionsToImport.size} transaction(s)!"
        }
    }

    fun clearSmsStatus() {
        smsScanStatusMessage.value = null
        detectedSmsTransactions.value = emptyList()
    }
}

