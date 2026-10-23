package com.subha.fileease.ui.screens

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

// Exact Tailwind Color Tokens
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
private val PrimaryContainer = Color(0xFF0066FF)
private val OnPrimaryContainer = Color(0xFFF8F7FF)
private val Outline = Color(0xFF727687)
private val OutlineVariant = Color(0xFFC2C6D8)

@Composable
fun CropImageScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val vibrator = remember { context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Execution State
    var isProcessing by remember { mutableStateOf(false) }
    var isComplete by remember { mutableStateOf(false) }
    var outputUri by remember { mutableStateOf<Uri?>(null) }

    // Source Data
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageName by remember { mutableStateOf("") }
    var imageWidth by remember { mutableIntStateOf(0) }
    var imageHeight by remember { mutableIntStateOf(0) }

    // Editor Viewport Matrix State
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var rotationDegrees by remember { mutableFloatStateOf(0f) }
    var viewportWidthPx by remember { mutableFloatStateOf(1f) }
    var viewportHeightPx by remember { mutableFloatStateOf(1f) }

    // Tool Configurations
    var selectedAspectRatio by remember { mutableStateOf("1:1 Square") }
    var targetWidth by remember { mutableStateOf("1080") }
    var targetHeight by remember { mutableStateOf("1080") }
    var isRatioLocked by remember { mutableStateOf(true) }
    var lockedRatio by remember { mutableFloatStateOf(1f) }
    var selectedEncoding by remember { mutableStateOf("Original (JPG)") }

    // Dynamically calculate the active aspect ratio for the viewport
    val currentRatio = remember(targetWidth, targetHeight) {
        val w = targetWidth.toFloatOrNull() ?: 1080f
        val h = targetHeight.toFloatOrNull() ?: 1080f
        (if (h > 0) w / h else 1f).coerceIn(0.2f, 5f)
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    var name = "Image.jpg"
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (cursor.moveToFirst()) name = cursor.getString(nameIdx) ?: "Image.jpg"
                    }

                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }

                    withContext(Dispatchers.Main) {
                        imageUri = uri
                        imageName = name
                        imageWidth = options.outWidth
                        imageHeight = options.outHeight

                        scale = 1f; offsetX = 0f; offsetY = 0f; rotationDegrees = 0f
                        isComplete = false
                        outputUri = null
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { snackbarHostState.showSnackbar("Failed to read image.") }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Background,
            topBar = { CropImageTopBar(onNavigateBack) },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                CropImageBottomBar(
                    isProcessing = isProcessing,
                    isComplete = isComplete,
                    isEnabled = imageUri != null,
                    originalDimensions = if(imageWidth > 0) "${imageWidth} x $imageHeight" else "2400 x 1800",
                    croppedDimensions = "${targetWidth.takeIf { it.isNotEmpty() } ?: "0"} x ${targetHeight.takeIf { it.isNotEmpty() } ?: "0"} px",
                    onConvert = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (imageUri == null) return@CropImageBottomBar

                        coroutineScope.launch(Dispatchers.IO) {
                            isProcessing = true
                            var pendingDestUri: Uri? = null

                            try {
                                val isPng = selectedEncoding == "Export as PNG"
                                val targetExt = if (isPng) "png" else "jpg"
                                val targetMime = if (isPng) "image/png" else "image/jpeg"
                                val compressFormat = if (isPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG

                                val originalName = imageName.substringBeforeLast(".")
                                val finalName = "${originalName}_cropped.$targetExt"

                                // Safely decode original into memory
                                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                context.contentResolver.openInputStream(imageUri!!)?.use { BitmapFactory.decodeStream(it, null, options) }

                                var sampleSize = 1
                                while (options.outWidth / sampleSize > 4000 || options.outHeight / sampleSize > 4000) { sampleSize *= 2 }
                                options.inJustDecodeBounds = false
                                options.inSampleSize = sampleSize

                                val original = context.contentResolver.openInputStream(imageUri!!)?.use {
                                    BitmapFactory.decodeStream(it, null, options)
                                } ?: throw Exception("Failed to load source image")

                                // Rotation Buffer
                                var activeBitmap = original
                                if (rotationDegrees % 360f != 0f) {
                                    val rotMatrix = Matrix().apply { postRotate(rotationDegrees) }
                                    activeBitmap = Bitmap.createBitmap(original, 0, 0, original.width, original.height, rotMatrix, true)
                                    if (activeBitmap != original) original.recycle()
                                }

                                // Native Canvas Coordinate Mapping Engine
                                val outW = targetWidth.toFloatOrNull() ?: 1080f
                                val outH = targetHeight.toFloatOrNull() ?: 1080f

                                val resultBitmap = Bitmap.createBitmap(outW.toInt(), outH.toInt(), Bitmap.Config.ARGB_8888)
                                val canvas = Canvas(resultBitmap)
                                val matrix = Matrix()

                                val scaleToFill = maxOf(outW / activeBitmap.width, outH / activeBitmap.height)
                                matrix.postTranslate((outW - activeBitmap.width) / 2f, (outH - activeBitmap.height) / 2f)
                                matrix.postScale(scaleToFill, scaleToFill, outW / 2f, outH / 2f)

                                val transformScaleMultiplierX = outW / viewportWidthPx
                                val transformScaleMultiplierY = outH / viewportHeightPx

                                matrix.postScale(scale, scale, outW / 2f, outH / 2f)
                                matrix.postTranslate(offsetX * transformScaleMultiplierX, offsetY * transformScaleMultiplierY)

                                canvas.drawBitmap(activeBitmap, matrix, Paint(Paint.FILTER_BITMAP_FLAG))

                                // Write to MediaStore
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
                                            resultBitmap.compress(compressFormat, 100, out)
                                        }
                                    }
                                } else {
                                    @Suppress("DEPRECATION")
                                    val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "FileEase")
                                    if (!dir.exists()) dir.mkdirs()
                                    val file = File(dir, finalName)
                                    FileOutputStream(file).use { out ->
                                        resultBitmap.compress(compressFormat, 100, out)
                                    }
                                    pendingDestUri = Uri.fromFile(file)
                                    MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf(targetMime), null)
                                }

                                activeBitmap.recycle()
                                resultBitmap.recycle()

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
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                pendingDestUri?.let { context.contentResolver.delete(it, null, null) }
                                withContext(Dispatchers.Main) {
                                    isProcessing = false
                                    snackbarHostState.showSnackbar("Failed to crop image.")
                                }
                            }
                        }
                    },
                    onShare = {
                        outputUri?.let { uri ->
                            try {
                                val isPng = selectedEncoding == "Export as PNG"
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = if (isPng) "image/png" else "image/jpeg"
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
                                val isPng = selectedEncoding == "Export as PNG"
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, if (isPng) "image/png" else "image/jpeg")
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

                if (imageUri != null) {
                    // Top Tags
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TagChip(Icons.Outlined.Tune, "Lossless Preview", PrimaryContainer, Primary)
                        TagChip(Icons.Outlined.Memory, "GPU Accelerated", SurfaceContainer, OnSurfaceVariant)
                    }

                    // Inline Cropper Viewport
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(1.dp, RoundedCornerShape(12.dp))
                            .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        // Viewport Layout
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(currentRatio)
                                .clip(RoundedCornerShape(8.dp))
                                .background(InverseSurface)
                                .pointerInput(currentRatio, rotationDegrees) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        if (isProcessing) return@detectTransformGestures
                                        scale = (scale * zoom).coerceIn(1f, 5f)
                                        val maxOffsetX = (viewportWidthPx * scale - viewportWidthPx) / 2f
                                        val maxOffsetY = (viewportHeightPx * scale - viewportHeightPx) / 2f
                                        offsetX = (offsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                                        offsetY = (offsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            viewportWidthPx = constraints.maxWidth.toFloat()
                            viewportHeightPx = constraints.maxHeight.toFloat()

                            AsyncImage(
                                model = imageUri,
                                contentDescription = "Viewport",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        rotationZ = rotationDegrees,
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offsetX,
                                        translationY = offsetY
                                    )
                            )

                            // Interactive Overlay UI
                            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                Box(modifier = Modifier.fillMaxSize().border(1.dp, Color.White.copy(alpha=0.6f))) {
                                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
                                        Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha=0.4f)))
                                        Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha=0.4f)))
                                    }
                                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                        Box(Modifier.fillMaxHeight().width(1.dp).background(Color.White.copy(alpha=0.4f)))
                                        Box(Modifier.fillMaxHeight().width(1.dp).background(Color.White.copy(alpha=0.4f)))
                                    }
                                }

                                Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp).clip(RoundedCornerShape(50)).background(InverseSurface.copy(alpha=0.7f)).padding(horizontal = 12.dp, vertical = 4.dp)) {
                                    Text("${targetWidth.takeIf { it.isNotEmpty() } ?: "0"} x ${targetHeight.takeIf { it.isNotEmpty() } ?: "0"} px", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                }

                                CornerMarker(Modifier.align(Alignment.TopStart))
                                CornerMarker(Modifier.align(Alignment.TopEnd))
                                CornerMarker(Modifier.align(Alignment.BottomStart))
                                CornerMarker(Modifier.align(Alignment.BottomEnd))

                                Box(modifier = Modifier.align(Alignment.BottomEnd).offset(x = 12.dp, y = 12.dp).size(36.dp).shadow(2.dp, CircleShape).background(Color.White, CircleShape).clickable(enabled = !isProcessing) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedAspectRatio = "Freeform"
                                    isRatioLocked = false
                                }, contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.GridOn, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Tools Row
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ToolButton(Modifier.weight(1f), Icons.Outlined.RestartAlt, "Reset", !isProcessing) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); scale=1f; offsetX=0f; offsetY=0f; rotationDegrees=0f }
                            ToolButton(Modifier.weight(1f), Icons.Outlined.FilterCenterFocus, "Center", !isProcessing) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); offsetX=0f; offsetY=0f }
                            ToolButton(Modifier.weight(1f), Icons.Outlined.RotateRight, "Rotate 90°", !isProcessing) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); rotationDegrees += 90f }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Aspect Ratio Area
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Aspect Ratio", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                        Text("Custom Grid", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Primary, modifier = Modifier.clickable(enabled = !isProcessing) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedAspectRatio = "Freeform"
                            isRatioLocked = false
                        })
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RatioChip(modifier = Modifier.weight(1f), label = "Freeform", icon = Icons.Outlined.CropFree, isSelected = selectedAspectRatio == "Freeform", !isProcessing) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedAspectRatio = "Freeform"
                            isRatioLocked = false
                        }
                        RatioChip(modifier = Modifier.weight(1f), label = "1:1 Square", icon = Icons.Outlined.CropSquare, isSelected = selectedAspectRatio == "1:1 Square", !isProcessing) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedAspectRatio = "1:1 Square"
                            targetWidth = "1080"
                            targetHeight = "1080"
                            lockedRatio = 1f
                            isRatioLocked = true
                        }
                        RatioChip(modifier = Modifier.weight(1f), label = "4:3 Standard", icon = Icons.Outlined.CropLandscape, isSelected = selectedAspectRatio == "4:3 Standard", !isProcessing) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedAspectRatio = "4:3 Standard"
                            targetWidth = "1440"
                            targetHeight = "1080"
                            lockedRatio = 4f / 3f
                            isRatioLocked = true
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Target Canvas Pixels
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(1.dp, RoundedCornerShape(16.dp))
                            .background(SurfaceContainerLowest, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Straighten, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Target Canvas Pixels", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                            }
                            Text("Unit: PX", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Width", fontSize = 11.sp, color = OnSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
                                DimensionInput(
                                    value = targetWidth,
                                    isEnabled = !isProcessing,
                                    onValueChange = { newText ->
                                        val digits = newText.filter { it.isDigit() }
                                        if (digits.length <= 4) {
                                            targetWidth = digits
                                            if (isRatioLocked && digits.isNotEmpty()) {
                                                val w = digits.toFloatOrNull() ?: 0f
                                                targetHeight = (w / lockedRatio).roundToInt().toString()
                                            } else if (!isRatioLocked) {
                                                selectedAspectRatio = "Freeform"
                                            }
                                        }
                                    }
                                )
                            }

                            Box(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp).size(36.dp).clip(CircleShape).background(SurfaceContainer).clickable(enabled = !isProcessing) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (!isRatioLocked) {
                                    val w = targetWidth.toFloatOrNull() ?: 1f
                                    val h = targetHeight.toFloatOrNull() ?: 1f
                                    lockedRatio = if (h > 0) w / h else 1f
                                }
                                isRatioLocked = !isRatioLocked
                            }, contentAlignment = Alignment.Center) {
                                Icon(if (isRatioLocked) Icons.Outlined.Lock else Icons.Outlined.LockOpen, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text("Height", fontSize = 11.sp, color = OnSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
                                DimensionInput(
                                    value = targetHeight,
                                    isEnabled = !isProcessing,
                                    onValueChange = { newText ->
                                        val digits = newText.filter { it.isDigit() }
                                        if (digits.length <= 4) {
                                            targetHeight = digits
                                            if (isRatioLocked && digits.isNotEmpty()) {
                                                val h = digits.toFloatOrNull() ?: 0f
                                                targetWidth = (h * lockedRatio).roundToInt().toString()
                                            } else if (!isRatioLocked) {
                                                selectedAspectRatio = "Freeform"
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Output Encoding
                    Text("Output Encoding", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        EncodingCard(
                            modifier = Modifier.weight(1f),
                            title = "Original (JPG)",
                            subtitle = "Small file size",
                            isSelected = selectedEncoding == "Original (JPG)",
                            isEnabled = !isProcessing,
                            onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); selectedEncoding = "Original (JPG)" }
                        )
                        EncodingCard(
                            modifier = Modifier.weight(1f),
                            title = "Export as PNG",
                            subtitle = "High fidelity",
                            isSelected = selectedEncoding == "Export as PNG",
                            isEnabled = !isProcessing,
                            onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); selectedEncoding = "Export as PNG" }
                        )
                    }
                } else {
                    // Empty State Pick Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .shadow(1.dp, RoundedCornerShape(12.dp))
                            .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
                            .clickable { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, tint = Primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Tap to browse gallery", fontSize = 13.sp, color = Outline, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(160.dp))
            }
        }
    }
}

