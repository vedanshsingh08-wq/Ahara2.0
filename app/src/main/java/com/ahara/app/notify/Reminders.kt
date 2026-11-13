package com.ahara.app.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ahara.app.MainActivity
import com.ahara.app.R
import com.ahara.app.data.AharaDatabase
import com.ahara.app.data.Prefs
import com.ahara.app.engine.ObservationEngine
import com.ahara.app.i18n.Strings
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

object Reminders {
    const val CHANNEL = "ahara_reminders"
    private const val MEAL = "ahara_meal_check"
    private const val DAILY = "ahara_daily_summary"
    private const val WEEKLY = "ahara_weekly_report"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "AHARA reminders", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }

    /** (Re)schedule or cancel all periodic work according to current settings. */
    suspend fun apply(context: Context) {
        val s = Prefs(context).current()
        val wm = WorkManager.getInstance(context)

        if (s.mealRemindersOn) {
            val req = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.HOURS)
                .setInputData(workDataOf("kind" to "meal"))
                .build()
            wm.enqueueUniquePeriodicWork(MEAL, ExistingPeriodicWorkPolicy.UPDATE, req)
        } else wm.cancelUniqueWork(MEAL)

        if (s.dailySummaryOn) {
            val now = LocalDateTime.now()
            var next = now.toLocalDate().atTime(s.dailySummaryHour, 0)
            if (!next.isAfter(now)) next = next.plusDays(1)
            val req = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(Duration.between(now, next).toMinutes(), TimeUnit.MINUTES)
                .setInputData(workDataOf("kind" to "daily"))
                .build()
            wm.enqueueUniquePeriodicWork(DAILY, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, req)
        } else wm.cancelUniqueWork(DAILY)

        if (s.weeklyReportOn) {
            val now = LocalDateTime.now()
            var next = now.toLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).atTime(19, 0)
            if (!next.isAfter(now)) next = next.plusWeeks(1)
            val req = PeriodicWorkRequestBuilder<ReminderWorker>(7, TimeUnit.DAYS)
                .setInitialDelay(Duration.between(now, next).toMinutes(), TimeUnit.MINUTES)
                .setInputData(workDataOf("kind" to "weekly"))
                .build()
            wm.enqueueUniquePeriodicWork(WEEKLY, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, req)
        } else wm.cancelUniqueWork(WEEKLY)
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun notify(context: Context, id: Int, title: String, text: String) {
        if (!canNotify(context)) return
        val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pi = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, n)
        } catch (_: SecurityException) {
        }
    }
}

class ReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val prefs = Prefs(applicationContext)
        val s = prefs.current()
        val lang = s.language
        val dao = AharaDatabase.get(applicationContext).dao()
        val meals = dao.allMeals()
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()

        when (inputData.getString("kind")) {
            "meal" -> {
                val hour = LocalDateTime.now().hour
                if (hour !in 8..21) return Result.success()
                val last = meals.filter { !it.skipped }.maxByOrNull { it.timestamp }
                val sp = applicationContext.getSharedPreferences("ahara_notify", Context.MODE_PRIVATE)
                val lastKey = last?.timestamp ?: 0L
                val hoursSince = if (last == null) Double.MAX_VALUE else (now - last.timestamp) / 3_600_000.0
                if (hoursSince >= s.mealReminderHours && sp.getLong("notified_for", -1L) != lastKey) {
                    val text = if (last == null) Strings.t(lang, "notif_meal_none")
                    else Strings.f(lang, "notif_meal_text", ObservationEngine.fmt(hoursSince))
                    Reminders.notify(applicationContext, 101, Strings.t(lang, "notif_meal_title"), text)
                    sp.edit().putLong("notified_for", lastKey).apply()
                }
            }
            "daily" -> {
                val today = LocalDate.now(zone)
                val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val st = ObservationEngine.stats(meals, start, end, s.yamaHours, s.includeOvernight, zone)
                val text = Strings.f(
                    lang, "notif_daily_text",
                    st.mealCount.toString(),
                    st.avgIntervalHours?.let { ObservationEngine.fmt(it) } ?: "—",
                    st.shortCount.toString(),
                    st.longCount.toString()
                )
                Reminders.notify(applicationContext, 102, Strings.t(lang, "notif_daily_title"), text)
            }
            "weekly" -> {
                val today = LocalDate.now(zone)
                val start = today.minusDays(6).atStartOfDay(zone).toInstant().toEpochMilli()
                val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val st = ObservationEngine.stats(meals, start, end, s.yamaHours, s.includeOvernight, zone)
                val text = Strings.f(
                    lang, "notif_weekly_text",
                    st.mealCount.toString(),
                    st.avgIntervalHours?.let { ObservationEngine.fmt(it) } ?: "—",
                    st.discomfortPct?.toString() ?: "—"
                )
                Reminders.notify(applicationContext, 103, Strings.t(lang, "notif_weekly_title"), text)
            }
        }
        return Result.success()
    }
}
