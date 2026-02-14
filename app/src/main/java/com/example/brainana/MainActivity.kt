package com.example.brainana

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.delay
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

// --- 🎨 CYBERPALETTE ---
val Midnight = Color(0xFF020408)
val DeepSpace = Color(0xFF0B101B)
val NeonCyan = Color(0xFF00F2FF)
val NeonYellow = Color(0xFFFFD700)
val CyberPink = Color(0xFFFF00E5)
val GlassWhite = Color(0x1AFFFFFF)
val DangerRed = Color(0xFFFF3131)
val SuccessGreen = Color(0xFF39FF14)

enum class Screen { WELCOME, HOME, MODES, PLAYING, LEADERBOARD }
enum class Mode(val time: Long, val bonus: Int) {
    EASY(25000L, 1), MEDIUM(15000L, 2), HARD(8000L, 5)
}

data class Player(
    val uid: String = "",
    val name: String = "Guest",
    val highScore: Int = 0,
    val isGuest: Boolean = true
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val client = OkHttpClient()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val prefs = application.getSharedPreferences("brain_prefs", Context.MODE_PRIVATE)

    var currentScreen by mutableStateOf(Screen.WELCOME)
    var player by mutableStateOf(Player(highScore = prefs.getInt("local_high", 0)))
    var currentScore by mutableStateOf(0)
    var selectedMode by mutableStateOf(Mode.MEDIUM)
    var isPaused by mutableStateOf(false)

    var feedbackMessage by mutableStateOf("")
    var showFeedback by mutableStateOf(false)
    var isCorrectFeedback by mutableStateOf(true)

    var puzzleUrl by mutableStateOf("")
    var solution by mutableStateOf(-1)
    var isLoading by mutableStateOf(false)
    var leaderboard = mutableStateListOf<Player>()

    init { checkAuth() }

    private fun checkAuth() {
        auth.currentUser?.let { user ->
            fetchPlayerData(user.uid, user.displayName ?: "Agent")
        }
    }

    private fun fetchPlayerData(uid: String, fallbackName: String) {
        db.collection("players").document(uid).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                player = doc.toObject(Player::class.java)!!.copy(isGuest = false)
                syncLocalHigh(player.highScore)
            } else {
                val newPlayer = Player(uid, fallbackName, prefs.getInt("local_high", 0), false)
                db.collection("players").document(uid).set(newPlayer)
                player = newPlayer
            }
            currentScreen = Screen.HOME
        }
    }

    fun signInWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential).addOnSuccessListener { res ->
            val user = res.user!!
            fetchPlayerData(user.uid, user.displayName ?: "Agent")
        }
    }

    private fun syncLocalHigh(score: Int) {
        if (score > prefs.getInt("local_high", 0)) {
            prefs.edit().putInt("local_high", score).apply()
        }
    }

    fun processAnswer(input: String) {
        if (input.toInt() == solution) {
            triggerFeedback("CORRECT! +${10 * selectedMode.bonus}", true)
            updateScore(10 * selectedMode.bonus)
            fetchPuzzle()
        } else {
            triggerFeedback("WRONG! -5", false)
            updateScore(-5)
        }
    }

    private fun triggerFeedback(msg: String, isCorrect: Boolean) {
        feedbackMessage = msg
        isCorrectFeedback = isCorrect
        showFeedback = true
    }

    private fun updateScore(points: Int) {
        currentScore = (currentScore + points).coerceAtLeast(0)
        if (currentScore > player.highScore) {
            player = player.copy(highScore = currentScore)
            prefs.edit().putInt("local_high", currentScore).apply()

            // --- SYNC TO DATABASE ---
            if (!player.isGuest) {
                db.collection("players").document(player.uid)
                    .update("highScore", currentScore)
                    .addOnFailureListener { /* Handle error */ }
            }
        }
    }

    fun fetchPuzzle() {
        isLoading = true
        val request = Request.Builder().url("https://marcconrad.com/uob/banana/api.php").build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                if (responseData != null) {
                    val json = JSONObject(responseData)
                    puzzleUrl = json.optString("question")
                    solution = json.optInt("solution")
                }
                isLoading = false
            }
            override fun onFailure(call: Call, e: IOException) { isLoading = false }
        })
    }

    fun loadLeaderboard() {
        db.collection("players").orderBy("highScore", Query.Direction.DESCENDING).limit(10).get().addOnSuccessListener { res ->
            leaderboard.clear()
            leaderboard.addAll(res.toObjects(Player::class.java))
        }
    }

    fun logout() {
        auth.signOut()
        player = Player(name = "Guest", highScore = prefs.getInt("local_high", 0), isGuest = true)
        currentScreen = Screen.WELCOME
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        enableEdgeToEdge()
        setContent {
            val vm: GameViewModel = viewModel()
            Surface(modifier = Modifier.fillMaxSize(), color = Midnight) {
                DynamicBackground()
                Crossfade(targetState = vm.currentScreen, label = "ScreenTransition") { screen ->
                    when (screen) {
                        Screen.WELCOME -> WelcomeView(vm)
                        Screen.HOME -> HomeView(vm)
                        Screen.MODES -> ModeSelectionView(vm)
                        Screen.PLAYING -> GameView(vm)
                        Screen.LEADERBOARD -> LeaderboardView(vm)
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeView(vm: GameViewModel) {
    val context = LocalContext.current
    // REPLACE WITH YOUR WEB CLIENT ID FROM FIREBASE
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken("113242005751-t33f11sucdci7h8egvb8lhi31s73tfp0.apps.googleusercontent.com")
        .requestEmail().build()
    val client = GoogleSignIn.getClient(context, gso)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(res.data)
            vm.signInWithGoogle(task.getResult(ApiException::class.java)!!)
        } catch (e: Exception) {
            Toast.makeText(context, "Sign-in Error", Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Rounded.Psychology, null, modifier = Modifier.size(100.dp), tint = NeonCyan)
        Text(text = "BRAINANA", fontSize = 48.sp, fontWeight = FontWeight.Black, color = NeonYellow)
        Spacer(modifier = Modifier.height(40.dp))
        CyberButton("GOOGLE SIGN IN", Icons.Rounded.Login) { launcher.launch(client.signInIntent) }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = { vm.currentScreen = Screen.HOME }) { Text(text = "CONTINUE AS GUEST", color = GlassWhite) }
    }
}

@Composable
fun HomeView(vm: GameViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "WELCOME BACK", color = NeonCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(text = vm.player.name.uppercase(), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(8.dp))
            Surface(color = GlassWhite, shape = RoundedCornerShape(20.dp)) {
                Text(text = "🏆 HIGH SCORE: ${vm.player.highScore}", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = NeonYellow, fontWeight = FontWeight.Bold)
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CyberButton("START MISSION", Icons.Rounded.PlayArrow) { vm.currentScreen = Screen.MODES }
            Spacer(modifier = Modifier.height(15.dp))
            CyberButton("LEADERBOARD", Icons.Rounded.FormatListNumbered) { vm.loadLeaderboard(); vm.currentScreen = Screen.LEADERBOARD }
            if (!vm.player.isGuest) {
                TextButton(onClick = { vm.logout() }, modifier = Modifier.padding(top = 10.dp)) {
                    Text(text = "SIGN OUT", color = DangerRed.copy(alpha = 0.7f))
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun GameView(vm: GameViewModel) {
    var timeLeft by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(vm.puzzleUrl) {
        timeLeft = 1f
        vm.showFeedback = false
    }

    LaunchedEffect(vm.puzzleUrl, vm.isPaused) {
        if (vm.puzzleUrl.isNotEmpty() && !vm.isPaused) {
            while (timeLeft > 0 && !vm.isPaused) {
                delay(100)
                timeLeft -= 100f / vm.selectedMode.time
            }
            if (timeLeft <= 0 && !vm.isPaused) {
                vm.processAnswer("-1")
                vm.fetchPuzzle()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp).blur(if (vm.isPaused) 15.dp else 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 30.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                ScoreBadge("CURRENT SCORE", vm.currentScore, NeonCyan)
                ScoreBadge("SESSION TOP", vm.player.highScore, NeonYellow)
            }

            LinearProgressIndicator(
                progress = { timeLeft },
                modifier = Modifier.fillMaxWidth().padding(vertical = 15.dp).height(8.dp).clip(CircleShape),
                color = if (timeLeft < 0.3f) CyberPink else NeonCyan,
                trackColor = GlassWhite
            )

            Box(modifier = Modifier.height(40.dp), contentAlignment = Alignment.Center) {
                if (vm.showFeedback) {
                    Text(text = vm.feedbackMessage, color = if (vm.isCorrectFeedback) SuccessGreen else DangerRed, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth().background(GlassWhite, RoundedCornerShape(20.dp)).border(1.dp, NeonCyan.copy(0.3f), RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                if (vm.isLoading) CircularProgressIndicator(color = NeonCyan) else AsyncImage(vm.puzzleUrl, null)
            }

            Numpad { vm.processAnswer(it) }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                IconButton(onClick = { vm.isPaused = true }) { Icon(Icons.Rounded.Pause, null, tint = Color.White) }
                IconButton(onClick = { vm.currentScreen = Screen.HOME }) { Icon(Icons.Rounded.ExitToApp, null, tint = DangerRed) }
            }
        }

        if (vm.isPaused) {
            Box(modifier = Modifier.fillMaxSize().background(Midnight.copy(0.9f)).clickable(enabled = false){}, contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "GAME PAUSED", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(20.dp))
                    CyberButton("RESUME") { vm.isPaused = false }
                    TextButton(onClick = { vm.currentScreen = Screen.HOME; vm.isPaused = false }) { Text(text = "QUIT TO DASHBOARD", color = DangerRed) }
                }
            }
        }
    }
}

@Composable
fun ModeSelectionView(vm: GameViewModel) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "SELECT DIFFICULTY", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(30.dp))
        Mode.entries.forEach { mode ->
            val color = when(mode) { Mode.EASY -> NeonCyan; Mode.MEDIUM -> NeonYellow; Mode.HARD -> CyberPink }
            Surface(
                modifier = Modifier.fillMaxWidth(0.7f).padding(8.dp).clickable {
                    vm.selectedMode = mode; vm.currentScore = 0; vm.fetchPuzzle(); vm.currentScreen = Screen.PLAYING
                },
                color = color.copy(0.1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(2.dp, color)
            ) {
                Row(modifier = Modifier.padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = mode.name, color = color, fontWeight = FontWeight.Black)
                    Text(text = "${mode.bonus}x PTS", color = color)
                }
            }
        }
        TextButton(onClick = { vm.currentScreen = Screen.HOME }) { Text(text = "BACK", color = GlassWhite) }
    }
}

@Composable
fun LeaderboardView(vm: GameViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text(text = "LEADERBOARD", color = NeonCyan, fontSize = 30.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 40.dp))
        LazyColumn(modifier = Modifier.weight(1f).padding(vertical = 15.dp)) {
            itemsIndexed(vm.leaderboard) { i, p ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).background(GlassWhite, RoundedCornerShape(10.dp)).padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "#${i+1}", color = NeonYellow, modifier = Modifier.width(40.dp), fontWeight = FontWeight.Bold)
                    Text(text = p.name, color = Color.White, modifier = Modifier.weight(1f))
                    Text(text = "${p.highScore}", color = NeonCyan, fontWeight = FontWeight.Black)
                }
            }
        }
        CyberButton("BACK") { vm.currentScreen = Screen.HOME }
    }
}

@Composable
fun Numpad(onInput: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        (1..9).chunked(3).forEach { row ->
            Row { row.forEach { n -> NumpadKey(n.toString(), onInput) } }
        }
        NumpadKey("0", onInput)
    }
}

@Composable
fun NumpadKey(text: String, onClick: (String) -> Unit) {
    Surface(
        modifier = Modifier.size(75.dp).padding(5.dp).clickable { onClick(text) },
        color = GlassWhite,
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = text, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ScoreBadge(label: String, score: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = color.copy(0.7f), fontSize = 11.sp)
        Text(text = score.toString(), color = color, fontSize = 22.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun CyberButton(text: String, icon: ImageVector? = null, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(0.75f).height(55.dp),
        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
        shape = RoundedCornerShape(10.dp)
    ) {
        if (icon != null) Icon(icon, null, tint = Midnight, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, color = Midnight, fontWeight = FontWeight.Black)
    }
}

@Composable
fun DynamicBackground() {
    val rot by rememberInfiniteTransition(label = "BgRotation").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(25000, easing = LinearEasing)),
        label = "rotation"
    )
    Box(modifier = Modifier.fillMaxSize().rotate(rot).background(Brush.radialGradient(listOf(DeepSpace, Midnight), radius = 2500f)))
}