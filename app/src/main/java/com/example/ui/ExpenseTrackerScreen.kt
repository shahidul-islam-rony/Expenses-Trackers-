package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.AboutDeveloperDialog
import com.example.ui.components.AddTransactionDialog
import com.example.ui.components.AdjustBalancesDialog
import com.example.ui.components.FundAccountsBreakdown
import com.example.ui.components.ManageCategoriesDialog
import com.example.ui.components.MonthlyReportDialog
import com.example.ui.components.QuickGroceryBar
import com.example.ui.components.SearchAndFilterSection
import com.example.ui.components.SettingsDialog
import com.example.ui.components.ShareSummaryDialog
import com.example.ui.components.SmsTrackerDialog
import com.example.ui.components.TotalNetFundsCard
import com.example.ui.components.TransactionItemCard
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val monthlyReport by viewModel.monthlyReport.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTimeframe by viewModel.selectedTimeframeFilter.collectAsStateWithLifecycle()
    val selectedType by viewModel.selectedTypeFilter.collectAsStateWithLifecycle()
    val selectedAccount by viewModel.selectedAccountFilter.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val activeFilterCount by viewModel.activeFilterCount.collectAsStateWithLifecycle()

    val isScanningSms by viewModel.isScanningSms.collectAsStateWithLifecycle()
    val smsStatusMsg by viewModel.smsScanStatusMessage.collectAsStateWithLifecycle()
    val detectedSmsTransactions by viewModel.detectedSmsTransactions.collectAsStateWithLifecycle()

    // Multi-Selection State
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedTransactionIds by remember { mutableStateOf(setOf<Int>()) }
    var showBulkDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Dialog Visibilities
    var showAddDialog by remember { mutableStateOf(false) }
    var addDialogPrefillCategory by remember { mutableStateOf("Groceries") }
    var showAdjustDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showSmsSheet by remember { mutableStateOf(false) }
    var showMonthlyReportDialog by remember { mutableStateOf(false) }
    var showManageCategoriesDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 30
        }
    }

    // Handle Hardware Back Button to Exit Multi-Selection Mode
    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedTransactionIds = emptySet()
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
    LaunchedEffect(Unit) {
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isSelectionMode,
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
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "App Vector Logo",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            contentScale = ContentScale.Fit
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

                // Side Drawer Navigation Items
                NavigationDrawerItem(
                    label = { Text("Monthly Expense Report", fontWeight = FontWeight.SemiBold) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showMonthlyReportDialog = true
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Manage Categories", fontWeight = FontWeight.SemiBold) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showManageCategoriesDialog = true
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

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
                if (isSelectionMode) {
                    // Contextual Multi-Selection TopAppBar
                    TopAppBar(
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    isSelectionMode = false
                                    selectedTransactionIds = emptySet()
                                },
                                modifier = Modifier.testTag("exit_selection_mode_button")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close Selection")
                            }
                        },
                        title = {
                            Text(
                                text = "${selectedTransactionIds.size} Selected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        actions = {
                            // Select All / Deselect All Action Button
                            val allSelected = transactions.isNotEmpty() && selectedTransactionIds.size == transactions.size
                            IconButton(
                                onClick = {
                                    selectedTransactionIds = if (allSelected) {
                                        emptySet()
                                    } else {
                                        transactions.map { it.id }.toSet()
                                    }
                                },
                                modifier = Modifier.testTag("select_all_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (allSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                                    contentDescription = if (allSelected) "Deselect All" else "Select All"
                                )
                            }

                            // Bulk Delete Action Button
                            IconButton(
                                onClick = {
                                    if (selectedTransactionIds.isNotEmpty()) {
                                        showBulkDeleteConfirmDialog = true
                                    }
                                },
                                enabled = selectedTransactionIds.isNotEmpty(),
                                modifier = Modifier.testTag("bulk_delete_action_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Selected",
                                    tint = if (selectedTransactionIds.isNotEmpty())
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                } else {
                    // Standard TopAppBar
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
                                            Image(
                                                painter = painterResource(id = R.drawable.app_logo),
                                                contentDescription = "App Vector Logo",
                                                modifier = Modifier.size(22.dp),
                                                contentScale = ContentScale.Fit
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
                            // Compact Net Funds badge appears on scroll
                            AnimatedVisibility(
                                visible = isScrolled,
                                enter = fadeIn() + slideInHorizontally { it / 2 },
                                exit = fadeOut() + slideOutHorizontally { it / 2 }
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
                                    modifier = Modifier.padding(end = 6.dp)
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

                            // Monthly Report Quick Action Icon
                            IconButton(
                                onClick = { showMonthlyReportDialog = true },
                                modifier = Modifier.testTag("top_bar_report_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = "Monthly Report",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            },
            floatingActionButton = {
                // Stack of Floating Action Buttons: Back-to-Top Button & Add Transaction FAB
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    // Smooth "Back to Top" Floating Button (appears as user scrolls down)
                    AnimatedVisibility(
                        visible = isScrolled,
                        enter = fadeIn() + slideInVertically { it / 2 },
                        exit = fadeOut() + slideOutVertically { it / 2 }
                    ) {
                        FloatingActionButton(
                            onClick = {
                                scope.launch {
                                    listState.animateScrollToItem(0)
                                }
                            },
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.primary,
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                            modifier = Modifier
                                .size(46.dp)
                                .testTag("scroll_to_top_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Scroll to top",
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    if (isSelectionMode) {
                        // Floating Delete Selected Button in Selection Mode
                        if (selectedTransactionIds.isNotEmpty()) {
                            ExtendedFloatingActionButton(
                                onClick = { showBulkDeleteConfirmDialog = true },
                                icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                text = { Text("Delete (${selectedTransactionIds.size})", fontWeight = FontWeight.Bold) },
                                shape = CircleShape,
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.testTag("bulk_delete_fab")
                            )
                        }
                    } else {
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
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Main Scrollable Content with Sticky Pinned Search & Timeframe Filters
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    // 1. Total Net Funds Overview Card
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            TotalNetFundsCard(summary = summary)
                        }
                    }

                    // 2. Wallet Cash & Online Banking Cards + Total Due Strip
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            FundAccountsBreakdown(summary = summary)
                        }
                    }

                    // 3. Quick Daily Entry Bar
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            QuickGroceryBar(
                                categories = categories,
                                onQuickAddCategory = { cat ->
                                    addDialogPrefillCategory = cat
                                    showAddDialog = true
                                }
                            )
                        }
                    }

                    // 4. PINNED STICKY HEADER: Search Bar & Multi-Filter Options (Sticks cleanly at the top when scrolling)
                    stickyHeader {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = if (isScrolled) 3.dp else 0.dp,
                            shadowElevation = if (isScrolled) 4.dp else 0.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                SearchAndFilterSection(
                                    searchQuery = searchQuery,
                                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                    selectedTimeframe = selectedTimeframe,
                                    onTimeframeChange = { viewModel.setTimeframeFilter(it) },
                                    selectedType = selectedType,
                                    onTypeChange = { viewModel.setTypeFilter(it) },
                                    selectedAccount = selectedAccount,
                                    onAccountChange = { viewModel.setAccountFilter(it) },
                                    selectedCategory = selectedCategory,
                                    onCategoryChange = { viewModel.setCategoryFilter(it) },
                                    categories = categories,
                                    activeFilterCount = activeFilterCount,
                                    onResetFilters = { viewModel.resetFilters() },
                                    onOpenManageCategories = { showManageCategoriesDialog = true }
                                )
                            }
                        }
                    }

                    // 5. Section Header for Transactions with "Select" Action
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "TRANSACTION HISTORY",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "${transactions.size} records",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (transactions.isNotEmpty() && !isSelectionMode) {
                                FilledTonalButton(
                                    onClick = { isSelectionMode = true },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("enter_selection_mode_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Checklist,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Select", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // 6. Transactions List or Empty State
                    if (transactions.isEmpty()) {
                        item {
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(28.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ReceiptLong,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                            modifier = Modifier.size(52.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = if (activeFilterCount > 0) "No Matching Transactions" else "No Transactions Yet",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (activeFilterCount > 0) "Try clearing some search filters or adjusting your timeframe." else "Tap '+ Add Transaction' or scan SMS messages to start tracking.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        items(
                            items = transactions,
                            key = { it.id }
                        ) { tx ->
                            val isSelected = selectedTransactionIds.contains(tx.id)
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                TransactionItemCard(
                                    transaction = tx,
                                    currencySymbol = summary.currencySymbol,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = isSelected,
                                    onToggleSelect = {
                                        selectedTransactionIds = if (isSelected) {
                                            selectedTransactionIds - tx.id
                                        } else {
                                            selectedTransactionIds + tx.id
                                        }
                                    },
                                    onLongClick = {
                                        if (!isSelectionMode) {
                                            isSelectionMode = true
                                            selectedTransactionIds = setOf(tx.id)
                                        }
                                    },
                                    onDeleteClick = { viewModel.deleteTransaction(tx) },
                                    onToggleClearedClick = { viewModel.toggleDueCleared(tx) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Bulk Delete Confirmation Dialog
    if (showBulkDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "Delete ${selectedTransactionIds.size} Transactions?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Are you sure you want to permanently delete the selected ${selectedTransactionIds.size} transactions? Your account balances will be recalculated.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMultipleTransactions(selectedTransactionIds)
                        selectedTransactionIds = emptySet()
                        isSelectionMode = false
                        showBulkDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_bulk_delete_button")
                ) {
                    Text("Delete All Selected", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Monthly Report Dialog
    if (showMonthlyReportDialog) {
        MonthlyReportDialog(
            report = monthlyReport,
            onNavigateMonth = { delta -> viewModel.navigateReportMonth(delta) },
            onDismissRequest = {
                showMonthlyReportDialog = false
                viewModel.setReportMonthOffset(0)
            }
        )
    }

    // Manage & Add Custom Categories Dialog
    if (showManageCategoriesDialog) {
        ManageCategoriesDialog(
            categories = categories,
            onAddCategory = { name, emoji -> viewModel.addCustomCategory(name, emoji) },
            onDeleteCategory = { cat -> viewModel.deleteCategory(cat) },
            onDismissRequest = { showManageCategoriesDialog = false }
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
            categories = categories,
            onOpenManageCategories = { showManageCategoriesDialog = true },
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
            onOpenMonthlyReport = { showMonthlyReportDialog = true },
            onOpenManageCategories = { showManageCategoriesDialog = true },
            onOpenDeveloperCredits = { showAboutDialog = true }
        )
    }

    if (showAboutDialog) {
        AboutDeveloperDialog(
            onDismissRequest = { showAboutDialog = false }
        )
    }
}
