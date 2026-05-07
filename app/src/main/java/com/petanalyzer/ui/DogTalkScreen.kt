package com.petanalyzer.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.petanalyzer.dogtalk.BarkAnalysisResult
import com.petanalyzer.dogtalk.BarkType
import com.petanalyzer.dogtalk.DogSoundPlayer
import com.petanalyzer.dogtalk.DogBarkAnalyzer
import com.petanalyzer.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun DogTalkScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val soundPlayer = remember { DogSoundPlayer(context) }
    val barkAnalyzer = remember { DogBarkAnalyzer(context) }

    val analysisResult by barkAnalyzer.result.collectAsState()
    val isRecording by barkAnalyzer.isRecordingState.collectAsState()

    var playingType by remember { mutableStateOf<BarkType?>(null) }

    // 录音权限
    var hasRecordPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasRecordPermission = isGranted
    }

    // 录音动画
    val recordPulse = rememberInfiniteTransition(label = "recordPulse")
    val pulseScale by recordPulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "recordingScale",
    )

    // 呼吸光晕动画
    val glowAlpha by recordPulse.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowAlpha",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(BackgroundStart, BackgroundEnd)))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // ========== 第一部分：播放叫声给狗狗听 ==========
        SectionHeader(emoji = "🔊", title = "跟狗狗说话", subtitle = "点击播放一种叫声与狗狗互动")

        Spacer(modifier = Modifier.height(12.dp))

        // 叫声按钮网格
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .height(370.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            userScrollEnabled = false,
        ) {
            items(BarkType.entries) { barkType ->
                val isThisPlaying = playingType == barkType
                SoundButton(
                    barkType = barkType,
                    isPlaying = isThisPlaying,
                    onClick = {
                        if (isThisPlaying) {
                            soundPlayer.stop()
                            playingType = null
                        } else {
                            playingType = barkType
                            scope.launch {
                                soundPlayer.play(barkType)
                                playingType = null
                            }
                        }
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalDivider(color = CardBorder, thickness = 1.dp)

        Spacer(modifier = Modifier.height(16.dp))

        // ========== 第二部分：分析狗狗叫声 ==========
        SectionHeader(emoji = "🎙️", title = "听懂狗狗在说什么", subtitle = "录一段狗狗的叫声进行分析")

        Spacer(modifier = Modifier.height(16.dp))

        // 录音按钮区域
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            if (isRecording) {
                // 录音中的光晕和按钮
                Box(contentAlignment = Alignment.Center) {
                    // 外光晕
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(WarmYellow.copy(alpha = glowAlpha)),
                    )
                    // 脉冲按钮
                    FloatingActionButton(
                        onClick = { barkAnalyzer.stopRecording() },
                        modifier = Modifier.scale(pulseScale),
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White,
                        shape = CircleShape,
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = "停止录音", modifier = Modifier.size(36.dp))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            } else {
                // 录音按钮
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FloatingActionButton(
                        onClick = {
                            if (!hasRecordPermission) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                scope.launch { barkAnalyzer.startRecording() }
                            }
                        },
                        containerColor = PinkDark,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(80.dp),
                    ) {
                        Icon(Icons.Filled.Mic, contentDescription = "开始录音", modifier = Modifier.size(40.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (hasRecordPermission) "点击录音" else "需要录音权限",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 分析结果卡片
        if (analysisResult.hasResult || analysisResult.interpretation != "等待分析狗狗的叫声...") {
            BarkAnalysisCard(result = analysisResult)
        }

        Spacer(modifier = Modifier.height(80.dp)) // 底部导航栏空间
    }
}

@Composable
private fun SectionHeader(emoji: String, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 24.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

@Composable
private fun SoundButton(barkType: BarkType, isPlaying: Boolean, onClick: () -> Unit) {
    val bgColor by animateColorAsState(
        targetValue = if (isPlaying) PinkLight else CardBackground,
        animationSpec = tween(300),
        label = "btnBg",
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = if (isPlaying) BorderStroke(2.dp, PinkDark) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPlaying) 6.dp else 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 图标
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isPlaying) PinkDark.copy(alpha = 0.15f) else MintGreenLight
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (isPlaying) {
                    Icon(
                        Icons.Filled.VolumeUp,
                        contentDescription = null,
                        tint = PinkDark,
                        modifier = Modifier.size(24.dp),
                    )
                } else {
                    Text(barkType.emoji, fontSize = 22.sp)
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    barkType.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    barkType.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                )
            }
            Icon(
                if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = if (isPlaying) PinkDark else TextSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun BarkAnalysisCard(result: BarkAnalysisResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
        ) {
            // 标题行
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = LavenderLight,
                    modifier = Modifier.size(42.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🎙️", fontSize = 20.sp)
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "叫声分析结果",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                    if (result.hasResult) {
                        Text(
                            "${result.detectedBark.emoji} ${result.detectedBark.label} · ${result.emotion.emoji} ${result.emotion.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                    }
                }
                if (result.confidence > 0f) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (result.confidence > 60f) MintGreenLight else WarmYellowLight,
                    ) {
                        Text(
                            "置信度 ${result.confidence.toInt()}%",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 叫声解读
            Text(
                text = result.interpretation,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                lineHeight = 26.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Cream.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .padding(14.dp),
            )

            // 建议
            if (result.suggestion.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SkyBlueLight.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text("💡", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = result.suggestion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        lineHeight = 22.sp,
                    )
                }
            }

            // 音频特征详情
            if (result.details.duration > 0f) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    DetailChip("时长", "${(result.details.duration * 1000).toInt()}ms")
                    DetailChip("能量", result.details.energyLevel.label)
                    DetailChip("音高", if (result.details.peakFrequency > 0f) "${result.details.peakFrequency.toInt()}Hz" else "—")
                }
            }
        }
    }
}

@Composable
private fun DetailChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.labelLarge, color = TextAccent, fontWeight = FontWeight.SemiBold)
    }
}
