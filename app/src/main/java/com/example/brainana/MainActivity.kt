package com.example.brainana

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.android.gms.auth.api.signin.GoogleSignIn
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
import kotlin.random.Random

// --- 🎨 PREMIUM CYBER PALETTE ---
val DarkObsidian = Color(0xFF050505)
val CyberBlue = Color(0xFF00D1FF)
val ElectricViolet = Color(0xFF8B5CF6)
val NeonGold = Color(0xFFFFD700)
val VividRose = Color(0xFFFF2E63)
val GlassLayer = Color(0x26FFFFFF)

enum class Screen { WELCOME, HOME, MODES, PLAYING, LEADERBOARD, PROFILE }
enum class Mode(val time: Long, val bonus: Int, val desc: String) {
    EASY(20000L, 1, "Standard Neural Sync"),
    MEDIUM(12000L, 2, "Accelerated Processing"),
    HARD(7000L, 4, "Overclocked Protocol")
}

enum class Rank(val label: String, val minXp: Int, val color: Color, val icon: ImageVector) {
    BEGINNER("INITIATE", 0, Color(0xFF4ADE80), Icons.Rounded.RocketLaunch),
    PRO("OPERATIVE", 2500, CyberBlue, Icons.Rounded.VerifiedUser),
    LEGEND("ARCHITECT", 10000, ElectricViolet, Icons.Rounded.AllInclusive);

    companion object {
        fun fromXp(xp: Int) = entries.lastOrNull { xp >= it.minXp } ?: BEGINNER
    }
}

data class Player(
    val uid: String = "",
    val name: String = "Agent Guest",
    val highScore: Int = 0,
    val totalEarnings: Int = 0,
    val photoUrl: String = "",
    val isGuest: Boolean = true
)

// --- 🧠 CORE ENGINE ---
class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val client = OkHttpClient()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val prefs = application.getSharedPreferences("brain_v2_prefs", Context.MODE_PRIVATE)

    var currentScreen by mutableStateOf(Screen.WELCOME)
    var backStack = mutableStateListOf(Screen.WELCOME)
    var player by mutableStateOf(Player())
    var currentScore by mutableStateOf(0)
    var selectedMode by mutableStateOf(Mode.MEDIUM)
    var isPaused by mutableStateOf(false)
    var isLoading by mutableStateOf(false)
    var puzzleUrl by mutableStateOf("")
    var solution by mutableStateOf(-1)
    var isOnline by mutableStateOf(true)
    var leaderboard = mutableStateListOf<Player>()
    var newRankReached by mutableStateOf<Rank?>(null)

    init {
        loadLocalData()
        checkNetwork(application)
        auth.currentUser?.let { fetchPlayerProfile(it.uid, it.displayName ?: "Agent", it.photoUrl?.toString() ?: "") }
    }

    fun checkNetwork(context: Context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val cap = cm.getNetworkCapabilities(cm.activeNetwork)
        isOnline = cap?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

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

    private fun loadLocalData() {
        player = Player(
            name = prefs.getString("name", "Agent Guest") ?: "Agent Guest",
            highScore = prefs.getInt("high", 0),
            totalEarnings = prefs.getInt("xp", 0),
            isGuest = true
        )
    }

    fun handleSignIn(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnSuccessListener { res ->
            val u = res.user!!
            fetchPlayerProfile(u.uid, u.displayName ?: "Agent", u.photoUrl?.toString() ?: "")
        }
    }

    private fun fetchPlayerProfile(uid: String, name: String, photo: String) {
        db.collection("players").document(uid).get().addOnSuccessListener { doc ->
            player = if (doc.exists()) {
                doc.toObject(Player::class.java)!!.copy(isGuest = false)
            } else {
                val p = Player(uid, name, 0, player.totalEarnings, photo, false)
                db.collection("players").document(uid).set(p)
                p
            }
            navigateTo(Screen.HOME)
        }
    }

    fun submitAnswer(input: String, timeout: Boolean = false) {
        if (isPaused || !isOnline) return
        val correct = !timeout && input.toIntOrNull() == solution
        val diff = if (correct) (10 * selectedMode.bonus) else -5

        val prevRank = Rank.fromXp(player.totalEarnings)
        currentScore = (currentScore + diff).coerceAtLeast(0)
        val newXp = (player.totalEarnings + diff).coerceAtLeast(0)
        val nextRank = Rank.fromXp(newXp)

        if (nextRank.minXp > prevRank.minXp) newRankReached = nextRank

        player = player.copy(
            totalEarnings = newXp,
            highScore = if (currentScore > player.highScore) currentScore else player.highScore
        )

        if (player.isGuest) {
            prefs.edit().putInt("high", player.highScore).putInt("xp", player.totalEarnings).apply()
        } else {
            db.collection("players").document(player.uid).set(player)
        }

        if (correct) fetchNewPuzzle()
    }

    fun fetchNewPuzzle() {
        isLoading = true
        val request = Request.Builder().url("https://marcconrad.com/uob/banana/api.php").build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val json = JSONObject(response.body?.string() ?: "{}")
                puzzleUrl = json.optString("question")
                solution = json.optInt("solution")
                isLoading = false
            }
            override fun onFailure(call: Call, e: IOException) { isLoading = false }
        })
    }

    fun getLeaderboard(field: String) {
        db.collection("players").orderBy(field, Query.Direction.DESCENDING).limit(20).get()
            .addOnSuccessListener { res ->
                leaderboard.clear()
                leaderboard.addAll(res.toObjects(Player::class.java))
            }
    }
}

