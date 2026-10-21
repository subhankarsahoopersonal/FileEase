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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
private val OutlineVariant = Color(0xFFC2C6D8) // FIXED: Re-added missing token

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
    var showCropOverlay by remember { mutableStateOf(false) }

    // Image Data
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageName by remember { mutableStateOf("") }
    var imageWidth by remember { mutableIntStateOf(0) }
    var imageHeight by remember { mutableIntStateOf(0) }
    var imageSizeMb by remember { mutableFloatStateOf(0f) }
    var imageExtension by remember { mutableStateOf("JPG") }

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
                    e.printStackTrace()
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
                    onConvert = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (imageUri == null) return@CropImageBottomBar

                        coroutineScope.launch(Dispatchers.IO) {
                            isProcessing = true
                            var pendingDestUri: Uri? = null

                            try {
                                val originalName = imageName.substringBeforeLast(".")
                                val finalName = "${originalName}_saved.jpg"

                                val options = BitmapFactory.Options()
                                var sampleSize = 1
                                while (imageWidth / sampleSize > 4000 || imageHeight / sampleSize > 4000) { sampleSize *= 2 }
                                options.inSampleSize = sampleSize

                                val bitmap = context.contentResolver.openInputStream(imageUri!!)?.use {
                                    BitmapFactory.decodeStream(it, null, options)
                                } ?: throw Exception("Failed to load source image")

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
                                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                                        }
                                    }
                                } else {
                                    @Suppress("DEPRECATION")
                                    val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "FileEase")
                                    if (!dir.exists()) dir.mkdirs()
                                    val file = File(dir, finalName)
                                    FileOutputStream(file).use { out ->
                                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                                    }
                                    pendingDestUri = Uri.fromFile(file)
                                }

                                bitmap.recycle()

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
                                    snackbarHostState.showSnackbar("Failed to save image.")
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

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(PrimaryContainer)
                                        .clickable(enabled = !isProcessing) {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            showCropOverlay = true
                                        }
                                        .alpha(if (isProcessing) 0.5f else 1f)
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Crop", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OnPrimaryContainer)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(SurfaceContainer)
                                        .clickable(enabled = !isProcessing) {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            imagePickerLauncher.launch("image/*")
                                        }
                                        .alpha(if (isProcessing) 0.5f else 1f)
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Change", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Primary)
                                }
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

                Spacer(modifier = Modifier.height(120.dp))
            }
        }

        // Interactive Crop Overlay
        AnimatedVisibility(
            visible = showCropOverlay,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (imageUri != null) {
                CropEditorOverlay(
                    imageUri = imageUri!!,
                    onDismiss = { showCropOverlay = false },
                    onCropApplied = { newUri, newSizeMb, newW, newH ->
                        imageUri = newUri
                        imageSizeMb = newSizeMb
                        imageWidth = newW
                        imageHeight = newH
                        showCropOverlay = false
                        imageName = "Cropped_${imageName}"
                    }
                )
            }
        }
    }
}

@Composable
fun CropEditorOverlay(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onCropApplied: (Uri, Float, Int, Int) -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isCropping by remember { mutableStateOf(false) }

    var targetAspectRatio by remember { mutableFloatStateOf(1f) }
    var rotationDegrees by remember { mutableFloatStateOf(0f) }
    var viewportWidthPx by remember { mutableFloatStateOf(1f) }
    var viewportHeightPx by remember { mutableFloatStateOf(1f) }

    fun resetViewport() {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    fun applyCropMatrix() {
        isCropping = true
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val outputBase = 1080f
                val outW = if (targetAspectRatio >= 1f) outputBase else outputBase * targetAspectRatio
                val outH = if (targetAspectRatio <= 1f) outputBase else outputBase / targetAspectRatio

                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(imageUri)?.use { BitmapFactory.decodeStream(it, null, options) }

                var sampleSize = 1
                while (options.outWidth / sampleSize > 3000 || options.outHeight / sampleSize > 3000) { sampleSize *= 2 }
                options.inJustDecodeBounds = false
                options.inSampleSize = sampleSize

                val original = context.contentResolver.openInputStream(imageUri)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                } ?: throw Exception("Failed to load bitmap")

                var activeBitmap = original
                if (rotationDegrees % 360f != 0f) {
                    val rotMatrix = Matrix().apply { postRotate(rotationDegrees) }
                    activeBitmap = Bitmap.createBitmap(original, 0, 0, original.width, original.height, rotMatrix, true)
                    if (activeBitmap != original) original.recycle()
                }

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

                val cacheFile = File(context.cacheDir, "crop_${System.currentTimeMillis()}.jpg")
                FileOutputStream(cacheFile).use { out ->
                    resultBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                }

                activeBitmap.recycle()
                resultBitmap.recycle()

                val newUri = Uri.fromFile(cacheFile)
                val newSizeMb = cacheFile.length() / (1024f * 1024f)

                withContext(Dispatchers.Main) {
                    onCropApplied(newUri, newSizeMb, outW.toInt(), outH.toInt())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { isCropping = false }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black).pointerInput(Unit) { }
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp).statusBarsPadding(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, contentDescription = "Cancel", tint = Color.White) }
            Text("Crop Photo", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { applyCropMatrix() }, enabled = !isCropping) {
                Icon(Icons.Outlined.Check, contentDescription = "Save", tint = PrimaryContainer)
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(targetAspectRatio)
                    .clip(RoundedCornerShape(4.dp))
                    .pointerInput(targetAspectRatio, rotationDegrees) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            if (isCropping) return@detectTransformGestures
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

                Box(modifier = Modifier.fillMaxSize().border(1.dp, Color.White.copy(alpha=0.5f), RoundedCornerShape(4.dp))) {
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
        }

        Column(modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (isCropping) {
                CircularProgressIndicator(color = PrimaryContainer)
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RatioChip("1:1", targetAspectRatio == 1f, !isCropping) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); targetAspectRatio = 1f; resetViewport() }
                        RatioChip("4:3", targetAspectRatio == 4f/3f, !isCropping) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); targetAspectRatio = 4f/3f; resetViewport() }
                        RatioChip("16:9", targetAspectRatio == 16f/9f, !isCropping) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); targetAspectRatio = 16f/9f; resetViewport() }
                        RatioChip("3:4", targetAspectRatio == 3f/4f, !isCropping) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); targetAspectRatio = 3f/4f; resetViewport() }
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha=0.15f))
                            .clickable(enabled = !isCropping) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                rotationDegrees += 90f
                                resetViewport()
                            }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.RotateRight, contentDescription = "Rotate", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Pinch to zoom, drag to pan.", color = Color.White.copy(alpha=0.6f), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun RatioChip(label: String, isSelected: Boolean, isEnabled: Boolean, onClick: () -> Unit) {
    val bg = if (isSelected) PrimaryContainer else Color.White.copy(alpha=0.15f)
    val textCol = if (isSelected) OnPrimaryContainer else Color.White
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
        Text("Crop Image", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OnSurface, letterSpacing = (-0.01).sp)
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
                                        Text("Saving Output...", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = contentColor)
                                    } else {
                                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Save Image", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = contentColor)
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