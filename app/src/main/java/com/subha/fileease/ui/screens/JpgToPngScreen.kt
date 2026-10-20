package com.subha.fileease.ui.screens

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.MediaScannerConnection
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

// Exact Tailwind Color Tokens from Mockup
private val Background = Color(0xFFFAF8FF)
private val Surface = Color(0xFFFAF8FF)
private val SurfaceContainerLowest = Color(0xFFFFFFFF)
private val SurfaceContainerLow = Color(0xFFF2F3FF)
private val SurfaceContainer = Color(0xFFEAEDFF)
private val SurfaceContainerHigh = Color(0xFFE2E7FF)
private val InverseSurface = Color(0xFF283044)
private val OnSurface = Color(0xFF131B2E)
private val OnSurfaceVariant = Color(0xFF424656)
private val Primary = Color(0xFF0050CB)
private val OnPrimary = Color(0xFFFFFFFF)
private val PrimaryContainer = Color(0xFF0066FF)
private val OnPrimaryContainer = Color(0xFFF8F7FF)
private val Secondary = Color(0xFF0058BE)
private val SecondaryContainer = Color(0xFF2170E4)
private val Tertiary = Color(0xFF4E5A69)
private val Outline = Color(0xFF727687)
private val OutlineVariant = Color(0xFFC2C6D8)
private val Error = Color(0xFFBA1A1A)
private val ErrorContainer = Color(0xFFFFDAD6)

data class LoadedJpg(
    val uri: Uri,
    val name: String,
    val dimensions: String,
    val sizeMb: Double,
    val sizeBytes: Long
)

