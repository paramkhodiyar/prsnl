package com.prsnl.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun TypewriterText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 22.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    color: Color = Color(0xFF2D2B28),
    cursorColor: Color = Color(0xFFC88A4B),
    typingDelayMs: Long = 40L
) {
    var displayedText by remember(text) { mutableStateOf("") }
    var isTypingComplete by remember(text) { mutableStateOf(false) }

    val cursorAlpha = remember { Animatable(1f) }

    LaunchedEffect(text) {
        displayedText = ""
        isTypingComplete = false
        for (i in 1..text.length) {
            displayedText = text.substring(0, i)
            delay(typingDelayMs)
        }
        isTypingComplete = true
    }

    LaunchedEffect(isTypingComplete) {
        if (!isTypingComplete) {
            cursorAlpha.snapTo(1f)
        } else {
            cursorAlpha.animateTo(
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 500, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = displayedText,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = color
        )
        Text(
            text = "|",
            fontSize = fontSize,
            fontWeight = FontWeight.Light,
            color = cursorColor,
            modifier = Modifier.alpha(cursorAlpha.value)
        )
    }
}
