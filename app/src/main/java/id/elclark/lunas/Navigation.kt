package id.elclark.lunas

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import id.elclark.lunas.data.LunasRepository
import id.elclark.lunas.ui.main.MainScreen
import id.elclark.lunas.ui.services.ManageServicesScreen
import id.elclark.lunas.ui.settings.SettingsScreen

@Composable
fun MainNavigation(
    initialOpenAddSheet: Boolean = false,
    onAddSheetOpened: () -> Unit = {}
) {
  val backStack = rememberNavBackStack(Main)
  val context = LocalContext.current
  val activity = context as? FragmentActivity
  val repository = LunasRepository.getInstance(context)

  NavDisplay(
    backStack = backStack,
    onBack = {
      if (backStack.size > 1) {
        backStack.removeAt(backStack.size - 1)
      }
    },
    entryProvider =
      entryProvider {
        entry<Main> {
          MainScreen(
            modifier = Modifier.fillMaxSize(),
            initialOpenAddSheet = initialOpenAddSheet,
            onAddSheetOpened = onAddSheetOpened,
            onNavigateToSettings = { backStack.add(Settings) },
            onNavigateToManageServices = { backStack.add(ManageServices) }
          )
        }
        entry<Settings> {
          if (activity != null) {
            SettingsScreen(
              repository = repository,
              activity = activity,
              onNavigateBack = {
                if (backStack.size > 1) {
                  backStack.removeAt(backStack.size - 1)
                }
              },
              onNavigateToManageServices = { backStack.add(ManageServices) }
            )
          }
        }
        entry<ManageServices> {
          ManageServicesScreen(
            repository = repository,
            onNavigateBack = {
              if (backStack.size > 1) {
                backStack.removeAt(backStack.size - 1)
              }
            }
          )
        }
      },
  )
}
