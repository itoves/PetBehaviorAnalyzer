package com.petanalyzer.ui.diary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petanalyzer.PetAnalyzerApp
import com.petanalyzer.ui.diary.components.CalendarGrid
import com.petanalyzer.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodCalendarScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as PetAnalyzerApp
    val repo = app.repository

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var moodMap by remember { mutableStateOf<Map<LocalDate, String>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedMood by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentMonth) {
        loading = true
        moodMap = repo.getMonthMoods(currentMonth)
        loading = false
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
    ) {
        // 月份导航
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                Text("◀ 上月", color = PinkDark)
            }
            Text(
                currentMonth.format(DateTimeFormatter.ofPattern("yyyy年M月")),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
            )
            TextButton(
                onClick = {
                    if (currentMonth < YearMonth.now()) currentMonth = currentMonth.plusMonths(1)
                },
            ) {
                Text(
                    "下月 ▶",
                    color = if (currentMonth < YearMonth.now()) PinkDark else TextSecondary,
                )
            }
        }

        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PinkDark)
            }
        } else {
            CalendarGrid(
                yearMonth = currentMonth,
                moodMap = moodMap,
                onDateClick = { date ->
                    selectedDate = date
                    selectedMood = moodMap[date]
                },
            )

            // 图例
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                com.petanalyzer.analyzer.Emotion.entries.take(5).forEach { emotion ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(emotion.emoji, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(emotion.label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
            }
        }
    }

    // 日期详情弹窗
    if (selectedDate != null) {
        AlertDialog(
            onDismissRequest = { selectedDate = null },
            title = {
                Text(
                    selectedDate!!.format(DateTimeFormatter.ofPattern("M月d日")) +
                            " " + (selectedDate!!.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINESE)),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column {
                    if (selectedMood != null) {
                        val emotion = try {
                            com.petanalyzer.analyzer.Emotion.valueOf(selectedMood!!)
                        } catch (_: Exception) { null }
                        Text(
                            "${emotion?.emoji ?: "😌"} 主要心情: ${emotion?.label ?: "未知"}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary,
                        )
                    } else {
                        Text("这一天还没有记录呢~", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedDate = null }) {
                    Text("知道了", color = PinkDark)
                }
            },
        )
    }
}
