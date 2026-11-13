package com.ahara.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ahara.app.AppViewModel
import com.ahara.app.Routes
import com.ahara.app.data.MealEntry
import com.ahara.app.engine.ObservationEngine
import com.ahara.app.goTab
import com.ahara.app.ui.components.AharaScaffold
import com.ahara.app.ui.components.Botanical
import com.ahara.app.ui.components.LineChart
import com.ahara.app.ui.components.Legend
import com.ahara.app.ui.components.MenuRow
import com.ahara.app.ui.components.ProportionBar
import com.ahara.app.ui.components.SectionCard
import com.ahara.app.ui.components.StatTile
import com.ahara.app.ui.components.t
import com.ahara.app.ui.components.tf
import com.ahara.app.ui.theme.AharaColors
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

val ChartIcon: ImageVector = materialIcon(name = "Ahara.Chart") {
    materialPath {
        moveTo(5f, 9.2f); horizontalLineTo(8f); verticalLineTo(19f); horizontalLineTo(5f); close()
        moveTo(10.6f, 5f); horizontalLineTo(13.4f); verticalLineTo(19f); horizontalLineTo(10.6f); close()
        moveTo(16.2f, 13f); horizontalLineTo(19f); verticalLineTo(19f); horizontalLineTo(16.2f); close()
    }
}

val timeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM")

fun hm(hours: Double): String {
    val total = (hours * 60).toLong()
    return "${total / 60}h ${total % 60}m"
}

