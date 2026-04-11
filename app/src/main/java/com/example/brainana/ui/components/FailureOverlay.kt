package com.example.brainana.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
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
import com.example.brainana.ui.theme.VividRose
import kotlinx.coroutines.delay

@Composable
fun FailureOverlay(
    showCorrectAnswer: Boolean = false,  // New parameter
    correctAnswer: Int = -1,
    currentScore: Int,
    scorePenalty: Int = 5,
    onDismiss: () -> Unit
) {
    val animScale = remember { Animatable(0.3f) }
    val animAlpha = remember { Animatable(0f) }
    val shakeAnim = remember { Animatable(0f) }
    val penaltyScale = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Entrance animation
        animScale.animateTo(1f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f))
        animAlpha.animateTo(1f, animationSpec = tween(300))

        // Shake animation for impact
        delay(100)
        repeat(4) {
            shakeAnim.animateTo(if (it % 2 == 0) 10f else -10f, animationSpec = tween(50))
        }
        shakeAnim.animateTo(0f, animationSpec = tween(50))

        // Penalty scale animation
        delay(300)
        penaltyScale.animateTo(1f, animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f))
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.92f))
            .clickable(enabled = false) {},
        Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = animScale.value
                    scaleY = animScale.value
                    alpha = animAlpha.value
                    translationX = shakeAnim.value
                }
                .padding(32.dp)
        ) {
            // Failed Icon Circle
            Surface(
                shape = CircleShape,
                color = VividRose.copy(0.2f),
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape),
                border = BorderStroke(3.dp, VividRose)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Close,
                        null,
                        modifier = Modifier.size(80.dp),
                        tint = VividRose
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Main Fail Message
            Text(
                "INCORRECT ANSWER",
                color = VividRose,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            // ========== CONDITIONAL: SHOW CORRECT ANSWER OR NOT ==========
            if (showCorrectAnswer) {
                // Correct Answer Display (Only when instructed)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(0.1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp)),
                    border = BorderStroke(2.dp, Color.White.copy(0.3f))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            "CORRECT ANSWER WAS:",
                            color = Color.White.copy(0.6f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "$correctAnswer",
                            color = Color.White,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }

            // Score Penalty Box
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = VividRose.copy(0.15f),
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = penaltyScale.value
                        scaleY = penaltyScale.value
                    }
                    .clip(RoundedCornerShape(16.dp)),
                border = BorderStroke(2.dp, VividRose)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "Score Penalty: ",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "-$scorePenalty",
                        color = VividRose,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Current Score Display
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Current Score: ",
                    color = Color.White.copy(0.7f),
                    fontSize = 14.sp
                )
                Text(
                    "$currentScore",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(Modifier.height(40.dp))

            // Dismiss Button
            GlassButton(
                text = "CONTINUE",
                icon = Icons.Rounded.Close,
                color = VividRose,
                textColor = Color.White,
                onClick = onDismiss
            )
        }
    }
}