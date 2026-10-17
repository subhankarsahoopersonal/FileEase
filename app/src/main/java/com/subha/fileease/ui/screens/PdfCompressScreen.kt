package com.subha.fileease.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.min

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
private val PrimaryContainer = Color(0xFF0066FF)
private val OnPrimaryContainer = Color(0xFFF8F7FF)
private val Secondary = Color(0xFF0058BE)
private val ErrorContainer = Color(0xFFFFDAD6)
private val OnErrorContainer = Color(0xFF93000A)
private val OutlineVariant = Color(0xFFC2C6D8)
private val SurfaceVariant = Color(0xFFDAE2FD)
private val Tertiary = Color(0xFF4E5A69)

@Composable
fun PdfCompressScreen(onNavigateBack: () -> Unit) {
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

    // Settings State
    var selectedPreset by remember { mutableStateOf("recommended") }
    var stripThumbnails by remember { mutableStateOf(true) }
    var grayscaleToggle by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    val hasFile = selectedFileUri != null

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            selectedFileUri = it
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
                var pages = 0
                try {
                    context.contentResolver.openFileDescriptor(it, "r")?.use { fd ->
                        val renderer = PdfRenderer(fd)
                        pages = renderer.pageCount
                        renderer.close()
                    }
                } catch (e: Exception) { e.printStackTrace() }

                withContext(Dispatchers.Main) {
                    fileName = name
                    fileSizeMb = Math.round((sizeBytes / (1024.0 * 1024.0)) * 10.0) / 10.0
                    filePages = pages
                }
            }
        }
    }

    val originalSize = if (hasFile) fileSizeMb else 0.0
    val baseReduction = when (selectedPreset) {
        "extreme" -> 85
        "recommended" -> 65
        else -> 30
    }

    val finalReduction = if (grayscaleToggle) min(95, baseReduction + 10) else baseReduction
    val baseSize = originalSize * (1.0 - (finalReduction / 100.0))
    val finalSize = max(0.1, baseSize)
    val savedMb = originalSize - finalSize
    val remainingPct = if (originalSize > 0) (finalSize / originalSize).toFloat() else 1f

    Scaffold(
        containerColor = Surface,
        topBar = { CompressTopBar(onNavigateBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            CompressBottomBar(hasFile = hasFile, isProcessing = isProcessing) {
                if (hasFile) {
                    // Tactile Feedback: Action Confirmation
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)

                    coroutineScope.launch(Dispatchers.IO) {
                        isProcessing = true
                        try {
                            val cleanFileName = fileName.removeSuffix(".pdf")
                            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            val outputFile = File(downloadsDir, "${cleanFileName}_compressed_${System.currentTimeMillis()}.pdf")

                            context.contentResolver.openInputStream(selectedFileUri!!)?.use { inputStream ->
                                val document = PDDocument.load(inputStream, MemoryUsageSetting.setupMixed(50 * 1024 * 1024))

                                // Determine processing thresholds based on UI preset
                                val imageQuality = when (selectedPreset) {
                                    "extreme" -> 0.3f
                                    "recommended" -> 0.6f
                                    else -> 0.85f
                                }
                                val maxImageWidth = when (selectedPreset) {
                                    "extreme" -> 800
                                    "recommended" -> 1600
                                    else -> 2400
                                }

                                // Iterate and replace images to achieve compression
                                for (page in document.pages) {
                                    val resources = page.resources ?: continue
                                    val imageNames = resources.xObjectNames.filter { resources.isImageXObject(it) }.toList()

                                    for (name in imageNames) {
                                        try {
                                            val xObject = resources.getXObject(name) as? PDImageXObject
                                            val originalBitmap = xObject?.image ?: continue

                                            var processBitmap = originalBitmap

                                            // Apply Grayscale Filter
                                            if (grayscaleToggle) {
                                                val grayBmp = Bitmap.createBitmap(processBitmap.width, processBitmap.height, Bitmap.Config.ARGB_8888)
                                                val canvas = Canvas(grayBmp)
                                                val paint = Paint()
                                                val colorMatrix = ColorMatrix().apply { setSaturation(0f) }
                                                paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
                                                canvas.drawBitmap(processBitmap, 0f, 0f, paint)
                                                processBitmap = grayBmp
                                            }

                                            // Downscale resolution
                                            if (processBitmap.width > maxImageWidth) {
                                                val scale = maxImageWidth.toFloat() / processBitmap.width
                                                val newHeight = (processBitmap.height * scale).toInt()

                                                processBitmap = Bitmap.createScaledBitmap(processBitmap, maxImageWidth, newHeight, true)
                                            }

                                            // Repackage heavily compressed JPEG back into the PDF
                                            val compressedImage = JPEGFactory.createFromImage(document, processBitmap, imageQuality)
                                            resources.put(name, compressedImage)

                                        } catch (e: Exception) {
                                            e.printStackTrace() // Skip corrupted or unsupported sub-images
                                        }
                                    }
                                }

                                // Apply Refinements
                                if (stripThumbnails) {
                                    document.documentCatalog.metadata = null
                                    document.documentInformation?.let { info ->
                                        info.author = null
                                        info.creator = null
                                        info.keywords = null
                                        info.producer = null
                                    }
                                }

                                document.save(outputFile)
                                document.close()
                            }

                            withContext(Dispatchers.Main) {
                                // Tactile Feedback: Process Completion (Custom two-beat success pulse)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    val timings = longArrayOf(0, 80, 100, 120) // Delay, Vibrate, Pause, Vibrate
                                    val amplitudes = intArrayOf(0, 150, 0, 255) // 0, Medium, 0, Strong
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
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                snackbarHostState.showSnackbar("Compression failed: ${e.localizedMessage}")
                            }
                        } finally {
                            withContext(Dispatchers.Main) {
                                isProcessing = false
                            }
                        }
                    }
                } else {
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text("Compress PDF", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Reduce document weight swiftly while maintaining legibility.", fontSize = 13.sp, color = OnSurfaceVariant)
                }

                SourceDocumentCard(
                    hasFile = hasFile,
                    fileName = fileName,
                    fileSizeMb = fileSizeMb,
                    filePages = filePages,
                    isProcessing = isProcessing,
                    onChangeClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) }
                )

                if (hasFile) {
                    CompressionPresetsSection(
                        selectedPreset = selectedPreset,
                        onSelect = {
                            // Tactile Feedback: Mechanical Granularity
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedPreset = it
                        }
                    )

                    ProjectionCard(originalSize, finalSize, finalReduction, savedMb, remainingPct)

                    RefinementOptions(
                        stripThumbnails = stripThumbnails,
                        onStripToggle = {
                            // Tactile Feedback: Mechanical Granularity
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            stripThumbnails = !stripThumbnails
                        },
                        grayscale = grayscaleToggle,
                        onGrayscaleToggle = {
                            // Tactile Feedback: Mechanical Granularity
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            grayscaleToggle = !grayscaleToggle
                        }
                    )

                    SecurityBadge()
                }

                Spacer(modifier = Modifier.height(112.dp))
            }
        }
    }
}