@Composable
private fun TagChip(icon: ImageVector, label: String, bg: Color, iconTint: Color) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Primary)
    }
}

@Composable
private fun CornerMarker(modifier: Modifier) {
    Box(modifier = modifier.size(14.dp).background(Color.White), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size(6.dp).background(PrimaryContainer))
    }
}

@Composable
private fun ToolButton(modifier: Modifier, icon: ImageVector, label: String, isEnabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, SurfaceContainer, RoundedCornerShape(8.dp))
            .clickable(enabled = isEnabled) { onClick() }
            .alpha(if (isEnabled) 1f else 0.5f)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
    }
}

@Composable
private fun RatioChip(modifier: Modifier, label: String, icon: ImageVector?, isSelected: Boolean, isEnabled: Boolean, onClick: () -> Unit) {
    val bg = if (isSelected) Primary else SurfaceContainerLowest
    val textCol = if (isSelected) Color.White else OnSurface
    val borderCol = if (isSelected) Color.Transparent else SurfaceContainer

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.dp, borderCol, RoundedCornerShape(50))
            .clickable(enabled = isEnabled) { onClick() }
            .alpha(if (isEnabled) 1f else 0.5f)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = textCol, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textCol)
    }
}

@Composable
private fun DimensionInput(value: String, isEnabled: Boolean, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceContainerLow)
            .alpha(if (isEnabled) 1f else 0.5f)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = isEnabled,
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface),
            cursorBrush = SolidColor(Primary),
            modifier = Modifier.weight(1f)
        )
        Text("px", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = OnSurfaceVariant)
    }
}

