package com.petanalyzer.analyzer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * YOLOv8-Pose 姿态检测器
 * 用于检测狗狗的关键点（头、颈、四肢、尾巴等）
 */
class YoloPoseDetector(context: Context) {

    private var interpreter: Interpreter? = null
    private val inputSize = 640  // YOLOv8 默认输入尺寸
    private val numKeypoints = 17  // COCO 格式关键点数量（可根据实际模型调整）

    // 狗狗关键点索引（基于 COCO 格式，需要根据实际训练的模型调整）
    // 如果使用 AP-10K 或自定义数据集，关键点定义会不同
    object KeypointIndex {
        const val NOSE = 0
        const val LEFT_EYE = 1
        const val RIGHT_EYE = 2
        const val LEFT_EAR = 3
        const val RIGHT_EAR = 4
        const val NECK = 5  // 或 shoulder center
        const val FRONT_LEFT_PAW = 6
        const val FRONT_RIGHT_PAW = 7
        const val BACK_LEFT_PAW = 8
        const val BACK_RIGHT_PAW = 9
        const val TAIL_BASE = 10
        const val TAIL_END = 11
    }

    data class Keypoint(
        val x: Float,
        val y: Float,
        val confidence: Float
    )

    data class PoseResult(
        val keypoints: List<Keypoint>,
        val boundingBox: RectF,
        val confidence: Float
    )

    init {
        try {
            // 尝试加载 YOLOv8-Pose 模型
            // 注意：需要先将模型文件放到 assets 目录
            val modelFile = FileUtil.loadMappedFile(context, "yolov8n-pose.tflite")
            val options = Interpreter.Options().apply {
                setNumThreads(4)
                // 如果有 GPU 代理，可以启用
                // addDelegate(GpuDelegate())
            }
            interpreter = Interpreter(modelFile, options)
        } catch (e: Exception) {
            // 模型文件不存在时的处理
            interpreter = null
        }
    }

    /**
     * 检测图像中的狗狗姿态
     */
    fun detectPose(bitmap: Bitmap): PoseResult? {
        if (interpreter == null) return null

        // 1. 预处理图像
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val inputBuffer = bitmapToByteBuffer(resizedBitmap)

        // 2. 准备输出缓冲区
        // YOLOv8-Pose 输出格式: [1, 56, 8400]
        // 56 = 4 (bbox) + 1 (confidence) + 51 (17 keypoints * 3)
        val outputShape = intArrayOf(1, 56, 8400)
        val outputBuffer = Array(1) { Array(56) { FloatArray(8400) } }

        // 3. 运行推理
        interpreter?.run(inputBuffer, outputBuffer)

        // 4. 解析输出
        return parseOutput(outputShape, outputBuffer, bitmap.width.toFloat(), bitmap.height.toFloat())
    }

