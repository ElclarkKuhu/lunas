package id.elclark.lunas.ui.main

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import id.elclark.lunas.model.BillItem
import id.elclark.lunas.theme.StatusPaid
import id.elclark.lunas.ui.components.*
import id.elclark.lunas.util.DateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel(),
    initialOpenAddSheet: Boolean = false,
    onAddSheetOpened: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToManageServices: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedYm by viewModel.selectedYm.collectAsStateWithLifecycle()

    val baseYm = remember { DateUtils.currentYearMonth() }
    val initialPage = 1200
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 2400 })

    val currentPagerYm = remember(pagerState.currentPage) {
        DateUtils.plusMonths(baseYm, (pagerState.currentPage - initialPage).toLong())
    }

    LaunchedEffect(currentPagerYm) {
        viewModel.setYearMonth(currentPagerYm)
    }

    var showAddSheet by remember { mutableStateOf(false) }
    var editingBillItem by remember { mutableStateOf<BillItem?>(null) }
    var showJsonImportDialog by remember { mutableStateOf(false) }
    var showTotalOwedSheet by remember { mutableStateOf(false) }

    LaunchedEffect(initialOpenAddSheet) {
        if (initialOpenAddSheet) {
            editingBillItem = null
            showAddSheet = true
            onAddSheetOpened()
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    when (val state = uiState) {
        MainUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is MainUiState.Success -> {
            if (state.isLocked) {
                BiometricLockScreen(
                    onUnlockSuccess = { viewModel.unlock() },
                    modifier = modifier
                )
            } else {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                painter = androidx.compose.ui.res.painterResource(id = id.elclark.lunas.R.drawable.ic_lunas_logo),
                                                contentDescription = "Logo Lunas",
                                                tint = Color.Unspecified,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Lunas",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Black,
                                                fontSize = 20.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = "Paylater & Cicilan Tracker",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Normal
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { showJsonImportDialog = true },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = "Ekstraksi Tagihan AI",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))

                                if (state.settings.biometricEnabled) {
                                    IconButton(
                                        onClick = { viewModel.lock() },
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = "Kunci Aplikasi",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                IconButton(
                                    onClick = onNavigateToSettings,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = "Pengaturan",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            )
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {
                                editingBillItem = null
                                showAddSheet = true
                            },
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            elevation = FloatingActionButtonDefaults.elevation(
                                defaultElevation = 4.dp,
                                pressedElevation = 6.dp
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Tambah Tagihan",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    modifier = modifier
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(innerPadding)
                    ) {
                        // Month Navigator (Fixed at top)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 2.dp)
                        ) {
                            MonthSelector(
                                selectedYm = currentPagerYm,
                                onPrevious = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                },
                                onNext = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                },
                                onCurrentMonth = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(initialPage)
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Smooth Horizontal Pager for month contents
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            beyondViewportPageCount = 1
                        ) { page ->
                            val pageYm = remember(page) {
                                DateUtils.plusMonths(baseYm, (page - initialPage).toLong())
                            }
                            val overview = remember(pageYm, state.services, state.items, state.payments) {
                                viewModel.computeOverview(pageYm)
                            }
                            val totalOwedSummary = remember(state.services, state.items, state.payments) {
                                viewModel.computeTotalOwed()
                            }

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Total Overview Card
                                item {
                                    TotalSummaryCard(
                                        overview = overview,
                                        totalOwed = totalOwedSummary.grandTotalOwed,
                                        onClickTotalOwed = { showTotalOwedSheet = true }
                                    )
                                }

                                // Section Title & Counter
                                item {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "RINCIAN LAYANAN",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.2.sp,
                                                fontSize = 11.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        if (overview.statements.isNotEmpty()) {
                                            Text(
                                                text = "${overview.statements.count { it.items.isNotEmpty() }} aktif",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                // Statements List
                                if (overview.statements.isEmpty()) {
                                    item {
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 16.dp)
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.padding(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = StatusPaid,
                                                    modifier = Modifier.size(40.dp)
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Text(
                                                    text = "Belum Ada Tagihan",
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = "Tambahkan cicilan atau hitung cepat tagihan bulan ini dengan tombol di bawah.",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    val sortedStatements = overview.statements.sortedByDescending { it.items.isNotEmpty() }
                                    items(
                                        items = sortedStatements,
                                        key = { it.service.id }
                                    ) { statement ->
                                        ServiceStatementCard(
                                            statement = statement,
                                            onTogglePaid = { viewModel.togglePayment(statement.service.id, pageYm) },
                                            onEditItem = { item -> editingBillItem = item },
                                            onDeleteItem = { itemId -> viewModel.deleteItem(itemId) }
                                        )
                                    }
                                }

                                // Bottom Spacer for FAB
                                item {
                                    Spacer(modifier = Modifier.height(84.dp))
                                }
                            }
                        }
                    }

                    // Dialogs & Sheets
                    if (showAddSheet || editingBillItem != null) {
                        AddBillBottomSheet(
                            services = state.services,
                            defaultYm = currentPagerYm,
                            editingItem = editingBillItem,
                            onDismiss = {
                                showAddSheet = false
                                editingBillItem = null
                            },
                            onAddCicilan = { serviceId, title, amount, tenor, startMonth ->
                                viewModel.addCicilan(serviceId, title, amount, tenor, startMonth)
                            },
                            onAddScratchpad = { serviceId, title, rawExpression, sum, targetMonth ->
                                viewModel.addScratchpad(serviceId, title, rawExpression, sum, targetMonth)
                            },
                            onUpdateCicilan = { id, serviceId, title, amount, tenor, startMonth ->
                                viewModel.updateCicilan(id, serviceId, title, amount, tenor, startMonth)
                            },
                            onUpdateScratchpad = { id, serviceId, title, rawExpression, sum, targetMonth ->
                                viewModel.updateScratchpad(id, serviceId, title, rawExpression, sum, targetMonth)
                            },
                            onDeleteItem = { id ->
                                viewModel.deleteItem(id)
                            },
                            onOpenManageServices = {
                                showAddSheet = false
                                editingBillItem = null
                                onNavigateToManageServices()
                            },
                            onOpenJsonImport = {
                                showAddSheet = false
                                editingBillItem = null
                                showJsonImportDialog = true
                            }
                        )
                    }

                    if (showJsonImportDialog) {
                        JsonImportDialog(
                            onDismiss = { showJsonImportDialog = false },
                            onImportJson = { text, merge -> viewModel.importJsonData(text, merge) },
                            services = state.services
                        )
                    }

                    if (showTotalOwedSheet) {
                        val totalOwedSummary = remember(state.services, state.items, state.payments) {
                            viewModel.computeTotalOwed()
                        }
                        TotalOwedBottomSheet(
                            summary = totalOwedSummary,
                            onDismiss = { showTotalOwedSheet = false }
                        )
                    }
                }
            }
        }
    }
}
