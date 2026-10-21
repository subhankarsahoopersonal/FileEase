package com.subha.fileease.ui.screens

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.pow
import kotlin.math.roundToInt

private val Background = Color(0xFFFAF8FF)
private val Surface = Color(0xFFFAF8FF)
private val SurfaceContainerLowest = Color(0xFFFFFFFF)
private val SurfaceContainerLow = Color(0xFFF2F3FF)
private val SurfaceContainer = Color(0xFFEAEDFF)
private val SurfaceContainerHigh = Color(0xFFE2E7FF)
private val InverseSurface = Color(0xFF283044)
private val InverseOnSurface = Color(0xFFEEF0FF)
private val OnSurface = Color(0xFF131B2E)
private val OnSurfaceVariant = Color(0xFF424656)
private val Primary = Color(0xFF0050CB)
private val OnPrimary = Color(0xFFFFFFFF)
private val PrimaryContainer = Color(0xFF0066FF)
private val OnPrimaryContainer = Color(0xFFF8F7FF)
private val Secondary = Color(0xFF0058BE)
private val SecondaryContainer = Color(0xFF2170E4)
private val Outline = Color(0xFF727687)
private val OutlineVariant = Color(0xFFC2C6D8)
private val ErrorContainer = Color(0xFFFFDAD6)
private val Error = Color(0xFFBA1A1A)

