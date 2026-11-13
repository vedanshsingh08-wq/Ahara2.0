package com.ahara.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ahara.app.AppViewModel
import com.ahara.app.data.EnergyLog
import com.ahara.app.data.MealType
import com.ahara.app.engine.ObservationEngine
import com.ahara.app.engine.ObservationEngine.IntervalClass
import com.ahara.app.ui.components.AharaScaffold
import com.ahara.app.ui.components.BarChart
import com.ahara.app.ui.components.ChoiceChips
import com.ahara.app.ui.components.Legend
import com.ahara.app.ui.components.LineChart
import com.ahara.app.ui.components.ModuleBadge
import com.ahara.app.ui.components.NoteBox
import com.ahara.app.ui.components.ProportionBar
import com.ahara.app.ui.components.ScaleSelector
import com.ahara.app.ui.components.SectionCard
import com.ahara.app.ui.components.StatTile
import com.ahara.app.ui.components.t
import com.ahara.app.ui.components.tf
import com.ahara.app.ui.theme.AharaColors
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private fun range(vm: AppViewModel, days: Int): Pair<Long, Long> {
    val today = LocalDate.now(vm.zone)
    return today.minusDays((days - 1).toLong()).atStartOfDay(vm.zone).toInstant().toEpochMilli() to
        today.plusDays(1).atStartOfDay(vm.zone).toInstant().toEpochMilli()
}

