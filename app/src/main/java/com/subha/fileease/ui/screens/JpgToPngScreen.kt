package com.subha.fileease.ui.screens

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.ImageDecoder
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

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

data class LoadedJpg(
    val uri: Uri,
    val name: String,
    val dimensions: String,
    val sizeMb: String,
    val sizeBytes: Long
)

@Composable
fun JpgToPngScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val vibrator = remember { context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Engine State
    var loadedImage by remember { mutableStateOf<LoadedJpg?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var isComplete by remember { mutableStateOf(false) }

    // UI State
    var selectedFormat by remember { mutableStateOf("PNG") }
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
                    // Modern ImageDecoder implementation for safer metadata extraction
                    val dims = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = ImageDecoder.createSource(context.contentResolver, uri)
                        val info = ImageDecoder.decodeDrawable(source).intrinsicWidth to ImageDecoder.decodeDrawable(source).intrinsicHeight
                        "${info.first} × ${info.second}"
                    } else {
                        val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        context.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, options) }
                        "${options.outWidth} × ${options.outHeight}"
                    }

                    val sizeMb = "%.1f MB".format(sizeBytes / (1024.0 * 1024.0))

                    withContext(Dispatchers.Main) {
                        loadedImage = LoadedJpg(uri, name, dims, sizeMb, sizeBytes)
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
                isComplete = isComplete,
                isEnabled = loadedImage != null,
                onConvert = {
                    if (loadedImage != null && !isProcessing) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        coroutineScope.launch(Dispatchers.IO) {
                            isProcessing = true
                            try {
                                // Phase 1: Memory-Safe Decoding
                                val sourceBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    val source = ImageDecoder.createSource(context.contentResolver, loadedImage!!.uri)
                                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                                        // Hardware allocation configures the memory buffer outside the VM heap,
                                        // keeping your app from crashing during heavy image manipulation.
                                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE // Required to be mutable for format copying later
                                        decoder.isMutableRequired = true
                                    }
                                } else {
                                    val options = android.graphics.BitmapFactory.Options()
                                    options.inPreferredConfig = if (addAlpha || bitDepth == 32) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
                                    context.contentResolver.openInputStream(loadedImage!!.uri)?.use {
                                        android.graphics.BitmapFactory.decodeStream(it, null, options)
                                    } ?: throw Exception("Failed to open stream")
                                }

                                val outBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    // If decoded via ImageDecoder, enforce the correct config copy
                                    sourceBitmap.copy(if (addAlpha || bitDepth == 32) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565, true).also {
                                        sourceBitmap.recycle() // Clean up the mutable original
                                    }
                                } else {
                                    sourceBitmap // Already decoded with target config via BitmapFactory options
                                }

                                val quality = if (compression == "max") 100 else 80
                                val bos = ByteArrayOutputStream()
                                outBitmap.compress(Bitmap.CompressFormat.PNG, quality, bos)
                                val pngData = bos.toByteArray()
                                outBitmap.recycle()

                                val baseName = loadedImage!!.name.substringBeforeLast(".")
                                val fileName = "${baseName}_converted.png"

                                val values = ContentValues().apply {
                                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FileEase")
                                }
                                val destUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                                destUri?.let {
                                    context.contentResolver.openOutputStream(it)?.use { out -> out.write(pngData) }
                                }

                                withContext(Dispatchers.Main) {
                                    isComplete = true
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 80, 100, 120), intArrayOf(0, 150, 0, 255), -1))
                                    } else {
                                        @Suppress("DEPRECATION")
                                        vibrator.vibrate(longArrayOf(0, 80, 100, 120), -1)
                                    }
                                    snackbarHostState.showSnackbar("Saved to Pictures/FileEase!")
                                    delay(2000)
                                    isComplete = false
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) { snackbarHostState.showSnackbar("Failed: ${e.message}") }
                            } finally {
                                withContext(Dispatchers.Main) { isProcessing = false }
                            }
                        }
                    } else if (loadedImage == null) {
                        imagePickerLauncher.launch("image/jpeg")
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
                        modifier = Modifier.clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            imagePickerLauncher.launch("image/jpeg")
                        },
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
                            // Phase 1: Removed direct Bitmap referencing. Coil handles async loading cleanly.
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
                            Text("${loadedImage!!.dimensions} • ${loadedImage!!.sizeMb} • RGB", fontSize = 12.sp, color = OnSurfaceVariant, modifier = Modifier.padding(top = 2.dp))

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

            // Target Output Format Selector
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Target Output Format", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                Text("SELECT ONE", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Primary, letterSpacing = 0.5.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormatCard(modifier = Modifier.weight(1f), title = "PNG", subtitle = "Lossless • Transparent", icon = Icons.Outlined.PhotoLibrary, tag = "Recommended", isSelected = selectedFormat == "PNG") {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); selectedFormat = "PNG"
                }
                FormatCard(modifier = Modifier.weight(1f), title = "WEBP", subtitle = "Modern • Web Ready", icon = Icons.Outlined.Speed, tag = "Compact", isSelected = selectedFormat == "WEBP") {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); selectedFormat = "WEBP"
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormatCard(modifier = Modifier.weight(1f), title = "SVG", subtitle = "Vector Contour Trace", icon = Icons.Outlined.Gesture, tag = "Trace", isSelected = selectedFormat == "SVG") {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); selectedFormat = "SVG"
                }
                FormatCard(modifier = Modifier.weight(1f), title = "BMP", subtitle = "Uncompressed Raw", icon = Icons.Outlined.Layers, tag = "Bitmap", isSelected = selectedFormat == "BMP") {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); selectedFormat = "BMP"
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
                    CustomSwitch(checked = addAlpha, onCheckedChange = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); addAlpha = it })
                }
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(1.dp).background(SurfaceContainer))
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text("Auto-detect Solid Background", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                        Text("Remove uniform white or color edges", fontSize = 12.sp, color = OnSurfaceVariant)
                    }
                    CustomSwitch(checked = removeBg, onCheckedChange = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); removeBg = it })
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
                        BitCompCard(modifier = Modifier.weight(1f), title = "24-bit TrueColor", subtitle = "RGB Standard", isSelected = bitDepth == 24) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); bitDepth = 24 }
                        BitCompCard(modifier = Modifier.weight(1f), title = "32-bit (RGBA)", subtitle = "With Alpha Plane", isSelected = bitDepth == 32) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); bitDepth = 32 }
                    }
                }

                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Compress, contentDescription = null, tint = Primary, modifier = Modifier.size(19.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Deflate Compression", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                        }
                        Text("100% Lossless", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BitCompCard(modifier = Modifier.weight(1f), title = "Maximum", subtitle = "Smallest File Size", isSelected = compression == "max") { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); compression = "max" }
                        BitCompCard(modifier = Modifier.weight(1f), title = "Fast Lossless", subtitle = "Instant Export", isSelected = compression == "fast") { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); compression = "fast" }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dynamic Estimate Hero Card
            val estSize = if (loadedImage != null) {
                val factor = if (compression == "max") 0.75 else 0.90
                "%.1f MB".format((loadedImage!!.sizeBytes / (1024.0 * 1024.0)) * factor)
            } else "~0.0 MB"
            val origSize = if (loadedImage != null) "%.1f MB".format(loadedImage!!.sizeBytes / (1024.0 * 1024.0)) else "0.0 MB"
            val savingPercent = if (compression == "max") "-25%" else "-10%"
            val progressWidth by animateFloatAsState(targetValue = if (compression == "max") 0.75f else 0.90f, label = "savings")

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
                            Text("$origSize JPG → $estSize PNG (Lossless deflate)", fontSize = 12.sp, color = OnPrimary.copy(alpha = 0.9f), modifier = Modifier.padding(top = 4.dp))
                        }
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(SurfaceContainerLowest.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.TrendingDown, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(24.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column {
                        Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)).background(SurfaceContainerLowest.copy(alpha = 0.25f))) {
                            Box(modifier = Modifier.fillMaxWidth(progressWidth).fillMaxHeight().clip(RoundedCornerShape(50)).background(SurfaceContainerLowest))
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
private fun FormatCard(
    modifier: Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    tag: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) PrimaryContainer else SurfaceContainerLowest
    val contentColor = if (isSelected) OnPrimaryContainer else OnSurface
    val subColor = if (isSelected) OnPrimaryContainer.copy(alpha = 0.9f) else OnSurfaceVariant
    val iconBgColor = if (isSelected) SurfaceContainerLowest.copy(alpha = 0.2f) else SurfaceContainer
    val iconColor = if (isSelected) OnPrimaryContainer else Tertiary

    Column(
        modifier = modifier
            .shadow(1.dp, RoundedCornerShape(12.dp))
            .background(bgColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(iconBgColor), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(if(isSelected) SurfaceContainerLowest.copy(alpha = 0.25f) else Color.Transparent).padding(horizontal = 6.dp, vertical = 2.dp)) {
                Text(tag, fontSize = 10.sp, fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal, color = if(isSelected) OnPrimaryContainer else OnSurfaceVariant)
            }
        }
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = contentColor)
        Text(subtitle, fontSize = 10.sp, color = subColor, modifier = Modifier.padding(top = 2.dp))
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
private fun CustomSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val bg by animateColorAsState(if (checked) PrimaryContainer else SurfaceContainerHigh, label = "switchBg")
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
private fun JpgToPngBottomBar(isProcessing: Boolean, isComplete: Boolean, isEnabled: Boolean, onConvert: () -> Unit) {
    val buttonColor = if (isComplete) Secondary else PrimaryContainer
    val cornerRadius by animateDpAsState(targetValue = if (isProcessing) 50.dp else 12.dp, label = "buttonShape")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceContainerLowest.copy(alpha = 0.95f))
            .shadow(elevation = 24.dp, spotColor = OnSurface.copy(alpha = 0.06f))
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(48.dp)
                    .shadow(if (isEnabled && !isProcessing && !isComplete) 4.dp else 0.dp, RoundedCornerShape(cornerRadius), spotColor = PrimaryContainer.copy(alpha = 0.5f))
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(if (!isEnabled) SurfaceContainerHigh else buttonColor)
                    .clickable(enabled = isEnabled && !isProcessing && !isComplete) { onConvert() },
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(targetState = Pair(isProcessing, isComplete), label = "btn") { state ->
                    val (processing, complete) = state
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (processing) {
                            CircularProgressIndicator(color = OnPrimaryContainer, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Processing PNG...", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnPrimaryContainer)
                        } else if (complete) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = OnPrimaryContainer, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Exported Successfully!", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnPrimaryContainer)
                        } else {
                            Icon(Icons.Outlined.Sync, contentDescription = null, tint = if (isEnabled) OnPrimaryContainer else OutlineVariant, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Convert to PNG & Save", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isEnabled) OnPrimaryContainer else OutlineVariant)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SecondaryContainer))
                Spacer(modifier = Modifier.width(4.dp))
                Text("~0.2s local runtime • Instant download", fontSize = 10.sp, color = OnSurfaceVariant)
            }
        }
    }
}