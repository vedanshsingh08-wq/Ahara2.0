package com.ahara.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ahara.app.data.AharaDatabase
import com.ahara.app.data.Assessment
import com.ahara.app.data.DailyLog
import com.ahara.app.data.EnergyLog
import com.ahara.app.data.MealEntry
import com.ahara.app.data.Prefs
import com.ahara.app.data.Settings
import com.ahara.app.data.UserProfile
import com.ahara.app.export.Exporter
import com.ahara.app.export.ParticipantSummary
import com.ahara.app.export.ReportLine
import com.ahara.app.notify.Reminders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx get() = getApplication<Application>()
    private val dao = AharaDatabase.get(app).dao()
    val prefs = Prefs(app)
    val zone: ZoneId = ZoneId.systemDefault()

    val settings: StateFlow<Settings?> = prefs.settings
        .map<Settings, Settings?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val meals: StateFlow<List<MealEntry>> = dao.observeMeals().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val profile: StateFlow<UserProfile?> = dao.observeProfile().stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val assessments: StateFlow<List<Assessment>> = dao.observeAssessments().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val dailyLogs: StateFlow<List<DailyLog>> = dao.observeDailyLogs().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val energyLogs: StateFlow<List<EnergyLog>> = dao.observeEnergyLogs().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Imported anonymised participant summaries for the research dashboard (kept in memory only). */
    val imported = MutableStateFlow<List<ParticipantSummary>>(emptyList())

    // ---- Meals
    fun saveMeal(m: MealEntry, done: () -> Unit = {}) = viewModelScope.launch {
        dao.insertMeal(m); done()
    }
    fun deleteMeal(m: MealEntry) = viewModelScope.launch { dao.deleteMeal(m) }
    suspend fun meal(id: Long): MealEntry? = dao.meal(id)

    // ---- Profile & assessments
    fun saveProfile(p: UserProfile, done: () -> Unit = {}) = viewModelScope.launch { dao.saveProfile(p); done() }
    fun addAssessment(a: Assessment) = viewModelScope.launch { dao.insertAssessment(a) }
    fun deleteAssessment(a: Assessment) = viewModelScope.launch { dao.deleteAssessment(a) }

    fun latest(kind: String): Assessment? = assessments.value.firstOrNull { it.kind == kind }

    // ---- Daily logs
    fun today(): String = LocalDate.now(zone).toString()

    fun changeWater(delta: Int) = viewModelScope.launch {
        val d = today()
        val cur = dao.dailyLog(d) ?: DailyLog(d)
        dao.saveDailyLog(cur.copy(waterGlasses = (cur.waterGlasses + delta).coerceIn(0, 30)))
    }

    fun setSleep(hours: Float) = viewModelScope.launch {
        val d = today()
        val cur = dao.dailyLog(d) ?: DailyLog(d)
        dao.saveDailyLog(cur.copy(sleepHours = hours.coerceIn(0f, 16f)))
    }

    // ---- Energy check-ins
    fun addEnergyLog(e: EnergyLog) = viewModelScope.launch { dao.insertEnergyLog(e) }
    fun deleteEnergyLog(e: EnergyLog) = viewModelScope.launch { dao.deleteEnergyLog(e) }

    // ---- Settings
    fun setLanguage(code: String) = viewModelScope.launch { prefs.setLanguage(code) }
    fun finishOnboarding() = viewModelScope.launch { prefs.setOnboardingDone() }
    fun acceptPrivacy(v: Boolean) = viewModelScope.launch { prefs.setPrivacyAccepted(v) }
    fun setYama(v: Float) = viewModelScope.launch { prefs.setYamaHours(v) }
    fun setOvernight(v: Boolean) = viewModelScope.launch { prefs.setIncludeOvernight(v) }
    fun setResearchConsent(v: Boolean) = viewModelScope.launch { prefs.setResearchConsent(v) }

    fun setMealReminders(on: Boolean, hours: Int) = viewModelScope.launch {
        prefs.setMealReminders(on, hours); Reminders.apply(ctx)
    }
    fun setDailySummary(on: Boolean, hour: Int) = viewModelScope.launch {
        prefs.setDailySummary(on, hour); Reminders.apply(ctx)
    }
    fun setWeeklyReport(on: Boolean) = viewModelScope.launch {
        prefs.setWeeklyReport(on); Reminders.apply(ctx)
    }

    fun deleteAllData(done: () -> Unit) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            dao.clearMeals(); dao.clearProfile(); dao.clearAssessments(); dao.clearDailyLogs(); dao.clearEnergyLogs()
            prefs.clearAll()
            Reminders.apply(ctx)
            File(ctx.cacheDir, "exports").deleteRecursively()
        }
        done()
    }

    // ---- Exports
    fun shareMealsCsv() = viewModelScope.launch {
        val f = withContext(Dispatchers.IO) {
            Exporter.writeText(ctx, "ahara_meals_${today()}.csv", Exporter.mealsCsv(dao.allMeals(), zone))
        }
        Exporter.share(ctx, f, "text/csv")
    }

    fun shareFullCsv() = viewModelScope.launch {
        val f = withContext(Dispatchers.IO) {
            Exporter.writeText(
                ctx, "ahara_full_export_${today()}.csv",
                Exporter.fullBackupCsv(
                    dao.allMeals(), dao.profile(), dao.allAssessments(),
                    dailyLogs.value, dao.allEnergyLogs(), zone
                )
            )
        }
        Exporter.share(ctx, f, "text/csv")
    }

    fun shareAnonymisedCsv() = viewModelScope.launch {
        val s = prefs.current()
        val f = withContext(Dispatchers.IO) {
            Exporter.writeText(
                ctx, "ahara_research_${s.participantId}_${today()}.csv",
                Exporter.anonymisedCsv(
                    dao.allMeals(), s.participantId, dao.profile(),
                    latest("PRAKRITI")?.result ?: "", latest("AGNI")?.result ?: "",
                    s.yamaHours, s.includeOvernight, zone
                )
            )
        }
        Exporter.share(ctx, f, "text/csv")
    }

    fun sharePdf(name: String, lines: List<ReportLine>) = viewModelScope.launch {
        val f = withContext(Dispatchers.IO) { Exporter.writePdf(ctx, name, lines) }
        Exporter.share(ctx, f, "application/pdf")
    }

    fun importResearchCsv(uris: List<Uri>) = viewModelScope.launch {
        val result = withContext(Dispatchers.IO) {
            uris.flatMap { uri ->
                val text = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: ""
                Exporter.summariseResearchCsv(text.removePrefix("﻿"))
            }
        }
        // replace summaries with the same participant id
        val ids = result.map { it.participantId }.toSet()
        imported.value = imported.value.filter { it.participantId !in ids } + result
    }

    fun clearImported() { imported.value = emptyList() }
}
