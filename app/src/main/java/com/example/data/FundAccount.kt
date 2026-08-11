package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fund_accounts")
data class FundAccount(
    @PrimaryKey val id: Int = 1,
    val initialWallet: Double = 0.0,
    val initialOnlineBank: Double = 0.0,
    val currencySymbol: String = "$",
    val lastUpdated: Long = System.currentTimeMillis()
)