// --- 📱 MAIN UI ---
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
                        if (vm.currentScreen == Screen.HOME || vm.currentScreen == Screen.WELCOME) finish()
                        else vm.goBack()
                    }
                }
                backDispatcher?.addCallback(callback)
                onDispose { callback.remove() }
            }

            Surface(modifier = Modifier.fillMaxSize(), color = DarkObsidian) {
                Box {
                    MeshGradientBackground()
                    Column {
                        if (vm.currentScreen != Screen.WELCOME) TopHUD(vm.player)
                        AnimatedContent(
                            targetState = vm.currentScreen,
                            transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(500)) },
                            label = "Navigation"
                        ) { screen ->
                            when (screen) {
                                Screen.WELCOME -> WelcomeLayout(vm)
                                Screen.HOME -> DashboardLayout(vm)
                                Screen.MODES -> ModeLayout(vm)
                                Screen.PLAYING -> ArenaLayout(vm)
                                Screen.LEADERBOARD -> RankingLayout(vm)
                                Screen.PROFILE -> ProfileLayout(vm)
                            }
                        }
                    }
                    if (!vm.isOnline) ConnectionOverlay { vm.checkNetwork(this@MainActivity) }
                    vm.newRankReached?.let { RankUpOverlay(it) { vm.newRankReached = null } }
                }
            }
        }
    }
}

@Composable
fun MeshGradientBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "mesh")
    val offset1 by infiniteTransition.animateFloat(
        0f, 1000f, infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Reverse), label = "x"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(brush = Brush.radialGradient(colors = listOf(ElectricViolet.copy(0.15f), Color.Transparent), center = Offset(offset1, offset1), radius = 1200f))
        drawRect(brush = Brush.radialGradient(colors = listOf(CyberBlue.copy(0.1f), Color.Transparent), center = Offset(size.width - offset1, 200f), radius = 1000f))
    }
}

@Composable
fun TopHUD(player: Player) {
    val rank = Rank.fromXp(player.totalEarnings)
    Box(modifier = Modifier.statusBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clip(CircleShape).background(GlassLayer).border(1.dp, Color.White.copy(0.1f), CircleShape).padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(model = player.photoUrl.ifEmpty { "https://api.dicebear.com/7.x/avataaars/svg?seed=${player.name}" }, contentDescription = null, modifier = Modifier.size(36.dp).clip(CircleShape).border(1.dp, rank.color, CircleShape))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(player.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Box(modifier = Modifier.height(4.dp).fillMaxWidth(0.5f).clip(CircleShape).background(Color.White.copy(0.1f))) {
                    val progress = (player.totalEarnings % 2500) / 2500f
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(progress).background(rank.color))
                }
            }
            Text("${player.totalEarnings} XP", color = NeonGold, fontWeight = FontWeight.Black, fontSize = 14.sp, modifier = Modifier.padding(end = 10.dp))
        }
    }
}

@Composable
fun WelcomeLayout(vm: GameViewModel) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(res.data)
        try { vm.handleSignIn(task.getResult(ApiException::class.java).idToken!!) } catch (e: Exception) {}
    }
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(contentAlignment = Alignment.Center) {
            Surface(modifier = Modifier.size(140.dp).blur(40.dp), color = CyberBlue.copy(0.3f), shape = CircleShape) {}
            Icon(Icons.Rounded.Psychology, null, modifier = Modifier.size(100.dp), tint = Color.White)
        }
        Text("BRAINANA", fontSize = 48.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 4.sp)
        Text("NEURAL COGNITION SYSTEM", color = CyberBlue, fontSize = 12.sp, fontWeight = FontWeight.Light, letterSpacing = 2.sp)
        Spacer(Modifier.height(80.dp))
        GlassButton("SYNCHRONIZE GOOGLE", Icons.Rounded.Security, CyberBlue) {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken("113242005751-t33f11sucdci7h8egvb8lhi31s73tfp0.apps.googleusercontent.com").requestEmail().build()
            launcher.launch(GoogleSignIn.getClient(context, gso).signInIntent)
        }
        TextButton(onClick = { vm.navigateTo(Screen.HOME) }, modifier = Modifier.padding(top = 16.dp)) { Text("PROCEED AS GUEST", color = Color.White.copy(0.4f), fontSize = 12.sp) }
    }
}

