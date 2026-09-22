package com.prsnl.ui.pdf

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.RectF
import com.prsnl.document.model.ImageElement
import com.prsnl.document.model.Page
import com.prsnl.document.model.Shape
import com.prsnl.document.model.Stroke
import com.prsnl.document.model.TextBox
import com.prsnl.drawing.render.ShapeRenderer
import com.prsnl.drawing.render.StrokeRenderer
import com.prsnl.drawing.view.CanvasToolMode
import com.prsnl.pdf.PdfExporter
import com.prsnl.ui.common.BrushPalettes
import com.prsnl.ui.editor.PageEditorViewModel
import com.prsnl.ui.editor.ToolIcon
import java.io.File
import java.util.UUID

val HIGHLIGHTER_COLORS = BrushPalettes.HIGHLIGHTER_COLORS
val PEN_COLORS = BrushPalettes.PEN_COLORS

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

    var selectedColor by remember { mutableIntStateOf(0xFF2D2B28.toInt()) }
    var penThickness by remember { mutableFloatStateOf(4f) }
    var highlighterThickness by remember { mutableFloatStateOf(28f) }
    var eraserRadius by remember { mutableFloatStateOf(36f) }

    val activeThickness = when (currentTool) {
        CanvasToolMode.HIGHLIGHTER -> highlighterThickness
        CanvasToolMode.STROKE_ERASER, CanvasToolMode.PIXEL_ERASER -> eraserRadius
        else -> penThickness
    }
    var isFingerDrawingEnabled by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var pdfViewRef by remember { mutableStateOf<com.github.barteksc.pdfviewer.PDFView?>(null) }
    var overlayViewRef by remember { mutableStateOf<PdfAnnotationOverlayView?>(null) }

    // Launcher to re-link or attach missing PDF binary on device
    val pdfPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null && pages.isNotEmpty()) {
            val notebookId = pages.first().notebookId
            val storageDir = File(context.filesDir, "pdf_imports/$notebookId")
            if (!storageDir.exists()) storageDir.mkdirs()
            val targetPdf = File(storageDir, "document.pdf")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    java.io.FileOutputStream(targetPdf).use { output ->
                        input.copyTo(output)
                        output.flush()
                    }
                }
                if (targetPdf.exists() && targetPdf.length() > 0L) {
                    viewModel.relinkPdfSource(targetPdf)
                    Toast.makeText(context, "PDF successfully linked! Notes restored.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to read selected PDF file.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error linking PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Resolve or synthesize valid document.pdf from files/legacy pngs
    val pdfFile = remember(pages) {
        resolvePdfDocumentFile(context, pages)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF1E1C1A) // PRSNL Moleskine Warm Dark Palette
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Navigation Bar
                TopAppBar(
                    title = {
                        Column(
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = notebookTitle,
                                color = Color(0xFFFAF8F5),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (pages.isNotEmpty()) "Page ${activeIndex + 1} of ${pages.size} • Markup Mode" else "PDF Document",
                                color = Color(0xFFC88A4B),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFFFAF8F5)
                            )
                        }
                    },
                    actions = {
                        // Finger Writing Toggle
                        IconButton(
                            onClick = {
                                isFingerDrawingEnabled = !isFingerDrawingEnabled
                                val msg = if (isFingerDrawingEnabled) "Finger drawing enabled" else "Finger drawing disabled (scroll with finger, write with stylus)"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = "Toggle Finger Drawing",
                                tint = if (isFingerDrawingEnabled) Color(0xFFC88A4B) else Color.White.copy(alpha = 0.5f)
                            )
                        }

                        // Export PDF Action
                        TextButton(
                            onClick = {
                                if (pages.isNotEmpty() && !isExporting) {
                                    isExporting = true
                                    val exportFile = File(context.cacheDir, "annotated_${UUID.randomUUID()}.pdf")
                                    val exporter = PdfExporter()
                                    val success = exporter.exportPagesToPdf(pages, exportFile, context)
                                    isExporting = false
                                    if (success) {
                                        Toast.makeText(context, "Exported PDF: ${exportFile.name}", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Failed to export PDF", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Export PDF", tint = Color(0xFFC88A4B))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export", color = Color(0xFFC88A4B), fontWeight = FontWeight.Bold)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF242220))
                )

                // Main PDF Reader with stylus ink overlay
                if (pages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFC88A4B))
                    }
                } else {
                    val activePage = pages.getOrNull(activeIndex) ?: pages.firstOrNull()

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 90.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (pdfFile != null && pdfFile.exists()) {

                            // 1. High-performance continuous PDF View with locked, synchronized annotation rendering
                            AndroidView(
                                factory = { ctx ->
                                    com.github.barteksc.pdfviewer.PDFView(ctx, null).apply {
                                        fromFile(pdfFile)
                                            .defaultPage(activeIndex.coerceIn(0, (pages.size - 1).coerceAtLeast(0)))
                                            .onPageChange { page, _ ->
                                                viewModel.setActivePageIndex(page)
                                            }
                                            .enableSwipe(true)
                                            .swipeHorizontal(false)
                                            .enableDoubletap(true)
                                            .pageFitPolicy(com.github.barteksc.pdfviewer.util.FitPolicy.WIDTH)
                                            .fitEachPage(true)
                                            .spacing(16)
                                            .scrollHandle(com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle(ctx))
                                            .onError { t ->
                                                android.util.Log.e("PdfReader", "Error loading PDF", t)
                                            }
                                            .onPageScroll { _, _ ->
                                                overlayViewRef?.invalidate()
                                            }
                                            .load()

                                        viewTreeObserver.addOnScrollChangedListener {
                                            overlayViewRef?.invalidate()
                                        }

                                        pdfViewRef = this
                                        overlayViewRef?.pdfView = this
                                    }
                                },
                                update = { pdfView ->
                                    pdfViewRef = pdfView
                                    overlayViewRef?.pdfView = pdfView
                                    if (pdfView.currentPage != activeIndex && activeIndex in 0 until pdfView.pageCount) {
                                        pdfView.jumpTo(activeIndex)
                                    }
                                    pdfView.invalidate()
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            // 2. Stylus and Touch Overlay for in-flight stroke tracking
                            AndroidView(
                                factory = { ctx ->
                                    PdfAnnotationOverlayView(ctx).apply {
                                        this.pdfView = pdfViewRef
                                        this.pages = pages
                                        this.currentToolMode = currentTool
                                        this.selectedColor = selectedColor
                                        this.selectedWidth = if (currentTool == CanvasToolMode.HIGHLIGHTER) highlighterThickness else penThickness
                                        this.eraserRadius = eraserRadius
                                        this.isFingerDrawingEnabled = isFingerDrawingEnabled
                                        this.onCommandIssued = { pageIdx, cmd ->
                                            viewModel.executeCommand(pageIdx, cmd)
                                            pdfViewRef?.invalidate()
                                        }
                                        overlayViewRef = this
                                    }
                                },
                                update = { overlay ->
                                    overlay.pdfView = pdfViewRef
                                    overlay.pages = pages
                                    overlay.currentToolMode = currentTool
                                    overlay.selectedColor = selectedColor
                                    overlay.selectedWidth = if (currentTool == CanvasToolMode.HIGHLIGHTER) highlighterThickness else penThickness
                                    overlay.eraserRadius = eraserRadius
                                    overlay.isFingerDrawingEnabled = isFingerDrawingEnabled
                                    overlay.onCommandIssued = { pageIdx, cmd ->
                                        viewModel.executeCommand(pageIdx, cmd)
                                        pdfViewRef?.invalidate()
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // Fallback if file could not be resolved
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .padding(24.dp)
                                    .background(Color(0xFF242220), RoundedCornerShape(16.dp))
                                    .border(1.dp, Color(0xFFC88A4B).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                    .padding(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = Color(0xFFC88A4B),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "PDF File Not Found On Device",
                                    color = Color(0xFFFAF8F5),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "The local PDF file is missing (e.g. after a fresh install). Your vector notes and annotations are safe! Re-link the PDF document to restore viewing instantly.",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = {
                                        pdfPickerLauncher.launch("application/pdf")
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFC88A4B),
                                        contentColor = Color(0xFFFAF8F5)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FileOpen,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Select & Re-link PDF",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Floating Quick Annotation Toolbar at bottom, styled to app theme
            PdfPenTrayToolbar(
                currentToolMode = currentTool,
                currentColor = selectedColor,
                currentThickness = activeThickness,
                canUndo = canUndo,
                canRedo = canRedo,
                onSelectTool = { tool ->
                    viewModel.setToolMode(tool)
                    if (tool == CanvasToolMode.HIGHLIGHTER && !BrushPalettes.isColorInPalette(selectedColor, CanvasToolMode.HIGHLIGHTER)) {
                        selectedColor = BrushPalettes.getDefaultColorForTool(CanvasToolMode.HIGHLIGHTER)
                    } else if (tool == CanvasToolMode.PEN && !BrushPalettes.isColorInPalette(selectedColor, CanvasToolMode.PEN)) {
                        selectedColor = BrushPalettes.getDefaultColorForTool(CanvasToolMode.PEN)
                    }
                },
                onSelectColor = { color -> selectedColor = color },
                onThicknessChange = { newThickness ->
                    when (currentTool) {
                        CanvasToolMode.HIGHLIGHTER -> highlighterThickness = newThickness
                        CanvasToolMode.STROKE_ERASER, CanvasToolMode.PIXEL_ERASER -> eraserRadius = newThickness
                        else -> penThickness = newThickness
                    }
                },
                onUndo = {
                    viewModel.undo()
                    overlayViewRef?.invalidate()
                    pdfViewRef?.invalidate()
                },
                onRedo = {
                    viewModel.redo()
                    overlayViewRef?.invalidate()
                    pdfViewRef?.invalidate()
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
            )
        }
    }
}

@Composable
private fun PdfPenTrayToolbar(
    currentToolMode: CanvasToolMode,
    currentColor: Int,
    currentThickness: Float,
    canUndo: Boolean,
    canRedo: Boolean,
    onSelectTool: (CanvasToolMode) -> Unit,
    onSelectColor: (Int) -> Unit,
    onThicknessChange: (Float) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isThicknessMenuOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Floating Thickness Menu Popover
        AnimatedVisibility(
            visible = isThicknessMenuOpen,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 }
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF242220).copy(alpha = 0.98f),
                tonalElevation = 16.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(1.5.dp, Color(0xFF47433E)),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .widthIn(min = 290.dp, max = 360.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header: Title & Value
                    val toolLabel = when (currentToolMode) {
                        CanvasToolMode.HIGHLIGHTER -> "Highlighter Thickness"
                        CanvasToolMode.STROKE_ERASER, CanvasToolMode.PIXEL_ERASER -> "Eraser Size"
                        CanvasToolMode.PENCIL -> "Pencil Thickness"
                        else -> "Pen Thickness"
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = toolLabel,
                            color = Color(0xFFFAF8F5),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFC88A4B).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFFC88A4B).copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "${currentThickness.toInt()} px",
                                color = Color(0xFFC88A4B),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Live Preview Box
                    val isEraser = currentToolMode == CanvasToolMode.STROKE_ERASER || currentToolMode == CanvasToolMode.PIXEL_ERASER
                    val previewColor = if (isEraser) Color(0xFF38BDF8) else Color(currentColor)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E1C1A))
                            .border(1.dp, Color(0xFF383531), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            val midY = size.height / 2f
                            if (isEraser) {
                                drawCircle(
                                    color = Color(0xFFFB7185),
                                    radius = (currentThickness / 2f).coerceIn(4f, size.height * 0.45f),
                                    center = Offset(size.width / 2f, midY),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = 2.2f,
                                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
                                    )
                                )
                            } else {
                                drawLine(
                                    color = previewColor,
                                    start = Offset(size.width * 0.12f, midY),
                                    end = Offset(size.width * 0.88f, midY),
                                    strokeWidth = currentThickness.coerceIn(1f, 36f),
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }

                    // Preset Chips
                    val presets = when (currentToolMode) {
                        CanvasToolMode.HIGHLIGHTER -> listOf(
                            Pair(18f, "Narrow"),
                            Pair(28f, "Normal"),
                            Pair(40f, "Broad"),
                            Pair(54f, "Chisel")
                        )
                        CanvasToolMode.STROKE_ERASER, CanvasToolMode.PIXEL_ERASER -> listOf(
                            Pair(16f, "Fine"),
                            Pair(36f, "Normal"),
                            Pair(60f, "Block")
                        )
                        else -> listOf(
                            Pair(2f, "Fine"),
                            Pair(4f, "Medium"),
                            Pair(8f, "Broad"),
                            Pair(14f, "Marker")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.forEach { (presetVal, label) ->
                            val isSelected = Math.abs(currentThickness - presetVal) < 1.5f
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFFC88A4B) else Color(0xFF2E2B27),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFFE5A96A) else Color(0xFF47433E)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onThicknessChange(presetVal) }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 5.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${presetVal.toInt()}",
                                        color = if (isSelected) Color.White else Color(0xFFFAF8F5),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.5f),
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }

                    // Slider
                    val (rangeMin, rangeMax) = when (currentToolMode) {
                        CanvasToolMode.HIGHLIGHTER -> Pair(12f, 64f)
                        CanvasToolMode.STROKE_ERASER, CanvasToolMode.PIXEL_ERASER -> Pair(12f, 76f)
                        else -> Pair(1f, 20f)
                    }

                    Slider(
                        value = currentThickness.coerceIn(rangeMin, rangeMax),
                        onValueChange = { onThicknessChange(it) },
                        valueRange = rangeMin..rangeMax,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFC88A4B),
                            activeTrackColor = Color(0xFFC88A4B),
                            inactiveTrackColor = Color(0xFF47433E)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Main Bottom Toolbar Surface
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF242220).copy(alpha = 0.96f),
            tonalElevation = 12.dp,
            border = BorderStroke(1.5.dp, Color(0xFF47433E)),
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. SELECT / Hand Tool
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (currentToolMode == CanvasToolMode.SELECT) Color(0xFFC88A4B) else Color(0xFF2E2B27))
                        .border(
                            1.2.dp,
                            if (currentToolMode == CanvasToolMode.SELECT) Color(0xFFE5A96A) else Color(0xFF47433E),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectTool(CanvasToolMode.SELECT) },
                    contentAlignment = Alignment.Center
                ) {
                    ToolIcon(
                        tool = CanvasToolMode.SELECT,
                        tintColor = if (currentToolMode == CanvasToolMode.SELECT) Color.White else Color(0xFF38BDF8),
                        size = 20.dp
                    )
                }

                // 2. PEN Tool
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (currentToolMode == CanvasToolMode.PEN) Color(0xFFC88A4B) else Color(0xFF2E2B27))
                        .border(
                            1.2.dp,
                            if (currentToolMode == CanvasToolMode.PEN) Color(0xFFE5A96A) else Color(0xFF47433E),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectTool(CanvasToolMode.PEN) },
                    contentAlignment = Alignment.Center
                ) {
                    ToolIcon(
                        tool = CanvasToolMode.PEN,
                        tintColor = if (currentToolMode == CanvasToolMode.PEN) Color(currentColor) else Color(0xFFFAF8F5),
                        size = 20.dp
                    )
                }

                // 3. HIGHLIGHTER Tool
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (currentToolMode == CanvasToolMode.HIGHLIGHTER) Color(0xFFC88A4B) else Color(0xFF2E2B27))
                        .border(
                            1.2.dp,
                            if (currentToolMode == CanvasToolMode.HIGHLIGHTER) Color(0xFFE5A96A) else Color(0xFF47433E),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectTool(CanvasToolMode.HIGHLIGHTER) },
                    contentAlignment = Alignment.Center
                ) {
                    ToolIcon(
                        tool = CanvasToolMode.HIGHLIGHTER,
                        tintColor = if (currentToolMode == CanvasToolMode.HIGHLIGHTER) Color(currentColor) else Color(0xFFFACC15),
                        size = 20.dp
                    )
                }

                // 4. ERASER Tool
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (currentToolMode == CanvasToolMode.STROKE_ERASER || currentToolMode == CanvasToolMode.PIXEL_ERASER) Color(0xFFEF4444) else Color(0xFF2E2B27))
                        .border(
                            1.2.dp,
                            if (currentToolMode == CanvasToolMode.STROKE_ERASER || currentToolMode == CanvasToolMode.PIXEL_ERASER) Color(0xFFFCA5A5) else Color(0xFF47433E),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectTool(CanvasToolMode.STROKE_ERASER) },
                    contentAlignment = Alignment.Center
                ) {
                    ToolIcon(
                        tool = CanvasToolMode.STROKE_ERASER,
                        tintColor = Color(0xFF38BDF8),
                        size = 20.dp
                    )
                }

                HorizontalDivider(
                    modifier = Modifier
                        .height(24.dp)
                        .width(1.dp),
                    color = Color.White.copy(alpha = 0.2f)
                )

                // Thickness Pill Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isThicknessMenuOpen) Color(0xFFC88A4B).copy(alpha = 0.25f) else Color(0xFF2E2B27),
                    border = BorderStroke(
                        1.2.dp,
                        if (isThicknessMenuOpen) Color(0xFFC88A4B) else Color(0xFF47433E)
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { isThicknessMenuOpen = !isThicknessMenuOpen }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val isEraser = currentToolMode == CanvasToolMode.STROKE_ERASER || currentToolMode == CanvasToolMode.PIXEL_ERASER
                        val dotColor = if (isEraser) Color(0xFF38BDF8) else Color(currentColor)

                        Box(
                            modifier = Modifier
                                .size(maxOf(6.dp, minOf(16.dp, (currentThickness * 0.4f).dp)))
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Text(
                            text = "${currentThickness.toInt()}px",
                            color = Color(0xFFFAF8F5),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Color Swatches (Shown when not in Eraser / Select mode)
                if (currentToolMode != CanvasToolMode.STROKE_ERASER &&
                    currentToolMode != CanvasToolMode.PIXEL_ERASER &&
                    currentToolMode != CanvasToolMode.SELECT
                ) {
                    HorizontalDivider(
                        modifier = Modifier
                            .height(24.dp)
                            .width(1.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    )

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
                }

                HorizontalDivider(
                    modifier = Modifier
                        .height(24.dp)
                        .width(1.dp),
                    color = Color.White.copy(alpha = 0.2f)
                )

                // Undo & Redo Actions
                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (canUndo) Color(0xFFFAF8F5) else Color.White.copy(alpha = 0.25f)
                    )
                }

                IconButton(
                    onClick = onRedo,
                    enabled = canRedo,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (canRedo) Color(0xFFFAF8F5) else Color.White.copy(alpha = 0.25f)
                    )
                }
            }
        }
    }
}

