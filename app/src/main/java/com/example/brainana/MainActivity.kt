package com.example.brainana

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

// 🎨 UPDATED COLOR PALETTE
val MidnightBlue = Color(0xFF0A192F)
val ElectricYellow = Color(0xFFFFD700)
val CyberBlue = Color(0xFF64FFDA)
val SoftGlass = Color(0x1AFFFFFF)
val WarningRed = Color(0xFFFF4B2B)
val SuccessGreen = Color(0xFF00E676)

enum class GameState { START, PLAYING, OVER }

class MainActivity : ComponentActivity() {
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BrainanaGame(this) }
    }

    @Composable
    fun BrainanaGame(context: Context) {
        val prefs = remember { context.getSharedPreferences("brainana_v3", Context.MODE_PRIVATE) }

        // --- PERSISTENT DATA ---
        var userName by remember { mutableStateOf(prefs.getString("name", "") ?: "") }
        var highScore by remember { mutableStateOf(prefs.getInt("high_score", 0)) }

        // --- GAME STATES ---
        var gameState by remember { mutableStateOf(GameState.START) }
        var score by remember { mutableStateOf(0) }
        var isOnline by remember { mutableStateOf(true) }

        // Puzzle States
        var imageUrl by remember { mutableStateOf("") }
        var solution by remember { mutableStateOf(-1) }
        var input by remember { mutableStateOf("") }
        var timeLeft by remember { mutableStateOf(1f) }
        var isLoading by remember { mutableStateOf(false) }
        var floatingMsg by remember { mutableStateOf("") }
        var isNewRecord by remember { mutableStateOf(false) }

        val scope = rememberCoroutineScope()
        val shakeAnim = remember { Animatable(0f) }

        // --- NETWORK MONITOR ---
        DisposableEffect(Unit) {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(n: Network) { isOnline = true }
                override fun onLost(n: Network) { isOnline = false }
            }
            cm.registerDefaultNetworkCallback(callback)
            onDispose { cm.unregisterNetworkCallback(callback) }
        }

        fun fetchNext() {
            if (!isOnline || isLoading) return
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

        // --- TIMER LOGIC (20 SECONDS) ---
        LaunchedEffect(gameState, imageUrl, isOnline) {
            if (gameState == GameState.PLAYING && isOnline && imageUrl.isNotEmpty()) {
                timeLeft = 1f
                val totalTime = 20000L // 20s
                val step = 100L
                while (timeLeft > 0 && isOnline) {
                    delay(step)
                    timeLeft -= step.toFloat() / totalTime
                }
                if (timeLeft <= 0 && isOnline) {
                    score = (score - 5).coerceAtLeast(0)
                    floatingMsg = "⏰ TIME OUT! -5"
                    delay(1000)
                    fetchNext()
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize().background(MidnightBlue)) {
            BackgroundDecor()

            when (gameState) {
                GameState.START -> StartView(userName, highScore,
                    onStart = {
                        score = 0
                        isNewRecord = false
                        gameState = GameState.PLAYING
                        fetchNext()
                    },
                    onNameSave = { name ->
                        userName = name
                        prefs.edit().putString("name", name).apply()
                    }
                )

                GameState.PLAYING -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        GameHUD(userName, score, highScore)

                        LinearProgressIndicator(
                            progress = { timeLeft },
                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                            color = if (timeLeft < 0.3f) WarningRed else ElectricYellow,
                            trackColor = SoftGlass
                        )

                        Box(
                            modifier = Modifier.weight(1f).offset(x = shakeAnim.value.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            PuzzleFrame(imageUrl, isLoading, floatingMsg)
                        }

                        Text(input.ifEmpty { "🍌" }, fontSize = 56.sp, fontWeight = FontWeight.Black, color = ElectricYellow)

                        NumberPad { num ->
                            if (input.length < 2 && isOnline && !isLoading) {
                                input += num
                                if (input.toInt() == solution) {
                                    score += 10
                                    floatingMsg = "✨ GENIUS! +10"
                                    scope.launch { delay(600); fetchNext() }
                                } else if (input.length >= solution.toString().length) {
                                    score = (score - 5).coerceAtLeast(0)
                                    floatingMsg = "❌ OOPS! -5"
                                    scope.launch {
                                        repeat(3) {
                                            shakeAnim.animateTo(15f, tween(40))
                                            shakeAnim.animateTo(-15f, tween(40))
                                        }
                                        shakeAnim.animateTo(0f)
                                        delay(400); input = ""; floatingMsg = ""
                                    }
                                }
                            }
                        }

                        TextButton(onClick = {
                            if (score > highScore) {
                                highScore = score
                                isNewRecord = true
                                prefs.edit().putInt("high_score", score).apply()
                            }
                            gameState = GameState.OVER
                        }) {
                            Text("FINISH GAME 🏳️", color = Color.White.copy(0.4f))
                        }
                    }
                }

                GameState.OVER -> GameOverView(score, highScore, isNewRecord) { gameState = GameState.START }
            }

            if (!isOnline) NetworkErrorLayer()
        }
    }

    // --- REUSABLE COMPONENTS ---

    @Composable
    fun BackgroundDecor() {
        Box(Modifier.fillMaxSize()) {
            val symbols = listOf("∑", "π", "∫", "√", "∞", "∆", "🍌", "÷", "×")
            symbols.forEachIndexed { index, s ->
                Text(
                    text = s,
                    color = Color.White.copy(0.04f),
                    fontSize = (40 + (index * 15)).sp,
                    modifier = Modifier
                        .offset(x = (index * 40).dp, y = (index * 100).dp)
                        .rotate(index * 30f)
                )
            }
        }
    }

    @Composable
    fun StartView(name: String, high: Int, onStart: () -> Unit, onNameSave: (String) -> Unit) {
        var textInput by remember { mutableStateOf("") }
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("BRAINANA", fontSize = 52.sp, fontWeight = FontWeight.ExtraBold, color = ElectricYellow)
            Text("MATH MONKEY CHALLENGE", color = CyberBlue, letterSpacing = 2.sp)

            Spacer(Modifier.height(48.dp))

            if (name.isEmpty()) {
                OutlinedTextField(
                    value = textInput, onValueChange = { textInput = it },
                    label = { Text("Enter Your Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = ElectricYellow
                    )
                )
                Button(
                    onClick = { if (textInput.isNotBlank()) onNameSave(textInput) },
                    modifier = Modifier.padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(ElectricYellow)
                ) {
                    Text("SAVE & PLAY", color = MidnightBlue, fontWeight = FontWeight.Bold)
                }
            } else {
                Text("Ready, $name?", color = Color.White, fontSize = 22.sp)
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onStart,
                    modifier = Modifier.size(120.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(ElectricYellow)
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, Modifier.size(64.dp), tint = MidnightBlue)
                }
                Spacer(Modifier.height(32.dp))
                Text("PERSONAL BEST: $high", color = CyberBlue, fontWeight = FontWeight.Bold)
            }
        }
    }

    @Composable
    fun GameHUD(name: String, score: Int, high: Int) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(name.uppercase(), color = CyberBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("🎯 $score", color = ElectricYellow, fontSize = 32.sp, fontWeight = FontWeight.Black)
            }
            Surface(color = SoftGlass, shape = RoundedCornerShape(12.dp)) {
                Text("🏆 BEST: $high", Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Color.White, fontSize = 14.sp)
            }
        }
    }

    @Composable
    fun PuzzleFrame(url: String, loading: Boolean, msg: String) {
        Box(contentAlignment = Alignment.Center) {
            Card(
                modifier = Modifier.fillMaxWidth().aspectRatio(1.2f).border(2.dp, SoftGlass, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                if (loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MidnightBlue)
                    }
                } else {
                    AsyncImage(url, null, modifier = Modifier.fillMaxSize().padding(12.dp))
                }
            }

            AnimatedVisibility(visible = msg.isNotEmpty(), enter = scaleIn(), exit = fadeOut()) {
                Surface(
                    color = MidnightBlue.copy(0.9f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(2.dp, ElectricYellow)
                ) {
                    Text(msg, Modifier.padding(20.dp), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                }
            }
        }
    }

    @Composable
    fun NumberPad(onPress: (String) -> Unit) {
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
        Column(modifier = Modifier.padding(top = 16.dp)) {
            keys.chunked(5).forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    row.forEach { num ->
                        Surface(
                            modifier = Modifier.size(62.dp).clickable { onPress(num) },
                            shape = RoundedCornerShape(16.dp),
                            color = SoftGlass,
                            border = BorderStroke(1.dp, Color.White.copy(0.1f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(num, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun GameOverView(score: Int, high: Int, isNew: Boolean, onRetry: () -> Unit) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(if(isNew) "👑 NEW RECORD! 👑" else "GAME OVER", fontSize = 28.sp, color = if(isNew) SuccessGreen else Color.White)
            Text("$score", fontSize = 110.sp, fontWeight = FontWeight.Black, color = ElectricYellow)
            Text("BEST SCORE: $high", color = CyberBlue)
            Spacer(Modifier.height(48.dp))
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(ElectricYellow)
            ) {
                Text("PLAY AGAIN", color = MidnightBlue, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }

    @Composable
    fun NetworkErrorLayer() {
        Box(
            modifier = Modifier.fillMaxSize().background(MidnightBlue.copy(0.95f)).clickable(enabled = false) {},
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.WifiOff, null, Modifier.size(80.dp), tint = WarningRed)
                Text("CONNECTION LOST", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Waiting for signal... 📡", color = Color.Gray)
            }
        }
    }
}