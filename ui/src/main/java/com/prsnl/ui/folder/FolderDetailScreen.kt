package com.prsnl.ui.folder

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prsnl.document.model.Background
import com.prsnl.document.model.Notebook
import com.prsnl.pdf.PdfImporter
import com.prsnl.ui.home.HomeViewModel
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    folderName: String,
    viewModel: HomeViewModel,
    onBackClick: () -> Unit,
    onNotebookClick: (String) -> Unit
) {
    val allNotebooks by viewModel.notebooks.collectAsState()
    val allFolders by viewModel.folders.collectAsState()
    var showCreateNotebookDialog by remember { mutableStateOf(false) }
    var selectedNotebookForMenu by remember { mutableStateOf<Notebook?>(null) }
    val context = LocalContext.current

    val folderNotebooks = remember(allNotebooks, folderName) {
        allNotebooks.filter { it.folderName.equals(folderName, ignoreCase = true) }
    }

    // PDF Import Launcher
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val tempPdfFile = File(context.cacheDir, "import_${UUID.randomUUID()}.pdf")
                val inputStream = context.contentResolver.openInputStream(uri)
                val outputStream = FileOutputStream(tempPdfFile)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()

                val pdfImporter = PdfImporter(context)
                val result = pdfImporter.importPdfToNotebook(tempPdfFile)
                if (result != null) {
                    val (notebook, pages) = result
                    pages.forEach { p -> viewModel.createFolder(folderName) }
                    onNotebookClick(notebook.id)
                }
            } catch (e: Exception) {
                android.util.Log.e("FolderDetailScreen", "Failed to import PDF file", e)
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
                        Column {
                            Text(folderName, fontWeight = FontWeight.Bold, color = Color(0xFF2D2B28))
                            Text("${folderNotebooks.size} Notebooks", fontSize = 12.sp, color = Color(0xFF5C5850))
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF2D2B28))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFBF9F4))
                )
            },
            floatingActionButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FloatingActionButton(
                        onClick = { pdfPickerLauncher.launch("application/pdf") },
                        containerColor = Color(0xFF4C6EF5),
                        contentColor = Color.White,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Import PDF")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import PDF", fontWeight = FontWeight.Bold)
                        }
                    }

                    FloatingActionButton(
                        onClick = { showCreateNotebookDialog = true },
                        containerColor = Color(0xFFC88A4B),
                        contentColor = Color.White,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "New Notebook")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("New Notebook", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        ) { innerPadding ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val columnCount = if (maxWidth > 700.dp) 4 else 2

                AnimatedContent(
                    targetState = folderNotebooks,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "NotebookGridTransition"
                ) { notebooksToRender ->
                    if (notebooksToRender.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Create,
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp),
                                    tint = Color(0xFF8E887E)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No notebooks in '$folderName'.\nTap '+ New Notebook' or 'Import PDF'!",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color(0xFF5C5850)
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columnCount),
                            contentPadding = PaddingValues(24.dp),
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            items(notebooksToRender, key = { it.id }) { notebook ->
                                NotebookItemCard(
                                    notebook = notebook,
                                    onClick = { onNotebookClick(notebook.id) },
                                    onLongClick = { selectedNotebookForMenu = notebook }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showCreateNotebookDialog) {
            CreateNotebookFullModal(
                folderName = folderName,
                onDismiss = { showCreateNotebookDialog = false },
                onCreate = { title, coverColor, bgType, paperColor ->
                    viewModel.createNotebook(
                        title = title,
                        folderName = folderName,
                        coverColor = coverColor,
                        backgroundType = bgType,
                        paperColor = paperColor,
                        onCreated = { nbId, _ -> onNotebookClick(nbId) }
                    )
                    showCreateNotebookDialog = false
                }
            )
        }

        if (selectedNotebookForMenu != null) {
            NotebookContextMenuModal(
                notebook = selectedNotebookForMenu!!,
                availableFolders = allFolders.map { it.name }.distinct(),
                onDismiss = { selectedNotebookForMenu = null },
                onUpdate = { newTitle, newColor, newFolder ->
                    viewModel.updateNotebook(selectedNotebookForMenu!!, newTitle, newColor, newFolder)
                    selectedNotebookForMenu = null
                },
                onDelete = {
                    viewModel.deleteNotebook(selectedNotebookForMenu!!.id)
                    selectedNotebookForMenu = null
                }
            )
        }
    }
}

@Composable
fun NotebookItemCard(
    notebook: Notebook,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val coverColor = Color(notebook.coverColor)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F0E6)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(coverColor, coverColor.copy(alpha = 0.85f))
                        )
                    )
            ) {
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .fillMaxHeight()
                        .background(Color(0x33000000))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x22FFFFFF))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = notebook.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = notebook.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D2B28)
                    )
                    Text(
                        text = "Hold for options",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF8E887E)
                    )
                }
            }
        }
    }
}

