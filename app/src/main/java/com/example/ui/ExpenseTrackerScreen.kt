package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.derivedStateOf
import java.util.Locale
import com.example.ui.components.AboutDeveloperDialog
import com.example.ui.components.AddTransactionDialog
import com.example.ui.components.AdjustBalancesDialog
import com.example.ui.components.FundAccountsBreakdown
import com.example.ui.components.GeminiChatBottomSheet
import com.example.ui.components.QuickGroceryBar
import com.example.ui.components.SettingsDialog
import com.example.ui.components.ShareSummaryDialog
import com.example.ui.components.SmsTrackerDialog
import com.example.ui.components.TotalNetFundsCard
import com.example.ui.components.TransactionItemCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseTrackerScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val summary by viewModel.balanceSummary.collectAsStateWithLifecycle()
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val selectedAccountFilter by viewModel.selectedAccountFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isThinking by viewModel.isGeminiThinking.collectAsStateWithLifecycle()
    val geminiError by viewModel.geminiError.collectAsStateWithLifecycle()

    val isScanningSms by viewModel.isScanningSms.collectAsStateWithLifecycle()
    val smsStatusMsg by viewModel.smsScanStatusMessage.collectAsStateWithLifecycle()
    val detectedSmsTransactions by viewModel.detectedSmsTransactions.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var addDialogPrefillCategory by remember { mutableStateOf("Groceries") }
    var showAdjustDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showSearchRow by remember { mutableStateOf(false) }

    var showChatSheet by remember { mutableStateOf(false) }
    var showSmsSheet by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 30
        }
    }

    // SMS permission launcher
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readGranted = permissions[Manifest.permission.READ_SMS] ?: false
        val receiveGranted = permissions[Manifest.permission.RECEIVE_SMS] ?: false
        if (readGranted || receiveGranted) {
            viewModel.autoScanAndImportSms(context)
        }
    }

    // Auto-scan on screen launch when permissions are present
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val hasRead = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        val hasReceive = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED

        if (hasRead || hasReceive) {
            viewModel.autoScanAndImportSms(context)
        } else {
            smsPermissionLauncher.launch(
                arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
            )
        }
    }

    val requestSmsPermissionAndScan = {
        val hasRead = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        val hasReceive = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED

        if (hasRead || hasReceive) {
            viewModel.scanSmsInbox(context)
        } else {
            smsPermissionLauncher.launch(
                arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
            )
        }
    }

    val categories = listOf("All", "Groceries", "Dining", "Utilities", "Transport", "Shopping", "Dues")
    val accounts = listOf("All", "Wallet", "Online Banking")

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(60.dp)
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.app_logo),
                            contentDescription = "App Vector Logo",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Expense Tracker",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Smart Fund & Expense Manager",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Side Drawer Navigation Items (Includes Initial Funds & Share Summary per requirement)
                NavigationDrawerItem(
                    label = { Text("Initial Funds & Balances", fontWeight = FontWeight.SemiBold) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showAdjustDialog = true
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Share Balance Summary", fontWeight = FontWeight.SemiBold) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showShareDialog = true
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("SMS Auto Sync & Scan", fontWeight = FontWeight.SemiBold) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Sms,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showSmsSheet = true
                        requestSmsPermissionAndScan()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Gemini AI Assistant", fontWeight = FontWeight.SemiBold) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showChatSheet = true
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showSettingsDialog = true
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("About / Developer Credits", fontWeight = FontWeight.SemiBold) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showAboutDialog = true
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.navigationBars,
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier
                                .testTag("top_logo_drawer_trigger")
                                .padding(start = 4.dp)
                        ) {
                            if (!isScrolled) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Menu,
                                            contentDescription = "Open Navigation Menu",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        androidx.compose.foundation.Image(
                                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.app_logo),
                                            contentDescription = "App Vector Logo",
                                            modifier = Modifier.size(22.dp),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Menu,
                                            contentDescription = "Open Navigation Menu",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    },
                    title = { },
                    actions = {
                        // Compact Net Funds badge appears on scroll before search button
                        AnimatedVisibility(
                            visible = isScrolled,
                            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInHorizontally { it / 2 },
                            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutHorizontally { it / 2 }
                        ) {
                            Surface(
                                onClick = {
                                    scope.launch {
                                        listState.animateScrollToItem(0)
                                    }
                                },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shadowElevation = 2.dp,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Net: ",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${summary.currencySymbol}${String.format(Locale.US, "%.2f", summary.totalNetBalance)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { showSearchRow = !showSearchRow },
                            modifier = Modifier.testTag("toggle_search_button")
                        ) {
                            Icon(
                                imageVector = if (showSearchRow) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            floatingActionButton = {
                // Floating Action Buttons Stacked (Gemini AI floating directly above Add Transaction)
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    // Floating Gemini AI Assistant Button
                    ExtendedFloatingActionButton(
                        onClick = { showChatSheet = true },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Gemini AI Assistant",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        },
                        text = {
                            Text(
                                text = "Gemini AI",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        },
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                        modifier = Modifier.testTag("gemini_ai_fab")
                    )

                    // Floating Add Transaction Button
                    ExtendedFloatingActionButton(
                        onClick = {
                            addDialogPrefillCategory = "Groceries"
                            showAddDialog = true
                        },
                        icon = { Icon(Icons.Default.Add, contentDescription = "Add Transaction") },
                        text = { Text("Add Transaction", fontWeight = FontWeight.Bold) },
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("add_transaction_fab")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // SCROLLABLE PAGE CONTENT: Total Net Funds Overview, Breakdown Cards, Quick Groceries, Filters, & Transactions
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Total Net Funds Large Overview Card (Above breakdown cards)
                    item {
                        TotalNetFundsCard(summary = summary)
                    }

                    // 2. Wallet Cash & Online Banking Cards + Total Due/Debt Strip
                    item {
                        FundAccountsBreakdown(summary = summary)
                    }

                    // 3. Quick Entry Bar
                    item {
                        QuickGroceryBar(
                            onQuickAddCategory = { cat ->
                                addDialogPrefillCategory = cat
                                showAddDialog = true
                            }
                        )
                    }

                    // Search Bar (Toggleable)
                    item {
                        AnimatedVisibility(visible = showSearchRow) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                placeholder = { Text("Search transaction title or note...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear")
                                        }
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("search_input")
                            )
                        }
                    }

                    // Filters Section (Account + Category Chips)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Account Filter Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Source:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                accounts.forEach { acc ->
                                    FilterChip(
                                        selected = selectedAccountFilter == acc,
                                        onClick = { viewModel.setAccountFilter(acc) },
                                        label = { Text(acc, fontSize = 12.sp) }
                                    )
                                }
                            }

                            // Category Filter Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Category:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                categories.forEach { cat ->
                                    FilterChip(
                                        selected = selectedCategoryFilter == cat,
                                        onClick = { viewModel.setCategoryFilter(cat) },
                                        label = { Text(cat, fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Section Title for Transactions
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TRANSACTION HISTORY",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "${transactions.size} records",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Transactions List or Empty State
                    if (transactions.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No Transactions Found",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Tap '+ Add Transaction' or open the side menu to sync SMS alerts & chat with Gemini AI.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    } else {
                        items(
                            items = transactions,
                            key = { it.id }
                        ) { tx ->
                            TransactionItemCard(
                                transaction = tx,
                                currencySymbol = summary.currencySymbol,
                                onDeleteClick = { viewModel.deleteTransaction(tx) },
                                onToggleClearedClick = { viewModel.toggleDueCleared(tx) }
                            )
                        }
                    }

                    // Bottom Spacing for Floating Action Buttons
                    item {
                        Spacer(modifier = Modifier.height(140.dp))
                    }
                }
            }
        }
    }

    // Gemini Chatbot Sheet
    if (showChatSheet) {
        GeminiChatBottomSheet(
            messages = chatMessages,
            isThinking = isThinking,
            errorMessage = geminiError,
            onSendMessage = { prompt -> viewModel.sendChatMessage(prompt) },
            onClearChat = { viewModel.clearChat() },
            onDismissRequest = { showChatSheet = false }
        )
    }

    // SMS & Email Auto Tracker Sheet
    if (showSmsSheet) {
        SmsTrackerDialog(
            isScanning = isScanningSms,
            statusMessage = smsStatusMsg,
            detectedTransactions = detectedSmsTransactions,
            currencySymbol = summary.currencySymbol,
            onScanClick = { requestSmsPermissionAndScan() },
            onEmailScanClick = { emailText ->
                viewModel.scanEmailAlertText(emailText)
            },
            onImportClick = { itemsToImport ->
                viewModel.importDetectedSmsTransactions(itemsToImport)
            },
            onDismissRequest = {
                showSmsSheet = false
                viewModel.clearSmsStatus()
            }
        )
    }

    // Modal Dialog Controllers
    if (showAddDialog) {
        AddTransactionDialog(
            currencySymbol = summary.currencySymbol,
            initialCategory = addDialogPrefillCategory,
            onDismissRequest = { showAddDialog = false },
            onSaveTransaction = { type, amount, category, accountType, title, note ->
                viewModel.addTransaction(type, amount, category, accountType, title, note)
            }
        )
    }

    if (showAdjustDialog) {
        AdjustBalancesDialog(
            summary = summary,
            onDismissRequest = { showAdjustDialog = false },
            onSaveBalances = { wallet, bank, symbol ->
                viewModel.updateInitialBalances(wallet, bank, symbol)
            }
        )
    }

    if (showShareDialog) {
        ShareSummaryDialog(
            summary = summary,
            onDismissRequest = { showShareDialog = false }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            onDismissRequest = { showSettingsDialog = false },
            onOpenDeveloperCredits = { showAboutDialog = true }
        )
    }

    if (showAboutDialog) {
        AboutDeveloperDialog(
            onDismissRequest = { showAboutDialog = false }
        )
    }
}
