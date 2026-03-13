package com.example.brainana.data.models

import androidx.compose.ui.graphics.Color

enum class Level(val levelNum: Int, val minXp: Int, val color: Color) {
    LVL_1(1, 0, Color(0xFF4ADE80)),
    LVL_2(2, 500, Color(0xFF3B82F6)),
    LVL_3(3, 1000, Color(0xFF8B5CF6)),
    LVL_4(4, 1500, Color(0xFFEC4899)),
    LVL_5(5, 2000, Color(0xFFFFD700)),
    LVL_6(6, 2500, Color(0xFF00D1FF)),
    LVL_7(7, 3000, Color(0xFFF97316)),
    LVL_8(8, 3500, Color(0xFF06B6D4)),
    LVL_9(9, 4000, Color(0xFF10B981)),
    LVL_10(10, 4500, Color(0xFFEF4444));

    companion object {
        fun fromXp(xp: Int) = entries.lastOrNull { xp >= it.minXp } ?: LVL_1

        fun getNextLevel(currentLevel: Level): Level? {
            val nextIndex = entries.indexOf(currentLevel) + 1
            return if (nextIndex < entries.size) entries[nextIndex] else null
        }

        fun xpToNextLevel(xp: Int): Int {
            val currentLevel = fromXp(xp)
            val nextLevel = getNextLevel(currentLevel)
            return nextLevel?.minXp?.minus(xp) ?: 0
        }
    }
}

data class LevelUpEvent(val previousLevel: Level, val newLevel: Level)