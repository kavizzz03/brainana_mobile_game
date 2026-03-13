package com.example.brainana.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brainana.ui.components.GlassButton
import com.example.brainana.ui.viewmodel.GameViewModel
import com.example.brainana.ui.viewmodel.Screen

@Composable
fun InstructionsScreen(vm: GameViewModel) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Rounded.MenuBook,
            null,
            Modifier.size(80.dp),
            tint = vm.selectedTheme.primary
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "HOW TO PLAY",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.height(32.dp))

        InstructionStep("1", "Observe the puzzle image on the screen carefully.", vm.selectedTheme.primary)
        InstructionStep("2", "Identify the mathematical pattern or logic sequence.", vm.selectedTheme.primary)
        InstructionStep("3", "Use the tactical keypad to enter the final missing number.", vm.selectedTheme.primary)
        InstructionStep("4", "Solve it before the timer runs out to gain XP and Level UP!", vm.selectedTheme.primary)

        Spacer(Modifier.height(40.dp))
        GlassButton(
            "I'M READY",
            Icons.Rounded.PlayArrow,
            vm.selectedTheme.primary,
            vm.selectedTheme.bg
        ) {
            vm.completeFirstLaunch()
        }
    }
}

@Composable
fun InstructionStep(number: String, text: String, color: Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            Modifier.size(40.dp),
            shape = CircleShape,
            color = color.copy(0.2f),
            border = BorderStroke(1.dp, color)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    number,
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Text(
            text,
            color = Color.White.copy(0.8f),
            fontSize = 14.sp
        )
    }
}