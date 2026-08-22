package com.prsnl.ui.editor

import android.graphics.Color as AndroidColor
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prsnl.document.model.Background

@Composable
fun NotebookSettingsModal(
    currentBackground: Background,
    isFingerDrawingEnabled: Boolean,
    isPressureSensitivityEnabled: Boolean,
    onDismiss: () -> Unit,
    onBackgroundTypeChange: (Background.Type) -> Unit,
    onLineSpacingChange: (Float) -> Unit,
    onPaperColorChange: (Int) -> Unit,
    onFingerDrawingToggle: () -> Unit,
    onPressureSensitivityToggle: () -> Unit
) {
    var selectedType by remember { mutableStateOf(currentBackground.type) }
    var lineSpacing by remember { mutableFloatStateOf(currentBackground.lineSpacing ?: 40f) }
    var selectedPaperColor by remember { mutableIntStateOf(currentBackground.colorLight) }
    val scrollState = rememberScrollState()

    val templateOptions = listOf(
        Pair("Margin Ruled (Date & Red Line)", Background.Type.MARGIN_RULED),
        Pair("Ruled Lines", Background.Type.RULED),
        Pair("Square Grid Pattern", Background.Type.GRID),
        Pair("Isometric 3D Grid", Background.Type.ISOMETRIC),
        Pair("Dot Matrix Grid", Background.Type.DOTTED),
        Pair("Cornell Notes Layout", Background.Type.CORNELL),
        Pair("2-Column Layout", Background.Type.COLUMN_2),
        Pair("Music Staves", Background.Type.MUSIC),
        Pair("Plain Blank Paper", Background.Type.BLANK)
    )

    val paperColorSwatches = listOf(
        Pair("Warm Cream", AndroidColor.parseColor("#FAF8F5")),
        Pair("Legal Yellow", AndroidColor.parseColor("#FFF9E6")),
        Pair("Soft Mint", AndroidColor.parseColor("#EAF4EC")),
        Pair("Pastel Pink", AndroidColor.parseColor("#FCE4EC")),
        Pair("Lavender", AndroidColor.parseColor("#F5F3FF")),
        Pair("Cool Gray", AndroidColor.parseColor("#F0F2F5")),
        Pair("Charcoal", AndroidColor.parseColor("#1C1C1E")),
        Pair("Pure White", AndroidColor.parseColor("#FFFFFF"))
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFF5F0E6),
        titleContentColor = Color(0xFF2D2B28),
        textContentColor = Color(0xFF2D2B28),
        title = { Text("Notebook & Page Formatting Settings", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                // 1. Input Mode Toggles
                Text("Touch & Input Control", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF5C5850))
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEFE8DA))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Finger Writing / Drawing", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF2D2B28))
                        Text(
                            if (isFingerDrawingEnabled) "Finger draws ink on canvas" else "Finger scrolls canvas (Stylus writing lock)",
                            fontSize = 11.sp,
                            color = Color(0xFF5C5850)
                        )
                    }
                    Switch(
                        checked = isFingerDrawingEnabled,
                        onCheckedChange = { onFingerDrawingToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFC88A4B)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEFE8DA))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Pressure Sensitivity", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF2D2B28))
                        Text(
                            if (isPressureSensitivityEnabled) "Dynamic pressure stroke width enabled" else "Fixed width stroke rendering",
                            fontSize = 11.sp,
                            color = Color(0xFF5C5850)
                        )
                    }
                    Switch(
                        checked = isPressureSensitivityEnabled,
                        onCheckedChange = { onPressureSensitivityToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFC88A4B)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 2. Paper Color Tint
                Text("Paper Color Swatch", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF5C5850))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    paperColorSwatches.forEach { (name, colorInt) ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(colorInt))
                                .border(
                                    width = if (selectedPaperColor == colorInt) 3.dp else 1.dp,
                                    color = if (selectedPaperColor == colorInt) Color(0xFFC88A4B) else Color(0xFFE2D7C5),
                                    shape = CircleShape
                                )
                                .clickable {
                                    selectedPaperColor = colorInt
                                    onPaperColorChange(colorInt)
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 3. Paper Lining Pattern Template
                Text("Paper Template Layout", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF5C5850))
                Spacer(modifier = Modifier.height(8.dp))

                templateOptions.forEach { (label, type) ->
                    val isSelected = selectedType == type
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Color(0xFFC88A4B) else Color(0xFFEFE8DA))
                            .border(1.dp, if (isSelected) Color(0xFFC88A4B) else Color(0xFFE2D7C5), RoundedCornerShape(10.dp))
                            .clickable {
                                selectedType = type
                                onBackgroundTypeChange(type)
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else Color(0xFF2D2B28),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }

                if (selectedType == Background.Type.RULED || selectedType == Background.Type.GRID || selectedType == Background.Type.MARGIN_RULED) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Line Spacing Height", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF5C5850))
                        Text("${lineSpacing.toInt()} px", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC88A4B))
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Slider(
                        value = lineSpacing,
                        onValueChange = {
                            lineSpacing = it
                            onLineSpacingChange(it)
                        },
                        valueRange = 20f..90f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFC88A4B),
                            activeTrackColor = Color(0xFFC88A4B)
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Save & Apply", color = Color(0xFFC88A4B), fontWeight = FontWeight.Bold)
            }
        }
    )
}
