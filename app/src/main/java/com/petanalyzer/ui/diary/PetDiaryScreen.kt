package com.petanalyzer.ui.diary

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petanalyzer.ui.theme.*
import java.time.LocalTime

private enum class DiaryTab(val label: String, val emoji: String) {
    TIMELINE("时间线", "📋"),
    STATS("活动统计", "📊"),
    CALENDAR("心情日历", "📅"),
    CARD("日记卡", "✨"),
}

@Composable
fun PetDiaryScreen() {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(DiaryTab.TIMELINE) }
    var alertMessages by remember { mutableStateOf<List<String>>(emptyList()) }

    val abnormalAlertManager = remember { AbnormalAlertManager().also { it.attach(context) } }

    // 周期性检查异常行为（每5分钟）
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(5000)
        while (true) {
            try {
                val alerts = abnormalAlertManager.check(LocalTime.now().hour)
                alertMessages = alerts
            } catch (_: Exception) {}
            kotlinx.coroutines.delay(300_000) // 5 minutes
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(BackgroundStart, BackgroundEnd))),
    ) {
        Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

        // 标题
        Text(
            "🐾 宠物日记",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        // 异常提醒横幅
        alertMessages.forEach { msg ->
            AnimatedVisibility(
                visible = true,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = WarmYellowLight.copy(alpha = 0.7f)),
                ) {
                    Text(
                        msg,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                    )
                }
            }
        }

        // 子标签栏
        ScrollableTabRow(
            selectedTabIndex = DiaryTab.entries.indexOf(selectedTab),
            modifier = Modifier.fillMaxWidth(),
            containerColor = BackgroundStart,
            contentColor = PinkDark,
            edgePadding = 16.dp,
            divider = {},
        ) {
            DiaryTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(tab.emoji, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(tab.label, fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal)
                        }
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 子页面
        when (selectedTab) {
            DiaryTab.TIMELINE -> BehaviorTimelineScreen()
            DiaryTab.STATS -> ActivityStatsScreen()
            DiaryTab.CALENDAR -> MoodCalendarScreen()
            DiaryTab.CARD -> DiaryCardScreen()
        }
    }
}
