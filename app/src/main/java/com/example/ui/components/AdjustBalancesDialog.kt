package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.BalanceSummary
import java.util.Locale

@Composable
fun AdjustBalancesDialog(
    summary: BalanceSummary,
    onDismissRequest: () -> Unit,
    onSaveBalances: (wallet: Double, bank: Double, symbol: String) -> Unit
) {
    var walletInput by remember {
        mutableStateOf(String.format(Locale.US, "%.2f", summary.initialWallet))
    }
    var bankInput by remember {
        mutableStateOf(String.format(Locale.US, "%.2f", summary.initialOnlineBank))
    }
    var selectedSymbol by remember { mutableStateOf(summary.currencySymbol) }

    val currencySymbols = listOf("$", "₹", "€", "£", "৳", "¥", "₩", "A$")

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(28.dp))
                .testTag("adjust_balances_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Set Initial Funds",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "Configure your baseline money in cash wallet and online bank.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Wallet Cash Input
                OutlinedTextField(
                    value = walletInput,
                    onValueChange = { walletInput = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Initial Wallet Cash") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("initial_wallet_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Online Bank Input
                OutlinedTextField(
                    value = bankInput,
                    onValueChange = { bankInput = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Initial Online Banking") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("initial_bank_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Currency Symbol / Code Selection
                Text(
                    text = "Active Tracker Currency (Auto-Detect Filter)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "SMS and Email transactions in this currency will be automatically added. Other currencies will be ignored.",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                val presetCurrencies = listOf("BDT", "AED", "USD", "EUR", "INR", "GBP")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetCurrencies.forEach { curr ->
                        FilterChip(
                            selected = selectedSymbol.equals(curr, ignoreCase = true) ||
                                    (curr == "BDT" && selectedSymbol == "৳") ||
                                    (curr == "USD" && selectedSymbol == "$") ||
                                    (curr == "EUR" && selectedSymbol == "€") ||
                                    (curr == "INR" && selectedSymbol == "₹"),
                            onClick = { selectedSymbol = curr },
                            label = { Text(curr, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = selectedSymbol,
                    onValueChange = { selectedSymbol = it.uppercase() },
                    label = { Text("Active Currency Code or Symbol (e.g., AED, BDT, USD, ৳)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val w = walletInput.toDoubleOrNull() ?: 0.0
                        val b = bankInput.toDoubleOrNull() ?: 0.0
                        onSaveBalances(w, b, selectedSymbol)
                        onDismissRequest()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_balances_button")
                ) {
                    Text("Save Baseline Funds", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
