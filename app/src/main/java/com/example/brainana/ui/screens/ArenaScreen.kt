package com.example.brainana.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.brainana.data.models.Mode
import com.example.brainana.ui.components.GlassButton
import com.example.brainana.ui.components.TacticalKeypad
import com.example.brainana.ui.components.FailureOverlay
import com.example.brainana.ui.theme.GlassSurface
import com.example.brainana.ui.theme.GlassBorder
import com.example.brainana.ui.theme.VividRose
import com.example.brainana.ui.viewmodel.GameViewModel
import com.example.brainana.ui.viewmodel.Screen
import kotlinx.coroutines.delay

@Composable
fun ArenaScreen(vm: GameViewModel) {
    var timerProgress by remember { mutableFloatStateOf(1f) }
    var showFailureOverlay by remember { mutableStateOf(false) }
    var lastWrongAnswer by remember { mutableIntStateOf(-1) }

    LaunchedEffect(vm.puzzleUrl, vm.isPaused) {
        if (vm.puzzleUrl.isNotEmpty() && !vm.isPaused) {
            timerProgress = 1f
            while (timerProgress > 0 && !vm.isPaused) {
                delay(50)
                timerProgress -= 50f / vm.selectedMode.time
            }
            if (timerProgress <= 0) {
                vm.handleTimeout()
                vm.submitAnswer("-1", true)
                vm.fetchNewPuzzle()
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        // Timer Progress Bar
        LinearProgressIndicator(
            progress = { timerProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = if (timerProgress < 0.3f) VividRose else vm.selectedTheme.primary,
            trackColor = Color.White.copy(0.1f)
        )

        // Score and Pause Button
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "SCORE: ${vm.currentScore}",
                color = vm.selectedTheme.primary,
                fontSize = 22.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Black
            )
            IconButton(
                onClick = { vm.isPaused = true },
                modifier = Modifier.background(GlassSurface, CircleShape)
            ) {
                Icon(Icons.Rounded.Pause, null, tint = Color.White)
            }
        }

        // Puzzle Display
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            color = GlassSurface,
            shape = RoundedCornerShape(32.dp),
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (vm.isLoading) {
                    CircularProgressIndicator(color = vm.selectedTheme.primary)
                } else {
                    AsyncImage(
                        model = vm.puzzleUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Answer Keypad
        TacticalKeypad(vm.selectedTheme.primary) { input ->
            if (input.toIntOrNull() != vm.solution && input != "-1") {
                // ========== WRONG ANSWER - SHOW OVERLAY WITHOUT CORRECT ANSWER ==========
                showFailureOverlay = true
                lastWrongAnswer = vm.solution
            }
            vm.submitAnswer(input)
        }
    }

    // ========== PAUSE OVERLAY ==========
    if (vm.isPaused) {
        Box(
            Modifier
                .fillMaxSize()
                .background(vm.selectedTheme.bg.copy(0.95f))
                .clickable(enabled = false) {},
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "PROCESS SUSPENDED",
                    color = Color.White,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                    fontSize = 26.sp
                )
                Spacer(Modifier.height(40.dp))

                GlassButton(
                    "RESUME",
                    Icons.Rounded.PlayArrow,
                    vm.selectedTheme.primary,
                    vm.selectedTheme.bg
                ) { vm.isPaused = false }

                Spacer(Modifier.height(12.dp))

                GlassButton(
                    "ABORT GAME",
                    Icons.Rounded.Close,
                    VividRose,
                    vm.selectedTheme.bg
                ) {
                    vm.isPaused = false
                    vm.abortGame()
                    vm.navigateTo(Screen.HOME)
                }
            }
        }
    }

    // ========== FAILURE OVERLAY (Wrong Answer) ==========
    // ✅ NOW: Only shows "INCORRECT ANSWER" - NOT the correct answer
    if (showFailureOverlay) {
        FailureOverlay(
            showCorrectAnswer = false,  // ✅ Don't show correct answer
            correctAnswer = lastWrongAnswer,
            currentScore = vm.currentScore,
            scorePenalty = 5,
            onDismiss = { showFailureOverlay = false }
        )
    }
}