@Composable
private fun EncodingCard(modifier: Modifier, title: String, subtitle: String, isSelected: Boolean, isEnabled: Boolean, onClick: () -> Unit) {
    val bg = if (isSelected) SurfaceContainer else SurfaceContainerLowest
    val borderCol = if (isSelected) Color.Transparent else SurfaceContainer

    Row(
        modifier = modifier
            .shadow(if (isSelected) 0.dp else 1.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, borderCol, RoundedCornerShape(12.dp))
            .clickable(enabled = isEnabled) { onClick() }
            .alpha(if (isEnabled) 1f else 0.5f)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(Color.White).border(if(isSelected) 5.dp else 1.dp, if(isSelected) Primary else OutlineVariant, CircleShape))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = OnSurface)
            Text(subtitle, fontSize = 10.sp, color = OnSurfaceVariant)
        }
    }
}

@Composable
private fun CropImageTopBar(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface.copy(alpha = 0.95f))
            .statusBarsPadding()
            .shadow(elevation = 1.dp, spotColor = Color.Black.copy(alpha = 0.04f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).clickable { onNavigateBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = OnSurface)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Crop Image",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = OnSurface,
            fontFamily = LocalTextStyle.current.fontFamily
        )
    }
}

@Composable
private fun CropImageBottomBar(
    isProcessing: Boolean,
    isComplete: Boolean,
    isEnabled: Boolean,
    originalDimensions: String,
    croppedDimensions: String,
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
            .background(Surface.copy(alpha = 0.98f))
            .shadow(elevation = 24.dp, spotColor = OnSurface.copy(alpha = 0.1f))
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
                        PostActionItem(icon = Icons.Outlined.Share, label = "Share File", onClick = onShare)
                        PostActionItem(icon = Icons.Outlined.OpenInNew, label = "Open File", onClick = onOpen)
                        PostActionItem(icon = Icons.Outlined.RestartAlt, label = "Crop New", onClick = onReset)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Original: $originalDimensions", fontSize = 10.sp, color = OnSurfaceVariant)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.ArrowForward, contentDescription = null, tint = Primary, modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cropped: $croppedDimensions", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Primary)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
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
                                        Text("Applying Matrix...", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = contentColor)
                                    } else {
                                        Icon(Icons.Outlined.Crop, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Apply Crop & Save", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = contentColor)
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