@Composable
fun JpgToPngScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val vibrator = remember { context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Engine & Output State
    var loadedImage by remember { mutableStateOf<LoadedJpg?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var isComplete by remember { mutableStateOf(false) }
    var outputUri by remember { mutableStateOf<Uri?>(null) }

    // Cancellation State
    var conversionJob by remember { mutableStateOf<Job?>(null) }
    var isCancelling by remember { mutableStateOf(false) }

    // UI State
    var addAlpha by remember { mutableStateOf(true) }
    var removeBg by remember { mutableStateOf(false) }
    var bitDepth by remember { mutableStateOf(32) }
    var compression by remember { mutableStateOf("max") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                var name = "Image.jpg"
                var sizeBytes = 0L

                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        name = cursor.getString(nameIdx) ?: "Image.jpg"
                        sizeBytes = cursor.getLong(sizeIdx)
                    }
                }

                try {
                    val dims = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = ImageDecoder.createSource(context.contentResolver, uri)
                        val info = ImageDecoder.decodeDrawable(source).intrinsicWidth to ImageDecoder.decodeDrawable(source).intrinsicHeight
                        "${info.first} × ${info.second}"
                    } else {
                        val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        context.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, options) }
                        "${options.outWidth} × ${options.outHeight}"
                    }

                    val sizeMb = sizeBytes / (1024.0 * 1024.0)

                    withContext(Dispatchers.Main) {
                        loadedImage = LoadedJpg(uri, name, dims, sizeMb, sizeBytes)
                        isComplete = false
                        outputUri = null
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    Scaffold(
        containerColor = Background,
        topBar = { JpgToPngTopBar(onNavigateBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            JpgToPngBottomBar(
                isProcessing = isProcessing,
                isCancelling = isCancelling,
                isComplete = isComplete,
                isEnabled = loadedImage != null,
                onConvert = {
                    if (loadedImage != null && !isProcessing) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)

                        val usableSpaceMb = Environment.getDataDirectory().usableSpace / (1024 * 1024)
                        if (usableSpaceMb < 15) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Not enough storage space. Please free up at least 15MB to convert this file.")
                            }
                            return@JpgToPngBottomBar
                        }

                        conversionJob = coroutineScope.launch(Dispatchers.IO) {
                            isProcessing = true
                            isCancelling = false
                            var pendingDestUri: Uri? = null

                            try {
                                val sourceBitmap = runCatching {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                        val source = ImageDecoder.createSource(context.contentResolver, loadedImage!!.uri)
                                        ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                                            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                                            decoder.isMutableRequired = true
                                        }
                                    } else {
                                        val options = android.graphics.BitmapFactory.Options()
                                        options.inPreferredConfig = if (addAlpha || bitDepth == 32) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
                                        context.contentResolver.openInputStream(loadedImage!!.uri)?.use {
                                            android.graphics.BitmapFactory.decodeStream(it, null, options)
                                        } ?: throw Exception("Failed to open stream")
                                    }
                                }.getOrElse { error ->
                                    if (error is IllegalArgumentException) {
                                        throw Exception("Unsupported color profile. Please use an RGB image.")
                                    } else throw error
                                }

                                if (!isActive) throw kotlinx.coroutines.CancellationException("Cancelled by user")

                                val outBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    sourceBitmap.copy(if (addAlpha || bitDepth == 32) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565, true).also {
                                        sourceBitmap.recycle()
                                    }
                                } else {
                                    sourceBitmap
                                }

                                if (removeBg && (addAlpha || bitDepth == 32)) {
                                    val width = outBitmap.width
                                    val height = outBitmap.height
                                    val pixels = IntArray(width * height)
                                    outBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                                    for (i in pixels.indices) {
                                        if (pixels[i] == android.graphics.Color.WHITE) {
                                            pixels[i] = android.graphics.Color.TRANSPARENT
                                        }
                                    }
                                    outBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
                                }

                                val quality = if (compression == "max") 100 else 80
                                val baseName = loadedImage!!.name.substringBeforeLast(".")
                                val fileName = "${baseName}_converted.png"

                                val values = ContentValues().apply {
                                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                                    // Hardcoded to strictly support PNG only
                                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FileEase")
                                }

                                val destUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                                pendingDestUri = destUri

                                destUri?.let { uri ->
                                    context.contentResolver.openOutputStream(uri)?.use { out ->
                                        if (!isActive) throw kotlinx.coroutines.CancellationException("Cancelled by user")
                                        // Hardcoded to strictly support PNG only
                                        outBitmap.compress(Bitmap.CompressFormat.PNG, quality, out)
                                    }

                                    val filePath = getFilePathFromUri(uri)
                                    if (filePath != null) {
                                        // Hardcoded to strictly support PNG only
                                        MediaScannerConnection.scanFile(context, arrayOf(filePath), arrayOf("image/png"), null)
                                    }
                                    outputUri = uri
                                }
                                outBitmap.recycle()

                                withContext(Dispatchers.Main) {
                                    isComplete = true
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 80, 100, 120), intArrayOf(0, 150, 0, 255), -1))
                                    } else {
                                        @Suppress("DEPRECATION")
                                        vibrator.vibrate(longArrayOf(0, 80, 100, 120), -1)
                                    }

                                    coroutineScope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Exported! Delete original JPG to free up ${"%.1f".format(loadedImage!!.sizeMb)} MB?",
                                            actionLabel = "Delete",
                                            duration = SnackbarDuration.Long
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            try {
                                                context.contentResolver.delete(loadedImage!!.uri, null, null)
                                                snackbarHostState.showSnackbar("Original JPG deleted successfully.")
                                                loadedImage = null
                                                isComplete = false
                                                outputUri = null
                                            } catch (e: SecurityException) {
                                                snackbarHostState.showSnackbar("Permission denied by Android Scoped Storage.")
                                            }
                                        }
                                    }
                                }
                            } catch (e: kotlinx.coroutines.CancellationException) {
                                pendingDestUri?.let { context.contentResolver.delete(it, null, null) }
                                withContext(Dispatchers.Main) { snackbarHostState.showSnackbar("Conversion cancelled.") }
                            } catch (e: Exception) {
                                pendingDestUri?.let { context.contentResolver.delete(it, null, null) }
                                withContext(Dispatchers.Main) { snackbarHostState.showSnackbar(e.message ?: "An unknown error occurred.") }
                            } finally {
                                withContext(Dispatchers.Main) {
                                    isProcessing = false
                                    isCancelling = false
                                }
                            }
                        }
                    } else if (loadedImage == null) {
                        imagePickerLauncher.launch("image/jpeg")
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
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share optimized PNG"))
                    }
                },
                onOpen = {
                    outputUri?.let { uri ->
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "image/png")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Open PNG"))
                    }
                },
                onReset = {
                    loadedImage = null
                    isComplete = false
                    outputUri = null
                    imagePickerLauncher.launch("image/jpeg")
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

            Text(
                text = "Convert image formats with transparency & lossless compression.",
                fontSize = 13.sp,
                color = OnSurfaceVariant,
                lineHeight = 20.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Active Source Image Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(12.dp), spotColor = Color.Black.copy(alpha = 0.04f))
                    .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(SurfaceContainer).padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Image, contentDescription = null, tint = Primary, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (loadedImage != null) "Active Source Loaded" else "No Image Loaded", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                    }
                    Row(
                        modifier = Modifier.clickable(enabled = !isProcessing) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            imagePickerLauncher.launch("image/jpeg")
                        }.alpha(if (isProcessing) 0.5f else 1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.SwapHoriz, contentDescription = null, tint = PrimaryContainer, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (loadedImage != null) "Change" else "Browse", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PrimaryContainer)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (loadedImage != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceContainer)) {
                            AsyncImage(
                                model = loadedImage!!.uri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).clip(RoundedCornerShape(4.dp)).background(InverseSurface.copy(alpha = 0.85f)).padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("JPG", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = SurfaceContainerLowest)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(loadedImage!!.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${loadedImage!!.dimensions} • ${"%.1f MB".format(loadedImage!!.sizeMb)} • RGB", fontSize = 12.sp, color = OnSurfaceVariant, modifier = Modifier.padding(top = 2.dp))

                            Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SecondaryContainer))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("8-bit/channel", fontSize = 11.sp, color = OnSurfaceVariant.copy(alpha = 0.8f))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Memory, contentDescription = null, tint = Primary, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("On-device Cached", fontSize = 11.sp, color = OnSurfaceVariant.copy(alpha = 0.8f))
                                }
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceContainerLow).clickable { imagePickerLauncher.launch("image/jpeg") }, contentAlignment = Alignment.Center) {
                        Text("Tap to select a JPG file", fontSize = 13.sp, color = Outline)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Alpha Channel Settings
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(12.dp), spotColor = Color.Black.copy(alpha = 0.04f))
                    .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                    Icon(Icons.Outlined.Opacity, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Alpha Channel & Transparency", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                }

                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text("Add Alpha Transparency Channel", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                        Text("Instantiates 8-bit RGBA mask channel", fontSize = 12.sp, color = OnSurfaceVariant)
                    }
                    CustomSwitch(checked = addAlpha, isEnabled = !isProcessing, onCheckedChange = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); addAlpha = it })
                }
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(1.dp).background(SurfaceContainer))
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text("Auto-detect Solid Background", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                        Text("Removes pure white background natively", fontSize = 12.sp, color = OnSurfaceVariant)
                    }
                    CustomSwitch(checked = removeBg, isEnabled = !isProcessing, onCheckedChange = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); removeBg = it })
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Compression & Bit Depth
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(12.dp), spotColor = Color.Black.copy(alpha = 0.04f))
                    .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Tune, contentDescription = null, tint = Primary, modifier = Modifier.size(19.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Bit Depth", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                        }
                        Text("Color Precision", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = OnSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BitCompCard(modifier = Modifier.weight(1f), title = "24-bit TrueColor", subtitle = "RGB Standard", isSelected = bitDepth == 24) { if (!isProcessing) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); bitDepth = 24 } }
                        BitCompCard(modifier = Modifier.weight(1f), title = "32-bit (RGBA)", subtitle = "With Alpha Plane", isSelected = bitDepth == 32) { if (!isProcessing) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); bitDepth = 32 } }
                    }
                }

                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Compress, contentDescription = null, tint = Primary, modifier = Modifier.size(19.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Deflate Compression", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                        }
                        Text("Lossless Processing", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BitCompCard(modifier = Modifier.weight(1f), title = "Maximum", subtitle = "Smallest File Size", isSelected = compression == "max") { if (!isProcessing) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); compression = "max" } }
                        BitCompCard(modifier = Modifier.weight(1f), title = "Fast Processing", subtitle = "Instant Export", isSelected = compression == "fast") { if (!isProcessing) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); compression = "fast" } }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dynamic Estimate Hero Card
            val formatMultiplier = 0.90
            val compressionFactor = if (compression == "max") 0.85 else 1.0

            val estSize = if (loadedImage != null) {
                "%.1f MB".format(loadedImage!!.sizeMb * formatMultiplier * compressionFactor)
            } else "~0.0 MB"
            val origSize = if (loadedImage != null) "%.1f MB".format(loadedImage!!.sizeMb) else "0.0 MB"

            val savingPercent = if (compression == "max") "-15%" else "-5%"
            val progressWidth by animateFloatAsState(targetValue = (formatMultiplier * compressionFactor).toFloat(), label = "savings")

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.linearGradient(listOf(PrimaryContainer, Secondary, Primary)),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            ) {
                Box(modifier = Modifier.align(Alignment.BottomEnd).offset(x = 32.dp, y = 32.dp).size(128.dp).clip(CircleShape).background(SurfaceContainerLowest.copy(alpha = 0.1f)))

                Column {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(modifier = Modifier.clip(RoundedCornerShape(50)).background(SurfaceContainerLowest.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Dynamic Estimate", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = OnPrimary, letterSpacing = 0.5.sp)
                        }
                        Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(SurfaceContainerLowest.copy(alpha = 0.25f)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                            Text(savingPercent, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnPrimary)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Column {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(estSize, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = OnPrimary, letterSpacing = (-0.5).sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("$origSize JPG", fontSize = 12.sp, color = OnPrimary.copy(alpha = 0.8f), textDecoration = TextDecoration.LineThrough, modifier = Modifier.padding(bottom = 4.dp))
                            }
                            Text("$origSize JPG → $estSize PNG", fontSize = 12.sp, color = OnPrimary.copy(alpha = 0.9f), modifier = Modifier.padding(top = 4.dp))
                        }
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(SurfaceContainerLowest.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.TrendingDown, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(24.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column {
                        Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)).background(SurfaceContainerLowest.copy(alpha = 0.25f))) {
                            Box(modifier = Modifier.fillMaxWidth(progressWidth.coerceIn(0.1f, 1f)).fillMaxHeight().clip(RoundedCornerShape(50)).background(SurfaceContainerLowest))
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Optimized Output", fontSize = 11.sp, color = OnPrimary.copy(alpha = 0.85f))
                            Text("Zero fidelity loss", fontSize = 11.sp, color = OnPrimary.copy(alpha = 0.85f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Privacy Notice
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainerLow)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(SurfaceContainer), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.VerifiedUser, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Zero Data Uploads", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                    Text("Processed inside isolated native Android sandbox", fontSize = 10.sp, color = OnSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

// Utility to resolve true absolute path from Content Uri to feed the MediaScanner natively
private fun getFilePathFromUri(uri: Uri): String? {
    var path: String? = null
    val pathSegments = uri.pathSegments
    if (pathSegments != null && pathSegments.size >= 2) {
        val volumeName = pathSegments[0]
        val documentId = pathSegments[1]
        val parts = documentId.split(":")
        if (parts.size >= 2) {
            val type = parts[0]
            val id = parts[1]
            if ("primary".equals(type, ignoreCase = true)) {
                path = "${Environment.getExternalStorageDirectory()}/$id"
            }
        }
    }
    return path
}

@Composable
private fun JpgToPngTopBar(onNavigateBack: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaPulse by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutLinearInEasing), RepeatMode.Reverse), label = "alphaPulse"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface.copy(alpha = 0.90f))
            .shadow(elevation = 1.dp, spotColor = Color.Black.copy(alpha = 0.04f))
            .padding(horizontal = 16.dp, vertical = 16.dp),
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
                Text("JPG To PNG", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OnSurface, letterSpacing = (-0.01).sp)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(50)).background(SurfaceContainerHigh.copy(alpha=0.6f)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(PrimaryContainer).alpha(alphaPulse))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Offline Engine Active • 100% On-device", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = OnSurfaceVariant)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).clickable { }, contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Info, contentDescription = "Info", tint = OnSurfaceVariant, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(4.dp))
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Primary).shadow(1.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Person, contentDescription = "User", tint = OnPrimary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun BitCompCard(modifier: Modifier, title: String, subtitle: String, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) SurfaceContainer else SurfaceContainerLow
    val titleColor = if (isSelected) Primary else OnSurface
    val subColor = if (isSelected) Primary.copy(alpha = 0.8f) else OnSurfaceVariant

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Text(title, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium, color = titleColor)
        Text(subtitle, fontSize = 10.sp, color = subColor, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun CustomSwitch(checked: Boolean, isEnabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val bg by animateColorAsState(if (checked) PrimaryContainer else SurfaceContainerHigh, label = "switchBg")
    val offset by animateDpAsState(if (checked) 20.dp else 0.dp, label = "switchOffset")

    Box(
        modifier = Modifier
            .width(48.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(50))
            .background(bg.copy(alpha = if (isEnabled) 1f else 0.5f))
            .clickable(enabled = isEnabled) { onCheckedChange(!checked) }
            .padding(4.dp),
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
private fun JpgToPngBottomBar(
    isProcessing: Boolean,
    isCancelling: Boolean,
    isComplete: Boolean,
    isEnabled: Boolean,
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
            .background(SurfaceContainerLowest.copy(alpha = 0.95f))
            .shadow(elevation = 24.dp, spotColor = OnSurface.copy(alpha = 0.06f))
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {

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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
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
                                    CircularProgressIndicator(color = contentColor, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Cancel Extraction", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = contentColor)
                                } else {
                                    Icon(Icons.Outlined.Sync, contentDescription = null, tint = if (isEnabled) contentColor else OutlineVariant, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Convert to PNG & Save", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isEnabled) contentColor else OutlineVariant)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (isComplete) Primary else SecondaryContainer))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isComplete) "File saved securely to native gallery" else "~0.2s local runtime • Instant download",
                    fontSize = 10.sp, color = OnSurfaceVariant
                )
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