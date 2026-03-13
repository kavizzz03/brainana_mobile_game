package com.example.brainana.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.*
import com.example.brainana.data.models.GameTheme

@Composable
fun MeshGradientBackground(theme: GameTheme) {
    val infiniteTransition = rememberInfiniteTransition(label = "mesh")
    val offset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1500f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "x"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    theme.secondary.copy(0.12f),
                    Color.Transparent
                ),
                center = Offset(offset1, 300f),
                radius = 1000f
            )
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    theme.primary.copy(0.1f),
                    Color.Transparent
                ),
                center = Offset(size.width - offset1, size.height - 300f),
                radius = 1200f
            )
        )
    }
}