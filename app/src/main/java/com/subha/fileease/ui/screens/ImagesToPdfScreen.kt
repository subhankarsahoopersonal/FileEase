package com.subha.fileease.ui.screens

import android.R.attr.translationZ
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

// Exact Tailwind Color Tokens
private val Surface = Color(0xFFFAF8FF)
private val SurfaceContainerLowest = Color(0xFFFFFFFF)
private val SurfaceContainerLow = Color(0xFFF2F3FF)
private val SurfaceContainer = Color(0xFFEAEDFF)
private val SurfaceContainerHigh = Color(0xFFE2E7FF)
private val OnSurface = Color(0xFF131B2E)
private val OnSurfaceVariant = Color(0xFF424656)
private val Primary = Color(0xFF0050CB)
private val OnPrimary = Color(0xFFFFFFFF)
private val PrimaryContainer = Color(0xFF0066FF)
private val OnPrimaryContainer = Color(0xFFF8F7FF)
private val PrimaryFixed = Color(0xFFDAE1FF)
private val OnPrimaryFixed = Color(0xFF001849)
private val Error = Color(0xFFBA1A1A)
private val ErrorContainer = Color(0xFFFFDAD6)
private val Outline = Color(0xFF727687)
private val OutlineVariant = Color(0xFFC2C6D8)
private val Secondary = Color(0xFF0058BE)

data class QueuedImage(
    val id: String,
    val uri: Uri,
    val name: String,
    val dimensions: String,
    val sizeBytes: Long,
    var rotation: Float = 0f
)

