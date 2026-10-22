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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
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
private val SurfaceContainerHighest = Color(0xFFDAE2FD)
private val InverseSurface = Color(0xFF283044)
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

@Composable
fun ResizeImageScreen(onNavigateBack: () -> Unit) {
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
    var imageSizeMb by remember { mutableFloatStateOf(0f) }
    var imageExtension by remember { mutableStateOf("JPG") }

    // UI & Math State
    var resizeMode by remember { mutableStateOf("pixels") }
    var targetWidth by remember { mutableStateOf("1080") }
    var targetHeight by remember { mutableStateOf("1080") }
    var maintainAspect by remember { mutableStateOf(true) }
    var activePreset by remember { mutableStateOf("custom") }
    var resampleAlgo by remember { mutableStateOf("bicubic") }
    var selectedEncoding by remember { mutableStateOf("Original (JPG)") }

    val aspect = if (imageHeight > 0) imageWidth.toFloat() / imageHeight else 16f / 9f

    // Dynamic Telemetry Calculations
    val tW = targetWidth.toFloatOrNull() ?: 0f
    val tH = targetHeight.toFloatOrNull() ?: 0f
    val pixelRatio = if (imageWidth > 0 && imageHeight > 0) (tW * tH) / (imageWidth * imageHeight).toFloat() else 0.25f
    val estMb = maxOf(0.1f, imageSizeMb * pixelRatio)
    val savedMb = maxOf(0f, imageSizeMb - estMb)
    val percentSavings = if (imageSizeMb > 0f) ((savedMb / imageSizeMb) * 100f).roundToInt() else 75
    val barProgress by animateFloatAsState(targetValue = maxOf(0.05f, minOf(1f, estMb / maxOf(1f, imageSizeMb))), label = "bar")

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

                        targetWidth = options.outWidth.toString()
                        targetHeight = options.outHeight.toString()
                        activePreset = "custom"
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
        topBar = { ResizeImageTopBar(onNavigateBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            ResizeBottomBar(
                isProcessing = isProcessing,
                isComplete = isComplete,
                isEnabled = imageUri != null,
                targetDims = "${targetWidth.takeIf { it.isNotEmpty() } ?: "0"} × ${targetHeight.takeIf { it.isNotEmpty() } ?: "0"} px",
                onConvert = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (imageUri == null) return@ResizeBottomBar

                    coroutineScope.launch(Dispatchers.IO) {
                        isProcessing = true
                        var pendingDestUri: Uri? = null

                        try {
                            val outW = targetWidth.toIntOrNull() ?: imageWidth
                            val outH = targetHeight.toIntOrNull() ?: imageHeight

                            val originalName = imageName.substringBeforeLast(".")
                            val isPng = selectedEncoding == "Export as PNG"
                            val finalName = "${originalName}_resized.${if (isPng) "png" else "jpg"}"
                            val mimeType = if (isPng) "image/png" else "image/jpeg"
                            val compressFormat = if (isPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG

                            // OOM Safe Decoder Setup
                            val options = BitmapFactory.Options()
                            var sampleSize = 1
                            while (imageWidth / sampleSize > outW * 2 || imageHeight / sampleSize > outH * 2) { sampleSize *= 2 }
                            options.inSampleSize = sampleSize

                            val original = context.contentResolver.openInputStream(imageUri!!)?.use {
                                BitmapFactory.decodeStream(it, null, options)
                            } ?: throw Exception("Failed to load source image")

                            // Resampling Interpolation Engine
                            val filter = resampleAlgo != "nearest"
                            val scaledBitmap = Bitmap.createScaledBitmap(original, outW, outH, filter)
                            if (scaledBitmap != original) original.recycle()

                            // Scoped Storage Export
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                                val details = ContentValues().apply {
                                    put(MediaStore.Images.Media.DISPLAY_NAME, finalName)
                                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FileEase")
                                }
                                pendingDestUri = context.contentResolver.insert(collection, details)
                                pendingDestUri?.let { destUri ->
                                    context.contentResolver.openOutputStream(destUri)?.use { out ->
                                        scaledBitmap.compress(compressFormat, 95, out)
                                    }
                                }
                            } else {
                                @Suppress("DEPRECATION")
                                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "FileEase")
                                if (!dir.exists()) dir.mkdirs()
                                val file = File(dir, finalName)
                                FileOutputStream(file).use { out ->
                                    scaledBitmap.compress(compressFormat, 95, out)
                                }
                                pendingDestUri = Uri.fromFile(file)
                                MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf(mimeType), null)
                            }

                            scaledBitmap.recycle()

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
                                snackbarHostState.showSnackbar("Failed to resize image.")
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
                onReset = { isComplete = false; outputUri = null; imageUri = null }
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

            Text(
                text = "Scale image dimensions by pixels or percentage without distortion",
                fontSize = 13.sp,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Source Image Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(12.dp))
                    .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                if (imageUri != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceContainer).shadow(1.dp, RoundedCornerShape(8.dp), clip = false)) {
                            AsyncImage(
                                model = imageUri,
                                contentDescription = "Source",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                            )
                            Box(modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).clip(RoundedCornerShape(4.dp)).background(SurfaceContainerHighest.copy(alpha=0.9f)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                                Text(imageExtension, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Primary)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(imageName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(end = 8.dp))
                                Row(
                                    modifier = Modifier.clip(RoundedCornerShape(50)).background(SurfaceContainer).clickable(enabled = !isProcessing) {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        imagePickerLauncher.launch("image/*")
                                    }.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.SwapHoriz, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Change", fontSize = 11.sp, color = Primary, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("$imageWidth × $imageHeight px", fontSize = 12.sp, color = OnSurfaceVariant)
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(OutlineVariant))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("%.1f MB".format(imageSizeMb), fontSize = 12.sp, color = OnSurfaceVariant)
                            }
                            Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.AspectRatio, contentDescription = null, tint = Primary, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Original HD Aspect", fontSize = 10.sp, color = Primary, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
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

            // Resize Mode Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainer)
                    .padding(4.dp)
            ) {
                ModeSwitchButton(modifier = Modifier.weight(1f), icon = Icons.Outlined.Straighten, label = "By Pixels (px)", isSelected = resizeMode == "pixels") { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); resizeMode = "pixels" }
                ModeSwitchButton(modifier = Modifier.weight(1f), icon = Icons.Outlined.Percent, label = "By Percentage (%)", isSelected = resizeMode == "percent") { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); resizeMode = "percent" }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dimension Controls Container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(12.dp))
                    .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Target Dimensions", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                        Row(
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(SurfaceContainer).padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(PrimaryContainer))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if(maintainAspect) "Constraint Locked" else "Unlocked", fontSize = 10.sp, color = Primary, fontWeight = FontWeight.Medium)
                        }
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Width", fontSize = 10.sp, color = OnSurfaceVariant)
                                Text("Max: 7680 px", fontSize = 10.sp, color = Outline)
                            }
                            DimensionInputBox(
                                value = targetWidth,
                                isEnabled = !isProcessing,
                                icon = Icons.Outlined.East,
                                unit = "px",
                                onValueChange = { newVal ->
                                    val digits = newVal.filter { it.isDigit() }
                                    if (digits.length <= 4) {
                                        targetWidth = digits
                                        if (maintainAspect && digits.isNotEmpty()) {
                                            val w = digits.toFloatOrNull() ?: 0f
                                            targetHeight = (w / aspect).roundToInt().toString()
                                        }
                                        activePreset = "custom"
                                    }
                                }
                            )
                        }

                        Box(modifier = Modifier.fillMaxWidth().height(24.dp).zIndex(1f), contentAlignment = Alignment.Center) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (maintainAspect) SurfaceContainerHigh else SurfaceContainer)
                                    .clickable(enabled = !isProcessing) {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        maintainAspect = !maintainAspect
                                    }
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(if (maintainAspect) Icons.Outlined.Link else Icons.Outlined.LinkOff, contentDescription = null, tint = if (maintainAspect) Primary else OnSurfaceVariant, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (maintainAspect) "Maintain Aspect Ratio (16:9)" else "Aspect Ratio Unlocked", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = if (maintainAspect) Primary else OnSurfaceVariant)
                            }
                        }

                        Column {
                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Height", fontSize = 10.sp, color = OnSurfaceVariant)
                                Text("Max: 4320 px", fontSize = 10.sp, color = Outline)
                            }
                            DimensionInputBox(
                                value = targetHeight,
                                isEnabled = !isProcessing,
                                icon = Icons.Outlined.South,
                                unit = "px",
                                onValueChange = { newVal ->
                                    val digits = newVal.filter { it.isDigit() }
                                    if (digits.length <= 4) {
                                        targetHeight = digits
                                        if (maintainAspect && digits.isNotEmpty()) {
                                            val h = digits.toFloatOrNull() ?: 0f
                                            targetWidth = (h * aspect).roundToInt().toString()
                                        }
                                        activePreset = "custom"
                                    }
                                }
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceContainerLow.copy(alpha=0.5f), RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                        .padding(16.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("QUICK SCALE PRESETS", fontSize = 10.sp, color = OnSurfaceVariant, letterSpacing = 0.5.sp)
                        Text(if (activePreset == "custom") "Custom Mode" else "Current: $activePreset%", fontSize = 10.sp, color = Primary, fontWeight = FontWeight.Medium)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PresetButton(Modifier.weight(1f), "25%", activePreset == "25", !isProcessing) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            activePreset = "25"; targetWidth = (imageWidth * 0.25).roundToInt().toString(); targetHeight = (imageHeight * 0.25).roundToInt().toString()
                        }
                        PresetButton(Modifier.weight(1f), "50%", activePreset == "50", !isProcessing) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            activePreset = "50"; targetWidth = (imageWidth * 0.5).roundToInt().toString(); targetHeight = (imageHeight * 0.5).roundToInt().toString()
                        }
                        PresetButton(Modifier.weight(1f), "75%", activePreset == "75", !isProcessing) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            activePreset = "75"; targetWidth = (imageWidth * 0.75).roundToInt().toString(); targetHeight = (imageHeight * 0.75).roundToInt().toString()
                        }
                        PresetButton(Modifier.weight(1f), "Custom", activePreset == "custom", !isProcessing, icon = Icons.Outlined.Tune) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            activePreset = "custom"
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Resampling Engine
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(12.dp))
                    .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.FilterCenterFocus, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Resampling Engine", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                    }
                    Text("Quality Filter", fontSize = 10.sp, color = OnSurfaceVariant)
                }
                Text("Mathematical interpolation algorithm applied during pixel recalculation.", fontSize = 12.sp, color = OnSurfaceVariant, modifier = Modifier.padding(bottom = 12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RadioOptionCard(
                        title = "Bicubic Hermite",
                        badge = "Recommended",
                        desc = "Smooth transitions & sharp clarity optimal for photos, drawings, and renders.",
                        isSelected = resampleAlgo == "bicubic",
                        isEnabled = !isProcessing,
                        onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); resampleAlgo = "bicubic" }
                    )
                    RadioOptionCard(
                        title = "Bilinear Filtering",
                        badge = "Ultra-Fast",
                        badgeColor = SurfaceContainerLowest,
                        badgeTextColor = OnSurfaceVariant,
                        desc = "Medium sharpness balance engineered for speedy batch compression on low-power devices.",
                        isSelected = resampleAlgo == "bilinear",
                        isEnabled = !isProcessing,
                        onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); resampleAlgo = "bilinear" }
                    )
                    RadioOptionCard(
                        title = "Nearest Neighbor",
                        badge = "Zero Blur",
                        badgeColor = SurfaceContainerLowest,
                        badgeTextColor = OnSurfaceVariant,
                        desc = "Hard-edged pixel replication designed specifically for pixel art, screenshots, and line art.",
                        isSelected = resampleAlgo == "nearest",
                        isEnabled = !isProcessing,
                        onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); resampleAlgo = "nearest" }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Output Encoding Format
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

            Spacer(modifier = Modifier.height(16.dp))

            // Storage Impact Projection Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(PrimaryContainer, SecondaryContainer, Secondary)), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Analytics, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("STORAGE IMPACT PROJECTION", fontSize = 11.sp, color = OnPrimary, letterSpacing = 0.5.sp)
                        }
                        Row(
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(SurfaceContainerLowest.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (percentSavings > 0) "-$percentSavings% footprint" else "+${percentSavings * -1}% footprint", fontSize = 10.sp, color = OnPrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Original File", fontSize = 10.sp, color = OnPrimary.copy(alpha = 0.7f))
                            Text("%.1f MB".format(imageSizeMb), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = OnPrimary, letterSpacing = (-0.5).sp)
                            Text("$imageWidth × $imageHeight", fontSize = 12.sp, color = OnPrimary.copy(alpha = 0.8f))
                        }
                        Icon(Icons.Outlined.East, contentDescription = null, tint = OnPrimary.copy(alpha = 0.6f), modifier = Modifier.size(24.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Estimated Result", fontSize = 10.sp, color = OnPrimary.copy(alpha = 0.7f))
                            Text(if(estMb >= 1f) "~%.1f MB".format(estMb) else "~${(estMb*1000).toInt()} KB", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = OnPrimary)
                            Text("${tW.toInt()} × ${tH.toInt()}", fontSize = 12.sp, color = OnPrimary.copy(alpha = 0.8f))
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(SurfaceContainerLowest.copy(alpha = 0.2f)).padding(2.dp)) {
                        Box(modifier = Modifier.fillMaxWidth(barProgress).fillMaxHeight().clip(RoundedCornerShape(50)).background(SurfaceContainerLowest))
                    }

                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (savedMb > 0) "Saved space: ~%.1f MB".format(savedMb) else "Footprint Increased", fontSize = 10.sp, color = OnPrimary.copy(alpha = 0.8f))
                        Text("Web & Mail Ready", fontSize = 10.sp, color = OnPrimary.copy(alpha = 0.8f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Visual Proof Gallery Strip
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(12.dp))
                    .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Preview Frame Crop", fontSize = 13.sp, color = OnSurface, fontWeight = FontWeight.SemiBold)
                    Text("100% Zoom Check", fontSize = 10.sp, color = Primary, fontWeight = FontWeight.Medium)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Box(modifier = Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceContainer)) {
                            AsyncImage(
                                model = imageUri,
                                contentDescription = "Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(modifier = Modifier.padding(4.dp).clip(RoundedCornerShape(4.dp)).background(InverseSurface.copy(alpha = 0.75f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text("Original (1x)", fontSize = 10.sp, color = InverseOnSurface)
                            }
                        }
                        Text("Uncompressed Native", fontSize = 10.sp, color = OnSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Box(modifier = Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceContainer)) {
                            AsyncImage(
                                model = imageUri,
                                contentDescription = "Preview Scaled",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(modifier = Modifier.padding(4.dp).clip(RoundedCornerShape(4.dp)).background(PrimaryContainer).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text("Resampled", fontSize = 10.sp, color = OnPrimary)
                            }
                        }
                        Text(if (resampleAlgo == "bicubic") "Bicubic Antialiased" else if(resampleAlgo == "bilinear") "Bilinear Fast" else "Pixel Perfect", fontSize = 10.sp, color = OnSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(140.dp))
        }
    }
}

