package com.ahara.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ahara.app.AppViewModel
import com.ahara.app.engine.ObservationEngine
import com.ahara.app.engine.ObservationEngine.IntervalClass
import com.ahara.app.export.Exporter
import com.ahara.app.export.ParticipantSummary
import com.ahara.app.ui.components.AharaScaffold
import com.ahara.app.ui.components.BarChart
import com.ahara.app.ui.components.NoteBox
import com.ahara.app.ui.components.SectionCard
import com.ahara.app.ui.components.SwitchRow
import com.ahara.app.ui.components.t
import com.ahara.app.ui.components.tf
import com.ahara.app.ui.theme.AharaColors
import java.time.Instant
import java.time.format.DateTimeFormatter

@Composable
fun ResearchScreen(vm: AppViewModel, onBack: () -> Unit) {
    val meals by vm.meals.collectAsState()
    val settings by vm.settings.collectAsState()
    val imported by vm.imported.collectAsState()
    val profile by vm.profile.collectAsState()
    val s = settings ?: return
    val zone = vm.zone
    var askConsent by remember { mutableStateOf(false) }
    var includeMine by remember { mutableStateOf(true) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) vm.importResearchCsv(uris)
    }

    val ivs = remember(meals, s) { ObservationEngine.intervals(meals, s.yamaHours, s.includeOvernight, zone) }
    val all = remember(meals, s) { ObservationEngine.stats(meals, 0L, Long.MAX_VALUE, s.yamaHours, s.includeOvernight, zone) }

    // Own anonymised summary (computed from the same CSV the export produces, so both paths agree)
    val mine: ParticipantSummary? = remember(meals, s, profile) {
        if (!s.researchConsent || meals.isEmpty()) null
        else Exporter.summariseResearchCsv(
            Exporter.anonymisedCsv(meals, s.participantId, profile, vm.latest("PRAKRITI")?.result ?: "", vm.latest("AGNI")?.result ?: "", s.yamaHours, s.includeOvernight, zone)
        ).firstOrNull()
    }
    val group = (if (includeMine && mine != null) listOf(mine) else emptyList()) + imported.filter { it.participantId != mine?.participantId }

    AharaScaffold(title = t("research_title"), onBack = onBack) {
        NoteBox(t("research_ethics"))

        SectionCard(title = t("consent_title"), subtitle = t("consent_sub")) {
            SwitchRow(t("consent_toggle"), null, s.researchConsent) { on ->
                if (on) askConsent = true else vm.setResearchConsent(false)
            }
            if (s.researchConsent) {
                Text(tf("participant_id", s.participantId), style = MaterialTheme.typography.titleSmall)
                Text(
                    tf("consent_given_on", Instant.ofEpochMilli(s.consentTime).atZone(zone).format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm"))),
                    style = MaterialTheme.typography.bodySmall, color = AharaColors.Muted
                )
                Button(onClick = { vm.shareAnonymisedCsv() }, colors = ButtonDefaults.buttonColors(containerColor = AharaColors.SageDark)) {
                    Text(t("export_anon_csv"))
                }
            }
        }

        SectionCard(title = t("interval_distribution"), subtitle = t("all_records")) {
            val bins = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8)
            BarChart(
                values = bins.map { b -> ivs.count { if (b == 8) it.hours >= 8 else it.hours >= b && it.hours < b + 1 }.toFloat() },
                labels = bins.map { if (it == 8) "8+" else "$it-${it + 1}" }
            )
        }

        SectionCard(title = t("digestive_response_freq"), subtitle = tf("responses_recorded_n", all.responsesRecorded.toString())) {
            val keys = all.symptomCounts.keys.toList()
            BarChart(keys.map { (all.symptomCounts[it] ?: 0).toFloat() }, keys.map { t("sym_short_$it") }, color = AharaColors.Clay)
        }

        SectionCard(title = t("energy_pattern_analysis"), subtitle = t("energy_by_interval_class")) {
            val byClass = IntervalClass.values().map { c ->
                val ids = ivs.filter { it.cls == c }.map { it.to.id }.toSet()
                val ms = meals.filter { it.id in ids }
                c to Pair(
                    ms.map { it.energyBefore }.takeIf { it.isNotEmpty() }?.average(),
                    ms.filter { it.responseRecorded }.map { it.energyAfter }.takeIf { it.isNotEmpty() }?.average()
                )
            }
            byClass.forEach { (c, e) ->
                InfoLine(
                    t("iv_${c.name.lowercase()}"),
                    t("before_short") + " " + (e.first?.let { ObservationEngine.fmt(it) } ?: "—") + "  ·  " +
                        t("after_short") + " " + (e.second?.let { ObservationEngine.fmt(it) } ?: "—")
                )
            }
        }

        SectionCard(title = t("group_comparison"), subtitle = t("group_comparison_sub")) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { picker.launch(arrayOf("text/*", "text/csv", "text/comma-separated-values", "application/octet-stream")) }) {
                    Text(t("import_csv"))
                }
                if (imported.isNotEmpty()) TextButton(onClick = { vm.clearImported() }) { Text(t("clear")) }
            }
            if (mine != null) SwitchRow(t("include_my_data"), null, includeMine) { includeMine = it }
            Text(tf("participants_n", group.size.toString()), style = MaterialTheme.typography.bodySmall, color = AharaColors.Muted)
            if (group.isNotEmpty()) {
                ParticipantTable(group)
                Text(t("by_prakriti"), style = MaterialTheme.typography.titleSmall)
                GroupTable(group.groupBy { it.prakriti })
                Text(t("by_agni"), style = MaterialTheme.typography.titleSmall)
                GroupTable(group.groupBy { it.agni })
            }
        }
        NoteBox(t("correlation_note"))
        Spacer(Modifier.height(24.dp))
    }

    if (askConsent) {
        AlertDialog(
            onDismissRequest = { askConsent = false },
            title = { Text(t("consent_title")) },
            text = { Text(t("consent_body")) },
            confirmButton = { TextButton(onClick = { vm.setResearchConsent(true); askConsent = false }) { Text(t("i_agree")) } },
            dismissButton = { TextButton(onClick = { askConsent = false }) { Text(t("cancel")) } }
        )
    }
}

