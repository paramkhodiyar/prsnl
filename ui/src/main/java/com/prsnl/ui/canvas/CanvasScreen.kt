package com.prsnl.ui.canvas

import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.prsnl.document.model.Command
import com.prsnl.document.model.Element
import com.prsnl.drawing.view.CanvasToolMode
import com.prsnl.drawing.view.DrawingCanvasView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasScreen(
    onBackClick: () -> Unit = {}
) {
    val committedElements = remember { mutableStateListOf<Element>() }
    var selectedColor by remember { mutableIntStateOf(Color.BLACK) }
    var selectedWidth by remember { mutableFloatStateOf(6f) }
    var selectedToolMode by remember { mutableStateOf(CanvasToolMode.PEN) }

    val colorOptions = listOf(
        Color.BLACK,
        Color.parseColor("#1E88E5"), // Blue
        Color.parseColor("#E53935"), // Red
        Color.parseColor("#43A047"), // Green
        Color.parseColor("#FDD835")  // Yellow
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bare Canvas Test (Phase 2 & 3)") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tool Toggle
                FilterChip(
                    selected = selectedToolMode == CanvasToolMode.PEN,
                    onClick = { selectedToolMode = CanvasToolMode.PEN },
                    label = { Text("Pen") },
                    leadingIcon = { Icon(Icons.Default.Create, contentDescription = null) }
                )
                FilterChip(
                    selected = selectedToolMode == CanvasToolMode.HIGHLIGHTER,
                    onClick = { selectedToolMode = CanvasToolMode.HIGHLIGHTER },
                    label = { Text("Highlighter") }
                )

                // Color Picker Options
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colorOptions.forEach { colorInt ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(androidx.compose.ui.graphics.Color(colorInt))
                                .border(
                                    width = if (selectedColor == colorInt) 3.dp else 1.dp,
                                    color = if (selectedColor == colorInt) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = colorInt }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AndroidView(
                factory = { context ->
                    DrawingCanvasView(context).apply {
                        this.currentColor = selectedColor
                        this.currentBaseWidth = selectedWidth
                        this.currentToolMode = selectedToolMode
                        this.onCommandIssued = { command ->
                            val updated = command.apply(
                                com.prsnl.document.model.Page(
                                    id = "test",
                                    notebookId = "test",
                                    index = 0,
                                    width = 1000f,
                                    height = 1000f,
                                    background = com.prsnl.document.model.Background(
                                        type = com.prsnl.document.model.Background.Type.BLANK,
                                        colorLight = 0,
                                        colorDark = 0
                                    ),
                                    elements = committedElements.toList()
                                )
                            )
                            committedElements.clear()
                            committedElements.addAll(updated.elements)
                        }
                    }
                },
                update = { view ->
                    view.currentColor = selectedColor
                    view.currentBaseWidth = selectedWidth
                    view.currentToolMode = selectedToolMode
                    view.committedElements = committedElements.toList()
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
