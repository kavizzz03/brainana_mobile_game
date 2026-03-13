package com.example.brainana.data.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class Rank(
    val label: String,
    val minXp: Int,
    val color: Color,
    val icon: ImageVector
) {
    BEGINNER("INITIATE", 0, Color(0xFF4ADE80), Icons.Rounded.RocketLaunch),
    PRO("OPERATIVE", 2500, Color(0xFF00D1FF), Icons.Rounded.VerifiedUser),
    LEGEND("ARCHITECT", 10000, Color(0xFF8B5CF6), Icons.Rounded.AllInclusive);

    companion object {
        fun fromXp(xp: Int) = entries.lastOrNull { xp >= it.minXp } ?: BEGINNER
    }
}