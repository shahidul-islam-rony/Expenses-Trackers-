package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.data.BalanceSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ShareUtils {

    fun generateFundSummaryText(summary: BalanceSummary): String {
        val dateStr = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault()).format(Date())
        val symbol = summary.currencySymbol

        return """
            📊 *LIVE FUNDS & BALANCE SUMMARY*
            🗓️ Date: $dateStr
            ━━━━━━━━━━━━━━━━━━━━━━
            💰 *Total Net Funds*: $symbol${String.format(Locale.US, "%.2f", summary.totalNetBalance)}
            
            💵 *Wallet Cash*: $symbol${String.format(Locale.US, "%.2f", summary.walletBalance)}
            🏦 *Online Banking*: $symbol${String.format(Locale.US, "%.2f", summary.onlineBankBalance)}
            ⚠️ *Total Due/Liability*: $symbol${String.format(Locale.US, "%.2f", summary.totalDue)}
            ━━━━━━━━━━━━━━━━━━━━━━
            *Initial Funds*:
            • Wallet Start: $symbol${String.format(Locale.US, "%.2f", summary.initialWallet)}
            • Online Bank Start: $symbol${String.format(Locale.US, "%.2f", summary.initialOnlineBank)}
            
            *Total Spending*:
            • Wallet Expenses: $symbol${String.format(Locale.US, "%.2f", summary.walletExpenses)}
            • Bank Expenses: $symbol${String.format(Locale.US, "%.2f", summary.onlineBankExpenses)}
            
            Generated with Expense Tracker App 🚀
        """.trimIndent()
    }

    fun shareSummaryText(context: Context, summary: BalanceSummary) {
        val text = generateFundSummaryText(summary)
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Fund Summary")
        context.startActivity(shareIntent)
    }

    fun copyToClipboard(context: Context, summary: BalanceSummary) {
        val text = generateFundSummaryText(summary)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Fund Summary", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Fund summary copied to clipboard!", Toast.LENGTH_SHORT).show()
    }
}