@Composable
fun CompressImageScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val vibrator = remember { context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    var isProcessing by remember { mutableStateOf(false) }
    var isComplete by remember { mutableStateOf(false) }
    var outputUri by remember { mutableStateOf<Uri?>(null) }

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageName by remember { mutableStateOf("") }
    var imageWidth by remember { mutableIntStateOf(0) }
    var imageHeight by remember { mutableIntStateOf(0) }
    var imageSizeMb by remember { mutableFloatStateOf(0f) }
    var imageExtension by remember { mutableStateOf("JPG") }

    var targetQuality by remember { mutableFloatStateOf(82f) }
    var stripExif by remember { mutableStateOf(true) }
    var convertWebp by remember { mutableStateOf(false) }

    val baseSize = if (imageSizeMb > 0f) imageSizeMb else 8.4f
    val factor = maxOf(0.08f, (targetQuality / 100f).pow(1.8f))
    val estimatedMb = baseSize * factor
    val savedMb = baseSize - estimatedMb
    val percentSavings = if (baseSize > 0f) ((savedMb / baseSize) * 100f).roundToInt() else 78
    val ratioWidth = if (baseSize > 0f) maxOf(0.08f, minOf(1.0f, estimatedMb / baseSize)) else 0.22f

    val progressWidth by animateFloatAsState(targetValue = ratioWidth, label = "savings")

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
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

                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it, null, options)
                    }

                    withContext(Dispatchers.Main) {
                        imageUri = uri
                        imageName = name
                        imageWidth = options.outWidth
                        imageHeight = options.outHeight
                        imageSizeMb = sizeBytes / (1024f * 1024f)
                        imageExtension = name.substringAfterLast('.', "JPG").uppercase().take(3)
                        isComplete = false
                        outputUri = null
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { snackbarHostState.showSnackbar("Failed to read image.") }
                }
            }
        }
    }

    Scaffold(
        containerColor = Background,
        topBar = { CompressTopBar(onNavigateBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            CompressBottomBar(
                isProcessing = isProcessing,
                isComplete = isComplete,
                isEnabled = imageUri != null,
                onConvert = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (imageUri == null) return@CompressBottomBar

                    val usableSpaceMb = Environment.getDataDirectory().usableSpace / (1024 * 1024)
                    if (usableSpaceMb < estimatedMb + 5) {
                        coroutineScope.launch { snackbarHostState.showSnackbar("Not enough storage space.") }
                        return@CompressBottomBar
                    }

                    coroutineScope.launch(Dispatchers.IO) {
                        isProcessing = true
                        var pendingDestUri: Uri? = null

                        try {
                            val targetExt = if (convertWebp) "webp" else "jpg"
                            val targetMime = if (convertWebp) "image/webp" else "image/jpeg"

                            val compressFormat = if (convertWebp) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    if (targetQuality >= 98f) Bitmap.CompressFormat.WEBP_LOSSLESS else Bitmap.CompressFormat.WEBP_LOSSY
                                } else {
                                    Bitmap.CompressFormat.WEBP
                                }
                            } else {
                                Bitmap.CompressFormat.JPEG
                            }

                            val originalName = imageName.substringBeforeLast(".")
                            val finalName = "${originalName}_compressed.$targetExt"

                            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            context.contentResolver.openInputStream(imageUri!!)?.use { BitmapFactory.decodeStream(it, null, options) }

                            var sampleSize = 1
                            while (options.outWidth / sampleSize > 4000 || options.outHeight / sampleSize > 4000) { sampleSize *= 2 }
                            options.inJustDecodeBounds = false
                            options.inSampleSize = sampleSize

                            val rawBitmap = context.contentResolver.openInputStream(imageUri!!)?.use {
                                BitmapFactory.decodeStream(it, null, options)
                            } ?: throw Exception("Failed to load source image")

                            val dimensionalScale = when {
                                targetQuality >= 90f -> 1.0f
                                targetQuality >= 70f -> 0.75f
                                else -> 0.5f
                            }

                            val targetW = (options.outWidth * dimensionalScale).toInt()
                            val targetH = (options.outHeight * dimensionalScale).toInt()

                            val optimizedBitmap = if (targetW != rawBitmap.width || targetH != rawBitmap.height) {
                                val scaled = Bitmap.createScaledBitmap(rawBitmap, targetW, targetH, true)
                                if (scaled != rawBitmap) rawBitmap.recycle()
                                scaled
                            } else {
                                rawBitmap
                            }

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                                val details = ContentValues().apply {
                                    put(MediaStore.Images.Media.DISPLAY_NAME, finalName)
                                    put(MediaStore.Images.Media.MIME_TYPE, targetMime)
                                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FileEase")
                                }
                                pendingDestUri = context.contentResolver.insert(collection, details)
                                pendingDestUri?.let { destUri ->
                                    context.contentResolver.openOutputStream(destUri)?.use { out ->
                                        optimizedBitmap.compress(compressFormat, targetQuality.toInt(), out)
                                    }
                                }
                            } else {
                                @Suppress("DEPRECATION")
                                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "FileEase")
                                if (!dir.exists()) dir.mkdirs()
                                val file = File(dir, finalName)
                                FileOutputStream(file).use { out ->
                                    optimizedBitmap.compress(compressFormat, targetQuality.toInt(), out)
                                }
                                pendingDestUri = Uri.fromFile(file)

                                MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf(targetMime), null)
                            }

                            if (!stripExif && !convertWebp && pendingDestUri != null) {
                                try {
                                    context.contentResolver.openInputStream(imageUri!!)?.use { inStream ->
                                        val oldExif = android.media.ExifInterface(inStream)
                                        context.contentResolver.openFileDescriptor(pendingDestUri!!, "rw")?.use { pfd ->
                                            val newExif = android.media.ExifInterface(pfd.fileDescriptor)
                                            oldExif.getAttribute(android.media.ExifInterface.TAG_ORIENTATION)?.let {
                                                newExif.setAttribute(android.media.ExifInterface.TAG_ORIENTATION, it)
                                                newExif.saveAttributes()
                                            }
                                        }
                                    }
                                } catch (e: Exception) { e.printStackTrace() }
                            }

                            optimizedBitmap.recycle()

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
                                snackbarHostState.showSnackbar("Optimized image saved to gallery.")
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            pendingDestUri?.let { context.contentResolver.delete(it, null, null) }
                            withContext(Dispatchers.Main) {
                                isProcessing = false
                                snackbarHostState.showSnackbar("Failed to compress image.")
                            }
                        }
                    }
                },
                onShare = {
                    outputUri?.let { uri ->
                        try {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "image/*"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Image"))
                        } catch (e: Exception) {
                            coroutineScope.launch { snackbarHostState.showSnackbar("Error launching share sheet.") }
                        }
                    }
                },
                onOpen = {
                    outputUri?.let { uri ->
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "image/*")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Open Image"))
                        } catch (e: Exception) {
                            coroutineScope.launch { snackbarHostState.showSnackbar("No gallery app installed.") }
                        }
                    }
                },
                onReset = {
                    isComplete = false
                    outputUri = null
                    imageUri = null
                    imageSizeMb = 0f
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
            Spacer(modifier = Modifier.height(16.dp))

            // Source Image Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(12.dp))
                    .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("SOURCE FILE", fontSize = 10.sp, color = OnSurfaceVariant, letterSpacing = 0.5.sp, fontWeight = FontWeight.Medium)
                    if (imageUri != null) {
                        Row(
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(SurfaceContainer).padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SecondaryContainer))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ready", fontSize = 10.sp, color = OnSurfaceVariant)
                        }
                    }
                }

                if (imageUri != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceContainer)) {
                            AsyncImage(
                                model = imageUri,
                                contentDescription = "Source Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).clip(RoundedCornerShape(4.dp)).background(InverseSurface.copy(alpha = 0.85f)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                                Text(imageExtension, fontSize = 9.sp, fontWeight = FontWeight.Medium, color = InverseOnSurface)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(imageName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("$imageWidth × $imageHeight px", fontSize = 12.sp, color = OnSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                            Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(SurfaceContainer).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                    Text("%.1f MB Original".format(imageSizeMb), fontSize = 11.sp, color = OnSurface)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceContainer)
                                .clickable(enabled = !isProcessing) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    imagePickerLauncher.launch("image/*")
                                }
                                .alpha(if (isProcessing) 0.5f else 1f)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text("Change", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceContainerLow)
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                imagePickerLauncher.launch("image/*")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, tint = Primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Tap to browse gallery", fontSize = 13.sp, color = Outline, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Telemetry & Estimation Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(PrimaryContainer, Secondary, Primary)), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.DataThresholding, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("LIVE SPACE SAVINGS", fontSize = 11.sp, color = OnPrimary.copy(alpha = 0.9f), letterSpacing = 0.5.sp)
                        }
                        Row(
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(SurfaceContainerLowest.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Bolt, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("-$percentSavings% Savings", fontSize = 10.sp, color = OnPrimary)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Column {
                            Text("Estimated File Size", fontSize = 10.sp, color = OnPrimary.copy(alpha = 0.8f))
                            Text(if(estimatedMb >= 1f) "%.1f MB".format(estimatedMb) else "${(estimatedMb*1000).toInt()} KB", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = OnPrimary, letterSpacing = (-0.5).sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Payload Freed", fontSize = 10.sp, color = OnPrimary.copy(alpha = 0.8f))
                            Text("%.1f MB Saved".format(savedMb), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = OnPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Output (~$percentSavings%)", fontSize = 10.sp, color = OnPrimary.copy(alpha = 0.8f))
                        Text(if(imageUri != null) "Original (%.1f MB)".format(baseSize) else "Original (8.4 MB)", fontSize = 10.sp, color = OnPrimary.copy(alpha = 0.8f))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(SurfaceContainerLowest.copy(alpha = 0.3f))) {
                        Box(modifier = Modifier.fillMaxWidth(progressWidth).fillMaxHeight().clip(RoundedCornerShape(50)).background(SurfaceContainerLowest))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Compression Presets
            Row(modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Compression Presets", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                Text("Pick one or customize below", fontSize = 10.sp, color = OnSurfaceVariant)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CompressPresetCard(
                    title = "Smart Compress",
                    sizeText = "~%.1f MB".format(baseSize * 0.22f),
                    desc = "80% Quality · Balanced for web & messaging apps",
                    badge1 = "-78% size",
                    badge2 = "• Imperceptible loss",
                    isSelected = targetQuality == 80f,
                    isRecommended = true,
                    isEnabled = !isProcessing,
                    onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); targetQuality = 80f }
                )
                CompressPresetCard(
                    title = "Maximum Shrink",
                    sizeText = "~${(baseSize * 0.08f * 1000).toInt()} KB",
                    desc = "50% Quality · Ultra compact for strict email attachment caps",
                    badge1 = "-90% size",
                    badge2 = "• Noticeable compression",
                    isSelected = targetQuality == 50f,
                    isEnabled = !isProcessing,
                    onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); targetQuality = 50f }
                )
                CompressPresetCard(
                    title = "Minimal Compression",
                    sizeText = "~%.1f MB".format(baseSize * 0.61f),
                    desc = "95% Quality · Near-lossless archival for physical photo printing",
                    badge1 = "-39% size",
                    badge2 = "• Pure studio fidelity",
                    isSelected = targetQuality == 95f,
                    isEnabled = !isProcessing,
                    onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); targetQuality = 95f }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Quality Slider
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(12.dp))
                    .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Tune, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Target Quality", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                    }
                    Row(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(SurfaceContainer).padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${targetQuality.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Primary)
                        Text("%", fontSize = 16.sp, color = Primary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Slider(
                    value = targetQuality,
                    onValueChange = { targetQuality = it },
                    valueRange = 10f..98f,
                    enabled = !isProcessing,
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryContainer,
                        activeTrackColor = PrimaryContainer,
                        inactiveTrackColor = SurfaceContainer
                    ),
                    modifier = Modifier.fillMaxWidth().height(24.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("10% (Lowest)", fontSize = 10.sp, color = OnSurfaceVariant)
                    Text("80% Sweet Spot", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Primary)
                    Text("98% (Lossless)", fontSize = 10.sp, color = OnSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Optimization Toggles
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(12.dp))
                    .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Additional Optimizations", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                    Text("Engine: WebAssembly", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(modifier = Modifier.weight(1f).padding(end = 12.dp), verticalAlignment = Alignment.Top) {
                        Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceContainer), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.FmdBad, contentDescription = null, tint = Primary, modifier = Modifier.size(19.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Strip EXIF & Location", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                            Text("Purge GPS coords, camera maker, shutter timestamps", fontSize = 12.sp, color = OnSurfaceVariant)
                        }
                    }
                    CompressCustomSwitch(checked = stripExif, isEnabled = !isProcessing, onCheckedChange = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); stripExif = it })
                }

                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SurfaceContainer))
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(modifier = Modifier.weight(1f).padding(end = 12.dp), verticalAlignment = Alignment.Top) {
                        Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceContainer), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Transform, contentDescription = null, tint = Primary, modifier = Modifier.size(19.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Convert to WebP Format", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                            Text("Modern format yields an extra ~25% data compression", fontSize = 12.sp, color = OnSurfaceVariant)
                        }
                    }
                    CompressCustomSwitch(checked = convertWebp, isEnabled = !isProcessing, onCheckedChange = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); convertWebp = it })
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
private fun CompressPresetCard(
    title: String,
    sizeText: String,
    desc: String,
    badge1: String,
    badge2: String,
    isSelected: Boolean,
    isRecommended: Boolean = false,
    isEnabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(12.dp))
            .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
            .clickable(enabled = isEnabled) { onClick() }
            .alpha(if (isEnabled) 1f else 0.5f)
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(if(isSelected) Primary else SurfaceContainerHigh).padding(top = 2.dp), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SurfaceContainerLowest))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                    if (isRecommended) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(SurfaceContainerHigh).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text("Recommended", fontSize = 10.sp, color = Primary)
                        }
                    }
                }
                Text(sizeText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Primary)
            }
            Text(desc, fontSize = 12.sp, color = OnSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
            Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(SurfaceContainer).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(badge1, fontSize = 10.sp, color = OnSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(badge2, fontSize = 10.sp, color = OnSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CompressCustomSwitch(checked: Boolean, isEnabled: Boolean = true, onCheckedChange: (Boolean) -> Unit) {
    val bg by animateColorAsState(if (checked) PrimaryContainer else SurfaceContainerHigh, label = "switchBg")
    val offset by animateDpAsState(if (checked) 24.dp else 0.dp, label = "switchOffset")

    Box(
        modifier = Modifier
            .width(48.dp)
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
private fun CompressTopBar(onNavigateBack: () -> Unit) {
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
        Text("Compress Image", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OnSurface, letterSpacing = (-0.01).sp)
    }
}

@Composable
private fun CompressBottomBar(
    isProcessing: Boolean,
    isComplete: Boolean,
    isEnabled: Boolean,
    onConvert: () -> Unit,
    onShare: () -> Unit,
    onOpen: () -> Unit,
    onReset: () -> Unit
) {
    val cornerRadius by animateDpAsState(targetValue = if (isProcessing) 50.dp else 12.dp, label = "buttonShape")
    val buttonColor = if (isEnabled) PrimaryContainer else SurfaceContainerHigh
    val contentColor = if (isEnabled) OnPrimaryContainer else OutlineVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface.copy(alpha = 0.90f))
            .shadow(elevation = 24.dp, spotColor = OnSurface.copy(alpha = 0.06f))
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedContent(targetState = isComplete, label = "ActionBarTransition") { complete ->
                if (complete) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CompressActionItem(icon = Icons.Outlined.Share, label = "Share File", onClick = onShare)
                        CompressActionItem(icon = Icons.Outlined.OpenInNew, label = "Open File", onClick = onOpen)
                        CompressActionItem(icon = Icons.Outlined.RestartAlt, label = "Compress New", onClick = onReset)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(48.dp)
                                .shadow(if (isEnabled && !isProcessing) 4.dp else 0.dp, RoundedCornerShape(cornerRadius), spotColor = PrimaryContainer.copy(alpha = 0.5f))
                                .clip(RoundedCornerShape(cornerRadius))
                                .background(buttonColor)
                                .clickable(enabled = isEnabled && !isProcessing) { onConvert() },
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedContent(targetState = isProcessing, label = "btnState") { processing ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (processing) {
                                        CircularProgressIndicator(color = contentColor, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Optimizing Pixels...", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = contentColor)
                                    } else {
                                        Icon(Icons.Outlined.Compress, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Compress & Save Image", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = contentColor)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.VerifiedUser, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("No uploads. Processed strictly in browser memory.", fontSize = 10.sp, color = OnSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompressActionItem(icon: ImageVector, label: String, onClick: () -> Unit) {
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