package com.ahara.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ahara_settings")

data class Settings(
    val language: String = "en",
    val languageChosen: Boolean = false,
    val onboardingDone: Boolean = false,
    val privacyAccepted: Boolean = false,
    /** Hours per Yama used as observational benchmark (conventional reference: 3 h). */
    val yamaHours: Float = 3f,
    /** Whether overnight gaps (last meal of a day to first meal of next day) count as intervals. */
    val includeOvernight: Boolean = false,
    val mealRemindersOn: Boolean = false,
    /** Remind when no meal has been recorded for this many hours (during waking hours). */
    val mealReminderHours: Int = 6,
    val dailySummaryOn: Boolean = false,
    val dailySummaryHour: Int = 21,
    val weeklyReportOn: Boolean = false,
    val researchConsent: Boolean = false,
    val consentTime: Long = 0L,
    val participantId: String = ""
)

class Prefs(private val context: Context) {
    private object K {
        val language = stringPreferencesKey("language")
        val languageChosen = booleanPreferencesKey("language_chosen")
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val privacyAccepted = booleanPreferencesKey("privacy_accepted")
        val yamaHours = floatPreferencesKey("yama_hours")
        val includeOvernight = booleanPreferencesKey("include_overnight")
        val mealRemindersOn = booleanPreferencesKey("meal_reminders_on")
        val mealReminderHours = intPreferencesKey("meal_reminder_hours")
        val dailySummaryOn = booleanPreferencesKey("daily_summary_on")
        val dailySummaryHour = intPreferencesKey("daily_summary_hour")
        val weeklyReportOn = booleanPreferencesKey("weekly_report_on")
        val researchConsent = booleanPreferencesKey("research_consent")
        val consentTime = longPreferencesKey("consent_time")
        val participantId = stringPreferencesKey("participant_id")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            language = p[K.language] ?: "en",
            languageChosen = p[K.languageChosen] ?: false,
            onboardingDone = p[K.onboardingDone] ?: false,
            privacyAccepted = p[K.privacyAccepted] ?: false,
            yamaHours = p[K.yamaHours] ?: 3f,
            includeOvernight = p[K.includeOvernight] ?: false,
            mealRemindersOn = p[K.mealRemindersOn] ?: false,
            mealReminderHours = p[K.mealReminderHours] ?: 6,
            dailySummaryOn = p[K.dailySummaryOn] ?: false,
            dailySummaryHour = p[K.dailySummaryHour] ?: 21,
            weeklyReportOn = p[K.weeklyReportOn] ?: false,
            researchConsent = p[K.researchConsent] ?: false,
            consentTime = p[K.consentTime] ?: 0L,
            participantId = p[K.participantId] ?: ""
        )
    }

    suspend fun current(): Settings = settings.first()

    suspend fun setLanguage(code: String) = context.dataStore.edit {
        it[K.language] = code
        it[K.languageChosen] = true
    }

    suspend fun setOnboardingDone() = context.dataStore.edit { it[K.onboardingDone] = true }
    suspend fun setPrivacyAccepted(v: Boolean) = context.dataStore.edit { it[K.privacyAccepted] = v }
    suspend fun setYamaHours(v: Float) = context.dataStore.edit { it[K.yamaHours] = v }
    suspend fun setIncludeOvernight(v: Boolean) = context.dataStore.edit { it[K.includeOvernight] = v }
    suspend fun setMealReminders(on: Boolean, hours: Int) = context.dataStore.edit {
        it[K.mealRemindersOn] = on
        it[K.mealReminderHours] = hours
    }
    suspend fun setDailySummary(on: Boolean, hour: Int) = context.dataStore.edit {
        it[K.dailySummaryOn] = on
        it[K.dailySummaryHour] = hour
    }
    suspend fun setWeeklyReport(on: Boolean) = context.dataStore.edit { it[K.weeklyReportOn] = on }

    suspend fun setResearchConsent(on: Boolean) = context.dataStore.edit {
        it[K.researchConsent] = on
        it[K.consentTime] = if (on) System.currentTimeMillis() else 0L
        if (on && (it[K.participantId] ?: "").isEmpty()) it[K.participantId] = newParticipantId()
    }

    suspend fun clearAll() = context.dataStore.edit { it.clear() }

    private fun newParticipantId(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return "AH-" + (1..6).map { chars.random() }.joinToString("")
    }
}
