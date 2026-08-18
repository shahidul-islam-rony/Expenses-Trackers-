package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AccountType
import com.example.data.ExpenseTransaction
import com.example.data.TransactionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionItemCard(
    transaction: ExpenseTransaction,
    currencySymbol: String,
    onDeleteClick: () -> Unit,
    onToggleClearedClick: () -> Unit,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val categoryIcon = getCategoryIcon(transaction.category)
    val categoryColor = getCategoryColor(transaction.category, transaction.type)
    val dateStr = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(transaction.timestamp))

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelect()
                    }
                },
                onLongClick = {
                    onLongClick()
                }
            )
            .testTag("transaction_item_${transaction.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox for selection mode
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .testTag("checkbox_${transaction.id}")
                )
            }

            // Category Icon Badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = transaction.category,
                    tint = categoryColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Main Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = transaction.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Account Type Badge
                    if (transaction.accountType != AccountType.NONE) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (transaction.accountType) {
                                AccountType.WALLET -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                AccountType.ONLINE_BANKING -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                else -> MaterialTheme.colorScheme.surface
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (transaction.accountType == AccountType.WALLET)
                                        Icons.Default.AccountBalanceWallet else Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (transaction.accountType == AccountType.WALLET) "Wallet" else "Bank",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                if (transaction.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = transaction.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$dateStr • ${transaction.category}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Amount & Individual Actions (Hidden in bulk selection mode to keep focus on selecting)
            Column(
                horizontalAlignment = Alignment.End
            ) {
                val (amountPrefix, amountColor) = when (transaction.type) {
                    TransactionType.EXPENSE -> "-" to MaterialTheme.colorScheme.error
                    TransactionType.INCOME -> "+" to Color(0xFF00A878)
                    TransactionType.DUE -> if (transaction.isCleared) "Cleared" to Color.Gray else "Due" to Color(0xFFFF9800)
                }

                Text(
                    text = if (transaction.type == TransactionType.DUE && transaction.isCleared)
                        "Cleared $currencySymbol${String.format(Locale.US, "%.2f", transaction.amount)}"
                    else
                        "$amountPrefix $currencySymbol${String.format(Locale.US, "%.2f", transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = amountColor
                )

                if (!isSelectionMode) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (transaction.type == TransactionType.DUE) {
                            IconButton(
                                onClick = onToggleClearedClick,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (transaction.isCleared) Icons.Default.CheckCircle else Icons.Default.Check,
                                    contentDescription = if (transaction.isCleared) "Mark Unpaid" else "Mark Paid",
                                    tint = if (transaction.isCleared) Color(0xFF00A878) else Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Transaction",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getCategoryIcon(category: String): ImageVector {
    return when (category.lowercase()) {
        "groceries", "grocery" -> Icons.Default.ShoppingCart
        "dining", "food", "restaurants" -> Icons.Default.Fastfood
        "utilities", "bills", "electricity" -> Icons.Default.Lightbulb
        "transport", "transportation", "fuel" -> Icons.Default.DirectionsCar
        "shopping" -> Icons.Default.ShoppingBag
        "salary", "income" -> Icons.Default.Payments
        "due", "borrowed", "debt" -> Icons.Default.Warning
        else -> Icons.Default.Receipt
    }
}

private fun getCategoryColor(category: String, type: TransactionType): Color {
    if (type == TransactionType.INCOME) return Color(0xFF00A878)
    if (type == TransactionType.DUE) return Color(0xFFFF9800)

    return when (category.lowercase()) {
        "groceries", "grocery" -> Color(0xFF4CAF50)
        "dining", "food" -> Color(0xFFFF7043)
        "utilities", "bills" -> Color(0xFF2196F3)
        "transport", "transportation" -> Color(0xFF9C27B0)
        "shopping" -> Color(0xFFE91E63)
        else -> Color(0xFF607D8B)
    }
}
