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
        val paidService = createStatement("s1", "SPayLater", hasItems = true, isPaid = true, daysUntilDue = 5)
        val unpaidOverdue = createStatement("s2", "Kredivo", hasItems = true, isPaid = false, daysUntilDue = -1)
        val unpaidDueSoon = createStatement("s3", "GoPayLater", hasItems = true, isPaid = false, daysUntilDue = 2)
        val emptyService = createStatement("s4", "Indodana", hasItems = false, isPaid = false, daysUntilDue = 10)

        val statements = listOf(paidService, emptyService, unpaidDueSoon, unpaidOverdue)

        val sorted = statements.sortedWith(
            compareBy<MonthlyServiceStatement> { statement ->
                when {
                    statement.items.isNotEmpty() && !statement.isPaid -> 0
                    statement.items.isNotEmpty() && statement.isPaid -> 1
                    else -> 2
                }
            }.thenBy { it.daysUntilDue }
            .thenBy { it.service.name }
        )

        assertEquals("Kredivo", sorted[0].service.name) // Unpaid overdue (-1)
        assertEquals("GoPayLater", sorted[1].service.name) // Unpaid due soon (2)
        assertEquals("SPayLater", sorted[2].service.name) // Paid (5)
        assertEquals("Indodana", sorted[3].service.name) // Empty (no items)
    }
}
