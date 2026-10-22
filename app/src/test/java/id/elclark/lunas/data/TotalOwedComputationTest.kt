package id.elclark.lunas.data

import id.elclark.lunas.model.BillItem
import id.elclark.lunas.model.PaylaterService
import id.elclark.lunas.model.PaymentStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TotalOwedComputationTest {

    private fun createService(
        id: String = "spaylater",
        dueDay: Int = 5,
        offset: Int = 1
    ) = PaylaterService(
        id = id,
        name = "SPayLater",
        colorHex = 0xFFEE4D2D,
        billingCutoffDay = 25,
        dueDay = dueDay,
        dueMonthOffset = offset,
        isActive = true
    )

    private fun createItem(
        id: String = "item-1",
        serviceId: String = "spaylater",
        amount: Long = 100_000L,
        tenor: Int = 1,
        startYm: String = "2026-10"
    ) = BillItem(
        id = id,
        serviceId = serviceId,
        title = "Test Bill",
        amountPerMonth = amount,
        totalTenor = tenor,
        startYearMonth = startYm,
        createdAt = System.currentTimeMillis()
    )

    @Test
    fun classifiesBillInCurrentMonthPastDueDateAsOverdue() {
        // Current month is 2026-10. Bill starts in 2026-10.
        // Service due date in same month: due on 10th of Oct (dueMonthOffset = 0).
        // Today is Oct 15th (due date 2026-10-10 has passed).
        val service = createService(dueDay = 10, offset = 0)
        val item = createItem(startYm = "2026-10", amount = 150_000L)
        val today = LocalDate.of(2026, 10, 15)

        val summary = LunasRepository.calculateTotalOwed(
            currentYm = "2026-10",
            allServices = listOf(service),
            allItems = listOf(item),
            allPayments = emptyList(),
            today = today
        )

        assertEquals(150_000L, summary.grandTotalOwed)
        assertEquals(150_000L, summary.pastOverdueOwed)
        assertEquals(0L, summary.currentMonthRemaining)
        assertEquals(0L, summary.futureInstallmentsOwed)
    }

    @Test
    fun classifiesBillInCurrentMonthBeforeDueDateAsCurrentMonthRemaining() {
        // Due on Oct 25th, today is Oct 15th.
        val service = createService(dueDay = 25, offset = 0)
        val item = createItem(startYm = "2026-10", amount = 200_000L)
        val today = LocalDate.of(2026, 10, 15)

        val summary = LunasRepository.calculateTotalOwed(
            currentYm = "2026-10",
            allServices = listOf(service),
            allItems = listOf(item),
            allPayments = emptyList(),
            today = today
        )

        assertEquals(200_000L, summary.grandTotalOwed)
        assertEquals(0L, summary.pastOverdueOwed)
        assertEquals(200_000L, summary.currentMonthRemaining)
        assertEquals(0L, summary.futureInstallmentsOwed)
    }

    @Test
    fun classifiesOffsetMonthBillDueThisMonthCorrectly() {
        // Statement is from 2026-09. Offset = 1, DueDay = 5 -> Due on 2026-10-05.
        val service = createService(dueDay = 5, offset = 1)
        val item = createItem(startYm = "2026-09", amount = 300_000L)

        // Case A: Today is Oct 2nd (before due date of Oct 5th) -> current month remaining, NOT overdue!
        val todayBeforeDue = LocalDate.of(2026, 10, 2)
        val summaryBefore = LunasRepository.calculateTotalOwed(
            currentYm = "2026-10",
            allServices = listOf(service),
            allItems = listOf(item),
            allPayments = emptyList(),
            today = todayBeforeDue
        )
        assertEquals(0L, summaryBefore.pastOverdueOwed)
        assertEquals(300_000L, summaryBefore.currentMonthRemaining)

        // Case B: Today is Oct 10th (after due date of Oct 5th) -> past overdue!
        val todayAfterDue = LocalDate.of(2026, 10, 10)
        val summaryAfter = LunasRepository.calculateTotalOwed(
            currentYm = "2026-10",
            allServices = listOf(service),
            allItems = listOf(item),
            allPayments = emptyList(),
            today = todayAfterDue
        )
        assertEquals(300_000L, summaryAfter.pastOverdueOwed)
        assertEquals(0L, summaryAfter.currentMonthRemaining)
    }

    @Test
    fun classifiesFutureInstallmentsCorrectly() {
        // Multi-tenor installment: starts 2026-10, 3 months (Oct, Nov, Dec), 100k/month
        // Due on 25th of month (offset=0). Today is Oct 15th.
        val service = createService(dueDay = 25, offset = 0)
        val item = createItem(startYm = "2026-10", tenor = 3, amount = 100_000L)
        val today = LocalDate.of(2026, 10, 15)

        val summary = LunasRepository.calculateTotalOwed(
            currentYm = "2026-10",
            allServices = listOf(service),
            allItems = listOf(item),
            allPayments = emptyList(),
            today = today
        )

        assertEquals(300_000L, summary.grandTotalOwed)
        assertEquals(0L, summary.pastOverdueOwed)
        assertEquals(100_000L, summary.currentMonthRemaining)
        assertEquals(200_000L, summary.futureInstallmentsOwed)
    }

    @Test
    fun excludesPaidBillsFromDebtSummary() {
        val service = createService(dueDay = 25, offset = 0)
        val item = createItem(startYm = "2026-10", tenor = 2, amount = 100_000L)
        val today = LocalDate.of(2026, 10, 15)

        // Oct is paid, Nov is unpaid
        val payments = listOf(
            PaymentStatus(serviceId = service.id, yearMonth = "2026-10", isPaid = true)
        )

        val summary = LunasRepository.calculateTotalOwed(
            currentYm = "2026-10",
            allServices = listOf(service),
            allItems = listOf(item),
            allPayments = payments,
            today = today
        )

        assertEquals(100_000L, summary.grandTotalOwed)
        assertEquals(0L, summary.pastOverdueOwed)
        assertEquals(0L, summary.currentMonthRemaining)
        assertEquals(100_000L, summary.futureInstallmentsOwed)
    }

    @Test
    fun classifiesCurrentMonthUsageWithNextMonthDueAsFutureInstallment() {
        // Current month usage (2026-10). Offset = 1, DueDay = 5 -> Due on 2026-11-05.
        // Today is Oct 15th. It is NOT due in October; it is due in November (future installment).
        val service = createService(dueDay = 5, offset = 1)
        val item = createItem(startYm = "2026-10", amount = 100_000L)
        val today = LocalDate.of(2026, 10, 15)

        val summary = LunasRepository.calculateTotalOwed(
            currentYm = "2026-10",
            allServices = listOf(service),
            allItems = listOf(item),
            allPayments = emptyList(),
            today = today
        )

        assertEquals(100_000L, summary.grandTotalOwed)
        assertEquals(0L, summary.pastOverdueOwed)
        assertEquals(0L, summary.currentMonthRemaining)
        assertEquals(100_000L, summary.futureInstallmentsOwed)
    }

    @Test
    fun classifiesMultipleServicesWithDifferentOffsetsCorrectly() {
        // Service A: offset 0, due 20th. Today is 15th -> current month remaining
        val serviceA = createService(id = "svcA", dueDay = 20, offset = 0)
        val itemA = createItem(id = "itemA", serviceId = "svcA", amount = 50_000L, startYm = "2026-10")

        // Service B: offset 1, due 5th.
        // - Bill from 2026-09 is due 2026-10-05 (which has passed on Oct 15) -> past overdue
        // - Bill from 2026-10 is due 2026-11-05 -> future installments
        val serviceB = createService(id = "svcB", dueDay = 5, offset = 1)
        val itemB1 = createItem(id = "itemB1", serviceId = "svcB", amount = 80_000L, startYm = "2026-09")
        val itemB2 = createItem(id = "itemB2", serviceId = "svcB", amount = 120_000L, startYm = "2026-10")

        val today = LocalDate.of(2026, 10, 15)
        val summary = LunasRepository.calculateTotalOwed(
            currentYm = "2026-10",
            allServices = listOf(serviceA, serviceB),
            allItems = listOf(itemA, itemB1, itemB2),
            allPayments = emptyList(),
            today = today
        )

        assertEquals(250_000L, summary.grandTotalOwed)
        assertEquals(80_000L, summary.pastOverdueOwed)
        assertEquals(50_000L, summary.currentMonthRemaining)
        assertEquals(120_000L, summary.futureInstallmentsOwed)
    }
}
