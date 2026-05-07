package com.petanalyzer.ui.diary.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petanalyzer.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth

data class CalendarCell(
    val date: LocalDate?,
    val isToday: Boolean = false,
    val emotionEmoji: String = "",
    val hasSnapshot: Boolean = false,
    val hasData: Boolean = false,
)

@Composable
fun CalendarGrid(
    yearMonth: YearMonth,
    moodMap: Map<LocalDate, String>,
    onDateClick: (LocalDate) -> Unit,
) {
    val today = LocalDate.now()
    val firstDay = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val startPadding = (firstDay.dayOfWeek.value - 1) % 7 // Monday=0

    val cells = buildList {
        repeat(startPadding) { add(CalendarCell(null)) }
        for (day in 1..daysInMonth) {
            val date = yearMonth.atDay(day)
            val emotionName = moodMap[date] ?: ""
            val emotionEmoji = try {
                com.petanalyzer.analyzer.Emotion.valueOf(emotionName).emoji
            } catch (_: Exception) { "" }

            add(
                CalendarCell(
                    date = date,
                    isToday = date == today,
                    emotionEmoji = emotionEmoji,
                    hasData = emotionEmoji.isNotEmpty(),
                )
            )
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // 周头
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                Text(
                    it,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth().height(260.dp),
            userScrollEnabled = false,
        ) {
            itemsIndexed(cells) { _, cell ->
                if (cell.date != null) {
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    cell.isToday -> PinkLight
                                    cell.hasData -> CardBackground
                                    else -> CardBackground.copy(alpha = 0.5f)
                                }
                            )
                            .then(
                                if (cell.isToday) Modifier.border(2.dp, PinkDark, CircleShape)
                                else Modifier
                            )
                            .clickable { onDateClick(cell.date) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                cell.date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (cell.isToday) PinkDark else TextPrimary,
                                fontWeight = if (cell.isToday) FontWeight.Bold else FontWeight.Normal,
                            )
                            if (cell.emotionEmoji.isNotEmpty()) {
                                Text(cell.emotionEmoji, fontSize = 16.sp)
                            } else {
                                Text("·", fontSize = 10.sp, color = TextSecondary)
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.aspectRatio(1f).padding(2.dp))
                }
            }
        }
    }
}
