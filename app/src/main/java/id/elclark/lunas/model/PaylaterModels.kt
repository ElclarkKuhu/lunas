package id.elclark.lunas.model

import kotlinx.serialization.Serializable

@Serializable
data class PaylaterService(
    val id: String,
    val name: String,
    val billingCutoffDay: Int, // e.g. 21
    val dueDay: Int,            // e.g. 1
    val dueMonthOffset: Int,    // 0 = same month, 1 = next month
    val colorHex: Long,         // e.g. 0xFFEE4D2D
    val isActive: Boolean = true
)

@Serializable
data class BillItem(
    val id: String,
    val serviceId: String,
    val title: String,
    val amountPerMonth: Long,
    val totalTenor: Int = 1,       // 1 for single/scratchpad, 3 for 3x, etc.
    val startYearMonth: String,    // "YYYY-MM", e.g. "2026-10"
    val rawMathExpression: String? = null, // e.g. "12182 + 4252 + 1000 + 98744"
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class PaymentStatus(
    val serviceId: String,
    val yearMonth: String,         // "YYYY-MM"
    val isPaid: Boolean,
    val paidAt: Long? = null
)

@Serializable
data class AppSettings(
    val biometricEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val reminderDaysBefore: Int = 2, // H-2 before jatuh tempo
    val reminderHour: Int = 9        // 09:00 WIB
)

data class ComputedBillItem(
    val item: BillItem,
    val currentTenorIndex: Int,    // e.g. 1 for 1st month of 3
    val totalTenor: Int,           // e.g. 3
    val monthlyAmount: Long,
    val isInstallment: Boolean
)

data class MonthlyServiceStatement(
    val service: PaylaterService,
    val yearMonth: String,
    val billingCycleText: String,  // e.g. "(21 Okt - 1 Nov)"
    val items: List<ComputedBillItem>,
    val subtotal: Long,
    val isPaid: Boolean,
    val dueDateText: String,       // e.g. "1 Nov 2026"
    val daysUntilDue: Int,
    val isDueSoon: Boolean,        // within 3 days
    val isOverdue: Boolean
)

data class MonthlyOverview(
    val yearMonth: String,
    val totalDue: Long,
    val totalPaid: Long,
    val paidCount: Int,
    val totalCount: Int,
    val statements: List<MonthlyServiceStatement>
)

@Serializable
data class BackupData(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val services: List<PaylaterService>,
    val items: List<BillItem>,
    val payments: List<PaymentStatus>,
    val settings: AppSettings
)

data class ServiceDebtBreakdown(
    val service: PaylaterService,
    val remainingDebt: Long,
    val activeInstallmentsCount: Int,
    val latestMonth: String?
)

data class TotalOwedSummary(
    val grandTotalOwed: Long,
    val currentMonthRemaining: Long,
    val futureInstallmentsOwed: Long,
    val pastOverdueOwed: Long,
    val latestEndMonth: String?,
    val serviceBreakdowns: List<ServiceDebtBreakdown>
)
