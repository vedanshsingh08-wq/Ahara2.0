package com.ahara.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ahara.app.AppViewModel
import com.ahara.app.Routes
import com.ahara.app.data.UserProfile
import com.ahara.app.i18n.Strings
import com.ahara.app.ui.components.AharaScaffold
import com.ahara.app.ui.components.Botanical
import com.ahara.app.ui.components.ChoiceChips
import com.ahara.app.ui.components.ModuleBadge
import com.ahara.app.ui.components.NoteBox
import com.ahara.app.ui.components.SectionCard
import com.ahara.app.ui.components.t
import com.ahara.app.ui.theme.AharaColors
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(vm: AppViewModel, nav: NavHostController) {
    val settings by vm.settings.collectAsState()
    LaunchedEffect(settings != null) {
        val s = settings ?: return@LaunchedEffect
        delay(1200)
        val next = when {
            !s.languageChosen -> Routes.LANGUAGE
            !s.onboardingDone -> Routes.ONBOARDING
            else -> Routes.HOME
        }
        nav.navigate(next) { popUpTo(Routes.SPLASH) { inclusive = true } }
    }
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(AharaColors.SageDark, AharaColors.Sage))),
        contentAlignment = Alignment.Center
    ) {
        Botanical(Modifier.size(260.dp).align(Alignment.BottomEnd).padding(16.dp), color = Color.White.copy(alpha = 0.12f))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Botanical(Modifier.size(90.dp), color = AharaColors.Gold)
            Spacer(Modifier.height(16.dp))
            Text("आहार", color = AharaColors.Ivory, fontSize = 44.sp, fontWeight = FontWeight.SemiBold)
            Text("AHARA", color = AharaColors.Gold, fontSize = 20.sp, letterSpacing = 8.sp)
            Spacer(Modifier.height(12.dp))
            Text(Strings.t("en", "tagline"), color = AharaColors.Ivory.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun LanguageScreen(vm: AppViewModel, nav: NavHostController) {
    val settings by vm.settings.collectAsState()
    val current = settings?.language ?: "en"
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("भाषा · Language · भाषा", style = MaterialTheme.typography.headlineMedium, color = AharaColors.SageDark)
        Spacer(Modifier.height(8.dp))
        Text("Choose your language. You can change it any time in Settings.", style = MaterialTheme.typography.bodyMedium, color = AharaColors.Muted)
        Spacer(Modifier.height(24.dp))
        listOf("en" to "English", "hi" to "हिंदी", "sa" to "संस्कृतम्").forEach { (code, name) ->
            val sel = code == current
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(16.dp))
                    .background(if (sel) AharaColors.SageLight else Color.White)
                    .clickable {
                        vm.setLanguage(code)
                        if (settings?.onboardingDone == true) nav.popBackStack()
                        else nav.navigate(Routes.ONBOARDING) { popUpTo(Routes.LANGUAGE) { inclusive = true } }
                    }
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (sel) Text("✓", color = AharaColors.SageDark, fontSize = 20.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Sanskrit interface text is a best-effort rendering of Ayurvedic terms and should be reviewed by a Sanskrit scholar.",
            style = MaterialTheme.typography.bodySmall, color = AharaColors.Muted
        )
    }
}

@Composable
fun OnboardingScreen(vm: AppViewModel, nav: NavHostController) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    var consent by rememberSaveable { mutableStateOf(false) }
    val pages = 4
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(24.dp))
            when (page) {
                0 -> {
                    Botanical(Modifier.size(110.dp))
                    Text(t("welcome_title"), style = MaterialTheme.typography.headlineLarge, color = AharaColors.SageDark)
                    Text(t("tagline"), style = MaterialTheme.typography.titleMedium, color = AharaColors.Olive)
                    Text(t("welcome_body"), style = MaterialTheme.typography.bodyLarge)
                }
                1 -> {
                    Text(t("shloka_title"), style = MaterialTheme.typography.headlineMedium, color = AharaColors.SageDark)
                    ShlokaBlock()
                    Text(t("shloka_meaning_short"), style = MaterialTheme.typography.bodyLarge)
                }
                2 -> {
                    Text(t("modules_title"), style = MaterialTheme.typography.headlineMedium, color = AharaColors.SageDark)
                    (1..4).forEach { m ->
                        Row(verticalAlignment = Alignment.Top) {
                            ModuleBadge(m)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(t("m${m}_sa"), style = MaterialTheme.typography.titleMedium)
                                Text(t("m${m}_name"), style = MaterialTheme.typography.bodyMedium, color = AharaColors.Muted)
                            }
                        }
                    }
                }
                else -> {
                    Text(t("privacy_title"), style = MaterialTheme.typography.headlineMedium, color = AharaColors.SageDark)
                    Text(t("privacy_short"), style = MaterialTheme.typography.bodyLarge)
                    NoteBox(t("disclaimer"))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { consent = !consent }) {
                        Checkbox(checked = consent, onCheckedChange = { consent = it })
                        Text(t("consent_check"), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(Modifier.weight(1f)) {
                repeat(pages) { i ->
                    Box(
                        Modifier.padding(end = 6.dp).size(if (i == page) 22.dp else 8.dp, 8.dp).clip(CircleShape)
                            .background(if (i == page) AharaColors.SageDark else AharaColors.Beige)
                    )
                }
            }
            if (page > 0) TextButton(onClick = { page-- }) { Text(t("back")) }
            Button(
                enabled = page < pages - 1 || consent,
                onClick = {
                    if (page < pages - 1) page++
                    else {
                        vm.acceptPrivacy(true)
                        nav.navigate("${Routes.PROFILE}?setup=true") { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                    }
                }
            ) { Text(if (page < pages - 1) t("next") else t("get_started")) }
        }
    }
}

