package id.elclark.lunas.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import id.elclark.lunas.data.LunasDatabaseHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                id.elclark.lunas.widget.LunasWidgetProvider.updateAllWidgets(context)

                val dbHelper = LunasDatabaseHelper.getInstance(context)
                val settings = dbHelper.getSettings()

                if (settings.notificationsEnabled) {
                    NotificationHelper.checkAndNotifyBills(context)
                    // Reschedule next alarm for the following day
                    NotificationHelper.scheduleDailyAlarm(context, settings.reminderHour, settings.reminderMinute)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
