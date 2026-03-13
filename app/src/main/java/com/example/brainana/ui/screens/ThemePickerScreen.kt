package com.example.brainana.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
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
import com.example.brainana.data.models.GameTheme
import com.example.brainana.ui.components.GlassButton
import com.example.brainana.ui.theme.GlassSurface
import com.example.brainana.ui.theme.GlassBorder
import com.example.brainana.ui.viewmodel.GameViewModel

@Composable
fun ThemePickerScreen(vm: GameViewModel) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "CHOOSE YOUR VIBE",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            "This sets your interface and puzzles.",
            color = Color.White.copy(0.5f),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        GameTheme.entries.forEach { theme ->
            val isSelected = vm.selectedTheme == theme
            Surface(
                onClick = { vm.updateTheme(theme) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                color = if (isSelected) theme.primary.copy(0.2f) else GlassSurface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(
                    2.dp,
                    if (isSelected) theme.primary else GlassBorder
                )
            ) {
                Row(
                    Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        theme.icon,
                        null,
                        tint = theme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        theme.label,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSelected) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = theme.primary)
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        GlassButton(
            if (vm.isFirstLaunch) "CONTINUE" else "SAVE",
            Icons.Rounded.ArrowForward,
            vm.selectedTheme.primary,
            vm.selectedTheme.bg
        ) {
            if (vm.isFirstLaunch) {
                vm.navigateTo(com.example.brainana.ui.viewmodel.Screen.INSTRUCTIONS)
            } else {
                vm.goBack()
            }
        }
    }
}