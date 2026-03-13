package com.example.brainana.data.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class GameTheme(
    val label: String,
    val apiPath: String,
    val primary: Color,
    val secondary: Color,
    val bg: Color,
    val icon: ImageVector
) {
    NEURAL(
        "NEURAL",
        "banana",
        Color(0xFF00D1FF),
        Color(0xFF8B5CF6),
        Color(0xFF020204),
        Icons.Rounded.Psychology
    ),
    HEROES(
        "HEROES",
        "tomato",
        Color(0xFFE23636),
        Color(0xFF1877F2),
        Color(0xFF0A0E17),
        Icons.Rounded.Bolt
    ),
    PRINCESS(
        "PRINCESS",
        "smile",
        Color(0xFFFF69B4),
        Color(0xFFDDA0DD),
        Color(0xFF2A0826),
        Icons.Rounded.AutoAwesome
    )
}