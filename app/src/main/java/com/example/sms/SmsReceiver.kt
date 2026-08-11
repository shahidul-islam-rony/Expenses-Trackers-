package com.example.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.data.AppDatabase
import com.example.data.ExpenseRepository
import com.example.sms.MessageTransactionParser.toExpenseTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val body = sms.messageBody ?: continue
                val sender = sms.originatingAddress ?: ""
                val timestamp = sms.timestampMillis

                val db = AppDatabase.getDatabase(context)
                val repository = ExpenseRepository(db.appDao())

                CoroutineScope(Dispatchers.IO).launch {
                    val fundAccount = db.appDao().getFundAccount()
                    val targetCurrency = fundAccount?.currencySymbol ?: "BDT"

                    val parsed = MessageTransactionParser.parseMessage(
                        text = body,
                        targetCurrencyCode = targetCurrency,
                        senderOrSource = sender,
                        sourceType = "SMS",
                        timestamp = timestamp
                    )

                    if (parsed != null) {
                        repository.addTransaction(parsed.toExpenseTransaction())
                    }
                }
            }
        }
    }
}
