package com.petanalyzer.analyzer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import org.tensorflow.lite.task.vision.detector.ObjectDetector.ObjectDetectorOptions
import org.tensorflow.lite.support.image.TensorImage
import kotlin.math.sqrt

class PetDetector(context: Context) {

    private val detector: ObjectDetector
    private val moodClassifier: PetMoodClassifier
    private val frameHistory = mutableListOf<FrameData>()
    private val maxHistorySize = 30

    // 平滑后的检测框（EMA）
    private var smoothedBox: RectF? = null
    private val smoothingAlpha = 0.35f

    // 行为状态持久化，避免闪烁
    private var currentBehavior: PetBehaviorType = PetBehaviorType.UNKNOWN
    private var behaviorConfidence = 0
    private val behaviorConfirmationFrames = 3

    // 情绪分类
    private var moodFrameCounter = 0
    private var lastMoodResult = PetMoodClassifier.MoodResult("", 0f, false)

    data class FrameData(
        val rawBox: RectF,
        val smoothedBox: RectF,
        val timestamp: Long,
        val centerX: Float,
        val centerY: Float,
        val aspectRatio: Float,
    )

    companion object {
        private const val CONFIDENCE_THRESHOLD = 0.35f
        private const val ASPECT_STANDING = 1.25f       // 高/宽 > 1.25 → 站立
        private const val ASPECT_LYING = 0.75f           // 高/宽 < 0.75 → 躺下
        private const val MOVEMENT_SLEEP = 1.2f          // 平均位移 < 1.2px → 静止
        private const val MOVEMENT_WALK = 5f             // 平均位移 > 5px → 行走
        private const val MOVEMENT_RUN = 14f             // 平均位移 > 14px → 奔跑
        private const val AREA_APPROACH_RATIO = 1.25f    // 面积增长 > 1.25x → 靠近
    }

    init {
        val options = ObjectDetectorOptions.builder()
            .setScoreThreshold(CONFIDENCE_THRESHOLD)
            .setMaxResults(5)
            .build()
        detector = ObjectDetector.createFromFileAndOptions(context, "efficientdet_lite0.tflite", options)
        moodClassifier = PetMoodClassifier(context)
    }

    // 快照回调：当检测到可爱行为时触发
    var onSnapshotTrigger: ((Bitmap) -> Unit)? = null

