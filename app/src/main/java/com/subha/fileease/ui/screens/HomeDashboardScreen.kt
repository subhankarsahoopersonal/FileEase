package com.subha.fileease.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.provider.Settings
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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

// Exact colors from the Stitch HTML file
val BgColor = Color(0xFFF8FAFC)
val SurfaceColor = Color(0xFFFFFFFF)
val PrimaryBlue = Color(0xFF0066FF)
val TextPrimary = Color(0xFF0F172A)
val TextSecondary = Color(0xFF64748B)
val TextTertiary = Color(0xFF94A3B8)
val Slate100 = Color(0xFFF1F5F9)
val Blue50 = Color(0xFFEFF6FF)
val Red500 = Color(0xFFEF4444)
val Red50 = Color(0xFFFEF2F2)
val Emerald500 = Color(0xFF10B981)
val Emerald50 = Color(0xFFECFDF5)

// Enums and Data Class for Real Files
enum class ActivityIconType { PDF, IMAGE, DOC }
data class RecentActivityData(
    val title: String,
    val subtitle: String,
    val size: String,
    val iconType: ActivityIconType
)

@Composable
fun HomeDashboardScreen(
    // Added navigation callbacks to make the dashboard functional
    onMergeClick: () -> Unit = {},
    onSplitClick: () -> Unit = {},
    onCompressClick: () -> Unit = {},
    onImagesToPdfClick: () -> Unit = {},
    onViewAllClick: () -> Unit = {}
) {
    val context = LocalContext.current

    // 1. Hoist the permission state so BOTH sections can react to it instantly
    var hasStoragePermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    // Launcher for modern Android (Android 11+)
    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        hasStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Launcher for old Android (Android 10 and below)
    val legacyLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasStoragePermission = isGranted
    }

    // The function that runs when you tap "Tap to Sync"
    val requestPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                settingsLauncher.launch(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                settingsLauncher.launch(intent)
            }
        } else {
            legacyLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    Scaffold(
        topBar = { DashboardTopBar() },
        containerColor = BgColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            GreetingSection()

            Spacer(modifier = Modifier.height(24.dp))
            QuickActionsSection(
                onMergeClick = onMergeClick,
                onSplitClick = onSplitClick,
                onCompressClick = onCompressClick,
                onImagesToPdfClick = onImagesToPdfClick,
                onViewAllClick = onViewAllClick
            )

            Spacer(modifier = Modifier.height(24.dp))
            // Pass the permission down to Storage
            DeviceStorageSection(hasStoragePermission, requestPermission)

            Spacer(modifier = Modifier.height(24.dp))
            // Pass the permission down to Recent Activity
            RecentActivitySection(hasStoragePermission)

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun DashboardTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceColor.copy(alpha = 0.9f))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo & App Name
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PrimaryBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = "Logo",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "FileEase",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        // Notifications & Avatar
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    tint = TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
                // Red Dot Indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 6.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Red500)
                        .border(2.dp, SurfaceColor, CircleShape)
                )
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Slate100)
                    .border(2.dp, SurfaceColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = "User Avatar",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun GreetingSection() {
    Column {
        Text(
            text = "Good morning,\nSubha",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            lineHeight = 28.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "What would you like to do today?",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
        )
    }
}

@Composable
fun QuickActionsSection(
    onMergeClick: () -> Unit,
    onSplitClick: () -> Unit,
    onCompressClick: () -> Unit,
    onImagesToPdfClick: () -> Unit,
    onViewAllClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quick Actions",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onViewAllClick() }
                    .padding(4.dp)
            ) {
                Text(
                    text = "View all",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryBlue
                )
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = "View all",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Images to PDF (Active) - Added onClick callback
            Column(
                modifier = Modifier
                    .width(84.dp)
                    .height(92.dp)
                    .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = PrimaryBlue)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PrimaryBlue)
                    .clickable { onImagesToPdfClick() }
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Image, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("Images to PDF", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1)
            }

            QuickActionCard(icon = Icons.Outlined.Layers, title = "Merge PDF", onClick = onMergeClick)
            QuickActionCard(icon = Icons.Outlined.ContentCut, title = "Split PDF", onClick = onSplitClick)
            QuickActionCard(icon = Icons.Outlined.ZoomInMap, title = "Compress", onClick = onCompressClick)
        }
    }
}

