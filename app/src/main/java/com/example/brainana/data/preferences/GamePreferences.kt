package com.example.brainana.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.brainana.data.models.GameTheme

class GamePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("brain_v4_prefs", Context.MODE_PRIVATE)

    // Player Data
    fun getPlayerName(): String = prefs.getString("name", "Agent Guest") ?: "Agent Guest"
    fun setPlayerName(name: String) = prefs.edit().putString("name", name).apply()

    fun getHighScore(): Int = prefs.getInt("high", 0)
    fun setHighScore(score: Int) = prefs.edit().putInt("high", score).apply()

    fun getTotalXp(): Int = prefs.getInt("xp", 0)
    fun setTotalXp(xp: Int) = prefs.edit().putInt("xp", xp).apply()

    fun getLevel(): Int = prefs.getInt("level", 1)
    fun setLevel(level: Int) = prefs.edit().putInt("level", level).apply()

    // Avatar & Theme
    fun getAvatarStyle(): String = prefs.getString("style", "bottts") ?: "bottts"
    fun setAvatarStyle(style: String) = prefs.edit().putString("style", style).apply()

    fun getTheme(): String = prefs.getString("theme", GameTheme.NEURAL.name) ?: GameTheme.NEURAL.name
    fun setTheme(theme: String) = prefs.edit().putString("theme", theme).apply()

    // First Launch
    fun isFirstLaunch(): Boolean = prefs.getBoolean("is_first_launch", true)
    fun completeFirstLaunch() = prefs.edit().putBoolean("is_first_launch", false).apply()
}