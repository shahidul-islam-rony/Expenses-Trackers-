package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class BalanceSummary(
    val initialWallet: Double = 0.0,
    val initialOnlineBank: Double = 0.0,
    val walletExpenses: Double = 0.0,
    val walletIncome: Double = 0.0,
    val onlineBankExpenses: Double = 0.0,
    val onlineBankIncome: Double = 0.0,
    val walletBalance: Double = 0.0,
    val onlineBankBalance: Double = 0.0,
    val totalNetBalance: Double = 0.0,
    val totalDue: Double = 0.0,
    val currencySymbol: String = "$"
)

class ExpenseRepository(private val appDao: AppDao) {

    val fundAccountFlow: Flow<FundAccount?> = appDao.getFundAccountFlow()
    val allTransactionsFlow: Flow<List<ExpenseTransaction>> = appDao.getAllTransactions()

    val balanceSummaryFlow: Flow<BalanceSummary> = combine(
        fundAccountFlow,
        allTransactionsFlow
    ) { fundAccount, transactions ->
        val initialWallet = fundAccount?.initialWallet ?: 0.0
        val initialBank = fundAccount?.initialOnlineBank ?: 0.0
        val symbol = fundAccount?.currencySymbol ?: "$"

        var walletExp = 0.0
        var walletInc = 0.0
        var bankExp = 0.0
        var bankInc = 0.0
        var duesAcc = 0.0

        for (tx in transactions) {
            when (tx.type) {
                TransactionType.EXPENSE -> {
                    when (tx.accountType) {
                        AccountType.WALLET -> walletExp += tx.amount
                        AccountType.ONLINE_BANKING -> bankExp += tx.amount
                        AccountType.NONE -> {}
                    }
                }
                TransactionType.INCOME -> {
                    when (tx.accountType) {
                        AccountType.WALLET -> walletInc += tx.amount
                        AccountType.ONLINE_BANKING -> bankInc += tx.amount
                        AccountType.NONE -> {}
                    }
                }
                TransactionType.DUE -> {
                    if (!tx.isCleared) {
                        duesAcc += tx.amount
                    }
                }
            }
        }

        val curWalletBalance = initialWallet + walletInc - walletExp
        val curBankBalance = initialBank + bankInc - bankExp
        val totalNet = curWalletBalance + curBankBalance

        BalanceSummary(
            initialWallet = initialWallet,
            initialOnlineBank = initialBank,
            walletExpenses = walletExp,
            walletIncome = walletInc,
            onlineBankExpenses = bankExp,
            onlineBankIncome = bankInc,
            walletBalance = curWalletBalance,
            onlineBankBalance = curBankBalance,
            totalNetBalance = totalNet,
            totalDue = duesAcc,
            currencySymbol = symbol
        )
    }

    suspend fun setInitialBalances(walletAmount: Double, bankAmount: Double, symbol: String = "$") {
        val currentAccount = appDao.getFundAccount()
        val updatedAccount = FundAccount(
            id = 1,
            initialWallet = walletAmount,
            initialOnlineBank = bankAmount,
            currencySymbol = symbol,
            lastUpdated = System.currentTimeMillis()
        )
        appDao.insertOrUpdateFundAccount(updatedAccount)
    }

    suspend fun addTransaction(transaction: ExpenseTransaction) {
        appDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: ExpenseTransaction) {
        appDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: ExpenseTransaction) {
        appDao.deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Int) {
        appDao.deleteTransactionById(id)
    }

    suspend fun seedSampleDataIfEmpty() {
        val fundAccount = appDao.getFundAccount()
        if (fundAccount == null) {
            // Setup default initial funds: e.g. Wallet = 250.0, Online Bank = 1200.0
            appDao.insertOrUpdateFundAccount(
                FundAccount(
                    id = 1,
                    initialWallet = 250.0,
                    initialOnlineBank = 1200.0,
                    currencySymbol = "$",
                    lastUpdated = System.currentTimeMillis()
                )
            )

            // Seed a few initial transactions so the user sees live data immediately
            val now = System.currentTimeMillis()
            val dayMs = 86400000L

            appDao.insertTransaction(
                ExpenseTransaction(
                    type = TransactionType.EXPENSE,
                    amount = 42.50,
                    category = "Groceries",
                    accountType = AccountType.WALLET,
                    title = "Weekly Groceries & Snacks",
                    note = "Apples, Milk, Vegetables, Bread",
                    timestamp = now - (0.1 * dayMs).toLong()
                )
            )

            appDao.insertTransaction(
                ExpenseTransaction(
                    type = TransactionType.EXPENSE,
                    amount = 18.00,
                    category = "Dining",
                    accountType = AccountType.ONLINE_BANKING,
                    title = "Lunch at Bistro",
                    note = "Paid with Debit Card",
                    timestamp = now - (0.5 * dayMs).toLong()
                )
            )

            appDao.insertTransaction(
                ExpenseTransaction(
                    type = TransactionType.EXPENSE,
                    amount = 65.00,
                    category = "Utilities",
                    accountType = AccountType.ONLINE_BANKING,
                    title = "Electricity & Wi-Fi Bill",
                    note = "Monthly auto pay",
                    timestamp = now - (1.2 * dayMs).toLong()
                )
            )

            appDao.insertTransaction(
                ExpenseTransaction(
                    type = TransactionType.DUE,
                    amount = 35.00,
                    category = "Borrowed/Due",
                    accountType = AccountType.NONE,
                    title = "Due to Alex for Concert Ticket",
                    note = "Pay back by weekend",
                    timestamp = now - (1.8 * dayMs).toLong(),
                    isCleared = false
                )
            )
        }
    }
}
