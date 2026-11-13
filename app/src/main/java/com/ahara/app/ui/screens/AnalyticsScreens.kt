package com.ahara.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ahara.app.AppViewModel
import com.ahara.app.Routes
import com.ahara.app.data.DailyLog
import com.ahara.app.data.EnergyLog
import com.ahara.app.data.MealEntry
import com.ahara.app.data.MealType
import com.ahara.app.data.Settings
import com.ahara.app.data.UserProfile
import com.ahara.app.data.hasDiscomfort
import com.ahara.app.data.symptomScore
import com.ahara.app.engine.ObservationEngine
import com.ahara.app.export.ReportLine
import com.ahara.app.i18n.Strings
import com.ahara.app.ui.components.AharaScaffold
import com.ahara.app.ui.components.BarChart
import com.ahara.app.ui.components.ChoiceChips
import com.ahara.app.ui.components.Legend
import com.ahara.app.ui.components.LineChart
import com.ahara.app.ui.components.LocalLang
import com.ahara.app.ui.components.NoteBox
import com.ahara.app.ui.components.SectionCard
import com.ahara.app.ui.components.StatTile
import com.ahara.app.ui.components.TimelineChart
import com.ahara.app.ui.components.t
import com.ahara.app.ui.components.tf
import com.ahara.app.ui.theme.AharaColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun mealColor(type: String): Color = when (type) {
    MealType.BREAKFAST.name -> AharaColors.Gold
    MealType.LUNCH.name -> AharaColors.SageDark
    MealType.DINNER.name -> AharaColors.Olive
    else -> AharaColors.Sky
}

fun periodDays(period: String) = when (period) {
    "daily" -> 1
    "monthly" -> 30
    else -> 7
}

