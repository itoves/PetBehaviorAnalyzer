package com.petanalyzer.analyzer

/**
 * 分析结果数据类
 */
data class AnalysisResult(
    val hasPet: Boolean = false,
    val petType: PetType = PetType.UNKNOWN,
    val behavior: PetBehaviorType = PetBehaviorType.UNKNOWN,
    val confidence: Float = 0f,
    val description: String = "没有检测到小可爱呢 🐾",
    val emotion: Emotion = Emotion.NEUTRAL,
    val boundingBox: BoundingBox? = null,
)

enum class PetType(val label: String, val emoji: String) {
    UNKNOWN("未知", "❓"),
    CAT("猫猫", "🐱"),
    DOG("狗狗", "🐶"),
    BOTH("猫猫和狗狗", "🐱🐶"),
}

enum class Emotion(val label: String, val emoji: String) {
    NEUTRAL("平静", "😌"),
    HAPPY("开心", "😊"),
    EXCITED("兴奋", "🤩"),
    SLEEPY("困了", "😴"),
    CURIOUS("好奇", "🤔"),
    PLAYFUL("想玩", "😝"),
    SAD("不开心", "😢"),
}

data class BoundingBox(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
)
