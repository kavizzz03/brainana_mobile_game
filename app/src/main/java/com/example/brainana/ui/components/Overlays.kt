package com.example.brainana.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import com.example.brainana.data.models.LevelUpEvent
import com.example.brainana.data.models.Rank
import com.example.brainana.data.models.GameTheme
import com.example.brainana.ui.theme.NeonGold
import com.example.brainana.ui.theme.VividRose
import kotlinx.coroutines.delay

@Composable
fun AuthProgressOverlay(
    progress: Float,
    statusMessage: String,
    theme: GameTheme
) {
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(progress) {
        animatedProgress.animateTo(
            progress,
            animationSpec = tween(durationMillis = 500, easing = LinearEasing)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.bg.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(32.dp)
                .width(250.dp)
        ) {
            // Circular Progress Indicator
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .size(120.dp),
                    shape = CircleShape,
                    color = theme.primary.copy(alpha = 0.1f),
                    border = BorderStroke(2.dp, theme.primary.copy(alpha = 0.3f))
                ) {}

                // Rotating circle border
                val rotation = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    rotation.animateTo(
                        360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        )
                    )
                }

                Surface(
                    modifier = Modifier
                        .size(120.dp)
                        .graphicsLayer {
                            rotationZ = rotation.value
                        },
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = BorderStroke(
                        3.dp,
                        brush = androidx.compose.ui.graphics.Brush.sweepGradient(
                            colors = listOf(
                                theme.primary.copy(alpha = 0.8f),
                                theme.primary.copy(alpha = 0f)
                            )
                        )
                    )
                ) {}

                // Center percentage
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Text(
                        "${(animatedProgress.value * 100).toInt()}%",
                        color = theme.primary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Linear Progress Bar
            LinearProgressIndicator(
                progress = animatedProgress.value,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp)),
                color = theme.primary,
                trackColor = Color.White.copy(alpha = 0.1f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Status Message
            Text(
                statusMessage,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Sub-message
            Text(
                "Neural sync in progress...",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Animated dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                repeat(3) { index ->
                    val animScale = remember { Animatable(0.6f) }

                    LaunchedEffect(Unit) {
                        delay(index * 150L)
                        animScale.animateTo(
                            1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600, easing = EaseInOutQuad),
                                repeatMode = RepeatMode.Reverse
                            )
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .size(8.dp)
                            .graphicsLayer {
                                scaleX = animScale.value
                                scaleY = animScale.value
                            },
                        shape = CircleShape,
                        color = theme.primary.copy(alpha = animScale.value)
                    ) {}
                }
            }
        }
    }
}

@Composable
fun RankUpOverlay(rank: Rank, onDismiss: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.95f))
            .clickable { onDismiss() },
        Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "PROMOTION SECURED",
                color = rank.color,
                letterSpacing = 8.sp,
                fontSize = 12.sp
            )
            Text(
                rank.label,
                color = Color.White,
                fontSize = 56.sp,
                fontWeight = FontWeight.Black
            )
            Icon(
                rank.icon,
                null,
                modifier = Modifier.size(140.dp),
                tint = rank.color
            )
            Text(
                "ACCESS GRANTED",
                color = Color.White.copy(0.4f),
                modifier = Modifier.padding(top = 20.dp)
            )
        }
    }
}

@Composable
fun LevelUpOverlay(event: LevelUpEvent, onDismiss: () -> Unit) {
    val animScale = remember { Animatable(0.5f) }
    val animAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animScale.animateTo(1f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f))
        animAlpha.animateTo(1f, animationSpec = tween(300))
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.95f))
            .clickable { onDismiss() },
        Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                scaleX = animScale.value
                scaleY = animScale.value
                alpha = animAlpha.value
            }
        ) {
            Text(
                "🚀 LEVEL UP! 🚀",
                color = event.newLevel.color,
                letterSpacing = 6.sp,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = event.previousLevel.color.copy(0.2f),
                        modifier = Modifier.size(80.dp),
                        border = BorderStroke(2.dp, event.previousLevel.color)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "${event.previousLevel.levelNum}",
                                color = event.previousLevel.color,
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("BEFORE", color = Color.White.copy(0.5f), fontSize = 10.sp)
                }

                Icon(
                    Icons.Rounded.TrendingUp,
                    null,
                    tint = NeonGold,
                    modifier = Modifier.size(48.dp)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = event.newLevel.color.copy(0.2f),
                        modifier = Modifier.size(80.dp),
                        border = BorderStroke(2.dp, event.newLevel.color)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "${event.newLevel.levelNum}",
                                color = event.newLevel.color,
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("NOW", color = Color.White.copy(0.5f), fontSize = 10.sp)
                }
            }

            Spacer(Modifier.height(32.dp))
            Text(
                "Keep grinding to reach Level ${event.newLevel.levelNum + 1}!",
                color = Color.White.copy(0.6f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ConnectionOverlay(theme: GameTheme, onRetry: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(theme.bg)
            .padding(32.dp),
        Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Rounded.CloudOff,
                null,
                Modifier.size(100.dp),
                tint = VividRose
            )
            Text(
                "NETWORK SEVERED",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 24.sp
            )
            Spacer(Modifier.height(40.dp))
            GlassButton(
                "RETRY SYNC",
                Icons.Rounded.Refresh,
                theme.primary,
                theme.bg
            ) { onRetry() }
        }
    }
}