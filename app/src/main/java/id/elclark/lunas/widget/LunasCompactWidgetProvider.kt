package id.elclark.lunas.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
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

class LunasCompactWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        updateWidgets(context, appWidgetManager, appWidgetIds, pendingResult)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == LunasWidgetProvider.ACTION_REFRESH_WIDGET) {
            val pendingResult = goAsync()
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, LunasCompactWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager?.getAppWidgetIds(componentName)
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                updateWidgets(context, appWidgetManager, appWidgetIds, pendingResult)
            } else {
                pendingResult?.finish()
            }
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val componentName = ComponentName(context, LunasCompactWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                updateWidgets(context, appWidgetManager, appWidgetIds)
            }
        }

        private fun updateWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray,
            pendingResult: BroadcastReceiver.PendingResult? = null
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val dbHelper = LunasDatabaseHelper.getInstance(context)
                    val services = dbHelper.getAllServices()
                    val items = dbHelper.getAllItems()
                    val payments = dbHelper.getAllPaymentStatuses()
                    val settings = dbHelper.getSettings()
                    val currencyCode = settings.currencyCode

                    val currentYm = DateUtils.currentYearMonth()
                    val overview = LunasRepository.computeMonthlyOverview(
                        targetYm = currentYm,
                        allServices = services,
                        allItems = items,
                        allPayments = payments,
                        serviceOrder = settings.serviceOrder
                    )

                    for (widgetId in appWidgetIds) {
                        val views = RemoteViews(context.packageName, R.layout.widget_lunas_compact)

                        // Month text
                        views.setTextViewText(R.id.widget_compact_month_text, DateUtils.formatYearMonthDisplay(currentYm))

                        // Total due amount
                        views.setTextViewText(R.id.widget_compact_total_amount, DateUtils.formatCurrency(overview.totalDue, currencyCode))

                        // Status Badge
                        val hasOverdue = overview.statements.any { !it.isPaid && it.subtotal > 0 && it.daysUntilDue < 0 }

                        views.setViewVisibility(R.id.widget_compact_status_badge_neutral, View.GONE)
                        views.setViewVisibility(R.id.widget_compact_status_badge_paid, View.GONE)
                        views.setViewVisibility(R.id.widget_compact_status_badge_due, View.GONE)
                        views.setViewVisibility(R.id.widget_compact_status_badge_overdue, View.GONE)

                        if (overview.totalDue == 0L) {
                            views.setViewVisibility(R.id.widget_compact_status_badge_neutral, View.VISIBLE)
                            views.setTextViewText(R.id.widget_compact_status_badge_neutral, context.getString(R.string.widget_compact_status_neutral))
                        } else if (overview.paidCount == overview.totalCount && overview.totalCount > 0) {
                            views.setViewVisibility(R.id.widget_compact_status_badge_paid, View.VISIBLE)
                            views.setTextViewText(R.id.widget_compact_status_badge_paid, context.getString(R.string.widget_compact_status_paid))
                        } else if (hasOverdue) {
                            views.setViewVisibility(R.id.widget_compact_status_badge_overdue, View.VISIBLE)
                            views.setTextViewText(R.id.widget_compact_status_badge_overdue, context.getString(R.string.widget_badge_overdue))
                        } else {
                            views.setViewVisibility(R.id.widget_compact_status_badge_due, View.VISIBLE)
                            views.setTextViewText(R.id.widget_compact_status_badge_due, context.getString(R.string.widget_due_status, overview.paidCount, overview.totalCount))
                        }

                        // Upcoming Due Bill
                        val unpaidStatements = overview.statements
                            .filter { !it.isPaid && it.subtotal > 0 }
                            .sortedBy { it.daysUntilDue }

                        val nextBill = unpaidStatements.firstOrNull()

                        if (nextBill != null) {
                            views.setTextViewText(R.id.widget_compact_upcoming_service, nextBill.service.name)
                            views.setTextViewText(R.id.widget_compact_upcoming_amount, DateUtils.formatCurrency(nextBill.subtotal, currencyCode))
                            views.setViewVisibility(R.id.widget_compact_upcoming_amount, View.VISIBLE)

                            val dueFormatted = nextBill.dueDate?.let { DateUtils.formatDueDateShort(it) } ?: nextBill.dueDateText
                            val (dueDesc, textColorRes, iconRes) = when {
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

                            views.setTextViewText(R.id.widget_compact_upcoming_subtext, dueDesc)
                            views.setTextColor(R.id.widget_compact_upcoming_subtext, ContextCompat.getColor(context, textColorRes))
                            views.setImageViewResource(R.id.widget_compact_upcoming_icon, iconRes)
                        } else {
                            if (overview.totalDue > 0) {
                                views.setTextViewText(R.id.widget_compact_upcoming_service, context.getString(R.string.widget_compact_all_settled))
                                views.setTextViewText(R.id.widget_compact_upcoming_subtext, context.getString(R.string.widget_compact_all_settled_hint))
                                views.setTextColor(R.id.widget_compact_upcoming_subtext, ContextCompat.getColor(context, R.color.dark_text_secondary))
                                views.setImageViewResource(R.id.widget_compact_upcoming_icon, R.drawable.ic_widget_check)
                            } else {
                                views.setTextViewText(R.id.widget_compact_upcoming_service, context.getString(R.string.widget_compact_no_bills))
                                views.setTextViewText(R.id.widget_compact_upcoming_subtext, context.getString(R.string.widget_compact_add_bill_hint))
                                views.setTextColor(R.id.widget_compact_upcoming_subtext, ContextCompat.getColor(context, R.color.dark_text_secondary))
                                views.setImageViewResource(R.id.widget_compact_upcoming_icon, R.drawable.ic_widget_calendar)
                            }
                            views.setViewVisibility(R.id.widget_compact_upcoming_amount, View.GONE)
                        }

                        // Root tap -> Open MainActivity
                        val mainIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        val mainPendingIntent = PendingIntent.getActivity(
                            context,
                            10,
                            mainIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_compact_root, mainPendingIntent)

                        // Add Button tap -> Open MainActivity with EXTRA_OPEN_ADD_BILL
                        val addIntent = Intent(context, MainActivity::class.java).apply {
                            action = LunasWidgetProvider.ACTION_ADD_BILL
                            putExtra(LunasWidgetProvider.EXTRA_OPEN_ADD_BILL, true)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        val addPendingIntent = PendingIntent.getActivity(
                            context,
                            11,
                            addIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_compact_btn_add, addPendingIntent)

                        appWidgetManager.updateAppWidget(widgetId, views)
                    }
                } finally {
                    pendingResult?.finish()
                }
            }
        }
    }
}
