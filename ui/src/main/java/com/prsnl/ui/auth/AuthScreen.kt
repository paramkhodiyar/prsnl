package com.prsnl.ui.auth

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.prsnl.storage.sync.SyncState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Stationery Warm Colors
private val IvoryBackground = Color(0xFFFBF9F4)
private val MoleskineSurface = Color(0xFFF5F0E6)
private val GoldAccent = Color(0xFFC88A4B)
private val CharcoalText = Color(0xFF2D2B28)
private val SoftBorderColor = Color(0xFFE8E2D5)
private val SuccessGreen = Color(0xFF2E7D32)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val lastSyncTimestamp by viewModel.lastSyncTimestamp.collectAsState()
    val syncErrorMessage by viewModel.syncErrorMessage.collectAsState()
    val authError by viewModel.authError.collectAsState()
    val isSigningIn by viewModel.isSigningIn.collectAsState()

    // Google Sign-In Launcher Setup
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken != null) {
                    viewModel.onGoogleSignInToken(idToken)
                } else {
                    Toast.makeText(context, "Failed to retrieve Google token", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                Toast.makeText(context, "Google Sign-In failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun launchGoogleSignIn() {
        val webClientId = try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) context.getString(resId) else null
        } catch (e: Exception) { null }

        val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()

        if (!webClientId.isNullOrBlank()) {
            gsoBuilder.requestIdToken(webClientId)
        }

        val gso = gsoBuilder.build()
        val googleSignInClient = GoogleSignIn.getClient(context, gso)
        googleSignInLauncher.launch(googleSignInClient.signInIntent)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Cloud Sync & Account",
                        fontWeight = FontWeight.Bold,
                        color = CharcoalText
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = CharcoalText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = IvoryBackground)
            )
        },
        containerColor = IvoryBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {

            // Top Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MoleskineSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(GoldAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Cloud Sync Logo",
                            tint = GoldAccent,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Multi-Device Sync",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalText
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Seamlessly sync notebooks across your Android Tablet and MacBook.",
                        fontSize = 14.sp,
                        color = CharcoalText.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (currentUser == null) {
                // SIGNED OUT STATE
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SoftBorderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Sign in to enable sync",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CharcoalText
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Connect your Google account to back up your handwritten notes and sync with your MacBook.",
                            fontSize = 13.sp,
                            color = CharcoalText.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        if (isSigningIn) {
                            CircularProgressIndicator(color = GoldAccent)
                        } else {
                            Button(
                                onClick = { launchGoogleSignIn() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Google Icon",
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Sign in with Google",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        if (authError != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = authError!!,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // SIGNED IN STATE
                val user = currentUser!!

                // User Profile Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SoftBorderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(GoldAccent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user.displayName?.take(1)?.uppercase() ?: user.email?.take(1)?.uppercase() ?: "U",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user.displayName ?: "Google User",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CharcoalText
                            )
                            Text(
                                text = user.email ?: "",
                                fontSize = 13.sp,
                                color = CharcoalText.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Connected",
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Signed In & Ready to Sync",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SuccessGreen
                                )
                            }
                        }
                    }
                }

                // Sync Controls Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MoleskineSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SoftBorderColor)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Manual Sync",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap to sync your local notebooks immediately with the cloud.",
                            fontSize = 12.sp,
                            color = CharcoalText.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.triggerSync() },
                            enabled = syncState != SyncState.SYNCING,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                        ) {
                            if (syncState == SyncState.SYNCING) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Syncing...", color = Color.White, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Sync Now",
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sync Now", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (lastSyncTimestamp != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            val formattedTime = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(Date(lastSyncTimestamp!!))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = "Last Synced",
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Last Synced: $formattedTime",
                                    fontSize = 12.sp,
                                    color = CharcoalText.copy(alpha = 0.7f)
                                )
                            }
                        }

                        if (syncErrorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = syncErrorMessage!!,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Nightly Auto-Sync Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SoftBorderColor)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NightlightRound,
                            contentDescription = "Nightly Auto Sync",
                            tint = GoldAccent,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Nightly Auto-Sync Active",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = CharcoalText
                            )
                            Text(
                                text = "Runs automatically overnight (2 AM - 4 AM) when charging and connected to Wi-Fi.",
                                fontSize = 12.sp,
                                color = CharcoalText.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                // Sign Out Button
                OutlinedButton(
                    onClick = { viewModel.signOut() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Text(
                        text = "Sign Out",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun String?.isNull_or_Empty(): Boolean = this == null || this.trim().isEmpty()