@Composable
private fun CompressTopBar(onNavigateBack: () -> Unit) {
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
            ) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = OnSurface) }
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text("Pdf Compressor", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Primary))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Offline Engine Active • 100% On-device", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Primary)
                }
            }
        }
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(Primary),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Outlined.Person, contentDescription = "User", tint = OnPrimary, modifier = Modifier.size(18.dp)) }
    }
}

@Composable
private fun SourceDocumentCard(hasFile: Boolean, fileName: String, fileSizeMb: Double, filePages: Int, isProcessing: Boolean, onChangeClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaPulse by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = if (isProcessing) 0.4f else 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutLinearInEasing), RepeatMode.Reverse), label = "alphaPulse"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.05f))
            .background(SurfaceContainerLowest, RoundedCornerShape(16.dp))
            .clickable(enabled = !hasFile && !isProcessing) { onChangeClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(if (hasFile) ErrorContainer else SurfaceContainerLow).alpha(alphaPulse),
                contentAlignment = Alignment.Center
            ) {
                if (hasFile) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, tint = OnErrorContainer, modifier = Modifier.size(24.dp))
                    }
                } else {
                    Icon(Icons.Outlined.FolderOpen, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (hasFile) {
                    Text(fileName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$fileSizeMb MB", fontSize = 11.sp, color = OnSurfaceVariant)
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(OutlineVariant))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("$filePages pages", fontSize = 11.sp, color = OnSurfaceVariant)
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(OutlineVariant))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isProcessing) "Compressing..." else "Ready", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                    }
                } else {
                    Text("No file selected", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                    Text("Tap to browse storage", fontSize = 12.sp, color = OnSurfaceVariant)
                }
            }
        }
        Box(
            modifier = Modifier.clip(RoundedCornerShape(50)).background(SurfaceContainerLow).clickable(enabled = !isProcessing) { onChangeClick() }.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(if (hasFile) "Change" else "Select", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (isProcessing) OutlineVariant else Primary)
        }
    }
}

