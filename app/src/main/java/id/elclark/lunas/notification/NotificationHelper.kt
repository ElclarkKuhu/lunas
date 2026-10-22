package id.elclark.lunas.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import id.elclark.lunas.MainActivity
import id.elclark.lunas.R
import id.elclark.lunas.data.LunasDatabaseHelper
import id.elclark.lunas.util.DateUtils
import java.time.LocalDate
import java.util.Calendar

object NotificationHelper {

    const val CHANNEL_ID = "lunas_bill_reminders"
    private const val CHANNEL_NAME = "Pengingat Tagihan Lunas"
    private const val ALARM_REQUEST_CODE = 1001
    const val TEST_NOTIFICATION_ID = 1099
    const val EXTRA_TARGET_YM = "extra_target_ym"
    const val EXTRA_SERVICE_ID = "extra_service_id"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi jatuh tempo dan cetak tagihan paylater"
                enableLights(true)
                enableVibration(true)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    fun scheduleDailyAlarm(context: Context, hourOfDay: Int = 9, minute: Int = 0) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } catch (_: Exception) {
                // Ignore fallback failure
            }
        }
    }

    fun cancelDailyAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun showBillNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        targetYm: String? = null,
        serviceId: String? = null
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (targetYm != null) putExtra(EXTRA_TARGET_YM, targetYm)
            if (serviceId != null) putExtra(EXTRA_SERVICE_ID, serviceId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_lunas)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Notification permission might not be granted
        }
    }

    fun showTestNotification(context: Context): Boolean {
        createNotificationChannel(context)
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return false
        }
        showBillNotification(
            context = context,
            notificationId = TEST_NOTIFICATION_ID,
            title = context.getString(R.string.notification_test_title),
            message = context.getString(R.string.notification_test_message)
        )
        return true
    }

    fun checkAndNotifyBills(context: Context) {
        val dbHelper = LunasDatabaseHelper.getInstance(context)
        val settings = dbHelper.getSettings()

        if (!settings.notificationsEnabled) return

        val services = dbHelper.getAllServices().filter { it.isActive }
        val items = dbHelper.getAllItems()
        val payments = dbHelper.getAllPaymentStatuses()

        val currentYm = DateUtils.currentYearMonth()
        val prevYm = DateUtils.plusMonths(currentYm, -1)
        val today = LocalDate.now()
        val todayDay = today.dayOfMonth

        for (svc in services) {
            // Check candidate billing statements: previous month (e.g. For offset=1 bills due this month) and current month
            val candidateYms = listOf(prevYm, currentYm)

            for (ym in candidateYms) {
                // Check if this statement is already paid
                val isPaid = payments.find { it.serviceId == svc.id && it.yearMonth == ym }?.isPaid ?: false
                if (isPaid) continue

                // Compute subtotal for this statement cycle
                var subtotal = 0L
                for (item in items) {
                    if (item.serviceId != svc.id) continue
                    val diff = DateUtils.monthDifference(item.startYearMonth, ym)
                    if (diff in 0 until item.totalTenor) {
                        subtotal += item.amountPerMonth
                    }
                }

                if (subtotal <= 0) continue

                val dueDate = DateUtils.computeDueDate(ym, svc.dueDay, svc.dueMonthOffset)
                val daysUntil = DateUtils.daysUntil(dueDate)
                val formattedAmount = DateUtils.formatCurrency(subtotal, settings.currencyCode)
                val dueDateFormatted = DateUtils.formatDueDate(dueDate)

                // 1. Cutoff notification: on cutoff (printing) day of the current month statement
                val cutoffDayClamped = svc.billingCutoffDay.coerceIn(1, today.lengthOfMonth())
                if (settings.notifyOnCutoff && ym == currentYm && todayDay == cutoffDayClamped) {
                    val notifId = (svc.id.hashCode() * 31 + ym.hashCode() * 7 + 1).and(0x7FFFFFFF)
                    showBillNotification(
                        context,
                        notifId,
                        context.getString(R.string.notification_cutoff_title, svc.name),
                        context.getString(R.string.notification_cutoff_message, formattedAmount, dueDateFormatted),
                        targetYm = ym,
                        serviceId = svc.id
                    )
                }

                // 2. Due Date Notifications (Deterministic ID for due events of this statement)
                val dueNotifId = (svc.id.hashCode() * 31 + ym.hashCode() * 7 + 2).and(0x7FFFFFFF)

                // Today is Due day
                if (settings.notifyOnDueDate && daysUntil == 0) {
                    showBillNotification(
                        context,
                        dueNotifId,
                        context.getString(R.string.notification_due_today_title, svc.name),
                        context.getString(R.string.notification_due_today_message, formattedAmount),
                        targetYm = ym,
                        serviceId = svc.id
                    )
                }
                // Approaching due date: H-reminderDaysBefore
                else if (settings.notifyDueSoon && daysUntil == settings.reminderDaysBefore) {
                    showBillNotification(
                        context,
                        dueNotifId,
                        context.getString(R.string.notification_due_soon_title, svc.name, daysUntil),
                        context.getString(R.string.notification_due_soon_message, formattedAmount, dueDateFormatted),
                        targetYm = ym,
                        serviceId = svc.id
                    )
                }
                // Overdue: 1 day past due date
                else if (settings.notifyOnOverdue && daysUntil == -1) {
                    showBillNotification(
                        context,
                        dueNotifId,
                        context.getString(R.string.notification_overdue_title, svc.name),
                        context.getString(R.string.notification_overdue_message, formattedAmount, dueDateFormatted),
                        targetYm = ym,
                        serviceId = svc.id
                    )
                }
            }
        }
    }
}
