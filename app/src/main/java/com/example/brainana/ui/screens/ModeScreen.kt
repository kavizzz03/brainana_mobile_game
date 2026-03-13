package com.example.brainana.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brainana.data.models.Mode
import com.example.brainana.ui.viewmodel.GameViewModel
import com.example.brainana.ui.viewmodel.Screen

@Composable
fun ModeScreen(vm: GameViewModel) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "INTENSITY SELECTION",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.height(32.dp))

        Mode.entries.forEach { mode ->
            Surface(
                onClick = {
                    vm.selectedMode = mode
                    vm.currentScore = 0
                    vm.fetchNewPuzzle()
                    vm.navigateTo(Screen.PLAYING)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                color = vm.selectedTheme.primary.copy(0.1f),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(
                    1.dp,
                    vm.selectedTheme.primary.copy(0.4f)
                )
            ) {
                Row(
                    Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            mode.name,
                            color = vm.selectedTheme.primary,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                        Text(
                            mode.desc,
                            color = Color.White.copy(0.5f),
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        "${mode.bonus}X",
                        color = vm.selectedTheme.primary,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}