@Composable
fun ImagesToPdfScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val nameFocusRequester = remember { FocusRequester() }

    var fileName by remember { mutableStateOf("Image_to_PDF_${System.currentTimeMillis() / 1000}") }
    var isProcessing by remember { mutableStateOf(false) }
    var isComplete by remember { mutableStateOf(false) }

    val imageQueue = remember { mutableStateListOf<QueuedImage>() }

    var selectedPageSize by remember { mutableStateOf("A4") }
    var selectedOrientation by remember { mutableStateOf("Auto") }
    var selectedMargins by remember { mutableStateOf("Compact (5mm)") }
    var selectedFit by remember { mutableStateOf("Fit Entire") }
    var selectedDpi by remember { mutableStateOf(150) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch(Dispatchers.IO) {
                val newImages = uris.mapNotNull { uri ->
                    var name = "Unknown_Image.jpg"
                    var sizeBytes = 0L
                    try {
                        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                            if (cursor.moveToFirst()) {
                                name = cursor.getString(nameIdx) ?: "Unknown_Image.jpg"
                                sizeBytes = cursor.getLong(sizeIdx)
                            }
                        }
                    } catch (e: Exception) { e.printStackTrace() }

                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    try {
                        context.contentResolver.openInputStream(uri)?.use {
                            BitmapFactory.decodeStream(it, null, options)
                        }
                    } catch (e: Exception) { e.printStackTrace() }

                    val width = options.outWidth
                    val height = options.outHeight
                    val dimensions = if (width > 0 && height > 0) "$width × $height" else "Unknown dims"

                    QueuedImage(
                        id = UUID.randomUUID().toString(),
                        uri = uri,
                        name = name,
                        dimensions = dimensions,
                        sizeBytes = sizeBytes
                    )
                }
                withContext(Dispatchers.Main) {
                    imageQueue.addAll(newImages)
                }
            }
        }
    }

    Scaffold(
        containerColor = Surface,
        topBar = { ImagesToPdfTopBar(onNavigateBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            val totalBytes = imageQueue.sumOf { it.sizeBytes }
            ImagesToPdfBottomBar(
                imageCount = imageQueue.size,
                totalBytes = totalBytes,
                isProcessing = isProcessing,
                isComplete = isComplete,
                onConvert = {
                    if (imageQueue.isNotEmpty()) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        coroutineScope.launch {
                            isProcessing = true
                            var success = false
                            var finalOutputFile: File? = null
                            var errorMessage = "Unknown Error"

                            withContext(Dispatchers.IO) {
                                try {
                                    val document = PDDocument()
                                    val cleanFileName = fileName.trim().ifEmpty { "Generated_PDF" }.removeSuffix(".pdf")
                                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                    val outputFile = File(downloadsDir, "${cleanFileName}.pdf")

                                    val marginPt = when (selectedMargins) {
                                        "Compact (5mm)" -> 14.17f
                                        "Std (12mm)" -> 34.01f
                                        else -> 0f
                                    }

                                    imageQueue.forEach { queuedImage ->
                                        val options = BitmapFactory.Options()
                                        if (selectedDpi == 150) options.inSampleSize = 2

                                        var bitmap = context.contentResolver.openInputStream(queuedImage.uri)?.use { stream ->
                                            BitmapFactory.decodeStream(stream, null, options)
                                        } ?: return@forEach

                                        if (queuedImage.rotation != 0f) {
                                            val matrix = Matrix().apply { postRotate(queuedImage.rotation) }
                                            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                                            if (rotated != bitmap) {
                                                bitmap.recycle()
                                                bitmap = rotated
                                            }
                                        }

                                        var pageW = PDRectangle.A4.width
                                        var pageH = PDRectangle.A4.height
                                        when (selectedPageSize) {
                                            "US Letter" -> { pageW = PDRectangle.LETTER.width; pageH = PDRectangle.LETTER.height }
                                            "Fit Image", "Original" -> { pageW = bitmap.width.toFloat(); pageH = bitmap.height.toFloat() }
                                        }

                                        val isImgLandscape = bitmap.width > bitmap.height
                                        val isPageLandscape = when (selectedOrientation) {
                                            "Landscape" -> true
                                            "Portrait" -> false
                                            else -> isImgLandscape
                                        }

                                        if (isPageLandscape && pageW < pageH) { val tmp = pageW; pageW = pageH; pageH = tmp }
                                        else if (!isPageLandscape && pageW > pageH) { val tmp = pageW; pageW = pageH; pageH = tmp }

                                        val page = PDPage(PDRectangle(pageW, pageH))
                                        document.addPage(page)

                                        val compressionQuality = if (selectedDpi == 300) 0.95f else 0.75f
                                        val pdImage = JPEGFactory.createFromImage(document, bitmap, compressionQuality)
                                        val contentStream = PDPageContentStream(document, page)

                                        val canvasW = pageW - (marginPt * 2)
                                        val canvasH = pageH - (marginPt * 2)
                                        val imgRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                                        val canvasRatio = canvasW / canvasH

                                        var drawW = canvasW
                                        var drawH = canvasH
                                        var startX = marginPt
                                        var startY = marginPt

                                        when (selectedFit) {
                                            "Fit Entire" -> {
                                                if (imgRatio > canvasRatio) {
                                                    drawH = drawW / imgRatio
                                                    startY += (canvasH - drawH) / 2f
                                                } else {
                                                    drawW = drawH * imgRatio
                                                    startX += (canvasW - drawW) / 2f
                                                }
                                            }
                                            "Fill Page" -> {
                                                if (imgRatio > canvasRatio) {
                                                    drawW = drawH * imgRatio
                                                    startX -= (drawW - canvasW) / 2f
                                                } else {
                                                    drawH = drawW / imgRatio
                                                    startY -= (drawH - canvasH) / 2f
                                                }
                                            }
                                            "Center" -> {
                                                drawW = bitmap.width * 0.75f
                                                drawH = bitmap.height * 0.75f
                                                if (drawW > canvasW || drawH > canvasH) {
                                                    if (imgRatio > canvasRatio) { drawH = canvasW / imgRatio; drawW = canvasW }
                                                    else { drawW = canvasH * imgRatio; drawH = canvasH }
                                                }
                                                startX = marginPt + (canvasW - drawW) / 2f
                                                startY = marginPt + (canvasH - drawH) / 2f
                                            }
                                        }

                                        contentStream.drawImage(pdImage, startX, startY, drawW, drawH)
                                        contentStream.close()
                                        bitmap.recycle()
                                    }

                                    document.save(outputFile)
                                    document.close()
                                    finalOutputFile = outputFile
                                    success = true
                                } catch (e: Exception) {
                                    errorMessage = e.localizedMessage ?: "Unknown processing error"
                                }
                            }

                            isProcessing = false
                            if (success && finalOutputFile != null) {
                                isComplete = true
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 80, 100, 120), intArrayOf(0, 150, 0, 255), -1))
                                } else {
                                    @Suppress("DEPRECATION")
                                    vibrator.vibrate(longArrayOf(0, 80, 100, 120), -1)
                                }

                                val result = snackbarHostState.showSnackbar(
                                    message = "Success! PDF saved to Downloads",
                                    actionLabel = "OPEN",
                                    duration = SnackbarDuration.Long
                                )

                                if (result == SnackbarResult.ActionPerformed) {
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", finalOutputFile!!)
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "application/pdf")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Open File"))
                                }
                                delay(2000)
                                isComplete = false
                                imageQueue.clear()
                            } else {
                                snackbarHostState.showSnackbar("Conversion failed: $errorMessage")
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
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Document Filename Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(1.dp, RoundedCornerShape(12.dp), spotColor = Color.Black.copy(alpha = 0.04f))
                        .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("FILE OUTPUT NAME", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant, letterSpacing = 0.5.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BasicTextField(
                                    value = fileName,
                                    onValueChange = { fileName = it },
                                    textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface),
                                    modifier = Modifier.weight(1f, fill = false).focusRequester(nameFocusRequester),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                                )
                                Text(".pdf", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Outline)
                            }
                        }
                    }
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        nameFocusRequester.requestFocus()
                    }.padding(4.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }

                // Source Queue Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Source Queue", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(PrimaryFixed).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                Text("${imageQueue.size} files", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = OnPrimaryFixed)
                            }
                        }
                        Text("Clear All", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Error, modifier = Modifier.clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            imageQueue.clear()
                        }.padding(4.dp))
                    }

                    AnimatedVisibility(
                        visible = imageQueue.isEmpty(),
                        enter = fadeIn() + slideInVertically { -it / 2 },
                        exit = fadeOut()
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceContainer).padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Queue is empty. Tap \"+ Add More Images\" below to start.", fontSize = 12.sp, color = OnSurfaceVariant, textAlign = TextAlign.Center)
                        }
                    }

                    // Drag and Drop State Holders
                    var draggedId by remember { mutableStateOf<String?>(null) }
                    var dragOffsetY by remember { mutableFloatStateOf(0f) }
                    val itemHeightPx = with(LocalDensity.current) { 80.dp.toPx() }

                    // Active Sorting Queue
                    imageQueue.forEachIndexed { _, img ->
                        key(img.id) {
                            val isDragged = draggedId == img.id
                            val offsetY = if (isDragged) dragOffsetY else 0f
                            val currentIndex = imageQueue.indexOfFirst { it.id == img.id }

                            QueueItemRow(
                                item = img,
                                index = currentIndex + 1,
                                isDragged = isDragged,
                                offsetY = offsetY,
                                onRotate = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val idx = imageQueue.indexOfFirst { it.id == img.id }
                                    if (idx != -1) imageQueue[idx] = img.copy(rotation = (img.rotation + 90f) % 360f)
                                },
                                onDelete = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    imageQueue.remove(img)
                                },
                                onDragStart = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    draggedId = img.id
                                    dragOffsetY = 0f
                                },
                                onDrag = { dragAmount ->
                                    dragOffsetY += dragAmount
                                    if (dragOffsetY > itemHeightPx * 0.65f) { // Dragged down
                                        val idx = imageQueue.indexOfFirst { it.id == img.id }
                                        if (idx != -1 && idx < imageQueue.size - 1) {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            val temp = imageQueue[idx]
                                            imageQueue[idx] = imageQueue[idx + 1]
                                            imageQueue[idx + 1] = temp
                                            dragOffsetY -= itemHeightPx // Seamlessly correct visual offset
                                        }
                                    } else if (dragOffsetY < -itemHeightPx * 0.65f) { // Dragged up
                                        val idx = imageQueue.indexOfFirst { it.id == img.id }
                                        if (idx > 0) {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            val temp = imageQueue[idx]
                                            imageQueue[idx] = imageQueue[idx - 1]
                                            imageQueue[idx - 1] = temp
                                            dragOffsetY += itemHeightPx // Seamlessly correct visual offset
                                        }
                                    }
                                },
                                onDragEnd = {
                                    draggedId = null
                                    dragOffsetY = 0f
                                }
                            )
                        }
                    }
                }

                // Add More Images Action Tile
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceContainer)
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            photoPickerLauncher.launch("image/*")
                        }
                        .padding(vertical = 14.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add More Images", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                }

                // Page Layout Settings Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(1.dp, RoundedCornerShape(12.dp), spotColor = Color.Black.copy(alpha = 0.04f))
                        .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.AutoStories, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Page Layout Settings", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                        }
                        Text("Sheet & Frame", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = OnSurfaceVariant)
                    }

                    SegmentControlRow("PAGE SIZE", listOf("A4", "US Letter", "Fit Image", "Original"), selectedPageSize) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedPageSize = it
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("ORIENTATION", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant, letterSpacing = 0.5.sp)
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceContainer).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val options = listOf(
                                Triple("Auto", Icons.Outlined.Sync, "Auto"),
                                Triple("Portrait", Icons.Outlined.Smartphone, "Portrait"),
                                Triple("Landscape", Icons.Outlined.StayCurrentLandscape, "Landscape")
                            )
                            options.forEach { (label, icon, value) ->
                                val isSelected = selectedOrientation == value
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) Primary else Color.Transparent)
                                        .clickable {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedOrientation = value
                                        }
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(icon, contentDescription = null, tint = if(isSelected) OnPrimary else OnSurfaceVariant, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(label, fontSize = 11.sp, fontWeight = if(isSelected) FontWeight.SemiBold else FontWeight.Medium, color = if(isSelected) OnPrimary else OnSurfaceVariant)
                                }
                            }
                        }
                    }

                    SegmentControlRow("MARGINS", listOf("None (0mm)", "Compact (5mm)", "Std (12mm)"), selectedMargins) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedMargins = it
                    }
                    SegmentControlRow("IMAGE FIT & SCALING", listOf("Fit Entire", "Fill Page", "Center"), selectedFit) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedFit = it
                    }
                }

                // Quality & Compression Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(1.dp, RoundedCornerShape(12.dp), spotColor = Color.Black.copy(alpha = 0.04f))
                        .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Tune, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Quality & Output Size", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                        }
                        Text("DPI Profile", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = OnSurfaceVariant)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QualityCard(
                            modifier = Modifier.weight(1f),
                            dpi = "300 DPI", title = "Print Quality", subtitle = "Lossless detail",
                            isSelected = selectedDpi == 300,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedDpi = 300
                            }
                        )
                        QualityCard(
                            modifier = Modifier.weight(1f),
                            dpi = "150 DPI", title = "Balanced Web", subtitle = "Fast sharing",
                            isSelected = selectedDpi == 150,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedDpi = 150
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(112.dp))
            }
        }
    }
}

