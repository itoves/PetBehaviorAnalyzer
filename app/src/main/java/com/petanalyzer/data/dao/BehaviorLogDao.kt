package com.petanalyzer.data.dao

import androidx.room.*
import com.petanalyzer.data.entity.BehaviorLogEntity
import kotlinx.coroutines.flow.Flow

data class ActivityCount(val behavior: String, val cnt: Int)
data class DailyCount(
    @ColumnInfo(name = "date_key") val dateKey: String,
    val cnt: Int,
)
data class DateMood(val dateKey: String, val emotion: String)
data class DateEmotionCount(
    @ColumnInfo(name = "date_key") val dateKey: String,
    val emotion: String,
    val cnt: Int,
)
data class EmotionCount(
    val emotion: String,
    val cnt: Int,
)

@Dao
interface BehaviorLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: BehaviorLogEntity): Long

    @Query("SELECT * FROM behavior_logs WHERE date_key = :dateKey ORDER BY timestamp DESC LIMIT 200")
    fun getLogsByDate(dateKey: String): Flow<List<BehaviorLogEntity>>

    @Query("SELECT DISTINCT date_key FROM behavior_logs ORDER BY date_key DESC LIMIT :limit")
    fun getRecentDates(limit: Int = 30): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM behavior_logs WHERE date_key = :dateKey")
    fun getLogCountForDate(dateKey: String): Flow<Int>

    @Query("""
        SELECT behavior, COUNT(*) as cnt FROM behavior_logs
        WHERE date_key = :dateKey AND behavior IN ('WALKING', 'RUNNING', 'PLAYING', 'SLEEPING', 'EATING', 'SITTING', 'LYING_DOWN')
        GROUP BY behavior
    """)
    suspend fun getActivityCountsForDate(dateKey: String): List<ActivityCount>

    @Query("""
        SELECT date_key, COUNT(*) as cnt FROM behavior_logs
        WHERE date_key >= :startDate AND behavior IN ('WALKING', 'RUNNING', 'PLAYING')
        GROUP BY date_key ORDER BY date_key ASC
    """)
    fun getActivityDailyCounts(startDate: String): Flow<List<DailyCount>>

    @Query("""
        SELECT emotion, COUNT(*) as cnt FROM behavior_logs
        WHERE date_key = :dateKey
        GROUP BY emotion ORDER BY cnt DESC LIMIT 1
    """)
    suspend fun getDominantEmotion(dateKey: String): EmotionCount?

    @Query("""
        SELECT date_key, emotion, COUNT(*) as cnt FROM behavior_logs
        WHERE date_key >= :startDate AND date_key <= :endDate
        GROUP BY date_key, emotion ORDER BY date_key ASC
    """)
    suspend fun getEmotionCountsInRange(startDate: String, endDate: String): List<DateEmotionCount>

    @Query("SELECT COUNT(*) FROM behavior_logs WHERE timestamp > :since AND behavior = 'SCRATCHING'")
    suspend fun countScratchingSince(since: Long): Int

    @Query("SELECT COUNT(*) FROM behavior_logs WHERE timestamp > :since AND behavior = 'SLEEPING'")
    suspend fun countSleepingSince(since: Long): Int

    @Query("SELECT COUNT(*) FROM behavior_logs WHERE timestamp > :since AND emotion = 'SAD'")
    suspend fun countSadSince(since: Long): Int

    @Query("SELECT COUNT(*) FROM behavior_logs WHERE timestamp > :since AND behavior IN ('WALKING', 'RUNNING', 'PLAYING')")
    suspend fun countActiveSince(since: Long): Int

    @Query("SELECT * FROM behavior_logs WHERE timestamp > :since AND pet_type != 'UNKNOWN'")
    suspend fun getLogsSince(since: Long): List<BehaviorLogEntity>

    @Query("SELECT * FROM behavior_logs WHERE date_key = :dateKey ORDER BY timestamp ASC")
    suspend fun getLogsForDate(dateKey: String): List<BehaviorLogEntity>

    @Query("DELETE FROM behavior_logs WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("SELECT COUNT(*) FROM behavior_logs WHERE date_key = :dateKey AND behavior = :behavior")
    fun getBehaviorCountForDate(dateKey: String, behavior: String): Flow<Int>
}
