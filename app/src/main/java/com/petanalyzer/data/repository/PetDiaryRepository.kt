package com.petanalyzer.data.repository

import com.petanalyzer.analyzer.AnalysisResult
import com.petanalyzer.analyzer.Emotion
import com.petanalyzer.analyzer.PetBehaviorType
import com.petanalyzer.analyzer.PetType
import com.petanalyzer.data.dao.*
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class PetDiaryRepository(private val dao: BehaviorLogDao) {

    private var lastLoggedBehavior: String? = null
    private var lastLoggedEmotion: String? = null

    suspend fun logResult(result: AnalysisResult, snapshotPath: String? = null) {
        if (!result.hasPet) return
        // 去重：仅行为或情绪变化时记录
        val behaviorName = result.behavior.name
        val emotionName = result.emotion.name
        if (behaviorName == lastLoggedBehavior && emotionName == lastLoggedEmotion) return
        lastLoggedBehavior = behaviorName
        lastLoggedEmotion = emotionName

        val entity = com.petanalyzer.data.entity.BehaviorLogEntity(
            timestamp = System.currentTimeMillis(),
            dateKey = LocalDate.now().toString(),
            petType = result.petType.name,
            behavior = behaviorName,
            emotion = emotionName,
            confidence = result.confidence,
            description = result.description,
            snapshotPath = snapshotPath,
        )
        dao.insert(entity)
    }

    fun getTimelineForDate(dateKey: String): Flow<List<com.petanalyzer.data.entity.BehaviorLogEntity>> =
        dao.getLogsByDate(dateKey)

    fun getRecentDates(limit: Int = 30): Flow<List<String>> = dao.getRecentDates(limit)

    fun getLogCountForDate(dateKey: String): Flow<Int> = dao.getLogCountForDate(dateKey)

    suspend fun getDailyActivityStats(dateKey: String): Map<String, Int> {
        val counts = dao.getActivityCountsForDate(dateKey)
        return counts.associate { it.behavior to it.cnt }
    }

    fun getWeeklyActivity(startDate: String): Flow<List<DailyCount>> = dao.getActivityDailyCounts(startDate)

    suspend fun getDominantEmotion(dateKey: String): Emotion? {
        val result = dao.getDominantEmotion(dateKey) ?: return null
        return try { Emotion.valueOf(result.emotion) } catch (_: Exception) { null }
    }

    suspend fun getMonthMoods(yearMonth: YearMonth): Map<LocalDate, String> {
        val start = yearMonth.atDay(1).toString()
        val end = yearMonth.atEndOfMonth().toString()
        val data = dao.getEmotionCountsInRange(start, end)
        return data.groupBy { LocalDate.parse(it.dateKey) }
            .mapValues { (_, list) -> list.maxByOrNull { it.cnt }?.emotion ?: "" }
    }

    suspend fun getDailySummary(dateKey: String): DailySummary {
        val logs = dao.getLogsForDate(dateKey)
        if (logs.isEmpty()) return DailySummary(dateKey = dateKey)

        val dominantEmotion = logs.groupBy { it.emotion }.maxByOrNull { it.value.size }?.key ?: "NEUTRAL"
        val activityMap = logs.filter { it.behavior in setOf("WALKING", "RUNNING", "PLAYING", "SLEEPING", "EATING") }
            .groupBy { it.behavior }.mapValues { it.value.size }
        val petType = logs.groupBy { it.petType }.maxByOrNull { it.value.size }?.key ?: "UNKNOWN"
        val interestingLog = logs.maxByOrNull { it.confidence }
        val snapshotPath = logs.firstOrNull { it.snapshotPath != null }?.snapshotPath

        return DailySummary(
            dateKey = dateKey,
            petType = try { PetType.valueOf(petType) } catch (_: Exception) { PetType.UNKNOWN },
            dominantEmotion = try { Emotion.valueOf(dominantEmotion) } catch (_: Exception) { Emotion.NEUTRAL },
            totalLogs = logs.size,
            activityCounts = activityMap,
            interestingDescription = interestingLog?.description ?: "",
            snapshotPath = snapshotPath,
        )
    }

    suspend fun checkAbnormalPatterns(currentHour: Int): List<String> {
        val alerts = mutableListOf<String>()
        val now = System.currentTimeMillis()

        // 仅在白天检测（8:00-22:00）
        if (currentHour in 8..21) {
            val twoHoursAgo = now - 2 * 3600_000
            val sleepCount = dao.countSleepingSince(twoHoursAgo)
            val totalLogs = dao.getLogsSince(twoHoursAgo).size
            if (totalLogs > 10 && sleepCount.toFloat() / totalLogs > 0.7f) {
                alerts.add("狗狗好像一直在睡觉...要不要带它出门走走？💤")
            }

            val threeHoursAgo = now - 3 * 3600_000
            val activeCount = dao.countActiveSince(threeHoursAgo)
            if (totalLogs > 15 && activeCount == 0) {
                alerts.add("狗狗好久没活动了，跟它互动一下吧~🐶")
            }
        }

        val oneHourAgo = now - 3600_000
        val scratchCount = dao.countScratchingSince(oneHourAgo)
        if (scratchCount >= 12) {
            alerts.add("狗狗挠得有点多，检查一下皮肤有没有问题？🐾")
        }

        val fifteenMinAgo = now - 900_000
        val sadCount = dao.countSadSince(fifteenMinAgo)
        if (sadCount >= 8) {
            alerts.add("狗狗好像心情不太好，多陪陪它吧~😢")
        }

        return alerts
    }

    suspend fun cleanupOldLogs(daysToKeep: Int = 90) {
        val cutoff = System.currentTimeMillis() - daysToKeep.toLong() * 24 * 3600_000
        dao.deleteOlderThan(cutoff)
    }
}

data class DailySummary(
    val dateKey: String = "",
    val petType: PetType = PetType.UNKNOWN,
    val dominantEmotion: Emotion = Emotion.NEUTRAL,
    val totalLogs: Int = 0,
    val activityCounts: Map<String, Int> = emptyMap(),
    val interestingDescription: String = "",
    val snapshotPath: String? = null,
)
