package com.subha.fileease.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.coroutineContext

// Exact Color Tokens
val Sky500 = Color(0xFF0EA5E9)
val Sky50 = Color(0xFFF0F9FF)
val Indigo500 = Color(0xFF6366F1)
val Indigo50 = Color(0xFFEEF2FF)
val Slate800 = Color(0xFF1E293B)
val Slate700 = Color(0xFF334155)
val Slate600 = Color(0xFF475569)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen() {
    val context = LocalContext.current
    var showBottomSheet by remember { mutableStateOf(false) }
    var showRenameSheet by remember { mutableStateOf(false) }
    var selectedFileForOptions by remember { mutableStateOf<File?>(null) }
    var newFileName by remember { mutableStateOf("") }
    var renameError by remember { mutableStateOf("") }

    // File System & Search State
    val rootPath = remember { Environment.getExternalStorageDirectory() }
    var currentDir by remember { mutableStateOf(rootPath) }
    var filesList by remember { mutableStateOf<List<File>>(emptyList()) }

    var searchQuery by remember { mutableStateOf("") }
    var globalSearchResults by remember { mutableStateOf<List<File>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Handle physical back button to navigate up folders
    BackHandler(enabled = currentDir.absolutePath != rootPath.absolutePath) {
        currentDir = currentDir.parentFile ?: rootPath
        searchQuery = ""
    }

    // Permission State
    var hasStoragePermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        hasStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    val legacyLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> hasStoragePermission = isGranted }

    val requestPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                settingsLauncher.launch(intent)
            } catch (e: Exception) {
                settingsLauncher.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        } else {
            legacyLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    // 1. Load current directory files
    LaunchedEffect(currentDir, hasStoragePermission) {
        if (hasStoragePermission) {
            withContext(Dispatchers.IO) {
                try {
                    val allFiles = currentDir.listFiles()?.toList() ?: emptyList()
                    filesList = allFiles.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                } catch (e: Exception) {
                    filesList = emptyList()
                }
            }
        }
    }

    // 2. Perform Global Device Search when typing
    LaunchedEffect(searchQuery, currentDir, hasStoragePermission) {
        if (searchQuery.isNotBlank() && hasStoragePermission) {
            isSearching = true
            delay(300)

            withContext(Dispatchers.IO) {
                globalSearchResults = searchFilesGlobally(context, searchQuery, currentDir)
            }
            isSearching = false
        } else {
            globalSearchResults = emptyList()
            isSearching = false
        }
    }

    // Instantly snap to the top when the folder or search changes
    LaunchedEffect(currentDir, searchQuery) {
        listState.scrollToItem(0)
    }

    val displayedFiles = if (searchQuery.isNotBlank()) globalSearchResults else filesList

    Scaffold(
        topBar = { FileManagerTopBar() },
        containerColor = BgColor
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) {
            // Header Section
            item {
                Spacer(modifier = Modifier.height(12.dp))
                FileSearchBar(
                    query = searchQuery,
                    isSearching = isSearching,
                    onQueryChange = { searchQuery = it }
                )
                Spacer(modifier = Modifier.height(16.dp))
                BreadcrumbNavigation(
                    rootPath = rootPath,
                    currentDir = currentDir,
                    onBreadcrumbClick = {
                        currentDir = it
                        searchQuery = ""
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Body Section
            if (!hasStoragePermission) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(PrimaryBlue.copy(alpha = 0.1f)).clickable { requestPermission() }.padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.Folder, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Storage Permission Required", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text("Tap here to allow access to your files", fontSize = 13.sp, color = TextSecondary)
                        }
                    }
                }
            } else if (isSearching) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(36.dp), color = PrimaryBlue, strokeWidth = 3.dp)
                    }
                }
            } else if (displayedFiles.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matches found for \"$searchQuery\"" else "This folder is empty",
                            fontSize = 14.sp,
                            color = TextTertiary
                        )
                    }
                }
            } else {
                items(displayedFiles) { file ->
                    Box(modifier = Modifier.padding(bottom = 8.dp)) {
                        val (icon, bg, fg) = getFileIconAndColors(file)

                        val subtitle = if (searchQuery.isNotBlank()) {
                            "In: ${file.parentFile?.name ?: "Storage"} • ${formatDate(file.lastModified())}"
                        } else if (file.isDirectory) {
                            "Folder • ${formatDate(file.lastModified())}"
                        } else {
                            "${formatSize(file.length())} • ${formatDate(file.lastModified())}"
                        }

                        FileListItem(
                            icon = icon, iconBgColor = bg, iconColor = fg,
                            title = file.name, subtitle = subtitle,
                            onOptionsClick = {
                                selectedFileForOptions = file
                                showBottomSheet = true
                            },
                            onItemClick = {
                                if (file.isDirectory) {
                                    currentDir = file
                                    searchQuery = ""
                                } else {
                                    Toast.makeText(context, "Opening ${file.name}...", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }

    // Contextual Bottom Drawer for File Options
    if (showBottomSheet && selectedFileForOptions != null) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFFCBD5E1)) }
        ) {
            FileOptionsDrawerContent(
                file = selectedFileForOptions!!,
                onDismiss = { showBottomSheet = false },
                onRename = {
                    newFileName = selectedFileForOptions?.name ?: ""
                    renameError = ""
                    showBottomSheet = false
                    showRenameSheet = true
                },
                onDelete = {
                    try {
                        if (selectedFileForOptions?.deleteRecursively() == true) {
                            filesList = filesList.filter { it.absolutePath != selectedFileForOptions?.absolutePath }
                            globalSearchResults = globalSearchResults.filter { it.absolutePath != selectedFileForOptions?.absolutePath }
                            Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {}
                    showBottomSheet = false
                }
            )
        }
    }

    // Modern Bottom Sheet for Renaming (FIXED HEIGHTS TO PREVENT JUMPING)
    if (showRenameSheet && selectedFileForOptions != null) {
        ModalBottomSheet(
            onDismissRequest = { showRenameSheet = false },
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFFCBD5E1)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 32.dp)
            ) {
                Text("Rename", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(16.dp))

                BasicTextField(
                    value = newFileName,
                    onValueChange = {
                        newFileName = it
                        if (renameError.isNotEmpty()) renameError = ""
                    },
                    textStyle = TextStyle(fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp) // <-- FIX 1: Hardcoded height prevents jumping when typing
                                .border(
                                    width = if (renameError.isNotEmpty()) 1.5.dp else 0.dp,
                                    color = if (renameError.isNotEmpty()) Red500 else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clip(RoundedCornerShape(12.dp))
                                .background(Slate100)
                                .padding(horizontal = 16.dp), // Removed vertical padding since height is fixed
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (newFileName.isEmpty()) {
                                Text("Enter new name", color = TextTertiary, fontSize = 14.sp)
                            }
                            innerTextField()
                        }
                    }
                )

                // <-- FIX 2: A Box that ALWAYS reserves 24.dp of height, so the sheet never resizes
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .padding(start = 4.dp, top = 4.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    if (renameError.isNotEmpty()) {
                        Text(
                            text = renameError,
                            color = Red500,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextTertiary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showRenameSheet = false }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(PrimaryBlue)
                            .clickable {
                                val oldFile = selectedFileForOptions!!

                                if (newFileName.isBlank()) {
                                    renameError = "File name cannot be empty"
                                    return@clickable
                                }
                                if (newFileName == oldFile.name) {
                                    renameError = "Please enter a different name"
                                    return@clickable
                                }

                                val newFile = File(oldFile.parentFile, newFileName)
                                if (!newFile.exists()) {
                                    if (oldFile.renameTo(newFile)) {
                                        filesList = filesList.map { if (it.absolutePath == oldFile.absolutePath) newFile else it }
                                            .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                                        globalSearchResults = globalSearchResults.map { if (it.absolutePath == oldFile.absolutePath) newFile else it }
                                            .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))

                                        Toast.makeText(context, "Renamed successfully", Toast.LENGTH_SHORT).show()
                                        showRenameSheet = false
                                    } else {
                                        Toast.makeText(context, "Failed to rename", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    renameError = "A file with this name already exists"
                                }
                            }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Rename", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// HELPER FUNCTIONS
// -------------------------------------------------------------

private suspend fun searchFilesGlobally(context: Context, query: String, currentDir: File): List<File> {
    val results = mutableSetOf<File>()

    currentDir.listFiles()?.forEach { file ->
        yield()
        if (file.name.contains(query, ignoreCase = true)) {
            results.add(file)
        }
    }

    try {
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(MediaStore.Files.FileColumns.DATA)
        val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$query%")

        context.contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            while (cursor.moveToNext()) {
                yield()
                val path = cursor.getString(dataCol)
                if (path != null) {
                    results.add(File(path))
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return results.toList().sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
}

private fun formatSize(sizeInBytes: Long): String {
    val kb = sizeInBytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1 -> "%.1f GB".format(gb)
        mb >= 1 -> "%.1f MB".format(mb)
        kb >= 1 -> "%.1f KB".format(kb)
        else -> "$sizeInBytes B"
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun getFileIconAndColors(file: File): Triple<ImageVector, Color, Color> {
    if (file.isDirectory) return Triple(Icons.Outlined.Folder, Blue50, PrimaryBlue)

    return when (file.extension.lowercase(Locale.getDefault())) {
        "pdf" -> Triple(Icons.Outlined.PictureAsPdf, Red50, Red500)
        "jpg", "jpeg", "png", "gif" -> Triple(Icons.Outlined.Image, Sky50, Sky500)
        "doc", "docx", "txt" -> Triple(Icons.Outlined.Description, Indigo50, Indigo500)
        "zip", "rar" -> Triple(Icons.Outlined.FolderZip, Slate100, Slate700)
        "mp3", "wav" -> Triple(Icons.Outlined.AudioFile, Sky50, Sky500)
        "mp4", "mkv" -> Triple(Icons.Outlined.VideoFile, Indigo50, Indigo500)
        else -> Triple(Icons.Outlined.InsertDriveFile, Slate100, Slate700)
    }
}

// -------------------------------------------------------------
// UI COMPONENTS
// -------------------------------------------------------------

@Composable
fun FileManagerTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceColor.copy(alpha = 0.95f))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Files", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(Slate100).clickable { },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Add, contentDescription = "Add new file", tint = Slate700, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun FileSearchBar(query: String, isSearching: Boolean, onQueryChange: (String) -> Unit) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        textStyle = TextStyle(fontSize = 14.sp, color = TextPrimary),
        singleLine = true,
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Slate100.copy(alpha = 0.9f)),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp).fillMaxWidth()
                ) {
                    if (isSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = PrimaryBlue, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Outlined.Search, contentDescription = "Search", tint = TextTertiary, modifier = Modifier.size(20.dp))
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text("Search files and folders", color = TextTertiary, fontSize = 14.sp)
                        }
                        innerTextField()
                    }

                    if (query.isNotEmpty()) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Clear Search",
                            tint = TextTertiary,
                            modifier = Modifier.size(18.dp).clip(CircleShape).clickable { onQueryChange("") }
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun BreadcrumbNavigation(rootPath: File, currentDir: File, onBreadcrumbClick: (File) -> Unit) {
    val relativePath = currentDir.absolutePath.removePrefix(rootPath.absolutePath).removePrefix("/")
    val segments = if (relativePath.isEmpty()) emptyList() else relativePath.split("/")

    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Internal Storage",
            color = if (segments.isEmpty()) Slate800 else PrimaryBlue,
            fontSize = 13.sp,
            fontWeight = if (segments.isEmpty()) FontWeight.SemiBold else FontWeight.Medium,
            modifier = Modifier.clickable(enabled = segments.isNotEmpty()) { onBreadcrumbClick(rootPath) }
        )

        var pathBuilder = rootPath.absolutePath
        segments.forEachIndexed { index, segment ->
            Text(" › ", color = TextTertiary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 4.dp))
            pathBuilder += "/$segment"
            val segmentFile = File(pathBuilder)
            val isLast = index == segments.lastIndex

            Text(
                text = segment,
                color = if (isLast) Slate800 else PrimaryBlue,
                fontSize = 13.sp,
                fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Medium,
                modifier = Modifier.clickable(enabled = !isLast) { onBreadcrumbClick(segmentFile) }
            )
        }
    }
}

