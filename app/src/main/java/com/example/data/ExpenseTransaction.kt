package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType {
    EXPENSE,
    INCOME,
    DUE
}

enum class AccountType {
    WALLET,
    ONLINE_BANKING,
    NONE
}

@Entity(tableName = "expense_transactions")
data class ExpenseTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: TransactionType,
    val amount: Double,
    val category: String, // e.g. "Groceries", "Utilities", "Dining", "Transport", "Shopping", "Salary", "Other"
    val accountType: AccountType, // WALLET or ONLINE_BANKING or NONE
    val title: String,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isCleared: Boolean = false // relevant for DUE type items
)
