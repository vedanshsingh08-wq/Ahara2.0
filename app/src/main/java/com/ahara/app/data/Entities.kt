package com.ahara.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Meal types recorded in the diary. Stored as their name. */
enum class MealType { BREAKFAST, LUNCH, DINNER, SNACK }

/** Portion size. */
enum class Quantity { SMALL, MEDIUM, LARGE }

/** Appetite status before eating. */
enum class Appetite { POOR, NORMAL, STRONG, VARIABLE }

/**
 * One meal record.
 * Scales:
 *  hungerBefore 0..4 (0 = no hunger at all, 4 = intense hunger)
 *  energyBefore / energyAfter 1..5
 *  heaviness, bloating, acidity, nausea, discomfort, fatigue, weakness 0..3 (none, mild, moderate, strong)
 *  comfort 1..5 overall post-meal comfort
 */
@Entity(tableName = "meals")
data class MealEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val mealType: String = MealType.LUNCH.name,
    val foodName: String = "",
    val ingredients: String = "",
    val quantity: String = Quantity.MEDIUM.name,
    val hungerBefore: Int = 2,
    val appetite: String = Appetite.NORMAL.name,
    val energyBefore: Int = 3,
    val energyAfter: Int = 3,
    val heaviness: Int = 0,
    val bloating: Int = 0,
    val acidity: Int = 0,
    val nausea: Int = 0,
    val discomfort: Int = 0,
    val comfort: Int = 4,
    val fatigue: Int = 0,
    val weakness: Int = 0,
    val responseRecorded: Boolean = false,
    val skipped: Boolean = false,
    val notes: String = ""
)

/** Sum of digestive symptom intensities (0..15). */
val MealEntry.symptomScore: Int get() = heaviness + bloating + acidity + nausea + discomfort

/** True when any digestive symptom was reported at moderate or higher, or comfort was low. */
val MealEntry.hasDiscomfort: Boolean
    get() = responseRecorded && (heaviness >= 2 || bloating >= 2 || acidity >= 2 || nausea >= 2 || discomfort >= 2 || comfort <= 2)

/** Single-row user profile (id is always 1). */
@Entity(tableName = "profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val displayName: String = "",
    val ageRange: String = "",
    val gender: String = "",
    val activityLevel: String = "",
    val dietPreference: String = "",
    val sleepHours: Float = 7f,
    val lifestyle: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * A Prakriti or Agni assessment.
 * kind = "PRAKRITI" or "AGNI".
 * result = e.g. "VATA", "VATA_PITTA", "TRIDOSHA" / "SAMAGNI", "VISHAMAGNI" ...
 * source = "SELF" or "PRACTITIONER".
 * scores = compact "V:4,P:3,K:1" text of questionnaire scores (empty for manual).
 */
@Entity(tableName = "assessments")
data class Assessment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,
    val result: String,
    val source: String,
    val practitioner: String = "",
    val scores: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/** Per-day hydration and sleep log. date = ISO yyyy-MM-dd. */
@Entity(tableName = "daily_logs")
data class DailyLog(
    @PrimaryKey val date: String,
    val waterGlasses: Int = 0,
    val sleepHours: Float = 0f
)

/** Stand-alone energy / hunger check-in (useful during long meal gaps). */
@Entity(tableName = "energy_logs")
data class EnergyLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val energy: Int = 3,
    val hunger: Int = 2,
    val fatigue: Int = 0,
    val weakness: Int = 0,
    val note: String = ""
)
