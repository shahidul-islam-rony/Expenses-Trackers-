package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AccountType
import com.example.data.AppDatabase
import com.example.data.BalanceSummary
import com.example.data.CategorySpendSummary
import com.example.data.DailySpendSummary
import com.example.data.ExpenseCategory
import com.example.data.ExpenseRepository
import com.example.data.ExpenseTransaction
import com.example.data.MonthlyReportData
import com.example.data.TransactionType
import com.example.sms.SmsScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class TransactionFilterState(
    val query: String = "",
    val timeframe: String = "ALL",
    val type: String = "ALL",
    val account: String = "ALL",
    val category: String = "ALL"
)

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExpenseRepository

    val balanceSummary: StateFlow<BalanceSummary>
    val rawTransactions: StateFlow<List<ExpenseTransaction>>
    val allCategories: StateFlow<List<ExpenseCategory>>

    // Comprehensive Filters
    val searchQuery = MutableStateFlow("")
    val selectedTimeframeFilter = MutableStateFlow("ALL") // ALL, TODAY, THIS_WEEK, THIS_MONTH, LAST_MONTH
    val selectedTypeFilter = MutableStateFlow("ALL") // ALL, EXPENSE, INCOME, DUE
    val selectedAccountFilter = MutableStateFlow("ALL") // ALL, WALLET, ONLINE_BANKING
    val selectedCategoryFilter = MutableStateFlow("ALL") // ALL or category name

    val filteredTransactions: StateFlow<List<ExpenseTransaction>>
    val activeFilterCount: StateFlow<Int>

    // Monthly Report State (Month Offset from current month: 0 = current, -1 = last month, etc.)
    val selectedReportMonthOffset = MutableStateFlow(0)
    val monthlyReport: StateFlow<MonthlyReportData>

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

        allCategories = repository.allCategoriesFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf(
                ExpenseCategory(name = "Groceries", iconEmoji = "🛒", isCustom = false),
                ExpenseCategory(name = "Dining", iconEmoji = "🍔", isCustom = false),
                ExpenseCategory(name = "Utilities", iconEmoji = "💡", isCustom = false),
                ExpenseCategory(name = "Transport", iconEmoji = "🚖", isCustom = false),
                ExpenseCategory(name = "Shopping", iconEmoji = "🛍️", isCustom = false),
                ExpenseCategory(name = "Bills", iconEmoji = "📄", isCustom = false),
                ExpenseCategory(name = "Healthcare", iconEmoji = "🏥", isCustom = false),
                ExpenseCategory(name = "Entertainment", iconEmoji = "🎬", isCustom = false),
                ExpenseCategory(name = "Salary", iconEmoji = "💰", isCustom = false),
                ExpenseCategory(name = "Due/Debt", iconEmoji = "⏳", isCustom = false),
                ExpenseCategory(name = "Other", iconEmoji = "📦", isCustom = false)
            )
        )

        activeFilterCount = combine(
            searchQuery,
            selectedTimeframeFilter,
            selectedTypeFilter,
            selectedAccountFilter,
            selectedCategoryFilter
        ) { q, tf, tp, acc, cat ->
            var count = 0
            if (q.isNotBlank()) count++
            if (tf != "ALL") count++
            if (tp != "ALL") count++
            if (acc != "ALL") count++
            if (cat != "ALL") count++
            count
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

        val filterStateFlow = combine(
            searchQuery,
            selectedTimeframeFilter,
            selectedTypeFilter,
            selectedAccountFilter,
            selectedCategoryFilter
        ) { q, tf, tp, acc, cat ->
            TransactionFilterState(
                query = q,
                timeframe = tf,
                type = tp,
                account = acc,
                category = cat
            )
        }

        filteredTransactions = combine(
            rawTransactions,
            filterStateFlow
        ) { list, filter ->
            val (startTime, endTime) = getTimeframeRange(filter.timeframe)

            list.filter { tx ->
                // Timeframe Filter
                val matchesTimeframe = (startTime == null || tx.timestamp >= startTime) &&
                        (endTime == null || tx.timestamp <= endTime)

                // Type Filter
                val matchesType = when (filter.type) {
                    "ALL" -> true
                    "EXPENSE" -> tx.type == TransactionType.EXPENSE
                    "INCOME" -> tx.type == TransactionType.INCOME
                    "DUE" -> tx.type == TransactionType.DUE
                    else -> true
                }

                // Account Filter
                val matchesAccount = when (filter.account) {
                    "ALL" -> true
                    "WALLET" -> tx.accountType == AccountType.WALLET
                    "ONLINE_BANKING" -> tx.accountType == AccountType.ONLINE_BANKING
                    else -> true
                }

                // Category Filter
                val matchesCategory = when (filter.category) {
                    "ALL" -> true
                    else -> tx.category.equals(filter.category, ignoreCase = true)
                }

                // Query Search (matches title, note, category, or amount)
                val matchesQuery = if (filter.query.isBlank()) {
                    true
                } else {
                    val q = filter.query.trim().lowercase(Locale.getDefault())
                    tx.title.lowercase(Locale.getDefault()).contains(q) ||
                            tx.note.lowercase(Locale.getDefault()).contains(q) ||
                            tx.category.lowercase(Locale.getDefault()).contains(q) ||
                            String.format(Locale.US, "%.2f", tx.amount).contains(q)
                }

                matchesTimeframe && matchesType && matchesAccount && matchesCategory && matchesQuery
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Monthly Report Calculation Flow
        monthlyReport = combine(
            rawTransactions,
            allCategories,
            balanceSummary,
            selectedReportMonthOffset
        ) { transactions, categories, summary, monthOffset ->
            computeMonthlyReport(transactions, categories, summary.currencySymbol, monthOffset)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MonthlyReportData()
        )
    }

    private fun getTimeframeRange(timeframe: String): Pair<Long?, Long?> {
        val cal = Calendar.getInstance()
        return when (timeframe) {
            "TODAY" -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            "THIS_WEEK" -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_WEEK, 6)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            "THIS_MONTH" -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            "LAST_MONTH" -> {
                cal.add(Calendar.MONTH, -1)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            else -> Pair(null, null)
        }
    }

    private fun computeMonthlyReport(
        transactions: List<ExpenseTransaction>,
        categories: List<ExpenseCategory>,
        currencySymbol: String,
        monthOffset: Int
    ): MonthlyReportData {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, monthOffset)
        val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val monthTitle = monthYearFormat.format(cal.time)
        val year = cal.get(Calendar.YEAR)
        val monthIndex = cal.get(Calendar.MONTH)

        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfMonth = cal.timeInMillis

        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        cal.set(Calendar.DAY_OF_MONTH, maxDay)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endOfMonth = cal.timeInMillis

        val monthTxs = transactions.filter { it.timestamp in startOfMonth..endOfMonth }

        var totalExp = 0.0
        var totalInc = 0.0
        val categoryTotals = mutableMapOf<String, Double>()
        val categoryCounts = mutableMapOf<String, Int>()
        val dailyMap = mutableMapOf<Int, Double>()

        for (day in 1..maxDay) {
            dailyMap[day] = 0.0
        }

        for (tx in monthTxs) {
            when (tx.type) {
                TransactionType.EXPENSE -> {
                    totalExp += tx.amount
                    categoryTotals[tx.category] = (categoryTotals[tx.category] ?: 0.0) + tx.amount
                    categoryCounts[tx.category] = (categoryCounts[tx.category] ?: 0) + 1

                    val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                    val day = txCal.get(Calendar.DAY_OF_MONTH)
                    dailyMap[day] = (dailyMap[day] ?: 0.0) + tx.amount
                }
                TransactionType.INCOME -> {
                    totalInc += tx.amount
                }
                TransactionType.DUE -> {}
            }
        }

        val emojiMap = categories.associate { it.name.lowercase(Locale.getDefault()) to it.iconEmoji }

        val categoryBreakdown = categoryTotals.entries.map { (catName, amount) ->
            val emoji = emojiMap[catName.lowercase(Locale.getDefault())] ?: "📁"
            val pct = if (totalExp > 0) ((amount / totalExp) * 100).toFloat() else 0f
            CategorySpendSummary(
                categoryName = catName,
                iconEmoji = emoji,
                totalAmount = amount,
                percentage = pct,
                transactionCount = categoryCounts[catName] ?: 0
            )
        }.sortedByDescending { it.totalAmount }

        val dailyBreakdown = dailyMap.entries.map { (day, amount) ->
            DailySpendSummary(
                dayOfMonth = day,
                dayLabel = "Day $day",
                amount = amount
            )
        }.sortedBy { it.dayOfMonth }

        val topTransactions = monthTxs
            .filter { it.type == TransactionType.EXPENSE }
            .sortedByDescending { it.amount }
            .take(5)

        return MonthlyReportData(
            monthTitle = monthTitle,
            year = year,
            monthIndex = monthIndex,
            totalExpenses = totalExp,
            totalIncome = totalInc,
            netCashFlow = totalInc - totalExp,
            transactionCount = monthTxs.size,
            categoryBreakdown = categoryBreakdown,
            dailyBreakdown = dailyBreakdown,
            topTransactions = topTransactions,
            currencySymbol = currencySymbol
        )
    }

    fun setReportMonthOffset(offset: Int) {
        selectedReportMonthOffset.value = offset
    }

    fun navigateReportMonth(delta: Int) {
        selectedReportMonthOffset.value += delta
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

    fun deleteMultipleTransactions(ids: Set<Int>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.deleteTransactionsByIds(ids.toList())
        }
    }

    fun addCustomCategory(name: String, iconEmoji: String = "📁") {
        if (name.isBlank()) return
        viewModelScope.launch {
            val cat = ExpenseCategory(
                name = name.trim(),
                iconEmoji = iconEmoji.ifBlank { "📁" },
                isCustom = true
            )
            repository.addCategory(cat)
        }
    }

    fun deleteCategory(category: ExpenseCategory) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    // --- Filter Setters ---
    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setTimeframeFilter(timeframe: String) {
        selectedTimeframeFilter.value = timeframe
    }

    fun setTypeFilter(type: String) {
        selectedTypeFilter.value = type
    }

    fun setAccountFilter(account: String) {
        selectedAccountFilter.value = account
    }

    fun setCategoryFilter(category: String) {
        selectedCategoryFilter.value = category
    }

    fun resetFilters() {
        searchQuery.value = ""
        selectedTimeframeFilter.value = "ALL"
        selectedTypeFilter.value = "ALL"
        selectedAccountFilter.value = "ALL"
        selectedCategoryFilter.value = "ALL"
    }

    // --- SMS & Email Detection Actions ---
    fun scanSmsInbox(context: Context) {
        viewModelScope.launch {
            isScanningSms.value = true
            val activeCurrency = balanceSummary.value.currencySymbol.ifBlank { "BDT" }
            smsScanStatusMessage.value = "Scanning SMS inbox for $activeCurrency transactions..."

            val detected = SmsScanner.scanInboxForTransactions(context, targetCurrencyCode = activeCurrency)

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
