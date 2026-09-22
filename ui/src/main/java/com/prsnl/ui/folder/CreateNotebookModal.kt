package com.prsnl.ui.folder

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

val PAPER_COLOR_SWATCHES = listOf(
    Pair("Warm Cream", 0xFFFAF8F5.toInt()),
    Pair("Pastel Pink", 0xFFFFE4E6.toInt()),
    Pair("Soft Mint", 0xFFE8F5E9.toInt()),
    Pair("Lavender", 0xFFF3E8FF.toInt()),
    Pair("Legal Yellow", 0xFFFFF9E6.toInt()),
    Pair("Pure White", 0xFFFFFFFF.toInt())
)

val COVER_THEME_COLORS = listOf(
    0xFF8B5E3C.toInt(),
    0xFFC88A4B.toInt(),
    0xFF4C6EF5.toInt(),
    0xFF4A7C59.toInt(),
    0xFFC85A32.toInt()
)

@Composable
fun CreateNotebookFullModal(
    folderName: String = "Personal",
    onDismiss: () -> Unit,
    onCreate: (title: String, coverColor: Int, coverStyle: String, bgType: Background.Type, paperColor: Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedCoverColor by remember { mutableIntStateOf(0xFF8B5E3C.toInt()) }
    var selectedCoverStyle by remember { mutableStateOf("DEFAULT") }
    var selectedPaperType by remember { mutableStateOf(Background.Type.MARGIN_RULED) }
    var selectedPaperColor by remember { mutableIntStateOf(PAPER_COLOR_SWATCHES[0].second) }

    val dialogTitle = if (folderName.isNotBlank() && !folderName.equals("Personal", ignoreCase = true)) {
        "Create Notebook in '$folderName'"
    } else {
        "Create New Notebook"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFF5F0E6),
        titleContentColor = Color(0xFF2D2B28),
        textContentColor = Color(0xFF2D2B28),
        title = { Text(dialogTitle, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Notebook Name (e.g. Maths, Physics, Journal)", color = Color(0xFF5C5850)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFC88A4B),
                        unfocusedBorderColor = Color(0xFFE2D7C5),
                        focusedTextColor = Color(0xFF2D2B28),
                        unfocusedTextColor = Color(0xFF2D2B28)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text("Notebook Topic / Icon", fontSize = 11.sp, color = Color(0xFF5C5850), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TOPIC_ICON_OPTIONS.take(4).forEach { (iconKey, labelText) ->
                        FilterChip(
                            selected = selectedCoverStyle == iconKey,
                            onClick = { selectedCoverStyle = iconKey },
                            label = { Text(labelText, fontSize = 10.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Cover Theme Color", fontSize = 11.sp, color = Color(0xFF5C5850), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    COVER_THEME_COLORS.forEach { c ->
                        val isSelected = selectedCoverColor == c
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(c))
                                .border(
                                    width = if (isSelected) 2.5.dp else 0.dp,
                                    color = if (isSelected) Color(0xFFC88A4B) else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedCoverColor = c },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Paper Format", fontSize = 11.sp, color = Color(0xFF5C5850), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = selectedPaperType == Background.Type.MARGIN_RULED,
                        onClick = { selectedPaperType = Background.Type.MARGIN_RULED },
                        label = { Text("Margin", fontSize = 10.sp) }
                    )
                    FilterChip(
                        selected = selectedPaperType == Background.Type.RULED,
                        onClick = { selectedPaperType = Background.Type.RULED },
                        label = { Text("Ruled", fontSize = 10.sp) }
                    )
                    FilterChip(
                        selected = selectedPaperType == Background.Type.GRID,
                        onClick = { selectedPaperType = Background.Type.GRID },
                        label = { Text("Grid", fontSize = 10.sp) }
                    )
                    FilterChip(
                        selected = selectedPaperType == Background.Type.CORNELL,
                        onClick = { selectedPaperType = Background.Type.CORNELL },
                        label = { Text("Cornell", fontSize = 10.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Paper Background Color", fontSize = 11.sp, color = Color(0xFF5C5850), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PAPER_COLOR_SWATCHES.forEach { (_, pc) ->
                        val isSelected = selectedPaperColor == pc
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(pc))
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF8B5E3C) else Color(0xFFD6C8B4),
                                    shape = CircleShape
                                )
                                .clickable { selectedPaperColor = pc },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF8B5E3C))
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) onCreate(title.trim(), selectedCoverColor, selectedCoverStyle, selectedPaperType, selectedPaperColor)
                }
            ) {
                Text("Create Notebook", color = Color(0xFFC88A4B), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF5C5850))
            }
        }
    )
}
