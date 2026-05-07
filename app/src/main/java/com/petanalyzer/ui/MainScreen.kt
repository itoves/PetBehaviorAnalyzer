package com.petanalyzer.ui

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.RecordVoiceOver
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.petanalyzer.PetAnalyzerApp
import com.petanalyzer.analyzer.AnalysisResult
import com.petanalyzer.analyzer.PetBehaviorType
import com.petanalyzer.camera.CameraController
import com.petanalyzer.camera.SnapshotManager
import com.petanalyzer.ui.diary.PetDiaryScreen
import com.petanalyzer.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class ScreenTab(val label: String, val selectedIcon: @Composable () -> Unit, val unselectedIcon: @Composable () -> Unit) {
    CAMERA("相机分析", { Icon(Icons.Filled.CameraAlt, contentDescription = null) }, { Icon(Icons.Outlined.CameraAlt, contentDescription = null) }),
    DOG_TALK("狗语对话", { Icon(Icons.Filled.RecordVoiceOver, contentDescription = null) }, { Icon(Icons.Outlined.RecordVoiceOver, contentDescription = null) }),
    PET_DIARY("宠物日记", { Icon(Icons.Filled.Book, contentDescription = null) }, { Icon(Icons.Outlined.Book, contentDescription = null) }),
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(ScreenTab.CAMERA) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = CardBackground,
                tonalElevation = 8.dp,
            ) {
                ScreenTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = {
                            if (tab == ScreenTab.CAMERA && !hasCameraPermission) {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                            selectedTab = tab
                        },
                        icon = { if (selectedTab == tab) tab.selectedIcon() else tab.unselectedIcon() },
                        label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PinkDark,
                            selectedTextColor = PinkDark,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = PinkLight,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                ScreenTab.CAMERA -> {
                    if (!hasCameraPermission) {
                        PermissionRequestScreen(onRequestPermission = {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        })
                    } else {
                        CameraScreen()
                    }
                }
                ScreenTab.DOG_TALK -> DogTalkScreen()
                ScreenTab.PET_DIARY -> PetDiaryScreen()
            }
        }
    }
}

@Composable
private fun PermissionRequestScreen(onRequestPermission: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(BackgroundStart, BackgroundEnd))),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.padding(32.dp).fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column(
                modifier = Modifier.padding(32.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = "🐾", fontSize = 64.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("需要摄像头权限", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "为了观察可爱的小动物们\n需要开启摄像头哦~",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onRequestPermission,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PinkDark),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Text("开启摄像头", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun CameraScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var analysisResult by remember { mutableStateOf(AnalysisResult()) }
    var showBubble by remember { mutableStateOf(true) }

    // 气泡弹入动画
    LaunchedEffect(analysisResult.description) {
        showBubble = false
        delay(50)
        showBubble = true
    }

    // 脉冲动画
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

    // 快照管理
    val snapshotManager = remember { SnapshotManager(context) }
    val app = context.applicationContext as PetAnalyzerApp
    val repo = app.repository

    // 相机控制器
    val cameraController = remember {
        CameraController(context).apply {
            onAnalysisResult = { result ->
                analysisResult = result
                // 记录行为到数据库
                if (result.hasPet) {
                    scope.launch(Dispatchers.IO) {
                        repo.logResult(result)
                    }
                }
            }
            onSnapshot = { bitmap ->
                scope.launch(Dispatchers.IO) {
                    snapshotManager.save(bitmap, analysisResult.behavior.name)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(BackgroundStart, BackgroundEnd))),
    ) {
        // 相机预览
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    cameraController.startCamera(
                        lifecycleOwner = ctx as androidx.lifecycle.LifecycleOwner,
                        previewView = this,
                        scope = scope,
                    )
                }
            }
        )

        // 底部渐变覆盖层（让文字更清晰）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0x00000000), Color(0x00000000), Color(0x20000000)),
                        startY = 0f,
                        endY = 2000f,
                    )
                )
        )

        // 顶部状态栏
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            TopBar(analysisResult)
        }

        // 底部行为气泡
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .padding(horizontal = 20.dp),
        ) {
            AnimatedVisibility(
                visible = showBubble,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                ) + fadeIn(),
                exit = fadeOut(),
            ) {
                BehaviorBubble(
                    result = analysisResult,
                    modifier = Modifier.fillMaxWidth().scale(if (analysisResult.hasPet) pulseScale else 1f),
                )
            }
        }
    }
}

@Composable
private fun TopBar(result: AnalysisResult) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = PinkLight, modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Pets, contentDescription = null, tint = PinkDark, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text("宠物翻译官", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                Text("Pet Analyzer", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }

        if (result.hasPet) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(shape = CircleShape, color = WarmYellowLight, modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text(text = result.emotion.emoji, fontSize = 20.sp) }
                }
                Surface(shape = CircleShape, color = MintGreenLight, modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text(text = result.petType.emoji, fontSize = 22.sp) }
                }
            }
        }
    }
}

@Composable
private fun BehaviorBubble(result: AnalysisResult, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = BehaviorBubbleBg.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            if (result.hasPet) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (result.behavior != PetBehaviorType.UNKNOWN) PinkLight else LavenderLight,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = result.behavior.emoji, fontSize = 26.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(result.behavior.label, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text(
                            "${result.petType.emoji} ${result.petType.label} · ${result.emotion.emoji} ${result.emotion.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Text(
                text = result.description,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                lineHeight = 28.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = CardBackground.copy(alpha = 0.6f), shape = RoundedCornerShape(16.dp))
                    .padding(16.dp),
            )

            if (result.confidence > 0f) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("准确度", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(MintGreenLight)) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = result.confidence.coerceIn(0f, 1f))
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (result.confidence > 0.6f) MintGreenDark else WarmYellow)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "${(result.confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}
