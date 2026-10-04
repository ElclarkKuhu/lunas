package id.elclark.lunas.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import id.elclark.lunas.data.LunasDatabaseHelper

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val dbHelper = LunasDatabaseHelper(context)
            val settings = dbHelper.getSettings()
            if (settings.notificationsEnabled) {
                NotificationHelper.scheduleDailyAlarm(context, settings.reminderHour)
            }
            id.elclark.lunas.widget.LunasWidgetProvider.updateAllWidgets(context)
        }
    }
}
