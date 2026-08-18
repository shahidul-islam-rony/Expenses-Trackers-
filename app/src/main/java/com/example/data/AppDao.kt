package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM fund_accounts WHERE id = 1 LIMIT 1")
    fun getFundAccountFlow(): Flow<FundAccount?>

    @Query("SELECT * FROM fund_accounts WHERE id = 1 LIMIT 1")
    suspend fun getFundAccount(): FundAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateFundAccount(account: FundAccount)

    @Query("SELECT * FROM expense_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<ExpenseTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: ExpenseTransaction)

    @Update
    suspend fun updateTransaction(transaction: ExpenseTransaction)

    @Delete
    suspend fun deleteTransaction(transaction: ExpenseTransaction)

    @Query("DELETE FROM expense_transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Int)

    @Query("DELETE FROM expense_transactions WHERE id IN (:ids)")
    suspend fun deleteTransactionsByIds(ids: List<Int>)

    @Query("DELETE FROM expense_transactions")
    suspend fun clearAllTransactions()

    @Query("SELECT * FROM expense_categories ORDER BY id ASC")
    fun getAllCategoriesFlow(): Flow<List<ExpenseCategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: ExpenseCategory)

    @Delete
    suspend fun deleteCategory(category: ExpenseCategory)
}