@Composable
fun AnalyticsScreen(vm: AppViewModel, nav: NavHostController, period: String) {
    val meals by vm.meals.collectAsState()
    val settings by vm.settings.collectAsState()
    val energyLogs by vm.energyLogs.collectAsState()
    val profile by vm.profile.collectAsState()
    val dailyLogs by vm.dailyLogs.collectAsState()
    val s = settings ?: return
    val zone = vm.zone
    val lang = LocalLang.current
    var endDate by remember(period) { mutableStateOf(LocalDate.now(zone)) }
    val days = periodDays(period)
    val startDate = endDate.minusDays((days - 1).toLong())
    val start = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
    val end = endDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val prevStart = startDate.minusDays(days.toLong()).atStartOfDay(zone).toInstant().toEpochMilli()

    val st = remember(meals, s, start) { ObservationEngine.stats(meals, start, end, s.yamaHours, s.includeOvernight, zone) }
    val prev = remember(meals, s, start) { ObservationEngine.stats(meals, prevStart, start, s.yamaHours, s.includeOvernight, zone) }
    val series = remember(meals, s, endDate, days) { ObservationEngine.daySeries(meals, endDate, days, s.yamaHours, s.includeOvernight, zone) }
    val periodEnergy = energyLogs.filter { it.timestamp in start until end }
    val obs = ObservationEngine.observations(st, s.yamaHours, periodEnergy)
    val periodMeals = meals.filter { it.timestamp in start until end }.sortedBy { it.timestamp }

    val titleKey = "${period}_analytics"
    AharaScaffold(title = t(titleKey), bottomBar = { MainBottomBar(nav) }, actions = {
        IconButton(onClick = {
            vm.sharePdf(
                "ahara_${period}_${endDate}.pdf",
                buildReport(lang, Strings.t(lang, titleKey), startDate, endDate, st, obs, periodMeals, profile, vm.latest("PRAKRITI")?.result, vm.latest("AGNI")?.result, dailyLogs, periodEnergy, s, zone)
            )
        }) { Icon(Icons.Filled.Share, t("export_pdf"), tint = AharaColors.SageDark) }
    }) {
        ChoiceChips(
            listOf("daily" to t("daily"), "weekly" to t("weekly"), "monthly" to t("monthly")),
            period
        ) { p -> if (p != period) nav.navigate(Routes.analytics(p)) { popUpTo(Routes.HOME); launchSingleTop = true } }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { endDate = endDate.minusDays(days.toLong()) }) { Icon(Icons.Filled.KeyboardArrowLeft, t("previous")) }
            Text(
                if (days == 1) endDate.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))
                else startDate.format(dateFmt) + " – " + endDate.format(dateFmt),
                style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f)
            )
            IconButton(
                enabled = endDate < LocalDate.now(zone),
                onClick = { endDate = endDate.plusDays(days.toLong()) }
            ) { Icon(Icons.Filled.KeyboardArrowRight, t("next")) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(t("meals"), st.mealCount.toString(), Modifier.weight(1f))
            StatTile(t("avg_interval"), st.avgIntervalHours?.let { hm(it) } ?: "—", Modifier.weight(1f), accent = AharaColors.Olive)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(t("iv_short"), st.shortCount.toString(), Modifier.weight(1f), accent = AharaColors.Clay)
            StatTile(t("iv_long"), st.longCount.toString(), Modifier.weight(1f), accent = AharaColors.Gold)
            StatTile(t("irregular"), st.irregularCount.toString(), Modifier.weight(1f), accent = AharaColors.Sky)
        }

        if (days == 1) DailyContent(periodMeals, st, zone)
        else {
            SectionCard(title = t("meals_per_day")) {
                BarChart(series.map { it.mealCount.toFloat() }, series.map { labelFor(it.date, days) })
            }
            SectionCard(title = t("avg_interval_per_day"), subtitle = tf("yama_reference", ObservationEngine.fmt(s.yamaHours.toDouble()))) {
                LineChart(
                    series.map { it.avgIntervalHours?.toFloat() }, series.map { labelFor(it.date, days) },
                    0f, (s.yamaHours * 3f),
                    refLines = listOf(s.yamaHours to AharaColors.Clay.copy(alpha = 0.6f), s.yamaHours * 2 to AharaColors.Gold.copy(alpha = 0.7f))
                )
                Legend(listOf(t("one_yama") to AharaColors.Clay, t("two_yamas") to AharaColors.Gold))
            }
            SectionCard(title = t("discomfort_trend")) {
                BarChart(series.map { it.discomfortCount.toFloat() }, series.map { labelFor(it.date, days) }, color = AharaColors.Clay)
            }
            SectionCard(title = t("energy_variation"), subtitle = t("energy_scale_hint")) {
                LineChart(series.map { it.avgEnergy?.toFloat() }, series.map { labelFor(it.date, days) }, 1f, 5f)
            }
            SectionCard(title = t("hunger_observations")) {
                BarChart(st.hungerDistribution.map { it.toFloat() }, (0..4).map { t("hunger_$it") }, color = AharaColors.Sage)
            }
            SectionCard(title = t("comparison_prev"), subtitle = t("comparison_prev_sub")) {
                CompareRow(t("meals"), prev.mealCount.toString(), st.mealCount.toString())
                CompareRow(t("avg_interval"), prev.avgIntervalHours?.let { hm(it) } ?: "—", st.avgIntervalHours?.let { hm(it) } ?: "—")
                CompareRow(t("iv_short"), prev.shortCount.toString(), st.shortCount.toString())
                CompareRow(t("iv_long"), prev.longCount.toString(), st.longCount.toString())
                CompareRow(t("irregular"), prev.irregularCount.toString(), st.irregularCount.toString())
                CompareRow(t("discomfort_freq"), prev.discomfortPct?.let { "$it%" } ?: "—", st.discomfortPct?.let { "$it%" } ?: "—")
                CompareRow(t("avg_energy_before"), prev.avgEnergyBefore?.let { ObservationEngine.fmt(it) } ?: "—", st.avgEnergyBefore?.let { ObservationEngine.fmt(it) } ?: "—")
                CompareRow(t("skipped_meals"), prev.skippedCount.toString(), st.skippedCount.toString())
            }
        }

        SectionCard(title = t("observations"), subtitle = t("observations_note")) {
            obs.forEach { ObservationRow(it) }
        }
        Button(
            onClick = {
                vm.sharePdf(
                    "ahara_${period}_${endDate}.pdf",
                    buildReport(lang, Strings.t(lang, titleKey), startDate, endDate, st, obs, periodMeals, profile, vm.latest("PRAKRITI")?.result, vm.latest("AGNI")?.result, dailyLogs, periodEnergy, s, zone)
                )
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AharaColors.SageDark)
        ) { Text(t("export_pdf")) }
        OutlinedButton(onClick = { vm.shareMealsCsv() }, modifier = Modifier.fillMaxWidth()) { Text(t("export_csv")) }
        Spacer(Modifier.height(24.dp))
    }
}

