package com.petanalyzer.ui.diary

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Picture
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.petanalyzer.PetAnalyzerApp
import com.petanalyzer.data.repository.DailySummary
import com.petanalyzer.ui.diary.components.DiaryCardComposable
import com.petanalyzer.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

@Composable
fun DiaryCardScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as PetAnalyzerApp
    val repo = app.repository
    val scope = rememberCoroutineScope()

    val today = LocalDate.now().toString()
    var summary by remember { mutableStateOf<DailySummary?>(null) }
    var loading by remember { mutableStateOf(true) }
    var generatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isGenerating by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        summary = repo.getDailySummary(today)
        loading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PinkDark)
            }
        } else if (summary == null || summary!!.totalLogs == 0) {
            EmptyDiaryState("今天还没有记录~", "用相机分析宠物后就能生成日记卡啦 ✨")
        } else {
            // 预览区域
            Card(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            ) {
                if (generatedBitmap != null) {
                    Image(
                        bitmap = generatedBitmap!!.asImageBitmap(),
                        contentDescription = "日记卡",
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    DiaryCardComposable(
                        summary = summary!!,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 生成按钮
            Button(
                onClick = {
                    isGenerating = true
                    scope.launch {
                        generatedBitmap = withContext(Dispatchers.Default) {
                            renderDiaryCardToBitmap(summary!!)
                        }
                        isGenerating = false
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PinkDark),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !isGenerating,
            ) {
                if (isGenerating) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                else Text("🖼️ 生成今日日记卡", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            // 分享按钮
            if (generatedBitmap != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val file = File(context.cacheDir, "shared/diary_card_$today.png")
                        file.parentFile?.mkdirs()
                        FileOutputStream(file).use { out ->
                            generatedBitmap!!.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "分享宠物日记卡"))
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MintGreenDark),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Text("📤 分享日记卡", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

private fun renderDiaryCardToBitmap(summary: DailySummary): Bitmap? {
    return try {
        val width = 400
        val height = 600
        val picture = Picture()
        // Simplified approach: create bitmap directly with Android Canvas
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)

        // Background
        val paint = android.graphics.Paint().apply {
            shader = android.graphics.LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(Color(0xFFFFF5F0).toArgb(), Color(0xFFF0F5FF).toArgb()),
                null, android.graphics.Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // Title
        val titlePaint = android.graphics.Paint().apply {
            color = Color(0xFF4A3F4D).toArgb()
            textSize = 28f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("🐾 巴特日记卡", 20f, 50f, titlePaint)

        // Date
        val datePaint = android.graphics.Paint().apply {
            color = Color(0xFF8A7F8D).toArgb()
            textSize = 16f
            isAntiAlias = true
        }
        canvas.drawText(summary.dateKey, 20f, 80f, datePaint)

        // Pet type emoji
        val emojiPaint = android.graphics.Paint().apply {
            textSize = 80f
            isAntiAlias = true
        }
        canvas.drawText(summary.petType.emoji, width / 2f - 40f, 170f, emojiPaint)

        // Emotion
        val emotionPaint = android.graphics.Paint().apply {
            textSize = 24f
            isAntiAlias = true
        }
        canvas.drawText("${summary.dominantEmotion.emoji} 今天心情: ${summary.dominantEmotion.label}", 20f, 220f, emotionPaint)

        // Stats
        val statsPaint = android.graphics.Paint().apply {
            color = Color(0xFF4A3F4D).toArgb()
            textSize = 18f
            isAntiAlias = true
        }
        var y = 270f
        summary.activityCounts.forEach { (behavior, count) ->
            val emoji = try { com.petanalyzer.analyzer.PetBehaviorType.valueOf(behavior).emoji } catch (_: Exception) { "🐾" }
            canvas.drawText("$emoji $behavior: ${count}次", 20f, y, statsPaint)
            y += 30f
        }

        // Description
        if (summary.interestingDescription.isNotEmpty()) {
            val descPaint = android.graphics.Paint().apply {
                color = Color(0xFFC44A7C).toArgb()
                textSize = 16f
                isAntiAlias = true
            }
            canvas.drawText("\"${summary.interestingDescription.take(30)}...\"", 20f, y + 20f, descPaint)
        }

        // Footer
        val footerPaint = android.graphics.Paint().apply {
            color = Color(0xFF8A7F8D).toArgb()
            textSize = 12f
            isAntiAlias = true
        }
        canvas.drawText("记录于 巴特对话机", 20f, height - 30f, footerPaint)

        bitmap
    } catch (e: Exception) {
        null
    }
}