@Composable
fun MainBottomBar(nav: NavHostController) {
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: ""
    NavigationBar(containerColor = Color.White) {
        val items = listOf(
            Triple(Routes.HOME, Icons.Filled.Home, t("nav_home")),
            Triple(Routes.DIARY, Icons.Filled.DateRange, t("nav_diary")),
            Triple(Routes.analytics("weekly"), ChartIcon, t("nav_analytics")),
            Triple(Routes.MORE, Icons.Filled.Menu, t("nav_more"))
        )
        items.forEach { (r, icon, label) ->
            val selected = route == r || (r.startsWith(Routes.ANALYTICS) && route.startsWith(Routes.ANALYTICS))
            NavigationBarItem(
                selected = selected,
                onClick = { nav.goTab(r) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(indicatorColor = AharaColors.SageLight)
            )
        }
    }
}

@Composable
fun AddMealFab(nav: NavHostController) {
    ExtendedFloatingActionButton(
        onClick = { nav.navigate(Routes.addMeal()) },
        containerColor = AharaColors.SageDark,
        contentColor = Color.White,
        icon = { Icon(Icons.Filled.Add, null) },
        text = { Text(t("add_meal")) }
    )
}

@Composable
fun DashboardScreen(vm: AppViewModel, nav: NavHostController) {
    val meals by vm.meals.collectAsState()
    val settings by vm.settings.collectAsState()
    val profile by vm.profile.collectAsState()
    val dailyLogs by vm.dailyLogs.collectAsState()
    val energyLogs by vm.energyLogs.collectAsState()
    val s = settings ?: return
    val zone = vm.zone

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { delay(30_000); now = System.currentTimeMillis() }
    }

    val today = LocalDate.now(zone)
    val dayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
    val dayEnd = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val weekStart = today.minusDays(6).atStartOfDay(zone).toInstant().toEpochMilli()

    val todayStats = remember(meals, s) { ObservationEngine.stats(meals, dayStart, dayEnd, s.yamaHours, s.includeOvernight, zone) }
    val weekStats = remember(meals, s) { ObservationEngine.stats(meals, weekStart, dayEnd, s.yamaHours, s.includeOvernight, zone) }
    val series = remember(meals, s) { ObservationEngine.daySeries(meals, today, 7, s.yamaHours, s.includeOvernight, zone) }
    val lastMeal: MealEntry? = meals.filter { !it.skipped && it.timestamp <= now }.maxByOrNull { it.timestamp }
    val todayLog = dailyLogs.firstOrNull { it.date == today.toString() }
    val todayEnergy = energyLogs.filter { it.timestamp in dayStart until dayEnd }
    val observations = ObservationEngine.observations(todayStats, s.yamaHours, todayEnergy)

    AharaScaffold(
        title = "आहार · AHARA",
        floating = { AddMealFab(nav) },
        bottomBar = { MainBottomBar(nav) },
        actions = {
            androidx.compose.material3.IconButton(onClick = { nav.navigate(Routes.SHLOKA) }) {
                Icon(Icons.Filled.Info, contentDescription = t("shloka_title"), tint = AharaColors.SageDark)
            }
        }
    ) {
        // Greeting
        Box(Modifier.fillMaxWidth()) {
            Column {
                val name = profile?.displayName.orEmpty()
                Text(if (name.isBlank()) t("greeting") else tf("greeting_name", name), style = MaterialTheme.typography.headlineSmall)
                Text(today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")), style = MaterialTheme.typography.bodyMedium, color = AharaColors.Muted)
            }
            Botanical(Modifier.size(56.dp).align(Alignment.CenterEnd))
        }

        // Yama clock hero
        YamaHeroCard(lastMeal, now, s.yamaHours)

        // Stats grid
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(t("today_meals"), todayStats.mealCount.toString(), Modifier.weight(1f))
            StatTile(t("avg_interval_7d"), weekStats.avgIntervalHours?.let { hm(it) } ?: "—", Modifier.weight(1f), accent = AharaColors.Olive)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(t("regularity"), weekStats.regularityPct?.let { "$it%" } ?: "—", Modifier.weight(1f), accent = AharaColors.Sky)
            StatTile(t("discomfort_freq"), weekStats.discomfortPct?.let { "$it%" } ?: "—", Modifier.weight(1f), accent = AharaColors.Clay)
        }

        // Last meal
        SectionCard(title = t("last_meal"), onClick = lastMeal?.let { m -> { nav.navigate(Routes.addMeal(m.id)) } }) {
            if (lastMeal == null) Text(t("no_meals_yet"), color = AharaColors.Muted)
            else {
                val time = Instant.ofEpochMilli(lastMeal.timestamp).atZone(zone)
                Text(
                    t("meal_${lastMeal.mealType}") + " · " + time.format(timeFmt) +
                        (if (time.toLocalDate() != today) " · " + time.format(dateFmt) else ""),
                    style = MaterialTheme.typography.titleSmall
                )
                if (lastMeal.foodName.isNotBlank()) Text(lastMeal.foodName, style = MaterialTheme.typography.bodyMedium)
                if (!lastMeal.responseRecorded) {
                    TextButton(onClick = { nav.navigate(Routes.POST_MEAL) }) { Text(t("record_response")) }
                }
            }
        }

        // Today's observations
        SectionCard(title = t("todays_observations"), subtitle = t("observations_note")) {
            observations.forEach { ObservationRow(it) }
        }

        // Hydration & sleep
        SectionCard(title = t("hydration_sleep")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t("water") + ": " + (todayLog?.waterGlasses ?: 0) + " " + t("glasses"), modifier = Modifier.weight(1f))
                FilledTonalIconButton(onClick = { vm.changeWater(-1) }) { Text("−") }
                Spacer(Modifier.width(6.dp))
                FilledTonalIconButton(onClick = { vm.changeWater(1) }) { Text("+") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                val sl = todayLog?.sleepHours ?: 0f
                Text(t("sleep_last_night") + ": " + (if (sl > 0f) ObservationEngine.fmt(sl.toDouble()) + " " + t("hours_short") else "—"), modifier = Modifier.weight(1f))
                FilledTonalIconButton(onClick = { vm.setSleep(sl - 0.5f) }) { Text("−") }
                Spacer(Modifier.width(6.dp))
                FilledTonalIconButton(onClick = { vm.setSleep(if (sl == 0f) (profile?.sleepHours ?: 7f) else sl + 0.5f) }) { Text("+") }
            }
        }

        // Hunger pattern
        SectionCard(title = t("hunger_pattern_7d")) {
            val d = weekStats.hungerDistribution
            val colors = listOf(Color(0xFFD9D2BE), AharaColors.SageLight, AharaColors.Sage, AharaColors.SageDark, AharaColors.Olive)
            ProportionBar(d.mapIndexed { i, v -> v to colors[i] })
            Legend((0..4).map { t("hunger_$it") + " (${d[it]})" to colors[it] })
        }

        // Energy trend
        SectionCard(title = t("energy_trend_7d"), subtitle = t("energy_scale_hint")) {
            LineChart(
                values = series.map { it.avgEnergy?.toFloat() },
                labels = series.map { it.date.dayOfMonth.toString() },
                minY = 1f, maxY = 5f
            )
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun YamaHeroCard(lastMeal: MealEntry?, now: Long, yamaHours: Float) {
    val hours = lastMeal?.let { (now - it.timestamp) / 3_600_000.0 }
    val (statusKey, color) = when {
        hours == null -> "yama_status_none" to AharaColors.Muted
        hours < yamaHours -> "yama_status_within" to AharaColors.Clay
        hours <= yamaHours * 2 -> "yama_status_window" to AharaColors.SageDark
        else -> "yama_status_beyond" to AharaColors.Gold
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(AharaColors.SageDark, AharaColors.Sage)))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(t("time_since_last_meal"), color = AharaColors.Ivory.copy(alpha = 0.85f), style = MaterialTheme.typography.labelLarge)
        Text(hours?.let { hm(it) } ?: "—", color = Color.White, style = MaterialTheme.typography.headlineLarge)
        // Progress across 0..2 Yama (+ overflow)
        val frac = ((hours ?: 0.0) / (yamaHours * 2.5)).toFloat().coerceIn(0f, 1f)
        Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Color.White.copy(alpha = 0.25f))) {
            Box(Modifier.fillMaxWidth(frac).height(10.dp).clip(RoundedCornerShape(5.dp)).background(AharaColors.Gold))
        }
        Row(Modifier.fillMaxWidth()) {
            Text("0", color = AharaColors.Ivory, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            Text("1 " + t("yama"), color = AharaColors.Ivory, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            Text("2 " + t("yama"), color = AharaColors.Ivory, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.5f))
        }
        Box(Modifier.clip(RoundedCornerShape(50)).background(Color.White).padding(horizontal = 12.dp, vertical = 5.dp)) {
            Text(t(statusKey), color = color, style = MaterialTheme.typography.labelLarge)
        }
        Text(tf("yama_reference", ObservationEngine.fmt(yamaHours.toDouble())), color = AharaColors.Ivory.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun ObservationRow(o: ObservationEngine.Observation) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 2.dp)) {
        com.ahara.app.ui.components.ModuleBadge(o.module)
        Spacer(Modifier.width(10.dp))
        Text(tf(o.key, *o.args.toTypedArray()), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
fun MoreScreen(vm: AppViewModel, nav: NavHostController) {
    AharaScaffold(title = t("nav_more"), bottomBar = { MainBottomBar(nav) }) {
        Text(t("shloka_modules"), style = MaterialTheme.typography.titleMedium, color = AharaColors.Olive)
        MenuRow(Icons.Filled.Info, t("shloka_title"), t("m_all_sa")) { nav.navigate(Routes.SHLOKA) }
        MenuRow(Icons.Filled.Refresh, t("interval_title"), t("m1_sa") + " · " + t("m2_sa")) { nav.navigate(Routes.INTERVAL) }
        MenuRow(Icons.Filled.Warning, t("postmeal_title"), t("m3_sa")) { nav.navigate(Routes.POST_MEAL) }
        MenuRow(Icons.Filled.Favorite, t("energy_title"), t("m4_sa")) { nav.navigate(Routes.ENERGY) }

        Text(t("assessments"), style = MaterialTheme.typography.titleMedium, color = AharaColors.Olive)
        MenuRow(Icons.Filled.Star, t("prakriti_title"), t("prakriti_sub")) { nav.navigate(Routes.PRAKRITI) }
        MenuRow(Icons.Filled.Star, t("agni_title"), t("agni_sub")) { nav.navigate(Routes.AGNI) }

        Text(t("analytics_reports"), style = MaterialTheme.typography.titleMedium, color = AharaColors.Olive)
        MenuRow(ChartIcon, t("daily_analytics")) { nav.navigate(Routes.analytics("daily")) }
        MenuRow(ChartIcon, t("weekly_analytics")) { nav.navigate(Routes.analytics("weekly")) }
        MenuRow(ChartIcon, t("monthly_analytics")) { nav.navigate(Routes.analytics("monthly")) }
        MenuRow(Icons.Filled.Share, t("reports_title"), t("reports_sub")) { nav.navigate(Routes.REPORTS) }
        MenuRow(Icons.Filled.Search, t("research_title"), t("research_sub")) { nav.navigate(Routes.RESEARCH) }

        Text(t("account_settings"), style = MaterialTheme.typography.titleMedium, color = AharaColors.Olive)
        MenuRow(Icons.Filled.Person, t("profile_title")) { nav.navigate(Routes.PROFILE) }
        MenuRow(Icons.Filled.Notifications, t("reminders_title")) { nav.navigate(Routes.REMINDERS) }
        MenuRow(Icons.Filled.Settings, t("settings_title"), t("settings_sub")) { nav.navigate(Routes.SETTINGS) }
        MenuRow(Icons.Filled.Lock, t("privacy_title")) { nav.navigate(Routes.SETTINGS) }
        Spacer(Modifier.height(16.dp))
        Text(t("tagline"), style = MaterialTheme.typography.bodySmall, color = AharaColors.Muted, modifier = Modifier.fillMaxWidth())
    }
}
