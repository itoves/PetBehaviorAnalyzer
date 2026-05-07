package com.petanalyzer.analyzer

/**
 * 宠物行为类型枚举
 */
enum class PetBehaviorType(val label: String, val emoji: String) {
    UNKNOWN("未知行为", "❓"),
    SITTING("坐着呢", "🪑"),
    STANDING("站着", "🧍"),
    LYING_DOWN("躺着休息", "🛋️"),
    SLEEPING("睡得好香", "💤"),
    WALKING("在散步", "🚶"),
    RUNNING("跑得好欢", "🏃"),
    PLAYING("在玩耍", "🎾"),
    EATING("在吃东西", "🍽️"),
    DRINKING("在喝水", "💧"),
    STRETCHING("伸懒腰", "🥱"),
    LOOKING("正看着你", "👀"),
    APPROACHING("走过来了", "🚶‍➡️"),
    HEAD_TILT("歪头杀", "🥺"),
    SCRATCHING("在挠痒痒", "🐾"),
    BARKING("在叫唤", "📢"),
}