private fun labelFor(d: LocalDate, days: Int): String =
    if (days <= 7) d.format(DateTimeFormatter.ofPattern("EEE")).take(2) else if (d.dayOfMonth % 5 == 0 || d.dayOfMonth == 1) d.dayOfMonth.toString() else ""

@Composable
private fun CompareRow(label: String, before: String, now: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = AharaColors.Muted, modifier = Modifier.weight(1.4f))
        Text(before, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text("→ $now", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun DailyContent(dayMeals: List<MealEntry>, st: ObservationEngine.Stats, zone: ZoneId) {
    SectionCard(title = t("meal_timeline")) {
        TimelineChart(dayMeals.filter { !it.skipped }.map { ObservationEngine.minuteOfDay(it.timestamp, zone) to mealColor(it.mealType) })
        Legend(MealType.values().map { t("meal_${it.name}") to mealColor(it.name) })
    }
    SectionCard(title = t("meal_intervals")) {
        if (st.intervals.isEmpty()) Text(t("not_enough_data"), color = AharaColors.Muted)
        st.intervals.forEach { iv ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    Instant.ofEpochMilli(iv.from.timestamp).atZone(zone).format(timeFmt) + " → " +
                        Instant.ofEpochMilli(iv.to.timestamp).atZone(zone).format(timeFmt),
                    modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium
                )
                intervalPill(iv.cls, iv.hours)
            }
        }
    }
    SectionCard(title = t("hunger_post_meal_energy")) {
        if (dayMeals.isEmpty()) Text(t("no_meals_day"), color = AharaColors.Muted)
        dayMeals.forEach { m ->
            Column {
                Text(
                    Instant.ofEpochMilli(m.timestamp).atZone(zone).format(timeFmt) + " · " + t("meal_${m.mealType}") +
                        (if (m.foodName.isNotBlank()) " · " + m.foodName else ""),
                    style = MaterialTheme.typography.titleSmall
                )
                if (m.skipped) Text(t("skipped_meal"), color = AharaColors.Clay, style = MaterialTheme.typography.bodySmall)
                else Text(
                    tf("daily_meal_line", t("hunger_${m.hungerBefore}"), m.energyBefore.toString(),
                        if (m.responseRecorded) m.energyAfter.toString() else "—",
                        if (m.responseRecorded) (if (m.hasDiscomfort) t("discomfort_short") else t("comfortable")) else t("response_pending")),
                    style = MaterialTheme.typography.bodySmall, color = AharaColors.Muted
                )
            }
            Spacer(Modifier.width(0.dp))
        }
    }
    SectionCard(title = t("energy_before_after")) {
        val eaten = dayMeals.filter { !it.skipped }
        if (eaten.isNotEmpty()) {
            BarChart(eaten.map { it.energyBefore.toFloat() }, eaten.map { Instant.ofEpochMilli(it.timestamp).atZone(zone).format(timeFmt) }, color = AharaColors.Sage, height = 100.dp)
            Text(t("energy_before"), style = MaterialTheme.typography.bodySmall, color = AharaColors.Muted)
            BarChart(eaten.map { if (it.responseRecorded) it.energyAfter.toFloat() else 0f }, eaten.map { Instant.ofEpochMilli(it.timestamp).atZone(zone).format(timeFmt) }, color = AharaColors.Olive, height = 100.dp)
            Text(t("energy_after"), style = MaterialTheme.typography.bodySmall, color = AharaColors.Muted)
        } else Text(t("not_enough_data"), color = AharaColors.Muted)
    }
}