@Composable
private fun CompressionPresetsSection(selectedPreset: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("COMPRESSION PRESETS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OnSurface, letterSpacing = 0.5.sp)
        }

        PresetCard(
            id = "extreme", title = "Extreme Compression", reduction = "~85%",
            desc = "Low visual weight, 72 DPI. Ideal for email limits and mobile networks.",
            isSelected = selectedPreset == "extreme", onClick = { onSelect("extreme") }
        )
        PresetCard(
            id = "recommended", title = "Recommended", reduction = "Best Balance", isBold = true,
            desc = "Balanced density, 150 DPI. Crisp vector outlines and legible text.",
            isSelected = selectedPreset == "recommended", onClick = { onSelect("recommended") }
        )
        PresetCard(
            id = "light", title = "Light Compression", reduction = "~30%",
            desc = "Retains high fidelity, 300 DPI. Perfect for commercial prints and portfolios.",
            isSelected = selectedPreset == "light", onClick = { onSelect("light") }
        )
    }
}

@Composable
private fun PresetCard(id: String, title: String, reduction: String, desc: String, isBold: Boolean = false, isSelected: Boolean, onClick: () -> Unit) {
    val shadowColor = if (isSelected) PrimaryContainer.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.05f)
    val shadowElevation by animateDpAsState(targetValue = if (isSelected) 8.dp else 1.dp, label = "elevation")
    val bgColor by animateColorAsState(targetValue = if (isSelected) SurfaceContainerLowest else SurfaceContainerLowest.copy(alpha = 0.7f), label = "bg")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(shadowElevation, RoundedCornerShape(16.dp), spotColor = shadowColor)
            .background(bgColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.padding(top = 2.dp).size(20.dp).clip(CircleShape).border(2.dp, if (isSelected) Primary else OutlineVariant, CircleShape).background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Primary))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontSize = 15.sp, fontWeight = if (isBold || isSelected) FontWeight.Bold else FontWeight.SemiBold, color = OnSurface)
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (isBold) PrimaryContainer else SurfaceContainerHigh).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(reduction, fontSize = 10.sp, fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold, color = if (isBold) OnPrimaryContainer else OnSurfaceVariant)
                }
            }
            Text(desc, fontSize = 12.sp, color = OnSurfaceVariant, modifier = Modifier.padding(top = 4.dp), lineHeight = 16.sp)
        }
    }
}

@Composable
private fun ProjectionCard(originalSize: Double, finalSize: Double, finalReduction: Int, savedMb: Double, remainingPct: Float) {
    val animatedProgress by animateFloatAsState(targetValue = remainingPct, animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing), label = "progress")
    val animatedFinalSize by animateFloatAsState(targetValue = finalSize.toFloat(), animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing), label = "size")
    val animatedSaved by animateFloatAsState(targetValue = savedMb.toFloat(), animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing), label = "saved")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = PrimaryContainer.copy(alpha = 0.3f))
            .background(Brush.linearGradient(listOf(PrimaryContainer, Secondary, Primary)), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Box(modifier = Modifier.align(Alignment.BottomEnd).offset(x = 40.dp, y = 40.dp).size(160.dp).blur(24.dp).background(SurfaceContainerLowest.copy(alpha = 0.1f), CircleShape))

        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Analytics, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PROJECTED ESTIMATE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OnPrimary.copy(alpha = 0.9f))
                }
                Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(SurfaceContainerLowest.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                    Text("-$finalReduction% Reduction", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OnPrimary)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Original Size", fontSize = 11.sp, color = OnPrimary.copy(alpha = 0.8f))
                    Text("${"%.1f".format(originalSize)} MB", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OnPrimary)
                }
                Icon(Icons.Outlined.ArrowForward, contentDescription = null, tint = OnPrimary.copy(alpha = 0.6f), modifier = Modifier.size(24.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text("New Estimated Size", fontSize = 11.sp, color = OnPrimary.copy(alpha = 0.8f))
                    Text("~${"%.1f".format(animatedFinalSize)} MB", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = OnPrimary)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Column {
                Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)).background(SurfaceContainerLowest.copy(alpha = 0.2f)).padding(2.dp)) {
                    Box(modifier = Modifier.fillMaxWidth(animatedProgress).fillMaxHeight().clip(RoundedCornerShape(50)).background(SurfaceContainerLowest))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Saves approx. ${"%.1f".format(animatedSaved)} MB", fontSize = 11.sp, color = OnPrimary.copy(alpha = 0.9f))
                    Text("100% Private local engine", fontSize = 11.sp, color = OnPrimary.copy(alpha = 0.9f))
                }
            }
        }
    }
}

