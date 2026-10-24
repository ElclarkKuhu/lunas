package id.elclark.lunas

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import id.elclark.lunas.data.LunasRepository
import id.elclark.lunas.ui.main.MainScreen
import id.elclark.lunas.ui.services.ManageServicesScreen
import id.elclark.lunas.ui.settings.SettingsScreen

import androidx.activity.BackEventCompat
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

private const val TRANSITION_DURATION_MS = 320

@Composable
fun MainNavigation(
    initialOpenAddSheet: Boolean = false,
    onAddSheetOpened: () -> Unit = {},
    initialTargetYm: String? = null,
    onTargetYmHandled: () -> Unit = {}
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
    transitionSpec = {
      val enter = slideInHorizontally(
        initialOffsetX = { fullWidth -> (fullWidth * 0.25f).toInt() },
        animationSpec = tween(durationMillis = TRANSITION_DURATION_MS, easing = FastOutSlowInEasing)
      ) + scaleIn(
        initialScale = 0.96f,
        animationSpec = tween(durationMillis = TRANSITION_DURATION_MS, easing = FastOutSlowInEasing)
      ) + fadeIn(
        animationSpec = tween(durationMillis = 220, easing = LinearOutSlowInEasing)
      )
      val exit = slideOutHorizontally(
        targetOffsetX = { fullWidth -> -(fullWidth * 0.15f).toInt() },
        animationSpec = tween(durationMillis = TRANSITION_DURATION_MS, easing = FastOutSlowInEasing)
      ) + scaleOut(
        targetScale = 0.96f,
        animationSpec = tween(durationMillis = TRANSITION_DURATION_MS, easing = FastOutSlowInEasing)
      ) + fadeOut(
        animationSpec = tween(durationMillis = 180, easing = FastOutLinearInEasing)
      )
      (enter togetherWith exit).apply {
        targetContentZIndex = 1f
      }
    },
    popTransitionSpec = {
      val enter = slideInHorizontally(
        initialOffsetX = { fullWidth -> -(fullWidth * 0.15f).toInt() },
        animationSpec = tween(durationMillis = TRANSITION_DURATION_MS, easing = FastOutSlowInEasing)
      ) + scaleIn(
        initialScale = 0.96f,
        animationSpec = tween(durationMillis = TRANSITION_DURATION_MS, easing = FastOutSlowInEasing)
      ) + fadeIn(
        animationSpec = tween(durationMillis = 220, easing = LinearOutSlowInEasing)
      )
      val exit = slideOutHorizontally(
        targetOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(durationMillis = TRANSITION_DURATION_MS, easing = FastOutSlowInEasing)
      ) + fadeOut(
        animationSpec = tween(durationMillis = 200, easing = FastOutLinearInEasing)
      )
      (enter togetherWith exit).apply {
        targetContentZIndex = -1f
      }
    },
    predictivePopTransitionSpec = { edge ->
      val isFromRight = edge == BackEventCompat.EDGE_RIGHT
      val enter = slideInHorizontally(
        initialOffsetX = { fullWidth -> if (isFromRight) (fullWidth * 0.10f).toInt() else -(fullWidth * 0.10f).toInt() },
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
      ) + scaleIn(
        initialScale = 0.94f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
      ) + fadeIn(
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
      )
      val exit = slideOutHorizontally(
        targetOffsetX = { fullWidth -> if (isFromRight) -fullWidth else fullWidth },
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
      ) + scaleOut(
        targetScale = 0.90f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
      ) + fadeOut(
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
      )
      (enter togetherWith exit).apply {
        targetContentZIndex = -1f
      }
    },
    entryProvider =
      entryProvider {
        entry<Main> {
          MainScreen(
            modifier = Modifier.fillMaxSize(),
            initialOpenAddSheet = initialOpenAddSheet,
            onAddSheetOpened = onAddSheetOpened,
            initialTargetYm = initialTargetYm,
            onTargetYmHandled = onTargetYmHandled,
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
