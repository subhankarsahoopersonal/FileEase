package com.subha.fileease.ui.screens

import android.content.ContentValues
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

// Exact Tailwind Color Tokens from Mockup
private val Background = Color(0xFFFAF8FF)
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
// FIX: Added the missing OnPrimaryContainer token
private val OnPrimaryContainer = Color(0xFFF8F7FF)
private val Secondary = Color(0xFF0058BE)
private val Outline = Color(0xFF727687)
private val OutlineVariant = Color(0xFFC2C6D8)
private val Error = Color(0xFFBA1A1A)
private val ErrorContainer = Color(0xFFFFDAD6)

@Composable
fun TextToPdfScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val vibrator = remember { context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Mode State
    var inputMode by remember { mutableStateOf("paste") }

    // Editor State
    var textContent by remember { mutableStateOf("# Project Executive Summary\nDate: Oct 24, 2024\nLead: FileEase Engineering Core\n\n1. Project Scope & Milestones\n- Native client-side document processing engine\n- Sub-50ms PDF assembly via Vector Streams\n- Complete offline privacy preservation architecture\n\n2. Key Deliverables\nReady-to-sign formatted brief exported for internal distribution.") }
    val charCount = textContent.length
    val wordCount = if (textContent.isBlank()) 0 else textContent.split("\\s+".toRegex()).size
    val lineCount = textContent.lines().size

    // Typography State
    var fontFamily by remember { mutableStateOf("Sans-Serif") }
    var fontSize by remember { mutableStateOf("12pt") }
    var lineSpacing by remember { mutableStateOf("1.5x") }

    // Formatting State
    var pageNumbersEnabled by remember { mutableStateOf(true) }
    var outputFileName by remember { mutableStateOf("Executive_Summary") }
    var authorName by remember { mutableStateOf("FileEase User") }

    // Export & Progress State
    var isProcessing by remember { mutableStateOf(false) }
    var isComplete by remember { mutableStateOf(false) }
    var outputUri by remember { mutableStateOf<Uri?>(null) }

    var conversionJob by remember { mutableStateOf<Job?>(null) }
    var isCancelling by remember { mutableStateOf(false) }
    var conversionProgress by remember { mutableFloatStateOf(0f) }
    var currentPage by remember { mutableIntStateOf(0) }

    // Text File Import Launcher
    val textPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val text = stream.bufferedReader().use { it.readText() }
                        withContext(Dispatchers.Main) {
                            textContent = text
                            inputMode = "paste"
                            snackbarHostState.showSnackbar("Text imported successfully.")
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        snackbarHostState.showSnackbar("Failed to read text file.")
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = Background,
        topBar = { TextToPdfTopBar(onNavigateBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            TextToPdfBottomBar(
                isProcessing = isProcessing,
                isCancelling = isCancelling,
                isComplete = isComplete,
                isEnabled = textContent.isNotBlank(),
                progress = conversionProgress,
                currentPage = currentPage,
                onConvert = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)

                    // Phase 3: Storage Quota Verification
                    val usableSpaceMb = Environment.getDataDirectory().usableSpace / (1024 * 1024)
                    if (usableSpaceMb < 5) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Not enough storage space. Please free up at least 5MB.")
                        }
                        return@TextToPdfBottomBar
                    }

                    conversionJob = coroutineScope.launch(Dispatchers.IO) {
                        isProcessing = true
                        isCancelling = false
                        conversionProgress = 0f
                        currentPage = 0

                        var pendingDestUri: Uri? = null
                        var pdfDocument: PdfDocument? = null

                        try {
                            // 1. PDF Architecture Setup (A4 Size: 595 x 842 points)
                            pdfDocument = PdfDocument()
                            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()

                            val margin = 56f // ~20mm margins
                            val usableWidth = 595f - (margin * 2)
                            val usableHeight = 842f - (margin * 2) - (if (pageNumbersEnabled) 30f else 0f)

                            // 2. Map UI selections to Native TextPaint
                            val textPaint = TextPaint().apply {
                                isAntiAlias = true
                                color = android.graphics.Color.BLACK

                                typeface = when (fontFamily) {
                                    "Serif" -> Typeface.SERIF
                                    "Monospace" -> Typeface.MONOSPACE
                                    else -> Typeface.SANS_SERIF
                                }

                                textSize = when (fontSize) {
                                    "10pt" -> 10f
                                    "14pt" -> 14f
                                    "16pt" -> 16f
                                    else -> 12f
                                } * 1.35f
                            }

                            val spacingMult = when (lineSpacing) {
                                "1.15x" -> 1.15f
                                "2.0x" -> 2.0f
                                else -> 1.5f
                            }

                            // 3. Phase 3: CPU Thread Offloading for StaticLayout to prevent UI Jank
                            val staticLayout = withContext(Dispatchers.Default) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    StaticLayout.Builder.obtain(textContent, 0, textContent.length, textPaint, usableWidth.toInt())
                                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                                        .setLineSpacing(0f, spacingMult)
                                        .setIncludePad(false)
                                        .build()
                                } else {
                                    @Suppress("DEPRECATION")
                                    StaticLayout(textContent, textPaint, usableWidth.toInt(), Layout.Alignment.ALIGN_NORMAL, spacingMult, 0f, false)
                                }
                            }

                            if (!isActive) throw kotlinx.coroutines.CancellationException("Cancelled")

                            // 4. Pagination Engine (Deterministic Stream Writing)
                            val totalLines = staticLayout.lineCount
                            var currentLine = 0
                            var pageCount = 0

                            while (currentLine < totalLines) {
                                if (!isActive) throw kotlinx.coroutines.CancellationException("Cancelled")
                                val page = pdfDocument.startPage(pageInfo)
                                val canvas = page.canvas

                                val startY = staticLayout.getLineTop(currentLine)
                                var linesToDraw = 0

                                // Calculate lines for this page
                                while (currentLine + linesToDraw < totalLines) {
                                    val lineBottom = staticLayout.getLineBottom(currentLine + linesToDraw)
                                    if (lineBottom - startY > usableHeight) break
                                    linesToDraw++
                                }
                                if (linesToDraw == 0) linesToDraw = 1

                                // Draw Content
                                canvas.save()
                                canvas.translate(margin, margin - startY.toFloat())
                                canvas.clipRect(0f, startY.toFloat(), usableWidth, startY.toFloat() + usableHeight)
                                staticLayout.draw(canvas)
                                canvas.restore()

                                pageCount++

                                // Footer Generation
                                if (pageNumbersEnabled) {
                                    val footerPaint = Paint().apply {
                                        textSize = 10f * 1.35f
                                        color = android.graphics.Color.GRAY
                                        textAlign = Paint.Align.CENTER
                                        typeface = textPaint.typeface
                                    }
                                    canvas.drawText("- $pageCount -", 595f / 2f, 842f - (margin / 1.5f), footerPaint)
                                }

                                pdfDocument.finishPage(page)
                                currentLine += linesToDraw

                                // Phase 3: Real-time UI Progress bridging
                                withContext(Dispatchers.Main) {
                                    conversionProgress = currentLine.toFloat() / totalLines.toFloat()
                                    currentPage = pageCount
                                }
                            }

                            // 5. Scoped Storage MediaStore Export (Downloads Folder)
                            val finalName = if (outputFileName.endsWith(".pdf", ignoreCase = true)) outputFileName else "$outputFileName.pdf"

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                                val details = ContentValues().apply {
                                    put(MediaStore.MediaColumns.DISPLAY_NAME, finalName)
                                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/FileEase")
                                }
                                pendingDestUri = context.contentResolver.insert(collection, details)
                                pendingDestUri?.let { uri ->
                                    context.contentResolver.openOutputStream(uri)?.use { out ->
                                        if (!isActive) throw kotlinx.coroutines.CancellationException("Cancelled by user")
                                        pdfDocument.writeTo(out)
                                    }
                                }
                            } else {
                                @Suppress("DEPRECATION")
                                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "FileEase")
                                if (!dir.exists()) dir.mkdirs()
                                val file = File(dir, finalName)
                                FileOutputStream(file).use { out ->
                                    if (!isActive) throw kotlinx.coroutines.CancellationException("Cancelled by user")
                                    pdfDocument.writeTo(out)
                                }
                                pendingDestUri = Uri.fromFile(file)
                            }

                            pdfDocument.close()
                            pdfDocument = null

                            if (!isActive) throw kotlinx.coroutines.CancellationException("Cancelled")

                            withContext(Dispatchers.Main) {
                                isProcessing = false
                                isComplete = true
                                outputUri = pendingDestUri

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 80, 100, 120), intArrayOf(0, 150, 0, 255), -1))
                                } else {
                                    @Suppress("DEPRECATION")
                                    vibrator.vibrate(longArrayOf(0, 80, 100, 120), -1)
                                }
                                snackbarHostState.showSnackbar("PDF Generated Successfully ($pageCount pages).")
                            }

                        } catch (e: kotlinx.coroutines.CancellationException) {
                            // Phase 3: Ghost File Rollback on Cancel
                            pdfDocument?.close()
                            pendingDestUri?.let { context.contentResolver.delete(it, null, null) }
                            withContext(Dispatchers.Main) {
                                isProcessing = false
                                isCancelling = false
                                snackbarHostState.showSnackbar("Export cancelled.")
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            pdfDocument?.close()
                            pendingDestUri?.let { context.contentResolver.delete(it, null, null) }
                            withContext(Dispatchers.Main) {
                                isProcessing = false
                                isCancelling = false
                                snackbarHostState.showSnackbar("Failed to export PDF.")
                            }
                        }
                    }
                },
                onCancel = {
                    if (isProcessing) {
                        isCancelling = true
                        conversionJob?.cancel()
                    }
                },
                onShare = {
                    outputUri?.let { uri ->
                        try {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Document"))
                        } catch (e: Exception) {
                            coroutineScope.launch { snackbarHostState.showSnackbar("Error launching share sheet.") }
                        }
                    }
                },
                onOpen = {
                    outputUri?.let { uri ->
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/pdf")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Open PDF"))
                        } catch (e: Exception) {
                            coroutineScope.launch { snackbarHostState.showSnackbar("No PDF viewer installed.") }
                        }
                    }
                },
                onReset = {
                    isComplete = false
                    outputUri = null
                    conversionProgress = 0f
                    currentPage = 0
                }
            )
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(12.dp))
                    .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Convert plain text, notes, and markdown into formatted PDF files", fontSize = 13.sp, color = OnSurface, lineHeight = 18.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Bolt, contentDescription = null, tint = Primary, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Zero cloud latency • Instant client-side render", fontSize = 10.sp, color = Primary, fontWeight = FontWeight.Medium)
                    }
                }
                Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceContainer), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Input Mode Segment Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(12.dp))
                    .background(SurfaceContainer, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                ModeTab(modifier = Modifier.weight(1f), title = "Type / Paste Text", icon = Icons.Outlined.EditNote, isSelected = inputMode == "paste") { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); inputMode = "paste" }
                ModeTab(modifier = Modifier.weight(1f), title = "Import .TXT / .MD", icon = Icons.Outlined.UploadFile, isSelected = inputMode == "import") { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); inputMode = "import" }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Import Dropzone
            AnimatedVisibility(visible = inputMode == "import") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(1.dp, RoundedCornerShape(12.dp))
                        .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(SurfaceContainerHigh), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.CloudUpload, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Drag file here or browse", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                    Text("Supports UTF-8 .txt, .markdown, .md notes", fontSize = 12.sp, color = OnSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(PrimaryContainer).clickable(enabled = !isProcessing) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            textPickerLauncher.launch("text/*")
                        }.alpha(if (isProcessing) 0.5f else 1f).padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.FolderOpen, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Select from Device", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = OnPrimary)
                    }
                }
            }

            // Editor Container
            AnimatedVisibility(visible = inputMode == "paste") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(1.dp, RoundedCornerShape(12.dp))
                        .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(PrimaryContainer))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Markdown / Text Source", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                        }
                        Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(SurfaceContainerHigh).padding(horizontal = 8.dp, vertical = 2.dp)) {
                            Text("$wordCount words • $charCount chars", fontSize = 10.sp, color = OnSurfaceVariant, letterSpacing = 0.5.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Text Area with Line Numbers
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceContainerLow)
                            .padding(12.dp)
                    ) {
                        Column(modifier = Modifier.width(20.dp), horizontalAlignment = Alignment.End) {
                            for (i in 1..maxOf(8, lineCount)) {
                                Text("$i", fontSize = 10.sp, color = Outline, modifier = Modifier.padding(bottom = 5.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        BasicTextField(
                            value = textContent,
                            onValueChange = { textContent = it },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                            textStyle = TextStyle(fontSize = 12.sp, color = OnSurface, lineHeight = 18.sp),
                            cursorBrush = SolidColor(Primary),
                            enabled = !isProcessing
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Primary, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Auto-synced to previewer", fontSize = 10.sp, color = OnSurfaceVariant)
                        }
                        Row(
                            modifier = Modifier.clickable(enabled = !isProcessing) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                textContent = "# Project Executive Summary\nDate: Oct 24, 2024\nLead: FileEase Engineering Core\n\n1. Project Scope & Milestones\n- Native client-side document processing engine\n- Sub-50ms PDF assembly via Vector Streams\n- Complete offline privacy preservation architecture\n\n2. Key Deliverables\nReady-to-sign formatted brief exported for internal distribution."
                            }.alpha(if (isProcessing) 0.5f else 1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Refresh, contentDescription = null, tint = Secondary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset sample", fontSize = 10.sp, color = Secondary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Typography & Layout Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(12.dp))
                    .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.FormatPaint, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Typography & Layout", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                    }
                    Text("PDF Style Sheet", fontSize = 10.sp, color = OnSurfaceVariant)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Font Family", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillButton(modifier = Modifier.weight(1f), text = "Sans-Serif", isSelected = fontFamily == "Sans-Serif", isEnabled = !isProcessing) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); fontFamily = "Sans-Serif" }
                    PillButton(modifier = Modifier.weight(1f), text = "Serif", isSelected = fontFamily == "Serif", isEnabled = !isProcessing) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); fontFamily = "Serif" }
                    PillButton(modifier = Modifier.weight(1f), text = "Monospace", isSelected = fontFamily == "Monospace", isEnabled = !isProcessing) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); fontFamily = "Monospace" }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Font Size", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant)
                    Text(fontSize, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PillButtonSmall(modifier = Modifier.weight(1f), text = "10pt", isSelected = fontSize == "10pt", isEnabled = !isProcessing) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); fontSize = "10pt" }
                    PillButtonSmall(modifier = Modifier.weight(1f), text = "12pt", isSelected = fontSize == "12pt", isEnabled = !isProcessing) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); fontSize = "12pt" }
                    PillButtonSmall(modifier = Modifier.weight(1f), text = "14pt", isSelected = fontSize == "14pt", isEnabled = !isProcessing) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); fontSize = "14pt" }
                    PillButtonSmall(modifier = Modifier.weight(1f), text = "16pt", isSelected = fontSize == "16pt", isEnabled = !isProcessing) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); fontSize = "16pt" }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Line Spacing", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillButtonSmall(modifier = Modifier.weight(1f), text = "1.15x", isSelected = lineSpacing == "1.15x", isEnabled = !isProcessing) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); lineSpacing = "1.15x" }
                    PillButtonSmall(modifier = Modifier.weight(1f), text = "1.5x (Rec.)", isSelected = lineSpacing == "1.5x", isEnabled = !isProcessing) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); lineSpacing = "1.5x" }
                    PillButtonSmall(modifier = Modifier.weight(1f), text = "2.0x", isSelected = lineSpacing == "2.0x", isEnabled = !isProcessing) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); lineSpacing = "2.0x" }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(SurfaceContainerLow).padding(8.dp)) {
                        Text("Page Format", fontSize = 10.sp, color = OnSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("A4 Standard", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                            Icon(Icons.Outlined.Description, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                        }
                    }
                    Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(SurfaceContainerLow).padding(8.dp)) {
                        Text("Margin", fontSize = 10.sp, color = OnSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("20mm Normal", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                            Icon(Icons.Outlined.SpaceBar, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceContainerLow).padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.FormatListNumbered, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Page numbers in footer", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                    }
                    CustomSwitch(checked = pageNumbersEnabled, isEnabled = !isProcessing, onCheckedChange = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); pageNumbersEnabled = it })
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Metadata Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(12.dp))
                    .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                    Icon(Icons.Outlined.Badge, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Document Metadata", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                }

                Text("Output File Name", fontSize = 10.sp, color = OnSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceContainerLow).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Description, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = outputFileName,
                        onValueChange = { outputFileName = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(fontSize = 12.sp, color = OnSurface, fontWeight = FontWeight.Medium),
                        cursorBrush = SolidColor(Primary),
                        enabled = !isProcessing
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Author / Signee", fontSize = 10.sp, color = OnSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceContainerLow).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Person, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = authorName,
                        onValueChange = { authorName = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(fontSize = 12.sp, color = OnSurface),
                        cursorBrush = SolidColor(Primary),
                        enabled = !isProcessing
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ISO Notice
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainerHigh.copy(alpha = 0.6f))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceContainerLowest), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Verified, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("ISO 32000-1 Standard", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                        Text("Embedded font metrics • Crisp print ready", fontSize = 10.sp, color = OnSurfaceVariant)
                    }
                }
                Text("100% On-device", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Primary)
            }

            Spacer(modifier = Modifier.height(140.dp))
        }
    }
}

@Composable
private fun TextToPdfTopBar(onNavigateBack: () -> Unit) {
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
        Text("Text To PDF", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OnSurface, letterSpacing = (-0.01).sp)
    }
}

@Composable
private fun ModeTab(modifier: Modifier, title: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) SurfaceContainerLowest else Color.Transparent
    val contentColor = if (isSelected) Primary else OnSurfaceVariant
    val elevation = if (isSelected) 1.dp else 0.dp

    Row(
        modifier = modifier
            .shadow(elevation, RoundedCornerShape(8.dp))
            .background(bgColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(title, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, color = contentColor)
    }
}

@Composable
private fun PillButton(modifier: Modifier, text: String, isSelected: Boolean, isEnabled: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) Primary else SurfaceContainerHigh
    val textColor = if (isSelected) OnPrimary else OnSurface

    Box(
        modifier = modifier
            .shadow(if(isSelected) 1.dp else 0.dp, RoundedCornerShape(8.dp))
            .background(bgColor, RoundedCornerShape(8.dp))
            .clickable(enabled = isEnabled) { onClick() }
            .alpha(if (isEnabled) 1f else 0.6f)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 11.sp, fontWeight = if(isSelected) FontWeight.SemiBold else FontWeight.Normal, color = textColor)
    }
}

@Composable
private fun PillButtonSmall(modifier: Modifier, text: String, isSelected: Boolean, isEnabled: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) Primary else SurfaceContainerHigh
    val textColor = if (isSelected) OnPrimary else OnSurface

    Box(
        modifier = modifier
            .shadow(if(isSelected) 1.dp else 0.dp, RoundedCornerShape(8.dp))
            .background(bgColor, RoundedCornerShape(8.dp))
            .clickable(enabled = isEnabled) { onClick() }
            .alpha(if (isEnabled) 1f else 0.6f)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 10.sp, fontWeight = if(isSelected) FontWeight.SemiBold else FontWeight.Normal, color = textColor)
    }
}

@Composable
private fun CustomSwitch(checked: Boolean, isEnabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val bg by animateColorAsState(if (checked) PrimaryContainer else OutlineVariant, label = "switchBg")
    val offset by animateDpAsState(if (checked) 16.dp else 0.dp, label = "switchOffset")

    Box(
        modifier = Modifier
            .width(40.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(50))
            .background(bg.copy(alpha = if (isEnabled) 1f else 0.5f))
            .clickable(enabled = isEnabled) { onCheckedChange(!checked) }
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = offset)
                .size(20.dp)
                .shadow(if (isEnabled) 1.dp else 0.dp, CircleShape)
                .background(SurfaceContainerLowest, CircleShape)
        )
    }
}

@Composable
private fun TextToPdfBottomBar(
    isProcessing: Boolean,
    isCancelling: Boolean,
    isComplete: Boolean,
    isEnabled: Boolean,
    progress: Float,
    currentPage: Int,
    onConvert: () -> Unit,
    onCancel: () -> Unit,
    onShare: () -> Unit,
    onOpen: () -> Unit,
    onReset: () -> Unit
) {
    val buttonColor = if (isCancelling) ErrorContainer else PrimaryContainer
    val contentColor = if (isCancelling) Error else OnPrimaryContainer
    val cornerRadius by animateDpAsState(targetValue = if (isProcessing) 50.dp else 12.dp, label = "buttonShape")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface.copy(alpha = 0.90f))
            .shadow(elevation = 24.dp, spotColor = OnSurface.copy(alpha = 0.06f))
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)) {

            AnimatedContent(targetState = isComplete, label = "ActionBarTransition") { complete ->
                if (complete) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PostActionItem(icon = Icons.Outlined.Share, label = "Share File", onClick = onShare)
                        PostActionItem(icon = Icons.Outlined.OpenInNew, label = "Open File", onClick = onOpen)
                        PostActionItem(icon = Icons.Outlined.RestartAlt, label = "Convert New", onClick = onReset)
                    }
                } else {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Primary))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Dynamic pagination & wrapping", fontSize = 10.sp, color = OnSurfaceVariant)
                            }
                            Text("A4 Document", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Primary)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .shadow(if (isEnabled && !isProcessing) 4.dp else 0.dp, RoundedCornerShape(cornerRadius), spotColor = buttonColor.copy(alpha = 0.5f))
                                .clip(RoundedCornerShape(cornerRadius))
                                .background(if (!isEnabled) SurfaceContainerHigh else buttonColor)
                                .clickable(enabled = isEnabled) {
                                    if (isProcessing && !isCancelling) onCancel() else if (!isProcessing) onConvert()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedContent(targetState = Pair(isProcessing, isCancelling), label = "buttonState") { (processing, cancelling) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (cancelling) {
                                        Icon(Icons.Outlined.Cancel, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Cancelling...", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = contentColor)
                                    } else if (processing) {
                                        CircularProgressIndicator(progress = { progress }, color = contentColor, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(if (currentPage > 0) "Rendering page $currentPage..." else "Compiling Vector PDF...", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = contentColor)
                                    } else {
                                        Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, tint = if (isEnabled) contentColor else OutlineVariant, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Generate PDF from Text", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isEnabled) contentColor else OutlineVariant)
                                    }
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
private fun PostActionItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(SurfaceContainerLow),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Primary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
    }
}