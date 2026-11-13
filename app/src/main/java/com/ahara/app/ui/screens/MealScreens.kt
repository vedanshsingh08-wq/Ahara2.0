package com.ahara.app.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ahara.app.AppViewModel
import com.ahara.app.Routes
import com.ahara.app.data.Appetite
import com.ahara.app.data.IndianFoods
import com.ahara.app.data.MealEntry
import com.ahara.app.data.MealType
import com.ahara.app.data.Quantity
import com.ahara.app.data.hasDiscomfort
import com.ahara.app.engine.ObservationEngine
import com.ahara.app.engine.ObservationEngine.IntervalClass
import com.ahara.app.ui.components.AharaScaffold
import com.ahara.app.ui.components.BarChart
import com.ahara.app.ui.components.ChoiceChips
import com.ahara.app.ui.components.LocalLang
import com.ahara.app.ui.components.NoteBox
import com.ahara.app.ui.components.Pill
import com.ahara.app.ui.components.ScaleSelector
import com.ahara.app.ui.components.SectionCard
import com.ahara.app.ui.components.SwitchRow
import com.ahara.app.ui.components.t
import com.ahara.app.ui.components.tf
import com.ahara.app.ui.theme.AharaColors
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun intervalPill(cls: IntervalClass, hours: Double) {
    val (key, color) = when (cls) {
        IntervalClass.SHORT -> "iv_short" to AharaColors.Clay.copy(alpha = 0.25f)
        IntervalClass.NORMAL -> "iv_normal" to AharaColors.SageLight
        IntervalClass.LONG -> "iv_long" to AharaColors.Gold.copy(alpha = 0.3f)
    }
    Pill("${hm(hours)} · ${t(key)}", color = color)
}

@Composable
fun DiaryScreen(vm: AppViewModel, nav: NavHostController) {
    val meals by vm.meals.collectAsState()
    val settings by vm.settings.collectAsState()
    val s = settings ?: return
    val zone = vm.zone
    val ivByTarget = remember(meals, s) {
        ObservationEngine.intervals(meals, s.yamaHours, s.includeOvernight, zone).associateBy { it.to.id }
    }
    val grouped = remember(meals) { meals.groupBy { ObservationEngine.dateOf(it.timestamp, zone) } }
    var toDelete by remember { mutableStateOf<MealEntry?>(null) }

    AharaScaffold(
        title = t("diary_title"),
        floating = { AddMealFab(nav) },
        bottomBar = { MainBottomBar(nav) },
        scroll = false
    ) {
        if (meals.isEmpty()) {
            Spacer(Modifier.height(40.dp))
            Text(t("diary_empty"), style = MaterialTheme.typography.bodyLarge, color = AharaColors.Muted)
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            grouped.forEach { (date, list) ->
                item(key = "h$date") {
                    Text(
                        dayLabel(date),
                        style = MaterialTheme.typography.titleMedium,
                        color = AharaColors.Olive,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(list, key = { it.id }) { m ->
                    MealCard(m, zone, ivByTarget[m.id], onClick = { nav.navigate(Routes.addMeal(m.id)) }, onDelete = { toDelete = m })
                }
            }
        }
    }

    toDelete?.let { m ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(t("delete_meal_q")) },
            text = { Text(t("delete_meal_body")) },
            confirmButton = { TextButton(onClick = { vm.deleteMeal(m); toDelete = null }) { Text(t("delete")) } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text(t("cancel")) } }
        )
    }
}

@Composable
fun dayLabel(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> t("today")
        today.minusDays(1) -> t("yesterday")
        else -> date.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))
    }
}

@Composable
fun MealCard(m: MealEntry, zone: ZoneId, iv: ObservationEngine.MealInterval?, onClick: () -> Unit, onDelete: () -> Unit) {
    SectionCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    Instant.ofEpochMilli(m.timestamp).atZone(zone).format(timeFmt) + " · " + t("meal_${m.mealType}"),
                    style = MaterialTheme.typography.titleSmall
                )
                if (m.skipped) Text(t("skipped_meal"), color = AharaColors.Clay, style = MaterialTheme.typography.bodySmall)
                else if (m.foodName.isNotBlank()) Text(m.foodName, style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = t("delete"), tint = AharaColors.Muted) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (iv != null) intervalPill(iv.cls, iv.hours)
            if (!m.skipped) {
                Pill(t("hunger") + " " + m.hungerBefore + "/4", color = AharaColors.Beige)
                if (m.responseRecorded) {
                    if (m.hasDiscomfort) Pill(t("discomfort_short"), color = AharaColors.Clay.copy(alpha = 0.25f))
                } else Pill(t("response_pending"), color = AharaColors.Beige)
            }
        }
    }
}

