package com.focusguard.app.domain.model

/** Platforms that host short-form video content BrainPal tracks */
enum class ShortVideoPlatform(
    val packageName: String,
    val displayName: String,
    val emoji: String
) {
    INSTAGRAM("com.instagram.android",         "Instagram Reels",  "📸"),
    YOUTUBE(  "com.google.android.youtube",    "YouTube Shorts",   "▶️"),
    TIKTOK(   "com.zhiliaoapp.musically",      "TikTok",           "🎵"),
    TIKTOK_IN("com.ss.android.ugc.trill",      "TikTok",           "🎵"),
    SNAPCHAT( "com.snapchat.android",          "Snapchat Spotlight","👻"),
    FACEBOOK( "com.facebook.katana",           "Facebook Reels",   "📘"),
    MXTAKATAK("com.mx.takatak",               "MX TakaTak",       "🎬"),
}

/** Brain degradation level based on reel count */
enum class BrainState(val level: Int, val label: String, val emoji: String, val color: Long) {
    PRISTINE(  0, "Brain is Fresh!",       "🧠✨", 0xFF66BB6A),  // green
    GOOD(      1, "Still Sharp",           "🧠",   0xFF26C6DA),  // cyan
    MODERATE(  2, "Getting Foggy",         "🌫️🧠", 0xFFFFCA28),  // amber
    DEGRADED(  3, "Brain Rot Setting In",  "🤯",   0xFFFF7043),  // orange
    CRITICAL(  4, "Total Brain Rot!",      "💀",   0xFFEF5350),  // red
}

fun Int.toBrainState(): BrainState = when {
    this == 0        -> BrainState.PRISTINE
    this < 10        -> BrainState.GOOD
    this < 25        -> BrainState.MODERATE
    this < 50        -> BrainState.DEGRADED
    else             -> BrainState.CRITICAL
}

data class ScrollBattle(
    val id: Long = 0,
    val myName: String,
    val opponentName: String,
    val myScrollCount: Int,
    val opponentScrollCount: Int,
    val date: String,
    val isWinning: Boolean = myScrollCount <= opponentScrollCount
)

data class ScrollSession(
    val packageName: String,
    val platform: ShortVideoPlatform?,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val estimatedScrolls: Int
)
