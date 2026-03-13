package com.example.brainana.utils

import androidx.compose.ui.graphics.Color

fun Color.asHex(): String {
    val color = this.value.toLong()
    return "#${String.format("%08X", color)}"
}

fun String.toColorOrDefault(): Color {
    return try {
        Color(this.toLong(16))
    } catch (e: Exception) {
        Color.White
    }
}