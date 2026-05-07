package com.petanalyzer.ui.diary.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petanalyzer.data.dao.DailyCount
import com.petanalyzer.ui.theme.*

@Composable
fun StatsBarChart(
    data: List<DailyCount>,
    modifier: Modifier = Modifier,
) {
    if (data.isEmpty()) {
        Box(modifier = modifier.height(160.dp), contentAlignment = Alignment.Center) {
            Text("数据收集中...", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        return
    }

    val maxCount = (data.maxOfOrNull { it.cnt } ?: 1).coerceAtLeast(1).toFloat()

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(start = 8.dp, end = 8.dp, top = 8.dp),
        ) {
            val barWidth = (size.width / data.size) * 0.6f
            val gap = (size.width / data.size) * 0.4f
            val chartHeight = size.height - 24f

            data.forEachIndexed { index, dailyCount ->
                val barHeight = (dailyCount.cnt / maxCount) * chartHeight
                val x = index * (barWidth + gap) + gap / 2

                drawRoundRect(
                    color = Color(0xFFB5EAD7),
                    topLeft = Offset(x, chartHeight - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(6f, 6f),
                )

                drawRoundRect(
                    color = Color(0xFF7BC4A8),
                    topLeft = Offset(x, chartHeight - barHeight),
                    size = Size(barWidth, barHeight * 0.3f.coerceAtMost(1f)),
                    cornerRadius = CornerRadius(6f, 6f),
                )
            }
        }

        // X轴标签
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            data.forEach { dailyCount ->
                Text(
                    dailyCount.dateKey.takeLast(5),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
