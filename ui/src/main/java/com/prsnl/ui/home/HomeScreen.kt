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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import com.prsnl.ui.R
import com.prsnl.ui.common.AppToastBanner
import com.prsnl.ui.common.ToastMessage
import com.prsnl.ui.common.ToastType
import com.prsnl.ui.folder.TOPIC_ICON_OPTIONS
import com.prsnl.ui.security.PinKeypadModal
import com.prsnl.ui.security.PinModalMode
import com.prsnl.ui.settings.AppSettingsModal

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
    var showSettingsModal by remember { mutableStateOf(false) }

    // Toast state
    var activeToast by remember { mutableStateOf<ToastMessage?>(null) }

    // PIN & Lock States
    var folderToUnlock by remember { mutableStateOf<Folder?>(null) }
    var folderToLockSetup by remember { mutableStateOf<Folder?>(null) }
    var folderToResetPin by remember { mutableStateOf<Folder?>(null) }
    var pendingCreationLockFolder by remember { mutableStateOf<Folder?>(null) }

    val displayFolders = remember(dbFolders) {
        if (dbFolders.isEmpty()) {
            listOf(
                Folder("f1", "Finance", color = 0xFF8B5E3C.toInt(), iconName = "FINANCE"),
                Folder("f2", "Personal", color = 0xFFC85A32.toInt(), iconName = "PERSONAL"),
                Folder("f3", "Work", color = 0xFF4A7C59.toInt(), iconName = "WORK")
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
                                    .clip(RoundedCornerShape(13.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_logo_vector),
                                    contentDescription = "prsnl Logo",
                                    modifier = Modifier.size(46.dp)
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

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { showSettingsModal = true }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = Color(0xFFC88A4B)
                                )
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
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search folders...", color = Color(0xFF8E887E)) },
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

                Box(modifier = Modifier.fillMaxSize()) {
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
                                        onClick = {
                                            if (folder.isLocked) {
                                                folderToUnlock = folder
                                            } else {
                                                onFolderClick(folder.name)
                                            }
                                        },
                                        onLongClick = { selectedFolderForMenu = folder }
                                    )
                                }
                            }
                        }
                    }

                    // In-App Toast Banner Overlay
                    AppToastBanner(
                        toast = activeToast,
                        onDismiss = { activeToast = null },
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }
        }

        // Modals & Dialogs
        if (showCreateFolderDialog) {
            CreateFolderDialog(
                onDismiss = { showCreateFolderDialog = false },
                onCreate = { folderName, folderColor, iconName, shouldLock ->
                    val createdFolder = viewModel.createFolder(folderName, folderColor, iconName)
                    showCreateFolderDialog = false
                    if (shouldLock) {
                        pendingCreationLockFolder = createdFolder
                    } else {
                        activeToast = ToastMessage("Folder created successfully", ToastType.SUCCESS)
                        onFolderClick(createdFolder.name)
                    }
                }
            )
        }

        if (pendingCreationLockFolder != null) {
            val target = pendingCreationLockFolder!!
            PinKeypadModal(
                mode = PinModalMode.SETUP_PIN,
                folderName = target.name,
                onSuccessPinSet = { newPin, question, answerHash ->
                    viewModel.updateFolderLock(target.id, true, newPin, question, answerHash)
                    pendingCreationLockFolder = null
                    activeToast = ToastMessage("Folder created and locked with PIN", ToastType.SUCCESS)
                    onFolderClick(target.name)
                },
                onSuccessUnlocked = {},
                onDismiss = {
                    pendingCreationLockFolder = null
                    activeToast = ToastMessage("Folder created", ToastType.SUCCESS)
                    onFolderClick(target.name)
                }
            )
        }

        if (selectedFolderForMenu != null) {
            val targetFolder = selectedFolderForMenu!!
            FolderContextMenuModal(
                folder = targetFolder,
                onDismiss = { selectedFolderForMenu = null },
                onUpdate = { newName, newColor, newIconName ->
                    viewModel.updateFolder(targetFolder.id, targetFolder.name, newName, newColor, newIconName)
                    selectedFolderForMenu = null
                    activeToast = ToastMessage("Folder updated successfully", ToastType.SUCCESS)
                },
                onDelete = {
                    viewModel.deleteFolder(targetFolder.id, targetFolder.name)
                    selectedFolderForMenu = null
                    activeToast = ToastMessage("Folder deleted", ToastType.INFO)
                },
                onLockToggle = {
                    selectedFolderForMenu = null
                    if (targetFolder.isLocked) {
                        viewModel.updateFolderLock(targetFolder.id, false)
                        activeToast = ToastMessage("Folder unlocked", ToastType.SUCCESS)
                    } else {
                        folderToLockSetup = targetFolder
                    }
                }
            )
        }

        // PIN Keypad Modals
        if (folderToUnlock != null) {
            val target = folderToUnlock!!
            PinKeypadModal(
                mode = PinModalMode.UNLOCK,
                folderName = target.name,
                existingPin = target.pin,
                existingQuestion = target.securityQuestion,
                existingAnswerHash = target.securityAnswerHash,
                onSuccessPinSet = { _, _, _ -> },
                onSuccessUnlocked = {
                    folderToUnlock = null
                    activeToast = ToastMessage("Folder unlocked", ToastType.SUCCESS)
                    onFolderClick(target.name)
                },
                onDismiss = { folderToUnlock = null }
            )
        }

        if (folderToLockSetup != null) {
            val target = folderToLockSetup!!
            PinKeypadModal(
                mode = PinModalMode.SETUP_PIN,
                folderName = target.name,
                onSuccessPinSet = { newPin, question, answerHash ->
                    viewModel.updateFolderLock(target.id, true, newPin, question, answerHash)
                    folderToLockSetup = null
                    activeToast = ToastMessage("Folder locked with PIN", ToastType.SUCCESS)
                },
                onSuccessUnlocked = {},
                onDismiss = { folderToLockSetup = null }
            )
        }

        if (folderToResetPin != null) {
            val target = folderToResetPin!!
            PinKeypadModal(
                mode = PinModalMode.RESET_PIN,
                folderName = target.name,
                existingPin = target.pin,
                existingQuestion = target.securityQuestion,
                existingAnswerHash = target.securityAnswerHash,
                onSuccessPinSet = { newPin, question, answerHash ->
                    viewModel.updateFolderLock(target.id, true, newPin, question, answerHash)
                    folderToResetPin = null
                    activeToast = ToastMessage("PIN reset successfully", ToastType.SUCCESS)
                },
                onSuccessUnlocked = {},
                onDismiss = { folderToResetPin = null }
            )
        }

        if (showSettingsModal) {
            AppSettingsModal(
                lockedFolders = displayFolders.filter { it.isLocked },
                onResetFolderPin = { folder ->
                    showSettingsModal = false
                    folderToResetPin = folder
                },
                onUnlockFolder = { folder ->
                    viewModel.updateFolderLock(folder.id, false)
                    activeToast = ToastMessage("Folder unlocked", ToastType.SUCCESS)
                },
                onDismiss = { showSettingsModal = false }
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F0E6))
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
                        imageVector = if (folder.isLocked) Icons.Default.Lock else Icons.Default.Create,
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D2B28)
                    )
                    if (folder.isLocked) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = Color(0xFFC88A4B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
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
    onUpdate: (newName: String, newColor: Int, newIconName: String) -> Unit,
    onDelete: () -> Unit,
    onLockToggle: () -> Unit
) {
    var name by remember { mutableStateOf(folder.name) }
    var selectedColor by remember { mutableIntStateOf(folder.color) }
    var selectedIcon by remember { mutableStateOf(folder.iconName) }

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

                Spacer(modifier = Modifier.height(12.dp))
                Text("Folder Topic / Icon", fontSize = 11.sp, color = Color(0xFF5C5850), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TOPIC_ICON_OPTIONS.take(4).forEach { (iconKey, labelText) ->
                        FilterChip(
                            selected = selectedIcon == iconKey,
                            onClick = { selectedIcon = iconKey },
                            label = { Text(labelText, fontSize = 10.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Folder Accent Color", fontSize = 11.sp, color = Color(0xFF5C5850), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))

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

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFAF8F5))
                        .border(1.dp, Color(0xFFE2D7C5), RoundedCornerShape(10.dp))
                        .clickable { onLockToggle() }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFC88A4B),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (folder.isLocked) "Unlock Folder (Remove PIN)" else "Lock Folder with PIN",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2D2B28)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDelete) {
                    Text("Delete Folder", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = { onUpdate(name, selectedColor, selectedIcon) }) {
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
    onCreate: (name: String, color: Int, iconName: String, shouldLock: Boolean) -> Unit
) {
    var folderName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableIntStateOf(0xFF8B5E3C.toInt()) }
    var selectedIcon by remember { mutableStateOf("FOLDER") }
    var shouldLock by remember { mutableStateOf(false) }

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
        title = { Text("Create New Folder", fontWeight = FontWeight.Bold) },
        text = {
            Column {
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

                Spacer(modifier = Modifier.height(10.dp))
                Text("Folder Topic / Icon", fontSize = 11.sp, color = Color(0xFF5C5850), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TOPIC_ICON_OPTIONS.take(4).forEach { (iconKey, labelText) ->
                        FilterChip(
                            selected = selectedIcon == iconKey,
                            onClick = { selectedIcon = iconKey },
                            label = { Text(labelText, fontSize = 10.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Folder Accent Color", fontSize = 11.sp, color = Color(0xFF5C5850), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colors.forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(c))
                                .border(if (selectedColor == c) 2.5.dp else 0.dp, Color(0xFFC88A4B), CircleShape)
                                .clickable { selectedColor = c }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = shouldLock,
                        onCheckedChange = { shouldLock = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFFC88A4B))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("Lock Folder with PIN", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D2B28))
                        Text("You will be asked to set a 4-digit PIN & security question", fontSize = 10.sp, color = Color(0xFF5C5850))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (folderName.isNotBlank()) onCreate(folderName, selectedColor, selectedIcon, shouldLock)
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
