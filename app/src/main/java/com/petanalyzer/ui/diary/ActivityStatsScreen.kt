package com.petanalyzer.ui.diary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petanalyzer.PetAnalyzerApp
import com.petanalyzer.data.dao.ActivityCount
import com.petanalyzer.data.dao.DailyCount
import com.petanalyzer.ui.diary.components.StatsBarChart
import com.petanalyzer.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ActivityStatsScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as PetAnalyzerApp
    val repo = app.repository

    val today = LocalDate.now().toString()
    val sevenDaysAgo = LocalDate.now().minusDays(6).toString()

    var todayStats by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    val weeklyData by repo.getWeeklyActivity(sevenDaysAgo).collectAsState(initial = emptyList())
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        todayStats = repo.getDailyActivityStats(today)
        loading = false
    }

    if (loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PinkDark)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 80.dp),
    ) {
        // 今日活动概览
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("📊 今日活动概览", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                if (todayStats.isEmpty()) {
                    Text("今天还没有活动数据", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                } else {
                    val totalActive = (todayStats["WALKING"] ?: 0) + (todayStats["RUNNING"] ?: 0) + (todayStats["PLAYING"] ?: 0)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatCard("🏃", "散步", todayStats["WALKING"] ?: 0)
                        StatCard("🎾", "玩耍", todayStats["PLAYING"] ?: 0)
                        StatCard("💤", "休息", (todayStats["SLEEPING"] ?: 0) + (todayStats["LYING_DOWN"] ?: 0))
                        StatCard("🍽️", "吃饭", todayStats["EATING"] ?: 0)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "今日活跃度: ${if (totalActive > 20) "🔥 活力满满" else if (totalActive > 10) "😊 适中" else "😴 偏低"}",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextAccent,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        // 7天趋势
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("📈 近7天活动趋势", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                Text("散步 + 奔跑 + 玩耍 次数", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Spacer(modifier = Modifier.height(12.dp))
                StatsBarChart(data = weeklyData)
            }
        }

        // 建议
        val totalActiveToday = (todayStats["WALKING"] ?: 0) + (todayStats["RUNNING"] ?: 0) + (todayStats["PLAYING"] ?: 0)
        val suggestion = when {
            totalActiveToday == 0 -> "今天还没有活动记录，带宠物出去玩一会儿吧！"
            totalActiveToday < 10 -> "活动量偏少，建议多陪宠物互动~"
            totalActiveToday < 30 -> "活动量适中，继续加油！"
            else -> "今天玩得真尽兴！别忘了让宠物多喝水休息~"
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SkyBlueLight.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        ) {
            Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.Top) {
                Text("💡", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(suggestion, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            }
        }
    }
}

@Composable
private fun StatCard(emoji: String, label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 28.sp)
        Text("$count", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}
