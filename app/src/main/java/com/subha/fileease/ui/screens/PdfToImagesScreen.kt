package com.subha.fileease.ui.screens

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

// Exact Tailwind Color Tokens
private val Surface = Color(0xFFFAF8FF)
private val SurfaceContainerLowest = Color(0xFFFFFFFF)
private val SurfaceContainerLow = Color(0xFFF2F3FF)
private val SurfaceContainer = Color(0xFFEAEDFF)
private val SurfaceContainerHigh = Color(0xFFE2E7FF)
private val SurfaceVariant = Color(0xFFDAE2FD)
private val OnSurface = Color(0xFF131B2E)
private val OnSurfaceVariant = Color(0xFF424656)
private val Primary = Color(0xFF0050CB)
private val OnPrimary = Color(0xFFFFFFFF)
private val PrimaryContainer = Color(0xFF0066FF)
private val OnPrimaryContainer = Color(0xFFF8F7FF)
private val PrimaryFixed = Color(0xFFDAE1FF)
private val Error = Color(0xFFBA1A1A)
private val ErrorContainer = Color(0xFFFFDAD6)
private val Outline = Color(0xFF727687)
private val OutlineVariant = Color(0xFFC2C6D8)
private val Secondary = Color(0xFF0058BE)

data class LoadedPdf(
    val uri: Uri,
    val name: String,
    val sizeMb: String,
    val pageCount: Int
)

