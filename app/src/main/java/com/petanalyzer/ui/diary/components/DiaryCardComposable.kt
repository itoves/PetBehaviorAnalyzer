package com.petanalyzer.ui.diary.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petanalyzer.data.repository.DailySummary
import com.petanalyzer.ui.theme.*

@Composable
fun DiaryCardComposable(
    summary: DailySummary,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(0.7f)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(colors = listOf(BackgroundStart, BackgroundEnd))),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 标题
            Text("🐾 巴特日记卡", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
            Text(summary.dateKey, style = MaterialTheme.typography.labelSmall, color = TextSecondary)

            Spacer(modifier = Modifier.height(16.dp))

            // 宠物表情
            Text(summary.petType.emoji, fontSize = 64.sp)

            Spacer(modifier = Modifier.height(8.dp))

            // 心情
            Text(
                "${summary.dominantEmotion.emoji} 今天主要心情",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(summary.dominantEmotion.label, style = MaterialTheme.typography.headlineMedium, color = TextAccent, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(12.dp))

            // 活动统计
            if (summary.activityCounts.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    summary.activityCounts.entries.take(4).forEach { (behavior, count) ->
                        val emoji = try {
                            com.petanalyzer.analyzer.PetBehaviorType.valueOf(behavior).emoji
                        } catch (_: Exception) { "🐾" }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(emoji, fontSize = 22.sp)
                            Text("$count", style = MaterialTheme.typography.labelLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 精选描述
            if (summary.interestingDescription.isNotEmpty()) {
                Text(
                    "\"${summary.interestingDescription.take(40)}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextAccent,
                    textAlign = TextAlign.Center,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("记录于 巴特对话机", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}
