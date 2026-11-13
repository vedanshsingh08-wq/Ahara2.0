package com.ahara.app.export

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.ahara.app.data.Assessment
import com.ahara.app.data.DailyLog
import com.ahara.app.data.EnergyLog
import com.ahara.app.data.MealEntry
import com.ahara.app.data.UserProfile
import com.ahara.app.engine.ObservationEngine
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class ReportLine(val text: String, val style: Style = Style.BODY) {
    enum class Style { TITLE, HEADING, BODY, SMALL }
}

/** Summary of one participant computed from an anonymised research CSV. */
data class ParticipantSummary(
    val participantId: String,
    val prakriti: String,
    val agni: String,
    val meals: Int,
    val avgInterval: Double?,
    val shortPct: Int?,
    val longPct: Int?,
    val discomfortPct: Int?,
    val avgEnergyBefore: Double?
)

object Exporter {
    private val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun esc(v: Any?): String {
        val s = v?.toString() ?: ""
        return if (s.contains(',') || s.contains('"') || s.contains('\n')) "\"" + s.replace("\"", "\"\"") + "\"" else s
    }

    private fun row(vararg v: Any?): String = v.joinToString(",") { esc(it) }

    fun mealsCsv(meals: List<MealEntry>, zone: ZoneId): String {
        val sb = StringBuilder()
        sb.appendLine(
            row(
                "id", "date_time", "meal_type", "skipped", "food", "ingredients", "quantity", "hunger_before_0_4",
                "appetite", "energy_before_1_5", "energy_after_1_5", "heaviness_0_3", "bloating_0_3", "acidity_0_3",
                "nausea_0_3", "discomfort_0_3", "comfort_1_5", "fatigue_0_3", "weakness_0_3", "response_recorded", "notes"
            )
        )
        meals.sortedBy { it.timestamp }.forEach { m ->
            sb.appendLine(
                row(
                    m.id, dtf.format(Instant.ofEpochMilli(m.timestamp).atZone(zone)), m.mealType, m.skipped, m.foodName,
                    m.ingredients, m.quantity, m.hungerBefore, m.appetite, m.energyBefore, m.energyAfter, m.heaviness,
                    m.bloating, m.acidity, m.nausea, m.discomfort, m.comfort, m.fatigue, m.weakness, m.responseRecorded, m.notes
                )
            )
        }
        return sb.toString()
    }

    fun fullBackupCsv(
        meals: List<MealEntry>,
        profile: UserProfile?,
        assessments: List<Assessment>,
        daily: List<DailyLog>,
        energy: List<EnergyLog>,
        zone: ZoneId
    ): String {
        val sb = StringBuilder()
        sb.appendLine("# AHARA full data export")
        sb.appendLine("# PROFILE")
        sb.appendLine(row("name", "age_range", "gender", "activity", "diet", "sleep_hours", "lifestyle"))
        if (profile != null) sb.appendLine(
            row(profile.displayName, profile.ageRange, profile.gender, profile.activityLevel, profile.dietPreference, profile.sleepHours, profile.lifestyle)
        )
        sb.appendLine("# ASSESSMENTS")
        sb.appendLine(row("date_time", "kind", "result", "source", "practitioner", "scores"))
        assessments.forEach {
            sb.appendLine(row(dtf.format(Instant.ofEpochMilli(it.timestamp).atZone(zone)), it.kind, it.result, it.source, it.practitioner, it.scores))
        }
        sb.appendLine("# DAILY_LOGS")
        sb.appendLine(row("date", "water_glasses", "sleep_hours"))
        daily.forEach { sb.appendLine(row(it.date, it.waterGlasses, it.sleepHours)) }
        sb.appendLine("# ENERGY_CHECKINS")
        sb.appendLine(row("date_time", "energy_1_5", "hunger_0_4", "fatigue_0_3", "weakness_0_3", "note"))
        energy.forEach {
            sb.appendLine(row(dtf.format(Instant.ofEpochMilli(it.timestamp).atZone(zone)), it.energy, it.hunger, it.fatigue, it.weakness, it.note))
        }
        sb.appendLine("# MEALS")
        sb.append(mealsCsv(meals, zone))
        return sb.toString()
    }