@Composable
private fun ImagesToPdfTopBar(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface.copy(alpha = 0.90f))
            .shadow(elevation = 1.dp, spotColor = Color.Black.copy(alpha = 0.04f))
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).clickable { onNavigateBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = OnSurface)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text("Images To Pdf", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = OnSurface, letterSpacing = (-0.01).sp)
    }
}

@Composable
private fun QueueItemRow(
    item: QueuedImage,
    index: Int,
    isDragged: Boolean,
    offsetY: Float,
    onRotate: () -> Unit,
    onDelete: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val rotation by animateFloatAsState(targetValue = item.rotation, label = "rotateAnim")
    val sizeStr = "%.1f MB".format(item.sizeBytes / (1024.0 * 1024.0))
    val elevation by animateDpAsState(targetValue = if (isDragged) 12.dp else 1.dp, label = "elevationAnim")
    val scale by animateFloatAsState(targetValue = if (isDragged) 1.02f else 1f, label = "scaleAnim")

    Row(
        modifier = Modifier
            .graphicsLayer {
                translationY = offsetY
                scaleX = scale
                scaleY = scale
                // FIX: Removed the invalid translationZ property completely.
                // The shadow modifier below natively elevates the item on the Z-axis.
            }
            .fillMaxWidth()
            .shadow(elevation, RoundedCornerShape(12.dp), spotColor = Color.Black.copy(alpha = if (isDragged) 0.1f else 0.04f))
            .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(56.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { onDragStart() },
                        onDragEnd = { onDragEnd() },
                        onDragCancel = { onDragEnd() }
                    ) { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.DragIndicator, contentDescription = "Drag to reorder", tint = Outline, modifier = Modifier.size(24.dp))
        }

        Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceContainerHigh)) {
            AsyncImage(
                model = item.uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().rotate(rotation)
            )
            Box(
                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).clip(RoundedCornerShape(4.dp)).background(OnSurface.copy(alpha = 0.75f)).padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(index.toString(), fontSize = 9.sp, color = SurfaceContainerLowest)
            }
        }

        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${item.dimensions} • $sizeStr", fontSize = 12.sp, color = OnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).clickable { onRotate() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.RotateRight, contentDescription = "Rotate", tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
            }
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).clickable { onDelete() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete", tint = Error, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun SegmentControlRow(title: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant, letterSpacing = 0.5.sp)
        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceContainer).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            options.forEach { option ->
                val isSelected = selected == option
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) Primary else Color.Transparent)
                        .clickable { onSelect(option) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        fontSize = 11.sp,
                        fontWeight = if(isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if(isSelected) OnPrimary else OnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun QualityCard(modifier: Modifier, dpi: String, title: String, subtitle: String, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) Primary else SurfaceContainer
    val textColor = if (isSelected) OnPrimary else OnSurface
    val subTextColor = if (isSelected) OnPrimary.copy(alpha = 0.8f) else OnSurfaceVariant

    Column(
        modifier = modifier
            .shadow(if (isSelected) 1.dp else 0.dp, RoundedCornerShape(12.dp))
            .background(bgColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp)
            .height(96.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(dpi, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = textColor)
            if (isSelected) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(18.dp))
            } else {
                Icon(Icons.Outlined.RadioButtonUnchecked, contentDescription = null, tint = Outline, modifier = Modifier.size(18.dp))
            }
        }
        Column {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = textColor)
            Text(subtitle, fontSize = 10.sp, color = subTextColor)
        }
    }
}

