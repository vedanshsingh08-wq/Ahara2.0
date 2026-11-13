package com.ahara.app.engine

import com.ahara.app.data.EnergyLog
import com.ahara.app.data.MealEntry
import com.ahara.app.data.MealType
import com.ahara.app.data.hasDiscomfort
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Shloka-based observation engine.
 *
 *   याममध्ये न भोक्तव्यं यामयुग्मं न लंघयेत् ।
 *   याममध्ये रसोद्वेगो युग्मातीते बलक्षयः ॥
 *
 * Interpretive assumptions (documented, configurable):
 *  - 1 Yama = [yamaHours] hours (conventional reference 3 h).
 *  - An interval shorter than 1 Yama is "within a Yama" (Module 1, याममध्ये).
 *  - An interval longer than 2 Yamas is "beyond a pair of Yamas" (Module 2, यामयुग्म-लंघन).
 *  - Overnight gaps (between meals on different calendar days) are excluded unless enabled,
 *    because the night fast is not what the verse addresses in common interpretation.
 *  - Skipped meals are not eaten, so they do not start or end an interval; they are counted separately.
 *
 * The engine only describes self-reported patterns. It never labels a person healthy, unhealthy or diseased.
 */
object ObservationEngine {

    enum class IntervalClass { SHORT, NORMAL, LONG }

    data class MealInterval(
        val from: MealEntry,
        val to: MealEntry,
        val hours: Double,
        val cls: IntervalClass
    )

    /** A localisable observation. [key] maps to a template in the string table; [args] fill {0}, {1}... */
    data class Observation(val module: Int, val key: String, val args: List<String> = emptyList())

    data class Stats(
        val mealCount: Int,
        val skippedCount: Int,
        val intervals: List<MealInterval>,
        val avgIntervalHours: Double?,
        val shortCount: Int,
        val normalCount: Int,
        val longCount: Int,
        val withoutHungerCount: Int,
        val irregularCount: Int,
        val regularityPct: Int?,
        val responsesRecorded: Int,
        val discomfortCount: Int,
        val discomfortPct: Int?,
        val discomfortAfterShortPct: Int?,
        val discomfortAfterOtherPct: Int?,
        val avgEnergyBefore: Double?,
        val avgEnergyAfter: Double?,
        val avgEnergyAfterLongGap: Double?,
        val avgEnergyOtherwise: Double?,
        val fatigueReports: Int,
        val weaknessReports: Int,
        val hungerDistribution: List<Int>,
        val symptomCounts: Map<String, Int>
    )

    data class DayPoint(
        val date: LocalDate,
        val mealCount: Int,
        val avgIntervalHours: Double?,
        val discomfortCount: Int,
        val avgEnergy: Double?
    )

