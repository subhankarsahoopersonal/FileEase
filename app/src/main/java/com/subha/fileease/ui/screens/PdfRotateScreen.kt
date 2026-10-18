package com.subha.fileease.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// Exact color tokens
private val Surface = Color(0xFFFAF8FF)
private val SurfaceContainerLowest = Color(0xFFFFFFFF)
private val SurfaceContainerLow = Color(0xFFF2F3FF)
private val SurfaceContainer = Color(0xFFEAEDFF)
private val SurfaceContainerHigh = Color(0xFFE2E7FF)
private val OnSurface = Color(0xFF131B2E)
private val OnSurfaceVariant = Color(0xFF424656)
private val Primary = Color(0xFF0050CB)
private val OnPrimary = Color(0xFFFFFFFF)
private val PrimaryFixed = Color(0xFFDAE1FF)
private val OnPrimaryFixed = Color(0xFF001849)
private val Secondary = Color(0xFF0058BE)
private val ErrorContainer = Color(0xFFFFDAD6)
private val OnErrorContainer = Color(0xFF93000A)
private val OutlineVariant = Color(0xFFC2C6D8)
private val SurfaceVariant = Color(0xFFDAE2FD)
private val OnTertiaryFixedVariant = Color(0xFF3B4856)

// Data class updated for actual PDF Thumbnails
data class RotatePage(
    val id: Int,
    var isSelected: Boolean,
    var rotation: Int,
    val thumbnail: Bitmap? = null
)

