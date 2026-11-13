package com.ahara.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ahara.app.ui.components.LocalLang
import com.ahara.app.ui.screens.AddMealScreen
import com.ahara.app.ui.screens.AgniScreen
import com.ahara.app.ui.screens.AnalyticsScreen
import com.ahara.app.ui.screens.DashboardScreen
import com.ahara.app.ui.screens.DiaryScreen
import com.ahara.app.ui.screens.EnergyScreen
import com.ahara.app.ui.screens.IntervalScreen
import com.ahara.app.ui.screens.LanguageScreen
import com.ahara.app.ui.screens.MoreScreen
import com.ahara.app.ui.screens.OnboardingScreen
import com.ahara.app.ui.screens.PostMealScreen
import com.ahara.app.ui.screens.PrakritiScreen
import com.ahara.app.ui.screens.ProfileScreen
import com.ahara.app.ui.screens.RemindersScreen
import com.ahara.app.ui.screens.ReportsScreen
import com.ahara.app.ui.screens.ResearchScreen
import com.ahara.app.ui.screens.SettingsScreen
import com.ahara.app.ui.screens.ShlokaScreen
import com.ahara.app.ui.screens.SplashScreen
import com.ahara.app.ui.theme.AharaTheme

object Routes {
    const val SPLASH = "splash"
    const val LANGUAGE = "language"
    const val ONBOARDING = "onboarding"
    const val PROFILE = "profile"
    const val HOME = "home"
    const val DIARY = "diary"
    const val ADD_MEAL = "addMeal"
    const val INTERVAL = "interval"
    const val POST_MEAL = "postMeal"
    const val ENERGY = "energy"
    const val PRAKRITI = "prakriti"
    const val AGNI = "agni"
    const val SHLOKA = "shloka"
    const val ANALYTICS = "analytics"
    const val RESEARCH = "research"
    const val REPORTS = "reports"
    const val REMINDERS = "reminders"
    const val SETTINGS = "settings"
    const val MORE = "more"

    fun addMeal(id: Long = 0L) = "$ADD_MEAL/$id"
    fun analytics(period: String) = "$ANALYTICS/$period"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: AppViewModel = viewModel()
            val settings by vm.settings.collectAsState()
            CompositionLocalProvider(LocalLang provides (settings?.language ?: "en")) {
                AharaTheme {
                    val nav = rememberNavController()
                    AharaNavHost(nav, vm)
                }
            }
        }
    }
}

/** Navigate to a top-level tab without stacking duplicates. */
fun NavHostController.goTab(route: String) {
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun AharaNavHost(nav: NavHostController, vm: AppViewModel) {
    val back: () -> Unit = { nav.popBackStack() }
    NavHost(navController = nav, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) { SplashScreen(vm, nav) }
        composable(Routes.LANGUAGE) { LanguageScreen(vm, nav) }
        composable(Routes.ONBOARDING) { OnboardingScreen(vm, nav) }
        composable(
            "${Routes.PROFILE}?setup={setup}",
            arguments = listOf(navArgument("setup") { type = NavType.BoolType; defaultValue = false })
        ) { entry ->
            ProfileScreen(vm, nav, setup = entry.arguments?.getBoolean("setup") ?: false)
        }
        composable(Routes.HOME) { DashboardScreen(vm, nav) }
        composable(Routes.DIARY) { DiaryScreen(vm, nav) }
        composable(
            "${Routes.ADD_MEAL}/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            AddMealScreen(vm, id = entry.arguments?.getLong("id") ?: 0L, onDone = back)
        }
        composable(Routes.INTERVAL) { IntervalScreen(vm, back) }
        composable(Routes.POST_MEAL) { PostMealScreen(vm, nav, back) }
        composable(Routes.ENERGY) { EnergyScreen(vm, back) }
        composable(Routes.PRAKRITI) { PrakritiScreen(vm, back) }
        composable(Routes.AGNI) { AgniScreen(vm, back) }
        composable(Routes.SHLOKA) { ShlokaScreen(vm, back) }
        composable(
            "${Routes.ANALYTICS}/{period}",
            arguments = listOf(navArgument("period") { type = NavType.StringType })
        ) { entry ->
            AnalyticsScreen(vm, nav, period = entry.arguments?.getString("period") ?: "weekly")
        }
        composable(Routes.RESEARCH) { ResearchScreen(vm, back) }
        composable(Routes.REPORTS) { ReportsScreen(vm, back) }
        composable(Routes.REMINDERS) { RemindersScreen(vm, back) }
        composable(Routes.SETTINGS) { SettingsScreen(vm, nav, back) }
        composable(Routes.MORE) { MoreScreen(vm, nav) }
    }
}
