package com.prsnl.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.prsnl.ui.R

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun WelcomeOnboardingModal(
    onDismiss: () -> Unit,
    onComplete: (name: String, initialFolder: String?) -> Unit
) {
    var nameInput by remember { mutableStateOf("") }
    var folderInput by remember { mutableStateOf("") }
    var wantsFolder by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    val contentAlpha by animateFloatAsState(
        targetValue = if (isSubmitting) 0f else 1f,
        animationSpec = tween(durationMillis = 280, easing = LinearEasing),
        label = "WelcomeAlpha"
    )

    Dialog(
        onDismissRequest = {
            if (!isSubmitting) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1E1C1A)),
            color = Color(0xFF1E1C1A)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .verticalScroll(rememberScrollState())
                        .alpha(contentAlpha),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(20.dp))

                    // Brand Identity Icon
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF2B2825))
                            .border(1.5.dp, Color(0xFFC88A4B), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_logo_vector),
                            contentDescription = "prsnl",
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Welcome to prsnl",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFAF8F5),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Personal handwriting, notebook organization, and PDF markup built for focused thinkers.",
                        fontSize = 13.sp,
                        color = Color(0xFFB0A99F),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Name Input Section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF262422))
                            .border(1.dp, Color(0xFF383430), RoundedCornerShape(16.dp))
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFFC88A4B),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "What should we call you?",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFAF8F5)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            placeholder = {
                                Text("Enter your name (e.g. Alex, Sarah)", color = Color(0xFF6B655D), fontSize = 14.sp)
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1E1C1A),
                                unfocusedContainerColor = Color(0xFF1E1C1A),
                                focusedBorderColor = Color(0xFFC88A4B),
                                unfocusedBorderColor = Color(0xFF3D3934),
                                focusedTextColor = Color(0xFFFAF8F5),
                                unfocusedTextColor = Color(0xFFFAF8F5)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Optional First Folder Toggle
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF262422))
                            .border(1.dp, Color(0xFF383430), RoundedCornerShape(16.dp))
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { wantsFolder = !wantsFolder },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = Color(0xFFC88A4B),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Create your first folder",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFFAF8F5)
                                    )
                                    Text(
                                        text = "Optional: you can also proceed directly",
                                        fontSize = 11.sp,
                                        color = Color(0xFF8E887E)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (wantsFolder) Color(0xFFC88A4B) else Color(0xFF383430)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (wantsFolder) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color(0xFF8E887E),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = wantsFolder,
                            enter = fadeIn(animationSpec = tween(200, easing = LinearEasing)) + slideInVertically(),
                            exit = fadeOut(animationSpec = tween(150, easing = LinearEasing))
                        ) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                OutlinedTextField(
                                    value = folderInput,
                                    onValueChange = { folderInput = it },
                                    placeholder = {
                                        Text("Folder name (e.g. Work, Maths, Ideas)", color = Color(0xFF6B655D), fontSize = 14.sp)
                                    },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF1E1C1A),
                                        unfocusedContainerColor = Color(0xFF1E1C1A),
                                        focusedBorderColor = Color(0xFFC88A4B),
                                        unfocusedBorderColor = Color(0xFF3D3934),
                                        focusedTextColor = Color(0xFFFAF8F5),
                                        unfocusedTextColor = Color(0xFFFAF8F5)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    // Primary Action Button
                    Button(
                        onClick = {
                            isSubmitting = true
                            val finalFolder = if (wantsFolder && folderInput.isNotBlank()) folderInput.trim() else null
                            onComplete(nameInput.trim(), finalFolder)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFC88A4B),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Enter Workspace",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(
                        onClick = {
                            isSubmitting = true
                            onComplete("", null)
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF8E887E))
                    ) {
                        Text(
                            text = "Skip for now",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}
