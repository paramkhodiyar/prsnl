package com.prsnl.ui.editor

import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prsnl.document.model.Shape
import com.prsnl.drawing.view.CanvasToolMode
import kotlin.math.roundToInt

@Composable
fun FloatingWritingToolbar(
    toolMode: CanvasToolMode,
    onToolModeChange: (CanvasToolMode) -> Unit,
    selectedColor: Int,
    onColorSelect: (Int) -> Unit,
    selectedWidth: Float,
    onWidthChange: (Float) -> Unit,
    eraserRadius: Float,
    onEraserRadiusChange: (Float) -> Unit,
    selectedShapeType: Shape.Type,
    onShapeTypeSelect: (Shape.Type) -> Unit,
    isFingerDrawingEnabled: Boolean,
    onFingerDrawingToggle: () -> Unit,
    isPressureSensitivityEnabled: Boolean,
    onPressureSensitivityToggle: () -> Unit,
    hasSelection: Boolean,
    onDeleteSelection: () -> Unit,
    onOpenColorWheel: () -> Unit,
    onOpenSettings: () -> Unit,
    onInsertImage: () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isExpanded by remember { mutableStateOf(true) }
    var activeSubMenu by remember { mutableStateOf<String?>(null) }
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var lastTappedTool by remember { mutableStateOf("") }

    val stationeryInkColors = listOf(
        AndroidColor.parseColor("#2D2B28"),
        AndroidColor.parseColor("#3A3F47"),
        AndroidColor.parseColor("#8B5E3C"),
        AndroidColor.parseColor("#C88A4B"),
        AndroidColor.parseColor("#4A7C59")
    )

    fun handleToolTap(toolName: String, onFirstTap: () -> Unit) {
        val now = System.currentTimeMillis()
        if (lastTappedTool == toolName && (now - lastTapTime) < 2500L) {
            activeSubMenu = if (activeSubMenu == toolName) null else toolName
        } else {
            onFirstTap()
            activeSubMenu = null
        }
        lastTappedTool = toolName
        lastTapTime = now
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFF5F0E6))
            .border(1.5.dp, Color(0xFFC88A4B), RoundedCornerShape(24.dp))
            .padding(8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(onClick = { isExpanded = !isExpanded }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.Create else Icons.Default.Menu,
                        contentDescription = "Toggle Toolbar",
                        tint = Color(0xFFC88A4B)
                    )
                }

                AnimatedVisibility(visible = isExpanded, enter = fadeIn(), exit = fadeOut()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val chipColors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFC88A4B),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFFEBE4D8),
                            labelColor = Color(0xFF2D2B28)
                        )

                        // Pen Tool
                        FilterChip(
                            selected = toolMode == CanvasToolMode.PEN,
                            onClick = { handleToolTap("PEN") { onToolModeChange(CanvasToolMode.PEN) } },
                            label = { Text("Pen", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.Create, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = chipColors
                        )

                        // Pencil Tool
                        FilterChip(
                            selected = toolMode == CanvasToolMode.PENCIL,
                            onClick = { handleToolTap("PENCIL") { onToolModeChange(CanvasToolMode.PENCIL) } },
                            label = { Text("Pencil", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = chipColors
                        )

                        // Lasso Tool
                        FilterChip(
                            selected = toolMode == CanvasToolMode.LASSO,
                            onClick = { handleToolTap("LASSO") { onToolModeChange(CanvasToolMode.LASSO) } },
                            label = { Text("Lasso", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = chipColors
                        )

                        // Typer Text Tool
                        FilterChip(
                            selected = toolMode == CanvasToolMode.TEXT,
                            onClick = { handleToolTap("TEXT") { onToolModeChange(CanvasToolMode.TEXT) } },
                            label = { Text("Typer", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.Create, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = chipColors
                        )

                        // Shapes Picker Tool Button
                        FilterChip(
                            selected = toolMode == CanvasToolMode.SHAPE_PICKER,
                            onClick = {
                                handleToolTap("SHAPE") { onToolModeChange(CanvasToolMode.SHAPE_PICKER) }
                            },
                            label = { Text("Shapes", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = chipColors
                        )

                        // Image Attachment Button
                        FilterChip(
                            selected = false,
                            onClick = onInsertImage,
                            label = { Text("+ Image", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = chipColors
                        )

                        // Eraser Tool
                        FilterChip(
                            selected = toolMode == CanvasToolMode.STROKE_ERASER || toolMode == CanvasToolMode.PIXEL_ERASER,
                            onClick = { handleToolTap("ERASER") { onToolModeChange(CanvasToolMode.STROKE_ERASER) } },
                            label = { Text(if (toolMode == CanvasToolMode.PIXEL_ERASER) "Pixel" else "Eraser", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = chipColors
                        )

                        if (hasSelection && (toolMode == CanvasToolMode.SELECT || toolMode == CanvasToolMode.LASSO)) {
                            IconButton(onClick = onDeleteSelection, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Clear, contentDescription = "Delete Selection", tint = Color(0xFFDC2626))
                            }
                        }

                        IconButton(onClick = onOpenSettings, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Settings, contentDescription = "Notebook Settings", tint = Color(0xFFC88A4B))
                        }
                    }
                }
            }

            // Sub-Menu Popover & Vector Shapes Palette
            AnimatedVisibility(visible = isExpanded && (activeSubMenu != null || toolMode == CanvasToolMode.SHAPE_PICKER), enter = fadeIn(), exit = fadeOut()) {
                Column(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFEBE4D8))
                        .border(1.dp, Color(0xFFE2D7C5), RoundedCornerShape(18.dp))
                        .padding(12.dp)
                ) {
                    if (toolMode == CanvasToolMode.SHAPE_PICKER || activeSubMenu == "SHAPE") {
                        Text("Vector Shapes & Graphs", fontSize = 11.sp, color = Color(0xFFC88A4B), fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                        VectorShapesPalette(
                            selectedShapeType = selectedShapeType,
                            onShapeTypeSelect = { shapeType ->
                                onShapeTypeSelect(shapeType)
                                onToolModeChange(CanvasToolMode.SHAPE_PICKER)
                            }
                        )
                    } else {
                        when (activeSubMenu) {
                            "PEN", "PENCIL", "HIGHLIGHTER" -> {
                                Text("Thickness & Pressure Sensitivity", fontSize = 11.sp, color = Color(0xFF5C5850), fontWeight = FontWeight.Bold)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Text("${selectedWidth.toInt()} px", fontSize = 11.sp, color = Color(0xFFC88A4B), fontWeight = FontWeight.Bold)
                                    Slider(
                                        value = selectedWidth,
                                        onValueChange = onWidthChange,
                                        valueRange = 2f..24f,
                                        colors = SliderDefaults.colors(thumbColor = Color(0xFFC88A4B), activeTrackColor = Color(0xFFC88A4B)),
                                        modifier = Modifier.width(140.dp)
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Text("Pressure Sensitivity", fontSize = 11.sp, color = Color(0xFF2D2B28))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Switch(
                                        checked = isPressureSensitivityEnabled,
                                        onCheckedChange = { onPressureSensitivityToggle() },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFFC88A4B),
                                            checkedTrackColor = Color(0xFFE2D7C5)
                                        )
                                    )
                                }
                            }

                            "ERASER" -> {
                                Text("Eraser Mode & Size", fontSize = 11.sp, color = Color(0xFF5C5850), fontWeight = FontWeight.Bold)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(vertical = 6.dp)
                                ) {
                                    FilterChip(
                                        selected = toolMode == CanvasToolMode.STROKE_ERASER,
                                        onClick = { onToolModeChange(CanvasToolMode.STROKE_ERASER) },
                                        label = { Text("Stroke", fontSize = 11.sp) }
                                    )
                                    FilterChip(
                                        selected = toolMode == CanvasToolMode.PIXEL_ERASER,
                                        onClick = { onToolModeChange(CanvasToolMode.PIXEL_ERASER) },
                                        label = { Text("Pixel", fontSize = 11.sp) }
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Size ${eraserRadius.toInt()} px", fontSize = 11.sp, color = Color(0xFFC88A4B))
                                    Slider(
                                        value = eraserRadius,
                                        onValueChange = onEraserRadiusChange,
                                        valueRange = 10f..80f,
                                        colors = SliderDefaults.colors(thumbColor = Color(0xFFC88A4B), activeTrackColor = Color(0xFFC88A4B)),
                                        modifier = Modifier.width(140.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Color Palette Dots
            AnimatedVisibility(visible = isExpanded && activeSubMenu == null && toolMode != CanvasToolMode.SHAPE_PICKER, enter = fadeIn(), exit = fadeOut()) {
                Row(
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    stationeryInkColors.forEach { colorInt ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color(colorInt))
                                .border(
                                    width = if (selectedColor == colorInt) 2.5.dp else 1.dp,
                                    color = if (selectedColor == colorInt) Color(0xFFC88A4B) else Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable { onColorSelect(colorInt) }
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF5F0E6))
                            .border(1.5.dp, Color(0xFFC88A4B), CircleShape)
                            .clickable { onOpenColorWheel() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+Hex", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC88A4B))
                    }
                }
            }
        }
    }
}

@Composable
fun VectorShapesPalette(
    selectedShapeType: Shape.Type,
    onShapeTypeSelect: (Shape.Type) -> Unit
) {
    val row1 = listOf(
        Shape.Type.LINE,
        Shape.Type.ARROW,
        Shape.Type.ARROW_DOUBLE,
        Shape.Type.CORNER,
        Shape.Type.CORNER_ARROW_SINGLE,
        Shape.Type.CORNER_ARROW_DOUBLE,
        Shape.Type.RECTANGLE
    )

    val row2 = listOf(
        Shape.Type.ELLIPSE,
        Shape.Type.PARALLELOGRAM,
        Shape.Type.TRIANGLE,
        Shape.Type.DIAMOND,
        Shape.Type.AXIS_2D,
        Shape.Type.QUADRANT_4,
        Shape.Type.AXIS_3D
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row1.forEach { type ->
                ShapeIconItem(
                    shapeType = type,
                    isSelected = selectedShapeType == type,
                    onClick = { onShapeTypeSelect(type) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row2.forEach { type ->
                ShapeIconItem(
                    shapeType = type,
                    isSelected = selectedShapeType == type,
                    onClick = { onShapeTypeSelect(type) }
                )
            }
        }
    }
}

@Composable
fun ShapeIconItem(
    shapeType: Shape.Type,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val iconColor = if (isSelected) Color(0xFFC88A4B) else Color(0xFF5C5850)
    val bgColor = if (isSelected) Color(0xFFFAF8F5) else Color(0xFFF5F0E6)
    val borderColor = if (isSelected) Color(0xFFC88A4B) else Color(0xFFE2D7C5)

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            val w = size.width
            val h = size.height
            val strokeStyle = Stroke(width = 2.5f)

            when (shapeType) {
                Shape.Type.LINE -> {
                    drawLine(iconColor, start = Offset(2f, h - 2f), end = Offset(w - 2f, 2f), strokeWidth = 2.5f)
                }
                Shape.Type.ARROW -> {
                    drawLine(iconColor, start = Offset(2f, 2f), end = Offset(w - 2f, h - 2f), strokeWidth = 2.5f)
                    val path = Path().apply {
                        moveTo(w - 2f, h - 2f)
                        lineTo(w - 10f, h - 2f)
                        moveTo(w - 2f, h - 2f)
                        lineTo(w - 2f, h - 10f)
                    }
                    drawPath(path, iconColor, style = strokeStyle)
                }
                Shape.Type.ARROW_DOUBLE -> {
                    drawLine(iconColor, start = Offset(4f, h - 4f), end = Offset(w - 4f, 4f), strokeWidth = 2.5f)
                    val p1 = Path().apply {
                        moveTo(4f, h - 4f)
                        lineTo(10f, h - 4f)
                        moveTo(4f, h - 4f)
                        lineTo(4f, h - 10f)
                    }
                    val p2 = Path().apply {
                        moveTo(w - 4f, 4f)
                        lineTo(w - 10f, 4f)
                        moveTo(w - 4f, 4f)
                        lineTo(w - 4f, 10f)
                    }
                    drawPath(p1, iconColor, style = strokeStyle)
                    drawPath(p2, iconColor, style = strokeStyle)
                }
                Shape.Type.CORNER -> {
                    val p = Path().apply {
                        moveTo(2f, h - 2f)
                        lineTo(w - 2f, h - 2f)
                        lineTo(w - 2f, 2f)
                    }
                    drawPath(p, iconColor, style = strokeStyle)
                }
                Shape.Type.CORNER_ARROW_SINGLE -> {
                    val p = Path().apply {
                        moveTo(2f, 2f)
                        lineTo(w - 2f, 2f)
                        lineTo(w - 2f, h - 2f)
                        moveTo(w - 2f, h - 2f)
                        lineTo(w - 8f, h - 6f)
                        moveTo(w - 2f, h - 2f)
                        lineTo(w - 6f, h - 8f)
                    }
                    drawPath(p, iconColor, style = strokeStyle)
                }
                Shape.Type.CORNER_ARROW_DOUBLE -> {
                    val p = Path().apply {
                        moveTo(2f, 2f)
                        lineTo(w - 2f, 2f)
                        lineTo(w - 2f, h - 2f)
                        moveTo(2f, 2f)
                        lineTo(8f, 6f)
                        moveTo(w - 2f, h - 2f)
                        lineTo(w - 8f, h - 6f)
                    }
                    drawPath(p, iconColor, style = strokeStyle)
                }
                Shape.Type.RECTANGLE -> {
                    drawRect(iconColor, topLeft = Offset(2f, 2f), size = Size(w - 4f, h - 4f), style = strokeStyle)
                }
                Shape.Type.ELLIPSE -> {
                    drawOval(iconColor, topLeft = Offset(2f, 4f), size = Size(w - 4f, h - 8f), style = strokeStyle)
                }
                Shape.Type.PARALLELOGRAM -> {
                    val p = Path().apply {
                        moveTo(6f, 2f)
                        lineTo(w - 2f, 2f)
                        lineTo(w - 6f, h - 2f)
                        lineTo(2f, h - 2f)
                        close()
                    }
                    drawPath(p, iconColor, style = strokeStyle)
                }
                Shape.Type.TRIANGLE -> {
                    val p = Path().apply {
                        moveTo(w / 2f, 2f)
                        lineTo(w - 2f, h - 2f)
                        lineTo(2f, h - 2f)
                        close()
                    }
                    drawPath(p, iconColor, style = strokeStyle)
                }
                Shape.Type.DIAMOND -> {
                    val p = Path().apply {
                        moveTo(w / 2f, 2f)
                        lineTo(w - 2f, h / 2f)
                        lineTo(w / 2f, h - 2f)
                        lineTo(2f, h / 2f)
                        close()
                    }
                    drawPath(p, iconColor, style = strokeStyle)
                }
                Shape.Type.AXIS_2D -> {
                    drawLine(iconColor, start = Offset(4f, 2f), end = Offset(4f, h - 4f), strokeWidth = 2.5f)
                    drawLine(iconColor, start = Offset(4f, h - 4f), end = Offset(w - 2f, h - 4f), strokeWidth = 2.5f)
                }
                Shape.Type.QUADRANT_4 -> {
                    drawLine(iconColor, start = Offset(2f, h / 2f), end = Offset(w - 2f, h / 2f), strokeWidth = 2.5f)
                    drawLine(iconColor, start = Offset(w / 2f, 2f), end = Offset(w / 2f, h - 2f), strokeWidth = 2.5f)
                }
                Shape.Type.AXIS_3D -> {
                    val cx = w / 2f
                    val cy = h / 2f
                    drawLine(iconColor, start = Offset(cx, cy), end = Offset(cx, 2f), strokeWidth = 2f)
                    drawLine(iconColor, start = Offset(cx, cy), end = Offset(w - 2f, cy), strokeWidth = 2f)
                    drawLine(iconColor, start = Offset(cx, cy), end = Offset(2f, h - 2f), strokeWidth = 2f)
                }
                else -> {}
            }
        }
    }
}