@Composable
fun QuickActionCard(icon: ImageVector, title: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(84.dp)
            .height(92.dp)
            .border(1.dp, Slate100, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceColor)
            .clickable { onClick() }
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Blue50),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B), maxLines = 1)
    }
}

@Composable
fun DeviceStorageSection(hasStoragePermission: Boolean, onRequestPermission: () -> Unit) {
    val context = LocalContext.current
    var totalGb by remember { mutableStateOf(128f) }
    var usedGb by remember { mutableStateOf(0f) }
    var docsGb by remember { mutableStateOf(0f) }
    var othersGb by remember { mutableStateOf(0f) }
    var progress by remember { mutableStateOf(0f) }

    // Fetch real storage stats asynchronously when UI loads or permission changes
    LaunchedEffect(hasStoragePermission) {
        withContext(Dispatchers.IO) {
            try {
                val stat = StatFs(Environment.getDataDirectory().path)
                val totalBytes = stat.totalBytes
                val availableBytes = stat.availableBytes
                val usedBytes = totalBytes - availableBytes

                val gigabyte = 1_000_000_000f
                val tGb = totalBytes / gigabyte
                val uGb = usedBytes / gigabyte

                totalGb = tGb
                usedGb = uGb
                progress = if (tGb > 0) uGb / tGb else 0f

                if (hasStoragePermission) {
                    val realDocsBytes = getRealDocumentsSize(context)
                    docsGb = realDocsBytes / gigabyte
                    othersGb = maxOf(0f, uGb - docsGb)
                } else {
                    docsGb = 0f
                    othersGb = uGb
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1E60F2), Color(0xFF3B82F6), Color(0xFF60A5FA))))
            // Make the whole card clickable as a fallback
            .clickable(enabled = !hasStoragePermission) { onRequestPermission() }
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Memory, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Device Storage", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.9f))
                }

                if (!hasStoragePermission) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable { onRequestPermission() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Tap to sync", fontSize = 10.sp, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text("${"%.1f".format(usedGb)} GB", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("of ${"%.0f".format(totalGb)} GB used", fontSize = 14.sp, fontWeight = FontWeight.Normal, color = Color.White.copy(alpha = 0.8f))
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0.01f, 1f))
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (hasStoragePermission) "Documents ${"%.2f".format(docsGb)} GB" else "Docs Hidden", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.9f))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.5f)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Others ${"%.1f".format(othersGb)} GB", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.9f))
                }
            }
        }
    }
}

@Composable
fun RecentActivitySection(hasStoragePermission: Boolean) {
    val context = LocalContext.current
    var recentFiles by remember { mutableStateOf<List<RecentActivityData>>(emptyList()) }

    // Fetch the 3 newest files whenever permission is granted
    LaunchedEffect(hasStoragePermission) {
        if (hasStoragePermission) {
            withContext(Dispatchers.IO) {
                recentFiles = getRecentDeviceFiles(context)
            }
        }
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Activity",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "View all",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryBlue
                )
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = "View all",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!hasStoragePermission) {
                // Keep showing dummy design if permissions aren't granted yet
                ActivityItem(
                    icon = Icons.Outlined.PictureAsPdf,
                    iconBgColor = Red50,
                    iconColor = Red500,
                    title = "document_scan.pdf",
                    subtitle = "Edited • 2 min ago",
                    size = "4.2 MB"
                )
                ActivityItem(
                    icon = Icons.Outlined.Image,
                    iconBgColor = Emerald50,
                    iconColor = Emerald500,
                    title = "invoice.jpg",
                    subtitle = "Converted • 18 min ago",
                    size = "1.1 MB"
                )
                ActivityItem(
                    icon = Icons.Outlined.Description,
                    iconBgColor = Blue50,
                    iconColor = PrimaryBlue,
                    title = "project_brief.docx",
                    subtitle = "Compressed • Yesterday",
                    size = "890 KB"
                )
            } else if (recentFiles.isEmpty()) {
                Text("No recent files found", color = TextTertiary, fontSize = 13.sp, modifier = Modifier.padding(14.dp))
            } else {
                // Render the REAL files from the device
                recentFiles.forEach { file ->
                    val icon = when (file.iconType) {
                        ActivityIconType.PDF -> Icons.Outlined.PictureAsPdf
                        ActivityIconType.IMAGE -> Icons.Outlined.Image
                        ActivityIconType.DOC -> Icons.Outlined.Description
                    }
                    val bg = when (file.iconType) {
                        ActivityIconType.PDF -> Red50
                        ActivityIconType.IMAGE -> Emerald50
                        ActivityIconType.DOC -> Blue50
                    }
                    val fg = when (file.iconType) {
                        ActivityIconType.PDF -> Red500
                        ActivityIconType.IMAGE -> Emerald500
                        ActivityIconType.DOC -> PrimaryBlue
                    }
                    ActivityItem(icon, bg, fg, file.title, file.subtitle, file.size)
                }
            }
        }
    }
}

