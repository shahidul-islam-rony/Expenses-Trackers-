package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ExpenseCategory

@Composable
fun QuickGroceryBar(
    categories: List<ExpenseCategory>,
    onQuickAddCategory: (category: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val displayCategories = if (categories.isNotEmpty()) {
        categories.take(6)
    } else {
        listOf(
            ExpenseCategory(name = "Groceries", iconEmoji = "🛒", isCustom = false),
            ExpenseCategory(name = "Dining", iconEmoji = "🍔", isCustom = false),
            ExpenseCategory(name = "Utilities", iconEmoji = "💡", isCustom = false),
            ExpenseCategory(name = "Transport", iconEmoji = "🚖", isCustom = false)
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "QUICK DAILY ENTRY",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            displayCategories.forEach { cat ->
                AssistChip(
                    onClick = { onQuickAddCategory(cat.name) },
                    label = { Text("${cat.iconEmoji} ${cat.name}", fontWeight = FontWeight.SemiBold) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.testTag("quick_${cat.name.lowercase()}_chip")
                )
            }
        }
    }
}
