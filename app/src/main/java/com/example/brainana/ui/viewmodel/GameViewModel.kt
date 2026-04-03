package com.example.brainana.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainana.data.models.*
import com.example.brainana.data.preferences.GamePreferences
import com.example.brainana.data.repository.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.brainana.utils.Constants
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

enum class Screen {
    WELCOME, THEME_PICKER, INSTRUCTIONS, HOME, MODES, PLAYING, LEADERBOARD, PROFILE, AVATAR_SELECT
}

class GameViewModel(application: Application) : AndroidViewModel(application) {

    // Repositories
    private val authRepository = AuthRepository()
    private val playerRepository = PlayerRepository()
    private val gameRepository = GameRepository()
    private val leaderboardRepository = LeaderboardRepository()
    private val gamePreferences = GamePreferences(application)

    // UI State
    var currentScreen by mutableStateOf(Screen.WELCOME)
    var backStack = mutableStateListOf(Screen.WELCOME)
    var player by mutableStateOf(Player())

    var currentScore by mutableStateOf(0)
    var selectedMode by mutableStateOf(Mode.MEDIUM)
    var selectedTheme by mutableStateOf(GameTheme.NEURAL)

    var isPaused by mutableStateOf(false)
    var isLoading by mutableStateOf(false)
    var puzzleUrl by mutableStateOf("")
    var solution by mutableStateOf(-1)

    var isOnline by mutableStateOf(true)
    var leaderboard = mutableStateListOf<Player>()
    var newRankReached by mutableStateOf<Rank?>(null)
    var levelUpEvent by mutableStateOf<LevelUpEvent?>(null)

    var isFirstLaunch by mutableStateOf(true)

    // Authentication Progress State
    var isAuthenticating by mutableStateOf(false)
    var authProgress by mutableStateOf(0f)
    var authStatusMessage by mutableStateOf("")
    var authError by mutableStateOf("")

    init {
        FirebaseApp.initializeApp(application)
        loadLocalData()
        checkNetwork(application)

        authRepository.getCurrentUser()?.let {
            fetchPlayerProfile(
                it.uid,
                it.displayName ?: "Agent",
                it.photoUrl?.toString() ?: ""
            )
        }
    }

