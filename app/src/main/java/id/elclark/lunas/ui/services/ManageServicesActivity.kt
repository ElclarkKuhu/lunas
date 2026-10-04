package id.elclark.lunas.ui.services

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.elclark.lunas.R
import id.elclark.lunas.data.LunasRepository
import id.elclark.lunas.model.PaylaterService
import id.elclark.lunas.theme.LunasTheme
import kotlinx.coroutines.launch
import java.util.UUID

class ManageServicesActivity : ComponentActivity() {

    private lateinit var repository: LunasRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = LunasRepository.getInstance(applicationContext)

        enableEdgeToEdge()
        setContent {
            LunasTheme {
                ManageServicesScreen(
                    repository = repository,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageServicesScreen(
    repository: LunasRepository,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val services by repository.services.collectAsStateWithLifecycle()

    var showServiceFormSheet by remember { mutableStateOf(false) }
    var editingService by remember { mutableStateOf<PaylaterService?>(null) }
    var serviceToArchive by remember { mutableStateOf<PaylaterService?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.manage_services_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            editingService = null
                            showServiceFormSheet = true
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.manage_services_add), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.manage_services_list_count, services.size),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
            }

            if (services.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.manage_services_empty_title),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.manage_services_empty_message),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(services, key = { it.id }) { svc ->
                    val svcColor = Color(svc.colorHex)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                editingService = svc
                                showServiceFormSheet = true
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(svcColor)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = svc.name,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = stringResource(
                                            R.string.manage_services_cutoff_due,
                                            svc.billingCutoffDay,
                                            svc.dueDay,
                                            stringResource(if (svc.dueMonthOffset == 1) R.string.manage_services_next_month else R.string.manage_services_same_month)
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (!svc.isActive) {
                                        Text(
                                            text = stringResource(R.string.manage_services_archived),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        editingService = svc
                                        showServiceFormSheet = true
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = stringResource(R.string.manage_services_edit_description),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (svc.isActive) {
                                            serviceToArchive = svc
                                        } else {
                                            coroutineScope.launch {
                                                repository.saveService(svc.copy(isActive = true))
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = if (svc.isActive) Icons.Default.Archive else Icons.Default.Restore,
                                        contentDescription = stringResource(
                                            if (svc.isActive) R.string.manage_services_archive_description
                                            else R.string.manage_services_restore_description
                                        ),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Add / Edit Service Bottom Sheet
    if (showServiceFormSheet) {
        ServiceFormBottomSheet(
            service = editingService,
            onDismiss = {
                showServiceFormSheet = false
                editingService = null
            },
            onSave = { updatedService ->
                coroutineScope.launch {
                    repository.saveService(updatedService)
                }
                showServiceFormSheet = false
                editingService = null
            }
        )
    }

    // Archive Confirmation Dialog
    if (serviceToArchive != null) {
        val target = serviceToArchive!!
        AlertDialog(
            onDismissRequest = { serviceToArchive = null },
            shape = RoundedCornerShape(18.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            title = {
                Text(stringResource(R.string.manage_services_archive_title), fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    stringResource(R.string.manage_services_archive_message, target.name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            repository.archiveService(target.id)
                        }
                        serviceToArchive = null
                    },
                ) {
                    Text(stringResource(R.string.manage_services_archive), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { serviceToArchive = null }) {
                    Text(stringResource(R.string.manage_services_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServiceFormBottomSheet(
    service: PaylaterService?,
    onDismiss: () -> Unit,
    onSave: (PaylaterService) -> Unit
) {
    val isEdit = service != null
    var name by remember { mutableStateOf(service?.name ?: "") }
    var cutoffDayText by remember { mutableStateOf((service?.billingCutoffDay ?: 20).toString()) }
    var dueDayText by remember { mutableStateOf((service?.dueDay ?: 1).toString()) }
    var dueOffset by remember { mutableIntStateOf(service?.dueMonthOffset ?: 1) }

    val presetColors = listOf(
        0xFFEE4D2D, // Shopee Orange
        0xFF00AA13, // Gojek Green
        0xFFF5A623, // Yup Gold
        0xFF0089FF, // Kredivo Blue
        0xFFE53935, // Akulaku Red
        0xFF8E24AA, // Purple
        0xFF00897B, // Teal
        0xFFD81B60  // Pink
    )
    var selectedColor by remember { mutableLongStateOf(service?.colorHex ?: presetColors.first()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .imePadding()
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(if (isEdit) R.string.manage_service_edit_title else R.string.manage_service_add_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.manage_service_name)) },
                placeholder = { Text(stringResource(R.string.manage_service_name_example)) },
                shape = RoundedCornerShape(10.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = cutoffDayText,
                    onValueChange = { cutoffDayText = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.manage_service_cutoff_day)) },
                    placeholder = { Text(stringResource(R.string.manage_service_day_range)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = dueDayText,
                    onValueChange = { dueDayText = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.manage_service_due_day)) },
                    placeholder = { Text(stringResource(R.string.manage_service_day_range)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Column {
                Text(
                    text = stringResource(R.string.manage_service_due_month),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (dueOffset == 1) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { dueOffset = 1 }
                        ) {
                            Text(
                                text = stringResource(R.string.manage_service_next_month_option),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (dueOffset == 1) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (dueOffset == 1) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 9.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (dueOffset == 0) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { dueOffset = 0 }
                        ) {
                            Text(
                                text = stringResource(R.string.manage_service_same_month_option),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (dueOffset == 0) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (dueOffset == 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 9.dp)
                            )
                        }
                    }
                }
            }

            Column {
                Text(
                    text = stringResource(R.string.manage_service_badge_color),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    presetColors.forEach { colorVal ->
                        val color = Color(colorVal)
                        val isSelected = selectedColor == colorVal
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = colorVal }
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val cutoff = cutoffDayText.toIntOrNull() ?: 0
            val due = dueDayText.toIntOrNull() ?: 0
            val isValid = name.isNotBlank() && cutoff in 1..31 && due in 1..31

            Button(
                onClick = {
                    val svcId = service?.id ?: name.lowercase().replace(" ", "_").trim()
                    val newService = PaylaterService(
                        id = if (svcId.isBlank()) UUID.randomUUID().toString() else svcId,
                        name = name.trim(),
                        billingCutoffDay = cutoff,
                        dueDay = due,
                        dueMonthOffset = dueOffset,
                        colorHex = selectedColor,
                        isActive = true
                    )
                    onSave(newService)
                },
                enabled = isValid,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text(
                    text = stringResource(if (isEdit) R.string.manage_service_save_changes else R.string.manage_service_add_button),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