@Composable
fun PdfRotateScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // File State
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var fileSizeMb by remember { mutableDoubleStateOf(0.0) }
    var filePages by remember { mutableIntStateOf(0) }
    val pages = remember { mutableStateListOf<RotatePage>() }

    // UI State
    var activeScope by remember { mutableStateOf("selected") }
    var isProcessing by remember { mutableStateOf(false) }
    var isParsing by remember { mutableStateOf(false) }
    var processingComplete by remember { mutableStateOf(false) }

    val hasFile = selectedFileUri != null
    val modifiedCount = pages.count { it.rotation % 360 != 0 }
    val selectedCount = pages.count { it.isSelected }

    // PDF Picker Launcher
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            selectedFileUri = it
            isParsing = true
            coroutineScope.launch(Dispatchers.IO) {
                var name = "Unknown.pdf"
                var sizeBytes = 0L

                // Get Metadata
                context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        name = cursor.getString(nameIndex) ?: "Unknown.pdf"
                        sizeBytes = cursor.getLong(sizeIndex)
                    }
                }

                // Generate Thumbnails using Android Native PdfRenderer
                val generatedPages = mutableListOf<RotatePage>()
                try {
                    context.contentResolver.openFileDescriptor(it, "r")?.use { fd ->
                        val renderer = PdfRenderer(fd)
                        val totalPages = renderer.pageCount
                        for (i in 0 until totalPages) {
                            val page = renderer.openPage(i)
                            // Create a small, fast thumbnail canvas
                            val bitmap = Bitmap.createBitmap(240, 320, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(bitmap)
                            canvas.drawColor(android.graphics.Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            generatedPages.add(RotatePage(id = i + 1, isSelected = false, rotation = 0, thumbnail = bitmap))
                            page.close()
                        }
                        renderer.close()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                withContext(Dispatchers.Main) {
                    fileName = name
                    fileSizeMb = Math.round((sizeBytes / (1024.0 * 1024.0)) * 10.0) / 10.0
                    filePages = generatedPages.size
                    pages.clear()
                    pages.addAll(generatedPages)
                    isParsing = false
                }
            }
        }
    }

    val updateScope = { scope: String ->
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        activeScope = scope
        if (scope in listOf("all", "odd", "even")) {
            pages.forEachIndexed { index, page ->
                val num = index + 1
                page.isSelected = when (scope) {
                    "all" -> true
                    "odd" -> num % 2 != 0
                    "even" -> num % 2 == 0
                    else -> page.isSelected
                }
            }
        }
    }

    val rotateActive = { degrees: Int ->
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        pages.forEachIndexed { index, page ->
            val num = index + 1
            val shouldRotate = when (activeScope) {
                "all" -> true
                "selected" -> page.isSelected
                "odd" -> num % 2 != 0
                "even" -> num % 2 == 0
                else -> false
            }
            if (shouldRotate) {
                var newRot = (page.rotation + degrees) % 360
                if (newRot < 0) newRot += 360
                // Trigger recomposition
                pages[index] = page.copy(rotation = newRot)
            }
        }
    }

    Scaffold(
        containerColor = Surface,
        topBar = { RotateTopBar(onNavigateBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (hasFile) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    StatusNotificationBar(
                        modifiedCount = modifiedCount,
                        unchangedCount = filePages - modifiedCount
                    )
                    RotateBottomBar(isProcessing, processingComplete, hasFile, filePages) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        coroutineScope.launch(Dispatchers.IO) {
                            isProcessing = true
                            try {
                                val cleanFileName = fileName.removeSuffix(".pdf")
                                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                val outputFile = File(downloadsDir, "${cleanFileName}_rotated_${System.currentTimeMillis()}.pdf")

                                // PDFBox Rotation Engine
                                context.contentResolver.openInputStream(selectedFileUri!!)?.use { inputStream ->
                                    val document = PDDocument.load(inputStream, MemoryUsageSetting.setupMixed(50 * 1024 * 1024))

                                    for (i in 0 until document.numberOfPages) {
                                        val targetRotation = pages[i].rotation
                                        if (targetRotation != 0) {
                                            val pdPage = document.getPage(i)
                                            // Add desired rotation to existing physical rotation
                                            val currentRot = pdPage.rotation
                                            pdPage.rotation = (currentRot + targetRotation) % 360
                                        }
                                    }

                                    document.save(outputFile)
                                    document.close()
                                }

                                withContext(Dispatchers.Main) {
                                    isProcessing = false
                                    processingComplete = true

                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        val timings = longArrayOf(0, 80, 100, 120)
                                        val amplitudes = intArrayOf(0, 150, 0, 255)
                                        vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                                    } else {
                                        @Suppress("DEPRECATION")
                                        vibrator.vibrate(longArrayOf(0, 80, 100, 120), -1)
                                    }

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
                                        context.startActivity(Intent.createChooser(intent, "Open File"))
                                    }
                                    delay(1500)
                                    processingComplete = false
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isProcessing = false
                                    snackbarHostState.showSnackbar("Rotation failed: ${e.localizedMessage}")
                                }
                            }
                        }
                    }
                }
            } else {
                RotateBottomBar(isProcessing = false, isComplete = false, hasFile = false, filePages = 0) {
                    pdfPickerLauncher.launch(arrayOf("application/pdf"))
                }
            }
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
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                FileInfoCard(
                    hasFile = hasFile,
                    fileName = fileName,
                    fileSizeMb = fileSizeMb,
                    filePages = filePages,
                    isProcessing = isProcessing || isParsing,
                    onChangeClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) }
                )

                if (hasFile) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        QuickRotationBar(
                            onRotateCW = { rotateActive(90) },
                            onRotateCCW = { rotateActive(-90) },
                            onRotate180 = { rotateActive(180) }
                        )

                        ScopeSelectorTabs(
                            activeScope = activeScope,
                            selectedCount = selectedCount,
                            totalCount = filePages,
                            onScopeSelect = updateScope,
                            onResetAll = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                pages.forEachIndexed { index, page ->
                                    pages[index] = page.copy(rotation = 0)
                                }
                            }
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        pages.chunked(2).forEach { rowPages ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                rowPages.forEach { page ->
                                    PageRotateTile(
                                        page = page,
                                        modifier = Modifier.weight(1f),
                                        onSelectToggle = {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            val idx = pages.indexOfFirst { it.id == page.id }
                                            if (idx != -1) {
                                                pages[idx] = page.copy(isSelected = !page.isSelected)
                                            }
                                        },
                                        onRotateSingle = {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            var newRot = (page.rotation + 90) % 360
                                            if (newRot < 0) newRot += 360
                                            val idx = pages.indexOfFirst { it.id == page.id }
                                            if (idx != -1) {
                                                pages[idx] = page.copy(rotation = newRot)
                                            }
                                        }
                                    )
                                }
                                if (rowPages.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    ProTipCard()
                    Spacer(modifier = Modifier.height(180.dp))
                }
            }
        }
    }
}

@Composable
private fun RotateTopBar(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface.copy(alpha = 0.95f))
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
        Text("Rotate PDF", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = OnSurface)
    }
}

