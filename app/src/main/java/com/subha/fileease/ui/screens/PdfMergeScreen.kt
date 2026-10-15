package com.subha.fileease.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.FileProvider
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.roundToInt

// Exact color tokens
private val Surface = Color(0xFFFAF8FF)
private val SurfaceContainerLowest = Color(0xFFFFFFFF)
private val SurfaceContainerLow = Color(0xFFF2F3FF)
private val SurfaceContainer = Color(0xFFEAEDFF)
private val SurfaceContainerHigh = Color(0xFFE2E7FF)
private val SurfaceContainerHighest = Color(0xFFDAE2FD)
private val OnSurface = Color(0xFF131B2E)
private val OnSurfaceVariant = Color(0xFF424656)
private val Primary = Color(0xFF0050CB)
private val OnPrimary = Color(0xFFFFFFFF)
private val PrimaryContainer = Color(0xFF0066FF)
private val OnPrimaryContainer = Color(0xFFF8F7FF)
private val PrimaryFixed = Color(0xFFDAE1FF)
private val Secondary = Color(0xFF0058BE)
private val SecondaryContainer = Color(0xFF2170E4)
private val Error = Color(0xFFBA1A1A)
private val ErrorContainer = Color(0xFFFFDAD6)
private val OutlineVariant = Color(0xFFC2C6D8)
private val Outline = Color(0xFF727687)

