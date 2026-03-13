package com.example.brainana.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.brainana.ui.components.GlassButton
import com.example.brainana.ui.theme.GlassSurface
import com.example.brainana.ui.theme.GlassBorder
import com.example.brainana.ui.viewmodel.GameViewModel

@Composable
fun AvatarSelectScreen(vm: GameViewModel) {
    val styles = listOf(
        "bottts", "bottts-neutral", "adventurer", "avataaars",
        "micah", "lorelei", "pixel-art", "notionists"
    )

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text(
            "SELECT PERSONA",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.height(20.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(styles) { style ->
                val isSelected = vm.player.avatarStyle == style
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            if (isSelected) vm.selectedTheme.primary.copy(0.2f)
                            else GlassSurface
                        )
                        .border(
                            2.dp,
                            if (isSelected) vm.selectedTheme.primary else GlassBorder,
                            RoundedCornerShape(24.dp)
                        )
                        .clickable { vm.updateAvatar(style) },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = "https://api.dicebear.com/9.x/$style/png?seed=${vm.player.name}&size=256",
                        contentDescription = null,
                        modifier = Modifier.size(80.dp)
                    )
                }
            }
        }

        GlassButton(
            "CONFIRM",
            Icons.Rounded.Check,
            vm.selectedTheme.primary,
            vm.selectedTheme.bg
        ) { vm.goBack() }
    }
}