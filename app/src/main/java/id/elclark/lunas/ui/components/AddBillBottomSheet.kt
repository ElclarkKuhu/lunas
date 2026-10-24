package id.elclark.lunas.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ViewTimeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import id.elclark.lunas.R
import id.elclark.lunas.model.BillItem
import id.elclark.lunas.model.PaylaterService
import id.elclark.lunas.util.DateUtils
import id.elclark.lunas.util.MathParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBillBottomSheet(
    services: List<PaylaterService>,
    defaultYm: String,
    currencyCode: String = "IDR",
    editingItem: BillItem? = null,
    onDismiss: () -> Unit,
    onAddCicilan: (serviceId: String, title: String, amountPerMonth: Long, tenor: Int, startMonth: String) -> Unit,
    onAddScratchpad: (serviceId: String, title: String, rawExpression: String, sum: Long, targetMonth: String) -> Unit,
    onUpdateCicilan: ((id: String, serviceId: String, title: String, amountPerMonth: Long, tenor: Int, startMonth: String) -> Unit)? = null,
    onUpdateScratchpad: ((id: String, serviceId: String, title: String, rawExpression: String, sum: Long, targetMonth: String) -> Unit)? = null,
    onDeleteItem: ((id: String) -> Unit)? = null,
    onOpenManageServices: () -> Unit,
    onOpenJsonImport: () -> Unit = {},
    onReorderServices: ((List<String>) -> Unit)? = null
) {
    val isEditing = editingItem != null
    var selectedServiceId by remember(editingItem?.id) {
        mutableStateOf(editingItem?.serviceId ?: services.firstOrNull()?.id ?: "")
    }

    LaunchedEffect(services) {
        if (selectedServiceId.isEmpty() || services.none { it.id == selectedServiceId }) {
            selectedServiceId = services.firstOrNull()?.id ?: ""
        }
    }
    var selectedTab by remember(editingItem?.id) {
        mutableIntStateOf(if (editingItem?.rawMathExpression != null) 1 else 0)
    }

    // Cicilan state
    var cicilanAmountText by remember(editingItem?.id) {
        mutableStateOf(if (editingItem != null && editingItem.rawMathExpression == null) editingItem.amountPerMonth.toString() else "")
    }
    var cicilanTenor by remember(editingItem?.id) {
        mutableIntStateOf(if (editingItem != null && editingItem.rawMathExpression == null) editingItem.totalTenor.coerceAtLeast(1) else 1)
    }
    var cicilanTitle by remember(editingItem?.id) {
        mutableStateOf(if (editingItem != null && editingItem.rawMathExpression == null) editingItem.title else "")
    }
    var cicilanStartYm by remember(editingItem?.id, defaultYm) {
        mutableStateOf(editingItem?.startYearMonth ?: defaultYm)
    }

    // Scratchpad state
    var scratchpadExpression by remember(editingItem?.id) {
        mutableStateOf(editingItem?.rawMathExpression ?: "")
    }
    var scratchpadTitle by remember(editingItem?.id) {
        mutableStateOf(if (editingItem?.rawMathExpression != null) editingItem.title else "")
    }
    val mathResult = remember(scratchpadExpression) { MathParser.parse(scratchpadExpression) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    var localServices by remember { mutableStateOf(services) }
    var draggingServiceId by remember { mutableStateOf<String?>(null) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var autoScrollJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(services) {
        if (draggingServiceId == null) {
            localServices = services
        }
    }

    fun finishDrag() {
        autoScrollJob?.cancel()
        autoScrollJob = null
        val currentDraggingId = draggingServiceId
        if (currentDraggingId != null) {
            val originalIds = services.map { it.id }
            val newIds = localServices.map { it.id }
            if (originalIds != newIds) {
                onReorderServices?.invoke(newIds)
            }
            draggingServiceId = null
            dragOffsetX = 0f
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        val sheetScrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(sheetScrollState)
                .padding(horizontal = 20.dp)
                .imePadding()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = if (isEditing) "Edit Tagihan" else "Catat Tagihan Paylater",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (isEditing) "Perbarui rincian atau nominal tagihan" else "Pilih layanan dan masukkan cicilan atau hitung cepat",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Service selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PILIH LAYANAN",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (onReorderServices != null && services.size > 1) {
                    Text(
                        text = stringResource(R.string.reorder_services_drag_hint),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                state = lazyListState,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(
                    items = localServices,
                    key = { it.id }
                ) { svc ->
                    val isSelected = svc.id == selectedServiceId
                    val isDragging = draggingServiceId == svc.id
                    val svcColor = Color(svc.colorHex)

                    val itemModifier = if (isDragging) {
                        Modifier
                    } else {
                        Modifier.animateItem()
                    }

                    val reorderModifier = if (onReorderServices != null && localServices.size > 1) {
                        Modifier.pointerInput(svc.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    draggingServiceId = svc.id
                                    dragOffsetX = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetX += dragAmount.x

                                    val curIdx = localServices.indexOfFirst { it.id == svc.id }
                                    if (curIdx != -1) {
                                        val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
                                        val curInfo = visibleItems.firstOrNull { it.key == svc.id }
                                        if (curInfo != null) {
                                            val curCenter = curInfo.offset + curInfo.size / 2f + dragOffsetX

                                            // Auto-scroll near edges
                                            val viewportWidth = lazyListState.layoutInfo.viewportEndOffset - lazyListState.layoutInfo.viewportStartOffset
                                            if (viewportWidth > 0) {
                                                val visualLeft = curInfo.offset + dragOffsetX
                                                val visualRight = visualLeft + curInfo.size
                                                if (visualRight > viewportWidth - 20) {
                                                    if (autoScrollJob?.isActive != true) {
                                                        autoScrollJob = coroutineScope.launch {
                                                            while (isActive) {
                                                                lazyListState.scrollBy(14f)
                                                                delay(16)
                                                            }
                                                        }
                                                    }
                                                } else if (visualLeft < 20) {
                                                    if (autoScrollJob?.isActive != true) {
                                                        autoScrollJob = coroutineScope.launch {
                                                            while (isActive) {
                                                                lazyListState.scrollBy(-14f)
                                                                delay(16)
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    autoScrollJob?.cancel()
                                                    autoScrollJob = null
                                                }
                                            }

                                            // Reorder right
                                            if (dragOffsetX > 0 && curIdx < localServices.size - 1) {
                                                val nextSvc = localServices[curIdx + 1]
                                                val nextInfo = visibleItems.firstOrNull { it.key == nextSvc.id }
                                                if (nextInfo != null) {
                                                    val nextCenter = nextInfo.offset + nextInfo.size / 2f
                                                    if (curCenter > nextCenter) {
                                                        val updated = localServices.toMutableList()
                                                        val moved = updated.removeAt(curIdx)
                                                        updated.add(curIdx + 1, moved)
                                                        localServices = updated
                                                        val delta = nextInfo.offset - curInfo.offset
                                                        dragOffsetX -= delta
                                                    }
                                                }
                                            }
                                            // Reorder left
                                            else if (dragOffsetX < 0 && curIdx > 0) {
                                                val prevSvc = localServices[curIdx - 1]
                                                val prevInfo = visibleItems.firstOrNull { it.key == prevSvc.id }
                                                if (prevInfo != null) {
                                                    val prevCenter = prevInfo.offset + prevInfo.size / 2f
                                                    if (curCenter < prevCenter) {
                                                        val updated = localServices.toMutableList()
                                                        val moved = updated.removeAt(curIdx)
                                                        updated.add(curIdx - 1, moved)
                                                        localServices = updated
                                                        val delta = curInfo.offset - prevInfo.offset
                                                        dragOffsetX += delta
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                                onDragEnd = { finishDrag() },
                                onDragCancel = { finishDrag() }
                            )
                        }
                    } else {
                        Modifier
                    }

                    Surface(
                        onClick = { selectedServiceId = svc.id },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = if (isDragging) 8.dp else 0.dp,
                        shadowElevation = if (isDragging) 8.dp else 0.dp,
                        modifier = Modifier
                            .then(itemModifier)
                            .zIndex(if (isDragging) 10f else 1f)
                            .graphicsLayer {
                                translationX = if (isDragging) dragOffsetX else 0f
                                scaleX = if (isDragging) 1.08f else 1.0f
                                scaleY = if (isDragging) 1.08f else 1.0f
                            }
                            .then(reorderModifier)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(svcColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = svc.name,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                item(key = "manage_services_btn") {
                    Surface(
                        onClick = { onOpenManageServices() },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Kelola",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item(key = "json_import_btn") {
                    Surface(
                        onClick = { onOpenJsonImport() },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Suntik AI",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Segmented Tab Row
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
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
                        color = if (selectedTab == 0) MaterialTheme.colorScheme.surface else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedTab = 0 }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.ViewTimeline,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (selectedTab == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Cicilan (Tenor)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (selectedTab == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (selectedTab == 1) MaterialTheme.colorScheme.surface else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedTab = 1 }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Calculate,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (selectedTab == 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Hitung Cepat",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (selectedTab == 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                // Cicilan Form
                OutlinedTextField(
                    value = cicilanTitle,
                    onValueChange = { cicilanTitle = it },
                    label = { Text("Nama Barang / Keperluan (Opsional)") },
                    placeholder = { Text("Contoh: Monitor Arm, HP Baru") },
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = cicilanAmountText,
                    onValueChange = { cicilanAmountText = it.filter { char -> char.isDigit() } },
                    label = { Text("Cicilan Per Bulan (Rp)") },
                    placeholder = { Text("115688") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    prefix = { Text("${DateUtils.currencySymbol(currencyCode)} ") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Bulan Mulai Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MULAI BULAN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { cicilanStartYm = DateUtils.plusMonths(cicilanStartYm, -1) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Bulan Lalu",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = DateUtils.formatYearMonthFull(cicilanStartYm),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )

                        IconButton(
                            onClick = { cicilanStartYm = DateUtils.plusMonths(cicilanStartYm, 1) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Bulan Depan",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "PILIH TENOR BULAN",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                val standardTenors = listOf(1, 2, 3, 6, 9, 12)
                val tenorOptions = if (standardTenors.contains(cicilanTenor)) standardTenors else (standardTenors + cicilanTenor).sorted()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tenorOptions.forEach { t ->
                        val isSelected = cicilanTenor == t
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { cicilanTenor = t }
                        ) {
                            Text(
                                text = "${t}x Bulan",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Schedule Preview
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Jadwal: ${DateUtils.formatYearMonthFull(cicilanStartYm)} s/d ${DateUtils.formatYearMonthFull(DateUtils.plusMonths(cicilanStartYm, (cicilanTenor - 1).toLong()))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                val isValidCicilan = (cicilanAmountText.toLongOrNull() ?: 0L) > 0 && selectedServiceId.isNotBlank()
                Button(
                    onClick = {
                        val amount = cicilanAmountText.toLongOrNull() ?: 0L
                        val safeTenor = cicilanTenor.coerceAtLeast(1)
                        val itemToEdit = editingItem
                        if (itemToEdit != null && onUpdateCicilan != null) {
                            onUpdateCicilan.invoke(
                                itemToEdit.id,
                                selectedServiceId,
                                cicilanTitle,
                                amount,
                                safeTenor,
                                cicilanStartYm
                            )
                        } else {
                            onAddCicilan(
                                selectedServiceId,
                                cicilanTitle,
                                amount,
                                safeTenor,
                                cicilanStartYm
                            )
                        }
                        onDismiss()
                    },
                    enabled = isValidCicilan,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text(
                        text = if (isEditing) "Simpan Perubahan" else "Simpan Cicilan",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            } else {
                // Scratchpad / Hitung Cepat Form
                OutlinedTextField(
                    value = scratchpadTitle,
                    onValueChange = { scratchpadTitle = it },
                    label = { Text("Keterangan Tagihan (Opsional)") },
                    placeholder = { Text("Contoh: Belanja Shopee 10.10") },
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = scratchpadExpression,
                    onValueChange = { scratchpadExpression = it },
                    label = { Text("Ketik atau Tempel Penjumlahan") },
                    placeholder = { Text("Contoh: 12182 + 4252 + 1000 + 98744") },
                    shape = RoundedCornerShape(10.dp),
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Real-time Calculator Box
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOTAL PERHITUNGAN",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (mathResult.isValid) {
                                Text(
                                    text = "${mathResult.terms.size} transaksi",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (mathResult.isValid) {
                                DateUtils.formatCurrency(mathResult.total, currencyCode)
                            } else {
                                "Ketik angka dipisah tanda + (misal: 10000 + 25000)"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 22.sp
                            ),
                            color = if (mathResult.isValid) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        val itemToEdit = editingItem
                        if (itemToEdit != null && onUpdateScratchpad != null) {
                            onUpdateScratchpad.invoke(
                                itemToEdit.id,
                                selectedServiceId,
                                scratchpadTitle,
                                scratchpadExpression,
                                mathResult.total,
                                defaultYm
                            )
                        } else {
                            onAddScratchpad(
                                selectedServiceId,
                                scratchpadTitle,
                                scratchpadExpression,
                                mathResult.total,
                                defaultYm
                            )
                        }
                        onDismiss()
                    },
                    enabled = mathResult.isValid && mathResult.total > 0 && selectedServiceId.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text(
                        text = if (isEditing) "Simpan Perubahan" else "Tambahkan ke Tagihan Bulan Ini",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            if (editingItem != null && onDeleteItem != null) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Hapus Tagihan Ini",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        if (showDeleteConfirmDialog && editingItem != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                },
                title = {
                    Text(
                        text = "Hapus Tagihan?",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    Text(
                        text = "Tagihan \"${editingItem.title.ifBlank { "Tagihan" }}\" akan dihapus secara permanen.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirmDialog = false
                            onDeleteItem?.invoke(editingItem.id)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text("Hapus")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}