    fun dateOf(ts: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(ts).atZone(zone).toLocalDate()

    fun minuteOfDay(ts: Long, zone: ZoneId): Int {
        val t = Instant.ofEpochMilli(ts).atZone(zone).toLocalTime()
        return t.hour * 60 + t.minute
    }

    fun classify(hours: Double, yamaHours: Float): IntervalClass = when {
        hours < yamaHours -> IntervalClass.SHORT
        hours > 2.0 * yamaHours -> IntervalClass.LONG
        else -> IntervalClass.NORMAL
    }

    /** All intervals between consecutive eaten meals, oldest first. */
    fun intervals(
        meals: List<MealEntry>,
        yamaHours: Float,
        includeOvernight: Boolean,
        zone: ZoneId
    ): List<MealInterval> {
        val eaten = meals.filter { !it.skipped }.sortedBy { it.timestamp }
        val out = ArrayList<MealInterval>()
        for (i in 1 until eaten.size) {
            val a = eaten[i - 1]
            val b = eaten[i]
            val overnight = dateOf(a.timestamp, zone) != dateOf(b.timestamp, zone)
            if (overnight && !includeOvernight) continue
            val hours = (b.timestamp - a.timestamp) / 3_600_000.0
            if (hours <= 0.0) continue
            out += MealInterval(a, b, hours, classify(hours, yamaHours))
        }
        return out
    }

    /** Median minute-of-day for each main meal type, from a baseline history. */
    fun typicalTimes(baseline: List<MealEntry>, zone: ZoneId): Map<String, Int> =
        baseline.filter { !it.skipped && it.mealType != MealType.SNACK.name }
            .groupBy { it.mealType }
            .filterValues { it.size >= 2 }
            .mapValues { (_, list) ->
                val mins = list.map { minuteOfDay(it.timestamp, zone) }.sorted()
                mins[mins.size / 2]
            }

    /** True when a main meal deviates more than [toleranceMin] minutes from its typical time. */
    fun isIrregular(meal: MealEntry, typical: Map<String, Int>, zone: ZoneId, toleranceMin: Int = 60): Boolean {
        if (meal.skipped || meal.mealType == MealType.SNACK.name) return false
        val ref = typical[meal.mealType] ?: return false
        val d = abs(minuteOfDay(meal.timestamp, zone) - ref)
        return minOf(d, 1440 - d) > toleranceMin
    }

    private fun pct(part: Int, whole: Int): Int? = if (whole == 0) null else (part * 100.0 / whole).roundToInt()

    /**
     * Statistics for meals whose timestamp is in [start, end).
     * [allMeals] is the whole history: used for intervals crossing the period start and typical meal times.
     */
    fun stats(
        allMeals: List<MealEntry>,
        start: Long,
        end: Long,
        yamaHours: Float,
        includeOvernight: Boolean,
        zone: ZoneId
    ): Stats {
        val period = allMeals.filter { it.timestamp in start until end }
        val eaten = period.filter { !it.skipped }
        val ivs = intervals(allMeals, yamaHours, includeOvernight, zone).filter { it.to.timestamp in start until end }
        val typical = typicalTimes(allMeals, zone)

        val main = eaten.filter { it.mealType != MealType.SNACK.name }
        val irregular = main.count { isIrregular(it, typical, zone) }

        val responded = eaten.filter { it.responseRecorded }
        val discomfort = responded.count { it.hasDiscomfort }

        val shortTargets = ivs.filter { it.cls == IntervalClass.SHORT }.map { it.to.id }.toSet()
        val longTargets = ivs.filter { it.cls == IntervalClass.LONG }.map { it.to.id }.toSet()
        val afterShort = responded.filter { it.id in shortTargets }
        val afterOther = responded.filter { it.id !in shortTargets }

        val afterLong = eaten.filter { it.id in longTargets }
        val notAfterLong = eaten.filter { it.id !in longTargets }

        val hungerDist = MutableList(5) { 0 }
        eaten.forEach { hungerDist[it.hungerBefore.coerceIn(0, 4)]++ }

        val symptoms = linkedMapOf(
            "heaviness" to responded.count { it.heaviness >= 1 },
            "bloating" to responded.count { it.bloating >= 1 },
            "acidity" to responded.count { it.acidity >= 1 },
            "nausea" to responded.count { it.nausea >= 1 },
            "discomfort" to responded.count { it.discomfort >= 1 }
        )

        return Stats(
            mealCount = eaten.size,
            skippedCount = period.count { it.skipped },
            intervals = ivs,
            avgIntervalHours = if (ivs.isEmpty()) null else ivs.map { it.hours }.average(),
            shortCount = ivs.count { it.cls == IntervalClass.SHORT },
            normalCount = ivs.count { it.cls == IntervalClass.NORMAL },
            longCount = ivs.count { it.cls == IntervalClass.LONG },
            withoutHungerCount = eaten.count { it.hungerBefore == 0 },
            irregularCount = irregular,
            regularityPct = if (main.isEmpty() || typical.isEmpty()) null else pct(main.size - irregular, main.size),
            responsesRecorded = responded.size,
            discomfortCount = discomfort,
            discomfortPct = pct(discomfort, responded.size),
            discomfortAfterShortPct = pct(afterShort.count { it.hasDiscomfort }, afterShort.size),
            discomfortAfterOtherPct = pct(afterOther.count { it.hasDiscomfort }, afterOther.size),
            avgEnergyBefore = eaten.map { it.energyBefore }.averageOrNull(),
            avgEnergyAfter = responded.map { it.energyAfter }.averageOrNull(),
            avgEnergyAfterLongGap = afterLong.map { it.energyBefore }.averageOrNull(),
            avgEnergyOtherwise = notAfterLong.map { it.energyBefore }.averageOrNull(),
            fatigueReports = eaten.count { it.fatigue >= 2 },
            weaknessReports = eaten.count { it.weakness >= 2 },
            hungerDistribution = hungerDist,
            symptomCounts = symptoms
        )
    }

    /** Contextual, non-diagnostic observations grouped by the four shloka modules. */
    fun observations(s: Stats, yamaHours: Float, energyLogs: List<EnergyLog> = emptyList()): List<Observation> {
        val o = ArrayList<Observation>()
        val y = fmt(yamaHours.toDouble())
        val y2 = fmt(yamaHours * 2.0)

        // Module 1 — याममध्ये न भोक्तव्यम्
        if (s.shortCount > 0) o += Observation(1, "obs_short", listOf(s.shortCount.toString(), y))
        if (s.withoutHungerCount > 0) o += Observation(1, "obs_no_hunger", listOf(s.withoutHungerCount.toString()))
        if (s.irregularCount > 0) o += Observation(1, "obs_irregular", listOf(s.irregularCount.toString()))

        // Module 2 — यामयुग्मं न लंघयेत्
        if (s.longCount > 0) o += Observation(2, "obs_long", listOf(s.longCount.toString(), y2))
        if (s.skippedCount > 0) o += Observation(2, "obs_skipped", listOf(s.skippedCount.toString()))

        // Module 3 — याममध्ये रसोद्वेगः
        val a = s.discomfortAfterShortPct
        val b = s.discomfortAfterOtherPct
        if (a != null && b != null) {
            o += Observation(3, "obs_discomfort_compare", listOf("$a", "$b"))
        } else if (s.discomfortPct != null) {
            o += Observation(3, "obs_discomfort_general", listOf("${s.discomfortPct}", "${s.responsesRecorded}"))
        }

        // Module 4 — युग्मातीते बलक्षयः
        val el = s.avgEnergyAfterLongGap
        val eo = s.avgEnergyOtherwise
        if (el != null && eo != null) {
            o += Observation(4, "obs_energy_compare", listOf(fmt(el), fmt(eo)))
        }
        if (s.fatigueReports > 0) o += Observation(4, "obs_fatigue", listOf(s.fatigueReports.toString()))
        val lowCheckins = energyLogs.count { it.energy <= 2 }
        if (lowCheckins > 0) o += Observation(4, "obs_low_checkins", listOf(lowCheckins.toString()))

        if (o.isEmpty()) o += Observation(0, "obs_none")
        return o
    }

    /** Per-day series for charts across [days] ending today (inclusive). */
    fun daySeries(
        allMeals: List<MealEntry>,
        lastDay: LocalDate,
        days: Int,
        yamaHours: Float,
        includeOvernight: Boolean,
        zone: ZoneId
    ): List<DayPoint> {
        val ivs = intervals(allMeals, yamaHours, includeOvernight, zone)
        return (days - 1 downTo 0).map { back ->
            val d = lastDay.minusDays(back.toLong())
            val dayMeals = allMeals.filter { !it.skipped && dateOf(it.timestamp, zone) == d }
            val dayIvs = ivs.filter { dateOf(it.to.timestamp, zone) == d }
            DayPoint(
                date = d,
                mealCount = dayMeals.size,
                avgIntervalHours = dayIvs.map { it.hours }.averageOrNull(),
                discomfortCount = dayMeals.count { it.hasDiscomfort },
                avgEnergy = dayMeals.map { it.energyBefore }.averageOrNull()
            )
        }
    }

    fun fmt(v: Double): String {
        val r = (v * 10).roundToInt() / 10.0
        return if (r == r.toLong().toDouble()) r.toLong().toString() else r.toString()
    }

    private fun List<Int>.averageOrNull(): Double? = if (isEmpty()) null else average()

    @JvmName("averageOrNullDouble")
    private fun List<Double>.averageOrNull(): Double? = if (isEmpty()) null else average()
}
