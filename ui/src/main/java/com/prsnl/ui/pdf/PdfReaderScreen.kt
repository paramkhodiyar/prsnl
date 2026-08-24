package com.prsnl.ui.pdf

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.prsnl.document.model.Command
import com.prsnl.document.model.Page
import com.prsnl.drawing.view.CanvasToolMode
import com.prsnl.drawing.view.DrawingCanvasView
import com.prsnl.pdf.PdfExporter
import com.prsnl.ui.editor.PageEditorViewModel
import java.io.File
import java.util.UUID

val HIGHLIGHTER_COLORS = listOf(
    0x88FFEB3B.toInt(), // Yellow
    0x888BC34A.toInt(), // Light Green
    0x88FF4081.toInt(), // Soft Pink
    0x8800BCD4.toInt(), // Cyan
    0x88FF9800.toInt()  // Orange
)

val PEN_COLORS = listOf(
    0xFF000000.toInt(), // Black
    0xFF1D4ED8.toInt(), // Royal Blue
    0xFFDC2626.toInt(), // Red
    0xFF15803D.toInt(), // Dark Green
    0xFF6B21A8.toInt()  // Purple
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    viewModel: PageEditorViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val pages by viewModel.pagesList.collectAsState()
    val notebookTitle by viewModel.notebookTitle.collectAsState()
    val activeIndex by viewModel.activePageIndex.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val currentTool by viewModel.toolMode.collectAsState()

    var selectedColor by remember { mutableIntStateOf(0xFF000000.toInt()) }
    var selectedWidth by remember { mutableFloatStateOf(6f) }
    var isFingerDrawingEnabled by remember { mutableStateOf(true) }
    var isExporting by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Sync active page index with scroll position
    LaunchedEffect(listState.firstVisibleItemIndex) {
        if (pages.isNotEmpty()) {
            val visibleIndex = listState.firstVisibleItemIndex.coerceIn(0, pages.size - 1)
            if (visibleIndex != activeIndex) {
                viewModel.setActivePageIndex(visibleIndex)
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F172A) // Slate Dark Background for PDF Reader
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Navigation Bar
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = notebookTitle,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (pages.isNotEmpty()) "Page ${activeIndex + 1} of ${pages.size} • PDF Markup Mode" else "PDF Reader",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        // Export PDF Action
                        TextButton(
                            onClick = {
                                if (pages.isNotEmpty() && !isExporting) {
                                    isExporting = true
                                    val exportFile = File(context.cacheDir, "marked_up_${UUID.randomUUID()}.pdf")
                                    val exporter = PdfExporter()
                                    val success = exporter.exportPagesToPdf(pages, exportFile)
                                    isExporting = false
                                    if (success) {
                                        Toast.makeText(context, "Exported PDF with annotations: ${exportFile.name}", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Failed to export marked-up PDF", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Export PDF", tint = Color(0xFF38BDF8))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
                )

                // Continuous PDF Page Vertical Reader with Stylus Annotation Overlays
                if (pages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF38BDF8))
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 100.dp),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        itemsIndexed(pages, key = { _, p -> p.id }) { index, page ->
                            PdfPageCard(
                                page = page,
                                index = index,
                                toolMode = currentTool,
                                currentColor = selectedColor,
                                currentWidth = selectedWidth,
                                isFingerDrawingEnabled = isFingerDrawingEnabled,
                                onCommandIssued = { cmd ->
                                    viewModel.executeCommand(index, cmd)
                                }
                            )
                        }
                    }
                }
            }

            // Floating Pen Tray Toolbar for PDF Markup
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .statusBarsPadding()
            ) {
                PdfPenTrayToolbar(
                    currentToolMode = currentTool,
                    currentColor = selectedColor,
                    canUndo = canUndo,
                    canRedo = canRedo,
                    isFingerDrawingEnabled = isFingerDrawingEnabled,
                    onSelectTool = { tool ->
                        viewModel.setToolMode(tool)
                        if (tool == CanvasToolMode.HIGHLIGHTER) {
                            if (selectedColor !in HIGHLIGHTER_COLORS) {
                                selectedColor = HIGHLIGHTER_COLORS.first()
                            }
                        } else if (tool == CanvasToolMode.PEN) {
                            if (selectedColor in HIGHLIGHTER_COLORS) {
                                selectedColor = PEN_COLORS.first()
                            }
                        }
                    },
                    onSelectColor = { color -> selectedColor = color },
                    onUndo = { viewModel.undo() },
                    onRedo = { viewModel.redo() }
                )
            }
        }
    }
}

