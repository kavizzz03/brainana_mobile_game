package com.example.brainana.data.models

data class Player(
    val uid: String = "",
    val name: String = "Agent Guest",
    val highScore: Int = 0,
    val totalEarnings: Int = 0,
    val photoUrl: String = "",
    val avatarStyle: String = "bottts",
    val isGuest: Boolean = true,
    val level: Int = 1
)