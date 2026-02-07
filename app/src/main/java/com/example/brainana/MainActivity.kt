package com.example.brainana

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

// 🎨 CYBERPUNK PALETTE
val MidnightBlue = Color(0xFF020817)
val ElectricYellow = Color(0xFFFFD700)
val CyberBlue = Color(0xFF00F2FF)
val NeonPink = Color(0xFFFF00E5)
val SoftGlass = Color(0x33FFFFFF)
val WarningRed = Color(0xFFFF3D00)

enum class GameState { START, DIFFICULTY_SELECT, PLAYING, OVER }
enum class Difficulty(val time: Long, val label: String) {
    EASY(30000L, "EASY"),
    MEDIUM(20000L, "MEDIUM"),
    HARD(10000L, "HARD")
}

class MainActivity : ComponentActivity() {
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BrainanaGame(this) }
    }

    @Composable
    fun BrainanaGame(context: Context) {
        val prefs = remember { context.getSharedPreferences("brainana_v4", Context.MODE_PRIVATE) }
        val scope = rememberCoroutineScope()

        // --- STATES ---
        var userName by remember { mutableStateOf(prefs.getString("name", "") ?: "") }
        var highScore by remember { mutableStateOf(prefs.getInt("high_score", 0)) }
        var gameState by remember { mutableStateOf(GameState.START) }
        var difficulty by remember { mutableStateOf(Difficulty.MEDIUM) }

        var isPaused by remember { mutableStateOf(false) }
        var is3DMode by remember { mutableStateOf(false) }
        var score by remember { mutableStateOf(0) }
        var isOnline by remember { mutableStateOf(true) }

        // Puzzle Logic
        var imageUrl by remember { mutableStateOf("") }
        var solution by remember { mutableStateOf(-1) }
        var input by remember { mutableStateOf("") }
        var timeLeft by remember { mutableStateOf(1f) }
        var isLoading by remember { mutableStateOf(false) }
        var floatingMsg by remember { mutableStateOf("") }
        val shakeAnim = remember { Animatable(0f) }

        // --- HELPERS ---
        fun fetchNext() {
            if (isLoading) return
            isLoading = true
            val request = Request.Builder().url("https://marcconrad.com/uob/banana/api.php").build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) { isLoading = false }
                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string()
                    val json = JSONObject(body ?: "{}")
                    runOnUiThread {
                        imageUrl = json.optString("question")
                        solution = json.optInt("solution")
                        input = ""
                        isLoading = false
                        floatingMsg = ""
                    }
                }
            })
        }

        // --- TIMER ---
        LaunchedEffect(gameState, imageUrl, isPaused) {
            if (gameState == GameState.PLAYING && !isPaused && imageUrl.isNotEmpty()) {
                val totalTime = difficulty.time
                val step = 50L
                while (timeLeft > 0 && !isPaused) {
                    delay(step)
                    timeLeft -= step.toFloat() / totalTime
                }
                if (timeLeft <= 0) {
                    score = (score - 5).coerceAtLeast(0)
                    floatingMsg = "⏰ TIME OUT! -5"
                    delay(800)
                    fetchNext()
                    timeLeft = 1f
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize().background(MidnightBlue)) {
            AnimatedBackground()

            when (gameState) {
                GameState.START -> StartView(userName) {
                    userName = it
                    prefs.edit().putString("name", it).apply()
                    gameState = GameState.DIFFICULTY_SELECT
                }

                GameState.DIFFICULTY_SELECT -> DifficultyView { selected ->
                    difficulty = selected
                    score = 0
                    gameState = GameState.PLAYING
                    fetchNext()
                }

                GameState.PLAYING -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(20.dp).blur(if (isPaused) 25.dp else 0.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        GameHUD(userName, score, is3DMode, onToggle3D = { is3DMode = !is3DMode })

                        LinearProgressIndicator(
                            progress = { timeLeft },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                            color = if (timeLeft < 0.3f) WarningRed else CyberBlue,
                            trackColor = SoftGlass
                        )

                        // Puzzle Area
                        Box(
                            modifier = Modifier.weight(1f)
                                .graphicsLayer {
                                    if (is3DMode) {
                                        rotationX = 15f
                                        cameraDistance = 12f
                                    }
                                }
                                .offset(x = shakeAnim.value.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            PuzzleFrame(imageUrl, isLoading, floatingMsg)
                        }

                        // Input Display
                        Text(input.ifEmpty { "❓" }, fontSize = 60.sp, fontWeight = FontWeight.Black, color = ElectricYellow)

                        NumberPad { num ->
                            if (input.length < 2 && !isLoading && !isPaused) {
                                input += num
                                if (input.toInt() == solution) {
                                    score += 10
                                    floatingMsg = "✨ BRAVO! +10"
                                    scope.launch { delay(700); fetchNext(); timeLeft = 1f }
                                } else if (input.length >= solution.toString().length) {
                                    score = (score - 5).coerceAtLeast(0)
                                    floatingMsg = "❌ WRONG! -5"
                                    scope.launch {
                                        repeat(4) {
                                            shakeAnim.animateTo(10f, tween(30))
                                            shakeAnim.animateTo(-10f, tween(30))
                                        }
                                        shakeAnim.animateTo(0f)
                                        input = ""
                                    }
                                }
                            }
                        }

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            IconButton(onClick = { isPaused = true }) {
                                Icon(Icons.Rounded.Pause, "Pause", tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                            TextButton(onClick = {
                                if (score > highScore) {
                                    highScore = score
                                    prefs.edit().putInt("high_score", score).apply()
                                }
                                gameState = GameState.OVER
                            }) {
                                Text("QUIT", color = Color.White.copy(0.5f))
                            }
                        }
                    }

                    if (isPaused) {
                        PauseOverlay { isPaused = false }
                    }
                }

                GameState.OVER -> GameOverView(score, highScore) { gameState = GameState.START }
            }
        }
    }

    @Composable
    fun PauseOverlay(onResume: () -> Unit) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.7f)).clickable {  },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("GAME PAUSED", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onResume,
                    colors = ButtonDefaults.buttonColors(ElectricYellow),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = MidnightBlue)
                    Text(" RESUME", color = MidnightBlue, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    @Composable
    fun DifficultyView(onSelected: (Difficulty) -> Unit) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("SELECT CHALLENGE", color = CyberBlue, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(32.dp))
            Difficulty.values().forEach { level ->
                Button(
                    onClick = { onSelected(level) },
                    modifier = Modifier.fillMaxWidth(0.7f).padding(8.dp).height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SoftGlass),
                    border = BorderStroke(1.dp, CyberBlue)
                ) {
                    Text(level.label, color = Color.White, fontSize = 18.sp)
                }
            }
        }
    }

    @Composable
    fun GameHUD(name: String, score: Int, is3D: Boolean, onToggle3D: () -> Unit) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(name, color = CyberBlue, fontWeight = FontWeight.Bold)
                Text("SCORE: $score", color = ElectricYellow, fontSize = 24.sp, fontWeight = FontWeight.Black)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("3D", color = Color.White, fontSize = 12.sp)
                Switch(checked = is3D, onCheckedChange = { onToggle3D() })
            }
        }
    }

    @Composable
    fun PuzzleFrame(url: String, loading: Boolean, msg: String) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.fillMaxWidth().aspectRatio(1.2f),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 20.dp
            ) {
                if (loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MidnightBlue)
                    }
                } else {
                    AsyncImage(url, null, modifier = Modifier.fillMaxSize().padding(12.dp))
                }
            }

            AnimatedVisibility(visible = msg.isNotEmpty(), enter = fadeIn() + scaleIn(), exit = fadeOut()) {
                Box(Modifier.background(MidnightBlue.copy(0.8f), RoundedCornerShape(12.dp)).padding(16.dp)) {
                    Text(msg, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    @Composable
    fun NumberPad(onPress: (String) -> Unit) {
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
        Column {
            keys.chunked(5).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(4.dp)) {
                    row.forEach { num ->
                        Surface(
                            modifier = Modifier.size(60.dp).clickable { onPress(num) },
                            shape = RoundedCornerShape(12.dp),
                            color = SoftGlass,
                            border = BorderStroke(1.dp, Color.White.copy(0.2f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(num, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun StartView(currentName: String, onProceed: (String) -> Unit) {
        var nameInput by remember { mutableStateOf(currentName) }
        Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
            Text("BRAINANA", fontSize = 56.sp, fontWeight = FontWeight.Black, color = ElectricYellow)
            Spacer(Modifier.height(40.dp))
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Agent Name") },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { if(nameInput.isNotBlank()) onProceed(nameInput) },
                colors = ButtonDefaults.buttonColors(ElectricYellow)
            ) {
                Text("ENTER SYSTEM", color = MidnightBlue, fontWeight = FontWeight.Bold)
            }
        }
    }

    @Composable
    fun GameOverView(score: Int, high: Int, onRetry: () -> Unit) {
        Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
            Text("MISSION OVER", color = WarningRed, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text("$score", fontSize = 100.sp, color = Color.White, fontWeight = FontWeight.Black)
            Text("BEST: $high", color = CyberBlue)
            Spacer(Modifier.height(50.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(ElectricYellow)) {
                Text("REBOOT", color = MidnightBlue)
            }
        }
    }

    @Composable
    fun AnimatedBackground() {
        val infiniteTransition = rememberInfiniteTransition()
        val angle by infiniteTransition.animateFloat(0f, 360f, infiniteRepeatable(tween(20000, easing = LinearEasing)))

        Box(Modifier.fillMaxSize().rotate(angle).background(
            Brush.radialGradient(listOf(Color(0xFF0F172A), MidnightBlue))
        ))
    }
}