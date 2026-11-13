package com.ahara.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.ahara.app.AppViewModel
import com.ahara.app.data.Assessment
import com.ahara.app.ui.components.AharaScaffold
import com.ahara.app.ui.components.ChoiceChips
import com.ahara.app.ui.components.NoteBox
import com.ahara.app.ui.components.Pill
import com.ahara.app.ui.components.SectionCard
import com.ahara.app.ui.components.t
import com.ahara.app.ui.components.tf
import com.ahara.app.ui.theme.AharaColors
import java.time.Instant
import java.time.format.DateTimeFormatter

/** Prakriti questionnaire: each question has options for V, P, K (in that order). */
const val PRAKRITI_QUESTIONS = 8
val PRAKRITI_OPTIONS = listOf("v", "p", "k")
val PRAKRITI_RESULTS = listOf("VATA", "PITTA", "KAPHA", "VATA_PITTA", "PITTA_KAPHA", "VATA_KAPHA", "TRIDOSHA")

/** Agni questionnaire: options for sama, vishama, tikshna, manda. */
const val AGNI_QUESTIONS = 6
val AGNI_OPTIONS = listOf("sama", "vishama", "tikshna", "manda")
val AGNI_RESULTS = listOf("SAMAGNI", "VISHAMAGNI", "TIKSHNAGNI", "MANDAGNI")

fun scorePrakriti(answers: List<Int>): Pair<String, String> {
    val c = IntArray(3)
    answers.filter { it >= 0 }.forEach { c[it]++ }
    val names = listOf("VATA", "PITTA", "KAPHA")
    val order = (0..2).sortedByDescending { c[it] }
    val top = c[order[0]]; val second = c[order[1]]; val third = c[order[2]]
    val scores = "V:${c[0]},P:${c[1]},K:${c[2]}"
    val result = when {
        top - third <= 1 -> "TRIDOSHA"
        top - second <= 1 -> {
            val pair = listOf(order[0], order[1]).sorted()
            when (pair) {
                listOf(0, 1) -> "VATA_PITTA"
                listOf(1, 2) -> "PITTA_KAPHA"
                else -> "VATA_KAPHA"
            }
        }
        else -> names[order[0]]
    }
    return result to scores
}

fun scoreAgni(answers: List<Int>): Pair<String, String> {
    val c = IntArray(4)
    answers.filter { it >= 0 }.forEach { c[it]++ }
    val best = (0..3).maxByOrNull { c[it] } ?: 0
    return AGNI_RESULTS[best] to "Sa:${c[0]},Vi:${c[1]},Ti:${c[2]},Ma:${c[3]}"
}

@Composable
fun PrakritiScreen(vm: AppViewModel, onBack: () -> Unit) =
    AssessmentScreen(vm, onBack, "PRAKRITI", "prakriti_title", "prakriti_intro", "pq", PRAKRITI_QUESTIONS, PRAKRITI_OPTIONS, PRAKRITI_RESULTS, ::scorePrakriti)

@Composable
fun AgniScreen(vm: AppViewModel, onBack: () -> Unit) =
    AssessmentScreen(vm, onBack, "AGNI", "agni_title", "agni_intro", "aq", AGNI_QUESTIONS, AGNI_OPTIONS, AGNI_RESULTS, ::scoreAgni)

@Composable
private fun AssessmentScreen(
    vm: AppViewModel,
    onBack: () -> Unit,
    kind: String,
    titleKey: String,
    introKey: String,
    qPrefix: String,
    questions: Int,
    options: List<String>,
    results: List<String>,
    score: (List<Int>) -> Pair<String, String>
) {
    val all by vm.assessments.collectAsState()
    val history = all.filter { it.kind == kind }
    var mode by remember { mutableStateOf("Q") } // Q = questionnaire, M = manual
    val answers = remember { mutableStateListOf<Int>().apply { repeat(questions) { add(-1) } } }
    var manual by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("SELF") }
    var practitioner by remember { mutableStateOf("") }
    var lastResult by remember { mutableStateOf<String?>(null) }

    AharaScaffold(title = t(titleKey), onBack = onBack) {
        NoteBox(t("assessment_disclaimer"))
        Text(t(introKey), style = MaterialTheme.typography.bodyMedium)
        ChoiceChips(listOf("Q" to t("questionnaire"), "M" to t("record_known_result")), mode) { mode = it }

        if (mode == "Q") {
            for (q in 0 until questions) {
                SectionCard(title = "${q + 1}. " + t("${qPrefix}${q + 1}_q")) {
                    options.forEachIndexed { oi, o ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { answers[q] = oi }
                        ) {
                            RadioButton(selected = answers[q] == oi, onClick = { answers[q] = oi })
                            Text(t("${qPrefix}${q + 1}_$o"), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        } else {
            SectionCard(title = t("select_result")) {
                ChoiceChips(results.map { it to t("res_$it") }, manual) { manual = it }
            }
        }

        SectionCard(title = t("assessment_source")) {
            ChoiceChips(listOf("SELF" to t("src_SELF"), "PRACTITIONER" to t("src_PRACTITIONER")), source) { source = it }
            if (source == "PRACTITIONER") {
                OutlinedTextField(practitioner, { practitioner = it }, label = { Text(t("practitioner_name")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        }

        val answered = answers.count { it >= 0 }
        val canSave = if (mode == "Q") answered == questions else manual.isNotEmpty()
        if (mode == "Q") Text(tf("answered_n", answered.toString(), questions.toString()), style = MaterialTheme.typography.bodySmall, color = AharaColors.Muted)
        Button(
            enabled = canSave,
            onClick = {
                val (res, sc) = if (mode == "Q") score(answers.toList()) else manual to ""
                vm.addAssessment(Assessment(kind = kind, result = res, source = source, practitioner = practitioner.trim(), scores = sc))
                lastResult = res
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AharaColors.SageDark)
        ) { Text(t("save_assessment")) }

        lastResult?.let { r ->
            SectionCard(title = t("result"), container = AharaColors.SageLight) {
                Text(t("res_$r"), style = MaterialTheme.typography.headlineSmall, color = AharaColors.SageDark)
                Text(t("res_${r}_desc"), style = MaterialTheme.typography.bodyMedium)
            }
        }

        SectionCard(title = t("history")) {
            if (history.isEmpty()) Text(t("no_assessments"), color = AharaColors.Muted)
            history.forEach { a ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(t("res_${a.result}"), style = MaterialTheme.typography.titleSmall)
                        Text(
                            Instant.ofEpochMilli(a.timestamp).atZone(vm.zone).format(DateTimeFormatter.ofPattern("d MMM yyyy")) +
                                " · " + t("src_${a.source}") + (if (a.practitioner.isNotBlank()) " (${a.practitioner})" else "") +
                                (if (a.scores.isNotBlank()) " · ${a.scores}" else ""),
                            style = MaterialTheme.typography.bodySmall, color = AharaColors.Muted
                        )
                    }
                    Pill(if (a.source == "SELF") t("self_reported") else t("practitioner_assisted"))
                    IconButton(onClick = { vm.deleteAssessment(a) }) { Icon(Icons.Filled.Delete, t("delete"), tint = AharaColors.Muted) }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