data class PdfFileItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val pages: Int,
    val sizeMb: Double,
    val uri: Uri,
    val thumbnail: Bitmap? = null,
    val isEncrypted: Boolean = false,
    val isProcessed: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfMergeScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var fileList by remember { mutableStateOf<List<PdfFileItem>>(emptyList()) }
    var outputName by remember { mutableStateOf("Merged_Document.pdf") }
    var addToc by remember { mutableStateOf(true) }
    var normalizeSize by remember { mutableStateOf(false) }
    var isMerging by remember { mutableStateOf(false) }

    // Async Thumbnail & Page Parsing Worker
    LaunchedEffect(fileList) {
        val unprocessed = fileList.filter { !it.isProcessed }
        if (unprocessed.isNotEmpty()) {
            coroutineScope.launch(Dispatchers.IO) {
                unprocessed.forEach { file ->
                    var pageCount = 0
                    var bitmap: Bitmap? = null
                    var encrypted = false

                    try {
                        context.contentResolver.openFileDescriptor(file.uri, "r")?.use { fd ->
                            val renderer = PdfRenderer(fd)
                            pageCount = renderer.pageCount
                            if (pageCount > 0) {
                                val page = renderer.openPage(0)
                                val width = 200
                                val height = (width * page.height) / page.width
                                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                bitmap.eraseColor(android.graphics.Color.WHITE)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                page.close()
                            }
                            renderer.close()
                        }
                    } catch (e: SecurityException) {
                        encrypted = true
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    withContext(Dispatchers.Main) {
                        fileList = fileList.map {
                            if (it.id == file.id) {
                                it.copy(pages = pageCount, thumbnail = bitmap, isEncrypted = encrypted, isProcessed = true)
                            } else it
                        }
                    }
                }
            }
        }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        coroutineScope.launch(Dispatchers.IO) {
            val newFiles = uris.mapNotNull { uri ->
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    cursor.moveToFirst()

                    val name = cursor.getString(nameIndex) ?: "Unknown_PDF.pdf"
                    val sizeBytes = cursor.getLong(sizeIndex)
                    val sizeMb = sizeBytes / (1024.0 * 1024.0)

                    PdfFileItem(
                        name = name,
                        pages = 0,
                        sizeMb = Math.round(sizeMb * 100.0) / 100.0,
                        uri = uri
                    )
                }
            }
            withContext(Dispatchers.Main) {
                fileList = fileList + newFiles
            }
        }
    }

    val executeMerge = {
        val totalSizeMb = fileList.sumOf { it.sizeMb }
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val stat = StatFs(downloadsDir.path)
        val availableMb = (stat.availableBlocksLong * stat.blockSizeLong) / (1024.0 * 1024.0)

        if (totalSizeMb > availableMb) {
            coroutineScope.launch { snackbarHostState.showSnackbar("Not enough storage space available.") }
        } else {
            coroutineScope.launch(Dispatchers.IO) {
                isMerging = true
                try {
                    val merger = PDFMergerUtility()
                    val finalFileName = if (outputName.endsWith(".pdf", ignoreCase = true)) outputName else "$outputName.pdf"
                    val outputFile = File(downloadsDir, finalFileName)

                    merger.destinationFileName = outputFile.absolutePath

                    fileList.forEach { item ->
                        context.contentResolver.openInputStream(item.uri)?.let { inputStream ->
                            merger.addSource(inputStream)
                        }
                    }

                    merger.mergeDocuments(null)

                    withContext(Dispatchers.Main) {
                        fileList = emptyList()
                        val result = snackbarHostState.showSnackbar(
                            message = "Success! Saved to Downloads",
                            actionLabel = "OPEN",
                            duration = SnackbarDuration.Long
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", outputFile)
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/pdf")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Open PDF"))
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        snackbarHostState.showSnackbar("Merge failed: ${e.localizedMessage}")
                    }
                } finally {
                    withContext(Dispatchers.Main) {
                        isMerging = false
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = Surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { MergeTopBar(onNavigateBack) },
        bottomBar = {
            MergeBottomBar(
                isMerging = isMerging,
                canMerge = fileList.size >= 2 && fileList.none { it.isEncrypted },
                onMergeClick = { executeMerge() }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            FileQueueSection(
                // FIX 1: Elevate Z-Index here so dragged cards render OVER the SummaryStrip below
                modifier = Modifier.zIndex(1f),
                files = fileList,
                onClearAll = { fileList = emptyList() },
                onAddMore = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                onRemove = { fileToRemove ->
                    fileList = fileList.filter { it.id != fileToRemove.id }
                },
                onSwap = { from, to ->
                    val mutableList = fileList.toMutableList()
                    val item = mutableList.removeAt(from)
                    mutableList.add(to, item)
                    fileList = mutableList
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            SummaryStrip(files = fileList)

            Spacer(modifier = Modifier.height(16.dp))
            OutputConfiguration(
                outputName = outputName,
                onNameChange = { outputName = it },
                addToc = addToc,
                onTocToggle = { addToc = !addToc },
                normalizeSize = normalizeSize,
                onNormalizeToggle = { normalizeSize = !normalizeSize }
            )

            Spacer(modifier = Modifier.height(16.dp))
            SecurityBadge()

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun MergeTopBar(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceContainerLowest.copy(alpha = 0.9f))
            .shadow(elevation = 1.dp, spotColor = Color.Black.copy(alpha = 0.04f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable { onNavigateBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = OnSurface)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Merge PDF",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = OnSurface
        )
    }
}

@Composable
private fun FileQueueSection(
    modifier: Modifier = Modifier,
    files: List<PdfFileItem>,
    onClearAll: () -> Unit,
    onAddMore: () -> Unit,
    onRemove: (PdfFileItem) -> Unit,
    onSwap: (Int, Int) -> Unit
) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Source Files", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(SurfaceContainerHigh)
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text("${files.size}", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                }
            }
            if (files.isNotEmpty()) {
                Text(
                    text = "Clear All",
                    fontSize = 11.sp,
                    color = Primary,
                    modifier = Modifier.clickable { onClearAll() }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (files.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceContainerLowest)
                    .clickable { onAddMore() }
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(SurfaceContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.FolderOpen, contentDescription = null, tint = Primary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Queue is empty", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                    Text("Tap here to pick PDF documents from your storage", fontSize = 12.sp, color = OnSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        } else {
            // FIX 2: Elevate Z-Index here so dragged cards render OVER the "Add More PDFs" button
            Column(
                modifier = Modifier.zIndex(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                files.forEachIndexed { index, file ->
                    key(file.id) {
                        PdfFileCard(
                            file = file,
                            order = index + 1,
                            isLast = index == files.lastIndex,
                            onRemove = { onRemove(file) },
                            onSwapUp = { if (index > 0) onSwap(index, index - 1) },
                            onSwapDown = { if (index < files.lastIndex) onSwap(index, index + 1) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceContainerLowest)
                    .clickable { onAddMore() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Add, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add More PDFs", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Primary)
                }
            }
        }
    }
}

@Composable
private fun PdfFileCard(
    file: PdfFileItem,
    order: Int,
    isLast: Boolean,
    onRemove: () -> Unit,
    onSwapUp: () -> Unit,
    onSwapDown: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var dragOffsetY by remember { mutableStateOf(0f) }

    val currentOnSwapUp by rememberUpdatedState(onSwapUp)
    val currentOnSwapDown by rememberUpdatedState(onSwapDown)
    val currentIsLast by rememberUpdatedState(isLast)
    val currentOrder by rememberUpdatedState(order)

    val density = LocalDensity.current
    val cardHeightPx = remember(density) { with(density) { 80.dp.toPx() } }
    val swapThresholdPx = cardHeightPx * 0.65f

    val isDragging = dragOffsetY != 0f
    val elevation by animateDpAsState(targetValue = if (isDragging) 8.dp else 1.dp, label = "card_elevation")

    Row(
        modifier = Modifier
            .zIndex(if (isDragging) 1f else 0f)
            .offset { IntOffset(0, dragOffsetY.roundToInt()) }
            .fillMaxWidth()
            .shadow(elevation = elevation, shape = RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.05f))
            .background(SurfaceContainerLowest, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
                        onDrag = { change, dragAmount ->
                            change.consume()

                            // FIX 3: Apply boundary clamping. Card hits a physical "wall"
                            // preventing it from being dragged out of the list infinitely.
                            val topLimit = if (currentOrder == 1) -cardHeightPx * 0.15f else -Float.MAX_VALUE
                            val bottomLimit = if (currentIsLast) cardHeightPx * 0.15f else Float.MAX_VALUE

                            dragOffsetY = (dragOffsetY + dragAmount.y).coerceIn(topLimit, bottomLimit)

                            if (dragOffsetY > swapThresholdPx && !currentIsLast) {
                                currentOnSwapDown()
                                dragOffsetY -= cardHeightPx
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            } else if (dragOffsetY < -swapThresholdPx && currentOrder > 1) {
                                currentOnSwapUp()
                                dragOffsetY += cardHeightPx
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                        onDragEnd = { dragOffsetY = 0f },
                        onDragCancel = { dragOffsetY = 0f }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.DragIndicator, contentDescription = "Drag to reorder", tint = OutlineVariant, modifier = Modifier.size(24.dp))
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(if (file.isEncrypted) ErrorContainer.copy(alpha = 0.6f) else SurfaceContainer),
            contentAlignment = Alignment.Center
        ) {
            if (file.isEncrypted) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = Error, modifier = Modifier.size(20.dp))
                }
            } else if (file.thumbnail != null) {
                Image(
                    bitmap = file.thumbnail.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Primary, strokeWidth = 2.dp)
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(file.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = if (file.isEncrypted) Error else OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (file.isEncrypted) {
                    Text("Encrypted - Cannot Merge", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Error)
                } else {
                    if (file.isProcessed) {
                        Text("${file.pages} pages", fontSize = 12.sp, color = OnSurfaceVariant)
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(OutlineVariant))
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text("${file.sizeMb} MB", fontSize = 12.sp, color = OnSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onRemove() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = "Remove", tint = Outline, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(SurfaceContainer).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text("#$order", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Primary)
            }
        }
    }
}

@Composable
private fun SummaryStrip(files: List<PdfFileItem>) {
    val totalSize = files.sumOf { it.sizeMb }
    val totalPages = files.sumOf { it.pages }

    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.horizontalGradient(listOf(PrimaryContainer, SecondaryContainer)))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceContainerLowest.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.AllInbox, contentDescription = null, tint = OnPrimaryContainer)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("QUEUE ESTIMATION", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = OnPrimaryContainer.copy(alpha = 0.9f))
                    Text("${files.size} files · $totalPages pages · ~${"%.1f".format(totalSize)} MB", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OnPrimaryContainer)
                }
            }
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(SurfaceContainerLowest.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Speed, contentDescription = null, tint = OnPrimaryContainer, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun OutputConfiguration(
    outputName: String, onNameChange: (String) -> Unit,
    addToc: Boolean, onTocToggle: () -> Unit,
    normalizeSize: Boolean, onNormalizeToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .shadow(1.dp, RoundedCornerShape(16.dp))
            .background(SurfaceContainerLowest, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Output Configuration", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurface)
            Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(SurfaceContainer).padding(horizontal = 8.dp, vertical = 2.dp)) {
                Text("Standard PDF 1.7", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Primary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Output File Name", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant)
        Spacer(modifier = Modifier.height(6.dp))

        BasicTextField(
            value = outputName,
            onValueChange = onNameChange,
            textStyle = TextStyle(fontSize = 13.sp, color = OnSurface),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceContainerLow)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.EditNote, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) { innerTextField() }
                    Icon(Icons.Outlined.ModeEdit, contentDescription = null, tint = Outline, modifier = Modifier.size(18.dp))
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = SurfaceContainerHigh.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(16.dp))

        CustomToggleRow(
            icon = Icons.Outlined.ListAlt,
            title = "Add Table of Contents",
            subtitle = "Generate bookmarks from source file names automatically",
            isChecked = addToc,
            onToggle = onTocToggle
        )

        Spacer(modifier = Modifier.height(16.dp))

        CustomToggleRow(
            icon = Icons.Outlined.AspectRatio,
            title = "Normalize Page Size",
            subtitle = "Auto-scale all varied sheets into unified A4 dimensions",
            isChecked = normalizeSize,
            onToggle = onNormalizeToggle
        )
    }
}

@Composable
private fun CustomToggleRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, isChecked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onToggle() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Row(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceContainer), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                Text(subtitle, fontSize = 12.sp, color = OnSurfaceVariant, lineHeight = 16.sp)
            }
        }

        val offset by animateFloatAsState(targetValue = if (isChecked) 20f else 0f, label = "")
        val bgColor = if (isChecked) Primary else SurfaceContainerHighest

        Box(
            modifier = Modifier
                .width(44.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(50))
                .background(bgColor)
                .padding(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .offset(x = offset.dp)
                    .clip(CircleShape)
                    .background(SurfaceContainerLowest)
                    .shadow(1.dp, CircleShape)
            )
        }
    }
}

@Composable
private fun SecurityBadge() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Security, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Zero Server Uploads • Encrypted sandboxed memory", fontSize = 10.sp, color = OnSurfaceVariant.copy(alpha = 0.7f))
    }
}

@Composable
private fun MergeBottomBar(isMerging: Boolean, canMerge: Boolean, onMergeClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceContainerLowest.copy(alpha = 0.9f))
            .shadow(elevation = 16.dp, spotColor = Color.Black.copy(alpha = 0.06f))
    ) {
        if (isMerging) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Primary, trackColor = SurfaceContainerHigh)
        }

        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (canMerge) (if (isMerging) Primary else PrimaryContainer) else SurfaceContainerHigh)
                .clickable(enabled = canMerge && !isMerging) { onMergeClick() },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isMerging) {
                    Text("Processing documents...", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnPrimary)
                } else {
                    Icon(Icons.Outlined.MergeType, contentDescription = null, tint = if (canMerge) OnPrimary else OutlineVariant, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Merge & Save PDF", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (canMerge) OnPrimary else OutlineVariant)
                }
            }
        }
    }
}