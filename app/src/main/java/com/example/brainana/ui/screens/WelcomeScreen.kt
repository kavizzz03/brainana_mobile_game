package com.example.brainana.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.example.brainana.ui.components.GlassButton
import com.example.brainana.ui.components.AuthProgressOverlay
import com.example.brainana.ui.viewmodel.GameViewModel
import com.example.brainana.utils.Constants
import androidx.compose.foundation.background

@Composable
fun WelcomeScreen(vm: GameViewModel) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(res.data)
        try {
            vm.startAuthenticationProcess()
            vm.handleSignIn(task.getResult(ApiException::class.java).idToken!!)
        } catch (e: Exception) {
            vm.authError = e.message ?: "Authentication failed"
            vm.isAuthenticating = false
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    Modifier
                        .size(160.dp)
                        .blur(50.dp),
                    color = vm.selectedTheme.primary.copy(0.2f),
                    shape = CircleShape
                ) {}
                Icon(
                    Icons.Rounded.Psychology,
                    null,
                    Modifier.size(110.dp),
                    tint = Color.White
                )
            }

            Text(
                "BRAINANA",
                fontSize = 52.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 6.sp
            )
            Text(
                "COGNITIVE TRAINING",
                color = vm.selectedTheme.primary,
                fontSize = 12.sp,
                letterSpacing = 3.sp
            )

            Spacer(Modifier.height(80.dp))

            GlassButton(
                "SYNCHRONIZE GOOGLE",
                Icons.Rounded.Security,
                vm.selectedTheme.primary,
                vm.selectedTheme.bg
            ) {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(Constants.GOOGLE_CLIENT_ID)
                    .requestEmail()
                    .build()
                launcher.launch(GoogleSignIn.getClient(context, gso).signInIntent)
            }

            TextButton(onClick = { vm.proceedFromWelcome() }) {
                Text(
                    "ENTER AS GUEST",
                    color = Color.White.copy(0.4f),
                    fontSize = 11.sp
                )
            }
        }

        // Authentication Progress Overlay
        if (vm.isAuthenticating) {
            AuthProgressOverlay(
                progress = vm.authProgress,
                statusMessage = vm.authStatusMessage,
                theme = vm.selectedTheme
            )
        }

        // Authentication Error Display
        if (vm.authError.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .padding(32.dp),
                    color = Color.DarkGray,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "AUTHENTICATION FAILED",
                            color = Color.Red,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            vm.authError,
                            color = Color.White,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = { vm.authError = "" }) {
                            Text("DISMISS", color = vm.selectedTheme.primary)
                        }
                    }
                }
            }
        }
    }
}