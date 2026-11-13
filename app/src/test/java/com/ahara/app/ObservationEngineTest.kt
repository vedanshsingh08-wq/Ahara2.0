package com.ahara.app

import com.ahara.app.data.MealEntry
import com.ahara.app.data.MealType
import com.ahara.app.engine.ObservationEngine
import com.ahara.app.engine.ObservationEngine.IntervalClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class ObservationEngineTest {
    private val zone: ZoneId = ZoneId.of("Asia/Kolkata")

    private fun at(day: Int, h: Int, m: Int = 0): Long =
        LocalDateTime.of(2026, 1, day, h, m).atZone(zone).toInstant().toEpochMilli()

    private fun meal(id: Long, ts: Long, type: MealType = MealType.LUNCH, skipped: Boolean = false, hunger: Int = 2) =
        MealEntry(id = id, timestamp = ts, mealType = type.name, skipped = skipped, hungerBefore = hunger)

    @Test
    fun classifiesAgainstYama() {
        assertEquals(IntervalClass.SHORT, ObservationEngine.classify(2.0, 3f))
        assertEquals(IntervalClass.NORMAL, ObservationEngine.classify(3.0, 3f))
        assertEquals(IntervalClass.NORMAL, ObservationEngine.classify(6.0, 3f))
        assertEquals(IntervalClass.LONG, ObservationEngine.classify(6.5, 3f))
    }

    @Test
    fun excludesOvernightAndSkipped() {
        val meals = listOf(
            meal(1, at(1, 8)),
            meal(2, at(1, 10), skipped = true),
            meal(3, at(1, 13)),
            meal(4, at(1, 21)),
            meal(5, at(2, 8))
        )
        val ivs = ObservationEngine.intervals(meals, 3f, includeOvernight = false, zone = zone)
        assertEquals(2, ivs.size)
        assertEquals(5.0, ivs[0].hours, 0.001)
        assertEquals(IntervalClass.LONG, ivs[1].cls)

        val withNight = ObservationEngine.intervals(meals, 3f, includeOvernight = true, zone = zone)
        assertEquals(3, withNight.size)
    }

    @Test
    fun statsAndObservations() {
        val meals = listOf(
            meal(1, at(3, 8), MealType.BREAKFAST),
            meal(2, at(3, 9, 30), MealType.SNACK, hunger = 0),
            meal(3, at(3, 17), MealType.LUNCH)
        )
        val s = ObservationEngine.stats(meals, at(3, 0), at(4, 0), 3f, false, zone)
        assertEquals(3, s.mealCount)
        assertEquals(1, s.shortCount)
        assertEquals(1, s.longCount)
        assertEquals(1, s.withoutHungerCount)
        assertNotNull(s.avgIntervalHours)
        val keys = ObservationEngine.observations(s, 3f).map { it.key }
        assertTrue("obs_short" in keys)
        assertTrue("obs_long" in keys)
        assertTrue("obs_no_hunger" in keys)
    }

    @Test
    fun formatsNumbers() {
        assertEquals("3", ObservationEngine.fmt(3.0))
        assertEquals("4.5", ObservationEngine.fmt(4.46))
    }
}