@Composable
fun PdfToImagesScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Document State
    var pdfDocument by remember { mutableStateOf<LoadedPdf?>(null) }
    val pageThumbnails = remember { mutableStateMapOf<Int, Bitmap>() }
    var isRenderingThumbnails by remember { mutableStateOf(false) }

    // Settings State
    var selectedScope by remember { mutableStateOf("All Pages") }
    var customRange by remember { mutableStateOf("") }
    val selectedPages = remember { mutableStateListOf<Int>() }
    var isGridExpanded by remember { mutableStateOf(false) }

    var selectedFormat by remember { mutableStateOf("PNG") }
    var selectedDpi by remember { mutableStateOf(300) }

    var saveAsZip by remember { mutableStateOf(true) }
    var saveToGallery by remember { mutableStateOf(false) }

    // Engine Execution State
    var isExtracting by remember { mutableStateOf(false) }
    var isExtractionComplete by remember { mutableStateOf(false) }

    // Smart Range Parser
    LaunchedEffect(customRange, selectedScope) {
        if (selectedScope == "Page Range" && pdfDocument != null) {
            val parsedSelection = mutableSetOf<Int>()
            val parts = customRange.split(",")
            for (part in parts) {
                val p = part.trim()
                if (p.contains("-")) {
                    val bounds = p.split("-")
                    if (bounds.size == 2) {
                        val start = bounds[0].toIntOrNull()
                        val end = bounds[1].toIntOrNull()
                        if (start != null && end != null && start <= end) {
                            parsedSelection.addAll(start..end)
                        }
                    }
                } else {
                    p.toIntOrNull()?.let { parsedSelection.add(it) }
                }
            }
            val validSelection = parsedSelection.filter { it in 1..pdfDocument!!.pageCount }
            selectedPages.clear()
            selectedPages.addAll(validSelection)
        }
    }

    // Native PDF Picker Launcher
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                isRenderingThumbnails = true
                var name = "Document.pdf"
                var sizeBytes = 0L

                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        name = cursor.getString(nameIdx) ?: "Document.pdf"
                        sizeBytes = cursor.getLong(sizeIdx)
                    }
                }
                val sizeMb = "%.1f MB".format(sizeBytes / (1024.0 * 1024.0))

                var pageCount = 0
                try {
                    val fd = context.contentResolver.openFileDescriptor(uri, "r")
                    if (fd != null) {
                        val renderer = PdfRenderer(fd)
                        pageCount = renderer.pageCount

                        withContext(Dispatchers.Main) {
                            pageThumbnails.clear()
                            selectedPages.clear()
                            selectedPages.addAll(1..pageCount)
                            selectedScope = "All Pages"
                            customRange = ""
                            isGridExpanded = false
                        }

                        for (i in 0 until pageCount) {
                            val page = renderer.openPage(i)
                            val ratio = page.height.toFloat() / page.width.toFloat()
                            val bitmap = Bitmap.createBitmap(300, (300 * ratio).toInt(), Bitmap.Config.ARGB_8888)

                            val canvas = Canvas(bitmap)
                            canvas.drawColor(android.graphics.Color.WHITE)

                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            page.close()

                            withContext(Dispatchers.Main) { pageThumbnails[i + 1] = bitmap }
                        }
                        renderer.close()
                        fd.close()
                    }
                } catch (e: Exception) { e.printStackTrace() }

                withContext(Dispatchers.Main) {
                    pdfDocument = LoadedPdf(uri, name, sizeMb, pageCount)
                    isRenderingThumbnails = false
                }
            }
        }
    }

    Scaffold(
        containerColor = Surface,
        topBar = { PdfToImagesTopBar(onNavigateBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            PdfToImagesBottomBar(
                selectedCount = selectedPages.size,
                format = selectedFormat,
                dpi = selectedDpi,
                isProcessing = isExtracting,
                isComplete = isExtractionComplete,
                onExtract = {
                    if (pdfDocument != null && selectedPages.isNotEmpty()) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        coroutineScope.launch(Dispatchers.IO) {
                            isExtracting = true
                            try {
                                val fd = context.contentResolver.openFileDescriptor(pdfDocument!!.uri, "r")
                                if (fd != null) {
                                    val renderer = PdfRenderer(fd)
                                    val formatExt = selectedFormat.lowercase()
                                    val compressFormat = when (selectedFormat) {
                                        "PNG" -> Bitmap.CompressFormat.PNG
                                        "WEBP" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY else @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP
                                        else -> Bitmap.CompressFormat.JPEG
                                    }
                                    val quality = if (selectedFormat == "PNG") 100 else 90

                                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

                                    // FIX: Dynamic Naming Logic
                                    val baseName = pdfDocument!!.name.removeSuffix(".pdf").ifEmpty { "PDF_to_Images" }

                                    var zipOut: ZipOutputStream? = null
                                    if (saveAsZip) {
                                        // FIX: Use the new baseName or default to PDF_to_Images.zip if name is blank
                                        val zipFileName = if (pdfDocument!!.name.isNotBlank()) "${baseName}_images.zip" else "PDF_to_Images.zip"
                                        val zipFile = File(downloadsDir, zipFileName)
                                        zipOut = ZipOutputStream(FileOutputStream(zipFile))
                                    }

                                    selectedPages.sorted().forEach { pageNum ->
                                        val pageIdx = pageNum - 1
                                        if (pageIdx in 0 until renderer.pageCount) {
                                            val page = renderer.openPage(pageIdx)
                                            val widthPx = ((page.width / 72f) * selectedDpi).toInt()
                                            val heightPx = ((page.height / 72f) * selectedDpi).toInt()

                                            val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
                                            val canvas = Canvas(bitmap)
                                            canvas.drawColor(android.graphics.Color.WHITE)

                                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                                            page.close()

                                            val bos = ByteArrayOutputStream()
                                            bitmap.compress(compressFormat, quality, bos)
                                            val bitmapData = bos.toByteArray()
                                            bitmap.recycle()

                                            val fileName = "${baseName}_page_$pageNum.$formatExt"

                                            if (saveAsZip) {
                                                zipOut?.putNextEntry(ZipEntry(fileName))
                                                zipOut?.write(bitmapData)
                                                zipOut?.closeEntry()
                                            }

                                            if (saveToGallery) {
                                                val values = ContentValues().apply {
                                                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                                                    put(MediaStore.Images.Media.MIME_TYPE, "image/$formatExt")
                                                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FileEase")
                                                }
                                                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                                                uri?.let { destUri ->
                                                    context.contentResolver.openOutputStream(destUri)?.use { outStream ->
                                                        outStream.write(bitmapData)
                                                    }
                                                }
                                            }

                                            if (!saveAsZip && !saveToGallery) {
                                                val rawFile = File(downloadsDir, fileName)
                                                FileOutputStream(rawFile).use { it.write(bitmapData) }
                                            }
                                        }
                                    }
                                    zipOut?.close()
                                    renderer.close()
                                    fd.close()

                                    withContext(Dispatchers.Main) {
                                        isExtractionComplete = true
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 80, 100, 120), intArrayOf(0, 150, 0, 255), -1))
                                        } else {
                                            @Suppress("DEPRECATION")
                                            vibrator.vibrate(longArrayOf(0, 80, 100, 120), -1)
                                        }
                                        snackbarHostState.showSnackbar("Extraction Complete!")
                                        delay(2000)
                                        isExtractionComplete = false
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    snackbarHostState.showSnackbar("Failed: ${e.localizedMessage}")
                                }
                            } finally {
                                withContext(Dispatchers.Main) { isExtracting = false }
                            }
                        }
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // PDF File Source Card
            if (pdfDocument == null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(1.dp, RoundedCornerShape(12.dp))
                        .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            pdfPickerLauncher.launch("application/pdf")
                        }
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(PrimaryContainer.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.UploadFile, contentDescription = null, tint = Primary, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Select PDF Document", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                    Text("Tap to browse files on your device", fontSize = 12.sp, color = OnSurfaceVariant)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(1.dp, RoundedCornerShape(12.dp))
                        .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(ErrorContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, tint = Error, modifier = Modifier.size(26.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(pdfDocument!!.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                    Text("${pdfDocument!!.pageCount} pages", fontSize = 11.sp, color = OnSurfaceVariant)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(OutlineVariant))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(pdfDocument!!.sizeMb, fontSize = 11.sp, color = OnSurfaceVariant)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(OutlineVariant))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(PrimaryFixed).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                        Text("Ready", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                                    }
                                }
                            }
                        }
                        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(SurfaceContainer).clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            pdfPickerLauncher.launch("application/pdf")
                        }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text("Change", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.VerifiedUser, contentDescription = null, tint = Primary, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Local Engine • 0 KB network used", fontSize = 10.sp, color = OnSurfaceVariant)
                        }
                        Text("Native Android Renderer", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                    }
                }
            }

            AnimatedVisibility(visible = pdfDocument != null) {
                Column {
                    Spacer(modifier = Modifier.height(24.dp))

                    // Extraction Scope Section
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Extraction Scope", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                        Text("Select All (${pdfDocument?.pageCount ?: 0})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PrimaryContainer, modifier = Modifier.clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedScope = "All Pages"
                            selectedPages.clear()
                            pdfDocument?.let { doc -> selectedPages.addAll(1..doc.pageCount) }
                        })
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    SegmentControlRow(
                        options = listOf("All Pages", "Page Range", "Selected Only"),
                        selected = selectedScope,
                        onSelect = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedScope = it
                            if (it == "All Pages") {
                                selectedPages.clear()
                                pdfDocument?.let { doc -> selectedPages.addAll(1..doc.pageCount) }
                            }
                        }
                    )

                    AnimatedVisibility(visible = selectedScope == "Page Range") {
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp).shadow(1.dp, RoundedCornerShape(12.dp)).background(SurfaceContainerLowest, RoundedCornerShape(12.dp)).padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Specify custom pages", fontSize = 11.sp, color = OnSurfaceVariant)
                                Text("e.g. 1-5, 8, 12", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceContainerLow).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.EditNote, contentDescription = null, tint = Outline, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                BasicTextField(
                                    value = customRange,
                                    onValueChange = { customRange = it },
                                    textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Visual Page Selector
                    Column(modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(12.dp)).background(SurfaceContainerLowest, RoundedCornerShape(12.dp)).padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Visual Page Selector", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                            Text("${selectedPages.size} of ${pdfDocument?.pageCount ?: 0} selected", fontSize = 10.sp, color = OnSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        if (isRenderingThumbnails) {
                            Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Primary, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                            }
                        } else {
                            val allPages = (1..(pdfDocument?.pageCount ?: 0)).toList()
                            val pagesToDisplay = if (isGridExpanded) allPages else allPages.take(6)

                            Column(modifier = Modifier.animateContentSize()) {
                                pagesToDisplay.chunked(3).forEach { row ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        row.forEach { pageNum ->
                                            val isSelected = selectedPages.contains(pageNum)
                                            val thumbnail = pageThumbnails[pageNum]

                                            Column(
                                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(SurfaceContainerLow).clickable {
                                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    if (isSelected) selectedPages.remove(pageNum) else selectedPages.add(pageNum)
                                                    selectedScope = "Selected Only"
                                                }.padding(6.dp)
                                            ) {
                                                Box(modifier = Modifier.fillMaxWidth().aspectRatio(0.75f).clip(RoundedCornerShape(4.dp)).background(SurfaceContainer)) {
                                                    if (thumbnail != null) {
                                                        Image(bitmap = thumbnail.asImageBitmap(), contentDescription = "Page $pageNum", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                                    } else {
                                                        Icon(Icons.Outlined.Description, contentDescription = null, tint = OutlineVariant, modifier = Modifier.align(Alignment.Center).size(24.dp))
                                                    }

                                                    if (isSelected) {
                                                        Box(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(20.dp).clip(CircleShape).background(PrimaryContainer).shadow(1.dp), contentAlignment = Alignment.Center) {
                                                            Icon(Icons.Filled.Check, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(14.dp))
                                                        }
                                                    }
                                                }
                                                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                                    Text("Page $pageNum", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                                                }
                                            }
                                        }
                                        if (row.size < 3) {
                                            repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }

                            if (allPages.size > 6) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SurfaceContainerHigh.copy(alpha = 0.6f))
                                        .clickable {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            isGridExpanded = !isGridExpanded
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(if (isGridExpanded) "Show less" else "Show all ${allPages.size} thumbnail previews", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(if (isGridExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Export Image Format
                    Text("Export Image Format", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface, modifier = Modifier.padding(bottom = 8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FormatCard(modifier = Modifier.weight(1f), format = "PNG", subtitle = "Lossless", icon = Icons.Filled.Verified, isSelected = selectedFormat == "PNG") {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); selectedFormat = "PNG"
                        }
                        FormatCard(modifier = Modifier.weight(1f), format = "JPG", subtitle = "Compact", icon = Icons.Outlined.Image, isSelected = selectedFormat == "JPG") {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); selectedFormat = "JPG"
                        }
                        FormatCard(modifier = Modifier.weight(1f), format = "WEBP", subtitle = "Web Opt.", icon = Icons.Outlined.Public, isSelected = selectedFormat == "WEBP") {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); selectedFormat = "WEBP"
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // DPI Settings
                    Column(modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(12.dp)).background(SurfaceContainerLowest, RoundedCornerShape(12.dp)).padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.GridOn, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Resolution & DPI Preset", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                            }
                            Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(PrimaryFixed).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                Text("$selectedDpi DPI", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        SegmentControlRow(
                            options = listOf("Screen (72)", "Standard (150)", "Ultra (300)"),
                            selected = when(selectedDpi) { 300 -> "Ultra (300)"; 150 -> "Standard (150)"; else -> "Screen (72)" },
                            onSelect = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedDpi = when(it) {
                                    "Ultra (300)" -> 300
                                    "Standard (150)" -> 150
                                    else -> 72
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        val dimSummary = when(selectedDpi) { 300 -> "2480 × 3508 px / page"; 150 -> "1240 × 1754 px / page"; else -> "595 × 842 px / page" }
                        val dpiProgress by animateFloatAsState(targetValue = when(selectedDpi) { 300 -> 1f; 150 -> 0.6f; else -> 0.25f }, label = "progress")

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Target Render Dimensions", fontSize = 10.sp, color = OnSurfaceVariant)
                            Text(dimSummary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(SurfaceContainer)) {
                            Box(modifier = Modifier.fillMaxWidth(dpiProgress).fillMaxHeight().clip(RoundedCornerShape(50)).background(PrimaryContainer))
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Packaging & Destination
                    Text("Packaging & Destination", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface, modifier = Modifier.padding(bottom = 8.dp))
                    Column(modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(12.dp)).background(SurfaceContainerLowest, RoundedCornerShape(12.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // FIX: Updated Subtitle UI Text to match the backend logic changes
                        PackagingRow(
                            title = "Save as single ZIP archive",
                            subtitle = if (pdfDocument?.name?.isNotBlank() == true) "Bundles into ${pdfDocument!!.name.removeSuffix(".pdf")}_images.zip" else "Bundles all pages into PDF_to_Images.zip",
                            icon = Icons.Outlined.FolderZip,
                            iconTint = Primary,
                            isChecked = saveAsZip,
                            onCheckedChange = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); saveAsZip = it }
                        )
                        PackagingRow(
                            title = "Save individually to Photos",
                            subtitle = "Write extracted frames to gallery album",
                            icon = Icons.Outlined.PhotoLibrary,
                            iconTint = OnSurfaceVariant,
                            isChecked = saveToGallery,
                            onCheckedChange = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); saveToGallery = it }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
private fun PdfToImagesTopBar(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface.copy(alpha = 0.90f))
            .shadow(elevation = 1.dp, spotColor = Color.Black.copy(alpha = 0.04f))
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).clickable { onNavigateBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = OnSurface)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text("PDF To Images", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = OnSurface, letterSpacing = (-0.01).sp)
    }
}

@Composable
private fun SegmentControlRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceContainer).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { option ->
            val isSelected = selected == option
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) SurfaceContainerLowest else Color.Transparent)
                    .shadow(if (isSelected) 1.dp else 0.dp, RoundedCornerShape(6.dp))
                    .clickable { onSelect(option) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    fontSize = 13.sp,
                    fontWeight = if(isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if(isSelected) PrimaryContainer else OnSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun FormatCard(modifier: Modifier, format: String, subtitle: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) PrimaryContainer else SurfaceContainerLowest
    val contentColor = if (isSelected) OnPrimaryContainer else OnSurface
    val subColor = if (isSelected) OnPrimaryContainer.copy(alpha = 0.9f) else OnSurfaceVariant
    val iconColor = if (isSelected) OnPrimaryContainer else Outline

    Column(
        modifier = modifier
            .shadow(1.dp, RoundedCornerShape(12.dp))
            .background(bgColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(format, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = contentColor)
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(subtitle, fontSize = 10.sp, color = subColor)
    }
}

@Composable
private fun PackagingRow(title: String, subtitle: String, icon: ImageVector, iconTint: Color, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onCheckedChange(!isChecked) }.padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceContainer), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                Text(subtitle, fontSize = 10.sp, color = OnSurfaceVariant)
            }
        }
        CustomSwitch(checked = isChecked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun CustomSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val bg by animateColorAsState(if (checked) PrimaryContainer else SurfaceVariant, label = "switchBg")
    val offset by animateDpAsState(if (checked) 20.dp else 0.dp, label = "switchOffset")

    Box(
        modifier = Modifier
            .width(48.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable { onCheckedChange(!checked) }
            .padding(4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = offset)
                .size(20.dp)
                .shadow(1.dp, CircleShape)
                .background(SurfaceContainerLowest, CircleShape)
        )
    }
}

@Composable
private fun PdfToImagesBottomBar(
    selectedCount: Int,
    format: String,
    dpi: Int,
    isProcessing: Boolean,
    isComplete: Boolean,
    onExtract: () -> Unit
) {
    val estSize = when (format) {
        "PNG" -> if (dpi == 300) 1.4 else if (dpi == 150) 0.7 else 0.25
        "JPG" -> if (dpi == 300) 0.6 else if (dpi == 150) 0.35 else 0.15
        else -> if (dpi == 300) 0.45 else if (dpi == 150) 0.25 else 0.12
    }
    val totalMB = Math.max(1, (selectedCount * estSize).toInt())

    val buttonText = when {
        isProcessing -> "Extracting Pages..."
        isComplete -> "Extraction Complete!"
        selectedCount == 0 -> "Select pages to extract"
        else -> "Extract ${if(selectedCount < 18) "$selectedCount " else ""}Pages as $format"
    }

    val buttonIcon = when {
        isProcessing -> Icons.Outlined.Refresh
        isComplete -> Icons.Filled.CheckCircle
        else -> Icons.Outlined.PhotoLibrary
    }

    val buttonColor = if (isComplete) Secondary else if (selectedCount > 0) PrimaryContainer else SurfaceContainerHigh
    val contentColor = if (selectedCount > 0 || isComplete) OnPrimaryContainer else OutlineVariant
    val cornerRadius by animateDpAsState(targetValue = if (isProcessing) 50.dp else 12.dp, label = "buttonShape")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceContainerLowest.copy(alpha = 0.95f))
            .shadow(elevation = 24.dp, spotColor = OnSurface.copy(alpha = 0.08f))
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Ready to extract", fontSize = 10.sp, color = OnSurfaceVariant)
                Text("$selectedCount images · ~$totalMB MB estimated", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Primary)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .shadow(if (!isComplete && selectedCount > 0 && !isProcessing) 4.dp else 0.dp, RoundedCornerShape(cornerRadius), spotColor = PrimaryContainer.copy(alpha = 0.5f))
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(if(selectedCount == 0) buttonColor.copy(alpha = 0.5f) else buttonColor)
                    .clickable(enabled = selectedCount > 0 && !isProcessing && !isComplete) { onExtract() },
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = Pair(isProcessing, isComplete),
                    label = "buttonContent"
                ) { states ->
                    val (processing, _) = states
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (processing) {
                            CircularProgressIndicator(color = contentColor, modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                        } else {
                            Icon(buttonIcon, contentDescription = null, tint = contentColor, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(buttonText, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = contentColor)
                    }
                }
            }
        }
    }
}