    /**
     * Anonymised research dataset: no names, food descriptions or notes; dates replaced with a day index.
     */
    fun anonymisedCsv(
        meals: List<MealEntry>,
        participantId: String,
        profile: UserProfile?,
        prakriti: String,
        agni: String,
        yamaHours: Float,
        includeOvernight: Boolean,
        zone: ZoneId
    ): String {
        val sorted = meals.sortedBy { it.timestamp }
        val first = sorted.firstOrNull()?.let { ObservationEngine.dateOf(it.timestamp, zone) }
        val ivByTarget = ObservationEngine.intervals(sorted, yamaHours, includeOvernight, zone).associateBy { it.to.id }
        val sb = StringBuilder()
        sb.appendLine(
            row(
                "participant_id", "age_range", "gender", "prakriti", "agni", "day_index", "minute_of_day", "meal_type",
                "skipped", "interval_hours", "interval_class", "hunger_before", "appetite", "quantity", "energy_before",
                "energy_after", "heaviness", "bloating", "acidity", "nausea", "discomfort", "comfort", "fatigue",
                "weakness", "response_recorded"
            )
        )
        sorted.forEach { m ->
            val day = if (first == null) 0 else ChronoUnit.DAYS.between(first, ObservationEngine.dateOf(m.timestamp, zone))
            val iv = ivByTarget[m.id]
            sb.appendLine(
                row(
                    participantId, profile?.ageRange ?: "", profile?.gender ?: "", prakriti, agni, day,
                    ObservationEngine.minuteOfDay(m.timestamp, zone), m.mealType, m.skipped,
                    iv?.let { ObservationEngine.fmt(it.hours) } ?: "", iv?.cls?.name ?: "", m.hungerBefore, m.appetite,
                    m.quantity, m.energyBefore, m.energyAfter, m.heaviness, m.bloating, m.acidity, m.nausea,
                    m.discomfort, m.comfort, m.fatigue, m.weakness, m.responseRecorded
                )
            )
        }
        return sb.toString()
    }