    @OptIn(ExperimentalGetImage::class)
    suspend fun analyze(imageProxy: ImageProxy): AnalysisResult {
        val fullBitmap = imageProxyToFullBitmap(imageProxy) ?: return AnalysisResult(description = "无法获取画面 📷")
        val bitmap = Bitmap.createScaledBitmap(fullBitmap, 320, 240, true)
        imageProxy.close()

        val tensorImage = TensorImage.fromBitmap(bitmap)
        val results = detector.detect(tensorImage)
        val pet = findBestPet(results)

        if (pet == null) {
            frameHistory.clear()
            smoothedBox = null
            currentBehavior = PetBehaviorType.UNKNOWN
            behaviorConfidence = 0
            return AnalysisResult(hasPet = false, description = "没有检测到小可爱呢 🐾")
        }

        val petType = classifyPetType(pet)
        val rawBox = pet.boundingBox

        // EMA 平滑检测框
        smoothedBox = smoothBoundingBox(rawBox)

        val now = System.currentTimeMillis()
        val aspectRatio = if (smoothedBox!!.width() > 0f) {
            smoothedBox!!.height() / smoothedBox!!.width()
        } else 1.0f

        frameHistory.add(
            FrameData(
                rawBox = rawBox,
                smoothedBox = smoothedBox!!,
                timestamp = now,
                centerX = smoothedBox!!.centerX(),
                centerY = smoothedBox!!.centerY(),
                aspectRatio = aspectRatio,
            )
        )
        if (frameHistory.size > maxHistorySize) frameHistory.removeAt(0)

        val behavior = analyzeBehavior()
        var emotion = inferEmotion(behavior)
        var description = generateDescription(petType, behavior, emotion)

        // 每隔5帧运行一次情绪分类器
        moodFrameCounter++
        if (moodFrameCounter % 5 == 0) {
            try {
                val cropped = cropPetRegion(bitmap, smoothedBox!!)
                if (cropped != null) {
                    lastMoodResult = moodClassifier.classify(cropped)
                }
            } catch (_: Exception) {}
        }

        // MobileNet 情绪覆盖：当行为是静止姿态时，用分类器置信度微调情绪
        val stillBehaviors = setOf(PetBehaviorType.SITTING, PetBehaviorType.STANDING,
            PetBehaviorType.LYING_DOWN, PetBehaviorType.SLEEPING, PetBehaviorType.LOOKING)
        if (behavior in stillBehaviors && lastMoodResult.topScore > 0f) {
            when {
                lastMoodResult.topScore < 0.3f -> {
                    emotion = Emotion.SAD
                    description = listOf(
                        "主人...我今天有点不开心...",
                        "唔...心情不太好，想要主人抱抱...",
                        "主人，我是不是哪里做得不好...",
                        "没什么精神...主人可以陪陪我吗...",
                    ).random()
                }
                lastMoodResult.topScore < 0.5f -> {
                    emotion = Emotion.CURIOUS
                    description = listOf(
                        "嗯？主人你在做什么呀？",
                        "有点好奇...主人今天好像不太一样~",
                        "歪着头看主人，在想什么呢？",
                    ).random()
                }
            }
        }

        // 检测到可爱行为时触发快照
        val cuteBehaviors = setOf(PetBehaviorType.HEAD_TILT, PetBehaviorType.PLAYING, PetBehaviorType.APPROACHING)
        if (behavior in cuteBehaviors && pet.categories.first().score > 0.4f) {
            onSnapshotTrigger?.invoke(fullBitmap)
        }

        return AnalysisResult(
            hasPet = true,
            petType = petType,
            behavior = behavior,
            confidence = pet.categories.first().score,
            description = description,
            emotion = emotion,
            boundingBox = BoundingBox(
                smoothedBox!!.left, smoothedBox!!.top,
                smoothedBox!!.width(), smoothedBox!!.height()
            ),
        )
    }

    private fun smoothBoundingBox(raw: RectF): RectF {
        val prev = smoothedBox
        if (prev == null) return RectF(raw)

        return RectF(
            prev.left + smoothingAlpha * (raw.left - prev.left),
            prev.top + smoothingAlpha * (raw.top - prev.top),
            prev.right + smoothingAlpha * (raw.right - prev.right),
            prev.bottom + smoothingAlpha * (raw.bottom - prev.bottom),
        )
    }

    private fun findBestPet(detections: List<Detection>): Detection? {
        return detections
            .filter { d ->
                d.categories.any { c ->
                    val label = c.label?.trim()?.lowercase() ?: ""
                    label == "cat" || label == "dog" || label.startsWith("cat") || label.startsWith("dog")
                }
            }
            .maxByOrNull { d -> d.categories.first().score }
    }

    private fun classifyPetType(detection: Detection): PetType {
        val label = detection.categories.first().label?.trim()?.lowercase() ?: ""
        return when {
            label.contains("cat") -> PetType.CAT
            label.contains("dog") -> PetType.DOG
            else -> PetType.UNKNOWN
        }
    }