@Composable
private fun FileInfoCard(hasFile: Boolean, fileName: String, fileSizeMb: Double, filePages: Int, isProcessing: Boolean, onChangeClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaPulse by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = if (isProcessing) 0.4f else 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutLinearInEasing), RepeatMode.Reverse), label = "alphaPulse"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.08f))
            .background(SurfaceContainerLowest, RoundedCornerShape(16.dp))
            .clickable(enabled = !hasFile && !isProcessing) { onChangeClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (hasFile) ErrorContainer else SurfaceContainerLow)
                    .alpha(alphaPulse),
                contentAlignment = Alignment.Center
            ) {
                if (hasFile) {
                    Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, tint = OnErrorContainer, modifier = Modifier.size(26.dp))
                } else {
                    Icon(Icons.Outlined.FolderOpen, contentDescription = null, tint = Primary, modifier = Modifier.size(26.dp))
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (hasFile) {
                    Text(fileName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$filePages pages", fontSize = 12.sp, color = OnSurfaceVariant)
                        Text(" • ", fontSize = 12.sp, color = OnSurfaceVariant)
                        Text("$fileSizeMb MB", fontSize = 12.sp, color = OnSurfaceVariant)
                    }
                } else {
                    Text("No file selected", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Tap to browse storage", fontSize = 12.sp, color = OnSurfaceVariant)
                }
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(SurfaceContainerLow)
                .clickable(enabled = !isProcessing) { onChangeClick() }
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(if (hasFile) "Change" else "Select", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isProcessing) OutlineVariant else Primary)
        }
    }
}

@Composable
private fun QuickRotationBar(onRotateCW: () -> Unit, onRotateCCW: () -> Unit, onRotate180: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("ROTATE DIRECTION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OnSurface, letterSpacing = 0.5.sp, modifier = Modifier.padding(horizontal = 4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RotateActionButton(modifier = Modifier.weight(1f), icon = Icons.Outlined.RotateRight, title = "90° Right", subtitle = "Clockwise", onClick = onRotateCW)
            RotateActionButton(modifier = Modifier.weight(1f), icon = Icons.Outlined.RotateLeft, title = "90° Left", subtitle = "Anti Clockwise", onClick = onRotateCCW)
            RotateActionButton(modifier = Modifier.weight(1f), icon = Icons.Outlined.FlipCameraAndroid, title = "180° Flip", subtitle = "Invert", onClick = onRotate180)
        }
    }
}

