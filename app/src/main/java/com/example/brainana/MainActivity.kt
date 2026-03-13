package com.example.brainana

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.FirebaseApp
import com.example.brainana.ui.components.ConnectionOverlay
import com.example.brainana.ui.components.LevelUpOverlay
import com.example.brainana.ui.components.MeshGradientBackground
import com.example.brainana.ui.components.RankUpOverlay
import com.example.brainana.ui.components.TopHUD
import com.example.brainana.ui.screens.AvatarSelectScreen
import com.example.brainana.ui.screens.DashboardScreen
import com.example.brainana.ui.screens.InstructionsScreen
import com.example.brainana.ui.screens.LeaderboardScreen
import com.example.brainana.ui.screens.ModeScreen
import com.example.brainana.ui.screens.ProfileScreen
import com.example.brainana.ui.screens.ThemePickerScreen
import com.example.brainana.ui.screens.WelcomeScreen
import com.example.brainana.ui.screens.ArenaScreen
import com.example.brainana.ui.viewmodel.GameViewModel
import com.example.brainana.ui.viewmodel.Screen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        enableEdgeToEdge()

        setContent {
            val vm: GameViewModel = viewModel()
            val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

            DisposableEffect(backDispatcher) {
                val callback = object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        when (vm.currentScreen) {
                            Screen.HOME, Screen.WELCOME -> finish()
                            else -> vm.goBack()
                        }
                    }
                }
                backDispatcher?.addCallback(callback)
                onDispose { callback.remove() }
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = vm.selectedTheme.bg
            ) {
                Box {
                    // Background mesh gradient
                    MeshGradientBackground(vm.selectedTheme)

                    Column {
                        // Top HUD - Show on all screens except welcome, theme picker, instructions
                        if (vm.currentScreen !in listOf(
                                Screen.WELCOME,
                                Screen.THEME_PICKER,
                                Screen.INSTRUCTIONS
                            )
                        ) {
                            TopHUD(vm.player, vm.selectedTheme)
                        }

                        // Screen Navigation with animations
                        AnimatedContent(
                            targetState = vm.currentScreen,
                            transitionSpec = {
                                slideInHorizontally(initialOffsetX = { it }) +
                                        fadeIn() togetherWith
                                        slideOutHorizontally(targetOffsetX = { -it }) +
                                        fadeOut()
                            },
                            label = "ScreenNavigation"
                        ) { screen ->
                            when (screen) {
                                Screen.WELCOME -> WelcomeScreen(vm)
                                Screen.THEME_PICKER -> ThemePickerScreen(vm)
                                Screen.INSTRUCTIONS -> InstructionsScreen(vm)
                                Screen.HOME -> DashboardScreen(vm)
                                Screen.MODES -> ModeScreen(vm)
                                Screen.PLAYING -> ArenaScreen(vm)
                                Screen.LEADERBOARD -> LeaderboardScreen(vm)
                                Screen.PROFILE -> ProfileScreen(vm)
                                Screen.AVATAR_SELECT -> AvatarSelectScreen(vm)
                            }
                        }
                    }

                    // Connection Error Overlay
                    if (!vm.isOnline) {
                        ConnectionOverlay(vm.selectedTheme) {
                            vm.checkNetwork(this@MainActivity)
                        }
                    }

                    // Rank Up Notification
                    vm.newRankReached?.let { rank ->
                        RankUpOverlay(rank) {
                            vm.newRankReached = null
                        }
                    }

                    // Level Up Notification
                    vm.levelUpEvent?.let { levelUpEvent ->
                        LevelUpOverlay(levelUpEvent) {
                            vm.levelUpEvent = null
                        }
                    }
                }
            }
        }
    }
}