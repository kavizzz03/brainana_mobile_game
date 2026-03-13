package com.example.brainana.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.brainana.data.models.Level
import com.example.brainana.data.models.Rank
import com.example.brainana.ui.components.GlassButton
import com.example.brainana.ui.theme.GlassSurface
import com.example.brainana.ui.theme.VividRose
import com.example.brainana.ui.viewmodel.GameViewModel

@Composable
fun ProfileScreen(vm: GameViewModel) {
    val context = LocalContext.current
    val rank = Rank.fromXp(vm.player.totalEarnings)
    val level = Level.fromXp(vm.player.totalEarnings)
    val avatarUrl = "https://api.dicebear.com/9.x/${vm.player.avatarStyle}/png?seed=${vm.player.name}&size=256"

    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Surface(
                Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .border(3.dp, vm.selectedTheme.primary, CircleShape)
                    .background(GlassSurface)
            ) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
            }
            Surface(
                Modifier.size(40.dp),
                color = vm.selectedTheme.primary,
                shape = CircleShape
            ) {
                Icon(
                    rank.icon,
                    null,
                    Modifier.padding(8.dp),
                    tint = vm.selectedTheme.bg
                )
            }
        }

        Text(
            vm.player.name,
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(top = 24.dp)
        )
        Text(
            rank.label,
            color = vm.selectedTheme.primary,
            fontWeight = FontWeight.ExtraBold,
            //letterSpacing = androidx.compose.ui.unit.sp(6)
        )

        Spacer(Modifier.height(50.dp))
        StatRow("TOTAL XP", "${vm.player.totalEarnings} XP")
        StatRow("LEVEL", "LEVEL ${level.levelNum}")
        StatRow("CLEARANCE", if (vm.player.isGuest) "GUEST" else "VERIFIED")

        Spacer(Modifier.weight(1f))
        GlassButton(
            "DISCONNECT",
            Icons.Rounded.Logout,
            VividRose,
            vm.selectedTheme.bg
        ) { vm.signOut(context) }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White.copy(0.4f), fontSize = 13.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold)
    }
    HorizontalDivider(color = Color.White.copy(0.05f))
}