    // ========== NETWORK ==========
    fun checkNetwork(context: Context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val cap = cm.getNetworkCapabilities(cm.activeNetwork)
        isOnline = cap?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    // ========== NAVIGATION ==========
    fun navigateTo(screen: Screen) {
        if (currentScreen != screen) {
            backStack.add(screen)
            currentScreen = screen
        }
    }

    fun goBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.size - 1)
            currentScreen = backStack.last()
        }
    }

    // ========== FIRST LAUNCH ==========
    fun completeFirstLaunch() {
        isFirstLaunch = false
        gamePreferences.completeFirstLaunch()
        navigateTo(Screen.HOME)
    }

    // ========== LOCAL DATA ==========
    private fun loadLocalData() {
        val savedXp = gamePreferences.getTotalXp()
        val level = Level.fromXp(savedXp).levelNum

        player = Player(
            name = gamePreferences.getPlayerName(),
            highScore = gamePreferences.getHighScore(),
            totalEarnings = savedXp,
            avatarStyle = gamePreferences.getAvatarStyle(),
            isGuest = true,
            level = level
        )

        selectedTheme = GameTheme.valueOf(gamePreferences.getTheme())
        isFirstLaunch = gamePreferences.isFirstLaunch()
    }

    // ========== THEME MANAGEMENT ==========
    fun updateTheme(theme: GameTheme) {
        selectedTheme = theme
        gamePreferences.setTheme(theme.name)
    }

    // ========== AVATAR MANAGEMENT ==========
    fun updateAvatar(style: String) {
        player = player.copy(avatarStyle = style)
        gamePreferences.setAvatarStyle(style)

        if (!player.isGuest) {
            viewModelScope.launch {
                try {
                    playerRepository.updatePlayerAvatar(player.uid, style)
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    }

    // ========== AUTHENTICATION PROGRESS ==========
    fun startAuthenticationProcess() {
        isAuthenticating = true
        authProgress = 0f
        authStatusMessage = ""
        authError = ""
    }

    private suspend fun updateAuthProgress(progress: Float, message: String) {
        authProgress = progress
        authStatusMessage = message
        delay(300) // Small delay for visual effect
    }

    // ========== AUTHENTICATION ==========
    fun handleSignIn(idToken: String) {
        viewModelScope.launch {
            try {
                updateAuthProgress(0.25f, "Initializing authentication...")
                authRepository.signInWithGoogle(idToken)

                updateAuthProgress(0.5f, "Verifying credentials...")
                val user = authRepository.getCurrentUser()

                user?.let {
                    updateAuthProgress(0.75f, "Loading profile...")
                    fetchPlayerProfile(
                        it.uid,
                        it.displayName ?: "Agent",
                        it.photoUrl?.toString() ?: ""
                    )
                    updateAuthProgress(1f, "Syncing data...")
                    delay(500)
                }
            } catch (e: Exception) {
                authError = e.message ?: "Authentication failed. Please try again."
                isAuthenticating = false
            }
        }
    }

    private fun fetchPlayerProfile(uid: String, name: String, photo: String) {
        viewModelScope.launch {
            try {
                val existingPlayer = playerRepository.fetchPlayerProfile(uid)

                if (existingPlayer != null) {
                    player = existingPlayer.copy(isGuest = false)
                } else {
                    val level = Level.fromXp(player.totalEarnings).levelNum
                    val newPlayer = Player(
                        uid = uid,
                        name = name,
                        highScore = 0,
                        totalEarnings = player.totalEarnings,
                        photoUrl = photo,
                        avatarStyle = player.avatarStyle,
                        isGuest = false,
                        level = level
                    )
                    playerRepository.createPlayer(newPlayer)
                    player = newPlayer
                }

                isAuthenticating = false

                if (isFirstLaunch) {
                    navigateTo(Screen.THEME_PICKER)
                } else {
                    navigateTo(Screen.HOME)
                }
            } catch (e: Exception) {
                authError = e.message ?: "Failed to load profile. Please try again."
                isAuthenticating = false
            }
        }
    }

    fun proceedFromWelcome() {
        if (isFirstLaunch) {
            navigateTo(Screen.THEME_PICKER)
        } else {
            navigateTo(Screen.HOME)
        }
    }

    // ========== GAME MECHANICS ==========
    fun submitAnswer(input: String, timeout: Boolean = false) {
        if (isPaused || !isOnline) return

        val correct = !timeout && input.toIntOrNull() == solution
        val xpGain = if (correct) {
            Constants.CORRECT_ANSWER_XP * selectedMode.bonus
        } else {
            Constants.WRONG_ANSWER_XP
        }

        val prevRank = Rank.fromXp(player.totalEarnings)
        val prevLevel = Level.fromXp(player.totalEarnings)

        currentScore = (currentScore + if (correct) 10 else -5).coerceAtLeast(0)
        val newXp = (player.totalEarnings + xpGain).coerceAtLeast(0)

        val nextRank = Rank.fromXp(newXp)
        val nextLevel = Level.fromXp(newXp)

        // Check for Rank Up
        if (nextRank.minXp > prevRank.minXp) {
            newRankReached = nextRank
        }

        // Check for Level Up
        if (nextLevel.levelNum > prevLevel.levelNum) {
            levelUpEvent = LevelUpEvent(prevLevel, nextLevel)
        }

        val newLevelNum = nextLevel.levelNum
        player = player.copy(
            totalEarnings = newXp,
            highScore = if (currentScore > player.highScore) currentScore else player.highScore,
            level = newLevelNum
        )

        // Save to preferences
        gamePreferences.setHighScore(player.highScore)
        gamePreferences.setTotalXp(player.totalEarnings)
        gamePreferences.setLevel(newLevelNum)

        // Save to Firebase if authenticated
        if (!player.isGuest) {
            viewModelScope.launch {
                try {
                    playerRepository.updatePlayerStats(
                        player.uid,
                        player.highScore,
                        player.totalEarnings,
                        newLevelNum
                    )
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }

        if (correct) {
            fetchNewPuzzle()
        }
    }

    // ========== PUZZLE FETCHING ==========
    fun fetchNewPuzzle() {
        viewModelScope.launch {
            isLoading = true
            try {
                val puzzleData = gameRepository.fetchPuzzle(selectedTheme)
                puzzleUrl = puzzleData.question
                solution = puzzleData.solution
            } catch (e: Exception) {
                // Handle error
            } finally {
                isLoading = false
            }
        }
    }

    // ========== LEADERBOARD ==========
    fun getLeaderboard(field: String) {
        viewModelScope.launch {
            try {
                val leaders = leaderboardRepository.getLeaderboard(field)
                leaderboard.clear()
                leaderboard.addAll(leaders)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    // ========== SIGN OUT ==========
    fun signOut(context: Context) {
        authRepository.signOut()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        GoogleSignIn.getClient(context, gso).signOut()

        loadLocalData()
        backStack.clear()
        backStack.add(Screen.WELCOME)
        currentScreen = Screen.WELCOME
    }
}