/** Builds a localised PDF report for a period. Non-composable so it can be reused by the Reports screen. */
fun buildReport(
    lang: String,
    title: String,
    startDate: LocalDate,
    endDate: LocalDate,
    st: ObservationEngine.Stats,
    obs: List<ObservationEngine.Observation>,
    periodMeals: List<MealEntry>,
    profile: UserProfile?,
    prakriti: String?,
    agni: String?,
    dailyLogs: List<DailyLog>,
    energyLogs: List<EnergyLog>,
    s: Settings,
    zone: ZoneId
): List<ReportLine> {
    fun tr(k: String) = Strings.t(lang, k)
    fun na(v: Any?) = v?.toString() ?: "—"
    val L = ArrayList<ReportLine>()
    val df = DateTimeFormatter.ofPattern("d MMM yyyy")
    L += ReportLine("AHARA · आहार — $title", ReportLine.Style.TITLE)
    L += ReportLine(tr("tagline"), ReportLine.Style.SMALL)
    L += ReportLine(startDate.format(df) + if (startDate != endDate) " – " + endDate.format(df) else "", ReportLine.Style.BODY)
    if (profile != null) {
        L += ReportLine(tr("profile_title"), ReportLine.Style.HEADING)
        if (profile.displayName.isNotBlank()) L += ReportLine(tr("display_name_optional") + ": " + profile.displayName)
        if (profile.ageRange.isNotBlank()) L += ReportLine(tr("age_range") + ": " + profile.ageRange)
        L += ReportLine(tr("prakriti") + ": " + (prakriti?.let { tr("res_$it") } ?: "—") + "   " + tr("agni") + ": " + (agni?.let { tr("res_$it") } ?: "—"))
    }
    L += ReportLine(tr("summary"), ReportLine.Style.HEADING)
    L += ReportLine(tr("meals") + ": " + st.mealCount + "   " + tr("skipped_meals") + ": " + st.skippedCount)
    L += ReportLine(tr("avg_interval") + ": " + (st.avgIntervalHours?.let { hm(it) } ?: "—"))
    L += ReportLine(tr("iv_short") + ": " + st.shortCount + "   " + tr("iv_normal") + ": " + st.normalCount + "   " + tr("iv_long") + ": " + st.longCount)
    L += ReportLine(tr("eating_without_hunger") + ": " + st.withoutHungerCount + "   " + tr("irregular_timings") + ": " + st.irregularCount)
    L += ReportLine(tr("regularity") + ": " + (st.regularityPct?.let { "$it%" } ?: "—"))
    L += ReportLine(tr("discomfort_freq") + ": " + (st.discomfortPct?.let { "$it%" } ?: "—") + "  (" + tr("responses") + ": " + st.responsesRecorded + ")")
    L += ReportLine(tr("avg_energy_before") + ": " + na(st.avgEnergyBefore?.let { ObservationEngine.fmt(it) }) + "   " + tr("avg_energy_after") + ": " + na(st.avgEnergyAfter?.let { ObservationEngine.fmt(it) }))
    val logsInRange = dailyLogs.filter { val d = LocalDate.parse(it.date); !d.isBefore(startDate) && !d.isAfter(endDate) }
    if (logsInRange.isNotEmpty()) {
        L += ReportLine(tr("water") + ": " + ObservationEngine.fmt(logsInRange.map { it.waterGlasses }.average()) + " " + tr("glasses") + " / " + tr("day_avg") +
            "   " + tr("sleep_last_night") + ": " + ObservationEngine.fmt(logsInRange.filter { it.sleepHours > 0 }.map { it.sleepHours.toDouble() }.ifEmpty { listOf(0.0) }.average()) + " " + tr("hours_short"))
    }
    if (energyLogs.isNotEmpty()) L += ReportLine(tr("recent_checkins") + ": " + energyLogs.size)

    L += ReportLine(tr("observations"), ReportLine.Style.HEADING)
    obs.forEach { o ->
        val prefix = if (o.module > 0) "[${tr("m${o.module}_sa")}] " else ""
        L += ReportLine("• " + prefix + Strings.f(lang, o.key, *o.args.toTypedArray()))
    }

    L += ReportLine(tr("meal_records"), ReportLine.Style.HEADING)
    periodMeals.forEach { m ->
        val time = Instant.ofEpochMilli(m.timestamp).atZone(zone)
        val head = time.format(DateTimeFormatter.ofPattern("d MMM HH:mm")) + " · " + tr("meal_${m.mealType}") +
            (if (m.foodName.isNotBlank()) " · " + m.foodName else "")
        L += ReportLine(head)
        L += ReportLine(
            if (m.skipped) "   " + tr("skipped_meal")
            else "   " + tr("hunger") + " " + m.hungerBefore + "/4 · " + tr("energy_before") + " " + m.energyBefore + "/5" +
                (if (m.responseRecorded) " · " + tr("energy_after") + " " + m.energyAfter + "/5 · " + tr("symptom_score") + " " + m.symptomScore + "/15 · " + tr("overall_comfort") + " " + m.comfort + "/5" else ""),
            ReportLine.Style.SMALL
        )
    }
    L += ReportLine(tr("method"), ReportLine.Style.HEADING)
    L += ReportLine(Strings.f(lang, "yama_reference", ObservationEngine.fmt(s.yamaHours.toDouble())), ReportLine.Style.SMALL)
    L += ReportLine(tr("assumptions_note"), ReportLine.Style.SMALL)
    L += ReportLine(tr("disclaimer"), ReportLine.Style.SMALL)
    return L
}

