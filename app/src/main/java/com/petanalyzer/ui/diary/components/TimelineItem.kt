package com.petanalyzer.ui.diary.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petanalyzer.data.entity.BehaviorLogEntity
import com.petanalyzer.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun TimelineItem(log: BehaviorLogEntity) {
    val timeStr = Instant.ofEpochMilli(log.timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
        .format(DateTimeFormatter.ofPattern("HH:mm"))

    val petEmoji = when {
        log.petType.contains("CAT") -> "🐱"
        log.petType.contains("DOG") -> "🐶"
        else -> "🐾"
    }

    val behaviorEmoji = try {
        com.petanalyzer.analyzer.PetBehaviorType.valueOf(log.behavior).emoji
    } catch (_: Exception) { "❓" }

    val emotionEmoji = try {
        com.petanalyzer.analyzer.Emotion.valueOf(log.emotion).emoji
    } catch (_: Exception) { "😌" }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 时间
            Text(
                timeStr,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.width(40.dp),
            )

            // 行为图标
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PinkLight),
                contentAlignment = Alignment.Center,
            ) {
                Text(behaviorEmoji, fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(petEmoji, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        try { com.petanalyzer.analyzer.PetBehaviorType.valueOf(log.behavior).label }
                        catch (_: Exception) { log.behavior },
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(emotionEmoji, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    log.description.take(60),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                )
            }

            // 准确度
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (log.confidence > 0.6f) MintGreenLight else WarmYellowLight,
            ) {
                Text(
                    "${(log.confidence * 100).toInt()}%",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
            }
        }
    }
}
