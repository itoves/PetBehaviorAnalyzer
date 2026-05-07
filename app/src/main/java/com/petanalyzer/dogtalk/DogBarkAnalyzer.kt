package com.petanalyzer.dogtalk

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * 狗叫声录音和分析引擎
 */
class DogBarkAnalyzer(private val context: Context) {

    private val sampleRate = 44100
    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    private val _result = MutableStateFlow(BarkAnalysisResult())
    val result: StateFlow<BarkAnalysisResult> = _result.asStateFlow()

    private val _isRecordingState = MutableStateFlow(false)
    val isRecordingState: StateFlow<Boolean> = _isRecordingState.asStateFlow()

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
    }

    /**
     * 开始录音分析，最长录制3秒
     */
    suspend fun startRecording() = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext

        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(sampleRate) // 至少1秒缓冲

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
            )

            audioRecord?.startRecording()
            isRecording = true
            _isRecordingState.value = true

            val buffer = ShortArray(bufferSize)
            val allSamples = mutableListOf<Short>()
            val maxDurationSamples = sampleRate * 3 // 最多3秒

            while (isRecording && allSamples.size < maxDurationSamples) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    allSamples.addAll(buffer.take(read))
                }
            }

            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            _isRecordingState.value = false

            if (allSamples.isNotEmpty()) {
                val analysis = analyzeSamples(allSamples.toShortArray())
                _result.value = analysis
            }
        } catch (e: SecurityException) {
            _isRecordingState.value = false
            _result.value = BarkAnalysisResult(
                interpretation = "需要录音权限才能分析狗狗的叫声哦~",
                suggestion = "请在设置中授予录音权限",
            )
        } catch (e: Exception) {
            _isRecordingState.value = false
            _result.value = BarkAnalysisResult(
                interpretation = "录音出了点小问题，请再试一次~",
                suggestion = "确保麦克风没有被其他应用占用",
            )
        }
    }

    fun stopRecording() {
        isRecording = false
    }

    fun reset() {
        _result.value = BarkAnalysisResult()
    }

    private fun analyzeSamples(samples: ShortArray): BarkAnalysisResult {
        if (samples.isEmpty()) {
            return BarkAnalysisResult(interpretation = "没有检测到声音呢...", suggestion = "请靠近狗狗再试一次~")
        }

        val duration = samples.size.toFloat() / sampleRate

        // 1. 计算 RMS 振幅
        val sumSq = samples.map { (it.toLong() * it.toLong()) }.sum()
        val rms = sqrt(sumSq.toDouble() / samples.size).toFloat()
        val normalizedRms = (rms / Short.MAX_VALUE).coerceIn(0f, 1f)

        // 2. 零交叉率（用于音高估计）
        var zeroCrossings = 0
        for (i in 1 until samples.size) {
            if ((samples[i] >= 0 && samples[i - 1] < 0) ||
                (samples[i] < 0 && samples[i - 1] >= 0)) {
                zeroCrossings++
            }
        }
        val zcr = zeroCrossings.toFloat() / samples.size

        // 3. 能量分段的方差（判断节奏模式）
        val segmentSize = sampleRate / 10 // 每 100ms 一段
        val segments = samples.toList().chunked(segmentSize)
        val segmentEnergies = segments.map { seg ->
            seg.map { (it.toLong() * it.toLong()).toDouble() }.average().toFloat()
        }
        val energyVariance = if (segmentEnergies.size > 1) {
            val mean = segmentEnergies.average().toFloat()
            segmentEnergies.map { (it - mean) * (it - mean) }.average().toFloat()
        } else 0f

        // 4. 估计峰值频率（简化FFT：寻找最大自相关）
        val peakFreq = estimatePeakFrequency(samples)

        // 5. 能量分级
        val energyLevel = when {
            normalizedRms < 0.05f -> EnergyLevel.LOW
            normalizedRms > 0.25f -> EnergyLevel.HIGH
            else -> EnergyLevel.MEDIUM
        }

        // 6. 分类逻辑
        val (category, emotion) = classifyBark(normalizedRms, zcr, peakFreq, duration, energyVariance)

        // 7. 生成解读
        val (interpretation, suggestion) = generateInterpretation(category, emotion, energyLevel)

        return BarkAnalysisResult(
            detectedBark = category,
            emotion = emotion,
            interpretation = interpretation,
            suggestion = suggestion,
            confidence = (normalizedRms * 100).coerceIn(0f, 100f),
            details = BarkDetails(
                averageAmplitude = normalizedRms,
                zeroCrossingRate = zcr,
                peakFrequency = peakFreq,
                duration = duration,
                energyLevel = energyLevel,
            ),
        )
    }

    private fun estimatePeakFrequency(samples: ShortArray): Float {
        // 使用自相关法估计基频
        val windowSize = samples.size / 4
        val correlations = FloatArray(windowSize)
        val base = samples.take(windowSize)

        for (lag in 1 until windowSize) {
            var sum = 0f
            for (i in 0 until windowSize - lag) {
                sum += base[i] * base[i + lag]
            }
            correlations[lag] = sum
        }

        // 找到第一个峰值（跳过 lag=0）
        var maxCorr = 0f
        var bestLag = 0
        for (lag in 1 until windowSize - 1) {
            if (correlations[lag] > correlations[lag - 1] && correlations[lag] > correlations[lag + 1]) {
                if (correlations[lag] > maxCorr) {
                    maxCorr = correlations[lag]
                    bestLag = lag
                }
            }
        }

        return if (bestLag > 0) sampleRate.toFloat() / bestLag else 0f
    }

    private fun classifyBark(
        rms: Float, zcr: Float, peakFreq: Float, duration: Float, energyVar: Float,
    ): Pair<BarkCategory, BarkEmotion> {
        return when {
            // 高能量 + 高频率 + 短促 → 开心/兴奋叫
            rms > 0.2f && zcr > 0.15f && duration < 1.0f ->
                BarkCategory.HAPPY_BARK to BarkEmotion.EXCITED

            // 高能量 + 低频率 + 长持续 → 警戒叫
            rms > 0.25f && peakFreq in 100f..400f && duration > 0.8f ->
                BarkCategory.ALERT_BARK to BarkEmotion.NEUTRAL

            // 低能量 + 短促 + 高频率 → 焦虑叫
            rms < 0.1f && zcr > 0.12f && duration < 0.8f ->
                BarkCategory.ANXIOUS_BARK to BarkEmotion.ANXIOUS

            // 中高能量 + 能量方差大 + 节奏感 → 玩耍叫
            rms > 0.12f && energyVar > 0.01f && duration > 0.5f ->
                BarkCategory.PLAYFUL_BARK to BarkEmotion.FRIENDLY

            // 中等能量 + 短促 → 问候叫
            rms > 0.08f && duration < 0.6f ->
                BarkCategory.GREETING_BARK to BarkEmotion.FRIENDLY

            // 低能量 + 长持续 → 焦虑/放松
            rms < 0.08f && duration > 1.0f ->
                BarkCategory.ANXIOUS_BARK to BarkEmotion.ANXIOUS

            else -> BarkCategory.GREETING_BARK to BarkEmotion.NEUTRAL
        }
    }

    private fun generateInterpretation(
        category: BarkCategory, emotion: BarkEmotion, energy: EnergyLevel,
    ): Pair<String, String> {
        val interpretations = when (category) {
            BarkCategory.HAPPY_BARK -> listOf(
                "汪汪汪！好开心呀！狗狗看到喜欢的东西了！",
                "欢快的叫声！狗狗现在心情超好的~",
                "尾巴一定摇得像螺旋桨一样！太高兴了！",
            )
            BarkCategory.ALERT_BARK -> listOf(
                "呜——汪！狗狗在警戒，可能听到了什么声音...",
                "低沉的警告叫声，狗狗在保护自己的领地！",
                "狗狗发现了异常情况，正在提醒主人注意~",
            )
            BarkCategory.ANXIOUS_BARK -> listOf(
                "嗷呜...狗狗好像有点焦虑不安...",
                "连续的低鸣声，狗狗可能需要主人的安慰~",
                "狗狗感到孤独或害怕了，快去陪陪它吧！",
            )
            BarkCategory.PLAYFUL_BARK -> listOf(
                "汪！汪汪！来玩呀！狗狗在邀请你一起玩！",
                "活泼的叫声夹杂着跳跃声，狗狗想跟你互动~",
                "玩得正开心呢！狗狗活力满满！",
            )
            BarkCategory.GREETING_BARK -> listOf(
                "汪汪~ 狗狗在跟你打招呼呢！",
                "温柔的问候叫声，狗狗认出你来啦~",
                "一声亲切的招呼，狗狗说'你好呀主人'！",
            )
            BarkCategory.UNKNOWN -> listOf(
                "嗯...这次叫声的特征不太明显呢~",
                "狗狗可能只是随意叫了一声~",
            )
        }

        val suggestions = when (category) {
            BarkCategory.HAPPY_BARK -> "可以陪狗狗多玩一会儿，或者奖励一些小零食！"
            BarkCategory.ALERT_BARK -> "去看看狗狗在关注什么，确认环境是否安全~"
            BarkCategory.ANXIOUS_BARK -> "轻轻抚摸狗狗，用温柔的声音安慰它，让它感到安心~"
            BarkCategory.PLAYFUL_BARK -> "拿起玩具跟狗狗互动吧，它正想跟你玩呢！"
            BarkCategory.GREETING_BARK -> "也跟狗狗打个招呼吧！摸摸头或者给个小零食~"
            BarkCategory.UNKNOWN -> "再试一次，或者换个安静的环境录音~"
        }

        return interpretations.random() to suggestions
    }
}
