package com.petanalyzer.ui.diary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import kotlinx.coroutines.flow.first
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petanalyzer.PetAnalyzerApp
import com.petanalyzer.data.entity.BehaviorLogEntity
import com.petanalyzer.ui.diary.components.TimelineItem
import com.petanalyzer.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun BehaviorTimelineScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as PetAnalyzerApp
    val repo = app.repository

    val dates by repo.getRecentDates(30).collectAsState(initial = emptyList())
    var logsByDate by remember { mutableStateOf<Map<String, List<BehaviorLogEntity>>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(dates) {
        if (dates.isEmpty()) {
            loading = false
            return@LaunchedEffect
        }
        loading = true
        val map = mutableMapOf<String, List<BehaviorLogEntity>>()
        // 只取每个flow的第一个值来填充初始数据
        for (date in dates.take(7)) {
            val logs = repo.getTimelineForDate(date).first()
            map[date] = logs
        }
        logsByDate = map
        loading = false
    }

    if (loading) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PinkDark)
        }
    } else if (dates.isEmpty() || logsByDate.isEmpty()) {
        EmptyDiaryState("今天还没有记录哦~", "打开相机分析一下宠物吧 📷")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
        ) {
            dates.forEach { dateKey ->
                val logs = logsByDate[dateKey] ?: return@forEach
                item {
                    val localDate = LocalDate.parse(dateKey)
                    val dayOfWeek = localDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINESE)
                    val formatted = localDate.format(DateTimeFormatter.ofPattern("M月d日"))
                    val isToday = localDate == LocalDate.now()

                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "$formatted $dayOfWeek",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                        if (isToday) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(shape = MaterialTheme.shapes.small, color = PinkLight) {
                                Text(
                                    "今天",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PinkDark,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            "${logs.size}条记录",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                    }
                }
                items(logs) { log -> TimelineItem(log = log) }
            }
        }
    }
}

@Composable
fun EmptyDiaryState(message: String, suggestion: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📖", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(message, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(suggestion, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}