    /**
     * 将 Bitmap 转换为 ByteBuffer
     */
    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputSize * inputSize)
        bitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        for (pixel in pixels) {
            // 归一化到 [0, 1]
            val r = ((pixel shr 16) and 0xFF) / 255.0f
            val g = ((pixel shr 8) and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f

            byteBuffer.putFloat(r)
            byteBuffer.putFloat(g)
            byteBuffer.putFloat(b)
        }

        return byteBuffer
    }

    /**
     * 解析 YOLO 输出
     */
    private fun parseOutput(
        shape: IntArray,
        output: Array<Array<FloatArray>>,
        originalWidth: Float,
        originalHeight: Float
    ): PoseResult? {
        val detections = mutableListOf<Detection>()

        // 遍历所有检测框
        for (i in 0 until shape[2]) {
            val confidence = output[0][4][i]

            // 置信度阈值过滤
            if (confidence < 0.5f) continue

            // 提取边界框 (cx, cy, w, h)
            val cx = output[0][0][i]
            val cy = output[0][1][i]
            val w = output[0][2][i]
            val h = output[0][3][i]

            // 提取关键点
            val keypoints = mutableListOf<Keypoint>()
            for (k in 0 until numKeypoints) {
                val kx = output[0][5 + k * 3][i]
                val ky = output[0][5 + k * 3 + 1][i]
                val kconf = output[0][5 + k * 3 + 2][i]

                keypoints.add(Keypoint(kx, ky, kconf))
            }

            detections.add(Detection(
                boundingBox = RectF(
                    (cx - w / 2) * originalWidth / inputSize,
                    (cy - h / 2) * originalHeight / inputSize,
                    (cx + w / 2) * originalWidth / inputSize,
                    (cy + h / 2) * originalHeight / inputSize
                ),
                confidence = confidence,
                keypoints = keypoints
            ))
        }

        // 返回置信度最高的检测结果
        val best = detections.maxByOrNull { it.confidence } ?: return null

        return PoseResult(
            keypoints = best.keypoints,
            boundingBox = best.boundingBox,
            confidence = best.confidence
        )
    }

    private data class Detection(
        val boundingBox: RectF,
        val confidence: Float,
        val keypoints: List<Keypoint>
    )

    /**
     * 基于关键点分析姿态
     */
    fun analyzePoseFromKeypoints(poseResult: PoseResult): PostureAnalysis {
        val kps = poseResult.keypoints

        // 提取关键点（需要根据实际模型的关键点定义调整）
        val nose = kps.getOrNull(KeypointIndex.NOSE)
        val neck = kps.getOrNull(KeypointIndex.NECK)
        val frontLeftPaw = kps.getOrNull(KeypointIndex.FRONT_LEFT_PAW)
        val frontRightPaw = kps.getOrNull(KeypointIndex.FRONT_RIGHT_PAW)
        val backLeftPaw = kps.getOrNull(KeypointIndex.BACK_LEFT_PAW)
        val backRightPaw = kps.getOrNull(KeypointIndex.BACK_RIGHT_PAW)

        // 计算身体角度和姿态特征
        val bodyAngle = calculateBodyAngle(neck, backLeftPaw, backRightPaw)
        val legExtension = calculateLegExtension(frontLeftPaw, frontRightPaw, backLeftPaw, backRightPaw)
        val headHeight = calculateHeadHeight(nose, neck, backLeftPaw, backRightPaw)

        // 姿态判断逻辑
        val posture = when {
            // 站立：身体接近水平，腿伸展，头部较高
            bodyAngle in -30f..30f && legExtension > 0.6f && headHeight > 0.7f ->
                PetBehaviorType.STANDING

            // 躺下：身体接近水平，腿不伸展，头部低
            bodyAngle in -30f..30f && legExtension < 0.3f && headHeight < 0.4f ->
                PetBehaviorType.LYING_DOWN

            // 坐着：身体有一定角度，前腿伸展但后腿弯曲
            bodyAngle in 30f..70f && legExtension in 0.3f..0.6f ->
                PetBehaviorType.SITTING

            else -> PetBehaviorType.SITTING
        }

        return PostureAnalysis(
            posture = posture,
            confidence = poseResult.confidence,
            bodyAngle = bodyAngle,
            legExtension = legExtension,
            headHeight = headHeight
        )
    }

    /**
     * 计算身体角度（相对于水平线）
     */
    private fun calculateBodyAngle(
        neck: Keypoint?,
        backLeftPaw: Keypoint?,
        backRightPaw: Keypoint?
    ): Float {
        if (neck == null || (backLeftPaw == null && backRightPaw == null)) return 0f

        val backPaw = backLeftPaw ?: backRightPaw ?: return 0f

        val dx = backPaw.x - neck.x
        val dy = backPaw.y - neck.y

        return Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    }

    /**
     * 计算腿部伸展程度
     */
    private fun calculateLegExtension(
        frontLeft: Keypoint?,
        frontRight: Keypoint?,
        backLeft: Keypoint?,
        backRight: Keypoint?
    ): Float {
        val validKeypoints = listOfNotNull(frontLeft, frontRight, backLeft, backRight)
        if (validKeypoints.size < 2) return 0.5f

        // 计算四肢的平均置信度和分散程度
        val avgConfidence = validKeypoints.map { it.confidence }.average().toFloat()

        // 计算前后腿的垂直距离（站立时距离大，躺下时距离小）
        val frontY = listOfNotNull(frontLeft, frontRight).map { it.y }.average().toFloat()
        val backY = listOfNotNull(backLeft, backRight).map { it.y }.average().toFloat()
        val verticalSpread = kotlin.math.abs(frontY - backY)

        return (verticalSpread / 100f).coerceIn(0f, 1f) * avgConfidence
    }

    /**
     * 计算头部高度（相对于身体）
     */
    private fun calculateHeadHeight(
        nose: Keypoint?,
        neck: Keypoint?,
        backLeft: Keypoint?,
        backRight: Keypoint?
    ): Float {
        if (nose == null) return 0.5f

        val bodyY = listOfNotNull(neck, backLeft, backRight).map { it.y }.average().toFloat()
        val headBodyDiff = bodyY - nose.y  // Y 轴向下为正

        return (headBodyDiff / 100f).coerceIn(0f, 1f)
    }

    data class PostureAnalysis(
        val posture: PetBehaviorType,
        val confidence: Float,
        val bodyAngle: Float,
        val legExtension: Float,
        val headHeight: Float
    )

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
