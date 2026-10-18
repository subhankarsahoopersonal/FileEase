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
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

// Exact color tokens from the Stitch HTML Tailwind config
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
private val Secondary = Color(0xFF0058BE)
private val ErrorContainer = Color(0xFFFFDAD6)
private val OnErrorContainer = Color(0xFF93000A)
private val Outline = Color(0xFF727687)
private val OutlineVariant = Color(0xFFC2C6D8)

// Upgraded Data class for actual PDF Thumbnails
data class ExtractPage(
    val id: Int,
    var isSelected: Boolean,
    val thumbnail: Bitmap? = null
)

@Composable
fun PdfExtractScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Hardware & File State Management
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var fileSizeMb by remember { mutableDoubleStateOf(0.0) }
    var filePages by remember { mutableIntStateOf(0) }
    val hasFile = selectedFileUri != null

    // State Management for inputs and toggles
    var isProcessing by remember { mutableStateOf(false) }
    var isParsing by remember { mutableStateOf(false) }
    var processingComplete by remember { mutableStateOf(false) }
    var extractionMode by remember { mutableStateOf("single") } // single, separate
    var deleteOriginal by remember { mutableStateOf(false) }

    // Dynamic Grid State
    val pages = remember { mutableStateListOf<ExtractPage>() }
    val selectedCount = pages.count { it.isSelected }
    var rangeInput by remember { mutableStateOf(generateRangeString(pages)) }

    // Phase 3: Document Picker & Thumbnail Extractor
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            selectedFileUri = it
            isParsing = true
            coroutineScope.launch(Dispatchers.IO) {
                var name = "Unknown.pdf"
                var sizeBytes = 0L

                context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        name = cursor.getString(nameIndex) ?: "Unknown.pdf"
                        sizeBytes = cursor.getLong(sizeIndex)
                    }
                }

                val generatedPages = mutableListOf<ExtractPage>()
                var totalPages = 0
                try {
                    context.contentResolver.openFileDescriptor(it, "r")?.use { fd ->
                        val renderer = PdfRenderer(fd)
                        totalPages = renderer.pageCount

                        // Limit rendering to 50 pages to prevent OOM errors on massive books
                        val renderLimit = minOf(totalPages, 50)
                        for (i in 0 until renderLimit) {
                            val page = renderer.openPage(i)
                            val bitmap = Bitmap.createBitmap(240, 320, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(bitmap)
                            canvas.drawColor(android.graphics.Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                            generatedPages.add(ExtractPage(id = i + 1, isSelected = false, thumbnail = bitmap))
                            page.close()
                        }

                        // Fill remainder with empty thumbnails if PDF > 50 pages
                        for (i in renderLimit until totalPages) {
                            generatedPages.add(ExtractPage(id = i + 1, isSelected = false, thumbnail = null))
                        }
                        renderer.close()
                    }
                } catch (e: Exception) { e.printStackTrace() }

                withContext(Dispatchers.Main) {
                    fileName = name
                    fileSizeMb = Math.round((sizeBytes / (1024.0 * 1024.0)) * 10.0) / 10.0
                    filePages = totalPages
                    pages.clear()
                    pages.addAll(generatedPages)
                    isParsing = false
                }
            }
        }
    }

    LaunchedEffect(selectedCount) {
        rangeInput = generateRangeString(pages)
    }

    val handleApplyRange = {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val selectedIds = parseRangeString(rangeInput)
        pages.forEachIndexed { index, page ->
            pages[index] = page.copy(isSelected = selectedIds.contains(page.id))
        }
    }

    Scaffold(
        containerColor = Surface,
        topBar = { ExtractTopBar(onNavigateBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            ExtractBottomBar(selectedCount, isProcessing, processingComplete, hasFile) {
                if (hasFile && selectedCount > 0) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    coroutineScope.launch(Dispatchers.IO) {
                        isProcessing = true
                        try {
                            val cleanFileName = fileName.removeSuffix(".pdf")
                            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

                            val selectedIndices = pages.filter { it.isSelected }.map { it.id - 1 }
                            var finalOutputFile: File? = null
                            var finalMimeType = "application/pdf"

                            context.contentResolver.openInputStream(selectedFileUri!!)?.use { inputStream ->
                                val sourceDoc = PDDocument.load(inputStream, MemoryUsageSetting.setupMixed(50 * 1024 * 1024))

                                if (extractionMode == "single") {
                                    val outputFile = File(downloadsDir, "${cleanFileName}_extracted_${System.currentTimeMillis()}.pdf")
                                    val targetDoc = PDDocument()
                                    for (i in selectedIndices) {
                                        if (i < sourceDoc.numberOfPages) {
                                            targetDoc.addPage(sourceDoc.getPage(i))
                                        }
                                    }
                                    targetDoc.save(outputFile)
                                    targetDoc.close()
                                    finalOutputFile = outputFile
                                    finalMimeType = "application/pdf"
                                } else {
                                    val outputFile = File(downloadsDir, "${cleanFileName}_extracted_${System.currentTimeMillis()}.zip")
                                    ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
                                        for (i in selectedIndices) {
                                            if (i < sourceDoc.numberOfPages) {
                                                val tempDoc = PDDocument()
                                                tempDoc.addPage(sourceDoc.getPage(i))
                                                val baos = ByteArrayOutputStream()
                                                tempDoc.save(baos)
                                                tempDoc.close()

                                                val entry = ZipEntry("${cleanFileName}_page_${i + 1}.pdf")
                                                zos.putNextEntry(entry)
                                                zos.write(baos.toByteArray())
                                                zos.closeEntry()
                                            }
                                        }
                                    }
                                    finalOutputFile = outputFile
                                    finalMimeType = "application/zip"
                                }
                                sourceDoc.close()
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

                                if (result == SnackbarResult.ActionPerformed && finalOutputFile != null) {
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", finalOutputFile!!)
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, finalMimeType)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Open File"))
                                }
                                delay(2000)
                                processingComplete = false
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                isProcessing = false
                                snackbarHostState.showSnackbar("Extraction failed: ${e.localizedMessage}")
                            }
                        }
                    }
                } else if (!hasFile) {
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
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                SourceDocumentStatusCard(
                    hasFile = hasFile,
                    fileName = fileName,
                    fileSizeMb = fileSizeMb,
                    filePages = filePages,
                    isParsing = isParsing,
                    onChangeClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) }
                )

                if (hasFile && !isParsing) {
                    SelectionControlsCard(
                        rangeInput = rangeInput,
                        onRangeChange = { rangeInput = it },
                        selectedCount = selectedCount,
                        onApplyText = handleApplyRange,
                        onSelectAll = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            pages.forEachIndexed { i, p -> pages[i] = p.copy(isSelected = true) }
                        },
                        onSelectNone = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            pages.forEachIndexed { i, p -> pages[i] = p.copy(isSelected = false) }
                        },
                        onSelectInvert = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            pages.forEachIndexed { i, p -> pages[i] = p.copy(isSelected = !p.isSelected) }
                        }
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("DOCUMENT PAGES", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant, letterSpacing = 0.5.sp)
                            Text("Tap card to toggle", fontSize = 10.sp, color = Outline)
                        }

                        val visiblePages = pages.take(50)
                        visiblePages.chunked(3).forEach { rowPages ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                rowPages.forEach { page ->
                                    ExtractPageTile(
                                        page = page,
                                        modifier = Modifier.weight(1f),
                                        onSelectToggle = {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            val idx = pages.indexOfFirst { it.id == page.id }
                                            if (idx != -1) pages[idx] = page.copy(isSelected = !page.isSelected)
                                        }
                                    )
                                }
                                if (rowPages.size < 3) {
                                    repeat(3 - rowPages.size) { Spacer(modifier = Modifier.weight(1f)) }
                                }
                            }
                        }

                        if (filePages > 50) {
                            val hiddenCount = filePages - 50
                            Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.Center) {
                                Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(SurfaceContainer).padding(horizontal = 12.dp, vertical = 6.dp)) {
                                    Text("+ $hiddenCount additional pages hidden to save memory", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = OnSurfaceVariant)
                                }
                            }
                        }
                    }

                    ExtractionModeCard(
                        extractionMode = extractionMode,
                        onModeChange = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            extractionMode = it
                        }
                    )

                    AdvancedSettingsCard(
                        deleteOriginal = deleteOriginal,
                        onToggle = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            deleteOriginal = !deleteOriginal
                        }
                    )

                    SpeedBenefitCard()
                }

                Spacer(modifier = Modifier.height(112.dp))
            }
        }
    }
}

