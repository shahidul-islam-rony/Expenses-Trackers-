package com.example.sms

import android.content.Context
import android.net.Uri
import com.example.data.ExpenseTransaction
import com.example.sms.MessageTransactionParser.toExpenseTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SmsScanner {

    suspend fun scanInboxForTransactions(
        context: Context,
        targetCurrencyCode: String,
        limit: Int = 100
    ): List<ExpenseTransaction> = withContext(Dispatchers.IO) {
        val detectedList = mutableListOf<ExpenseTransaction>()
        val uri = Uri.parse("content://sms/inbox")

        val projection = arrayOf("_id", "address", "body", "date")

        try {
            context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "date DESC"
            )?.use { cursor ->
                val bodyIndex = cursor.getColumnIndex("body")
                val addressIndex = cursor.getColumnIndex("address")
                val dateIndex = cursor.getColumnIndex("date")

                var count = 0
                while (cursor.moveToNext() && count < limit) {
                    val body = if (bodyIndex >= 0) cursor.getString(bodyIndex) else ""
                    val sender = if (addressIndex >= 0) cursor.getString(addressIndex) else ""
                    val date = if (dateIndex >= 0) cursor.getLong(dateIndex) else System.currentTimeMillis()

                    if (body.isNotBlank()) {
                        val parsed = MessageTransactionParser.parseMessage(
                            text = body,
                            targetCurrencyCode = targetCurrencyCode,
                            senderOrSource = sender,
                            sourceType = "SMS",
                            timestamp = date
                        )
                        if (parsed != null) {
                            detectedList.add(parsed.toExpenseTransaction())
                            count++
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext detectedList
    }
}
