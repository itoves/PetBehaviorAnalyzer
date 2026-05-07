package com.petanalyzer.analyzer

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.Detection
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * 宠物姿态检测器 - 基于关键点的姿态分析
 * 用于更准确地判断站/坐/躺等姿态
 */
class PetPoseDetector {

    data class PoseKeypoints(
        val nose: PointF?,
        val neck: PointF?,
        val frontLeftPaw: PointF?,
        val frontRightPaw: PointF?,
        val backLeftPaw: PointF?,
        val backRightPaw: PointF?,
        val tail: PointF?,
        val confidence: Float
    )

    /**
     * 从检测框中提取姿态特征
     * 由于我们使用的是目标检测模型而非姿态估计模型，
     * 这里通过检测框的几何特征来推断姿态
     */
    fun analyzePoseFromBoundingBox(
        box: RectF,
        aspectRatio: Float,
        recentBoxes: List<RectF>
    ): PetBehaviorType {

        // 1. 基于宽高比的初步判断
        val basePosture = when {
            aspectRatio > 1.4f -> PostureHint.VERTICAL  // 明显竖直 → 站立
            aspectRatio < 0.65f -> PostureHint.HORIZONTAL  // 明显水平 → 躺下
            else -> PostureHint.NEUTRAL  // 中间状态 → 坐着
        }

        // 2. 检测框形状稳定性
        val shapeStability = calculateShapeStability(recentBoxes)

        // 3. 检测框重心位置（相对于框的位置）
        val centerBias = analyzeCenterBias(box)

        // 4. 综合判断
        return inferPosture(basePosture, shapeStability, centerBias, aspectRatio)
    }

    /**
     * 增强版姿态分析 - 结合多帧数据
     */
    fun analyzeEnhancedPosture(
        currentBox: RectF,
        aspectRatio: Float,
        recentBoxes: List<RectF>,
        recentAspectRatios: List<Float>
    ): PostureAnalysis {

        // 计算宽高比的变化趋势
        val aspectTrend = if (recentAspectRatios.size >= 3) {
            val recent = recentAspectRatios.takeLast(3)
            (recent.last() - recent.first()) / recent.first()
        } else 0f

        // 检测框面积变化
        val areaChange = if (recentBoxes.size >= 2) {
            val oldArea = recentBoxes[recentBoxes.size - 2].width() * recentBoxes[recentBoxes.size - 2].height()
            val newArea = currentBox.width() * currentBox.height()
            if (oldArea > 0f) (newArea - oldArea) / oldArea else 0f
        } else 0f

        // 宽高比标准差（姿态稳定性）
        val aspectStdDev = if (recentAspectRatios.size > 1) {
            val mean = recentAspectRatios.average().toFloat()
            sqrt(recentAspectRatios.map { (it - mean) * (it - mean) }.average().toFloat())
        } else 0f

        // 改进的姿态判断逻辑
        val posture = when {
            // 站立：高宽比大，且相对稳定
            aspectRatio > 1.35f && aspectStdDev < 0.2f -> PetBehaviorType.STANDING

            // 躺下：高宽比小，形状稳定
            aspectRatio < 0.7f && aspectStdDev < 0.15f -> PetBehaviorType.LYING_DOWN

            // 从站到躺的过渡：宽高比快速下降
            aspectTrend < -0.15f && aspectRatio < 1.0f -> PetBehaviorType.LYING_DOWN

            // 从躺到站的过渡：宽高比快速上升
            aspectTrend > 0.15f && aspectRatio > 1.0f -> PetBehaviorType.STANDING

            // 坐着：中等宽高比，稳定
            aspectRatio in 0.85f..1.35f && aspectStdDev < 0.18f -> PetBehaviorType.SITTING

            // 默认坐着
            else -> PetBehaviorType.SITTING
        }

        return PostureAnalysis(
            posture = posture,
            confidence = calculatePostureConfidence(aspectRatio, aspectStdDev),
            aspectRatio = aspectRatio,
            aspectStability = 1f - aspectStdDev.coerceIn(0f, 1f),
            aspectTrend = aspectTrend
        )
    }

    /**
     * 计算姿态置信度
     */
    private fun calculatePostureConfidence(aspectRatio: Float, stability: Float): Float {
        // 宽高比越极端（很大或很小），置信度越高
        val aspectConfidence = when {
            aspectRatio > 1.5f || aspectRatio < 0.6f -> 0.9f  // 明显的站或躺
            aspectRatio > 1.3f || aspectRatio < 0.75f -> 0.75f  // 较明显
            else -> 0.5f  // 模糊区域
        }

        // 稳定性越高，置信度越高
        val stabilityConfidence = (1f - stability.coerceIn(0f, 0.3f) / 0.3f)

        return (aspectConfidence * 0.7f + stabilityConfidence * 0.3f).coerceIn(0f, 1f)
    }

    /**
     * 计算形状稳定性
     */
    private fun calculateShapeStability(boxes: List<RectF>): Float {
        if (boxes.size < 3) return 0.5f

        val recent = boxes.takeLast(5)
        val aspectRatios = recent.map {
            if (it.width() > 0f) it.height() / it.width() else 1f
        }

        val mean = aspectRatios.average().toFloat()
        val variance = aspectRatios.map { (it - mean) * (it - mean) }.average().toFloat()

        // 方差越小，稳定性越高
        return (1f - variance.coerceIn(0f, 0.5f) / 0.5f)
    }

    /**
     * 分析重心偏移
     * 狗狗坐着时重心偏下，站立时重心居中，躺下时重心更偏下
     */
    private fun analyzeCenterBias(box: RectF): Float {
        val centerY = box.centerY()
        val boxHeight = box.height()

        // 返回重心相对位置 (0 = 顶部, 0.5 = 中间, 1 = 底部)
        return if (boxHeight > 0f) {
            (centerY - box.top) / boxHeight
        } else 0.5f
    }

    /**
     * 综合推断姿态
     */
    private fun inferPosture(
        hint: PostureHint,
        stability: Float,
        centerBias: Float,
        aspectRatio: Float
    ): PetBehaviorType {
        return when (hint) {
            PostureHint.VERTICAL -> {
                // 站立姿态：重心应该相对居中
                if (stability > 0.6f) PetBehaviorType.STANDING
                else PetBehaviorType.SITTING
            }
            PostureHint.HORIZONTAL -> {
                // 躺下姿态：形状应该稳定
                if (stability > 0.5f) PetBehaviorType.LYING_DOWN
                else PetBehaviorType.SITTING
            }
            PostureHint.NEUTRAL -> {
                // 坐姿：根据细微特征判断
                when {
                    aspectRatio > 1.15f && stability > 0.6f -> PetBehaviorType.STANDING
                    aspectRatio < 0.8f && stability > 0.5f -> PetBehaviorType.LYING_DOWN
                    else -> PetBehaviorType.SITTING
                }
            }
        }
    }

    enum class PostureHint {
        VERTICAL,    // 竖直方向为主
        HORIZONTAL,  // 水平方向为主
        NEUTRAL      // 中间状态
    }

    data class PostureAnalysis(
        val posture: PetBehaviorType,
        val confidence: Float,
        val aspectRatio: Float,
        val aspectStability: Float,
        val aspectTrend: Float
    )
}