@Composable
fun ReportsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val meals by vm.meals.collectAsState()
    val settings by vm.settings.collectAsState()
    val profile by vm.profile.collectAsState()
    val dailyLogs by vm.dailyLogs.collectAsState()
    val energyLogs by vm.energyLogs.collectAsState()
    val s = settings ?: return
    val lang = LocalLang.current
    val zone = vm.zone

    fun pdf(period: String) {
        val days = periodDays(period)
        val endDate = LocalDate.now(zone)
        val startDate = endDate.minusDays((days - 1).toLong())
        val start = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = endDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val st = ObservationEngine.stats(meals, start, end, s.yamaHours, s.includeOvernight, zone)
        val el = energyLogs.filter { it.timestamp in start until end }
        val obs = ObservationEngine.observations(st, s.yamaHours, el)
        val title = Strings.t(lang, "${period}_analytics")
        vm.sharePdf(
            "ahara_${period}_report_$endDate.pdf",
            buildReport(lang, title, startDate, endDate, st, obs, meals.filter { it.timestamp in start until end }.sortedBy { it.timestamp },
                profile, vm.latest("PRAKRITI")?.result, vm.latest("AGNI")?.result, dailyLogs, el, s, zone)
        )
    }

    AharaScaffold(title = t("reports_title"), onBack = onBack) {
        SectionCard(title = t("pdf_reports"), subtitle = t("pdf_reports_sub")) {
            listOf("daily", "weekly", "monthly").forEach { p ->
                OutlinedButton(onClick = { pdf(p) }, modifier = Modifier.fillMaxWidth()) { Text(t("${p}_analytics") + " · PDF") }
            }
        }
        SectionCard(title = t("csv_exports"), subtitle = t("csv_exports_sub")) {
            OutlinedButton(onClick = { vm.shareMealsCsv() }, modifier = Modifier.fillMaxWidth()) { Text(t("export_meals_csv")) }
            OutlinedButton(onClick = { vm.shareFullCsv() }, modifier = Modifier.fillMaxWidth()) { Text(t("export_full_csv")) }
            OutlinedButton(
                enabled = s.researchConsent,
                onClick = { vm.shareAnonymisedCsv() }, modifier = Modifier.fillMaxWidth()
            ) { Text(t("export_anon_csv")) }
            if (!s.researchConsent) Text(t("anon_requires_consent"), style = MaterialTheme.typography.bodySmall, color = AharaColors.Muted)
        }
        NoteBox(t("export_privacy_note"))
        Spacer(Modifier.height(24.dp))
    }
}
