package com.prsnl.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.prsnl.ui.R
import kotlinx.coroutines.delay

private enum class OnboardingStep {
    WELCOME_NAME,
    CREATE_FOLDER,
    SETTING_UP
}

private val FOLDER_COLORS = listOf(
    0xFF8B5E3C.toInt(), // Warm Leather Brown
    0xFFC85A32.toInt(), // Terracotta Clay
    0xFF4A7C59.toInt(), // Forest Sage
    0xFF2C4A6F.toInt(), // Deep Slate Navy
    0xFFD4A373.toInt(), // Warm Mustard Ochre
    0xFF4A4640.toInt()  // Charcoal Ink
)

@Composable
fun WelcomeOnboardingModal(
    onDismiss: () -> Unit,
    onComplete: (name: String, initialFolder: String?, folderColor: Int?) -> Unit
) {
    var currentStep by remember { mutableStateOf(OnboardingStep.WELCOME_NAME) }
    var nameInput by remember { mutableStateOf("") }
    var folderInput by remember { mutableStateOf("") }
    var selectedFolderColor by remember { mutableIntStateOf(FOLDER_COLORS.first()) }
    var createdFolderName by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = {
            if (currentStep != OnboardingStep.SETTING_UP) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFFBF9F4)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut()
                            )
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut()
                            )
                        }
                    },
                    label = "OnboardingStepTransition"
                ) { step ->
                    when (step) {
                        OnboardingStep.WELCOME_NAME -> {
                            WelcomeNameStep(
                                name = nameInput,
                                onNameChange = { nameInput = it },
                                onNext = { currentStep = OnboardingStep.CREATE_FOLDER }
                            )
                        }
                        OnboardingStep.CREATE_FOLDER -> {
                            CreateFolderStep(
                                folderName = folderInput,
                                onFolderNameChange = { folderInput = it },
                                selectedColor = selectedFolderColor,
                                onColorChange = { selectedFolderColor = it },
                                onNext = { folderToCreate ->
                                    createdFolderName = folderToCreate
                                    currentStep = OnboardingStep.SETTING_UP
                                },
                                onSkip = {
                                    createdFolderName = null
                                    currentStep = OnboardingStep.SETTING_UP
                                }
                            )
                        }
                        OnboardingStep.SETTING_UP -> {
                            SettingUpWorkspaceStep(
                                onFinished = {
                                    onComplete(nameInput.trim(), createdFolderName, selectedFolderColor)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeNameStep(
    name: String,
    onNameChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Step Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFC88A4B).copy(alpha = 0.12f))
                .padding(horizontal = 12.dp, vertical = 5.dp)
        ) {
            Text(
                text = "STEP 1 OF 2",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFC88A4B),
                letterSpacing = 1.2.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Brand Identity Icon
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFFF2ECE1))
                .border(1.5.dp, Color(0xFFD8CFBF), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_logo_vector),
                contentDescription = "prsnl Logo",
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Welcome to prsnl",
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF2D2B28),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your limitless canvas for ideas, handwritten knowledge, and structured thought.",
            fontSize = 14.sp,
            color = Color(0xFF686259),
            textAlign = TextAlign.Center,
            lineHeight = 21.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Name Input Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFE8E2D8)),
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFC88A4B).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFFC88A4B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "What should we call you?",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2D2B28)
                        )
                        Text(
                            text = "Personalizes your daily workspace greeting",
                            fontSize = 11.sp,
                            color = Color(0xFF8E887E)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    placeholder = {
                        Text(
                            "Enter your name (leave blank for 'Boss')",
                            color = Color(0xFF9E988E),
                            fontSize = 14.sp
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFFAF8F5),
                        unfocusedContainerColor = Color(0xFFFAF8F5),
                        focusedBorderColor = Color(0xFFC88A4B),
                        unfocusedBorderColor = Color(0xFFDDD6C9),
                        focusedTextColor = Color(0xFF2D2B28),
                        unfocusedTextColor = Color(0xFF2D2B28)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Next Button
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFC88A4B),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Continue",
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
    }
}

