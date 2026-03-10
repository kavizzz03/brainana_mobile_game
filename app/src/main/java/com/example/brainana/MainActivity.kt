package com.example.brainana

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

// --- 🎨 ENHANCED CYBER PALETTE ---
val DarkObsidian = Color(0xFF020204)
val CyberBlue = Color(0xFF00D1FF)
val ElectricViolet = Color(0xFF8B5CF6)
val NeonGold = Color(0xFFFFD700)
val VividRose = Color(0xFFFF2E63)
val GlassSurface = Color(0x1AFFFFFF)
val GlassBorder = Color(0x33FFFFFF)

enum class Screen { WELCOME, HOME, MODES, PLAYING, LEADERBOARD, PROFILE, AVATAR_SELECT }
enum class Mode(val time: Long, val bonus: Int, val desc: String, val color: Color) {
    EASY(20000L, 1, "Standard Neural Sync", Color(0xFF4ADE80)),
    MEDIUM(12000L, 2, "Accelerated Processing", NeonGold),
    HARD(7000L, 4, "Overclocked Protocol", VividRose)
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
    val avatarStyle: String = "bottts",
    val isGuest: Boolean = true
)

// --- 🧠 CORE ENGINE ---
class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val client = OkHttpClient()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val prefs = application.getSharedPreferences("brain_v3_prefs", Context.MODE_PRIVATE)

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
            avatarStyle = prefs.getString("style", "bottts") ?: "bottts",
            isGuest = true
        )
    }

    fun updateAvatar(style: String) {
        player = player.copy(avatarStyle = style)
        if (player.isGuest) {
            prefs.edit().putString("style", style).apply()
        } else {
            db.collection("players").document(player.uid).update("avatarStyle", style)
        }
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
                val p = Player(uid, name, 0, player.totalEarnings, photo, player.avatarStyle, false)
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

    fun signOut(context: Context) {
        auth.signOut()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        GoogleSignIn.getClient(context, gso).signOut()

        player = Player(
            name = prefs.getString("name", "Agent Guest") ?: "Agent Guest",
            highScore = prefs.getInt("high", 0),
            totalEarnings = prefs.getInt("xp", 0),
            avatarStyle = prefs.getString("style", "bottts") ?: "bottts",
            isGuest = true
        )

        backStack.clear()
        backStack.add(Screen.WELCOME)
        currentScreen = Screen.WELCOME
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
                            transitionSpec = { slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut() },
                            label = "Nav"
                        ) { screen ->
                            when (screen) {
                                Screen.WELCOME -> WelcomeLayout(vm)
                                Screen.HOME -> DashboardLayout(vm)
                                Screen.MODES -> ModeLayout(vm)
                                Screen.PLAYING -> ArenaLayout(vm)
                                Screen.LEADERBOARD -> RankingLayout(vm)
                                Screen.PROFILE -> ProfileLayout(vm)
                                Screen.AVATAR_SELECT -> AvatarSelectionLayout(vm)
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
    val offset1 by infiniteTransition.animateFloat(0f, 1500f, infiniteRepeatable(tween(15000, easing = LinearEasing), RepeatMode.Reverse), label = "x")
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(brush = Brush.radialGradient(colors = listOf(ElectricViolet.copy(0.12f), Color.Transparent), center = Offset(offset1, 300f), radius = 1000f))
        drawRect(brush = Brush.radialGradient(colors = listOf(CyberBlue.copy(0.1f), Color.Transparent), center = Offset(size.width - offset1, size.height - 300f), radius = 1200f))
    }
}

@Composable
fun TopHUD(player: Player) {
    val rank = Rank.fromXp(player.totalEarnings)
    val avatarUrl = "https://api.dicebear.com/9.x/${player.avatarStyle}/png?seed=${player.name}&size=128"

    Box(modifier = Modifier.statusBarsPadding().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(GlassSurface).border(1.dp, GlassBorder, RoundedCornerShape(24.dp)).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                modifier = Modifier.size(42.dp).clip(CircleShape).background(DarkObsidian).border(1.5.dp, rank.color, CircleShape)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(player.name.uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (player.totalEarnings % 2500) / 2500f },
                    modifier = Modifier.fillMaxWidth(0.7f).height(4.dp).clip(CircleShape),
                    color = rank.color,
                    trackColor = Color.White.copy(0.05f)
                )
            }
            Text("${player.totalEarnings} XP", color = NeonGold, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
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
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(contentAlignment = Alignment.Center) {
            Surface(Modifier.size(160.dp).blur(50.dp), color = CyberBlue.copy(0.2f), shape = CircleShape) {}
            Icon(Icons.Rounded.Psychology, null, Modifier.size(110.dp), tint = Color.White)
        }
        Text("BRAINANA", fontSize = 52.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 6.sp)
        Text("NEURAL INTERFACE", color = CyberBlue, fontSize = 12.sp, letterSpacing = 3.sp)
        Spacer(Modifier.height(80.dp))
        GlassButton("SYNCHRONIZE GOOGLE", Icons.Rounded.Security, CyberBlue) {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken("113242005751-t33f11sucdci7h8egvb8lhi31s73tfp0.apps.googleusercontent.com").requestEmail().build()
            launcher.launch(GoogleSignIn.getClient(context, gso).signInIntent)
        }
        TextButton(onClick = { vm.navigateTo(Screen.HOME) }) { Text("ENTER AS GUEST", color = Color.White.copy(0.3f), fontSize = 11.sp) }
    }
}

