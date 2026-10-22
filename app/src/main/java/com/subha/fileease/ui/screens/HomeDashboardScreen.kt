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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.subha.fileease.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

// App-Wide Color Tokens
val BgColor = Color(0xFFFAF8FF)
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

enum class ActivityIconType { PDF, IMAGE, DOC }
data class RecentActivityData(
    val title: String,
    val subtitle: String,
    val size: String,
    val iconType: ActivityIconType
)

// Premium SaaS Bounce Animation Modifier
fun Modifier.bounceClick(onClick: () -> Unit): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.6f),
        label = "bounce"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            while (true) {
                awaitPointerEventScope {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    waitForUpOrCancellation()
                    isPressed = false
                }
            }
        }
        .clickable { onClick() }
}

@Composable
fun HomeDashboardScreen(
    onMergeClick: () -> Unit = {},
    onSplitClick: () -> Unit = {},
    onCompressClick: () -> Unit = {},
    onImagesToPdfClick: () -> Unit = {},
    onViewAllClick: () -> Unit = {},
    onRecentViewAllClick: () -> Unit = {} // Added navigation parameter for Recent Activity
) {
    val context = LocalContext.current

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
    ) { isGranted ->
        hasStoragePermission = isGranted
    }

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
            Spacer(modifier = Modifier.height(8.dp))
            GreetingSection()

            Spacer(modifier = Modifier.height(28.dp))
            QuickActionsSection(
                onMergeClick = onMergeClick,
                onSplitClick = onSplitClick,
                onCompressClick = onCompressClick,
                onImagesToPdfClick = onImagesToPdfClick,
                onViewAllClick = onViewAllClick
            )

            Spacer(modifier = Modifier.height(28.dp))
            DeviceStorageSection(hasStoragePermission, requestPermission)

            Spacer(modifier = Modifier.height(28.dp))
            RecentActivitySection(hasStoragePermission, onRecentViewAllClick) // Wired to section

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun DashboardTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgColor.copy(alpha = 0.95f))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.fileease_logo),
            contentDescription = "App Logo",
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "FileEase",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontFamily = LocalTextStyle.current.fontFamily
        )
    }
}

@Composable
fun GreetingSection() {
    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = when (currentHour) {
        in 0..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..20 -> "Good evening"
        else -> "Good night"
    }

    Column {
        Text(
            text = "$greeting",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            lineHeight = 34.sp,
            fontFamily = LocalTextStyle.current.fontFamily
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "What would you like to do today?",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary,
            fontFamily = LocalTextStyle.current.fontFamily
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
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontFamily = LocalTextStyle.current.fontFamily
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .bounceClick { onViewAllClick() }
                    .padding(4.dp)
            ) {
                Text(
                    text = "View all",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryBlue,
                    fontFamily = LocalTextStyle.current.fontFamily
                )
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = "View all",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .width(96.dp)
                    .height(108.dp)
                    .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = PrimaryBlue.copy(alpha = 0.4f))
                    .clip(RoundedCornerShape(20.dp))
                    .background(PrimaryBlue)
                    .bounceClick { onImagesToPdfClick() }
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Image, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Images to\nPDF",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    fontFamily = LocalTextStyle.current.fontFamily
                )
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
            .width(96.dp)
            .height(108.dp)
            .border(1.dp, Slate100, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceColor)
            .bounceClick { onClick() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Blue50),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp,
            fontFamily = LocalTextStyle.current.fontFamily
        )
    }
}

@Composable
fun DeviceStorageSection(hasStoragePermission: Boolean, onRequestPermission: () -> Unit) {
    val context = LocalContext.current

    var totalGb by rememberSaveable { mutableStateOf(128f) }
    var usedGb by rememberSaveable { mutableStateOf(0f) }
    var docsGb by rememberSaveable { mutableStateOf(0f) }
    var othersGb by rememberSaveable { mutableStateOf(0f) }
    var progress by rememberSaveable { mutableStateOf(0f) }
    var isDataLoaded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(hasStoragePermission) {
        if (!isDataLoaded) {
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
                        isDataLoaded = true
                    } else {
                        docsGb = 0f
                        othersGb = uGb
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF2563EB), Color(0xFF1D4ED8))))
            .bounceClick { if (!hasStoragePermission) onRequestPermission() }
            .padding(24.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Memory, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Device Storage", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.9f), fontFamily = LocalTextStyle.current.fontFamily)
                }

                if (!hasStoragePermission) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Tap to sync", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White, fontFamily = LocalTextStyle.current.fontFamily)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text("${"%.1f".format(usedGb)} GB", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, fontFamily = LocalTextStyle.current.fontFamily)
                Spacer(modifier = Modifier.width(6.dp))
                Text("of ${"%.0f".format(totalGb)} GB used", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.8f), fontFamily = LocalTextStyle.current.fontFamily, modifier = Modifier.padding(bottom = 4.dp))
            }

            Spacer(modifier = Modifier.height(18.dp))

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

            Spacer(modifier = Modifier.height(18.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (hasStoragePermission) "Documents ${"%.0f".format(docsGb)} GB" else "Docs Hidden", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.9f), fontFamily = LocalTextStyle.current.fontFamily)
                }
                Spacer(modifier = Modifier.width(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.5f)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Others ${"%.0f".format(othersGb)} GB", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.9f), fontFamily = LocalTextStyle.current.fontFamily)
                }
            }
        }
    }
}

@Composable
fun RecentActivitySection(hasStoragePermission: Boolean, onViewAllClick: () -> Unit) {
    val context = LocalContext.current
    var recentFiles by remember { mutableStateOf<List<RecentActivityData>>(emptyList()) }
    var isFilesLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(hasStoragePermission) {
        if (hasStoragePermission && !isFilesLoaded) {
            withContext(Dispatchers.IO) {
                recentFiles = getRecentDeviceFiles(context)
                isFilesLoaded = true
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
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontFamily = LocalTextStyle.current.fontFamily
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .bounceClick { onViewAllClick() } // Trigger navigation here
                    .padding(4.dp)
            ) {
                Text(
                    text = "View all",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryBlue,
                    fontFamily = LocalTextStyle.current.fontFamily
                )
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = "View all",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!hasStoragePermission) {
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
            } else if (recentFiles.isEmpty() && isFilesLoaded) {
                Text("No recent files found", color = TextTertiary, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(14.dp), fontFamily = LocalTextStyle.current.fontFamily)
            } else {
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
            .border(1.dp, Slate100, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceColor)
            .bounceClick { }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = LocalTextStyle.current.fontFamily
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextTertiary,
                    fontFamily = LocalTextStyle.current.fontFamily
                )
            }
        }
        Text(
            text = size,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            modifier = Modifier.padding(start = 8.dp),
            fontFamily = LocalTextStyle.current.fontFamily
        )
    }
}

private fun getRealDocumentsSize(context: Context): Long {
    var totalSize = 0L
    try {
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(MediaStore.Files.FileColumns.SIZE)
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

                if (sizeBytes == 0L) continue

                val sizeStr = if (sizeBytes > 1024 * 1024) {
                    "%.1f MB".format(sizeBytes / (1024f * 1024f))
                } else {
                    "${sizeBytes / 1024} KB"
                }

                val nowSecs = System.currentTimeMillis() / 1000
                val diff = nowSecs - dateSecs
                val timeStr = when {
                    diff < 60 -> "Just now"
                    diff < 3600 -> "${diff / 60} min ago"
                    diff < 86400 -> "${diff / 3600} hours ago"
                    diff < 172800 -> "Yesterday"
                    else -> "${diff / 86400} days ago"
                }

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