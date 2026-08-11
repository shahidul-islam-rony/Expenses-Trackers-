package com.example.sms

import com.example.data.ExpenseTransaction
import com.example.sms.MessageTransactionParser.toExpenseTransaction

object EmailScanner {

    fun parseEmailText(
        emailText: String,
        targetCurrencyCode: String,
        emailSender: String = "Email Alert"
    ): ExpenseTransaction? {
        val parsed = MessageTransactionParser.parseMessage(
            text = emailText,
            targetCurrencyCode = targetCurrencyCode,
            senderOrSource = emailSender,
            sourceType = "EMAIL"
        )
        return parsed?.toExpenseTransaction()
    }
}
