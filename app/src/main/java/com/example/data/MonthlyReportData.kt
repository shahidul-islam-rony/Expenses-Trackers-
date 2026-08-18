package com.example.data

data class CategorySpendSummary(
    val categoryName: String,
    val iconEmoji: String,
    val totalAmount: Double,
    val percentage: Float, // 0.0 to 100.0
    val transactionCount: Int
)

data class DailySpendSummary(
    val dayOfMonth: Int,
    val dayLabel: String,
    val amount: Double
)

data class MonthlyReportData(
    val monthTitle: String = "",
    val year: Int = 2026,
    val monthIndex: Int = 0, // 0-based
    val totalExpenses: Double = 0.0,
    val totalIncome: Double = 0.0,
    val netCashFlow: Double = 0.0,
    val transactionCount: Int = 0,
    val categoryBreakdown: List<CategorySpendSummary> = emptyList(),
    val dailyBreakdown: List<DailySpendSummary> = emptyList(),
    val topTransactions: List<ExpenseTransaction> = emptyList(),
    val currencySymbol: String = "$"
)
