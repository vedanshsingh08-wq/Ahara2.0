package com.ahara.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ahara.app.AppViewModel
import com.ahara.app.BuildConfig
import com.ahara.app.Routes
import com.ahara.app.engine.ObservationEngine
import com.ahara.app.notify.Reminders
import com.ahara.app.ui.components.AharaScaffold
import com.ahara.app.ui.components.ChoiceChips
import com.ahara.app.ui.components.MenuRow
import com.ahara.app.ui.components.NoteBox
import com.ahara.app.ui.components.SectionCard
import com.ahara.app.ui.components.SwitchRow
import com.ahara.app.ui.components.t
import com.ahara.app.ui.components.tf
import com.ahara.app.ui.theme.AharaColors

@Composable
fun RemindersScreen(vm: AppViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsState()
    val s = settings ?: return
    var permissionGranted by remember { mutableStateOf(Reminders.canNotify(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionGranted = granted
    }
    fun ensurePermission() {
        if (Build.VERSION.SDK_INT >= 33 && !Reminders.canNotify(context)) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    val testTitle = t("notif_test_title")
    val testText = t("notif_test_text")
    AharaScaffold(title = t("reminders_title"), onBack = onBack) {
        NoteBox(t("reminders_note"))
        if (!permissionGranted) {
            SectionCard(title = t("permission_needed")) {
                Text(t("permission_body"), style = MaterialTheme.typography.bodyMedium)
                Button(onClick = { ensurePermission() }, colors = ButtonDefaults.buttonColors(containerColor = AharaColors.SageDark)) { Text(t("allow_notifications")) }
            }
        }
        SectionCard(title = t("meal_diary_reminders")) {
            SwitchRow(t("missing_meal_reminder"), tf("missing_meal_reminder_sub", s.mealReminderHours.toString()), s.mealRemindersOn) {
                if (it) ensurePermission()
                vm.setMealReminders(it, s.mealReminderHours)
            }
            if (s.mealRemindersOn) {
                Text(t("remind_after"), style = MaterialTheme.typography.titleSmall)
                ChoiceChips(listOf(3, 4, 5, 6, 8).map { it.toString() to "$it " + t("hours_short") }, s.mealReminderHours.toString()) {
                    vm.setMealReminders(true, it.toInt())
                }
            }
        }
        SectionCard(title = t("summaries")) {
            SwitchRow(t("daily_summary"), tf("daily_summary_sub", "%02d:00".format(s.dailySummaryHour)), s.dailySummaryOn) {
                if (it) ensurePermission()
                vm.setDailySummary(it, s.dailySummaryHour)
            }
            if (s.dailySummaryOn) {
                ChoiceChips(listOf(19, 20, 21, 22).map { it.toString() to "%02d:00".format(it) }, s.dailySummaryHour.toString()) {
                    vm.setDailySummary(true, it.toInt())
                }
            }
            SwitchRow(t("weekly_report"), t("weekly_report_sub"), s.weeklyReportOn) {
                if (it) ensurePermission()
                vm.setWeeklyReport(it)
            }
        }
        OutlinedButton(onClick = {
            ensurePermission()
            Reminders.notify(context, 199, testTitle, testText)
        }, modifier = Modifier.fillMaxWidth()) { Text(t("send_test")) }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun SettingsScreen(vm: AppViewModel, nav: NavHostController, onBack: () -> Unit) {
    val settings by vm.settings.collectAsState()
    val s = settings ?: return
    var yama by remember(s.yamaHours) { mutableStateOf(s.yamaHours) }
    var showPolicy by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    AharaScaffold(title = t("settings_title"), onBack = onBack) {
        SectionCard(title = t("language")) {
            ChoiceChips(listOf("en" to "English", "hi" to "हिंदी", "sa" to "संस्कृतम्"), s.language) { vm.setLanguage(it) }
        }
        SectionCard(title = t("observation_settings"), subtitle = t("observation_settings_sub")) {
            Text(tf("yama_length", ObservationEngine.fmt(yama.toDouble())), style = MaterialTheme.typography.titleSmall)
            Slider(
                value = yama, onValueChange = { yama = (it * 4).toInt() / 4f },
                onValueChangeFinished = { vm.setYama(yama) },
                valueRange = 2f..4f, steps = 7
            )
            Text(t("yama_length_note"), style = MaterialTheme.typography.bodySmall, color = AharaColors.Muted)
            SwitchRow(t("include_overnight"), t("include_overnight_sub"), s.includeOvernight) { vm.setOvernight(it) }
        }
        MenuRow(Icons.Filled.Person, t("profile_title"), t("profile_sub")) { nav.navigate(Routes.PROFILE) }
        MenuRow(Icons.Filled.Search, t("research_title"), t("research_sub")) { nav.navigate(Routes.RESEARCH) }
        MenuRow(Icons.Filled.Share, t("reports_title"), t("backup_sub")) { nav.navigate(Routes.REPORTS) }

        SectionCard(title = t("privacy_security")) {
            Text(t("privacy_summary"), style = MaterialTheme.typography.bodyMedium)
            Text(t("cloud_sync_note"), style = MaterialTheme.typography.bodySmall, color = AharaColors.Muted)
            MenuRow(Icons.Filled.Lock, t("privacy_policy")) { showPolicy = true }
            OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Text(t("delete_all_data"), color = MaterialTheme.colorScheme.error)
            }
        }
        SectionCard(title = t("about")) {
            Text("AHARA · आहार  v" + BuildConfig.VERSION_NAME, style = MaterialTheme.typography.titleSmall)
            Text(t("tagline"), style = MaterialTheme.typography.bodySmall, color = AharaColors.Muted)
            Text(t("disclaimer"), style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showPolicy) {
        AlertDialog(
            onDismissRequest = { showPolicy = false },
            title = { Text(t("privacy_policy")) },
            text = {
                Text(
                    t("privacy_policy_full"),
                    modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = { TextButton(onClick = { showPolicy = false }) { Text(t("close")) } }
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(t("delete_all_q")) },
            text = { Text(t("delete_all_body")) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.deleteAllData { nav.navigate(Routes.SPLASH) { popUpTo(0) { inclusive = true } } }
                }) { Text(t("delete"), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(t("cancel")) } }
        )
    }
}