@Composable
private fun RotateActionButton(modifier: Modifier = Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .shadow(1.dp, RoundedCornerShape(16.dp))
            .background(SurfaceContainerLowest, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 16.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(SurfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OnSurface, textAlign = TextAlign.Center)
        Text(subtitle, fontSize = 11.sp, color = OnSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ScopeSelectorTabs(activeScope: String, selectedCount: Int, totalCount: Int, onScopeSelect: (String) -> Unit, onResetAll: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("APPLY SCOPE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OnSurface, letterSpacing = 0.5.sp)
            Text("Reset All to 0°", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Primary, modifier = Modifier.clickable { onResetAll() }.padding(4.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ScopePill("Selected ($selectedCount)", isActive = activeScope == "selected") { onScopeSelect("selected") }
            ScopePill("All Pages ($totalCount)", isActive = activeScope == "all") { onScopeSelect("all") }
            ScopePill("Odd Pages", isActive = activeScope == "odd") { onScopeSelect("odd") }
            ScopePill("Even Pages", isActive = activeScope == "even") { onScopeSelect("even") }
        }
    }
}

@Composable
private fun ScopePill(text: String, isActive: Boolean, onClick: () -> Unit) {
    val bgColor = if (isActive) Primary else SurfaceContainerLowest
    val textColor = if (isActive) OnPrimary else OnSurfaceVariant
    val elevation = if (isActive) 4.dp else 1.dp

    Box(
        modifier = Modifier
            .shadow(elevation, RoundedCornerShape(50))
            .background(bgColor, RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        AnimatedContent(targetState = text, label = "scopeText") { targetText ->
            Text(targetText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textColor)
        }
    }
}

@Composable
private fun PageRotateTile(page: RotatePage, modifier: Modifier = Modifier, onSelectToggle: () -> Unit, onRotateSingle: () -> Unit) {
    val borderColor = if (page.isSelected) Primary else Color.Transparent
    val badgeBg = if (page.rotation == 0) SurfaceContainerHigh else Primary
    val badgeText = if (page.rotation == 0) OnSurfaceVariant else OnPrimary

    val animatedRotation by animateFloatAsState(
        targetValue = page.rotation.toFloat(),
        animationSpec = tween(durationMillis = 300),
        label = "rotationAnim"
    )

    Column(
        modifier = modifier
            .shadow(1.dp, RoundedCornerShape(16.dp))
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .background(SurfaceContainerLowest, RoundedCornerShape(16.dp))
            .clickable { onSelectToggle() }
            .padding(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Page ${page.id}", fontSize = 12.sp, fontWeight = if(page.isSelected) FontWeight.Bold else FontWeight.SemiBold, color = if(page.isSelected) Primary else OnSurfaceVariant)
            Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(badgeBg).padding(horizontal = 8.dp, vertical = 2.dp)) {
                val sign = if (page.rotation > 0 && page.rotation < 180) "+" else ""
                Text("$sign${page.rotation}°", fontSize = 11.sp, fontWeight = if(page.rotation != 0) FontWeight.Bold else FontWeight.Medium, color = badgeText)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceContainerLow)
        ) {
            if (page.thumbnail != null) {
                Image(
                    bitmap = page.thumbnail.asImageBitmap(),
                    contentDescription = "Thumbnail for Page ${page.id}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(animatedRotation)
                )
            }

            if (page.isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(24.dp)
                        .shadow(2.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Check, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            val orientationText = when (page.rotation) {
                90, 270 -> "Landscape"
                180 -> "Inverted"
                else -> "Portrait"
            }
            Text(orientationText, fontSize = 11.sp, fontWeight = if(page.isSelected) FontWeight.SemiBold else FontWeight.Medium, color = if(page.isSelected) Primary else OnSurfaceVariant)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if(page.isSelected) PrimaryFixed else SurfaceContainerLow)
                    .clickable { onRotateSingle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.RotateRight, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun StatusNotificationBar(modifiedCount: Int, unchangedCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp)
            .shadow(6.dp, RoundedCornerShape(16.dp), spotColor = Primary.copy(alpha = 0.15f))
            .background(SurfaceContainerLowest, RoundedCornerShape(16.dp))
            .border(1.dp, PrimaryFixed, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Sync, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))

            AnimatedContent(
                targetState = Pair(modifiedCount, unchangedCount),
                transitionSpec = {
                    if (targetState.first > initialState.first) {
                        (slideInVertically { height -> height } + fadeIn()).togetherWith(slideOutVertically { height -> -height } + fadeOut())
                    } else {
                        (slideInVertically { height -> -height } + fadeIn()).togetherWith(slideOutVertically { height -> height } + fadeOut())
                    }.using(SizeTransform(clip = false))
                }, label = "statusCounts"
            ) { counts ->
                Text("${counts.first} modified • ${counts.second} unchanged", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OnSurface)
            }
        }
        Row(
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(PrimaryFixed.copy(alpha = 0.5f)).padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Bolt, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Lossless", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Primary)
        }
    }
}

@Composable
private fun ProTipCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(PrimaryFixed, SurfaceVariant)))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(SurfaceContainerLowest).shadow(1.dp, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Lightbulb, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text("Pro Tip: Multi-Page Select", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnPrimaryFixed)
            Spacer(modifier = Modifier.height(2.dp))
            Text("Tap pages directly or use Odd/Even scope pills to rotate bulk plans instantly.", fontSize = 13.sp, color = OnTertiaryFixedVariant, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun RotateBottomBar(isProcessing: Boolean, isComplete: Boolean, hasFile: Boolean, filePages: Int, onApplyClick: () -> Unit) {
    val buttonText = when {
        !hasFile -> "Select a PDF to Rotate"
        isProcessing -> "Rebuilding Blueprint PDF..."
        isComplete -> "Saved to Documents"
        else -> "Apply & Save Rotated PDF"
    }

    val buttonIcon = when {
        !hasFile -> Icons.Outlined.Folder
        isComplete -> Icons.Outlined.CheckCircle
        else -> Icons.Outlined.SaveAs
    }

    val buttonColor = if (isComplete) Secondary else if (hasFile) Primary else SurfaceContainerHigh
    val contentColor = if (hasFile || isComplete) OnPrimary else OutlineVariant

    val cornerRadius by animateDpAsState(targetValue = if (isProcessing) 50.dp else 16.dp, label = "buttonShape")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceContainerLowest.copy(alpha = 0.98f))
            .shadow(elevation = 24.dp, spotColor = OnSurface.copy(alpha = 0.1f))
            .navigationBarsPadding()
    ) {
        if (hasFile) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Primary))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ready to export $filePages-page PDF", fontSize = 12.sp, color = OnSurfaceVariant)
                }
                Text("Original Quality Retained", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Primary)
            }
        } else {
            Spacer(modifier = Modifier.height(16.dp))
        }

        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .fillMaxWidth()
                .height(56.dp)
                .shadow(if (hasFile && !isComplete && !isProcessing) 12.dp else 0.dp, RoundedCornerShape(cornerRadius), spotColor = buttonColor.copy(alpha = 0.4f))
                .clip(RoundedCornerShape(cornerRadius))
                .background(buttonColor)
                .clickable(enabled = !isProcessing && !isComplete) { onApplyClick() },
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = Pair(isProcessing, isComplete),
                label = "buttonContent"
            ) { states ->
                val (processing, _) = states
                if (processing) {
                    CircularProgressIndicator(color = contentColor, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(buttonIcon, contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(buttonText, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = contentColor)
                    }
                }
            }
        }
    }
}