@Composable
private fun CreateFolderStep(
    folderName: String,
    onFolderNameChange: (String) -> Unit,
    selectedColor: Int,
    onColorChange: (Int) -> Unit,
    onNext: (folderName: String?) -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Step Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFC88A4B).copy(alpha = 0.12f))
                .padding(horizontal = 12.dp, vertical = 5.dp)
        ) {
            Text(
                text = "STEP 2 OF 2",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFC88A4B),
                letterSpacing = 1.2.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Create Your First Folder",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF2D2B28),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Folders keep your notes and notebooks organized. Choose a style or skip to start fresh.",
            fontSize = 13.sp,
            color = Color(0xFF686259),
            textAlign = TextAlign.Center,
            lineHeight = 19.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Tactile Real Folder Preview Card
        TactileFolderPreview(
            name = folderName,
            color = Color(selectedColor)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Folder Settings Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFE8E2D8)),
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Folder Name",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D2B28)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = folderName,
                    onValueChange = onFolderNameChange,
                    placeholder = {
                        Text(
                            "e.g. Field Notes, Work, Journal",
                            color = Color(0xFF9E988E),
                            fontSize = 14.sp
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFFAF8F5),
                        unfocusedContainerColor = Color(0xFFFAF8F5),
                        focusedBorderColor = Color(0xFFC88A4B),
                        unfocusedBorderColor = Color(0xFFDDD6C9),
                        focusedTextColor = Color(0xFF2D2B28),
                        unfocusedTextColor = Color(0xFF2D2B28)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Folder Color",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D2B28)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Color swatches row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FOLDER_COLORS.forEach { colorInt ->
                        val isSelected = selectedColor == colorInt
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(colorInt))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF2D2B28) else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { onColorChange(colorInt) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        // Action Buttons
        Button(
            onClick = {
                val finalName = folderName.trim().ifBlank { "Notes" }
                onNext(finalName)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFC88A4B),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CreateNewFolder,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (folderName.isNotBlank()) "Create Folder & Continue" else "Create Default Folder",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        TextButton(
            onClick = onSkip,
            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF756F64)),
            modifier = Modifier.height(44.dp)
        ) {
            Text(
                text = "Skip for now (start with no folders)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun TactileFolderPreview(
    name: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Folder Tab Protrusion at top
        Box(
            modifier = Modifier
                .padding(start = 14.dp)
                .width(105.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(color)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.85f))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "FOLDER",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White.copy(alpha = 0.95f),
                    letterSpacing = 1.sp
                )
            }
        }

        // Main Folder Body
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(
                topStart = 0.dp,
                topEnd = 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            color = color.copy(alpha = 0.12f),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, color.copy(alpha = 0.45f)),
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(color.copy(alpha = 0.22f))
                        .border(1.2.dp, color, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (name.isNotBlank()) name else "Untitled Folder",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D2B28),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "0 notebooks • Ready for notes",
                        fontSize = 12.sp,
                        color = Color(0xFF686259)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingUpWorkspaceStep(
    onFinished: () -> Unit
) {
    var targetProgress by remember { mutableFloatStateOf(0f) }

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
        label = "SetupProgress"
    )

    LaunchedEffect(Unit) {
        targetProgress = 1f
        delay(1300)
        onFinished()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth(0.88f)
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo Container
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFF2ECE1))
                .border(1.5.dp, Color(0xFFD8CFBF), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_logo_vector),
                contentDescription = "prsnl Logo",
                modifier = Modifier.size(52.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Setting up things for you...",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2D2B28),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Preparing your stationery workspace and notebooks.",
            fontSize = 13.sp,
            color = Color(0xFF756F64),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(36.dp))

        // Progress Indicator
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .width(220.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = Color(0xFFC88A4B),
            trackColor = Color(0xFFE8E2D8),
            strokeCap = StrokeCap.Round
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "${(animatedProgress * 100).toInt()}%",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFC88A4B)
        )
    }
}
