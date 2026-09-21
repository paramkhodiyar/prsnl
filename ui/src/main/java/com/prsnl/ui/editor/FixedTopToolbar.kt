package com.prsnl.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prsnl.document.model.Shape
import com.prsnl.drawing.view.CanvasToolMode
import com.prsnl.ui.common.BrushPalettes

@Composable
fun FixedTopToolbar(
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
    isPressureSensitivityEnabled: Boolean,
    onPressureSensitivityToggle: () -> Unit,
    hasSelection: Boolean,
    onDeleteSelection: () -> Unit,
    onOpenColorWheel: () -> Unit,
    onOpenSettings: () -> Unit,
    onInsertImage: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeSubMenu by remember { mutableStateOf<String?>(null) }
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var lastTappedTool by remember { mutableStateOf("") }

    val activePalette = BrushPalettes.getPaletteForTool(toolMode)

    fun handleToolTap(toolName: String, onFirstTap: () -> Unit) {
        val now = System.currentTimeMillis()
        if (lastTappedTool == toolName && (now - lastTapTime) < 3000L) {
            activeSubMenu = if (activeSubMenu == toolName) null else toolName
        } else {
            onFirstTap()
            activeSubMenu = toolName // Open sub-options like thickness or palettes smoothly
        }
        lastTappedTool = toolName
        lastTapTime = now
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = Color(0xFFF5F0E6),
        tonalElevation = 6.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            // Main Top Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val chipColors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFC88A4B),
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFFEBE4D8),
                    labelColor = Color(0xFF2D2B28)
                )

                val activeColorCompose = Color(selectedColor)

                // 1. Pen Tool
                FilterChip(
                    selected = toolMode == CanvasToolMode.PEN,
                    onClick = { handleToolTap("PEN") { onToolModeChange(CanvasToolMode.PEN) } },
                    label = { Text("Pen", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    leadingIcon = { ToolIcon(CanvasToolMode.PEN, activeColorCompose, size = 18.dp) },
                    colors = chipColors
                )

                // 2. Pencil Tool
                FilterChip(
                    selected = toolMode == CanvasToolMode.PENCIL,
                    onClick = { handleToolTap("PENCIL") { onToolModeChange(CanvasToolMode.PENCIL) } },
                    label = { Text("Pencil", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    leadingIcon = { ToolIcon(CanvasToolMode.PENCIL, activeColorCompose, size = 18.dp) },
                    colors = chipColors
                )

                // 3. Highlighter Tool
                FilterChip(
                    selected = toolMode == CanvasToolMode.HIGHLIGHTER,
                    onClick = {
                        handleToolTap("HIGHLIGHTER") {
                            onToolModeChange(CanvasToolMode.HIGHLIGHTER)
                            if (!BrushPalettes.isColorInPalette(selectedColor, CanvasToolMode.HIGHLIGHTER)) {
                                onColorSelect(BrushPalettes.getDefaultColorForTool(CanvasToolMode.HIGHLIGHTER))
                            }
                        }
                    },
                    label = { Text("Highlighter", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    leadingIcon = { ToolIcon(CanvasToolMode.HIGHLIGHTER, activeColorCompose, size = 18.dp) },
                    colors = chipColors
                )

                // 4. Eraser Tool
                FilterChip(
                    selected = toolMode == CanvasToolMode.STROKE_ERASER || toolMode == CanvasToolMode.PIXEL_ERASER,
                    onClick = { handleToolTap("ERASER") { onToolModeChange(CanvasToolMode.STROKE_ERASER) } },
                    label = { Text(if (toolMode == CanvasToolMode.PIXEL_ERASER) "Pixel" else "Eraser", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    leadingIcon = { ToolIcon(CanvasToolMode.STROKE_ERASER, activeColorCompose, size = 18.dp) },
                    colors = chipColors
                )

                // 5. Lasso Tool
                FilterChip(
                    selected = toolMode == CanvasToolMode.LASSO,
                    onClick = {
                        activeSubMenu = null
                        onToolModeChange(CanvasToolMode.LASSO)
                    },
                    label = { Text("Lasso", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    leadingIcon = { ToolIcon(CanvasToolMode.LASSO, activeColorCompose, size = 18.dp) },
                    colors = chipColors
                )

                // 6. Typer Text Tool
                FilterChip(
                    selected = toolMode == CanvasToolMode.TEXT,
                    onClick = {
                        activeSubMenu = null
                        onToolModeChange(CanvasToolMode.TEXT)
                    },
                    label = { Text("Typer", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    leadingIcon = { ToolIcon(CanvasToolMode.TEXT, activeColorCompose, size = 18.dp) },
                    colors = chipColors
                )

                // 7. Pure Geometry Shapes Tool
                FilterChip(
                    selected = toolMode == CanvasToolMode.SHAPE_PICKER && activeSubMenu != "GRAPH",
                    onClick = {
                        handleToolTap("SHAPE") { onToolModeChange(CanvasToolMode.SHAPE_PICKER) }
                    },
                    label = { Text("Shapes", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    leadingIcon = { ToolIcon(CanvasToolMode.SHAPE_PICKER, activeColorCompose, size = 18.dp) },
                    colors = chipColors
                )

                // 8. Dedicated STEM Graph Tool
                FilterChip(
                    selected = activeSubMenu == "GRAPH" || isGraphShape(selectedShapeType),
                    onClick = {
                        handleToolTap("GRAPH") {
                            onToolModeChange(CanvasToolMode.SHAPE_PICKER)
                            if (!isGraphShape(selectedShapeType)) {
                                onShapeTypeSelect(Shape.Type.AXIS_2D)
                            }
                        }
                    },
                    label = { Text("Graph", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    leadingIcon = { GraphToolIcon(size = 18.dp) },
                    colors = chipColors
                )

                // Image Attachment Button
                FilterChip(
                    selected = false,
                    onClick = onInsertImage,
                    label = { Text("+ Image", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = chipColors
                )

                if (hasSelection) {
                    IconButton(onClick = onDeleteSelection, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Clear, contentDescription = "Delete Selection", tint = Color(0xFFDC2626))
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                TextButton(onClick = onUndo, enabled = canUndo) {
                    Text("Undo", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (canUndo) Color(0xFFC88A4B) else Color.Gray)
                }

                TextButton(onClick = onRedo, enabled = canRedo) {
                    Text("Redo", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (canRedo) Color(0xFFC88A4B) else Color.Gray)
                }

                IconButton(onClick = onOpenSettings, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color(0xFFC88A4B))
                }
            }

            // Sub-Menu: Dynamic Palette Dots, Thickness Slider with Live Preview, and Graph/Shape Pickers
            AnimatedVisibility(
                visible = activeSubMenu != null || toolMode == CanvasToolMode.SHAPE_PICKER,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFEBE4D8))
                        .border(1.dp, Color(0xFFE2D7C5), RoundedCornerShape(14.dp))
                        .padding(8.dp)
                ) {
                    when (activeSubMenu) {
                        "GRAPH" -> {
                            Text(
                                "STEM Coordinate Grids & Templates",
                                fontSize = 11.sp,
                                color = Color(0xFFC88A4B),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            StemGraphPalette(
                                selectedShapeType = selectedShapeType,
                                onShapeTypeSelect = { shapeType ->
                                    onShapeTypeSelect(shapeType)
                                    onToolModeChange(CanvasToolMode.SHAPE_PICKER)
                                }
                            )
                        }

                        "SHAPE" -> {
                            Text(
                                "Geometric Shapes",
                                fontSize = 11.sp,
                                color = Color(0xFFC88A4B),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            GeometricShapesPalette(
                                selectedShapeType = selectedShapeType,
                                onShapeTypeSelect = { shapeType ->
                                    onShapeTypeSelect(shapeType)
                                    onToolModeChange(CanvasToolMode.SHAPE_PICKER)
                                }
                            )
                        }

                        "PEN", "PENCIL", "HIGHLIGHTER" -> {
                            val (minW, maxW, defaultW, finePreset, medPreset, boldPreset) = when (toolMode) {
                                CanvasToolMode.HIGHLIGHTER -> Tuple6(8f, 40f, 18f, 12f, 18f, 28f)
                                CanvasToolMode.PENCIL -> Tuple6(1f, 10f, 2f, 1.5f, 2.5f, 4.5f)
                                else -> Tuple6(1f, 12f, 3f, 1.5f, 3.0f, 5.0f) // Pen
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Thickness Presets & Slider
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("Thickness:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5C5850))

                                        // Fine Preset Chip
                                        PresetChip(label = "Fine", width = finePreset, isSelected = Math.abs(selectedWidth - finePreset) < 0.2f) {
                                            onWidthChange(finePreset)
                                        }
                                        // Medium Preset Chip
                                        PresetChip(label = "Medium", width = medPreset, isSelected = Math.abs(selectedWidth - medPreset) < 0.2f) {
                                            onWidthChange(medPreset)
                                        }
                                        // Bold Preset Chip
                                        PresetChip(label = "Bold", width = boldPreset, isSelected = Math.abs(selectedWidth - boldPreset) < 0.2f) {
                                            onWidthChange(boldPreset)
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Text(
                                            "${String.format("%.1f", selectedWidth)} px",
                                            fontSize = 11.sp,
                                            color = Color(0xFFC88A4B),
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.width(42.dp)
                                        )

                                        Slider(
                                            value = selectedWidth.coerceIn(minW, maxW),
                                            onValueChange = onWidthChange,
                                            valueRange = minW..maxW,
                                            colors = SliderDefaults.colors(
                                                thumbColor = Color(0xFFC88A4B),
                                                activeTrackColor = Color(0xFFC88A4B)
                                            ),
                                            modifier = Modifier.width(150.dp)
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // Live Rendered Thickness Preview Swatch
                                        Box(
                                            modifier = Modifier
                                                .size(width = 44.dp, height = 28.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color.White)
                                                .border(1.dp, Color(0xFFD6CEBF), RoundedCornerShape(6.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Canvas(modifier = Modifier.size(36.dp, 20.dp)) {
                                                drawLine(
                                                    color = Color(selectedColor),
                                                    start = Offset(4f, size.height / 2f),
                                                    end = Offset(size.width - 4f, size.height / 2f),
                                                    strokeWidth = selectedWidth,
                                                    cap = StrokeCap.Round
                                                )
                                            }
                                        }
                                    }
                                }

                                // Pressure Sensitivity Switch
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Pressure", fontSize = 10.sp, color = Color(0xFF2D2B28))
                                    Spacer(modifier = Modifier.width(4.dp))
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
                        }

                        "ERASER" -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                        modifier = Modifier.width(130.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Curated Color Palette Dots Row
                    if (toolMode == CanvasToolMode.PEN || toolMode == CanvasToolMode.PENCIL || toolMode == CanvasToolMode.HIGHLIGHTER) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (toolMode == CanvasToolMode.HIGHLIGHTER) "Neon & Pastel:" else "Ink Colors:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF5C5850)
                            )

                            activePalette.forEach { colorInt ->
                                val isSelected = selectedColor == colorInt
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(colorInt))
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) Color(0xFFC88A4B) else Color.Gray.copy(alpha = 0.5f),
                                            shape = CircleShape
                                        )
                                        .clickable { onColorSelect(colorInt) }
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF5F0E6))
                                    .border(1.5.dp, Color(0xFFC88A4B), CircleShape)
                                    .clickable { onOpenColorWheel() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+Hex", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC88A4B))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetChip(label: String, width: Float, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) Color(0xFFC88A4B) else Color(0xFFF5F0E6))
            .border(1.dp, if (isSelected) Color(0xFFC88A4B) else Color(0xFFD6CEBF), RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = "$label (${width.toInt()}px)",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else Color(0xFF2D2B28)
        )
    }
}

private fun isGraphShape(type: Shape.Type): Boolean {
    return when (type) {
        Shape.Type.AXIS_2D,
        Shape.Type.QUADRANT_4,
        Shape.Type.AXIS_3D,
        Shape.Type.NUMBER_LINE,
        Shape.Type.POLAR_GRID,
        Shape.Type.GRAPH_PAPER_BG,
        Shape.Type.BAR_CHART_TEMPLATE,
        Shape.Type.PIE_CHART_TEMPLATE -> true
        else -> false
    }
}

@Composable
fun StemGraphPalette(
    selectedShapeType: Shape.Type,
    onShapeTypeSelect: (Shape.Type) -> Unit
) {
    val graphTypes = listOf(
        Pair(Shape.Type.AXIS_2D, "2D Axis"),
        Pair(Shape.Type.QUADRANT_4, "4-Quadrant Grid"),
        Pair(Shape.Type.AXIS_3D, "3D Axis"),
        Pair(Shape.Type.NUMBER_LINE, "Number Line"),
        Pair(Shape.Type.POLAR_GRID, "Polar Grid"),
        Pair(Shape.Type.GRAPH_PAPER_BG, "Grid Paper"),
        Pair(Shape.Type.BAR_CHART_TEMPLATE, "Bar Chart"),
        Pair(Shape.Type.PIE_CHART_TEMPLATE, "Pie Chart")
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        graphTypes.forEach { (type, label) ->
            val isSelected = selectedShapeType == type
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) Color(0xFFFAF8F5) else Color(0xFFF5F0E6))
                    .border(1.5.dp, if (isSelected) Color(0xFFC88A4B) else Color(0xFFE2D7C5), RoundedCornerShape(8.dp))
                    .clickable { onShapeTypeSelect(type) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                ToolbarShapeIconItem(shapeType = type, isSelected = isSelected, onClick = { onShapeTypeSelect(type) })
                Spacer(modifier = Modifier.height(4.dp))
                Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color(0xFFC88A4B) else Color(0xFF5C5850))
            }
        }
    }
}

@Composable
fun GeometricShapesPalette(
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
        Shape.Type.ROUNDED_RECTANGLE,
        Shape.Type.ELLIPSE,
        Shape.Type.PARALLELOGRAM,
        Shape.Type.TRIANGLE,
        Shape.Type.DIAMOND
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            row1.forEach { type ->
                ToolbarShapeIconItem(shapeType = type, isSelected = selectedShapeType == type, onClick = { onShapeTypeSelect(type) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            row2.forEach { type ->
                ToolbarShapeIconItem(shapeType = type, isSelected = selectedShapeType == type, onClick = { onShapeTypeSelect(type) })
            }
        }
    }
}

@Composable
private fun ToolbarShapeIconItem(
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
            val strokeStyle = Stroke(width = 2f)

            when (shapeType) {
                Shape.Type.LINE -> {
                    drawLine(iconColor, start = Offset(2f, h - 2f), end = Offset(w - 2f, 2f), strokeWidth = 2.5f)
                }
                Shape.Type.ARROW -> {
                    drawLine(iconColor, start = Offset(2f, 2f), end = Offset(w - 2f, h - 2f), strokeWidth = 2.5f)
                    val path = Path().apply {
                        moveTo(w - 2f, h - 2f)
                        lineTo(w - 8f, h - 2f)
                        moveTo(w - 2f, h - 2f)
                        lineTo(w - 2f, h - 8f)
                    }
                    drawPath(path, iconColor, style = strokeStyle)
                }
                Shape.Type.ARROW_DOUBLE -> {
                    drawLine(iconColor, start = Offset(4f, h - 4f), end = Offset(w - 4f, 4f), strokeWidth = 2.5f)
                }
                Shape.Type.CORNER -> {
                    drawLine(iconColor, start = Offset(2f, 2f), end = Offset(w - 2f, 2f), strokeWidth = 2.5f)
                    drawLine(iconColor, start = Offset(w - 2f, 2f), end = Offset(w - 2f, h - 2f), strokeWidth = 2.5f)
                }
                Shape.Type.CORNER_ARROW_SINGLE -> {
                    drawLine(iconColor, start = Offset(2f, 2f), end = Offset(w - 2f, 2f), strokeWidth = 2.5f)
                    drawLine(iconColor, start = Offset(w - 2f, 2f), end = Offset(w - 2f, h - 2f), strokeWidth = 2.5f)
                }
                Shape.Type.CORNER_ARROW_DOUBLE -> {
                    drawLine(iconColor, start = Offset(2f, 2f), end = Offset(w - 2f, 2f), strokeWidth = 2.5f)
                    drawLine(iconColor, start = Offset(w - 2f, 2f), end = Offset(w - 2f, h - 2f), strokeWidth = 2.5f)
                }
                Shape.Type.RECTANGLE -> {
                    drawRect(iconColor, topLeft = Offset(2f, 2f), size = Size(w - 4f, h - 4f), style = strokeStyle)
                }
                Shape.Type.ROUNDED_RECTANGLE -> {
                    drawRoundRect(iconColor, topLeft = Offset(2f, 2f), size = Size(w - 4f, h - 4f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f), style = strokeStyle)
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
                    drawLine(iconColor, start = Offset(3f, 2f), end = Offset(3f, h - 3f), strokeWidth = 2f)
                    drawLine(iconColor, start = Offset(3f, h - 3f), end = Offset(w - 2f, h - 3f), strokeWidth = 2f)
                }
                Shape.Type.QUADRANT_4 -> {
                    drawLine(iconColor, start = Offset(2f, h / 2f), end = Offset(w - 2f, h / 2f), strokeWidth = 1.8f)
                    drawLine(iconColor, start = Offset(w / 2f, 2f), end = Offset(w / 2f, h - 2f), strokeWidth = 1.8f)
                }
                Shape.Type.AXIS_3D -> {
                    val cx = w / 2f
                    val cy = h / 2f
                    drawLine(iconColor, start = Offset(cx, cy), end = Offset(cx, 2f), strokeWidth = 1.8f)
                    drawLine(iconColor, start = Offset(cx, cy), end = Offset(w - 2f, cy), strokeWidth = 1.8f)
                    drawLine(iconColor, start = Offset(cx, cy), end = Offset(2f, h - 2f), strokeWidth = 1.8f)
                }
                Shape.Type.NUMBER_LINE -> {
                    val midY = h / 2f
                    drawLine(iconColor, start = Offset(2f, midY), end = Offset(w - 2f, midY), strokeWidth = 2f)
                    drawLine(iconColor, start = Offset(w / 2f, midY - 4f), end = Offset(w / 2f, midY + 4f), strokeWidth = 2f)
                    drawLine(iconColor, start = Offset(w * 0.25f, midY - 3f), end = Offset(w * 0.25f, midY + 3f), strokeWidth = 1.5f)
                    drawLine(iconColor, start = Offset(w * 0.75f, midY - 3f), end = Offset(w * 0.75f, midY + 3f), strokeWidth = 1.5f)
                }
                Shape.Type.POLAR_GRID -> {
                    drawCircle(iconColor, radius = w * 0.42f, center = Offset(w / 2f, h / 2f), style = Stroke(width = 1.2f))
                    drawCircle(iconColor, radius = w * 0.22f, center = Offset(w / 2f, h / 2f), style = Stroke(width = 1.2f))
                    drawLine(iconColor, start = Offset(2f, h / 2f), end = Offset(w - 2f, h / 2f), strokeWidth = 1.2f)
                    drawLine(iconColor, start = Offset(w / 2f, 2f), end = Offset(w / 2f, h - 2f), strokeWidth = 1.2f)
                }
                Shape.Type.GRAPH_PAPER_BG -> {
                    drawRect(iconColor, topLeft = Offset(2f, 2f), size = Size(w - 4f, h - 4f), style = Stroke(width = 1.2f))
                    drawLine(iconColor, start = Offset(w * 0.33f, 2f), end = Offset(w * 0.33f, h - 2f), strokeWidth = 1f)
                    drawLine(iconColor, start = Offset(w * 0.66f, 2f), end = Offset(w * 0.66f, h - 2f), strokeWidth = 1f)
                    drawLine(iconColor, start = Offset(2f, h * 0.33f), end = Offset(w - 2f, h * 0.33f), strokeWidth = 1f)
                    drawLine(iconColor, start = Offset(2f, h * 0.66f), end = Offset(w - 2f, h * 0.66f), strokeWidth = 1f)
                }
                Shape.Type.BAR_CHART_TEMPLATE -> {
                    drawLine(iconColor, start = Offset(2f, 2f), end = Offset(2f, h - 2f), strokeWidth = 1.8f)
                    drawLine(iconColor, start = Offset(2f, h - 2f), end = Offset(w - 2f, h - 2f), strokeWidth = 1.8f)
                    drawRect(iconColor, topLeft = Offset(6f, h * 0.5f), size = Size(4f, h * 0.5f - 2f), style = Stroke(width = 1.2f))
                    drawRect(iconColor, topLeft = Offset(13f, h * 0.25f), size = Size(4f, h * 0.75f - 2f), style = Stroke(width = 1.2f))
                }
                Shape.Type.PIE_CHART_TEMPLATE -> {
                    drawCircle(iconColor, radius = w * 0.42f, center = Offset(w / 2f, h / 2f), style = Stroke(width = 1.5f))
                    drawLine(iconColor, start = Offset(w / 2f, h / 2f), end = Offset(w - 2f, h / 2f), strokeWidth = 1.5f)
                    drawLine(iconColor, start = Offset(w / 2f, h / 2f), end = Offset(w * 0.25f, 4f), strokeWidth = 1.5f)
                    drawLine(iconColor, start = Offset(w / 2f, h / 2f), end = Offset(w * 0.35f, h - 4f), strokeWidth = 1.5f)
                }
            }
        }
    }
}

private data class Tuple6<A, B, C, D, E, F>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
    val sixth: F
)