@Composable
private fun PdfPageCard(
    page: Page,
    index: Int,
    toolMode: CanvasToolMode,
    currentColor: Int,
    currentWidth: Float,
    isFingerDrawingEnabled: Boolean,
    onCommandIssued: (Command) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    DrawingCanvasView(ctx).apply {
                        this.pageIndex = index
                        this.documentWidth = page.width
                        this.documentHeight = page.height
                        this.currentBackground = page.background
                        this.currentToolMode = toolMode
                        this.currentColor = currentColor
                        this.currentBaseWidth = currentWidth
                        this.isFingerDrawingEnabled = isFingerDrawingEnabled
                        this.committedElements = page.elements
                        this.onCommandIssued = onCommandIssued
                    }
                },
                update = { view ->
                    view.pageIndex = index
                    view.documentWidth = page.width
                    view.documentHeight = page.height
                    view.currentBackground = page.background
                    view.currentToolMode = toolMode
                    view.currentColor = currentColor
                    view.currentBaseWidth = currentWidth
                    view.isFingerDrawingEnabled = isFingerDrawingEnabled
                    view.committedElements = page.elements
                    view.onCommandIssued = onCommandIssued
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(page.width / page.height)
            )
        }
    }
}

@Composable
private fun PdfPenTrayToolbar(
    currentToolMode: CanvasToolMode,
    currentColor: Int,
    canUndo: Boolean,
    canRedo: Boolean,
    isFingerDrawingEnabled: Boolean,
    onSelectTool: (CanvasToolMode) -> Unit,
    onSelectColor: (Int) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.95f),
        tonalElevation = 12.dp,
        modifier = Modifier
            .border(1.5.dp, Color(0xFF334155), RoundedCornerShape(28.dp))
            .padding(horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Read / Pan Mode
            IconButton(
                onClick = { onSelectTool(CanvasToolMode.SELECT) },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (currentToolMode == CanvasToolMode.SELECT) Color(0xFF38BDF8) else Color.Transparent)
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = "Read/Scroll Mode",
                    tint = if (currentToolMode == CanvasToolMode.SELECT) Color.Black else Color.White
                )
            }

            // Pen Tool
            IconButton(
                onClick = { onSelectTool(CanvasToolMode.PEN) },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (currentToolMode == CanvasToolMode.PEN) Color(0xFF38BDF8) else Color.Transparent)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Pen",
                    tint = if (currentToolMode == CanvasToolMode.PEN) Color.Black else Color.White
                )
            }

            // Highlighter Tool
            IconButton(
                onClick = { onSelectTool(CanvasToolMode.HIGHLIGHTER) },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (currentToolMode == CanvasToolMode.HIGHLIGHTER) Color(0xFFFACC15) else Color.Transparent)
            ) {
                Icon(
                    imageVector = Icons.Default.Create,
                    contentDescription = "Highlighter",
                    tint = if (currentToolMode == CanvasToolMode.HIGHLIGHTER) Color.Black else Color.White
                )
            }

            // Eraser Tool
            IconButton(
                onClick = { onSelectTool(CanvasToolMode.STROKE_ERASER) },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (currentToolMode == CanvasToolMode.STROKE_ERASER) Color(0xFFEF4444) else Color.Transparent)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eraser",
                    tint = if (currentToolMode == CanvasToolMode.STROKE_ERASER) Color.White else Color.White
                )
            }

            // Lasso Tool
            IconButton(
                onClick = { onSelectTool(CanvasToolMode.LASSO) },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (currentToolMode == CanvasToolMode.LASSO) Color(0xFF818CF8) else Color.Transparent)
            ) {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = "Lasso Selection",
                    tint = if (currentToolMode == CanvasToolMode.LASSO) Color.White else Color.White
                )
            }

            HorizontalDivider(
                modifier = Modifier
                    .height(24.dp)
                    .width(1.dp),
                color = Color.White.copy(alpha = 0.2f)
            )

            // Color Swatches
            val activePalette = if (currentToolMode == CanvasToolMode.HIGHLIGHTER) HIGHLIGHTER_COLORS else PEN_COLORS
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                activePalette.forEach { colorInt ->
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(colorInt))
                            .border(
                                width = if (currentColor == colorInt) 2.5.dp else 0.dp,
                                color = Color.White,
                                shape = CircleShape
                            )
                            .clickable { onSelectColor(colorInt) }
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier
                    .height(24.dp)
                    .width(1.dp),
                color = Color.White.copy(alpha = 0.2f)
            )

            // Undo & Redo
            IconButton(onClick = onUndo, enabled = canUndo) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Undo",
                    tint = if (canUndo) Color.White else Color.White.copy(alpha = 0.3f)
                )
            }

            IconButton(onClick = onRedo, enabled = canRedo) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Redo",
                    tint = if (canRedo) Color.White else Color.White.copy(alpha = 0.3f)
                )
            }
        }
    }
}