@Composable
fun ShlokaBlock() {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFFF6EFD9), Color(0xFFEFE5C8))))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "याममध्ये न भोक्तव्यं यामयुग्मं न लंघयेत् ।\nयाममध्ये रसोद्वेगो युग्मातीते बलक्षयः ॥",
            textAlign = TextAlign.Center, fontSize = 19.sp, lineHeight = 32.sp, color = Color(0xFF5A4A1E), fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(6.dp))
        Text("(क्षे.कु.)", color = AharaColors.Muted, fontSize = 13.sp)
    }
}

@Composable
fun ProfileScreen(vm: AppViewModel, nav: NavHostController, setup: Boolean) {
    val existing by vm.profile.collectAsState()
    var loaded by remember { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var age by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf("") }
    var activity by rememberSaveable { mutableStateOf("") }
    var diet by rememberSaveable { mutableStateOf("") }
    var sleep by rememberSaveable { mutableFloatStateOf(7f) }
    var lifestyle by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(existing) {
        val p = existing
        if (!loaded && p != null) {
            name = p.displayName; age = p.ageRange; gender = p.gender; activity = p.activityLevel
            diet = p.dietPreference; sleep = p.sleepHours; lifestyle = p.lifestyle
            loaded = true
        }
    }

    fun save() {
        vm.saveProfile(
            UserProfile(
                displayName = name.trim(), ageRange = age, gender = gender, activityLevel = activity,
                dietPreference = diet, sleepHours = sleep, lifestyle = lifestyle.trim(),
                createdAt = existing?.createdAt ?: System.currentTimeMillis()
            )
        ) {
            if (setup) {
                vm.finishOnboarding()
                nav.navigate(Routes.HOME) { popUpTo(0) { inclusive = true } }
            } else nav.popBackStack()
        }
    }

    AharaScaffold(title = t("profile_title"), onBack = if (setup) null else ({ nav.popBackStack(); Unit })) {
        if (setup) Text(t("profile_setup_hint"), style = MaterialTheme.typography.bodyMedium, color = AharaColors.Muted)
        SectionCard(title = t("about_you")) {
            OutlinedTextField(name, { name = it }, label = { Text(t("display_name_optional")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text(t("age_range"), style = MaterialTheme.typography.titleSmall)
            ChoiceChips(listOf("<18", "18-25", "26-35", "36-45", "46-60", "60+").map { it to it }, age) { age = it }
            Text(t("gender_optional"), style = MaterialTheme.typography.titleSmall)
            ChoiceChips(listOf("FEMALE", "MALE", "OTHER", "NA").map { it to t("gender_$it") }, gender) { gender = if (gender == it) "" else it }
        }
        SectionCard(title = t("lifestyle")) {
            Text(t("activity_level"), style = MaterialTheme.typography.titleSmall)
            ChoiceChips(listOf("SEDENTARY", "LIGHT", "MODERATE", "ACTIVE").map { it to t("act_$it") }, activity) { activity = it }
            Text(t("diet_pref"), style = MaterialTheme.typography.titleSmall)
            ChoiceChips(listOf("VEG", "LACTO_VEG", "VEGAN", "EGG", "NON_VEG", "SATTVIC").map { it to t("diet_$it") }, diet) { diet = it }
            Text(t("sleep_duration") + ": " + String.format("%.1f", sleep) + " " + t("hours_short"), style = MaterialTheme.typography.titleSmall)
            Slider(value = sleep, onValueChange = { sleep = (it * 2).toInt() / 2f }, valueRange = 3f..12f, steps = 17)
            OutlinedTextField(lifestyle, { lifestyle = it }, label = { Text(t("lifestyle_notes")) }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        }
        SectionCard(title = t("assessments_optional"), subtitle = t("assessments_optional_sub")) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { nav.navigate(Routes.PRAKRITI) }) { Text(t("prakriti")) }
                OutlinedButton(onClick = { nav.navigate(Routes.AGNI) }) { Text(t("agni")) }
            }
        }
        Button(
            onClick = { save() },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AharaColors.SageDark)
        ) { Text(if (setup) t("save_continue") else t("save")) }
        if (setup) TextButton(onClick = { save() }, modifier = Modifier.fillMaxWidth()) { Text(t("skip_for_now")) }
        Spacer(Modifier.height(24.dp))
    }
}