/**
 * Resolves or synthesizes document.pdf from current pages or legacy imported png files.
 */
private fun resolvePdfDocumentFile(context: Context, pages: List<Page>): File? {
    if (pages.isEmpty()) return null
    val firstRef = pages.firstOrNull()?.background?.pdfSourceRef
    val notebookId = pages.firstOrNull()?.notebookId

    if (!firstRef.isNullOrBlank()) {
        val target = File(firstRef)
        if (target.exists() && target.extension.equals("pdf", ignoreCase = true) && target.length() > 0) {
            return target
        }

        val relTarget = File(context.filesDir, firstRef)
        if (relTarget.exists() && relTarget.extension.equals("pdf", ignoreCase = true) && relTarget.length() > 0) {
            return relTarget
        }

        if (firstRef.contains("pdf_imports/")) {
            val relSub = firstRef.substringAfter("pdf_imports/")
            val resolvedInFiles = File(context.filesDir, "pdf_imports/$relSub")
            if (resolvedInFiles.exists() && resolvedInFiles.length() > 0) {
                return resolvedInFiles
            }
        }

        val parentDir = if (target.isDirectory) target else target.parentFile
        if (parentDir != null && parentDir.exists()) {
            val docPdf = File(parentDir, "document.pdf")
            if (docPdf.exists() && docPdf.length() > 0) {
                return docPdf
            }
            val anyPdf = parentDir.listFiles { f -> f.extension.equals("pdf", ignoreCase = true) }?.firstOrNull()
            if (anyPdf != null && anyPdf.length() > 0) {
                return anyPdf
            }

            // Check if there are PNG pages in parentDir (from older legacy imports)
            val pngFiles = parentDir.listFiles { f -> f.extension.equals("png", ignoreCase = true) }
                ?.sortedBy { f ->
                    val num = f.nameWithoutExtension.filter { it.isDigit() }.toIntOrNull() ?: 0
                    num
                }

            if (!pngFiles.isNullOrEmpty()) {
                try {
                    val pdfDoc = android.graphics.pdf.PdfDocument()
                    for ((index, pngFile) in pngFiles.withIndex()) {
                        val bitmap = android.graphics.BitmapFactory.decodeFile(pngFile.absolutePath)
                        if (bitmap != null) {
                            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                            val pdfPage = pdfDoc.startPage(pageInfo)
                            pdfPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                            pdfDoc.finishPage(pdfPage)
                            bitmap.recycle()
                        }
                    }
                    java.io.FileOutputStream(docPdf).use { out ->
                        pdfDoc.writeTo(out)
                        out.flush()
                    }
                    pdfDoc.close()
                    if (docPdf.exists() && docPdf.length() > 0) {
                        return docPdf
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PdfReaderScreen", "Failed to synthesize document.pdf from legacy pngs", e)
                }
            }
        }
    }

    if (!notebookId.isNullOrBlank()) {
        val standardDoc = File(context.filesDir, "pdf_imports/$notebookId/document.pdf")
        if (standardDoc.exists() && standardDoc.length() > 0) {
            return standardDoc
        }
        val nbFolder = File(context.filesDir, "pdf_imports/$notebookId")
        if (nbFolder.exists()) {
            val anyPdf = nbFolder.listFiles { f -> f.extension.equals("pdf", ignoreCase = true) }?.firstOrNull()
            if (anyPdf != null && anyPdf.length() > 0) {
                return anyPdf
            }
        }
    }

    return null
}
