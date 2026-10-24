package id.elclark.lunas.data

import id.elclark.lunas.model.BillItem
import id.elclark.lunas.model.ComputedBillItem
import id.elclark.lunas.model.MonthlyServiceStatement
import id.elclark.lunas.model.PaylaterService
import org.junit.Assert.assertEquals
import org.junit.Test

class StatementSortingTest {

    private fun createStatement(
        serviceId: String,
        serviceName: String,
        hasItems: Boolean,
        isPaid: Boolean,
        daysUntilDue: Int
    ): MonthlyServiceStatement {
        val service = PaylaterService(
            id = serviceId,
            name = serviceName,
            colorHex = 0xFF000000L,
            billingCutoffDay = 20,
            dueDay = 1,
            dueMonthOffset = 1
        )
        val items = if (hasItems) {
            listOf(
                ComputedBillItem(
                    item = BillItem(
                        id = "item-$serviceId",
                        serviceId = serviceId,
                        title = "Test Item",
                        amountPerMonth = 50_000,
                        totalTenor = 1,
                        startYearMonth = "2026-10"
                    ),
                    currentTenorIndex = 1,
                    totalTenor = 1,
                    monthlyAmount = 50_000,
                    isInstallment = false
                )
            )
        } else {
            emptyList()
        }

        return MonthlyServiceStatement(
            service = service,
            yearMonth = "2026-10",
            billingCycleText = "(20 Okt - 1 Nov)",
            items = items,
            subtotal = if (hasItems) 50_000 else 0L,
            isPaid = isPaid,
            dueDateText = "1 Nov 2026",
            daysUntilDue = daysUntilDue,
            isDueSoon = daysUntilDue in 0..3 && !isPaid,
            isOverdue = daysUntilDue < 0 && !isPaid
        )
    }

    @Test
    fun sortsUnpaidBeforePaidAndEmptyAtBottom() {
        val s1 = PaylaterService(id = "s1", name = "SPayLater", colorHex = 0xFF000000L, billingCutoffDay = 20, dueDay = 25, dueMonthOffset = 0, isActive = true)
        val s2 = PaylaterService(id = "s2", name = "Kredivo", colorHex = 0xFF000000L, billingCutoffDay = 20, dueDay = 1, dueMonthOffset = 0, isActive = true)
        val s3 = PaylaterService(id = "s3", name = "GoPayLater", colorHex = 0xFF000000L, billingCutoffDay = 20, dueDay = 15, dueMonthOffset = 0, isActive = true)
        val s4 = PaylaterService(id = "s4", name = "Indodana", colorHex = 0xFF000000L, billingCutoffDay = 20, dueDay = 28, dueMonthOffset = 0, isActive = true)

        val items = listOf(
            BillItem("i1", "s1", "Item 1", 50_000, 1, "2026-10"),
            BillItem("i2", "s2", "Item 2", 50_000, 1, "2026-10"),
            BillItem("i3", "s3", "Item 3", 50_000, 1, "2026-10")
        )
        // s1 is paid, s2 and s3 unpaid, s4 has no items
        val payments = listOf(
            id.elclark.lunas.model.PaymentStatus("s1", "2026-10", isPaid = true)
        )

        val overview = LunasRepository.computeMonthlyOverview(
            targetYm = "2026-10",
            allServices = listOf(s1, s4, s3, s2),
            allItems = items,
            allPayments = payments
        )

        val sorted = overview.statements
        assertEquals("Kredivo", sorted[0].service.name) // Unpaid (due day 1)
        assertEquals("GoPayLater", sorted[1].service.name) // Unpaid (due day 15)
        assertEquals("SPayLater", sorted[2].service.name) // Paid (due day 25)
        assertEquals("Indodana", sorted[3].service.name) // Empty (no items)
    }

    @Test
    fun tieBreakerRespectsCustomServiceOrderThenName() {
        // Both s1 and s2 have identical due day
        val s1 = PaylaterService(id = "s1", name = "SPayLater", colorHex = 0xFF000000L, billingCutoffDay = 20, dueDay = 10, dueMonthOffset = 0, isActive = true)
        val s2 = PaylaterService(id = "s2", name = "GoPayLater", colorHex = 0xFF000000L, billingCutoffDay = 20, dueDay = 10, dueMonthOffset = 0, isActive = true)

        val items = listOf(
            BillItem("i1", "s1", "Item 1", 50_000, 1, "2026-10"),
            BillItem("i2", "s2", "Item 2", 50_000, 1, "2026-10")
        )

        // Custom order gives priority to s2
        val overview = LunasRepository.computeMonthlyOverview(
            targetYm = "2026-10",
            allServices = listOf(s1, s2),
            allItems = items,
            allPayments = emptyList(),
            serviceOrder = listOf("s2", "s1")
        )

        assertEquals("GoPayLater", overview.statements[0].service.name)
        assertEquals("SPayLater", overview.statements[1].service.name)
    }
}