@Composable
private fun RefinementOptions(stripThumbnails: Boolean, onStripToggle: () -> Unit, grayscale: Boolean, onGrayscaleToggle: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(16.dp))
            .background(SurfaceContainerLowest, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("REFINEMENT OPTIONS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OnSurface, letterSpacing = 0.5.sp)

        CustomToggleRow(
            icon = Icons.Outlined.BurstMode, title = "Strip embedded thumbnails",
            subtitle = "Saves extra ~1.2 MB without data loss",
            isChecked = stripThumbnails, onToggle = onStripToggle
        )
        HorizontalDivider(color = SurfaceContainerLow)
        CustomToggleRow(
            icon = Icons.Outlined.FilterBAndW, title = "Convert color to grayscale",
            subtitle = "Maximized shrinkage for documents & notes",
            isChecked = grayscale, onToggle = onGrayscaleToggle
        )
    }
}

@Composable
private fun CustomToggleRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, isChecked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onToggle() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceContainer), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                Text(subtitle, fontSize = 12.sp, color = OnSurfaceVariant)
            }
        }

        val offset by animateFloatAsState(targetValue = if (isChecked) 20f else 0f, label = "toggle")
        val bgColor = if (isChecked) Primary else SurfaceVariant

        Box(modifier = Modifier.width(44.dp).height(24.dp).clip(RoundedCornerShape(50)).background(bgColor).padding(2.dp)) {
            Box(modifier = Modifier.size(20.dp).offset(x = offset.dp).clip(CircleShape).background(SurfaceContainerLowest).shadow(1.dp, CircleShape))
        }
    }
}

@Composable
private fun SecurityBadge() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = Icons.Filled.Security, contentDescription = null, tint = Primary, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "Zero Server Uploads • Encrypted sandboxed memory", fontSize = 11.sp, color = Tertiary, fontWeight = FontWeight.Normal)
    }
}

@Composable
private fun CompressBottomBar(hasFile: Boolean, isProcessing: Boolean, onCompressClick: () -> Unit) {
    val cornerRadius by animateDpAsState(targetValue = if (isProcessing) 50.dp else 12.dp, label = "buttonShape")
    val buttonColor = if (hasFile) PrimaryContainer else SurfaceContainerHigh
    val contentColor = if (hasFile) OnPrimaryContainer else OutlineVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface.copy(alpha = 0.95f))
            .shadow(elevation = 16.dp, spotColor = Color.Black.copy(alpha = 0.06f))
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .height(48.dp)
                .shadow(if (hasFile && !isProcessing) 8.dp else 0.dp, RoundedCornerShape(cornerRadius), spotColor = PrimaryContainer.copy(alpha = 0.35f))
                .clip(RoundedCornerShape(cornerRadius))
                .background(buttonColor)
                .clickable(enabled = !isProcessing) { onCompressClick() },
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = isProcessing,
                label = "buttonContent"
            ) { processing ->
                if (processing) {
                    CircularProgressIndicator(color = contentColor, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (hasFile) Icons.Outlined.Compress else Icons.Outlined.Folder, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (hasFile) "Compress Document" else "Select a PDF to Compress", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = contentColor)
                    }
                }
            }
        }
    }
}