// ---- HELPER LOGIC ----
private fun generateRangeString(pages: List<ExtractPage>): String {
    val selected = pages.filter { it.isSelected }.map { it.id }.sorted()
    if (selected.isEmpty()) return ""
    return selected.joinToString(", ")
}

private fun parseRangeString(input: String): Set<Int> {
    val parts = input.split(",").map { it.trim() }
    val wanted = mutableSetOf<Int>()
    parts.forEach { part ->
        if (part.contains("-")) {
            val rangeParts = part.split("-").mapNotNull { it.toIntOrNull() }
            if (rangeParts.size == 2) {
                val start = minOf(rangeParts[0], rangeParts[1])
                val end = maxOf(rangeParts[0], rangeParts[1])
                for (i in start..end) wanted.add(i)
            }
        } else {
            part.toIntOrNull()?.let { wanted.add(it) }
        }
    }
    return wanted
}
// ---- END LOGIC ----

@Composable
private fun ExtractTopBar(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface.copy(alpha = 0.95f))
            .shadow(elevation = 1.dp, spotColor = Color.Black.copy(alpha = 0.04f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).clickable { onNavigateBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = OnSurface)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text("Extract Pages", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = OnSurface)
            }
        }
    }
}

@Composable
private fun SourceDocumentStatusCard(hasFile: Boolean, fileName: String, fileSizeMb: Double, filePages: Int, isParsing: Boolean, onChangeClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaPulse by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = if (isParsing) 0.4f else 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutLinearInEasing), RepeatMode.Reverse), label = "alphaPulse"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.08f))
            .background(SurfaceContainerLowest, RoundedCornerShape(16.dp))
            .clickable(enabled = !hasFile && !isParsing) { onChangeClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (hasFile) ErrorContainer.copy(alpha = 0.6f) else SurfaceContainerLow)
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
            Column {
                if (hasFile) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(fileName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Text("$filePages pages", fontSize = 12.sp, color = OnSurfaceVariant)
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(OutlineVariant))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("$fileSizeMb MB", fontSize = 12.sp, color = OnSurfaceVariant)
                        if (isParsing) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(OutlineVariant))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Indexing...", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                        }
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
                .clickable(enabled = !isParsing) { onChangeClick() }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(if (hasFile) "Change" else "Select", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isParsing) OutlineVariant else Primary)
        }
    }
}