    /** Minimal CSV line parser supporting quoted fields. */
    fun parseLine(line: String): List<String> {
        val out = ArrayList<String>()
        val cur = StringBuilder()
        var q = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (q) {
                if (c == '"') {
                    if (i + 1 < line.length && line[i + 1] == '"') { cur.append('"'); i++ } else q = false
                } else cur.append(c)
            } else {
                when (c) {
                    '"' -> q = true
                    ',' -> { out += cur.toString(); cur.clear() }
                    else -> cur.append(c)
                }
            }
            i++
        }
        out += cur.toString()
        return out
    }

    /** Parse one or more anonymised CSVs into per-participant summaries. */
    fun summariseResearchCsv(text: String): List<ParticipantSummary> {
        val lines = text.lines().filter { it.isNotBlank() && !it.startsWith("#") }
        if (lines.size < 2) return emptyList()
        val header = parseLine(lines[0]).map { it.trim() }
        fun idx(n: String) = header.indexOf(n)
        val iP = idx("participant_id"); val iPr = idx("prakriti"); val iAg = idx("agni")
        val iSk = idx("skipped"); val iIv = idx("interval_hours"); val iCl = idx("interval_class")
        val iRr = idx("response_recorded"); val iEb = idx("energy_before")
        val iH = idx("heaviness"); val iB = idx("bloating"); val iA = idx("acidity"); val iN = idx("nausea")
        val iD = idx("discomfort"); val iC = idx("comfort")
        if (iP < 0) return emptyList()
        val rows = lines.drop(1).map { parseLine(it) }.filter { it.size == header.size && it[iP] != "participant_id" }
        return rows.groupBy { it[iP] }.map { (pid, rs) ->
            fun g(r: List<String>, i: Int) = if (i >= 0) r[i] else ""
            val eaten = rs.filter { g(it, iSk) != "true" }
            val ivs = eaten.mapNotNull { g(it, iIv).toDoubleOrNull() }
            val classes = eaten.map { g(it, iCl) }.filter { it.isNotEmpty() }
            val responded = eaten.filter { g(it, iRr) == "true" }
            val disc = responded.count { r ->
                listOf(iH, iB, iA, iN, iD).any { (g(r, it).toIntOrNull() ?: 0) >= 2 } || (g(r, iC).toIntOrNull() ?: 5) <= 2
            }
            val energy = eaten.mapNotNull { g(it, iEb).toDoubleOrNull() }
            ParticipantSummary(
                participantId = pid,
                prakriti = rs.firstOrNull()?.let { g(it, iPr) }.orEmpty().ifEmpty { "—" },
                agni = rs.firstOrNull()?.let { g(it, iAg) }.orEmpty().ifEmpty { "—" },
                meals = eaten.size,
                avgInterval = if (ivs.isEmpty()) null else ivs.average(),
                shortPct = if (classes.isEmpty()) null else classes.count { it == "SHORT" } * 100 / classes.size,
                longPct = if (classes.isEmpty()) null else classes.count { it == "LONG" } * 100 / classes.size,
                discomfortPct = if (responded.isEmpty()) null else disc * 100 / responded.size,
                avgEnergyBefore = if (energy.isEmpty()) null else energy.average()
            )
        }
    }

    fun writeText(context: Context, name: String, content: String): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val f = File(dir, name)
        // UTF-8 BOM so spreadsheet apps show Devanagari correctly
        f.outputStream().use { os ->
            os.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            os.write(content.toByteArray(Charsets.UTF_8))
        }
        return f
    }

    fun writePdf(context: Context, name: String, lines: List<ReportLine>): File {
        val doc = PdfDocument()
        val pageW = 595
        val pageH = 842
        val margin = 40f
        val maxW = pageW - 2 * margin

        val title = Paint().apply { textSize = 20f; isAntiAlias = true; typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD); color = 0xFF4E6B52.toInt() }
        val heading = Paint().apply { textSize = 14f; isAntiAlias = true; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = 0xFF3A3A2E.toInt() }
        val body = Paint().apply { textSize = 11f; isAntiAlias = true; color = 0xFF2B2B24.toInt() }
        val small = Paint().apply { textSize = 9f; isAntiAlias = true; color = 0xFF6B6B5E.toInt() }

        var pageNo = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNo).create())
        var canvas = page.canvas
        var y = margin + 10f

        fun newPage() {
            doc.finishPage(page)
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNo).create())
            canvas = page.canvas
            y = margin + 10f
        }

        for (line in lines) {
            val p = when (line.style) {
                ReportLine.Style.TITLE -> title
                ReportLine.Style.HEADING -> heading
                ReportLine.Style.BODY -> body
                ReportLine.Style.SMALL -> small
            }
            if (line.style == ReportLine.Style.HEADING) y += 8f
            for (wrapped in wrap(line.text, p, maxW)) {
                val h = p.textSize * 1.45f
                if (y + h > pageH - margin) newPage()
                y += h
                canvas.drawText(wrapped, margin, y, p)
            }
        }
        doc.finishPage(page)

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val f = File(dir, name)
        FileOutputStream(f).use { doc.writeTo(it) }
        doc.close()
        return f
    }

    private fun wrap(text: String, p: Paint, maxW: Float): List<String> {
        if (text.isEmpty()) return listOf("")
        val out = ArrayList<String>()
        for (para in text.split('\n')) {
            var cur = ""
            for (word in para.split(' ')) {
                val cand = if (cur.isEmpty()) word else "$cur $word"
                if (p.measureText(cand) <= maxW || cur.isEmpty()) cur = cand
                else { out += cur; cur = word }
            }
            out += cur
        }
        return out
    }

    fun share(context: Context, file: File, mime: String) {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, file.name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