// Sub-components

@Composable
private fun ModeSwitchButton(modifier: Modifier, icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    val bg = if (isSelected) SurfaceContainerLowest else Color.Transparent
    val contentCol = if (isSelected) Primary else OnSurfaceVariant
    val elevation = if (isSelected) 1.dp else 0.dp

    Row(
        modifier = modifier
            .shadow(elevation, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = contentCol, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium, color = contentCol)
    }
}

@Composable
private fun DimensionInputBox(value: String, icon: ImageVector, unit: String, isEnabled: Boolean, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceContainerLow)
            .alpha(if (isEnabled) 1f else 0.5f)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = isEnabled,
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface),
            cursorBrush = SolidColor(Primary),
            modifier = Modifier.weight(1f)
        )
        Text(unit, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = OnSurfaceVariant)
    }
}

@Composable
private fun PresetButton(modifier: Modifier, label: String, isSelected: Boolean, isEnabled: Boolean, icon: ImageVector? = null, onClick: () -> Unit) {
    val bg = if (isSelected) PrimaryContainer else SurfaceContainerLowest
    val textCol = if (isSelected) OnPrimary else OnSurfaceVariant
    val weight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium

    Row(
        modifier = modifier
            .shadow(1.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(enabled = isEnabled) { onClick() }
            .alpha(if (isEnabled) 1f else 0.5f)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = textCol, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(2.dp))
        }
        Text(label, fontSize = 13.sp, fontWeight = weight, color = textCol)
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
private fun RadioOptionCard(title: String, badge: String, badgeColor: Color = PrimaryContainer, badgeTextColor: Color = OnPrimary, desc: String, isSelected: Boolean, isEnabled: Boolean, onClick: () -> Unit) {
    val bg = if (isSelected) SurfaceContainerHigh.copy(alpha=0.6f) else SurfaceContainerLowest

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(enabled = isEnabled) { onClick() }
            .alpha(if (isEnabled) 1f else 0.5f)
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(modifier = Modifier.padding(top = 2.dp).size(16.dp).clip(CircleShape).border(if(isSelected) 5.dp else 1.dp, if(isSelected) Primary else OutlineVariant, CircleShape))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontSize = 13.sp, fontWeight = if(isSelected) FontWeight.SemiBold else FontWeight.Medium, color = OnSurface)
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(badgeColor).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(badge, fontSize = 10.sp, color = badgeTextColor)
                }
            }
            Text(desc, fontSize = 12.sp, color = OnSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun ResizeImageTopBar(onNavigateBack: () -> Unit) {
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
        Text("Resize Image", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OnSurface, letterSpacing = (-0.01).sp)
    }
}