    @OptIn(ExperimentalGetImage::class)
    fun imageProxyToFullBitmap(imageProxy: ImageProxy): Bitmap? {
        val image = imageProxy.image ?: return null
        val rotation = imageProxy.imageInfo.rotationDegrees
        val planes = image.planes

        val yPlane = planes[0]
        val uPlane = planes[1]
        val vPlane = planes[2]

        val yBuf = yPlane.buffer
        val uBuf = uPlane.buffer
        val vBuf = vPlane.buffer
        val yRowStride = yPlane.rowStride
        val uRowStride = uPlane.rowStride
        val vRowStride = vPlane.rowStride
        val uPixelStride = uPlane.pixelStride
        val vPixelStride = vPlane.pixelStride

        val width = image.width
        val height = image.height
        val pixels = IntArray(width * height)

        for (y in 0 until height) {
            val yRowStart = y * yRowStride
            val uvRow = y / 2
            val uRowStart = uvRow * uRowStride
            val vRowStart = uvRow * vRowStride

            for (x in 0 until width) {
                val yIdx = yRowStart + x
                val uvCol = x / 2
                val uIdx = uRowStart + uvCol * uPixelStride
                val vIdx = vRowStart + uvCol * vPixelStride

                val Y = (yBuf.get(yIdx).toInt() and 0xFF).coerceIn(16, 235)
                val U = (uBuf.get(uIdx).toInt() and 0xFF).coerceIn(16, 240) - 128
                val V = (vBuf.get(vIdx).toInt() and 0xFF).coerceIn(16, 240) - 128

                val r = (1.164f * Y + 1.596f * V).toInt().coerceIn(0, 255)
                val g = (1.164f * Y - 0.392f * U - 0.813f * V).toInt().coerceIn(0, 255)
                val b = (1.164f * Y + 2.017f * U).toInt().coerceIn(0, 255)

                pixels[y * width + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        var bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)

        if (rotation != 0) {
            val matrix = Matrix()
            matrix.postRotate(rotation.toFloat())
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }

        return bitmap
    }

    // ==================== 行为分析核心 ====================

    private fun analyzeBehavior(): PetBehaviorType {
        if (frameHistory.size < 3) return PetBehaviorType.SITTING

        val recent = frameHistory.takeLast(8)

        // 1. 计算位移数据
        val movements = mutableListOf<Float>()
        val velocitiesX = mutableListOf<Float>()
        val velocitiesY = mutableListOf<Float>()

        for (i in 1 until recent.size) {
            val dx = recent[i].centerX - recent[i - 1].centerX
            val dy = recent[i].centerY - recent[i - 1].centerY
            movements.add(sqrt((dx * dx + dy * dy).toDouble()).toFloat())
            velocitiesX.add(dx)
            velocitiesY.add(dy)
        }

        if (movements.isEmpty()) return currentBehavior

        val avgMovement = movements.average().toFloat()

        // 方向变化次数（用于检测玩耍）
        var directionChanges = 0
        if (velocitiesX.size >= 3) {
            for (i in 1 until velocitiesX.size) {
                if (velocitiesX[i] * velocitiesX[i - 1] < 0) directionChanges++
            }
        }

        // 2. 计算面积变化（靠近/远离）
        var isApproaching = false
        if (frameHistory.size >= 8) {
            val oldBox = frameHistory[frameHistory.size - 8].smoothedBox
            val newBox = frameHistory.last().smoothedBox
            val oldArea = oldBox.width() * oldBox.height()
            val newArea = newBox.width() * newBox.height()
            if (oldArea > 0f && newArea / oldArea > AREA_APPROACH_RATIO) {
                isApproaching = true
            }
        }

        // 3. 计算近期平均宽高比
        val recentAspectRatios = recent.map { it.aspectRatio }
        val avgAspectRatio = recentAspectRatios.average().toFloat()

        // 宽高比的稳定性（标准差小 = 姿态稳定）
        val aspectMean = avgAspectRatio
        val aspectVariance = if (recentAspectRatios.size > 1) {
            recentAspectRatios.map { (it - aspectMean) * (it - aspectMean) }.average().toFloat()
        } else 0f

        // 4. 分类逻辑
        val candidate = when {
            // 睡觉：整个历史窗口（30帧 ≈ 3秒）位移极小
            frameHistory.size >= maxHistorySize && isSleeping() ->
                PetBehaviorType.SLEEPING

            // 靠近：面积持续增长且有明显移动
            isApproaching && avgMovement > 2f ->
                PetBehaviorType.APPROACHING

            // 奔跑：快速移动
            avgMovement > MOVEMENT_RUN ->
                PetBehaviorType.RUNNING

            // 玩耍：方向频繁变化 + 有明显移动
            directionChanges >= 3 && avgMovement > 3f ->
                PetBehaviorType.PLAYING

            // 行走：中等速度移动，方向相对稳定
            avgMovement > MOVEMENT_WALK && directionChanges <= 1 ->
                PetBehaviorType.WALKING

            // 低速但有方向变化 → 玩耍（小范围活动）
            avgMovement > 2.5f && directionChanges >= 2 ->
                PetBehaviorType.PLAYING

            // 静止状态：根据宽高比区分站/坐/躺
            avgMovement <= MOVEMENT_SLEEP && aspectVariance < 0.15f -> {
                when {
                    avgAspectRatio > ASPECT_STANDING -> PetBehaviorType.STANDING
                    avgAspectRatio < ASPECT_LYING -> PetBehaviorType.LYING_DOWN
                    else -> PetBehaviorType.SITTING
                }
            }

            // 微动状态 → 可能是看着你或在做小动作
            avgMovement <= 3f && avgMovement > MOVEMENT_SLEEP -> {
                when {
                    avgAspectRatio > ASPECT_STANDING -> PetBehaviorType.STANDING
                    avgAspectRatio < ASPECT_LYING -> PetBehaviorType.LYING_DOWN
                    else -> PetBehaviorType.LOOKING
                }
            }

            else -> PetBehaviorType.SITTING
        }

        // 5. 滞后机制：行为需要连续确认才切换
        return applyHysteresis(candidate)
    }

    private fun isSleeping(): Boolean {
        val allMovements = mutableListOf<Float>()
        for (i in 1 until frameHistory.size) {
            val dx = frameHistory[i].centerX - frameHistory[i - 1].centerX
            val dy = frameHistory[i].centerY - frameHistory[i - 1].centerY
            allMovements.add(sqrt((dx * dx + dy * dy).toDouble()).toFloat())
        }
        // 整个窗口平均位移极小，且最大位移也不大
        return allMovements.average() < 0.8f && allMovements.maxOrNull()!! < 2.5f
    }

    private fun applyHysteresis(candidate: PetBehaviorType): PetBehaviorType {
        if (candidate == currentBehavior) {
            behaviorConfidence = (behaviorConfidence + 1).coerceAtMost(behaviorConfirmationFrames * 2)
            return currentBehavior
        }

        behaviorConfidence--
        if (behaviorConfidence <= 0) {
            behaviorConfidence = 1
            // 对于静止姿态（站/坐/躺），切换更谨慎
            val stillPoses = setOf(PetBehaviorType.SITTING, PetBehaviorType.STANDING, PetBehaviorType.LYING_DOWN)
            if (candidate in stillPoses && currentBehavior in stillPoses) {
                // 静止姿态间切换需要 2 帧确认
                behaviorConfidence = -1
                return currentBehavior
            }
            currentBehavior = candidate
        }
        return currentBehavior
    }

    // ==================== 情绪推断 ====================

    private fun inferEmotion(behavior: PetBehaviorType): Emotion {
        return when (behavior) {
            PetBehaviorType.SLEEPING -> Emotion.SLEEPY
            PetBehaviorType.PLAYING, PetBehaviorType.RUNNING -> Emotion.EXCITED
            PetBehaviorType.APPROACHING, PetBehaviorType.WALKING -> Emotion.HAPPY
            PetBehaviorType.EATING, PetBehaviorType.DRINKING -> Emotion.HAPPY
            PetBehaviorType.HEAD_TILT -> Emotion.CURIOUS
            PetBehaviorType.STRETCHING -> Emotion.SLEEPY
            PetBehaviorType.SITTING, PetBehaviorType.LOOKING -> Emotion.NEUTRAL
            PetBehaviorType.STANDING -> Emotion.NEUTRAL
            PetBehaviorType.LYING_DOWN -> Emotion.SLEEPY
            PetBehaviorType.BARKING -> Emotion.EXCITED
            PetBehaviorType.SCRATCHING -> Emotion.NEUTRAL
            else -> Emotion.NEUTRAL
        }
    }

    // ==================== 拟人化第一人称描述 ====================

    private fun generateDescription(petType: PetType, behavior: PetBehaviorType, emotion: Emotion): String {
        val typePrefix = when (petType) {
            PetType.CAT -> "喵~ "
            PetType.DOG -> "汪! "
            else -> ""
        }

        val messages = when (behavior) {
            PetBehaviorType.SITTING -> listOf(
                "主人，我乖乖坐在这里等你呢！",
                "哼哼～我就想陪在主人身边~",
                "主人，你看我乖不乖呀？",
                "坐得好端正，等主人夸我~",
            )
            PetBehaviorType.STANDING -> listOf(
                "主人你看，我站得好直吧！",
                "我在帮主人站岗呢！",
                "站得高高的，这样才能看清主人~",
                "随时准备跟主人出发！",
            )
            PetBehaviorType.LYING_DOWN -> listOf(
                "好舒服呀～就想这么躺着~",
                "主人，一起躺会儿嘛~",
                "躺平好幸福，嘿嘿~",
                "地板凉凉的，趴着超舒服~",
            )
            PetBehaviorType.SLEEPING -> listOf(
                "呼噜...主人别吵我...我想睡觉觉...",
                "做梦梦到主人了...嘿嘿...💤",
                "好困哦...让窝再睡一会儿嘛...",
                "呼...呼...（睡得超香）",
            )
            PetBehaviorType.WALKING -> listOf(
                "我在散步呢，主人要一起来吗？",
                "到处走走看看，帮主人巡逻~",
                "今天的天气真好呀~心情也不错~",
                "慢悠悠地走走，享受生活~",
            )
            PetBehaviorType.RUNNING -> listOf(
                "好开心呀！跑起来真舒服！",
                "主人追我呀！来追我呀！",
                "哈哈哈哈哈停不下来！太快乐了！",
                "飞奔中！速度与激情！",
            )
            PetBehaviorType.PLAYING -> listOf(
                "好好玩！主人陪我一起玩嘛！",
                "你看这个！超好玩的！",
                "嘿嘿嘿我可太开心了！",
                "主人你看我会这个！厉害吧！",
                "蹦蹦跳跳，开心到飞起！",
            )
            PetBehaviorType.APPROACHING -> listOf(
                "主人我过来啦！想我了吗！",
                "主人~主人~我来找你玩啦！",
                "小碎步跑来见主人！",
                "主人！我来啦！开心！",
            )
            PetBehaviorType.HEAD_TILT -> listOf(
                "嗯？主人在说什么呀？",
                "歪头杀！主人被可爱到了吗？",
                "唔？不懂...但是主人开心就好~",
            )
            PetBehaviorType.EATING -> listOf(
                "好吃好吃！主人最好了！",
                "本宝宝在享用美食呢~",
                "好好吃呀～幸福就是这么简单~",
            )
            PetBehaviorType.DRINKING -> listOf(
                "喝点水～要健康生活！",
                "渴了渴了，吨吨吨~",
                "嗯嗯嗯好喝！",
            )
            PetBehaviorType.SCRATCHING -> listOf(
                "身上痒痒的...挠一挠舒服~",
                "好痒哦～挠挠挠~",
                "挠痒痒时间到！",
            )
            PetBehaviorType.STRETCHING -> listOf(
                "伸个懒腰～舒服~",
                "刚睡醒，活动一下~嘿嘿~",
                "舒展一下身体，好惬意呀~",
            )
            PetBehaviorType.BARKING -> listOf(
                "汪汪！主人看我！看我！",
                "我在跟主人说话呢！",
                "汪！想引起主人的注意~",
            )
            PetBehaviorType.LOOKING -> listOf(
                "主人，我在看你呢~",
                "嘿嘿，主人好好看呀~",
                "一直盯着主人，好喜欢主人~",
            )
            PetBehaviorType.UNKNOWN -> listOf(
                "嗯..在想一些猫生大事~",
                "嘿嘿，我现在好开心呀！",
                "主人你知道吗，我真的好喜欢你！",
            )
        }

        return typePrefix + messages.random()
    }

    private fun cropPetRegion(bitmap: Bitmap, box: RectF): Bitmap? {
        val x = box.left.toInt().coerceIn(0, bitmap.width - 1)
        val y = box.top.toInt().coerceIn(0, bitmap.height - 1)
        val w = box.width().toInt().coerceIn(1, bitmap.width - x)
        val h = box.height().toInt().coerceIn(1, bitmap.height - y)
        return Bitmap.createBitmap(bitmap, x, y, w, h)
    }

    fun reset() {
        frameHistory.clear()
        smoothedBox = null
        currentBehavior = PetBehaviorType.UNKNOWN
        behaviorConfidence = 0
        moodFrameCounter = 0
    }
}
