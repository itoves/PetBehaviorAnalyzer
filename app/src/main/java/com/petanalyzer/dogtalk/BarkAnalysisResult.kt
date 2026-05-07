package com.petanalyzer.dogtalk

/**
 * 狗叫声分析结果
 */
data class BarkAnalysisResult(
    val detectedBark: BarkCategory = BarkCategory.UNKNOWN,
    val emotion: BarkEmotion = BarkEmotion.NEUTRAL,
    val interpretation: String = "等待分析狗狗的叫声...",
    val suggestion: String = "",
    val confidence: Float = 0f,
    val details: BarkDetails = BarkDetails(),
) {
    val hasResult: Boolean get() = detectedBark != BarkCategory.UNKNOWN
}

data class BarkDetails(
    val averageAmplitude: Float = 0f,
    val zeroCrossingRate: Float = 0f,
    val peakFrequency: Float = 0f,
    val duration: Float = 0f,
    val energyLevel: EnergyLevel = EnergyLevel.MEDIUM,
)

enum class BarkCategory(val label: String, val emoji: String) {
    UNKNOWN("未知", "❓"),
    HAPPY_BARK("开心叫", "😊"),
    ALERT_BARK("警戒叫", "🚨"),
    ANXIOUS_BARK("焦虑叫", "😰"),
    PLAYFUL_BARK("玩耍叫", "🎾"),
    GREETING_BARK("问候叫", "👋"),
}

enum class BarkEmotion(val label: String, val emoji: String) {
    NEUTRAL("平静", "😌"),
    EXCITED("兴奋", "🤩"),
    CALM("放松", "😊"),
    ANXIOUS("焦虑", "😟"),
    FRIENDLY("友善", "🥰"),
}

enum class EnergyLevel(val label: String) {
    LOW("低"), MEDIUM("中"), HIGH("高"),
}
