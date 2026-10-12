package com.subha.fileease.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    var selectedFileForOptions by remember { mutableStateOf<File?>(null) }

    // File System State
    val rootPath = remember { Environment.getExternalStorageDirectory() }
    var currentDir by remember { mutableStateOf(rootPath) }
    var filesList by remember { mutableStateOf<List<File>>(emptyList()) }

    // Handle physical back button to navigate up folders instead of closing app
    BackHandler(enabled = currentDir.absolutePath != rootPath.absolutePath) {
        currentDir = currentDir.parentFile ?: rootPath
    }

    // Permission State (Reused from HomeDashboard)
    var hasStoragePermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        hasStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
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
            legacyLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    // Reactively load files whenever the directory or permissions change
    LaunchedEffect(currentDir, hasStoragePermission) {
        if (hasStoragePermission) {
            withContext(Dispatchers.IO) {
                try {
                    val allFiles = currentDir.listFiles()?.toList() ?: emptyList()
                    // Sort folders first, then alphabetical
                    filesList = allFiles.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                } catch (e: Exception) {
                    filesList = emptyList()
                }
            }
        }
    }

    Scaffold(
        topBar = { FileManagerTopBar() },
        containerColor = BgColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            FileSearchBar()

            Spacer(modifier = Modifier.height(16.dp))

            // Dynamic Breadcrumbs
            BreadcrumbNavigation(
                rootPath = rootPath,
                currentDir = currentDir,
                onBreadcrumbClick = { currentDir = it }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // File List UI
            if (!hasStoragePermission) {
                // Permission Prompt UI
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
            } else if (filesList.isEmpty()) {
                // Empty Folder UI
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("This folder is empty", fontSize = 14.sp, color = TextTertiary)
                }
            } else {
                // Real Files UI
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filesList.forEach { file ->
                        val (icon, bg, fg) = getFileIconAndColors(file)
                        val subtitle = if (file.isDirectory) {
                            val count = file.listFiles()?.size ?: 0
                            "$count items • ${formatDate(file.lastModified())}"
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
                                } else {
                                    // For now, show a toast. Later you can add a FileProvider to open files externally!
                                    Toast.makeText(context, "Opening ${file.name}...", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(100.dp)) // Scroll clearance
        }
    }

    // Contextual Bottom Drawer
    if (showBottomSheet && selectedFileForOptions != null) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFFCBD5E1)) }
        ) {
            FileOptionsDrawerContent(
                file = selectedFileForOptions!!,
                onDismiss = { showBottomSheet = false },
                onDelete = {
                    try {
                        if (selectedFileForOptions?.deleteRecursively() == true) {
                            filesList = filesList.filter { it.absolutePath != selectedFileForOptions?.absolutePath }
                            Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {}
                    showBottomSheet = false
                }
            )
        }
    }
}

// -------------------------------------------------------------
// HELPER FUNCTIONS (Sizes, Dates, and Themes)
// -------------------------------------------------------------

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
fun FileSearchBar() {
    Box(
        modifier = Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(50)).background(Slate100.copy(alpha = 0.9f)).clickable { },
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp)) {
            Icon(Icons.Outlined.Search, contentDescription = "Search", tint = TextTertiary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text("Search files and folders", color = TextTertiary, fontSize = 14.sp)
        }
    }
}

@Composable
fun BreadcrumbNavigation(rootPath: File, currentDir: File, onBreadcrumbClick: (File) -> Unit) {
    val relativePath = currentDir.absolutePath.removePrefix(rootPath.absolutePath).removePrefix("/")
    val segments = if (relativePath.isEmpty()) emptyList() else relativePath.split("/")

    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Root Breadcrumb
        Text(
            text = "Internal Storage",
            color = if (segments.isEmpty()) Slate800 else PrimaryBlue,
            fontSize = 13.sp,
            fontWeight = if (segments.isEmpty()) FontWeight.SemiBold else FontWeight.Medium,
            modifier = Modifier.clickable(enabled = segments.isNotEmpty()) { onBreadcrumbClick(rootPath) }
        )

        // Subfolder Breadcrumbs
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
            .clickable { onItemClick() } // Made the row interactive!
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
fun FileOptionsDrawerContent(file: File, onDismiss: () -> Unit, onDelete: () -> Unit) {
    val (icon, bg, fg) = getFileIconAndColors(file)
    val subtitle = if (file.isDirectory) "Folder • ${file.listFiles()?.size ?: 0} items" else "File • ${formatSize(file.length())}"

    Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 24.dp)) {
        // Selected File Header
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

        // Actions
        DrawerActionItem(icon = Icons.Outlined.Edit, title = "Rename", color = Slate800, iconColor = Slate600, onClick = { onDismiss() })
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