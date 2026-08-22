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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
    onDismiss: () -> Unit,
    onBackgroundTypeChange: (Background.Type) -> Unit,
    onLineSpacingChange: (Float) -> Unit,
    onPaperColorChange: (Int) -> Unit
) {
    var selectedType by remember { mutableStateOf(currentBackground.type) }
    var lineSpacing by remember { mutableFloatStateOf(currentBackground.lineSpacing ?: 40f) }
    var selectedPaperColor by remember { mutableIntStateOf(currentBackground.colorLight) }

    val patternOptions = listOf(
        Pair("Ruled Lines", Background.Type.RULED),
        Pair("Grid Pattern", Background.Type.GRID),
        Pair("Dot Matrix", Background.Type.DOTTED),
        Pair("Plain Blank", Background.Type.BLANK)
    )

    val paperColorSwatches = listOf(
        Pair("Cream White", AndroidColor.parseColor("#FAF8F5")),
        Pair("Pale Yellow", AndroidColor.parseColor("#FFF9E6")),
        Pair("Pale Green", AndroidColor.parseColor("#EAF4EC")),
        Pair("Pastel Pink", AndroidColor.parseColor("#FCE4EC")),
        Pair("Cool Gray", AndroidColor.parseColor("#F0F2F5")),
        Pair("Charcoal", AndroidColor.parseColor("#1C1C1E"))
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2C2C2E),
        titleContentColor = Color(0xFFF5F2EB),
        title = { Text("Notebook Paper Settings", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Paper Color Tint", style = MaterialTheme.typography.labelMedium, color = Color(0xFFA1A1AA))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    paperColorSwatches.forEach { (name, colorInt) ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(colorInt))
                                .border(
                                    width = if (selectedPaperColor == colorInt) 3.dp else 1.dp,
                                    color = if (selectedPaperColor == colorInt) Color(0xFFE0A96D) else Color.Gray,
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

                Text("Paper Lining Pattern", style = MaterialTheme.typography.labelMedium, color = Color(0xFFA1A1AA))
                Spacer(modifier = Modifier.height(8.dp))

                patternOptions.forEach { (label, type) ->
                    val isSelected = selectedType == type
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Color(0xFFE0A96D) else Color(0xFF1C1C1E))
                            .border(1.dp, if (isSelected) Color(0xFFE0A96D) else Color(0xFF3A3A3C), RoundedCornerShape(10.dp))
                            .clickable {
                                selectedType = type
                                onBackgroundTypeChange(type)
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color(0xFF1C1C1E) else Color(0xFFF5F2EB),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                if (selectedType == Background.Type.RULED || selectedType == Background.Type.GRID) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Line Height / Spacing Bar", style = MaterialTheme.typography.labelMedium, color = Color(0xFFA1A1AA))
                        Text("${lineSpacing.toInt()} px", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE0A96D))
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
                            thumbColor = Color(0xFFE0A96D),
                            activeTrackColor = Color(0xFFE0A96D)
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = Color(0xFFE0A96D), fontWeight = FontWeight.Bold)
            }
        }
    )
}
