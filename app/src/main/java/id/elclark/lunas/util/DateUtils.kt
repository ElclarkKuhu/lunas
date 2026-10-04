package id.elclark.lunas.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object DateUtils {

    private val ID_MONTH_NAMES_FULL = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    private val ID_MONTH_NAMES_SHORT = listOf(
        "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
        "Jul", "Agt", "Sep", "Okt", "Nov", "Des"
    )

    fun currentYearMonth(): String {
        return YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
    }

    fun parseYearMonth(ym: String): YearMonth {
        return try {
            YearMonth.parse(ym)
        } catch (_: Exception) {
            YearMonth.now()
        }
    }

    fun formatYearMonthFull(ym: String): String {
        val parsed = parseYearMonth(ym)
        val monthName = ID_MONTH_NAMES_FULL.getOrElse(parsed.monthValue - 1) { parsed.month.name }
        return "$monthName ${parsed.year}"
    }

    fun formatYearMonthShort(ym: String): String {
        val parsed = parseYearMonth(ym)
        val monthName = ID_MONTH_NAMES_SHORT.getOrElse(parsed.monthValue - 1) { parsed.month.name }
        return "$monthName ${parsed.year}"
    }

    fun plusMonths(ym: String, monthsToAdd: Long): String {
        val parsed = parseYearMonth(ym)
        return parsed.plusMonths(monthsToAdd).format(DateTimeFormatter.ofPattern("yyyy-MM"))
    }

    fun monthDifference(fromYm: String, toYm: String): Int {
        val from = parseYearMonth(fromYm)
        val to = parseYearMonth(toYm)
        return ChronoUnit.MONTHS.between(from, to).toInt()
    }

    /**
     * Computes the billing cycle description, e.g. "(21 Okt - 1 Nov)" or "(13 Okt - 22 Okt)"
     * @param ym the statement month (the month the bill is due or listed under)
     */
    fun computeBillingCycleText(
        ym: String,
        billingCutoffDay: Int,
        dueDay: Int,
        dueMonthOffset: Int
    ): String {
        val currentYm = parseYearMonth(ym)
        val currentMonthShort = ID_MONTH_NAMES_SHORT.getOrElse(currentYm.monthValue - 1) { "" }

        return if (dueMonthOffset == 1) {
            // Cutoff in currentMonth, Due in nextMonth, e.g. 21 Okt - 1 Nov
            val nextYm = currentYm.plusMonths(1)
            val nextMonthShort = ID_MONTH_NAMES_SHORT.getOrElse(nextYm.monthValue - 1) { "" }
            "($billingCutoffDay $currentMonthShort - $dueDay $nextMonthShort)"
        } else {
            // Cutoff and Due in the same month, e.g. 13 Okt - 22 Okt
            "($billingCutoffDay $currentMonthShort - $dueDay $currentMonthShort)"
        }
    }

    /**
     * Computes exact due date for a given yearMonth
     */
    fun computeDueDate(
        ym: String,
        dueDay: Int,
        dueMonthOffset: Int
    ): LocalDate {
        val currentYm = parseYearMonth(ym)
        val dueYm = if (dueMonthOffset == 1) currentYm.plusMonths(1) else currentYm
        val maxDays = dueYm.lengthOfMonth()
        val clampedDay = dueDay.coerceIn(1, maxDays)
        return dueYm.atDay(clampedDay)
    }

    fun formatDueDate(dueDate: LocalDate): String {
        val monthShort = ID_MONTH_NAMES_SHORT.getOrElse(dueDate.monthValue - 1) { "" }
        return "${dueDate.dayOfMonth} $monthShort ${dueDate.year}"
    }

    fun daysUntil(targetDate: LocalDate): Int {
        return ChronoUnit.DAYS.between(LocalDate.now(), targetDate).toInt()
    }

    fun formatRupiah(amount: Long, withPrefix: Boolean = true): String {
        val symbols = DecimalFormatSymbols(Locale.forLanguageTag("id-ID")).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
        val formatter = DecimalFormat("#,###", symbols)
        val formattedNumber = formatter.format(amount)
        return if (withPrefix) "Rp $formattedNumber" else formattedNumber
    }
}