@Composable
fun ArenaLayout(vm: GameViewModel) {
    val rank = Rank.fromXp(vm.player.totalEarnings)
    var timerProgress by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(vm.puzzleUrl, vm.isPaused) {
        if (vm.puzzleUrl.isNotEmpty() && !vm.isPaused) {
            timerProgress = 1f
            while (timerProgress > 0 && !vm.isPaused) {
                delay(50)
                timerProgress -= 50f / vm.selectedMode.time
            }
            if (timerProgress <= 0) { vm.submitAnswer("-1", true); vm.fetchNewPuzzle() }
        }
    }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Color.White.copy(0.1f))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(timerProgress).background(if (timerProgress < 0.3f) VividRose else rank.color))
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("CURRENT YIELD", color = Color.White.copy(0.5f), fontSize = 10.sp)
                Text("${vm.currentScore} XP", color = CyberBlue, fontSize = 24.sp, fontWeight = FontWeight.Black)
            }
            IconButton(onClick = { vm.isPaused = true }, modifier = Modifier.background(GlassLayer, CircleShape)) { Icon(Icons.Rounded.Pause, null, tint = Color.White) }
        }
        Surface(modifier = Modifier.weight(1f).fillMaxWidth(), color = GlassLayer, shape = RoundedCornerShape(32.dp), border = BorderStroke(1.dp, rank.color.copy(0.3f))) {
            Box(contentAlignment = Alignment.Center) {
                if (vm.isLoading) CircularProgressIndicator(color = rank.color)
                else { AsyncImage(model = vm.puzzleUrl, contentDescription = null, modifier = Modifier.fillMaxSize().padding(24.dp).graphicsLayer {
                    val scale = 1f + (0.02f * kotlin.math.sin(System.currentTimeMillis() / 500.0).toFloat()); scaleX = scale; scaleY = scale
                })}
            }
        }
        Spacer(Modifier.height(24.dp))
        TacticalKeypad(rank.color) { vm.submitAnswer(it) }
    }
    if (vm.isPaused) {
        Box(Modifier.fillMaxSize().background(DarkObsidian.copy(0.9f)).clickable(enabled = false){}, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("NEURAL LINK PAUSED", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp)
                Spacer(Modifier.height(32.dp))
                GlassButton("RESUME", Icons.Rounded.PlayArrow, CyberBlue) { vm.isPaused = false }
                Spacer(Modifier.height(12.dp))
                GlassButton("TERMINATE", Icons.Rounded.Close, VividRose) { vm.isPaused = false; vm.navigateTo(Screen.HOME) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TacticalKeypad(color: Color, onInput: (String) -> Unit) {
    val context = LocalContext.current
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        keys.forEach { key ->
            Surface(
                modifier = Modifier.padding(6.dp).size(85.dp).clickable {
                    (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(VibrationEffect.createOneShot(40, 50))
                    onInput(key)
                },
                shape = RoundedCornerShape(20.dp), color = GlassLayer, border = BorderStroke(1.dp, color.copy(0.2f))
            ) { Box(contentAlignment = Alignment.Center) { Text(key, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Light) } }
        }
    }
}

@Composable
fun DashboardLayout(vm: GameViewModel) {
    Column(Modifier.fillMaxSize().padding(30.dp), verticalArrangement = Arrangement.SpaceEvenly, horizontalAlignment = Alignment.CenterHorizontally) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("DATA PEAK", color = Color.White.copy(0.4f), fontWeight = FontWeight.Bold)
            Text("${vm.player.highScore}", fontSize = 100.sp, fontWeight = FontWeight.Black, color = Color.White)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            GlassButton("ENTER ARENA", Icons.Rounded.Bolt, CyberBlue) { vm.navigateTo(Screen.MODES) }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SmallCircleButton(Icons.Rounded.EmojiEvents, NeonGold) { vm.getLeaderboard("highScore"); vm.navigateTo(Screen.LEADERBOARD) }
                SmallCircleButton(Icons.Rounded.Person, Color.White) { vm.navigateTo(Screen.PROFILE) }
            }
        }
    }
}

