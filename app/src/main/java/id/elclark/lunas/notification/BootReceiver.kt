package id.elclark.lunas.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import id.elclark.lunas.data.LunasDatabaseHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val dbHelper = LunasDatabaseHelper.getInstance(context)
                    val settings = dbHelper.getSettings()
                    if (settings.notificationsEnabled) {
                        NotificationHelper.scheduleDailyAlarm(context, settings.reminderHour, settings.reminderMinute)
                    }
                    id.elclark.lunas.widget.LunasWidgetProvider.updateAllWidgets(context)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
