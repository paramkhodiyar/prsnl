package com.prsnl.ui.editor

import android.graphics.Color as AndroidColor
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.prsnl.document.model.Command
import com.prsnl.document.model.ImageElement
import com.prsnl.document.model.RectData
import com.prsnl.document.model.Shape
import com.prsnl.document.model.TextBox
import com.prsnl.drawing.view.CanvasToolMode
import com.prsnl.drawing.view.DrawingCanvasView
import com.prsnl.pdf.PdfExporter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

private fun defaultPdfFilename(title: String): String {
    val sanitized = title.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        .trim('_')
        .ifEmpty { "notebook" }
    return "${sanitized}.pdf"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageEditorScreen(
    viewModel: PageEditorViewModel,
    onBackClick: () -> Unit
) {
    val pagesList by viewModel.pagesList.collectAsState()
    val notebookTitle by viewModel.notebookTitle.collectAsState()
    val activeNotebook by viewModel.activeNotebook.collectAsState()
    val activePageIndex by viewModel.activePageIndex.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val toolMode by viewModel.toolMode.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val context = LocalContext.current

    var selectedColor by remember { mutableIntStateOf(AndroidColor.parseColor("#2D2B28")) }
    var selectedWidth by remember { mutableFloatStateOf(6f) }
    var eraserRadius by remember { mutableFloatStateOf(32f) }
    var selectedShapeType by remember { mutableStateOf(Shape.Type.RECTANGLE) }
    var isFingerDrawingEnabled by remember { mutableStateOf(false) }
    var isPressureSensitivityEnabled by remember { mutableStateOf(true) }
    var showSettingsModal by remember { mutableStateOf(false) }
    var showColorWheel by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportFilename by remember(notebookTitle) { mutableStateOf(defaultPdfFilename(notebookTitle)) }
    var hasCanvasSelection by remember { mutableStateOf(false) }
    val canvasViews = remember { mutableMapOf<Int, DrawingCanvasView>() }

    var editingTextBox by remember { mutableStateOf<TextBox?>(null) }
    var editTypedTextContent by remember { mutableStateOf("") }

    // Typer Text Box State
    var pendingTextInsertPos by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var typedTextContent by remember { mutableStateOf("") }

    val activePage = pagesList.getOrNull(activePageIndex) ?: pagesList.firstOrNull()

    val exportPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null && pagesList.isNotEmpty()) {
            val tempFile = File(context.cacheDir, "export_${UUID.randomUUID()}.pdf")
            val success = PdfExporter().exportPagesToPdf(pagesList, tempFile)
            if (success) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        FileInputStream(tempFile).use { input -> input.copyTo(output) }
                    }
                    Toast.makeText(context, "PDF exported successfully.", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    android.util.Log.e("PageEditorScreen", "Failed to write exported PDF", e)
                    Toast.makeText(context, "Could not save PDF to the selected location.", Toast.LENGTH_LONG).show()
                } finally {
                    tempFile.delete()
                }
            } else {
                Toast.makeText(context, "Export PDF failed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && activePage != null) {
            try {
                val imageDir = File(context.filesDir, "images")
                if (!imageDir.exists()) imageDir.mkdirs()

                val imageFile = File(imageDir, "img_${UUID.randomUUID()}.png")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(imageFile).use { output -> input.copyTo(output) }
                }

                val targetView = canvasViews[activePageIndex]
                val imgElement = ImageElement(
                    id = UUID.randomUUID().toString(),
                    zIndex = (activePage.elements.maxOfOrNull { it.zIndex } ?: -1) + 1,
                    boundingBox = RectData(100f, 100f, 500f, 500f),
                    createdAt = System.currentTimeMillis(),
                    assetPath = imageFile.absolutePath
                )
                viewModel.executeCommand(activePageIndex, Command.AddElement(imgElement))
                targetView?.invalidate()
            } catch (e: Exception) {
                android.util.Log.e("PageEditorScreen", "Failed to save image attachment", e)
                Toast.makeText(context, "Could not import that image.", Toast.LENGTH_LONG).show()
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFFBF9F4)
    ) {
        Scaffold(
            modifier = Modifier.statusBarsPadding(),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$notebookTitle • Page ${activePageIndex + 1} of ${pagesList.size}",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2D2B28),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (isSaving) {
                                Text(
                                    "  (Saving...)",
                                    fontSize = 12.sp,
                                    color = Color(0xFFC88A4B)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF2D2B28))
                        }
                    },
                    actions = {
                        var crashLogText by remember { mutableStateOf(com.prsnl.core.log.CrashLogger.getLatestCrashLog(context)) }
                        var showCrashLogModal by remember { mutableStateOf(false) }

                        // Export PDF Action
                        IconButton(
                            onClick = {
                                if (pagesList.isNotEmpty()) {
                                    exportFilename = defaultPdfFilename(notebookTitle)
                                    showExportDialog = true
                                }
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Export PDF", tint = Color(0xFF4C6EF5))
                        }

                        IconButton(
                            onClick = {
                                crashLogText = com.prsnl.core.log.CrashLogger.getLatestCrashLog(context) ?: "No crashes recorded. System running cleanly!"
                                showCrashLogModal = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "View Crash Logs",
                                tint = if (crashLogText != null) Color(0xFFDC2626) else Color(0xFFC88A4B)
                            )
                        }

                        if (showCrashLogModal && crashLogText != null) {
                            com.prsnl.ui.common.CrashLogViewerModal(
                                crashLogText = crashLogText!!,
                                onDismiss = { showCrashLogModal = false }
                            )
                        }

                        IconButton(onClick = { showSettingsModal = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Notebook Settings", tint = Color(0xFFC88A4B))
                        }
                        TextButton(onClick = { viewModel.undo() }, enabled = canUndo) {
                            Text("Undo", color = if (canUndo) Color(0xFFC88A4B) else Color(0xFF8E887E))
                        }
                        TextButton(onClick = { viewModel.redo() }, enabled = canRedo) {
                            Text("Redo", color = if (canRedo) Color(0xFFC88A4B) else Color(0xFF8E887E))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFBF9F4))
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { viewModel.addPage() },
                    containerColor = Color(0xFFC88A4B),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Page", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Page", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        ) { innerPadding ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFFFBF9F4))
            ) {
                val scrollState = rememberScrollState()
                var hasAutoScrolled by remember { mutableStateOf(false) }

                LaunchedEffect(activeNotebook, pagesList) {
                    val nb = activeNotebook
                    if (nb != null && !hasAutoScrolled && pagesList.isNotEmpty()) {
                        val targetIdx = nb.lastViewedPageIndex.coerceIn(0, pagesList.size - 1)
                        if (targetIdx > 0) {
                            scrollState.scrollTo(targetIdx * 1750)
                        }
                        hasAutoScrolled = true
                    }
                }

                if (pagesList.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        pagesList.forEachIndexed { index, singlePage ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                            ) {
                                // Page Separator Header Banner
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 10.dp)
                                ) {
                                    Box(modifier = Modifier.width(40.dp).height(1.5.dp).background(Color(0xFFC88A4B)))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "A4 Page ${index + 1} of ${pagesList.size}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF8B5E3C)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(modifier = Modifier.width(40.dp).height(1.5.dp).background(Color(0xFFC88A4B)))
                                }

                                Card(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .widthIn(max = 1000.dp)
                                        .aspectRatio(1f / 1.414f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(singlePage.background.colorLight)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                                ) {
                                    AndroidView(
                                        factory = { context ->
                                            DrawingCanvasView(context).apply {
                                                this.currentColor = selectedColor
                                                this.currentBaseWidth = selectedWidth
                                                this.eraserRadius = eraserRadius
                                                this.currentToolMode = toolMode
                                                this.currentInsertedShapeType = selectedShapeType
                                                this.currentBackground = singlePage.background
                                                this.committedElements = singlePage.elements
                                                this.pageIndex = index
                                                this.documentWidth = singlePage.width
                                                this.documentHeight = singlePage.height
                                                this.isFingerDrawingEnabled = isFingerDrawingEnabled
                                                this.isPressureSensitivityEnabled = isPressureSensitivityEnabled
                                                this.onInteractionStarted = {
                                                    viewModel.setActivePageIndex(index)
                                                }
                                                this.onSelectionChanged = { hasSelection ->
                                                    if (hasSelection) {
                                                        viewModel.setActivePageIndex(index)
                                                    }
                                                    hasCanvasSelection = hasSelection
                                                }
                                                this.onCommandIssued = { command ->
                                                    viewModel.executeCommand(index, command)
                                                }
                                                this.onAutoStylusSwitch = {
                                                    val current = viewModel.toolMode.value
                                                    val isInkTool = current == CanvasToolMode.PEN ||
                                                        current == CanvasToolMode.PENCIL ||
                                                        current == CanvasToolMode.HIGHLIGHTER
                                                    if (!isInkTool) {
                                                        viewModel.setToolMode(CanvasToolMode.PEN)
                                                    }
                                                }
                                                this.onInsertTextBoxRequested = { x, y ->
                                                    pendingTextInsertPos = Pair(x, y)
                                                }
                                                this.onEditTextBoxRequested = { textBox ->
                                                    editingTextBox = textBox
                                                    editTypedTextContent = textBox.content
                                                }
                                                this.onToolModeAutoSwitchRequested = { newMode ->
                                                    viewModel.setToolMode(newMode)
                                                }
                                                canvasViews[index] = this
                                            }
                                        },
                                        update = { view ->
                                            view.currentColor = selectedColor
                                            view.currentBaseWidth = selectedWidth
                                            view.eraserRadius = eraserRadius
                                            view.currentToolMode = toolMode
                                            view.currentInsertedShapeType = selectedShapeType
                                            view.currentBackground = singlePage.background
                                            view.committedElements = singlePage.elements
                                            view.pageIndex = index
                                            view.documentWidth = singlePage.width
                                            view.documentHeight = singlePage.height
                                            view.isFingerDrawingEnabled = isFingerDrawingEnabled
                                            view.isPressureSensitivityEnabled = isPressureSensitivityEnabled
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Floating Control Bar
                FloatingWritingToolbar(
                    toolMode = toolMode,
                    onToolModeChange = { viewModel.setToolMode(it) },
                    selectedColor = selectedColor,
                    onColorSelect = { selectedColor = it },
                    selectedWidth = selectedWidth,
                    onWidthChange = { selectedWidth = it },
                    eraserRadius = eraserRadius,
                    onEraserRadiusChange = { eraserRadius = it },
                    selectedShapeType = selectedShapeType,
                    onShapeTypeSelect = { selectedShapeType = it },
                    isFingerDrawingEnabled = isFingerDrawingEnabled,
                    onFingerDrawingToggle = { isFingerDrawingEnabled = !isFingerDrawingEnabled },
                    isPressureSensitivityEnabled = isPressureSensitivityEnabled,
                    onPressureSensitivityToggle = { isPressureSensitivityEnabled = !isPressureSensitivityEnabled },
                    hasSelection = hasCanvasSelection,
                    onDeleteSelection = {
                        val activeView = canvasViews[activePageIndex]
                        activeView?.deleteSelectedElements()
                    },
                    onOpenColorWheel = { showColorWheel = true },
                    onOpenSettings = { showSettingsModal = true },
                    onInsertImage = { imagePickerLauncher.launch("image/*") }
                )
            }
        }

        // Export PDF Dialog
        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                containerColor = Color(0xFFF5F0E6),
                titleContentColor = Color(0xFF2D2B28),
                textContentColor = Color(0xFF2D2B28),
                title = { Text("Export Annotated PDF", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            text = "Export your handwritten notes, highlighters, and PDF annotations into a single high-resolution PDF document.",
                            fontSize = 13.sp,
                            color = Color(0xFF5C5850)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = exportFilename,
                            onValueChange = { exportFilename = it },
                            label = { Text("File Name", color = Color(0xFF5C5850)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFC88A4B),
                                unfocusedBorderColor = Color(0xFFE2D7C5),
                                focusedTextColor = Color(0xFF2D2B28),
                                unfocusedTextColor = Color(0xFF2D2B28)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showExportDialog = false
                            val safeName = exportFilename.trim().ifEmpty { defaultPdfFilename(notebookTitle) }
                            exportPdfLauncher.launch(safeName)
                        }
                    ) {
                        Text("Export PDF", color = Color(0xFF4C6EF5), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("Cancel", color = Color(0xFF5C5850))
                    }
                }
            )
        }

        // Custom Page Settings Modal
        if (showSettingsModal && activePage != null) {
            NotebookSettingsModal(
                currentBackground = activePage.background,
                isFingerDrawingEnabled = isFingerDrawingEnabled,
                isPressureSensitivityEnabled = isPressureSensitivityEnabled,
                onDismiss = { showSettingsModal = false },
                onBackgroundTypeChange = { viewModel.changeBackgroundType(it) },
                onLineSpacingChange = { viewModel.changeLineSpacing(it) },
                onLineWeightChange = { viewModel.changeLineWeight(it) },
                onLineOpacityChange = { viewModel.changeLineOpacity(it) },
                onLineColorChange = { viewModel.changeLineColor(it) },
                onMarginWeightChange = { viewModel.changeMarginWeight(it) },
                onPaperColorChange = { viewModel.changePaperColor(it) },
                onFingerDrawingToggle = { isFingerDrawingEnabled = !isFingerDrawingEnabled },
                onPressureSensitivityToggle = { isPressureSensitivityEnabled = !isPressureSensitivityEnabled }
            )
        }

        // Color Wheel Modal
        if (showColorWheel) {
            ColorWheelPickerModal(
                initialColor = selectedColor,
                onColorSelected = {
                    selectedColor = it
                    showColorWheel = false
                },
                onDismiss = { showColorWheel = false }
            )
        }

        // Insert Text Dialog
        if (pendingTextInsertPos != null && activePage != null) {
            val (posX, posY) = pendingTextInsertPos!!
            AlertDialog(
                onDismissRequest = { pendingTextInsertPos = null },
                containerColor = Color(0xFFF5F0E6),
                titleContentColor = Color(0xFF2D2B28),
                textContentColor = Color(0xFF2D2B28),
                title = { Text("Insert Text Box", fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = typedTextContent,
                        onValueChange = { typedTextContent = it },
                        label = { Text("Type annotation text...", color = Color(0xFF5C5850)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFC88A4B),
                            unfocusedBorderColor = Color(0xFFE2D7C5),
                            focusedTextColor = Color(0xFF2D2B28),
                            unfocusedTextColor = Color(0xFF2D2B28)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (typedTextContent.isNotBlank()) {
                                val targetView = canvasViews[activePageIndex]
                                val nextZ = (activePage.elements.maxOfOrNull { it.zIndex } ?: -1) + 1
                                val textBox = TextBox(
                                    id = UUID.randomUUID().toString(),
                                    zIndex = nextZ,
                                    boundingBox = RectData(posX, posY, posX + (typedTextContent.length * 16f).coerceAtLeast(120f), posY + 48f),
                                    createdAt = System.currentTimeMillis(),
                                    content = typedTextContent,
                                    fontSize = 28f,
                                    color = selectedColor
                                )
                                viewModel.executeCommand(activePageIndex, Command.AddElement(textBox))
                                targetView?.invalidate()
                            }
                            typedTextContent = ""
                            pendingTextInsertPos = null
                        }
                    ) {
                        Text("Add Text", color = Color(0xFFC88A4B), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            typedTextContent = ""
                            pendingTextInsertPos = null
                        }
                    ) {
                        Text("Cancel", color = Color(0xFF5C5850))
                    }
                }
            )
        }

        // Edit Text Dialog (Double-tap inline text editor)
        if (editingTextBox != null && activePage != null) {
            val targetBox = editingTextBox!!
            AlertDialog(
                onDismissRequest = { editingTextBox = null },
                containerColor = Color(0xFFF5F0E6),
                titleContentColor = Color(0xFF2D2B28),
                textContentColor = Color(0xFF2D2B28),
                title = { Text("Edit Text Annotation", fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = editTypedTextContent,
                        onValueChange = { editTypedTextContent = it },
                        label = { Text("Edit text...", color = Color(0xFF5C5850)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFC88A4B),
                            unfocusedBorderColor = Color(0xFFE2D7C5),
                            focusedTextColor = Color(0xFF2D2B28),
                            unfocusedTextColor = Color(0xFF2D2B28)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                viewModel.executeCommand(activePageIndex, Command.DeleteElement(targetBox))
                                editingTextBox = null
                                canvasViews[activePageIndex]?.invalidate()
                            }
                        ) {
                            Text("Delete Text", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                        }
                        TextButton(
                            onClick = {
                                if (editTypedTextContent.isNotBlank()) {
                                    val updatedBox = targetBox.copy(
                                        content = editTypedTextContent,
                                        boundingBox = targetBox.boundingBox.copy(
                                            right = targetBox.boundingBox.left + (editTypedTextContent.length * 16f).coerceAtLeast(120f)
                                        )
                                    )
                                    viewModel.executeCommand(
                                        activePageIndex,
                                        Command.ReplaceElement(targetBox, updatedBox)
                                    )
                                    canvasViews[activePageIndex]?.invalidate()
                                }
                                editingTextBox = null
                            }
                        ) {
                            Text("Save Text", color = Color(0xFFC88A4B), fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingTextBox = null }) {
                        Text("Cancel", color = Color(0xFF5C5850))
                    }
                }
            )
        }
    }
}
