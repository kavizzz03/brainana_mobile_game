package com.example.brainana.data.models

enum class Mode(
    val time: Long,
    val bonus: Int,
    val desc: String
) {
    EASY(20000L, 1, "Standard Processing"),
    MEDIUM(12000L, 2, "Accelerated Pace"),
    HARD(7000L, 4, "Overclocked Protocol")
}