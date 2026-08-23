package com.prsnl.ui.security

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.security.MessageDigest

val PREDEFINED_SECURITY_QUESTIONS = listOf(
    "What was the name of your first pet?",
    "What is your primary school name?",
    "What city were you born in?",
    "What is your favorite book title?",
    "What was your childhood nickname?"
)

fun hashSecurityAnswer(answer: String): String {
    val normalized = answer.trim().lowercase()
    val bytes = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

enum class PinModalMode {
    SETUP_PIN,
    UNLOCK,
    RESET_PIN
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinKeypadModal(
    mode: PinModalMode,
    folderName: String,
    existingPin: String? = null,
    existingQuestion: String? = null,
    existingAnswerHash: String? = null,
    onSuccessPinSet: (pin: String, question: String, answerHash: String) -> Unit,
    onSuccessUnlocked: () -> Unit,
    onDismiss: () -> Unit
) {
    var step by remember { mutableStateOf(if (mode == PinModalMode.SETUP_PIN) 1 else 0) }
    var enteredPin by remember { mutableStateOf("") }
    var firstPinAttempt by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Security Question state for setup & reset
    var selectedQuestion by remember { mutableStateOf(PREDEFINED_SECURITY_QUESTIONS.first()) }
    var securityAnswerInput by remember { mutableStateOf("") }
    var questionExpanded by remember { mutableStateOf(false) }
    var isResettingMode by remember { mutableStateOf(mode == PinModalMode.RESET_PIN) }

    fun triggerErrorShake(msg: String) {
        errorMessage = msg
        enteredPin = ""
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFFAF8F5),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, Color(0xFFE2D7C5), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon & Title
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF5F0E6))
                        .border(1.dp, Color(0xFFC88A4B), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFFC88A4B),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                val titleText = when {
                    isResettingMode -> "Reset Security Question"
                    mode == PinModalMode.SETUP_PIN && step == 1 -> "Set 4-Digit PIN"
                    mode == PinModalMode.SETUP_PIN && step == 2 -> "Re-Confirm 4-Digit PIN"
                    mode == PinModalMode.SETUP_PIN && step == 3 -> "Security Question"
                    mode == PinModalMode.UNLOCK -> "Unlock $folderName"
                    else -> "Folder PIN Security"
                }

                Text(
                    text = titleText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D2B28)
                )

                Spacer(modifier = Modifier.height(6.dp))

                val subtitleText = when {
                    isResettingMode -> "Answer your security question to set a new PIN"
                    mode == PinModalMode.SETUP_PIN && step == 1 -> "Choose a 4-digit PIN for $folderName"
                    mode == PinModalMode.SETUP_PIN && step == 2 -> "Re-enter your 4-digit PIN to confirm"
                    mode == PinModalMode.SETUP_PIN && step == 3 -> "Set a security question for PIN recovery"
                    else -> "Enter your 4-digit PIN to access folder"
                }

                Text(
                    text = subtitleText,
                    fontSize = 13.sp,
                    color = Color(0xFF5C5850),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Reset Mode (Answering Security Question)
                if (isResettingMode) {
                    val displayQuestion = existingQuestion ?: PREDEFINED_SECURITY_QUESTIONS.first()
                    Text(
                        text = displayQuestion,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2D2B28),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = securityAnswerInput,
                        onValueChange = { securityAnswerInput = it; errorMessage = null },
                        label = { Text("Your Answer", color = Color(0xFF5C5850)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFC88A4B),
                            unfocusedBorderColor = Color(0xFFE2D7C5),
                            focusedTextColor = Color(0xFF2D2B28),
                            unfocusedTextColor = Color(0xFF2D2B28)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = Color(0xFFC85A32),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = Color(0xFF5C5850))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                val inputHash = hashSecurityAnswer(securityAnswerInput)
                                if (existingAnswerHash != null && inputHash == existingAnswerHash) {
                                    isResettingMode = false
                                    step = 1
                                    firstPinAttempt = ""
                                    enteredPin = ""
                                    errorMessage = null
                                } else {
                                    errorMessage = "Incorrect answer. Please try again."
                                }
                            }
                        ) {
                            Text("Verify Answer", color = Color(0xFFC88A4B), fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (mode == PinModalMode.SETUP_PIN && step == 3) {
                    // Security Question Setup Form
                    ExposedDropdownMenuBox(
                        expanded = questionExpanded,
                        onExpandedChange = { questionExpanded = !questionExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedQuestion,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Security Question", color = Color(0xFF5C5850)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = questionExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFC88A4B),
                                unfocusedBorderColor = Color(0xFFE2D7C5),
                                focusedTextColor = Color(0xFF2D2B28),
                                unfocusedTextColor = Color(0xFF2D2B28)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = questionExpanded,
                            onDismissRequest = { questionExpanded = false }
                        ) {
                            PREDEFINED_SECURITY_QUESTIONS.forEach { q ->
                                DropdownMenuItem(
                                    text = { Text(q, fontSize = 13.sp) },
                                    onClick = {
                                        selectedQuestion = q
                                        questionExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = securityAnswerInput,
                        onValueChange = { securityAnswerInput = it; errorMessage = null },
                        label = { Text("Security Answer", color = Color(0xFF5C5850)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFC88A4B),
                            unfocusedBorderColor = Color(0xFFE2D7C5),
                            focusedTextColor = Color(0xFF2D2B28),
                            unfocusedTextColor = Color(0xFF2D2B28)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = Color(0xFFC85A32),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = Color(0xFF5C5850))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                if (securityAnswerInput.trim().isBlank()) {
                                    errorMessage = "Please enter a security answer"
                                } else {
                                    val answerHash = hashSecurityAnswer(securityAnswerInput)
                                    onSuccessPinSet(firstPinAttempt, selectedQuestion, answerHash)
                                }
                            }
                        ) {
                            Text("Save PIN & Question", color = Color(0xFFC88A4B), fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // PIN Dot Indicators
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        repeat(4) { idx ->
                            val isFilled = idx < enteredPin.length
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 10.dp)
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(if (isFilled) Color(0xFFC88A4B) else Color(0xFFE2D7C5))
                                    .border(1.dp, Color(0xFFC88A4B), CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = Color(0xFFC85A32),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // 3x4 Numeric Keypad
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val keyRows = listOf(
                            listOf("1", "2", "3"),
                            listOf("4", "5", "6"),
                            listOf("7", "8", "9"),
                            listOf("Clear", "0", "Delete")
                        )

                        for (row in keyRows) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (key in row) {
                                    KeypadButton(
                                        label = key,
                                        onClick = {
                                            errorMessage = null
                                            when (key) {
                                                "Clear" -> enteredPin = ""
                                                "Delete" -> if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                                                else -> {
                                                    if (enteredPin.length < 4) {
                                                        enteredPin += key
                                                        if (enteredPin.length == 4) {
                                                            if (mode == PinModalMode.SETUP_PIN) {
                                                                if (step == 1) {
                                                                    firstPinAttempt = enteredPin
                                                                    enteredPin = ""
                                                                    step = 2
                                                                } else if (step == 2) {
                                                                    if (enteredPin == firstPinAttempt) {
                                                                        step = 3
                                                                        enteredPin = ""
                                                                    } else {
                                                                        triggerErrorShake("PINs do not match. Try setting PIN again.")
                                                                        step = 1
                                                                        firstPinAttempt = ""
                                                                    }
                                                                }
                                                            } else if (mode == PinModalMode.UNLOCK) {
                                                                if (existingPin != null && enteredPin == existingPin) {
                                                                    onSuccessUnlocked()
                                                                } else {
                                                                    triggerErrorShake("Incorrect PIN. Please try again.")
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (mode == PinModalMode.UNLOCK && existingQuestion != null) {
                        TextButton(
                            onClick = {
                                isResettingMode = true
                                errorMessage = null
                            }
                        ) {
                            Text("Forgot PIN?", color = Color(0xFFC88A4B), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xFF5C5850))
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(Color(0xFFF5F0E6))
            .border(1.dp, Color(0xFFE2D7C5), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (label == "Delete") {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = Color(0xFF2D2B28),
                modifier = Modifier.size(20.dp)
            )
        } else {
            Text(
                text = label,
                fontSize = if (label == "Clear") 12.sp else 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2D2B28)
            )
        }
    }
}
