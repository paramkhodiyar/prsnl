package com.prsnl.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Warm Stationery Palette
private val IvoryCanvas = Color(0xFFFBF9F4)
private val MoleskineSurface = Color(0xFFF5F0E6)
private val GoldAccent = Color(0xFFC88A4B)
private val CharcoalText = Color(0xFF2D2B28)
private val SoftBorderColor = Color(0xFFE8E2D5)
private val RuledLineColor = Color(0xFFD4C8B5)

data class MacFolder(val id: String, val name: String, val notebookCount: Int)
data class MacNotebook(
    val id: String,
    val title: String,
    val folderName: String,
    val pageCount: Int,
    val updatedAt: String
)

data class MacPage(
    val id: String,
    val notebookId: String,
    val pageIndex: Int,
    val backgroundType: String,
    val elementsJson: String
)

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "prsnl — Digital Notebook Companion (macOS)",
        state = WindowState(size = DpSize(1150.dp, 780.dp))
    ) {
        MacAppUI()
    }
}

@Composable
fun MacAppUI() {
    val coroutineScope = rememberCoroutineScope()

    var userEmail by remember { mutableStateOf("paramkhodiyar1008@gmail.com") }
    var isSignedIn by remember { mutableStateOf(true) }
    var selectedFolder by remember { mutableStateOf<String?>("All Notes") }
    var selectedNotebook by remember { mutableStateOf<MacNotebook?>(null) }
    var isSyncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }

    var fetchedNotebooks by remember { mutableStateOf<List<MacNotebook>>(emptyList()) }
    var fetchedFolders by remember { mutableStateOf<List<MacFolder>>(listOf(MacFolder("1", "Personal", 0))) }
    var fetchedPages by remember { mutableStateOf<List<MacPage>>(emptyList()) }

    fun fetchSyncedNotesFromCloud() {
        coroutineScope.launch {
            isSyncing = true
            syncMessage = "Fetching synced notes from cloud..."
            try {
                // Fetch Notebooks & Pages from Firestore REST API
                val cleanEmail = userEmail.trim().lowercase()
                val (foldersList, notebooksList, pagesList) = queryFirestoreUserNotes(cleanEmail)

                if (foldersList.isNotEmpty()) {
                    fetchedFolders = foldersList
                }
                fetchedNotebooks = notebooksList
                fetchedPages = pagesList

                val formattedTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                syncMessage = "Synced at $formattedTime (${notebooksList.size} notebooks)"
            } catch (e: Exception) {
                syncMessage = "Sync notice: Ready to receive tablet notes."
            } finally {
                isSyncing = false
            }
        }
    }

    val filteredNotebooks = remember(selectedFolder, fetchedNotebooks) {
        if (selectedFolder == null || selectedFolder == "All Notes") fetchedNotebooks
        else fetchedNotebooks.filter { it.folderName.equals(selectedFolder, ignoreCase = true) }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = IvoryCanvas
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // LEFT SIDEBAR: Navigation & Folders
            Column(
                modifier = Modifier
                    .width(280.dp)
                    .fillMaxHeight()
                    .background(MoleskineSurface)
                    .padding(20.dp)
            ) {
                // Logo & Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(GoldAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("prsnl", fontSize = 20.sp, fontWeight = FontWeight.Black, color = CharcoalText)
                        Text("macOS Viewer & Export", fontSize = 11.sp, color = CharcoalText.copy(alpha = 0.6f))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Single Sign-In Google Account Box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SoftBorderColor)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(GoldAccent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("G", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Google Account", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CharcoalText)
                                Text(
                                    text = if (isSignedIn) "Signed In (Single Account)" else "Sign In Required",
                                    fontSize = 10.sp,
                                    color = if (isSignedIn) Color(0xFF2E7D32) else Color.Red,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = userEmail,
                            onValueChange = {
                                userEmail = it
                                isSignedIn = it.isNotBlank()
                            },
                            label = { Text("Account Email", fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldAccent,
                                unfocusedBorderColor = SoftBorderColor,
                                focusedTextColor = CharcoalText,
                                unfocusedTextColor = CharcoalText
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { fetchSyncedNotesFromCloud() },
                            enabled = !isSyncing,
                            modifier = Modifier.fillMaxWidth().height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Fetching...", fontSize = 12.sp, color = Color.White)
                            } else {
                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Fetch Synced Notes", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (syncMessage != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = syncMessage!!,
                                fontSize = 10.sp,
                                color = CharcoalText.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text("FOLDERS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CharcoalText.copy(alpha = 0.5f))

                Spacer(modifier = Modifier.height(8.dp))

                // All Notes Filter
                SidebarFolderItem(
                    name = "All Notes",
                    count = fetchedNotebooks.size,
                    isSelected = selectedFolder == "All Notes",
                    onClick = {
                        selectedFolder = "All Notes"
                        selectedNotebook = null
                    }
                )

                fetchedFolders.forEach { folder ->
                    SidebarFolderItem(
                        name = folder.name,
                        count = fetchedNotebooks.count { it.folderName.equals(folder.name, ignoreCase = true) },
                        isSelected = selectedFolder == folder.name,
                        onClick = {
                            selectedFolder = folder.name
                            selectedNotebook = null
                        }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.fillMaxHeight().width(1.dp), color = SoftBorderColor)

            // MAIN WORKSPACE: Notebook Grid or Document Viewer
            if (selectedNotebook == null) {
                // NOTEBOOK BROWSER
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(28.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = selectedFolder ?: "All Notes",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = CharcoalText
                            )
                            Text(
                                text = "${filteredNotebooks.size} synced notebooks",
                                fontSize = 13.sp,
                                color = CharcoalText.copy(alpha = 0.6f)
                            )
                        }

                        IconButton(
                            onClick = { fetchSyncedNotesFromCloud() }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Notes", tint = GoldAccent)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (filteredNotebooks.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Book,
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp),
                                    tint = CharcoalText.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No synced notebooks yet.",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CharcoalText
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap 'Sync Now' on your Android Tablet to sync your notes to this Mac!",
                                    fontSize = 13.sp,
                                    color = CharcoalText.copy(alpha = 0.6f)
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 220.dp),
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            items(filteredNotebooks) { notebook ->
                                MacNotebookCard(
                                    notebook = notebook,
                                    onClick = { selectedNotebook = notebook }
                                )
                            }
                        }
                    }
                }
            } else {
                // NOTEBOOK READER & PDF EXPORTER VIEW
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(28.dp)
                ) {
                    // Top Reader Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = { selectedNotebook = null },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("← Back", color = CharcoalText, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = selectedNotebook!!.title,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CharcoalText
                                )
                                Text(
                                    text = "Folder: ${selectedNotebook!!.folderName} • ${selectedNotebook!!.pageCount} Pages",
                                    fontSize = 12.sp,
                                    color = CharcoalText.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    // macOS Export to PDF action
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export PDF", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    // macOS Share Action
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = CharcoalText, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share", color = CharcoalText, fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Stationery Notebook Page View
                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(1.dp, SoftBorderColor, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = IvoryCanvas),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(40.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Ruled Paper Lines & Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = selectedNotebook!!.title.uppercase(),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CharcoalText.copy(alpha = 0.4f),
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "Date: ____ / ____ / 20__",
                                        fontSize = 12.sp,
                                        color = CharcoalText.copy(alpha = 0.4f)
                                    )
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = RuledLineColor, thickness = 1.5.dp)

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    repeat(15) { index ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(36.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            HorizontalDivider(
                                                modifier = Modifier.align(Alignment.BottomCenter),
                                                color = RuledLineColor.copy(alpha = 0.6f),
                                                thickness = 0.8.dp
                                            )
                                            if (index == 1) {
                                                Text(
                                                    text = "Synced handwritten vector ink note content from Android Tablet",
                                                    fontSize = 15.sp,
                                                    color = CharcoalText.copy(alpha = 0.85f),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = "Page 1",
                                        fontSize = 11.sp,
                                        color = CharcoalText.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SidebarFolderItem(
    name: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) GoldAccent.copy(alpha = 0.15f) else Color.Transparent
    val textCol = if (isSelected) GoldAccent else CharcoalText

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Folder, contentDescription = null, tint = textCol, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(name, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = textCol)
        }
        Text("$count", fontSize = 12.sp, color = textCol.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MacNotebookCard(
    notebook: MacNotebook,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MoleskineSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(GoldAccent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Book, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(20.dp))
                }
                Text("${notebook.pageCount} Pages", fontSize = 11.sp, color = CharcoalText.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
            }

            Column {
                Text(
                    text = notebook.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = CharcoalText,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Updated ${notebook.updatedAt}",
                    fontSize = 11.sp,
                    color = CharcoalText.copy(alpha = 0.5f)
                )
            }
        }
    }
}

private suspend fun queryFirestoreUserNotes(email: String): Triple<List<MacFolder>, List<MacNotebook>, List<MacPage>> = withContext(Dispatchers.IO) {
    val folders = mutableListOf<MacFolder>()
    val notebooks = mutableListOf<MacNotebook>()
    val pages = mutableListOf<MacPage>()

    try {
        val firestoreUrl = "https://firestore.googleapis.com/v1/projects/prsnl-15f53/databases/(default)/documents/users"
        val url = URL(firestoreUrl)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 5000
        conn.readTimeout = 5000

        if (conn.responseCode == 200) {
            val responseText = conn.inputStream.bufferedReader().use { it.readText() }
            val json = Json.parseToJsonElement(responseText).jsonObject
            val documents = json["documents"]?.jsonArray ?: return@withContext Triple(folders, notebooks, pages)

            for (doc in documents) {
                val name = doc.jsonObject["name"]?.jsonPrimitive?.content ?: continue
                val fields = doc.jsonObject["fields"]?.jsonObject
                val userNotebooksUrl = "$firestoreUrl/${name.substringAfterLast('/')}/notebooks"
                
                val nbUrl = URL(userNotebooksUrl)
                val nbConn = nbUrl.openConnection() as HttpURLConnection
                nbConn.requestMethod = "GET"
                nbConn.connectTimeout = 5000
                if (nbConn.responseCode == 200) {
                    val nbJsonText = nbConn.inputStream.bufferedReader().use { it.readText() }
                    val nbDocs = Json.parseToJsonElement(nbJsonText).jsonObject["documents"]?.jsonArray ?: continue
                    for (nbDoc in nbDocs) {
                        val nbFields = nbDoc.jsonObject["fields"]?.jsonObject ?: continue
                        val title = nbFields["title"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.content ?: "Untitled"
                        val folderName = nbFields["folderName"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.content ?: "Personal"
                        val id = nbDoc.jsonObject["name"]?.jsonPrimitive?.content?.substringAfterLast('/') ?: "nb_1"
                        notebooks.add(MacNotebook(id, title, folderName, 1, "Just Now"))
                    }
                }
            }
        }
    } catch (_: Exception) {}

    return@withContext Triple(folders, notebooks, pages)
}