@Composable
fun IntervalScreen(vm: AppViewModel, onBack: () -> Unit) {
    val meals by vm.meals.collectAsState()
    val settings by vm.settings.collectAsState()
    val s = settings ?: return
    var days by remember { mutableIntStateOf(7) }
    val (start, end) = range(vm, days)
    val st = remember(meals, s, days) { ObservationEngine.stats(meals, start, end, s.yamaHours, s.includeOvernight, vm.zone) }
    val typical = remember(meals) { ObservationEngine.typicalTimes(meals, vm.zone) }

    AharaScaffold(title = t("interval_title"), onBack = onBack) {
        ChoiceChips(listOf("7" to t("last_7_days"), "30" to t("last_30_days")), days.toString()) { days = it.toInt() }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(t("avg_interval"), st.avgIntervalHours?.let { hm(it) } ?: "—", Modifier.weight(1f))
            StatTile(t("intervals_n"), st.intervals.size.toString(), Modifier.weight(1f), accent = AharaColors.Olive)
        }

        SectionCard(title = t("yama_classification"), subtitle = tf("yama_reference", ObservationEngine.fmt(s.yamaHours.toDouble()))) {
            ProportionBar(
                listOf(
                    st.shortCount to AharaColors.Clay,
                    st.normalCount to AharaColors.Sage,
                    st.longCount to AharaColors.Gold
                )
            )
            Legend(
                listOf(
                    t("iv_short") + " (${st.shortCount})" to AharaColors.Clay,
                    t("iv_normal") + " (${st.normalCount})" to AharaColors.Sage,
                    t("iv_long") + " (${st.longCount})" to AharaColors.Gold
                )
            )
        }

        SectionCard(title = t("interval_distribution")) {
            val bins = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8)
            val counts = bins.map { b ->
                st.intervals.count { iv -> if (b == 8) iv.hours >= 8 else iv.hours >= b && iv.hours < b + 1 }.toFloat()
            }
            BarChart(
                values = counts,
                labels = bins.map { if (it == 8) "8+" else "$it-${it + 1}" },
                highlight = { i ->
                    val mid = if (i == 8) 9.0 else i + 0.5
                    when (ObservationEngine.classify(mid, s.yamaHours)) {
                        IntervalClass.SHORT -> AharaColors.Clay
                        IntervalClass.LONG -> AharaColors.Gold
                        IntervalClass.NORMAL -> AharaColors.Sage
                    }
                }
            )
            Text(t("hours_axis"), style = MaterialTheme.typography.bodySmall, color = AharaColors.Muted)
        }

        SectionCard(title = t("m1_sa"), subtitle = t("m1_name")) {
            Text(t("m1_desc"), style = MaterialTheme.typography.bodyMedium)
            InfoLine(t("meals_within_yama"), st.shortCount.toString())
            InfoLine(t("eating_without_hunger"), st.withoutHungerCount.toString())
            InfoLine(t("irregular_timings"), st.irregularCount.toString())
            InfoLine(t("regularity"), st.regularityPct?.let { "$it%" } ?: "—")
            Text(t("typical_times"), style = MaterialTheme.typography.titleSmall)
            listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER).forEach { mt ->
                val mins = typical[mt.name]
                InfoLine(t("meal_${mt.name}"), mins?.let { String.format("%02d:%02d", it / 60, it % 60) } ?: "—")
            }
        }

        SectionCard(title = t("m2_sa"), subtitle = t("m2_name")) {
            Text(t("m2_desc"), style = MaterialTheme.typography.bodyMedium)
            InfoLine(t("long_gaps"), st.longCount.toString())
            InfoLine(t("skipped_meals"), st.skippedCount.toString())
            InfoLine(t("energy_before_after_long"), st.avgEnergyAfterLongGap?.let { ObservationEngine.fmt(it) } ?: "—")
            InfoLine(t("energy_before_other"), st.avgEnergyOtherwise?.let { ObservationEngine.fmt(it) } ?: "—")
        }

        SectionCard(title = t("recent_intervals")) {
            val recent = st.intervals.takeLast(12).reversed()
            if (recent.isEmpty()) Text(t("not_enough_data"), color = AharaColors.Muted)
            recent.forEach { iv ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val a = Instant.ofEpochMilli(iv.from.timestamp).atZone(vm.zone)
                    val b = Instant.ofEpochMilli(iv.to.timestamp).atZone(vm.zone)
                    Text(
                        b.format(DateTimeFormatter.ofPattern("d MMM")) + "  " + a.format(timeFmt) + " → " + b.format(timeFmt),
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f)
                    )
                    intervalPill(iv.cls, iv.hours)
                }
            }
        }
        NoteBox(t("assumptions_note"))
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = AharaColors.Muted, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
fun EnergyScreen(vm: AppViewModel, onBack: () -> Unit) {
    val meals by vm.meals.collectAsState()
    val logs by vm.energyLogs.collectAsState()
    val settings by vm.settings.collectAsState()
    val s = settings ?: return
    val (start, end) = range(vm, 14)
    val st = remember(meals, s) { ObservationEngine.stats(meals, start, end, s.yamaHours, s.includeOvernight, vm.zone) }
    val series = remember(meals, s) { ObservationEngine.daySeries(meals, LocalDate.now(vm.zone), 14, s.yamaHours, s.includeOvernight, vm.zone) }
    val recentLogs = logs.filter { it.timestamp in start until end }

    var energy by remember { mutableIntStateOf(3) }
    var hunger by remember { mutableIntStateOf(2) }
    var fatigue by remember { mutableIntStateOf(0) }
    var weakness by remember { mutableIntStateOf(0) }
    var note by remember { mutableStateOf("") }

    // last meal gap for context
    val lastMeal = meals.filter { !it.skipped }.maxByOrNull { it.timestamp }
    val gap = lastMeal?.let { (System.currentTimeMillis() - it.timestamp) / 3_600_000.0 }

    AharaScaffold(title = t("energy_title"), onBack = onBack) {
        SectionCard(title = t("m4_sa"), subtitle = t("m4_name")) {
            Text(t("m4_desc"), style = MaterialTheme.typography.bodyMedium)
        }
        SectionCard(title = t("energy_checkin"), subtitle = gap?.let { tf("checkin_gap", hm(it)) }) {
            ScaleSelector(t("energy_now"), 1..5, energy, (1..5).map { t("energy_$it") }) { energy = it }
            ScaleSelector(t("hunger_now"), 0..4, hunger, (0..4).map { t("hunger_$it") }) { hunger = it }
            ScaleSelector(t("fatigue"), 0..3, fatigue, (0..3).map { t("sev_$it") }) { fatigue = it }
            ScaleSelector(t("weakness"), 0..3, weakness, (0..3).map { t("sev_$it") }) { weakness = it }
            OutlinedTextField(note, { note = it }, label = { Text(t("notes")) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Button(
                onClick = {
                    vm.addEnergyLog(EnergyLog(timestamp = System.currentTimeMillis(), energy = energy, hunger = hunger, fatigue = fatigue, weakness = weakness, note = note.trim()))
                    note = ""; fatigue = 0; weakness = 0
                },
                colors = ButtonDefaults.buttonColors(containerColor = AharaColors.SageDark)
            ) { Text(t("save_checkin")) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(t("avg_energy_before"), st.avgEnergyBefore?.let { ObservationEngine.fmt(it) } ?: "—", Modifier.weight(1f))
            StatTile(t("avg_energy_after"), st.avgEnergyAfter?.let { ObservationEngine.fmt(it) } ?: "—", Modifier.weight(1f), accent = AharaColors.Olive)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(t("fatigue_reports"), st.fatigueReports.toString(), Modifier.weight(1f), accent = AharaColors.Clay)
            StatTile(t("weakness_reports"), st.weaknessReports.toString(), Modifier.weight(1f), accent = AharaColors.Gold)
        }

        SectionCard(title = t("energy_vs_gap"), subtitle = t("last_14_days")) {
            BarChart(
                values = listOf((st.avgEnergyOtherwise ?: 0.0).toFloat(), (st.avgEnergyAfterLongGap ?: 0.0).toFloat()),
                labels = listOf(t("after_normal_gap"), t("after_long_gap")),
                highlight = { if (it == 1) AharaColors.Gold else null }
            )
        }

        SectionCard(title = t("daily_energy_fluctuation"), subtitle = t("energy_scale_hint")) {
            LineChart(series.map { it.avgEnergy?.toFloat() }, series.map { it.date.dayOfMonth.toString() }, 1f, 5f)
        }

        SectionCard(title = t("recent_checkins")) {
            if (recentLogs.isEmpty()) Text(t("no_checkins"), color = AharaColors.Muted)
            recentLogs.take(15).forEach { e ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            Instant.ofEpochMilli(e.timestamp).atZone(vm.zone).format(DateTimeFormatter.ofPattern("d MMM, HH:mm")),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            tf("checkin_summary", e.energy.toString(), e.hunger.toString(), t("sev_${e.fatigue}")) +
                                (if (e.note.isNotBlank()) " · " + e.note else ""),
                            style = MaterialTheme.typography.bodySmall, color = AharaColors.Muted
                        )
                    }
                    IconButton(onClick = { vm.deleteEnergyLog(e) }) { Icon(Icons.Filled.Delete, t("delete"), tint = AharaColors.Muted) }
                }
            }
        }
        NoteBox(t("disclaimer"))
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun ShlokaScreen(vm: AppViewModel, onBack: () -> Unit) {
    val settings by vm.settings.collectAsState()
    val y = ObservationEngine.fmt((settings?.yamaHours ?: 3f).toDouble())
    AharaScaffold(title = t("shloka_title"), onBack = onBack) {
        ShlokaBlock()
        SectionCard(title = t("padaccheda")) {
            Text(
                "याम-मध्ये न भोक्तव्यम् । याम-युग्मम् न लंघयेत् ।\nयाम-मध्ये रस-उद्वेगः । युग्म-अतीते बल-क्षयः ॥",
                style = MaterialTheme.typography.bodyLarge, color = Color(0xFF5A4A1E)
            )
        }
        SectionCard(title = t("hindi_interpretation")) {
            Text(t("shloka_hi"), style = MaterialTheme.typography.bodyLarge)
        }
        SectionCard(title = t("english_interpretation")) {
            Text(t("shloka_en"), style = MaterialTheme.typography.bodyLarge)
        }
        (1..4).forEach { m ->
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ModuleBadge(m)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(t("m${m}_sa"), style = MaterialTheme.typography.titleMedium, color = AharaColors.SageDark)
                        Text(t("m${m}_name"), style = MaterialTheme.typography.bodySmall, color = AharaColors.Muted)
                    }
                }
                Text(t("m${m}_meaning"), style = MaterialTheme.typography.bodyMedium)
                Text(tf("m${m}_app", y, ObservationEngine.fmt(y.toDouble() * 2)), style = MaterialTheme.typography.bodySmall, color = AharaColors.Olive)
            }
        }
        SectionCard(title = t("yama_concept")) {
            Text(tf("yama_concept_body", y), style = MaterialTheme.typography.bodyMedium)
        }
        NoteBox(t("source_note"))
        NoteBox(t("disclaimer"))
        Spacer(Modifier.height(24.dp))
    }
}
