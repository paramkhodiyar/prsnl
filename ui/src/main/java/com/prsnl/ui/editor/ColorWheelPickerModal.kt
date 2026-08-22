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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ColorWheelPickerModal(
    initialColor: Int,
    onDismiss: () -> Unit,
    onColorSelected: (Int) -> Unit
) {
    var red by remember { mutableFloatStateOf(AndroidColor.red(initialColor) / 255f) }
    var green by remember { mutableFloatStateOf(AndroidColor.green(initialColor) / 255f) }
    var blue by remember { mutableFloatStateOf(AndroidColor.blue(initialColor) / 255f) }
    var hexInput by remember { mutableStateOf(String.format("#%06X", 0xFFFFFF and initialColor)) }

    val currentColorInt = remember(red, green, blue) {
        AndroidColor.rgb((red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())
    }

    val presetColors = listOf(
        "#000000", "#1E293B", "#64748B", "#FFFFFF",
        "#E53935", "#D81B60", "#8E24AA", "#5E35B1",
        "#3949AB", "#1E88E5", "#039BE5", "#00ACC1",
        "#00897B", "#43A047", "#7CB342", "#C0CA33",
        "#FDD835", "#FB8C00", "#F4511E", "#6D4C41"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        titleContentColor = Color.White,
        title = { Text("Color Picker & Hex Chooser", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Color Preview Box
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(currentColorInt))
                            .border(2.dp, Color.White, CircleShape)
                    )

                    OutlinedTextField(
                        value = hexInput,
                        onValueChange = { input ->
                            hexInput = input
                            if (input.matches(Regex("^#[0-9A-Fa-f]{6}$"))) {
                                try {
                                    val parsed = AndroidColor.parseColor(input)
                                    red = AndroidColor.red(parsed) / 255f
                                    green = AndroidColor.green(parsed) / 255f
                                    blue = AndroidColor.blue(parsed) / 255f
                                } catch (_: Exception) {}
                            }
                        },
                        label = { Text("Hex Code", color = Color(0xFF94A3B8)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.width(160.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // RGB Sliders
                Text("Red", color = Color(0xFFEF4444), style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = red,
                    onValueChange = {
                        red = it
                        hexInput = String.format("#%06X", 0xFFFFFF and currentColorInt)
                    },
                    colors = SliderDefaults.colors(thumbColor = Color(0xFFEF4444), activeTrackColor = Color(0xFFEF4444))
                )

                Text("Green", color = Color(0xFF22C55E), style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = green,
                    onValueChange = {
                        green = it
                        hexInput = String.format("#%06X", 0xFFFFFF and currentColorInt)
                    },
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF22C55E), activeTrackColor = Color(0xFF22C55E))
                )

                Text("Blue", color = Color(0xFF38BDF8), style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = blue,
                    onValueChange = {
                        blue = it
                        hexInput = String.format("#%06X", 0xFFFFFF and currentColorInt)
                    },
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF38BDF8))
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Preset Swatches", color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(6.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    items(presetColors) { hex ->
                        val colorParsed = AndroidColor.parseColor(hex)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(colorParsed))
                                .border(1.dp, Color.Gray, CircleShape)
                                .clickable {
                                    red = AndroidColor.red(colorParsed) / 255f
                                    green = AndroidColor.green(colorParsed) / 255f
                                    blue = AndroidColor.blue(colorParsed) / 255f
                                    hexInput = hex
                                }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onColorSelected(currentColorInt)
                    onDismiss()
                }
            ) {
                Text("Select Color", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        }
    )
}
