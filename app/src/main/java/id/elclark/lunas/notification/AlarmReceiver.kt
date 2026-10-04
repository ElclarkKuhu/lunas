package id.elclark.lunas.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import id.elclark.lunas.data.LunasDatabaseHelper
import id.elclark.lunas.util.DateUtils
import java.time.LocalDate

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        id.elclark.lunas.widget.LunasWidgetProvider.updateAllWidgets(context)

        val dbHelper = LunasDatabaseHelper(context)
        val settings = dbHelper.getSettings()

        if (!settings.notificationsEnabled) return

        val services = dbHelper.getAllServices().filter { it.isActive }
        val items = dbHelper.getAllItems()
        val payments = dbHelper.getAllPaymentStatuses()

        val currentYm = DateUtils.currentYearMonth()
        val today = LocalDate.now()
        val todayDay = today.dayOfMonth

        var notificationId = 2000

        for (svc in services) {
            // Check if service is paid this month
            val isPaid = payments.find { it.serviceId == svc.id && it.yearMonth == currentYm }?.isPaid ?: false
            if (isPaid) continue

            // Compute subtotal for current month
            var subtotal = 0L
            for (item in items) {
                if (item.serviceId != svc.id) continue
                val diff = DateUtils.monthDifference(item.startYearMonth, currentYm)
                if (diff in 0 until item.totalTenor) {
                    subtotal += item.amountPerMonth
                }
            }

            if (subtotal <= 0) continue

            val dueDate = DateUtils.computeDueDate(currentYm, svc.dueDay, svc.dueMonthOffset)
            val daysUntil = DateUtils.daysUntil(dueDate)
            val formattedAmount = DateUtils.formatRupiah(subtotal)

            // 1. On Cutoff day
            if (todayDay == svc.billingCutoffDay) {
                NotificationHelper.showBillNotification(
                    context,
                    notificationId++,
                    "Tagihan ${svc.name} siap dicek",
                    "Tagihan periode ini: $formattedAmount. Jatuh tempo pada ${DateUtils.formatDueDate(dueDate)}."
                )
            }
            // 2. H-reminderDaysBefore
            else if (daysUntil == settings.reminderDaysBefore) {
                NotificationHelper.showBillNotification(
                    context,
                    notificationId++,
                    "${svc.name} jatuh tempo dalam $daysUntil hari",
                    "Total tagihan belum lunas: $formattedAmount. Jatuh tempo: ${DateUtils.formatDueDate(dueDate)}."
                )
            }
            // 3. Due day
            else if (daysUntil == 0) {
                NotificationHelper.showBillNotification(
                    context,
                    notificationId++,
                    "Batas pembayaran ${svc.name} hari ini",
                    "Jangan sampai terlambat! Segera bayar tagihan $formattedAmount hari ini."
                )
            }
        }
    }
}
