package com.example.brainana.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import coil.compose.AsyncImage
import com.example.brainana.data.models.Level
import com.example.brainana.data.models.Player
import com.example.brainana.data.models.GameTheme
import com.example.brainana.ui.theme.GlassSurface
import com.example.brainana.ui.theme.GlassBorder
import com.example.brainana.ui.theme.NeonGold

@Composable
fun TopHUD(player: Player, theme: GameTheme) {
    val level = Level.fromXp(player.totalEarnings)
    val avatarUrl = "https://api.dicebear.com/9.x/${player.avatarStyle}/png?seed=${player.name}&size=128"

    Box(
        modifier = Modifier
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(GlassSurface)
                .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(theme.bg)
                    .border(1.5.dp, theme.primary, CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        player.name.uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.width(8.dp))

                    Surface(
                        shape = CircleShape,
                        color = level.color.copy(0.3f),
                        modifier = Modifier.size(24.dp),
                        border = BorderStroke(1.dp, level.color)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "${level.levelNum}",
                                color = level.color,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 8.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                LinearProgressIndicator(
                    progress = { (player.totalEarnings % 500) / 500f },
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(4.dp)
                        .clip(CircleShape),
                    color = level.color,
                    trackColor = Color.White.copy(0.05f)
                )
            }

            Text(
                "${player.totalEarnings} XP",
                color = NeonGold,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp
            )
        }
    }
}