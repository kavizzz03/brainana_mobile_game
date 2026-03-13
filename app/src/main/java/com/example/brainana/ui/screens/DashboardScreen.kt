package com.example.brainana.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brainana.data.models.Level
import com.example.brainana.ui.components.GlassButton
import com.example.brainana.ui.components.SmallCircleButton
import com.example.brainana.ui.theme.NeonGold
import com.example.brainana.ui.viewmodel.GameViewModel
import com.example.brainana.ui.viewmodel.Screen

@Composable
fun DashboardScreen(vm: GameViewModel) {
    // Animation for floating score
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "y"
    )

    // Level calculations
    val currentLevel = Level.fromXp(vm.player.totalEarnings)
    val xpToNextLevel = Level.xpToNextLevel(vm.player.totalEarnings)
    val levelProgressPercentage = (vm.player.totalEarnings % 500) / 500f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // ========== PEAK SCORE SECTION ==========
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer { translationY = floatAnim }
        ) {
            Text(
                text = "PEAK SCORE",
                color = Color.White.copy(0.4f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontSize = 12.sp
            )
            Text(
                text = "${vm.player.highScore}",
                fontSize = 110.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 2.sp
            )
        }

        // ========== LEVEL DISPLAY CARD ==========
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .fillMaxWidth(),
            color = currentLevel.color.copy(0.1f),
            border = BorderStroke(2.dp, currentLevel.color),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Level Number and Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.TrendingUp,
                        contentDescription = "Level Icon",
                        tint = currentLevel.color,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "CURRENT LEVEL",
                            color = Color.White.copy(0.5f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${currentLevel.levelNum}",
                            color = currentLevel.color,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // XP Progress Bar
                LinearProgressIndicator(
                    progress = { levelProgressPercentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = currentLevel.color,
                    trackColor = Color.White.copy(0.1f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // XP to Next Level Text
                Text(
                    text = "$xpToNextLevel XP to next level",
                    color = Color.White.copy(0.4f),
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp
                )

                // Additional Level Info
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Total XP: ${vm.player.totalEarnings}",
                    color = currentLevel.color.copy(0.7f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // ========== ACTION BUTTONS SECTION ==========
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Launch Arena Button
            GlassButton(
                text = "LAUNCH ARENA",
                icon = Icons.Rounded.Bolt,
                color = vm.selectedTheme.primary,
                textColor = vm.selectedTheme.bg,
                onClick = { vm.navigateTo(Screen.MODES) }
            )

            // Quick Action Buttons Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leaderboard Button
                SmallCircleButton(
                    icon = Icons.Rounded.EmojiEvents,
                    color = NeonGold,
                    onClick = {
                        vm.getLeaderboard("highScore")
                        vm.navigateTo(Screen.LEADERBOARD)
                    }
                )

                // Avatar Selection Button
                SmallCircleButton(
                    icon = Icons.Rounded.Face,
                    color = Color.White,
                    onClick = {
                        vm.navigateTo(Screen.AVATAR_SELECT)
                    }
                )

                // Theme Picker Button
                SmallCircleButton(
                    icon = Icons.Rounded.Palette,
                    color = vm.selectedTheme.primary,
                    onClick = {
                        vm.navigateTo(Screen.THEME_PICKER)
                    }
                )

                // Profile Button
                SmallCircleButton(
                    icon = Icons.Rounded.Person,
                    color = Color.White,
                    onClick = {
                        vm.navigateTo(Screen.PROFILE)
                    }
                )
            }
        }
    }
}