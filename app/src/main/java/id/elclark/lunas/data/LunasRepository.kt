package id.elclark.lunas.data

import android.content.Context
import id.elclark.lunas.model.*
import id.elclark.lunas.util.DateUtils
import id.elclark.lunas.widget.LunasWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.time.YearMonth
import java.util.UUID

class LunasRepository(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: LunasRepository? = null

        fun getInstance(context: Context): LunasRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LunasRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val dbHelper = LunasDatabaseHelper(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _services = MutableStateFlow<List<PaylaterService>>(emptyList())
    val services: StateFlow<List<PaylaterService>> = _services.asStateFlow()

    private val _items = MutableStateFlow<List<BillItem>>(emptyList())
    val items: StateFlow<List<BillItem>> = _items.asStateFlow()

    private val _payments = MutableStateFlow<List<PaymentStatus>>(emptyList())
    val payments: StateFlow<List<PaymentStatus>> = _payments.asStateFlow()

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    init {
        reloadAll()
    }

    suspend fun reloadAllNow() = withContext(Dispatchers.IO) {
        val s = dbHelper.getAllServices()
        val i = dbHelper.getAllItems()
        val p = dbHelper.getAllPaymentStatuses()
        val cfg = dbHelper.getSettings()
        _services.value = s
        _items.value = i
        _payments.value = p
        _settings.value = cfg
        LunasWidgetProvider.updateAllWidgets(context)
    }

    fun reloadAll() {
        scope.launch {
            reloadAllNow()
        }
    }

    suspend fun addBillItem(
        serviceId: String,
        title: String,
        amountPerMonth: Long,
        totalTenor: Int,
        startYearMonth: String,
        rawMathExpression: String? = null
    ): BillItem = withContext(Dispatchers.IO) {
        val item = BillItem(
            id = UUID.randomUUID().toString(),
            serviceId = serviceId,
            title = title,
            amountPerMonth = amountPerMonth,
            totalTenor = totalTenor,
            startYearMonth = startYearMonth,
            rawMathExpression = rawMathExpression,
            createdAt = System.currentTimeMillis()
        )
        dbHelper.insertItem(item)
        reloadAll()
        item
    }

    suspend fun updateBillItem(
        id: String,
        serviceId: String,
        title: String,
        amountPerMonth: Long,
        totalTenor: Int,
        startYearMonth: String,
        rawMathExpression: String? = null
    ) = withContext(Dispatchers.IO) {
        val existing = _items.value.find { it.id == id }
        val item = BillItem(
            id = id,
            serviceId = serviceId,
            title = title,
            amountPerMonth = amountPerMonth,
            totalTenor = totalTenor,
            startYearMonth = startYearMonth,
            rawMathExpression = rawMathExpression,
            createdAt = existing?.createdAt ?: System.currentTimeMillis()
        )
        dbHelper.insertItem(item)
        reloadAll()
    }

    suspend fun deleteBillItem(id: String) = withContext(Dispatchers.IO) {
        dbHelper.deleteItem(id)
        reloadAll()
    }

    suspend fun togglePaymentStatus(serviceId: String, yearMonth: String) = withContext(Dispatchers.IO) {
        val currentStatus = _payments.value.find { it.serviceId == serviceId && it.yearMonth == yearMonth }
        val newIsPaid = !(currentStatus?.isPaid ?: false)
        dbHelper.setPaymentStatus(serviceId, yearMonth, newIsPaid)
        reloadAll()
    }

    suspend fun saveService(service: PaylaterService) = withContext(Dispatchers.IO) {
        dbHelper.upsertService(service)
        reloadAll()
    }

    suspend fun archiveService(serviceId: String) = withContext(Dispatchers.IO) {
        dbHelper.archiveService(serviceId)
        reloadAll()
    }

    suspend fun updateSettings(settings: AppSettings) = withContext(Dispatchers.IO) {
        dbHelper.saveSettings(settings)
        _settings.value = settings
    }

    fun computeMonthlyOverview(
        targetYm: String,
        allServices: List<PaylaterService>,
        allItems: List<BillItem>,
        allPayments: List<PaymentStatus>
    ): MonthlyOverview {
        val statements = mutableListOf<MonthlyServiceStatement>()

        var totalDue = 0L
        var totalPaid = 0L
        var paidCount = 0

        for (svc in allServices) {
            // Find items active in targetYm
            val computedItems = mutableListOf<ComputedBillItem>()
            var subtotal = 0L

            for (item in allItems) {
                if (item.serviceId != svc.id) continue
                val diff = DateUtils.monthDifference(item.startYearMonth, targetYm)
                if (diff in 0 until item.totalTenor) {
                    val computed = ComputedBillItem(
                        item = item,
                        currentTenorIndex = diff + 1,
                        totalTenor = item.totalTenor,
                        monthlyAmount = item.amountPerMonth,
                        isInstallment = item.totalTenor > 1
                    )
                    computedItems.add(computed)
                    subtotal += item.amountPerMonth
                }
            }

            // Only show services that have items, or show active ones anyway
            val isPaid = allPayments.find { it.serviceId == svc.id && it.yearMonth == targetYm }?.isPaid ?: false
            val dueDate = DateUtils.computeDueDate(targetYm, svc.dueDay, svc.dueMonthOffset)
            val daysUntil = DateUtils.daysUntil(dueDate)

            val statement = MonthlyServiceStatement(
                service = svc,
                yearMonth = targetYm,
                billingCycleText = DateUtils.computeBillingCycleText(
                    targetYm,
                    svc.billingCutoffDay,
                    svc.dueDay,
                    svc.dueMonthOffset
                ),
                items = computedItems,
                subtotal = subtotal,
                isPaid = isPaid,
                dueDateText = DateUtils.formatDueDate(dueDate),
                daysUntilDue = daysUntil,
                isDueSoon = daysUntil in 0..3 && !isPaid,
                isOverdue = daysUntil < 0 && !isPaid
            )

            // If it has items or is a known default service, include it
            if (computedItems.isNotEmpty() || svc.isActive) {
                statements.add(statement)
                totalDue += subtotal
                if (isPaid && subtotal > 0) {
                    totalPaid += subtotal
                    paidCount++
                }
            }
        }

        val servicesWithBillsCount = statements.count { it.subtotal > 0 }

        return MonthlyOverview(
            yearMonth = targetYm,
            totalDue = totalDue,
            totalPaid = totalPaid,
            paidCount = paidCount,
            totalCount = servicesWithBillsCount,
            statements = statements
        )
    }

    fun computeTotalOwed(
        currentYm: String = DateUtils.currentYearMonth(),
        allServices: List<PaylaterService>,
        allItems: List<BillItem>,
        allPayments: List<PaymentStatus>
    ): TotalOwedSummary {
        var grandTotal = 0L
        var currentMonthRem = 0L
        var futureInstallments = 0L
        var pastOverdue = 0L
        var latestYm: String? = null

        val breakdowns = mutableListOf<ServiceDebtBreakdown>()

        for (svc in allServices) {
            val svcItems = allItems.filter { it.serviceId == svc.id }
            if (svcItems.isEmpty()) continue

            var svcDebt = 0L
            var svcActiveInstallments = 0
            var svcLatestYm: String? = null

            for (item in svcItems) {
                var isInstallmentCounted = false
                for (t in 0 until item.totalTenor) {
                    val ym = DateUtils.plusMonths(item.startYearMonth, t.toLong())
                    val isPaid = allPayments.any { it.serviceId == svc.id && it.yearMonth == ym && it.isPaid }
                    if (!isPaid) {
                        svcDebt += item.amountPerMonth
                        grandTotal += item.amountPerMonth

                        when {
                            ym < currentYm -> pastOverdue += item.amountPerMonth
                            ym == currentYm -> currentMonthRem += item.amountPerMonth
                            else -> futureInstallments += item.amountPerMonth
                        }

                        if (!isInstallmentCounted && item.totalTenor > 1) {
                            svcActiveInstallments++
                            isInstallmentCounted = true
                        }

                        if (svcLatestYm == null || ym > svcLatestYm) {
                            svcLatestYm = ym
                        }
                        if (latestYm == null || ym > latestYm) {
                            latestYm = ym
                        }
                    }
                }
            }

            if (svcDebt > 0) {
                breakdowns.add(
                    ServiceDebtBreakdown(
                        service = svc,
                        remainingDebt = svcDebt,
                        activeInstallmentsCount = svcActiveInstallments,
                        latestMonth = svcLatestYm?.let { DateUtils.formatYearMonthFull(it) }
                    )
                )
            }
        }

        return TotalOwedSummary(
            grandTotalOwed = grandTotal,
            currentMonthRemaining = currentMonthRem,
            futureInstallmentsOwed = futureInstallments,
            pastOverdueOwed = pastOverdue,
            latestEndMonth = latestYm?.let { DateUtils.formatYearMonthFull(it) },
            serviceBreakdowns = breakdowns.sortedByDescending { it.remainingDebt }
        )
    }

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val backup = BackupData(
            services = dbHelper.getAllServices(),
            items = dbHelper.getAllItems(),
            payments = dbHelper.getAllPaymentStatuses(),
            settings = dbHelper.getSettings()
        )
        json.encodeToString(backup)
    }

    suspend fun importJsonData(rawText: String, merge: Boolean = true): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var clean = rawText.trim()
            if (clean.startsWith("```")) {
                clean = clean.substringAfter("\n")
            }
            if (clean.endsWith("```")) {
                clean = clean.substringBeforeLast("```").trim()
            }

            val jsonElement = json.parseToJsonElement(clean)
            var count = 0

            var currentServices = mutableListOf<PaylaterService>()
            fun ensureServiceExists(serviceId: String) {
                if (currentServices.none { it.id.equals(serviceId, ignoreCase = true) }) {
                    val fallbackName = serviceId.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    val newSvc = PaylaterService(
                        id = serviceId,
                        name = fallbackName,
                        billingCutoffDay = 25,
                        dueDay = 5,
                        dueMonthOffset = 1,
                        colorHex = 0xFF4F46E5,
                        isActive = true
                    )
                    dbHelper.upsertService(newSvc)
                    currentServices.add(newSvc)
                }
            }

            fun parseItem(obj: JsonObject): BillItem {
                val id = obj["id"]?.jsonPrimitive?.contentOrNull?.ifBlank { null } ?: UUID.randomUUID().toString()
                val rawService = obj["serviceId"]?.jsonPrimitive?.contentOrNull
                    ?: obj["service"]?.jsonPrimitive?.contentOrNull
                    ?: "spaylater"
                val serviceId = when {
                    rawService.contains("gopay", ignoreCase = true) -> "gopaylater"
                    rawService.contains("shopee", ignoreCase = true) || rawService.contains("spay", ignoreCase = true) -> "spaylater"
                    rawService.contains("yup", ignoreCase = true) -> "yup"
                    else -> rawService.lowercase().replace(" ", "").replace("_", "").replace("-", "")
                }

                val title = obj["title"]?.jsonPrimitive?.contentOrNull
                    ?: obj["name"]?.jsonPrimitive?.contentOrNull
                    ?: obj["keterangan"]?.jsonPrimitive?.contentOrNull
                    ?: "Tagihan"
                val amount = obj["amountPerMonth"]?.jsonPrimitive?.longOrNull
                    ?: obj["amount"]?.jsonPrimitive?.longOrNull
                    ?: obj["nominal"]?.jsonPrimitive?.longOrNull
                    ?: obj["totalAmount"]?.jsonPrimitive?.longOrNull
                    ?: 0L
                val tenor = obj["totalTenor"]?.jsonPrimitive?.intOrNull
                    ?: obj["tenor"]?.jsonPrimitive?.intOrNull
                    ?: 1
                val startYm = obj["startYearMonth"]?.jsonPrimitive?.contentOrNull
                    ?: obj["startMonth"]?.jsonPrimitive?.contentOrNull
                    ?: obj["month"]?.jsonPrimitive?.contentOrNull
                    ?: DateUtils.currentYearMonth()
                YearMonth.parse(startYm)
                ensureServiceExists(serviceId)
                val math = obj["rawMathExpression"]?.jsonPrimitive?.contentOrNull
                    ?: obj["math"]?.jsonPrimitive?.contentOrNull
                    ?: obj["expression"]?.jsonPrimitive?.contentOrNull

                return BillItem(
                    id = id,
                    serviceId = serviceId,
                    title = title,
                    amountPerMonth = amount,
                    totalTenor = tenor,
                    startYearMonth = startYm,
                    rawMathExpression = math,
                    createdAt = System.currentTimeMillis()
                )
            }

            fun validateImportedItems(items: List<BillItem>) {
                items.forEach { YearMonth.parse(it.startYearMonth) }
            }

            dbHelper.runInTransaction {
                currentServices = dbHelper.getAllServices().toMutableList()
                when (jsonElement) {
                    is JsonArray -> {
                        for (el in jsonElement) {
                            if (el is JsonObject) {
                                val item = parseItem(el)
                                dbHelper.insertItem(item)
                                count++
                            }
                        }
                    }
                    is JsonObject -> {
                        if (jsonElement.containsKey("services") && jsonElement.containsKey("items")) {
                            val backup = json.decodeFromJsonElement<BackupData>(jsonElement)
                            validateImportedItems(backup.items)
                            if (merge) {
                                for (s in backup.services) {
                                    dbHelper.upsertService(s)
                                }
                                for (item in backup.items) {
                                    dbHelper.insertItem(item)
                                    count++
                                }
                                for (p in backup.payments) {
                                    dbHelper.setPaymentStatus(p.serviceId, p.yearMonth, p.isPaid)
                                }
                                dbHelper.saveSettings(backup.settings)
                            } else {
                                val servicesToRestore = if (backup.services.isNotEmpty()) backup.services else dbHelper.getAllServices()
                                dbHelper.clearAndRestoreAll(servicesToRestore, backup.items, backup.payments, backup.settings)
                                count = backup.items.size
                            }
                        } else if (jsonElement.containsKey("items")) {
                            val itemsArray = jsonElement["items"]?.jsonArray
                            if (itemsArray != null) {
                                for (el in itemsArray) {
                                    if (el is JsonObject) {
                                        val item = parseItem(el)
                                        dbHelper.insertItem(item)
                                        count++
                                    }
                                }
                            }
                        } else if (jsonElement.containsKey("serviceId") || jsonElement.containsKey("service") || jsonElement.containsKey("amount")) {
                            val item = parseItem(jsonElement)
                            dbHelper.insertItem(item)
                            count++
                        } else {
                            val backup = json.decodeFromJsonElement<BackupData>(jsonElement)
                            validateImportedItems(backup.items)
                            if (merge) {
                                for (s in backup.services) {
                                    dbHelper.upsertService(s)
                                }
                                for (item in backup.items) {
                                    dbHelper.insertItem(item)
                                    count++
                                }
                                for (p in backup.payments) {
                                    dbHelper.setPaymentStatus(p.serviceId, p.yearMonth, p.isPaid)
                                }
                            } else {
                                dbHelper.clearAndRestoreAll(backup.services, backup.items, backup.payments, backup.settings)
                                count = backup.items.size
                            }
                        }
                    }
                    else -> throw IllegalArgumentException("Format JSON harus berupa Array atau Object")
                }
            }

            reloadAllNow()
            Result.success(count)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun importBackupJson(jsonString: String): Boolean {
        val res = importJsonData(jsonString, merge = false)
        return res.isSuccess
    }
}
