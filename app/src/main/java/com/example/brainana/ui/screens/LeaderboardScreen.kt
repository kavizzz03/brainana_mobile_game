package com.example.brainana.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brainana.data.models.Level
import com.example.brainana.ui.components.GlassButton
import com.example.brainana.ui.theme.GlassSurface
import com.example.brainana.ui.theme.GlassBorder
import com.example.brainana.ui.theme.NeonGold
import com.example.brainana.ui.viewmodel.GameViewModel

@Composable
fun LeaderboardScreen(vm: GameViewModel) {
    var tab by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text(
            "HALL OF FAME",
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Black
        )

        TabRow(
            selectedTabIndex = tab,
            containerColor = Color.Transparent,
            contentColor = vm.selectedTheme.primary,
            divider = {}
        ) {
            Tab(
                selected = tab == 0,
                onClick = { tab = 0; vm.getLeaderboard("highScore") }
            ) {
                Text("SCORE", Modifier.padding(16.dp))
            }
            Tab(
                selected = tab == 1,
                onClick = { tab = 1; vm.getLeaderboard("totalEarnings") }
            ) {
                Text("XP", Modifier.padding(16.dp))
            }
        }

        LazyColumn(Modifier.weight(1f).padding(top = 16.dp)) {
            itemsIndexed(vm.leaderboard) { i, p ->
                val playerLevel = Level.fromXp(p.totalEarnings)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(GlassSurface)
                        .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "#${i + 1}",
                        color = vm.selectedTheme.primary,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.width(40.dp)
                    )
                    Column(Modifier.weight(1f)) {
                        Text(p.name, color = Color.White)
                        Text(
                            "Lvl ${playerLevel.levelNum}",
                            color = playerLevel.color,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        if (tab == 0) "${p.highScore}" else "${p.totalEarnings}",
                        color = NeonGold,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        GlassButton(
            "BACK",
            Icons.Rounded.ArrowBack,
            Color.White.copy(0.1f),
            vm.selectedTheme.bg
        ) { vm.goBack() }
    }
}