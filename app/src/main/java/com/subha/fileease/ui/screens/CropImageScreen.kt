package com.subha.fileease.ui.screens

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
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
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.roundToInt

// Exact Tailwind Color Tokens
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
private val SecondaryContainer = Color(0xFF2170E4)
private val Outline = Color(0xFF727687)
private val OutlineVariant = Color(0xFFC2C6D8)
private val ErrorContainer = Color(0xFFFFDAD6)
private val Error = Color(0xFFBA1A1A)

@Composable
fun CropImageScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val vibrator = remember { context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    // State
    var isProcessing by remember { mutableStateOf(false) }
    var isComplete by remember { mutableStateOf(false) }
    var outputUri by remember { mutableStateOf<Uri?>(null) }

    // Image Data
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageName by remember { mutableStateOf("") }
    var imageWidth by remember { mutableIntStateOf(0) }
    var imageHeight by remember { mutableIntStateOf(0) }
    var imageSizeMb by remember { mutableFloatStateOf(0f) }
    var imageExtension by remember { mutableStateOf("JPG") }

    // Phase 5: Viewport Math & Rotation State
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var targetAspectRatio by remember { mutableFloatStateOf(1f) }
    var rotationDegrees by remember { mutableFloatStateOf(0f) }
    var viewportWidthPx by remember { mutableFloatStateOf(1f) }
    var viewportHeightPx by remember { mutableFloatStateOf(1f) }

    fun resetViewport() {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

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

                        resetViewport()
                        rotationDegrees = 0f
                        targetAspectRatio = 1f
                        isComplete = false
                        outputUri = null
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) { snackbarHostState.showSnackbar("Failed to read image.") }
                }
            }
        }
    }

    Scaffold(
        containerColor = Background,
        topBar = { CropImageTopBar(onNavigateBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            CropImageBottomBar(
                isProcessing = isProcessing,
                isComplete = isComplete,
                isEnabled = imageUri != null,
                onConvert = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (imageUri == null) return@CropImageBottomBar

                    coroutineScope.launch(Dispatchers.IO) {
                        isProcessing = true
                        var pendingDestUri: Uri? = null

                        try {
                            // Phase 5: Dynamic Resolution based on Aspect Ratio
                            val outputBase = 1080f
                            val outW = if (targetAspectRatio >= 1f) outputBase else outputBase * targetAspectRatio
                            val outH = if (targetAspectRatio <= 1f) outputBase else outputBase / targetAspectRatio

                            val originalName = imageName.substringBeforeLast(".")
                            val finalName = "${originalName}_cropped.jpg"

                            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            context.contentResolver.openInputStream(imageUri!!)?.use { BitmapFactory.decodeStream(it, null, options) }

                            var sampleSize = 1
                            while (options.outWidth / sampleSize > 3000 || options.outHeight / sampleSize > 3000) { sampleSize *= 2 }
                            options.inJustDecodeBounds = false
                            options.inSampleSize = sampleSize

                            val original = context.contentResolver.openInputStream(imageUri!!)?.use {
                                BitmapFactory.decodeStream(it, null, options)
                            } ?: throw Exception("Failed to load source image")

                            // Phase 5: Safe Pre-Rotation Buffer
                            var activeBitmap = original
                            if (rotationDegrees % 360f != 0f) {
                                val rotMatrix = Matrix().apply { postRotate(rotationDegrees) }
                                activeBitmap = Bitmap.createBitmap(original, 0, 0, original.width, original.height, rotMatrix, true)
                                if (activeBitmap != original) original.recycle()
                            }

                            // Native Canvas Coordinate Mapping Engine
                            val resultBitmap = Bitmap.createBitmap(outW.toInt(), outH.toInt(), Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(resultBitmap)
                            val matrix = Matrix()

                            // Base ContentScale.Crop mapping
                            val scaleToFill = maxOf(outW / activeBitmap.width, outH / activeBitmap.height)
                            matrix.postTranslate((outW - activeBitmap.width) / 2f, (outH - activeBitmap.height) / 2f)
                            matrix.postScale(scaleToFill, scaleToFill, outW / 2f, outH / 2f)

                            // Map user gestures from UI viewport to canvas
                            val transformScaleMultiplierX = outW / viewportWidthPx
                            val transformScaleMultiplierY = outH / viewportHeightPx

                            matrix.postScale(scale, scale, outW / 2f, outH / 2f)
                            matrix.postTranslate(offsetX * transformScaleMultiplierX, offsetY * transformScaleMultiplierY)

                            canvas.drawBitmap(activeBitmap, matrix, Paint(Paint.FILTER_BITMAP_FLAG))

                            // Save to Storage
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                                val details = ContentValues().apply {
                                    put(MediaStore.Images.Media.DISPLAY_NAME, finalName)
                                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FileEase")
                                }
                                pendingDestUri = context.contentResolver.insert(collection, details)
                                pendingDestUri?.let { destUri ->
                                    context.contentResolver.openOutputStream(destUri)?.use { out ->
                                        resultBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                                    }
                                }
                            } else {
                                @Suppress("DEPRECATION")
                                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "FileEase")
                                if (!dir.exists()) dir.mkdirs()
                                val file = File(dir, finalName)
                                FileOutputStream(file).use { out ->
                                    resultBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                                }
                                pendingDestUri = Uri.fromFile(file)
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
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "image/jpeg"
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
                                setDataAndType(uri, "image/jpeg")
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
            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle Callout
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(SurfaceContainerHigh).shadow(1.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Crop, contentDescription = null, tint = Primary, modifier = Modifier.size(17.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Visual Framing", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                    Text("Adjust boundaries, pan, and zoom to cleanly re-frame your photo on-device.", fontSize = 12.sp, color = OnSurfaceVariant)
                }
            }

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
                                    Text("%.1f MB".format(imageSizeMb), fontSize = 11.sp, color = OnSurface)
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

            // Interactive Crop Viewport
            if (imageUri != null) {
                Row(modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Interactive Editor", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(enabled = !isProcessing) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            resetViewport()
                        }.alpha(if (isProcessing) 0.5f else 1f).padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.RestartAlt, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Framing", fontSize = 11.sp, color = Primary, fontWeight = FontWeight.SemiBold)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(1.dp, RoundedCornerShape(12.dp))
                        .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(targetAspectRatio)
                            .clip(RoundedCornerShape(8.dp))
                            .background(InverseSurface)
                            .pointerInput(targetAspectRatio, rotationDegrees) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    if (isProcessing) return@detectTransformGestures
                                    scale = (scale * zoom).coerceIn(1f, 5f)
                                    val maxOffsetX = (viewportWidthPx * scale - viewportWidthPx) / 2f
                                    val maxOffsetY = (viewportHeightPx * scale - viewportHeightPx) / 2f
                                    offsetX = (offsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                                    offsetY = (offsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                }
                            }
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

                        // Pro 3x3 Grid Overlay
                        Box(modifier = Modifier.fillMaxSize().border(1.dp, Color.White.copy(alpha=0.5f), RoundedCornerShape(8.dp))) {
                            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
                                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha=0.3f)))
                                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha=0.3f)))
                            }
                            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                Box(Modifier.fillMaxHeight().width(1.dp).background(Color.White.copy(alpha=0.3f)))
                                Box(Modifier.fillMaxHeight().width(1.dp).background(Color.White.copy(alpha=0.3f)))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Phase 5: Aspect Ratio & Rotation Segmented Controls
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RatioChip("1:1", targetAspectRatio == 1f, !isProcessing) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); targetAspectRatio = 1f; resetViewport() }
                            RatioChip("4:3", targetAspectRatio == 4f/3f, !isProcessing) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); targetAspectRatio = 4f/3f; resetViewport() }
                            RatioChip("16:9", targetAspectRatio == 16f/9f, !isProcessing) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); targetAspectRatio = 16f/9f; resetViewport() }
                            RatioChip("3:4", targetAspectRatio == 3f/4f, !isProcessing) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); targetAspectRatio = 3f/4f; resetViewport() }
                        }

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(SurfaceContainerHigh)
                                .clickable(enabled = !isProcessing) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    rotationDegrees += 90f
                                    resetViewport()
                                }
                                .alpha(if (isProcessing) 0.5f else 1f)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.RotateRight, contentDescription = "Rotate 90", tint = Primary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