@Composable
private fun ImagesToPdfBottomBar(imageCount: Int, totalBytes: Long, isProcessing: Boolean, isComplete: Boolean, onConvert: () -> Unit) {
    val estimatedMb = totalBytes / (1024.0 * 1024.0)
    val estimatedSize = if (imageCount > 0) "~${String.format("%.1f", estimatedMb)} MB" else "0 MB"

    val buttonText = when {
        isProcessing -> "Generating PDF..."
        isComplete -> "Saved to Downloads!"
        imageCount == 0 -> "Add images to proceed"
        else -> "Convert $imageCount Images to PDF"
    }

    val buttonIcon = when {
        isProcessing -> Icons.Outlined.Refresh
        isComplete -> Icons.Filled.CheckCircle
        else -> Icons.Outlined.PictureAsPdf
    }

    val buttonColor = if (isComplete) Secondary else if (imageCount > 0) PrimaryContainer else SurfaceContainerHigh
    val contentColor = if (imageCount > 0 || isComplete) OnPrimaryContainer else OutlineVariant
    val cornerRadius by animateDpAsState(targetValue = if (isProcessing) 50.dp else 12.dp, label = "buttonShape")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface.copy(alpha = 0.95f))
            .shadow(elevation = 24.dp, spotColor = OnSurface.copy(alpha = 0.1f))
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(if (!isComplete && imageCount > 0 && !isProcessing) 6.dp else 0.dp, RoundedCornerShape(cornerRadius), spotColor = PrimaryContainer.copy(alpha = 0.4f))
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(if(imageCount == 0) buttonColor.copy(alpha = 0.5f) else buttonColor)
                    .clickable(enabled = imageCount > 0 && !isProcessing && !isComplete) { onConvert() },
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = Pair(isProcessing, isComplete),
                    label = "buttonContent"
                ) { states ->
                    val (processing, _) = states
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (processing) {
                            CircularProgressIndicator(color = contentColor, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                        } else {
                            Icon(buttonIcon, contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(buttonText, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = contentColor)
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.SdCard, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Estimated size: $estimatedSize", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = OnSurfaceVariant)
            }
        }
    }
}