@Composable
private fun SelectionControlsCard(
    rangeInput: String, onRangeChange: (String) -> Unit, selectedCount: Int,
    onApplyText: () -> Unit, onSelectAll: () -> Unit, onSelectNone: () -> Unit, onSelectInvert: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.08f))
            .background(SurfaceContainerLowest, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            // Added weight(1f) so this side yields horizontal space to the buttons if needed
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text("Selection", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.width(6.dp))
                Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(PrimaryContainer).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("Active", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OnPrimaryContainer)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            // Tightened the spacing between the action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                QuickActionBtn("All", onSelectAll)
                QuickActionBtn("None", onSelectNone)
                QuickActionBtn("Invert", onSelectInvert)
            }
        }

        BasicTextField(
            value = rangeInput, onValueChange = onRangeChange,
            textStyle = TextStyle(fontSize = 13.sp, color = OnSurface),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceContainerLow).padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Outlined.Keyboard, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (rangeInput.isEmpty()) Text("e.g. 2, 3, 5, 12-16", fontSize = 13.sp, color = Outline)
                        innerTextField()
                    }
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(PrimaryContainer).clickable { onApplyText() }.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Apply", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OnPrimary)
                    }
                }
            }
        )
    }
}

@Composable
private fun QuickActionBtn(title: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(SurfaceContainerLow)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp) // Tightened horizontal padding
    ) {
        // Enforced softWrap = false to completely prevent the text from breaking into two lines
        Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun ExtractPageTile(page: ExtractPage, modifier: Modifier = Modifier, onSelectToggle: () -> Unit) {
    val borderColor = if (page.isSelected) PrimaryContainer.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.04f)
    val elevation = if (page.isSelected) 4.dp else 1.dp
    val thumbBg = if (page.isSelected) SurfaceContainerHigh.copy(alpha = 0.6f) else SurfaceContainerLow

    val pColor = if (page.isSelected) Primary else Outline
    val checkBg = if (page.isSelected) PrimaryContainer else SurfaceContainerLowest
    val checkTint = if (page.isSelected) OnPrimary else Color.Transparent

    val labelColor = if (page.isSelected) Primary else OnSurface
    val labelWeight = if (page.isSelected) FontWeight.Bold else FontWeight.Medium

    Column(
        modifier = modifier
            .shadow(elevation, RoundedCornerShape(16.dp), spotColor = borderColor)
            .background(SurfaceContainerLowest, RoundedCornerShape(16.dp))
            .clickable { onSelectToggle() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(0.75f).clip(RoundedCornerShape(10.dp)).background(thumbBg)
        ) {
            if (page.thumbnail != null) {
                Image(
                    bitmap = page.thumbnail.asImageBitmap(),
                    contentDescription = "Page ${page.id}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().alpha(if (page.isSelected) 1f else 0.8f)
                )
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(6.dp)).background(SurfaceContainer), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Article, contentDescription = null, tint = Primary.copy(alpha = 0.5f), modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(SurfaceContainerLowest.copy(alpha = 0.9f)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                    Text("P.${page.id}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = pColor)
                }
                Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(checkBg).border(1.dp, if(page.isSelected) Color.Transparent else OutlineVariant, CircleShape).shadow(if(page.isSelected) 2.dp else 0.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Check, contentDescription = null, tint = checkTint, modifier = Modifier.size(14.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Page ${page.id}", fontSize = 12.sp, fontWeight = labelWeight, color = labelColor)
    }
}

@Composable
private fun ExtractionModeCard(extractionMode: String, onModeChange: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.08f))
            .background(SurfaceContainerLowest, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Extraction Mode", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurface)
            Icon(Icons.Outlined.Tune, contentDescription = null, tint = Outline, modifier = Modifier.size(20.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if(extractionMode == "single") SurfaceContainerLow else Color.Transparent).clickable { onModeChange("single") }.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            RadioButton(selected = extractionMode == "single", onClick = { onModeChange("single") }, modifier = Modifier.size(20.dp).padding(top = 2.dp), colors = RadioButtonDefaults.colors(selectedColor = Primary, unselectedColor = Outline))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Single Combined PDF", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(PrimaryContainer).padding(horizontal = 8.dp, vertical = 2.dp)) {
                        Text("Recommended", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OnPrimaryContainer)
                    }
                }
                Text("Merges the extracted sequence into one unified, ready-to-share document.", fontSize = 12.sp, color = OnSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if(extractionMode == "separate") SurfaceContainerLow else Color.Transparent).clickable { onModeChange("separate") }.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            RadioButton(selected = extractionMode == "separate", onClick = { onModeChange("separate") }, modifier = Modifier.size(20.dp).padding(top = 2.dp), colors = RadioButtonDefaults.colors(selectedColor = Primary, unselectedColor = Outline))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Separate Pages (ZIP Archive)", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                Text("Produces individual PDF files per page packaged into a lightweight ZIP.", fontSize = 12.sp, color = OnSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun AdvancedSettingsCard(deleteOriginal: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.08f))
            .background(SurfaceContainerLowest, RoundedCornerShape(16.dp))
            .clickable { onToggle() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(SurfaceContainerHigh), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.AutoDelete, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text("Delete pages from source", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                Spacer(modifier = Modifier.height(2.dp))
                Text("Non-destructive duplicate created in Trash", fontSize = 12.sp, color = OnSurfaceVariant)
            }
        }
        val offset by animateFloatAsState(targetValue = if (deleteOriginal) 20f else 0f, label = "switch")
        val bgColor = if (deleteOriginal) PrimaryContainer else SurfaceContainerHigh
        Box(modifier = Modifier.width(44.dp).height(24.dp).clip(RoundedCornerShape(50)).background(bgColor).padding(2.dp)) {
            Box(modifier = Modifier.size(20.dp).offset(x = offset.dp).clip(CircleShape).background(SurfaceContainerLowest).shadow(1.dp, CircleShape))
        }
    }
}

@Composable
private fun SpeedBenefitCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = PrimaryContainer.copy(alpha = 0.25f))
            .background(Brush.linearGradient(listOf(PrimaryContainer, Secondary)), RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text("Zero Cloud Upload", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(2.dp))
                Text("Processed instantly in device WebAssembly sandbox.", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
            }
        }
        Text("0.3s", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun ExtractBottomBar(selectedCount: Int, isProcessing: Boolean, isComplete: Boolean, hasFile: Boolean, onClick: () -> Unit) {
    val estSize = "%.1f".format(selectedCount * 0.47)
    val buttonText = when {
        !hasFile -> "Select a PDF to Extract"
        isProcessing -> "Generating PDF..."
        isComplete -> "Saved to Downloads"
        selectedCount == 0 -> "Select Pages to Extract"
        else -> "Extract $selectedCount ${if(selectedCount==1)"Page" else "Pages"}"
    }
    val buttonIcon = when {
        !hasFile -> Icons.Outlined.Folder
        isProcessing -> Icons.Outlined.Refresh
        isComplete -> Icons.Outlined.CheckCircle
        else -> Icons.Outlined.Download
    }
    val buttonColor = if (isComplete) Secondary else if (hasFile) PrimaryContainer else SurfaceContainerHigh
    val contentColor = if (hasFile || isComplete) OnPrimaryContainer else OutlineVariant
    val cornerRadius by animateDpAsState(targetValue = if (isProcessing) 50.dp else 16.dp, label = "buttonShape")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface.copy(alpha = 0.98f))
            .shadow(elevation = 24.dp, spotColor = OnSurface.copy(alpha = 0.1f))
            .navigationBarsPadding()
    ) {
        if (hasFile) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(PrimaryContainer))
                    Spacer(modifier = Modifier.width(6.dp))
                    AnimatedContent(
                        targetState = selectedCount,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInVertically { height -> height } + fadeIn()).togetherWith(slideOutVertically { height -> -height } + fadeOut())
                            } else {
                                (slideInVertically { height -> -height } + fadeIn()).togetherWith(slideOutVertically { height -> height } + fadeOut())
                            }.using(SizeTransform(clip = false))
                        }, label = "countAnim"
                    ) { count ->
                        Text("$count pages selected", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                    }
                }
                Text(if(selectedCount>0) "~$estSize MB estimated" else "0 MB", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant)
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
                .shadow(if (!isComplete && selectedCount > 0 && !isProcessing && hasFile) 8.dp else 0.dp, RoundedCornerShape(cornerRadius), spotColor = PrimaryContainer.copy(alpha = 0.35f))
                .clip(RoundedCornerShape(cornerRadius))
                .background(if(selectedCount==0 && hasFile) buttonColor.copy(alpha = 0.5f) else buttonColor)
                .clickable(enabled = (!hasFile) || (selectedCount > 0 && !isProcessing && !isComplete)) { onClick() },
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
    }
}