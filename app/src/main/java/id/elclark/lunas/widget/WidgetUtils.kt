package id.elclark.lunas.widget

import android.content.Context
import id.elclark.lunas.R
import id.elclark.lunas.model.MonthlyOverview
import id.elclark.lunas.model.MonthlyServiceStatement
import id.elclark.lunas.util.DateUtils

object WidgetUtils {

    /**
     * Selects the next upcoming unpaid bill from the monthly overview,
     * sorted by days until due.
     */
    fun selectNextBill(overview: MonthlyOverview): MonthlyServiceStatement? {
        return overview.statements
            .filter { !it.isPaid && it.subtotal > 0 }
            .sortedBy { it.daysUntilDue }
            .firstOrNull()
    }

    /**
     * Formats the due display for an upcoming bill, returning a Triple of:
     * - description text (e.g. Overdue, Today, Tomorrow, or in N days)
     * - textColor resource ID
     * - icon drawable resource ID
     */
    fun formatUpcomingDueDisplay(
        context: Context,
        nextBill: MonthlyServiceStatement
    ): Triple<String, Int, Int> {
        val dueFormatted = nextBill.dueDate?.let { DateUtils.formatDueDateShort(it) } ?: nextBill.dueDateText
        return when {
            nextBill.daysUntilDue < 0 -> {
                val days = -nextBill.daysUntilDue
                Triple(
                    context.getString(R.string.widget_due_overdue, days, dueFormatted),
                    R.color.status_overdue,
                    R.drawable.ic_widget_warning
                )
            }
            nextBill.daysUntilDue == 0 -> {
                Triple(
                    context.getString(R.string.widget_due_today, dueFormatted),
                    R.color.status_due_soon,
                    R.drawable.ic_widget_calendar
                )
            }
            nextBill.daysUntilDue == 1 -> {
                Triple(
                    context.getString(R.string.widget_due_tomorrow, dueFormatted),
                    R.color.status_due_soon,
                    R.drawable.ic_widget_calendar
                )
            }
            else -> {
                Triple(
                    context.getString(R.string.widget_due_days, dueFormatted, nextBill.daysUntilDue),
                    R.color.dark_text_secondary,
                    R.drawable.ic_widget_calendar
                )
            }
        }
    }
}