@Composable
fun DashboardLayout(vm: GameViewModel) {
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val floatAnim by infiniteTransition.animateFloat(0f, -15f, infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse), label = "y")

    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.graphicsLayer { translationY = floatAnim }) {
            Text("PEAK SCORE", color = Color.White.copy(0.4f), fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text("${vm.player.highScore}", fontSize = 110.sp, fontWeight = FontWeight.Black, color = Color.White)
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            GlassButton("LAUNCH ARENA", Icons.Rounded.Bolt, CyberBlue) { vm.navigateTo(Screen.MODES) }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallCircleButton(Icons.Rounded.EmojiEvents, NeonGold) { vm.getLeaderboard("highScore"); vm.navigateTo(Screen.LEADERBOARD) }
                SmallCircleButton(Icons.Rounded.Face, Color.White) { vm.navigateTo(Screen.AVATAR_SELECT) }
                SmallCircleButton(Icons.Rounded.Person, Color.White) { vm.navigateTo(Screen.PROFILE) }
            }
        }
    }
}

@Composable
fun AvatarSelectionLayout(vm: GameViewModel) {
    val styles = listOf("bottts", "bottts-neutral", "adventurer", "avataaars", "micah", "lorelei", "pixel-art", "notionists")
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("SELECT PERSONA", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(20.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(styles) { style ->
                val isSelected = vm.player.avatarStyle == style
                Box(
                    modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(24.dp)).background(if (isSelected) CyberBlue.copy(0.2f) else GlassSurface).border(2.dp, if (isSelected) CyberBlue else GlassBorder, RoundedCornerShape(24.dp)).clickable { vm.updateAvatar(style) },
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
        GlassButton("CONFIRM", Icons.Rounded.Check, CyberBlue) { vm.goBack() }
    }
}

@Composable
fun ModeLayout(vm: GameViewModel) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("INTENSITY SELECTION", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(32.dp))
        Mode.entries.forEach { mode ->
            Surface(
                onClick = { vm.selectedMode = mode; vm.currentScore = 0; vm.fetchNewPuzzle(); vm.navigateTo(Screen.PLAYING) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                color = mode.color.copy(0.1f), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, mode.color.copy(0.4f))
            ) {
                Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(mode.name, color = mode.color, fontWeight = FontWeight.Black, fontSize = 20.sp)
                        Text(mode.desc, color = Color.White.copy(0.5f), fontSize = 12.sp)
                    }
                    Text("${mode.bonus}X", color = mode.color, fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
fun ArenaLayout(vm: GameViewModel) {
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
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        LinearProgressIndicator(progress = { timerProgress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = if (timerProgress < 0.3f) VividRose else vm.selectedMode.color, trackColor = Color.White.copy(0.1f))

        Row(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("YIELD: ${vm.currentScore} XP", color = CyberBlue, fontSize = 22.sp, fontWeight = FontWeight.Black)
            IconButton(onClick = { vm.isPaused = true }, modifier = Modifier.background(GlassSurface, CircleShape)) { Icon(Icons.Rounded.Pause, null, tint = Color.White) }
        }

        Surface(modifier = Modifier.weight(1f).fillMaxWidth(), color = GlassSurface, shape = RoundedCornerShape(32.dp), border = BorderStroke(1.dp, GlassBorder)) {
            Box(contentAlignment = Alignment.Center) {
                if (vm.isLoading) CircularProgressIndicator(color = CyberBlue)
                else AsyncImage(model = vm.puzzleUrl, contentDescription = null, modifier = Modifier.fillMaxSize().padding(24.dp))
            }
        }

        Spacer(Modifier.height(24.dp))
        TacticalKeypad(vm.selectedMode.color) { vm.submitAnswer(it) }
    }

    if (vm.isPaused) {
        Box(Modifier.fillMaxSize().background(DarkObsidian.copy(0.95f)).clickable(enabled = false){}, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("PROCESS SUSPENDED", color = Color.White, fontWeight = FontWeight.Black, fontSize = 26.sp)
                Spacer(Modifier.height(40.dp))
                GlassButton("RESUME", Icons.Rounded.PlayArrow, CyberBlue) { vm.isPaused = false }
                Spacer(Modifier.height(12.dp))
                GlassButton("ABORT", Icons.Rounded.Close, VividRose) { vm.isPaused = false; vm.navigateTo(Screen.HOME) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TacticalKeypad(color: Color, onInput: (String) -> Unit) {
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        keys.forEach { key ->
            Surface(
                modifier = Modifier.padding(6.dp).size(65.dp).clickable { onInput(key) },
                shape = RoundedCornerShape(16.dp), color = GlassSurface, border = BorderStroke(1.dp, color.copy(0.3f))
            ) { Box(contentAlignment = Alignment.Center) { Text(key, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold) } }
        }
    }
}

@Composable
fun RankingLayout(vm: GameViewModel) {
    var tab by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("HALL OF FAME", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
        TabRow(selectedTabIndex = tab, containerColor = Color.Transparent, contentColor = CyberBlue, divider = {}) {
            Tab(selected = tab == 0, onClick = { tab = 0; vm.getLeaderboard("highScore") }) { Text("SCORE", Modifier.padding(16.dp)) }
            Tab(selected = tab == 1, onClick = { tab = 1; vm.getLeaderboard("totalEarnings") }) { Text("XP", Modifier.padding(16.dp)) }
        }
        LazyColumn(Modifier.weight(1f).padding(top = 16.dp)) {
            itemsIndexed(vm.leaderboard) { i, p ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(20.dp)).background(GlassSurface).border(1.dp, GlassBorder, RoundedCornerShape(20.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("#${i+1}", color = CyberBlue, fontWeight = FontWeight.Black, modifier = Modifier.width(40.dp))
                    Text(p.name, color = Color.White, modifier = Modifier.weight(1f))
                    Text(if(tab==0) "${p.highScore}" else "${p.totalEarnings}", color = NeonGold, fontWeight = FontWeight.Bold)
                }
            }
        }
        GlassButton("BACK", Icons.Rounded.ArrowBack, Color.White.copy(0.1f)) { vm.goBack() }
    }
}

@Composable
fun ProfileLayout(vm: GameViewModel) {
    val context = LocalContext.current
    val rank = Rank.fromXp(vm.player.totalEarnings)
    val avatarUrl = "https://api.dicebear.com/9.x/${vm.player.avatarStyle}/png?seed=${vm.player.name}&size=256"

    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Surface(Modifier.size(140.dp).clip(CircleShape).border(3.dp, rank.color, CircleShape).background(GlassSurface)) {
                AsyncImage(model = avatarUrl, contentDescription = null, contentScale = ContentScale.Crop)
            }
            Surface(Modifier.size(40.dp), color = rank.color, shape = CircleShape) {
                Icon(rank.icon, null, Modifier.padding(8.dp), tint = DarkObsidian)
            }
        }
        Text(vm.player.name, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 24.dp))
        Text(rank.label, color = rank.color, fontWeight = FontWeight.ExtraBold, letterSpacing = 6.sp)

        Spacer(Modifier.height(50.dp))
        StatRow("TOTAL XP", "${vm.player.totalEarnings} XP")
        StatRow("CLEARANCE", if(vm.player.isGuest) "GUEST" else "VERIFIED")

        Spacer(Modifier.weight(1f))
        GlassButton("DISCONNECT", Icons.Rounded.Logout, VividRose) { vm.signOut(context) }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.White.copy(0.4f), fontSize = 13.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold)
    }
    HorizontalDivider(color = Color.White.copy(0.05f))
}

@Composable
fun GlassButton(text: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(60.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(18.dp)
    ) {
        Icon(icon, null, tint = DarkObsidian)
        Spacer(Modifier.width(12.dp))
        Text(text, color = DarkObsidian, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
    }
}

@Composable
fun SmallCircleButton(icon: ImageVector, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(56.dp),
        shape = CircleShape,
        color = GlassSurface,
        border = BorderStroke(1.dp, color.copy(0.4f))
    ) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = color) } }
}

@Composable
fun RankUpOverlay(rank: Rank, onDismiss: () -> Unit) {
    Box(Modifier.fillMaxSize().background(DarkObsidian.copy(0.95f)).clickable { onDismiss() }, Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("PROMOTION SECURED", color = rank.color, letterSpacing = 8.sp, fontSize = 12.sp)
            Text(rank.label, color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Black)
            Icon(rank.icon, null, modifier = Modifier.size(140.dp), tint = rank.color)
            Text("ACCESS GRANTED", color = Color.White.copy(0.4f), modifier = Modifier.padding(top = 20.dp))
        }
    }
}

@Composable
fun ConnectionOverlay(onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().background(DarkObsidian).padding(32.dp), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.CloudOff, null, Modifier.size(100.dp), tint = VividRose)
            Text("NEURAL LINK SEVERED", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp)
            Spacer(Modifier.height(40.dp))
            GlassButton("RETRY SYNC", Icons.Rounded.Refresh, CyberBlue) { onRetry() }
        }
    }
}