@Composable
fun AddMealScreen(vm: AppViewModel, id: Long, onDone: () -> Unit) {
    val context = LocalContext.current
    val meals by vm.meals.collectAsState()
    val settings by vm.settings.collectAsState()
    val zone = vm.zone
    var m by remember { mutableStateOf(MealEntry(timestamp = System.currentTimeMillis(), mealType = defaultMealType(zone))) }
    var loaded by remember { mutableStateOf(id == 0L) }
    var recordResponse by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        if (id != 0L) {
            vm.meal(id)?.let { m = it; recordResponse = it.responseRecorded }
            loaded = true
        }
    }
    if (!loaded) return
    val s = settings ?: return

    val time = Instant.ofEpochMilli(m.timestamp).atZone(zone)
    val previous = meals.filter { !it.skipped && it.id != m.id && it.timestamp < m.timestamp }.maxByOrNull { it.timestamp }

    AharaScaffold(title = if (id == 0L) t("add_meal") else t("edit_meal"), onBack = onDone) {
        SectionCard(title = t("meal_basics")) {
            ChoiceChips(MealType.values().map { it.name to t("meal_${it.name}") }, m.mealType) { m = m.copy(mealType = it) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    DatePickerDialog(context, { _, y, mo, d ->
                        val ldt = LocalDateTime.of(y, mo + 1, d, time.hour, time.minute)
                        m = m.copy(timestamp = ldt.atZone(zone).toInstant().toEpochMilli())
                    }, time.year, time.monthValue - 1, time.dayOfMonth).show()
                }) { Text(time.format(DateTimeFormatter.ofPattern("d MMM yyyy"))) }
                OutlinedButton(onClick = {
                    TimePickerDialog(context, { _, h, min ->
                        val ldt = time.toLocalDate().atTime(h, min)
                        m = m.copy(timestamp = ldt.atZone(zone).toInstant().toEpochMilli())
                    }, time.hour, time.minute, true).show()
                }) { Text(time.format(timeFmt)) }
            }
            if (previous != null && !m.skipped) {
                val hrs = (m.timestamp - previous.timestamp) / 3_600_000.0
                val sameDay = ObservationEngine.dateOf(previous.timestamp, zone) == time.toLocalDate()
                if (sameDay || s.includeOvernight) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(t("since_previous") + " ", style = MaterialTheme.typography.bodySmall, color = AharaColors.Muted)
                        intervalPill(ObservationEngine.classify(hrs, s.yamaHours), hrs)
                    }
                }
            }
            SwitchRow(t("mark_skipped"), t("mark_skipped_sub"), m.skipped) { m = m.copy(skipped = it) }
        }

        if (!m.skipped) {
            SectionCard(title = t("what_did_you_eat")) {
                IndianFoodPicker(m, meals) { m = it }
                OutlinedTextField(m.foodName, { m = m.copy(foodName = it) }, label = { Text(t("food_name")) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(m.ingredients, { m = m.copy(ingredients = it) }, label = { Text(t("ingredients")) }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                Text(t("quantity"), style = MaterialTheme.typography.titleSmall)
                ChoiceChips(Quantity.values().map { it.name to t("qty_${it.name}") }, m.quantity) { m = m.copy(quantity = it) }
            }
            SectionCard(title = t("before_eating"), subtitle = t("m1_sa")) {
                ScaleSelector(t("hunger_before"), 0..4, m.hungerBefore, (0..4).map { t("hunger_$it") }) { m = m.copy(hungerBefore = it) }
                Text(t("appetite"), style = MaterialTheme.typography.titleSmall)
                ChoiceChips(Appetite.values().map { it.name to t("app_${it.name}") }, m.appetite) { m = m.copy(appetite = it) }
                ScaleSelector(t("energy_before"), 1..5, m.energyBefore, (1..5).map { t("energy_$it") }) { m = m.copy(energyBefore = it) }
                ScaleSelector(t("fatigue"), 0..3, m.fatigue, (0..3).map { t("sev_$it") }) { m = m.copy(fatigue = it) }
                ScaleSelector(t("weakness"), 0..3, m.weakness, (0..3).map { t("sev_$it") }) { m = m.copy(weakness = it) }
            }
            SectionCard(title = t("after_eating"), subtitle = t("m3_sa") + " · " + t("m4_sa")) {
                SwitchRow(t("record_response_now"), t("record_response_later"), recordResponse) { recordResponse = it }
                if (recordResponse) PostMealFields(m) { m = it }
            }
        }

        SectionCard(title = t("notes")) {
            OutlinedTextField(m.notes, { m = m.copy(notes = it) }, label = { Text(t("additional_notes")) }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        }

        Button(
            onClick = { vm.saveMeal(m.copy(responseRecorded = recordResponse && !m.skipped)) { onDone() } },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AharaColors.SageDark)
        ) { Text(t("save")) }
        if (id != 0L) {
            TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Text(t("delete"), color = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(t("delete_meal_q")) },
            text = { Text(t("delete_meal_body")) },
            confirmButton = { TextButton(onClick = { vm.deleteMeal(m); confirmDelete = false; onDone() }) { Text(t("delete")) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(t("cancel")) } }
        )
    }
}

fun defaultMealType(zone: ZoneId): String {
    val h = LocalDateTime.now(zone).hour
    return when (h) {
        in 5..10 -> MealType.BREAKFAST.name
        in 11..15 -> MealType.LUNCH.name
        in 18..23 -> MealType.DINNER.name
        else -> MealType.SNACK.name
    }
}

@Composable
fun PostMealFields(m: MealEntry, onChange: (MealEntry) -> Unit) {
    val sev = (0..3).map { t("sev_$it") }
    ScaleSelector(t("sym_heaviness"), 0..3, m.heaviness, sev) { onChange(m.copy(heaviness = it)) }
    ScaleSelector(t("sym_bloating"), 0..3, m.bloating, sev) { onChange(m.copy(bloating = it)) }
    ScaleSelector(t("sym_acidity"), 0..3, m.acidity, sev) { onChange(m.copy(acidity = it)) }
    ScaleSelector(t("sym_nausea"), 0..3, m.nausea, sev) { onChange(m.copy(nausea = it)) }
    ScaleSelector(t("sym_discomfort"), 0..3, m.discomfort, sev) { onChange(m.copy(discomfort = it)) }
    ScaleSelector(t("overall_comfort"), 1..5, m.comfort, (1..5).map { t("comfort_$it") }) { onChange(m.copy(comfort = it)) }
    ScaleSelector(t("energy_after"), 1..5, m.energyAfter, (1..5).map { t("energy_$it") }) { onChange(m.copy(energyAfter = it)) }
}

@Composable
fun PostMealScreen(vm: AppViewModel, nav: NavHostController, onBack: () -> Unit) {
    val meals by vm.meals.collectAsState()
    val settings by vm.settings.collectAsState()
    val s = settings ?: return
    val zone = vm.zone
    val cutoff = System.currentTimeMillis() - 3L * 24 * 3_600_000
    val pending = meals.filter { !it.skipped && !it.responseRecorded && it.timestamp >= cutoff }
    val recent = meals.filter { !it.skipped && it.responseRecorded }.take(15)
    var editing by remember { mutableLongStateOf(-1L) }
    var draft by remember { mutableStateOf<MealEntry?>(null) }

    val today = LocalDate.now(zone)
    val start = today.minusDays(6).atStartOfDay(zone).toInstant().toEpochMilli()
    val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val st = remember(meals, s) { ObservationEngine.stats(meals, start, end, s.yamaHours, s.includeOvernight, zone) }

    AharaScaffold(title = t("postmeal_title"), onBack = onBack) {
        SectionCard(title = t("m3_sa"), subtitle = t("m3_name")) {
            Text(t("m3_desc"), style = MaterialTheme.typography.bodyMedium)
        }
        SectionCard(title = t("symptom_freq_7d"), subtitle = tf("responses_recorded_n", st.responsesRecorded.toString())) {
            val keys = st.symptomCounts.keys.toList()
            BarChart(
                values = keys.map { (st.symptomCounts[it] ?: 0).toFloat() },
                labels = keys.map { t("sym_short_$it") },
                color = AharaColors.Clay
            )
            if (st.discomfortAfterShortPct != null && st.discomfortAfterOtherPct != null) {
                Text(tf("obs_discomfort_compare", st.discomfortAfterShortPct.toString(), st.discomfortAfterOtherPct.toString()), style = MaterialTheme.typography.bodyMedium)
            }
        }
        Text(t("pending_responses"), style = MaterialTheme.typography.titleMedium, color = AharaColors.Olive)
        if (pending.isEmpty()) Text(t("no_pending"), color = AharaColors.Muted)
        pending.forEach { meal -> ResponseEditor(meal, zone, editing == meal.id, draft, { editing = meal.id; draft = meal }, { draft = it }, {
            draft?.let { d -> vm.saveMeal(d.copy(responseRecorded = true)) }
            editing = -1L; draft = null
        }) }
        Text(t("recent_responses"), style = MaterialTheme.typography.titleMedium, color = AharaColors.Olive)
        recent.forEach { meal -> ResponseEditor(meal, zone, editing == meal.id, draft, { editing = meal.id; draft = meal }, { draft = it }, {
            draft?.let { d -> vm.saveMeal(d.copy(responseRecorded = true)) }
            editing = -1L; draft = null
        }) }
        NoteBox(t("disclaimer"))
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ResponseEditor(
    meal: MealEntry,
    zone: ZoneId,
    open: Boolean,
    draft: MealEntry?,
    onOpen: () -> Unit,
    onDraft: (MealEntry) -> Unit,
    onSave: () -> Unit
) {
    SectionCard(onClick = if (open) null else onOpen) {
        val time = Instant.ofEpochMilli(meal.timestamp).atZone(zone)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(time.format(DateTimeFormatter.ofPattern("d MMM, HH:mm")) + " · " + t("meal_${meal.mealType}"), style = MaterialTheme.typography.titleSmall)
                if (meal.foodName.isNotBlank()) Text(meal.foodName, style = MaterialTheme.typography.bodySmall, color = AharaColors.Muted)
            }
            if (!open) Text(if (meal.responseRecorded) t("edit") else t("record"), color = AharaColors.SageDark, modifier = Modifier.clickable { onOpen() })
        }
        if (open && draft != null) {
            HorizontalDivider()
            PostMealFields(draft, onDraft)
            Button(onClick = onSave, colors = ButtonDefaults.buttonColors(containerColor = AharaColors.SageDark)) { Text(t("save")) }
        } else if (meal.responseRecorded) {
            Text(
                tf("response_summary", meal.comfort.toString(), meal.energyAfter.toString()),
                style = MaterialTheme.typography.bodySmall, color = AharaColors.Muted
            )
        }
    }
}

/** Searchable picker of common Indian foods; tapping a food adds/removes it from the food name and adds its ingredients. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IndianFoodPicker(m: MealEntry, history: List<MealEntry>, onChange: (MealEntry) -> Unit) {
    val lang = LocalLang.current
    var open by remember { mutableStateOf(m.foodName.isBlank()) }
    var query by remember { mutableStateOf("") }
    var category by remember(m.mealType) { mutableStateOf(IndianFoods.suggestedCategory(m.mealType)) }
    val selected = m.foodName.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    fun ingredientsFor(name: String): String =
        IndianFoods.all.firstOrNull { it.en.equals(name, true) || it.hi == name }?.ingredients ?: ""

    fun toggle(name: String) {
        if (name in selected) {
            onChange(m.copy(foodName = (selected - name).joinToString(", ")))
        } else {
            val existing = m.ingredients.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val added = ingredientsFor(name).split(",").map { it.trim() }.filter { it.isNotEmpty() && it !in existing }
            onChange(m.copy(foodName = (selected + name).joinToString(", "), ingredients = (existing + added).joinToString(", ")))
        }
    }

    val frequent = remember(history) {
        history.flatMap { it.foodName.split(",") }.map { it.trim() }.filter { it.isNotEmpty() }
            .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(12).map { it.key }
    }

    @Composable
    fun FoodChip(name: String) {
        FilterChip(
            selected = name in selected,
            onClick = { toggle(name) },
            label = { Text(name) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AharaColors.SageLight, selectedLabelColor = AharaColors.Ink)
        )
    }

    OutlinedButton(onClick = { open = !open }, modifier = Modifier.fillMaxWidth()) {
        Text(if (open) t("hide_food_picker") else t("pick_indian_foods"))
    }
    if (!open) return

    OutlinedTextField(
        query, { query = it }, label = { Text(t("search_foods")) },
        singleLine = true, modifier = Modifier.fillMaxWidth()
    )
    if (query.isNotBlank()) {
        val q = query.trim()
        val matches = IndianFoods.all.filter { it.en.contains(q, true) || it.hi.contains(q) }.take(40)
        if (matches.isEmpty()) Text(t("no_food_match"), style = MaterialTheme.typography.bodySmall, color = AharaColors.Muted)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            matches.forEach { FoodChip(IndianFoods.name(it, lang)) }
        }
    } else {
        if (frequent.isNotEmpty()) {
            Text(t("frequent_foods"), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { frequent.forEach { FoodChip(it) } }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IndianFoods.categories.forEach { c ->
                FilterChip(
                    selected = c == category,
                    onClick = { category = c },
                    label = { Text(t("fcat_$c")) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AharaColors.Beige)
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IndianFoods.all.filter { it.category == category }.forEach { FoodChip(IndianFoods.name(it, lang)) }
        }
    }
    Text(t("food_picker_hint"), style = MaterialTheme.typography.bodySmall, color = AharaColors.Muted)
}
