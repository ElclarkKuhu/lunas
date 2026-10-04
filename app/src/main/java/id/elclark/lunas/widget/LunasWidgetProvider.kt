package id.elclark.lunas.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import id.elclark.lunas.MainActivity
import id.elclark.lunas.R
import id.elclark.lunas.data.LunasDatabaseHelper
import id.elclark.lunas.data.LunasRepository
import id.elclark.lunas.util.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LunasWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET) {
            updateAllWidgets(context)
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "id.elclark.lunas.widget.ACTION_REFRESH"
        const val EXTRA_OPEN_ADD_BILL = "extra_open_add_bill"
        const val ACTION_ADD_BILL = "id.elclark.lunas.ACTION_ADD_BILL"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val componentName = ComponentName(context, LunasWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                updateWidgets(context, appWidgetManager, appWidgetIds)
            }
        }

        private fun updateWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
            CoroutineScope(Dispatchers.IO).launch {
                val dbHelper = LunasDatabaseHelper(context)
                val services = dbHelper.getAllServices()
                val items = dbHelper.getAllItems()
                val payments = dbHelper.getAllPaymentStatuses()

                val repo = LunasRepository.getInstance(context)
                val currentYm = DateUtils.currentYearMonth()
                val overview = repo.computeMonthlyOverview(currentYm, services, items, payments)

                for (widgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_lunas_overview)

                    // Month text
                    views.setTextViewText(R.id.widget_month_text, DateUtils.formatYearMonthFull(currentYm))

                    // Total due amount
                    views.setTextViewText(R.id.widget_total_amount, DateUtils.formatRupiah(overview.totalDue))

                    // Status Badge & Progress
                    if (overview.totalDue == 0L) {
                        views.setViewVisibility(R.id.widget_status_badge_neutral, View.VISIBLE)
                        views.setViewVisibility(R.id.widget_status_badge_paid, View.GONE)
                        views.setViewVisibility(R.id.widget_status_badge_due, View.GONE)
                        views.setTextViewText(R.id.widget_status_badge_neutral, "Tidak Ada Tagihan")
                        views.setProgressBar(R.id.widget_progress_bar, 100, 0, false)
                    } else if (overview.paidCount == overview.totalCount && overview.totalCount > 0) {
                        views.setViewVisibility(R.id.widget_status_badge_paid, View.VISIBLE)
                        views.setViewVisibility(R.id.widget_status_badge_neutral, View.GONE)
                        views.setViewVisibility(R.id.widget_status_badge_due, View.GONE)
                        views.setTextViewText(R.id.widget_status_badge_paid, "Semua Lunas (${overview.paidCount}/${overview.totalCount})")
                        views.setProgressBar(R.id.widget_progress_bar, 100, 100, false)
                    } else {
                        val progress = if (overview.totalDue > 0) {
                            ((overview.totalPaid.toDouble() / overview.totalDue) * 100).toInt().coerceIn(0, 100)
                        } else 0
                        views.setViewVisibility(R.id.widget_status_badge_due, View.VISIBLE)
                        views.setViewVisibility(R.id.widget_status_badge_paid, View.GONE)
                        views.setViewVisibility(R.id.widget_status_badge_neutral, View.GONE)
                        views.setTextViewText(R.id.widget_status_badge_due, "${overview.paidCount}/${overview.totalCount} Lunas")
                        views.setProgressBar(R.id.widget_progress_bar, 100, progress, false)
                    }

                    // Upcoming Due Bill
                    val unpaidStatements = overview.statements
                        .filter { !it.isPaid && it.subtotal > 0 }
                        .sortedBy { it.daysUntilDue }

                    val nextBill = unpaidStatements.firstOrNull()

                    if (nextBill != null) {
                        val dueDesc = when {
                            nextBill.daysUntilDue < 0 -> "Telat ${-nextBill.daysUntilDue} hari (${nextBill.dueDateText})"
                            nextBill.daysUntilDue == 0 -> "Jatuh Tempo HARI INI (${nextBill.dueDateText})"
                            nextBill.daysUntilDue == 1 -> "Jatuh Tempo BESOK (${nextBill.dueDateText})"
                            else -> "Jatuh Tempo ${nextBill.dueDateText} (${nextBill.daysUntilDue} hari lagi)"
                        }
                        views.setTextViewText(R.id.widget_upcoming_service_and_due, "${nextBill.service.name} • $dueDesc")

                        val subtext = when {
                            nextBill.isOverdue -> "Lewat jatuh tempo! Segera bayar"
                            nextBill.isDueSoon -> "Jatuh tempo segera!"
                            else -> "Belum dibayar bulan ini"
                        }
                        views.setTextViewText(R.id.widget_upcoming_subtext, subtext)
                        views.setTextViewText(R.id.widget_upcoming_amount, DateUtils.formatRupiah(nextBill.subtotal))
                        views.setViewVisibility(R.id.widget_upcoming_amount, View.VISIBLE)
                    } else {
                        if (overview.totalDue > 0) {
                            views.setTextViewText(R.id.widget_upcoming_service_and_due, "Semua tagihan bulan ini beres! ")
                            views.setTextViewText(R.id.widget_upcoming_subtext, "Tidak ada pembayaran tertunda")
                        } else {
                            views.setTextViewText(R.id.widget_upcoming_service_and_due, "Belum ada tagihan aktif")
                            views.setTextViewText(R.id.widget_upcoming_subtext, "Tekan + untuk mencatat cicilan")
                        }
                        views.setViewVisibility(R.id.widget_upcoming_amount, View.GONE)
                    }

                    // 1. Root tap -> Open MainActivity
                    val mainIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val mainPendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        mainIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_root, mainPendingIntent)

                    // 2. Add Button tap -> Open MainActivity with EXTRA_OPEN_ADD_BILL
                    val addIntent = Intent(context, MainActivity::class.java).apply {
                        action = ACTION_ADD_BILL
                        putExtra(EXTRA_OPEN_ADD_BILL, true)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val addPendingIntent = PendingIntent.getActivity(
                        context,
                        1,
                        addIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_btn_add, addPendingIntent)

                    // 3. Refresh Button tap -> Broadcast ACTION_REFRESH_WIDGET
                    val refreshIntent = Intent(context, LunasWidgetProvider::class.java).apply {
                        action = ACTION_REFRESH_WIDGET
                    }
                    val refreshPendingIntent = PendingIntent.getBroadcast(
                        context,
                        2,
                        refreshIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_btn_refresh, refreshPendingIntent)

                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            }
        }
    }
}
