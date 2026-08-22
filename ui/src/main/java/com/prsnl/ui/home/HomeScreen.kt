package com.prsnl.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prsnl.document.model.Folder
import com.prsnl.pdf.PdfImporter
import com.prsnl.ui.R
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onFolderClick: (String) -> Unit
) {
    val notebooks by viewModel.notebooks.collectAsState()
    val dbFolders by viewModel.folders.collectAsState()
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var selectedFolderForMenu by remember { mutableStateOf<Folder?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    var crashLogText by remember { mutableStateOf(com.prsnl.core.log.CrashLogger.getLatestCrashLog(context)) }
    var showCrashLogModal by remember { mutableStateOf(false) }

    val displayFolders = remember(dbFolders) {
        if (dbFolders.isEmpty()) {
            listOf(
                Folder("f1", "Finance", color = 0xFF8B5E3C.toInt()),
                Folder("f2", "Personal", color = 0xFFC85A32.toInt()),
                Folder("f3", "Work", color = 0xFF4A7C59.toInt())
            )
        } else dbFolders
    }

    val filteredFolders = remember(displayFolders, searchQuery) {
        if (searchQuery.isBlank()) displayFolders
        else displayFolders.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFFBF9F4)
    ) {
        Scaffold(
            modifier = Modifier.statusBarsPadding(),
            containerColor = Color.Transparent,
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFBF9F4))
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF5F0E6))
                                    .border(1.5.dp, Color(0xFFC88A4B), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_logo_vector),
                                    contentDescription = "prsnl Logo",
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "prsnl",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 24.sp,
                                    color = Color(0xFF2D2B28)
                                )
                                Text(
                                    text = "Folders & Document Storage",
                                    fontSize = 12.sp,
                                    color = Color(0xFF5C5850)
                                )
                            }
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
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search folders (e.g. Finance)...", color = Color(0xFF8E887E)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFC88A4B)) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF5F0E6),
                            unfocusedContainerColor = Color(0xFFF5F0E6),
                            focusedBorderColor = Color(0xFFC88A4B),
                            unfocusedBorderColor = Color(0xFFE2D7C5),
                            focusedTextColor = Color(0xFF2D2B28),
                            unfocusedTextColor = Color(0xFF2D2B28)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showCreateFolderDialog = true },
                    containerColor = Color(0xFFC88A4B),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New Folder")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("New Folder", fontWeight = FontWeight.Bold)
                    }
                }
            }
        ) { innerPadding ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val columnCount = if (maxWidth > 700.dp) 3 else 2

                AnimatedContent(
                    targetState = filteredFolders,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "FolderGridTransition"
                ) { foldersToRender ->
                    if (foldersToRender.isEmpty()) {
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
                                    text = "No folders found.\nTap '+ New Folder' to create one!",
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
                            items(foldersToRender, key = { it.id }) { folder ->
                                val notebookCount = notebooks.count { it.folderName.equals(folder.name, ignoreCase = true) }
                                FolderCard(
                                    folder = folder,
                                    notebookCount = notebookCount,
                                    onClick = { onFolderClick(folder.name) },
                                    onLongClick = { selectedFolderForMenu = folder }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showCreateFolderDialog) {
            CreateFolderDialog(
                onDismiss = { showCreateFolderDialog = false },
                onCreate = { folderName ->
                    viewModel.createFolder(folderName)
                    showCreateFolderDialog = false
                    onFolderClick(folderName)
                }
            )
        }

        if (selectedFolderForMenu != null) {
            FolderContextMenuModal(
                folder = selectedFolderForMenu!!,
                onDismiss = { selectedFolderForMenu = null },
                onUpdate = { newName, newColor ->
                    viewModel.updateFolder(selectedFolderForMenu!!.id, selectedFolderForMenu!!.name, newName, newColor)
                    selectedFolderForMenu = null
                },
                onDelete = {
                    viewModel.deleteFolder(selectedFolderForMenu!!.id, selectedFolderForMenu!!.name)
                    selectedFolderForMenu = null
                }
            )
        }

        if (showCrashLogModal && crashLogText != null) {
            com.prsnl.ui.common.CrashLogViewerModal(
                crashLogText = crashLogText!!,
                onDismiss = { showCrashLogModal = false }
            )
        }
    }
}

@Composable
fun FolderCard(
    folder: Folder,
    notebookCount: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val themeColor = Color(folder.color)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F0E6)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(themeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Create,
                        contentDescription = "Folder",
                        tint = themeColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "$notebookCount ${if (notebookCount == 1) "Notebook" else "Notebooks"}",
                    fontSize = 12.sp,
                    color = Color(0xFF5C5850),
                    fontWeight = FontWeight.Bold
                )
            }

            Column {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D2B28)
                )
                Text(
                    text = "Long-press for options",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF8E887E)
                )
            }
        }
    }
}

@Composable
fun FolderContextMenuModal(
    folder: Folder,
    onDismiss: () -> Unit,
    onUpdate: (newName: String, newColor: Int) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(folder.name) }
    var selectedColor by remember { mutableIntStateOf(folder.color) }

    val colors = listOf(
        0xFF8B5E3C.toInt(),
        0xFFC85A32.toInt(),
        0xFF4A7C59.toInt(),
        0xFFC88A4B.toInt(),
        0xFF4C6EF5.toInt()
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFF5F0E6),
        titleContentColor = Color(0xFF2D2B28),
        textContentColor = Color(0xFF2D2B28),
        title = { Text("Edit Folder: '${folder.name}'", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Folder Name", color = Color(0xFF5C5850)) },
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
                Text("Folder Accent Color", fontSize = 12.sp, color = Color(0xFF5C5850))
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
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDelete) {
                    Text("Delete Folder", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = { onUpdate(name, selectedColor) }) {
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
fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var folderName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFF5F0E6),
        titleContentColor = Color(0xFF2D2B28),
        textContentColor = Color(0xFF2D2B28),
        title = { Text("Create New Folder", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = folderName,
                onValueChange = { folderName = it },
                label = { Text("Folder Name (e.g. Finance, Maths)", color = Color(0xFF5C5850)) },
                singleLine = true,
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
                    if (folderName.isNotBlank()) onCreate(folderName)
                }
            ) {
                Text("Create Folder", color = Color(0xFFC88A4B), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF5C5850))
            }
        }
    )
}
