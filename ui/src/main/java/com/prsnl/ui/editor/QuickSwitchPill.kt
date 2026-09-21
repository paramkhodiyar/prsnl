package com.prsnl.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.prsnl.drawing.view.CanvasToolMode
import kotlin.math.roundToInt

@Composable
fun QuickSwitchPill(
    toolMode: CanvasToolMode,
    onToolModeChange: (CanvasToolMode) -> Unit,
    selectedColor: Int,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var previousDrawingTool by remember { mutableStateOf(CanvasToolMode.PEN) }

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
            .shadow(10.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFF5F0E6))
            .border(1.5.dp, Color(0xFFC88A4B), RoundedCornerShape(24.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isEraserActive = toolMode == CanvasToolMode.STROKE_ERASER || toolMode == CanvasToolMode.PIXEL_ERASER
            val activeColor = Color(selectedColor)

            // 1. Current Active Tool Indicator
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEBE4D8))
                    .border(1.5.dp, Color(0xFFC88A4B), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                ToolIcon(tool = toolMode, tintColor = activeColor, size = 18.dp)
            }

            // 2. Quick Pen Toggle
            val isPenSelected = toolMode == CanvasToolMode.PEN
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isPenSelected) Color(0xFFC88A4B) else Color(0xFFEBE4D8))
                    .clickable {
                        previousDrawingTool = CanvasToolMode.PEN
                        onToolModeChange(CanvasToolMode.PEN)
                    },
                contentAlignment = Alignment.Center
            ) {
                ToolIcon(
                    tool = CanvasToolMode.PEN,
                    tintColor = if (isPenSelected) Color.White else activeColor,
                    size = 18.dp
                )
            }

            // 3. Quick Eraser Toggle (Toggles between Eraser and previous drawing tool)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isEraserActive) Color(0xFFC88A4B) else Color(0xFFEBE4D8))
                    .clickable {
                        if (isEraserActive) {
                            onToolModeChange(previousDrawingTool)
                        } else {
                            if (toolMode != CanvasToolMode.STROKE_ERASER && toolMode != CanvasToolMode.PIXEL_ERASER) {
                                previousDrawingTool = toolMode
                            }
                            onToolModeChange(CanvasToolMode.STROKE_ERASER)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                ToolIcon(
                    tool = CanvasToolMode.STROKE_ERASER,
                    tintColor = activeColor,
                    size = 18.dp
                )
            }

            // 4. Quick Undo Button
            IconButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Quick Undo",
                    tint = if (canUndo) Color(0xFFC88A4B) else Color.Gray.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }

            // 5. Quick Redo Button
            IconButton(
                onClick = onRedo,
                enabled = canRedo,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Quick Redo",
                    tint = if (canRedo) Color(0xFFC88A4B) else Color.Gray.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