private fun RatioChip(label: String, isSelected: Boolean, isEnabled: Boolean, onClick: () -> Unit) {
    val bg = if (isSelected) PrimaryContainer else SurfaceContainerHigh
    val textCol = if (isSelected) OnPrimaryContainer else OnSurfaceVariant
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .clickable(enabled = isEnabled) { onClick() }
            .alpha(if (isEnabled) 1f else 0.5f)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = textCol)
    }
}

@Composable
private fun CropImageTopBar(onNavigateBack: () -> Unit) {
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
                Text("Crop Image", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OnSurface, letterSpacing = (-0.01).sp)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(50)).background(SurfaceContainerHigh.copy(alpha=0.6f)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(PrimaryContainer).alpha(alphaPulse))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Offline Engine Active • 100% On-device", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = OnSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun CropImageBottomBar(
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
                        PostActionItem(icon = Icons.Outlined.Share, label = "Share File", onClick = onShare)
                        PostActionItem(icon = Icons.Outlined.OpenInNew, label = "Open File", onClick = onOpen)
                        PostActionItem(icon = Icons.Outlined.RestartAlt, label = "Crop New", onClick = onReset)
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
                                        Text("Extracting Matrix...", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = contentColor)
                                    } else {
                                        Icon(Icons.Outlined.Crop, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Crop & Save Image", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = contentColor)
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