@Composable
fun ModeLayout(vm: GameViewModel) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("INTENSITY SELECTION", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(32.dp))
        Mode.entries.forEach { mode ->
            val color = when(mode) { Mode.EASY -> Color(0xFF4ADE80); Mode.MEDIUM -> NeonGold; Mode.HARD -> VividRose }
            Surface(
                onClick = { vm.selectedMode = mode; vm.currentScore = 0; vm.fetchNewPuzzle(); vm.navigateTo(Screen.PLAYING) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                color = color.copy(0.1f), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, color.copy(0.5f))
            ) {
                Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(mode.name, color = color, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text(mode.desc, color = Color.White.copy(0.5f), fontSize = 12.sp)
                    }
                    Text("${mode.bonus}x XP", color = color, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RankingLayout(vm: GameViewModel) {
    var tab by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("HALL OF FAME", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
        TabRow(selectedTabIndex = tab, containerColor = Color.Transparent, contentColor = CyberBlue, divider = {}) {
            Tab(selected = tab == 0, onClick = { tab = 0; vm.getLeaderboard("highScore") }) { Text("SCORE", Modifier.padding(16.dp)) }
            Tab(selected = tab == 1, onClick = { tab = 1; vm.getLeaderboard("totalEarnings") }) { Text("XP", Modifier.padding(16.dp)) }
        }
        LazyColumn(Modifier.weight(1f).padding(top = 16.dp)) {
            itemsIndexed(vm.leaderboard) { i, p ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(16.dp)).background(GlassLayer).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("#${i+1}", color = CyberBlue, fontWeight = FontWeight.Black, modifier = Modifier.width(40.dp))
                    Text(p.name, color = Color.White, modifier = Modifier.weight(1f))
                    Text(if(tab==0) "${p.highScore}" else "${p.totalEarnings}", color = NeonGold, fontWeight = FontWeight.Bold)
                }
            }
        }
        GlassButton("BACK", Icons.Rounded.ArrowBack, Color.White.copy(0.2f)) { vm.goBack() }
    }
}

@Composable
fun ProfileLayout(vm: GameViewModel) {
    val rank = Rank.fromXp(vm.player.totalEarnings)
    Column(Modifier.fillMaxSize().padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(40.dp))
        Surface(modifier = Modifier.size(120.dp).border(2.dp, rank.color, CircleShape), shape = CircleShape, color = GlassLayer) { AsyncImage(model = vm.player.photoUrl, contentDescription = null, contentScale = ContentScale.Crop) }
        Text(vm.player.name, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 20.dp))
        Text(rank.label, color = rank.color, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
        Spacer(Modifier.height(48.dp))
        StatRow("COLLECTED DATA", "${vm.player.totalEarnings} XP")
        StatRow("SYSTEM CLEARANCE", if(vm.player.isGuest) "GUEST" else "VERIFIED")
        Spacer(Modifier.weight(1f))
        GlassButton("RETURN", Icons.Rounded.ArrowBack, Color.White.copy(0.1f)) { vm.goBack() }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.White.copy(0.4f), fontSize = 12.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold)
    }
    HorizontalDivider(color = Color.White.copy(0.05f))
}

@Composable
fun GlassButton(text: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth(0.85f).height(64.dp), colors = ButtonDefaults.buttonColors(containerColor = color), shape = RoundedCornerShape(20.dp)) {
        Icon(icon, null, tint = DarkObsidian)
        Spacer(Modifier.width(12.dp))
        Text(text, color = DarkObsidian, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
    }
}

@Composable
fun SmallCircleButton(icon: ImageVector, color: Color, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.size(64.dp), shape = CircleShape, color = GlassLayer, border = BorderStroke(1.dp, color.copy(0.3f))) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = color) } }
}

@Composable
fun RankUpOverlay(rank: Rank, onDismiss: () -> Unit) {
    Box(Modifier.fillMaxSize().background(DarkObsidian.copy(0.95f)).clickable { onDismiss() }, Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("PROMOTION SECURED", color = rank.color, letterSpacing = 8.sp, fontSize = 12.sp)
            Text(rank.label, color = Color.White, fontSize = 54.sp, fontWeight = FontWeight.Black)
            Icon(rank.icon, null, modifier = Modifier.size(120.dp), tint = rank.color)
        }
    }
}

@Composable
fun ConnectionOverlay(onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().background(DarkObsidian).clickable(enabled = false){}, Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.SignalWifiStatusbarConnectedNoInternet4, null, modifier = Modifier.size(80.dp), tint = VividRose)
            Text("NEURAL LINK SEVERED", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
            Spacer(Modifier.height(32.dp))
            GlassButton("RETRY CONNECTION", Icons.Rounded.Refresh, CyberBlue) { onRetry() }
        }
    }
}