@Composable
fun ActivityItem(
    icon: ImageVector,
    iconBgColor: Color,
    iconColor: Color,
    title: String,
    subtitle: String,
    size: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Slate100, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceColor)
            .clickable { }
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextTertiary
                )
            }
        }
        Text(
            text = size,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

// ---------------------------------------------------------
// ANDROID NATIVE HELPER FUNCTIONS (MEDIA STORE QUERIES)
// ---------------------------------------------------------

private fun getRealDocumentsSize(context: Context): Long {
    var totalSize = 0L
    try {
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(MediaStore.Files.FileColumns.SIZE)
        // MEDIA_TYPE_DOCUMENT includes PDFs, TXT, DOCX, etc.
        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"
        val selectionArgs = arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_DOCUMENT.toString())

        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            while (cursor.moveToNext()) {
                totalSize += cursor.getLong(sizeColumn)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return totalSize
}

private fun getRecentDeviceFiles(context: Context): List<RecentActivityData> {
    val list = mutableListOf<RecentActivityData>()
    try {
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.MIME_TYPE
        )
        // Sort by newest modified
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)

            var count = 0
            while (cursor.moveToNext() && count < 3) {
                val name = cursor.getString(nameCol) ?: "Unknown File"
                val sizeBytes = cursor.getLong(sizeCol)
                val dateSecs = cursor.getLong(dateCol)
                val mime = cursor.getString(mimeCol) ?: ""

                // Skip empty directories or 0 byte ghost files
                if (sizeBytes == 0L) continue

                // 1. Format Size
                val sizeStr = if (sizeBytes > 1024 * 1024) {
                    "%.1f MB".format(sizeBytes / (1024f * 1024f))
                } else {
                    "${sizeBytes / 1024} KB"
                }

                // 2. Format Time
                val nowSecs = System.currentTimeMillis() / 1000
                val diff = nowSecs - dateSecs
                val timeStr = when {
                    diff < 60 -> "Just now"
                    diff < 3600 -> "${diff / 60} min ago"
                    diff < 86400 -> "${diff / 3600} hours ago"
                    diff < 172800 -> "Yesterday"
                    else -> "${diff / 86400} days ago"
                }

                // 3. Determine Icon Theme based on extension/mime
                val iconType = when {
                    mime.contains("pdf", true) || name.endsWith(".pdf", true) -> ActivityIconType.PDF
                    mime.contains("image", true) || name.endsWith(".jpg", true) || name.endsWith(".png", true) -> ActivityIconType.IMAGE
                    else -> ActivityIconType.DOC
                }

                val verb = when(iconType) {
                    ActivityIconType.PDF -> "Modified"
                    ActivityIconType.IMAGE -> "Saved"
                    ActivityIconType.DOC -> "Edited"
                }

                list.add(
                    RecentActivityData(
                        title = name,
                        subtitle = "$verb • $timeStr",
                        size = sizeStr,
                        iconType = iconType
                    )
                )
                count++
            }
        }
    } catch (e: Exception) { e.printStackTrace() }
    return list
}