@Composable
fun FileListItem(
    icon: ImageVector, iconBgColor: Color, iconColor: Color,
    title: String, subtitle: String,
    onOptionsClick: () -> Unit, onItemClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Slate100.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceColor)
            .clickable { onItemClick() }
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(iconBgColor), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.sp, color = TextTertiary)
            }
        }
        Box(modifier = Modifier.size(32.dp).clip(CircleShape).clickable { onOptionsClick() }, contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.MoreVert, contentDescription = "Options", tint = TextTertiary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun FileOptionsDrawerContent(
    file: File,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val (icon, bg, fg) = getFileIconAndColors(file)
    val subtitle = if (file.isDirectory) "Folder" else "File • ${formatSize(file.length())}"

    Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 24.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(bg), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(file.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, fontSize = 12.sp, color = TextTertiary)
            }
        }

        HorizontalDivider(color = Slate100)
        Spacer(modifier = Modifier.height(8.dp))

        DrawerActionItem(icon = Icons.Outlined.Edit, title = "Rename", color = Slate800, iconColor = Slate600, onClick = onRename)
        DrawerActionItem(icon = Icons.Outlined.OpenWith, title = "Move", color = Slate800, iconColor = Slate600, onClick = { onDismiss() })
        DrawerActionItem(icon = Icons.Outlined.DeleteOutline, title = "Delete", color = Red500, iconColor = Red500, onClick = onDelete)
    }
}

@Composable
fun DrawerActionItem(icon: ImageVector, title: String, color: Color, iconColor: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = color)
    }
}