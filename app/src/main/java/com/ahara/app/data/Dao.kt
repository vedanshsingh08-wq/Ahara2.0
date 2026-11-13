package com.ahara.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AharaDao {
    // Meals
    @Query("SELECT * FROM meals ORDER BY timestamp DESC")
    fun observeMeals(): Flow<List<MealEntry>>

    @Query("SELECT * FROM meals ORDER BY timestamp DESC")
    suspend fun allMeals(): List<MealEntry>

    @Query("SELECT * FROM meals WHERE id = :id")
    suspend fun meal(id: Long): MealEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealEntry): Long

    @Delete
    suspend fun deleteMeal(meal: MealEntry)

    // Profile
    @Query("SELECT * FROM profile WHERE id = 1")
    fun observeProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM profile WHERE id = 1")
    suspend fun profile(): UserProfile?

    @Upsert
    suspend fun saveProfile(profile: UserProfile)

    // Assessments
    @Query("SELECT * FROM assessments ORDER BY timestamp DESC")
    fun observeAssessments(): Flow<List<Assessment>>

    @Query("SELECT * FROM assessments ORDER BY timestamp DESC")
    suspend fun allAssessments(): List<Assessment>

    @Insert
    suspend fun insertAssessment(a: Assessment): Long

    @Delete
    suspend fun deleteAssessment(a: Assessment)

    // Daily logs
    @Query("SELECT * FROM daily_logs ORDER BY date DESC")
    fun observeDailyLogs(): Flow<List<DailyLog>>

    @Query("SELECT * FROM daily_logs WHERE date = :date")
    suspend fun dailyLog(date: String): DailyLog?

    @Upsert
    suspend fun saveDailyLog(log: DailyLog)

    // Energy logs
    @Query("SELECT * FROM energy_logs ORDER BY timestamp DESC")
    fun observeEnergyLogs(): Flow<List<EnergyLog>>

    @Query("SELECT * FROM energy_logs ORDER BY timestamp DESC")
    suspend fun allEnergyLogs(): List<EnergyLog>

    @Insert
    suspend fun insertEnergyLog(e: EnergyLog): Long

    @Delete
    suspend fun deleteEnergyLog(e: EnergyLog)

    // Wipe
    @Query("DELETE FROM meals") suspend fun clearMeals()
    @Query("DELETE FROM profile") suspend fun clearProfile()
    @Query("DELETE FROM assessments") suspend fun clearAssessments()
    @Query("DELETE FROM daily_logs") suspend fun clearDailyLogs()
    @Query("DELETE FROM energy_logs") suspend fun clearEnergyLogs()
}
