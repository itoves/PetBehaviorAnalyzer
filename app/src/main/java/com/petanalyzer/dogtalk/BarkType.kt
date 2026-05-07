package com.petanalyzer.dogtalk

/**
 * 狗叫声播放类型 — 映射到 assets/sounds/ 下的真实音频文件
 */
enum class BarkType(
    val label: String,
    val emoji: String,
    val description: String,
    val assetFileName: String,
) {
    FRIENDLY(
        label = "友好问候",
        emoji = "🥰",
        description = "温柔的叫声，向狗狗表达友好",
        assetFileName = "sounds/friendly_bark.wav",
    ),
    PLAY_INVITE(
        label = "玩耍邀请",
        emoji = "🎾",
        description = "轻快活泼的叫声，邀请狗狗一起玩",
        assetFileName = "sounds/play_invite.wav",
    ),
    FOOD_CALL(
        label = "食物召唤",
        emoji = "🍖",
        description = "兴奋的叫声，引起狗狗对食物的兴趣",
        assetFileName = "sounds/food_call.wav",
    ),
    COME_HERE(
        label = "呼唤过来",
        emoji = "👋",
        description = "中频持续的叫声，呼唤狗狗靠近",
        assetFileName = "sounds/come_here.wav",
    ),
    WARNING(
        label = "警告制止",
        emoji = "⚠️",
        description = "低频有力的叫声，用于制止不良行为",
        assetFileName = "sounds/warning.wav",
    ),
    COMFORT(
        label = "安抚安慰",
        emoji = "😢",
        description = "柔和低沉的叫声，安抚焦虑的狗狗",
        assetFileName = "sounds/comfort.wav",
    ),
    PRAISE(
        label = "兴奋夸奖",
        emoji = "🤩",
        description = "高频欢快的叫声，表扬狗狗",
        assetFileName = "sounds/praise.wav",
    ),
    ATTENTION(
        label = "吸引注意",
        emoji = "🔔",
        description = "短促清脆的叫声，引起狗狗注意",
        assetFileName = "sounds/attention.wav",
    ),
}
