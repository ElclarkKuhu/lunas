package id.elclark.lunas

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import id.elclark.lunas.data.LunasDatabaseHelper
import id.elclark.lunas.notification.NotificationHelper
import id.elclark.lunas.theme.LunasTheme
import id.elclark.lunas.widget.LunasWidgetProvider

class MainActivity : FragmentActivity() {

  private val openAddBillState = mutableStateOf(false)

  private val requestNotificationPermissionLauncher =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
      if (isGranted) {
        val dbHelper = LunasDatabaseHelper(this)
        val settings = dbHelper.getSettings()
        if (settings.notificationsEnabled) {
          NotificationHelper.scheduleDailyAlarm(this, settings.reminderHour, settings.reminderMinute)
        }
      }
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    NotificationHelper.createNotificationChannel(this)

    val dbHelper = LunasDatabaseHelper(this)
    val settings = dbHelper.getSettings()
    if (settings.notificationsEnabled) {
      NotificationHelper.scheduleDailyAlarm(this, settings.reminderHour, settings.reminderMinute)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
          requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
      }
    }

    checkAddBillIntent(intent)

    enableEdgeToEdge()
    setContent {
      LunasTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          MainNavigation(
            initialOpenAddSheet = openAddBillState.value,
            onAddSheetOpened = { openAddBillState.value = false }
          )
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    checkAddBillIntent(intent)
  }

  private fun checkAddBillIntent(intent: Intent?) {
    if (intent?.action == LunasWidgetProvider.ACTION_ADD_BILL ||
        intent?.getBooleanExtra(LunasWidgetProvider.EXTRA_OPEN_ADD_BILL, false) == true) {
      openAddBillState.value = true
    }
  }
}
