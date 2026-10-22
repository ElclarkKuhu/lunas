package id.elclark.lunas.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import id.elclark.lunas.data.LunasDatabaseHelper

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        id.elclark.lunas.widget.LunasWidgetProvider.updateAllWidgets(context)

        val dbHelper = LunasDatabaseHelper.getInstance(context)
        val settings = dbHelper.getSettings()

        if (settings.notificationsEnabled) {
            NotificationHelper.checkAndNotifyBills(context)
            // Reschedule next alarm for the following day
            NotificationHelper.scheduleDailyAlarm(context, settings.reminderHour, settings.reminderMinute)
        }
    }
}
