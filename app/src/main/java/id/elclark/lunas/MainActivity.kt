package id.elclark.lunas

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import id.elclark.lunas.notification.NotificationHelper
import id.elclark.lunas.theme.LunasTheme
import id.elclark.lunas.widget.LunasWidgetProvider

class MainActivity : FragmentActivity() {

  private val openAddBillState = mutableStateOf(false)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    NotificationHelper.createNotificationChannel(this)
    NotificationHelper.scheduleDailyAlarm(this)

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