@Composable
fun NotebookContextMenuModal(
    notebook: Notebook,
    availableFolders: List<String>,
    onDismiss: () -> Unit,
    onUpdate: (newTitle: String, newColor: Int, newFolder: String) -> Unit,
    onDelete: () -> Unit
) {
    var title by remember { mutableStateOf(notebook.title) }
    var selectedColor by remember { mutableIntStateOf(notebook.coverColor) }
    var selectedFolder by remember { mutableStateOf(notebook.folderName) }

    val colors = listOf(
        0xFF8B5E3C.toInt(),
        0xFFC88A4B.toInt(),
        0xFF4C6EF5.toInt(),
        0xFF4A7C59.toInt(),
        0xFFC85A32.toInt()
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFF5F0E6),
        titleContentColor = Color(0xFF2D2B28),
        textContentColor = Color(0xFF2D2B28),
        title = { Text("Edit Notebook: '${notebook.title}'", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Notebook Name", color = Color(0xFF5C5850)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFC88A4B),
                        unfocusedBorderColor = Color(0xFFE2D7C5),
                        focusedTextColor = Color(0xFF2D2B28),
                        unfocusedTextColor = Color(0xFF2D2B28)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))
                Text("Cover Theme Color", fontSize = 12.sp, color = Color(0xFF5C5850))
                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    colors.forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(c))
                                .border(
                                    width = if (selectedColor == c) 2.5.dp else 0.dp,
                                    color = Color(0xFFC88A4B),
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = c }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("Folder Location", fontSize = 12.sp, color = Color(0xFF5C5850))
                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val allF = (listOf(notebook.folderName) + availableFolders).distinct()
                    allF.take(4).forEach { fName ->
                        FilterChip(
                            selected = selectedFolder == fName,
                            onClick = { selectedFolder = fName },
                            label = { Text(fName, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDelete) {
                    Text("Delete Notebook", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = { onUpdate(title, selectedColor, selectedFolder) }) {
                    Text("Save Changes", color = Color(0xFFC88A4B), fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF5C5850))
            }
        }
    )
}

@Composable
fun CreateNotebookFullModal(
    folderName: String,
    onDismiss: () -> Unit,
    onCreate: (title: String, coverColor: Int, bgType: Background.Type, paperColor: Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedCoverColor by remember { mutableIntStateOf(0xFF8B5E3C.toInt()) }
    var selectedPaperType by remember { mutableStateOf(Background.Type.MARGIN_RULED) }
    var selectedPaperColor by remember { mutableIntStateOf(0xFFFAF8F5.toInt()) }

    val coverColors = listOf(
        0xFF8B5E3C.toInt(),
        0xFFC88A4B.toInt(),
        0xFF4C6EF5.toInt(),
        0xFF4A7C59.toInt(),
        0xFFC85A32.toInt()
    )

    val paperColors = listOf(
        0xFFFAF8F5.toInt(),
        0xFFFFFDF0.toInt(),
        0xFFF0F7F4.toInt(),
        0xFFFDF2F4.toInt(),
        0xFFF5F3FF.toInt(),
        0xFFFFFFFF.toInt()
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFF5F0E6),
        titleContentColor = Color(0xFF2D2B28),
        textContentColor = Color(0xFF2D2B28),
        title = { Text("Create Notebook in '$folderName'", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Notebook Name (e.g. Maths, Microeconomics)", color = Color(0xFF5C5850)) },
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
                Text("Cover Theme Color", fontSize = 11.sp, color = Color(0xFF5C5850), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    coverColors.forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(c))
                                .border(if (selectedCoverColor == c) 2.5.dp else 0.dp, Color(0xFFC88A4B), CircleShape)
                                .clickable { selectedCoverColor = c }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Paper Template Format", fontSize = 11.sp, color = Color(0xFF5C5850), fontWeight = FontWeight.Bold)
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
                    FilterChip(
                        selected = selectedPaperType == Background.Type.ISOMETRIC,
                        onClick = { selectedPaperType = Background.Type.ISOMETRIC },
                        label = { Text("3D Grid", fontSize = 10.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Paper Background Color", fontSize = 11.sp, color = Color(0xFF5C5850), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    paperColors.forEach { pc ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(pc))
                                .border(if (selectedPaperColor == pc) 2.5.dp else 1.dp, Color(0xFFC88A4B), CircleShape)
                                .clickable { selectedPaperColor = pc }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) onCreate(title, selectedCoverColor, selectedPaperType, selectedPaperColor)
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
