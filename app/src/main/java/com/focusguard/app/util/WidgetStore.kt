package com.focusguard.app.util

import android.content.Context
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Tiny SharedPreferences store shared by the app and the home-screen widgets.
 * (Widgets can't easily read DataStore/Room, so the values they show live here.)
 */
object WidgetStore {
    private const val FILE = "focusguard_widget"
    private const val K_EXAM = "exam_epoch_day"
    private const val K_START = "prep_start_epoch_day"
    private const val K_REELS = "reels_today"
    private const val K_REELS_DAY = "reels_day"
    private const val K_FOCUS = "focus_today_min"
    private const val K_FOCUS_DAY = "focus_day"

    /** CLAT exam date: 6 December 2026 (change it any time in Settings). */
    val DEFAULT_EXAM: LocalDate = LocalDate.of(2026, 12, 6)

    private fun p(c: Context) = c.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun examDate(c: Context): LocalDate =
        LocalDate.ofEpochDay(p(c).getLong(K_EXAM, DEFAULT_EXAM.toEpochDay()))

    fun setExamDate(c: Context, date: LocalDate) {
        p(c).edit().putLong(K_EXAM, date.toEpochDay()).apply()
    }

    private fun startDate(c: Context): LocalDate {
        val prefs = p(c)
        if (!prefs.contains(K_START)) {
            prefs.edit().putLong(K_START, LocalDate.now().toEpochDay()).apply()
        }
        return LocalDate.ofEpochDay(prefs.getLong(K_START, LocalDate.now().toEpochDay()))
    }

    fun daysLeft(c: Context): Int =
        ChronoUnit.DAYS.between(LocalDate.now(), examDate(c)).toInt().coerceAtLeast(0)

    /** 0..100 – how much of the time between you starting prep and the exam has passed. */
    fun prepProgress(c: Context): Int {
        val start = startDate(c)
        val total = ChronoUnit.DAYS.between(start, examDate(c)).coerceAtLeast(1)
        val done = ChronoUnit.DAYS.between(start, LocalDate.now()).coerceIn(0, total)
        return (done * 100 / total).toInt()
    }

    fun setReelsToday(c: Context, count: Int) {
        p(c).edit().putInt(K_REELS, count).putLong(K_REELS_DAY, LocalDate.now().toEpochDay()).apply()
    }

    fun reelsToday(c: Context): Int {
        val prefs = p(c)
        return if (prefs.getLong(K_REELS_DAY, -1) == LocalDate.now().toEpochDay()) prefs.getInt(K_REELS, 0) else 0
    }

    fun setFocusMinutesToday(c: Context, minutes: Long) {
        p(c).edit().putLong(K_FOCUS, minutes).putLong(K_FOCUS_DAY, LocalDate.now().toEpochDay()).apply()
    }

    fun focusMinutesToday(c: Context): Long {
        val prefs = p(c)
        return if (prefs.getLong(K_FOCUS_DAY, -1) == LocalDate.now().toEpochDay()) prefs.getLong(K_FOCUS, 0L) else 0L
    }
}
