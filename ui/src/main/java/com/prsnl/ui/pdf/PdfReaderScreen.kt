package com.prsnl.ui.pdf

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.prsnl.document.model.Page
import com.prsnl.drawing.view.CanvasToolMode
import com.prsnl.drawing.view.DrawingCanvasView
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
    var selectedWidth by remember { mutableFloatStateOf(6f) }
    var isFingerDrawingEnabled by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

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
                                    val success = exporter.exportPagesToPdf(pages, exportFile)
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
                            // High-performance continuous PDF View
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
                                            .load()
                                    }
                                },
                                update = { pdfView ->
                                    if (pdfView.currentPage != activeIndex && activeIndex in 0 until pdfView.pageCount) {
                                        pdfView.jumpTo(activeIndex)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // Fallback if file could not be resolved
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Text(
                                    text = "PDF file could not be loaded",
                                    color = Color(0xFFFAF8F5),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Please re-import the document or select another notebook.",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Transparent ink & annotation canvas layer positioned over the PDF
                        if (activePage != null) {
                            AndroidView(
                                factory = { ctx ->
                                    DrawingCanvasView(ctx).apply {
                                        this.pageIndex = activeIndex
                                        this.documentWidth = activePage.width
                                        this.documentHeight = activePage.height
                                        this.currentBackground = activePage.background
                                        this.currentToolMode = currentTool
                                        this.currentColor = selectedColor
                                        this.currentBaseWidth = selectedWidth
                                        this.isFingerDrawingEnabled = isFingerDrawingEnabled
                                        this.passThroughAllTouches = (currentTool == CanvasToolMode.SELECT)
                                        this.committedElements = activePage.elements
                                        this.onCommandIssued = { cmd ->
                                            viewModel.executeCommand(activeIndex, cmd)
                                        }
                                    }
                                },
                                update = { view ->
                                    view.pageIndex = activeIndex
                                    view.documentWidth = activePage.width
                                    view.documentHeight = activePage.height
                                    view.currentBackground = activePage.background
                                    view.currentToolMode = currentTool
                                    view.currentColor = selectedColor
                                    view.currentBaseWidth = selectedWidth
                                    view.isFingerDrawingEnabled = isFingerDrawingEnabled
                                    view.passThroughAllTouches = (currentTool == CanvasToolMode.SELECT)
                                    view.committedElements = activePage.elements
                                    view.onCommandIssued = { cmd ->
                                        viewModel.executeCommand(activeIndex, cmd)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // Floating Quick Annotation Toolbar at bottom, styled to app theme
            PdfPenTrayToolbar(
                currentToolMode = currentTool,
                currentColor = selectedColor,
                canUndo = canUndo,
                canRedo = canRedo,
                onSelectTool = { tool -> viewModel.setToolMode(tool) },
                onSelectColor = { color -> selectedColor = color },
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
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
    onSelectTool: (CanvasToolMode) -> Unit,
    onSelectColor: (Int) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF242220).copy(alpha = 0.96f),
        tonalElevation = 12.dp,
        modifier = modifier
            .border(1.5.dp, Color(0xFF47433E), RoundedCornerShape(28.dp))
            .padding(horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val activeColor = Color(currentColor)

            // Read / Pan Mode (SELECT)
            IconButton(
                onClick = { onSelectTool(CanvasToolMode.SELECT) },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (currentToolMode == CanvasToolMode.SELECT) Color(0xFFC88A4B) else Color.Transparent)
            ) {
                ToolIcon(tool = CanvasToolMode.SELECT, tintColor = if (currentToolMode == CanvasToolMode.SELECT) Color.White else Color(0xFFFAF8F5), size = 18.dp)
            }

            // Pen Tool
            IconButton(
                onClick = { onSelectTool(CanvasToolMode.PEN) },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (currentToolMode == CanvasToolMode.PEN) Color(0xFFC88A4B) else Color.Transparent)
            ) {
                ToolIcon(tool = CanvasToolMode.PEN, tintColor = if (currentToolMode == CanvasToolMode.PEN) Color.White else Color(0xFFFAF8F5), size = 18.dp)
            }

            // Highlighter Tool
            IconButton(
                onClick = { onSelectTool(CanvasToolMode.HIGHLIGHTER) },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (currentToolMode == CanvasToolMode.HIGHLIGHTER) Color(0xFFC88A4B) else Color.Transparent)
            ) {
                ToolIcon(tool = CanvasToolMode.HIGHLIGHTER, tintColor = if (currentToolMode == CanvasToolMode.HIGHLIGHTER) Color.White else Color(0xFFFAF8F5), size = 18.dp)
            }

            // Eraser Tool
            IconButton(
                onClick = { onSelectTool(CanvasToolMode.STROKE_ERASER) },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (currentToolMode == CanvasToolMode.STROKE_ERASER || currentToolMode == CanvasToolMode.PIXEL_ERASER) Color(0xFFEF4444) else Color.Transparent)
            ) {
                ToolIcon(tool = CanvasToolMode.STROKE_ERASER, tintColor = Color.White, size = 18.dp)
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
                    tint = if (canUndo) Color(0xFFFAF8F5) else Color.White.copy(alpha = 0.3f)
                )
            }

            IconButton(onClick = onRedo, enabled = canRedo) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Redo",
                    tint = if (canRedo) Color(0xFFFAF8F5) else Color.White.copy(alpha = 0.3f)
                )
            }
        }
    }
}

/**
 * Resolves or synthesizes document.pdf from current pages or legacy imported png files.
 */
private fun resolvePdfDocumentFile(context: Context, pages: List<Page>): File? {
    if (pages.isEmpty()) return null
    val firstRef = pages.firstOrNull()?.background?.pdfSourceRef ?: return null
    val target = File(firstRef)
    if (target.exists() && target.extension.equals("pdf", ignoreCase = true) && target.length() > 0) {
        return target
    }

    val parentDir = if (target.isDirectory) target else target.parentFile ?: return null
    val docPdf = File(parentDir, "document.pdf")
    if (docPdf.exists() && docPdf.length() > 0) {
        return docPdf
    }

    // Check if there are PNG pages in parentDir (from older imports)
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

    return if (target.exists()) target else null
}