@Composable
private fun Cell(text: String, w: Int, bold: Boolean = false) {
    Text(
        text, modifier = Modifier.width(w.dp), style = MaterialTheme.typography.bodySmall,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1
    )
}

@Composable
private fun ParticipantTable(rows: List<ParticipantSummary>) {
    Column(Modifier.horizontalScroll(rememberScrollState())) {
        Row {
            Cell(t("participant"), 90, true); Cell(t("prakriti"), 90, true); Cell(t("agni"), 90, true)
            Cell(t("meals"), 50, true); Cell(t("avg_interval_h"), 70, true); Cell("% " + t("iv_short"), 70, true)
            Cell("% " + t("iv_long"), 70, true); Cell("% " + t("discomfort_short"), 80, true); Cell(t("energy"), 60, true)
        }
        rows.forEach { r ->
            Row {
                Cell(r.participantId, 90); Cell(prettyResult(r.prakriti), 90); Cell(prettyResult(r.agni), 90)
                Cell(r.meals.toString(), 50); Cell(r.avgInterval?.let { ObservationEngine.fmt(it) } ?: "—", 70)
                Cell(r.shortPct?.toString() ?: "—", 70); Cell(r.longPct?.toString() ?: "—", 70)
                Cell(r.discomfortPct?.toString() ?: "—", 80); Cell(r.avgEnergyBefore?.let { ObservationEngine.fmt(it) } ?: "—", 60)
            }
        }
    }
}

@Composable
private fun GroupTable(groups: Map<String, List<ParticipantSummary>>) {
    Column(Modifier.horizontalScroll(rememberScrollState())) {
        Row {
            Cell(t("group"), 110, true); Cell("n", 30, true); Cell(t("avg_interval_h"), 80, true)
            Cell("% " + t("discomfort_short"), 90, true); Cell(t("energy"), 60, true)
        }
        groups.forEach { (k, list) ->
            val iv = list.mapNotNull { it.avgInterval }
            val d = list.mapNotNull { it.discomfortPct }
            val e = list.mapNotNull { it.avgEnergyBefore }
            Row {
                Cell(prettyResult(k), 110); Cell(list.size.toString(), 30)
                Cell(if (iv.isEmpty()) "—" else ObservationEngine.fmt(iv.average()), 80)
                Cell(if (d.isEmpty()) "—" else ObservationEngine.fmt(d.average()), 90)
                Cell(if (e.isEmpty()) "—" else ObservationEngine.fmt(e.average()), 60)
            }
        }
    }
}

@Composable
private fun prettyResult(code: String): String =
    if (code.isBlank() || code == "—") "—" else t("res_$code")
