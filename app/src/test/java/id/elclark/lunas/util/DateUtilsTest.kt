package id.elclark.lunas.util

import org.junit.Assert.assertEquals
import org.junit.Test

class DateUtilsTest {

    @Test
    fun formatCurrencyDefaultsToRupiah() {
        assertEquals("Rp 1.234.567", DateUtils.formatCurrency(1_234_567))
        assertEquals("Rp 1.234.567", DateUtils.formatRupiah(1_234_567))
    }

    @Test
    fun formatCurrencyChangesSymbolWithoutConvertingAmount() {
        assertEquals("$ 1,234,567", DateUtils.formatCurrency(1_234_567, "USD"))
    }

    @Test
    fun supportedCurrenciesPutsRupiahFirst() {
        assertEquals("IDR", DateUtils.supportedCurrencies.first().code)
    }

    @Test
    fun computeDueDateCalculatesSameMonthAndNextMonthOffset() {
        // Offset 0: Cutoff and due date in the same month (e.g. Yup: cutoff 13, due 22)
        val sameMonthDue = DateUtils.computeDueDate("2026-10", 22, 0)
        assertEquals(2026, sameMonthDue.year)
        assertEquals(10, sameMonthDue.monthValue)
        assertEquals(22, sameMonthDue.dayOfMonth)

        // Offset 1: Cutoff in current month, due date in next month (e.g. SPayLater: cutoff 21, due 1)
        val nextMonthDue = DateUtils.computeDueDate("2026-10", 1, 1)
        assertEquals(2026, nextMonthDue.year)
        assertEquals(11, nextMonthDue.monthValue)
        assertEquals(1, nextMonthDue.dayOfMonth)
    }

    @Test
    fun computeBillingCycleTextFormatsCyclesProperly() {
        val sameMonthText = DateUtils.computeBillingCycleText("2026-10", 13, 22, 0)
        assertEquals("(13 Okt - 22 Okt)", sameMonthText)

        val nextMonthText = DateUtils.computeBillingCycleText("2026-10", 21, 1, 1)
        assertEquals("(21 Okt - 1 Nov)", nextMonthText)
    }

    @Test
    fun formatYearMonthDisplaySupportsLocales() {
        val idLocale = java.util.Locale.forLanguageTag("id-ID")
        val enLocale = java.util.Locale.forLanguageTag("en-US")
        assertEquals("Oktober 2026", DateUtils.formatYearMonthDisplay("2026-10", idLocale))
        assertEquals("October 2026", DateUtils.formatYearMonthDisplay("2026-10", enLocale))
    }

    @Test
    fun formatDueDateShortFormatsCleanly() {
        val currentYear = java.time.LocalDate.now().year
        val dateThisYear = java.time.LocalDate.of(currentYear, 11, 1)
        val idLocale = java.util.Locale.forLanguageTag("id-ID")
        val enLocale = java.util.Locale.forLanguageTag("en-US")
        assertEquals("1 Nov", DateUtils.formatDueDateShort(dateThisYear, idLocale))
        assertEquals("1 Nov", DateUtils.formatDueDateShort(dateThisYear, enLocale))

        val dateNextYear = java.time.LocalDate.of(currentYear + 1, 1, 15)
        assertEquals("15 Jan " + (currentYear + 1), DateUtils.formatDueDateShort(dateNextYear, idLocale))
    }
}