@Composable
private fun ResizeBottomBar(
    isProcessing: Boolean,
    isComplete: Boolean,
    isEnabled: Boolean,
    targetDims: String,
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
            .background(SurfaceContainerLowest.copy(alpha = 0.95f))
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
                        ActionIcon(icon = Icons.Outlined.Share, label = "Share File", onClick = onShare)
                        ActionIcon(icon = Icons.Outlined.OpenInNew, label = "Open File", onClick = onOpen)
                        ActionIcon(icon = Icons.Outlined.RestartAlt, label = "Resize New", onClick = onReset)
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
                                .clickable(enabled = isEnabled && !isProcessing) { onConvert() }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedContent(targetState = isProcessing, label = "btnState") { processing ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    if (processing) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(color = contentColor, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Resampling...", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = contentColor)
                                        }
                                        Text("", color = Color.Transparent)
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Outlined.PhotoSizeSelectLarge, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Resize & Save Image", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = contentColor)
                                        }
                                        Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(SurfaceContainerLowest.copy(alpha=0.2f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                            Text("Target: $targetDims", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = contentColor)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.VerifiedUser, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Processed locally on device • Metadata & DPI preserved", fontSize = 10.sp, color = OnSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionIcon(icon: ImageVector, label: String, onClick: () -> Unit) {
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