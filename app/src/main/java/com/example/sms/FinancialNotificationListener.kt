package com.example.sms

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.data.AppDatabase
import com.example.data.ExpenseRepository
import com.example.sms.MessageTransactionParser.toExpenseTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FinancialNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val packageName = sbn.packageName ?: ""
        val extras = sbn.notification?.extras ?: return

        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val bigText = extras.getCharSequence("android.bigText")?.toString() ?: ""

        val combinedContent = "$title $text $bigText".trim()
        if (combinedContent.isBlank()) return

        // Target apps: Email apps (Gmail, Outlook, Yahoo) or SMS apps
        val isFinancialApp = packageName.contains("gm") ||
                packageName.contains("mail") ||
                packageName.contains("outlook") ||
                packageName.contains("mms") ||
                packageName.contains("messaging") ||
                packageName.contains("sms") ||
                packageName.contains("bank") ||
                packageName.contains("pay") ||
                packageName.contains("bkash") ||
                packageName.contains("nagad")

        if (!isFinancialApp) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                val repository = ExpenseRepository(db.appDao())
                val fundAccount = db.appDao().getFundAccount()
                val currentCurrency = fundAccount?.currencySymbol ?: "BDT"

                val parsed = MessageTransactionParser.parseMessage(
                    text = combinedContent,
                    targetCurrencyCode = currentCurrency,
                    senderOrSource = title.ifBlank { packageName },
                    sourceType = if (packageName.contains("mail") || packageName.contains("gm") || packageName.contains("outlook")) "EMAIL" else "SMS"
                )

                if (parsed != null) {
                    repository.addTransaction(parsed.toExpenseTransaction())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
