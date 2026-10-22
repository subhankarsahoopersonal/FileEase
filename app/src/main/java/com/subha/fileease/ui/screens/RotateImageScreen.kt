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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
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

// Namespaced Tailwind Color Tokens
private val RotBackground = Color(0xFFFAF8FF)
private val RotSurface = Color(0xFFFAF8FF)
private val RotSurfaceContainerLowest = Color(0xFFFFFFFF)
private val RotSurfaceContainerLow = Color(0xFFF2F3FF)
private val RotSurfaceContainer = Color(0xFFEAEDFF)
private val RotSurfaceContainerHigh = Color(0xFFE2E7FF)
private val RotInverseSurface = Color(0xFF283044)
private val RotInverseOnSurface = Color(0xFFEEF0FF)
private val RotOnSurface = Color(0xFF131B2E)
private val RotOnSurfaceVariant = Color(0xFF424656)
private val RotPrimary = Color(0xFF0050CB)
private val RotOnPrimary = Color(0xFFFFFFFF)
private val RotPrimaryContainer = Color(0xFF0066FF)
private val RotOnPrimaryContainer = Color(0xFFF8F7FF)
private val RotSecondaryContainer = Color(0xFF2170E4)
private val RotOnSecondary = Color(0xFFFFFFFF)
private val RotOutline = Color(0xFF727687)

@Composable
fun RotateImageScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    // Safely retrieve Vibrator to prevent Service Context crashes
    val vibrator = remember {
        try { context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator }
        catch (e: Exception) { null }
    }

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Source Data State
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageName by remember { mutableStateOf("Rotate_Preview.jpg") }
    var imageWidth by remember { mutableIntStateOf(4032) }
    var imageHeight by remember { mutableIntStateOf(3024) }
    var imageSizeMb by remember { mutableFloatStateOf(5.8f) }

    // Transformation State
    var baseRotation by remember { mutableFloatStateOf(0f) }
    var fineAngle by remember { mutableFloatStateOf(0f) }
    var scaleX by remember { mutableFloatStateOf(1f) }
    var scaleY by remember { mutableFloatStateOf(1f) }
    var isGridVisible by remember { mutableStateOf(true) }
    var isBatchMode by remember { mutableStateOf(false) }

    val totalRotation = baseRotation + fineAngle

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
                        name = if (nameIdx != -1) cursor.getString(nameIdx) ?: "Image.jpg" else "Image.jpg"
                        sizeBytes = if (sizeIdx != -1) cursor.getLong(sizeIdx) else 0L
                    }
                }
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }

                withContext(Dispatchers.Main) {
                    imageUri = uri
                    imageName = name
                    imageWidth = options.outWidth
                    imageHeight = options.outHeight
                    imageSizeMb = sizeBytes / (1024f * 1024f)

                    baseRotation = 0f
                    fineAngle = 0f
                    scaleX = 1f
                    scaleY = 1f
                }
            }
        }
    }

    Scaffold(
        containerColor = RotBackground,
        topBar = { RotateImageTopBar(onNavigateBack) },
        bottomBar = {
            RotateBottomBar(
                totalRotation = totalRotation,
                scaleX = scaleX,
                scaleY = scaleY,
                isBatchMode = isBatchMode,
                onSaveClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) }
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

            // Mode Selector & Header Info
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(RotSurfaceContainer).padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ModeButton(
                            label = "Single Image",
                            icon = Icons.Outlined.Photo,
                            isSelected = !isBatchMode,
                            onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); isBatchMode = false }
                        )
                        ModeButton(
                            label = "Batch Folder (12)",
                            icon = Icons.Outlined.FolderCopy,
                            isSelected = isBatchMode,
                            onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); isBatchMode = true }
                        )
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(RotSurfaceContainer)
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                baseRotation = 0f; fineAngle = 0f; scaleX = 1f; scaleY = 1f
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.RestartAlt, contentDescription = null, tint = RotOnSurfaceVariant, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset", fontSize = 10.sp, color = RotOnSurfaceVariant, fontWeight = FontWeight.Medium)
                    }
                }
                Text("Adjust image angle, flip orientation, and level horizons seamlessly.", fontSize = 12.sp, color = RotOnSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            }

            // Interactive Preview Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(12.dp))
                    .background(RotSurfaceContainerLowest, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f/3f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(RotSurfaceContainerLow)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .zIndex(20f),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(RotInverseSurface.copy(alpha=0.85f)).padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Explore, contentDescription = null, tint = RotInverseOnSurface, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            val safeTotal = if (totalRotation.isNaN()) 0f else totalRotation
                            val sign = if (safeTotal > 0) "+" else ""
                            val displayRot = ((safeTotal * 10f).roundToInt() / 10f).toString()
                            Text("${sign}${displayRot}°", fontSize = 11.sp, color = RotInverseOnSurface, fontWeight = FontWeight.SemiBold)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (scaleX == -1f || scaleY == -1f) {
                                Row(
                                    modifier = Modifier.clip(RoundedCornerShape(50)).background(RotSecondaryContainer).padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.Flip, contentDescription = null, tint = RotOnSecondary, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Flipped", fontSize = 10.sp, color = RotOnSecondary, fontWeight = FontWeight.Medium)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(RotSurfaceContainerLowest.copy(alpha=0.9f))
                                    .clickable { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); isGridVisible = !isGridVisible },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Grid3x3, contentDescription = null, tint = if (isGridVisible) RotPrimary else RotOnSurfaceVariant, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(color = RotPrimary.copy(alpha=0.15f), radius = 96.dp.toPx(), center = center, style = Stroke(1.dp.toPx()))
                        drawCircle(color = RotPrimary.copy(alpha=0.20f), radius = 72.dp.toPx(), center = center, style = Stroke(1.dp.toPx()))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageUri != null) {
                            AsyncImage(
                                model = imageUri,
                                contentDescription = "Viewport",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(4.dp))
                                    .graphicsLayer(
                                        rotationZ = if (totalRotation.isNaN()) 0f else totalRotation,
                                        scaleX = scaleX,
                                        scaleY = scaleY
                                    )
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, tint = RotPrimary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Tap to browse gallery", fontSize = 13.sp, color = RotOutline, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    if (isGridVisible) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            if (w > 0f && h > 0f) {
                                drawLine(color = RotPrimary.copy(alpha=0.3f), start = Offset(w/3, 0f), end = Offset(w/3, h), strokeWidth = 1.dp.toPx())
                                drawLine(color = RotPrimary.copy(alpha=0.3f), start = Offset(w*2/3, 0f), end = Offset(w*2/3, h), strokeWidth = 1.dp.toPx())
                                drawLine(color = RotPrimary.copy(alpha=0.3f), start = Offset(0f, h/3), end = Offset(w, h/3), strokeWidth = 1.dp.toPx())
                                drawLine(color = RotPrimary.copy(alpha=0.3f), start = Offset(0f, h*2/3), end = Offset(w, h*2/3), strokeWidth = 1.dp.toPx())
                                drawCircle(color = RotPrimaryContainer, radius = 3.dp.toPx(), center = Offset(w/2, h/2))
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = isBatchMode) {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Batch Targets (12 files)", fontSize = 10.sp, color = RotOnSurfaceVariant, fontWeight = FontWeight.Medium)
                            Text("Select All", fontSize = 10.sp, color = RotPrimary, fontWeight = FontWeight.SemiBold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).border(2.dp, RotPrimaryContainer, RoundedCornerShape(8.dp)).background(RotSurfaceContainer)) {
                                AsyncImage(model = imageUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                Box(modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).size(12.dp).clip(CircleShape).background(RotPrimary), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.Check, contentDescription = null, tint = RotOnPrimary, modifier = Modifier.size(8.dp))
                                }
                            }
                            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(RotSurfaceContainer).alpha(0.85f)) {
                                AsyncImage(model = imageUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            }
                            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(RotSurfaceContainerHigh), contentAlignment = Alignment.Center) {
                                Text("+10", fontSize = 10.sp, color = RotOnSurfaceVariant)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lossless Quality Engine Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(RotSurfaceContainer)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(RotPrimaryContainer.copy(alpha=0.15f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Verified, contentDescription = null, tint = RotPrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("100% Lossless Engine", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = RotOnSurface)
                    Text("EXIF orientation rewrite · Zero re-encoding artifacts", fontSize = 12.sp, color = RotOnSurfaceVariant, maxLines = 1)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Direction Directional Buttons
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Orientation Tools", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = RotOnSurface)
                    Text("Tap to rotate/mirror", fontSize = 10.sp, color = RotOnSurfaceVariant)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DirectionalButton(Modifier.weight(1f), Icons.Outlined.RotateLeft, "-90°", "Left") { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); baseRotation -= 90f }
                    DirectionalButton(Modifier.weight(1f), Icons.Outlined.RotateRight, "+90°", "Right") { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); baseRotation += 90f }
                    DirectionalButton(Modifier.weight(1f), Icons.Outlined.Flip, "Flip X", "Horizontal", 0f) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); scaleX = if(scaleX == 1f) -1f else 1f }
                    DirectionalButton(Modifier.weight(1f), Icons.Outlined.Flip, "Flip Y", "Vertical", 90f) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); scaleY = if(scaleY == 1f) -1f else 1f }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Fine-Tuning Horizon / Angle Dial
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(12.dp))
                    .background(RotSurfaceContainerLowest, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Straighten, contentDescription = null, tint = RotPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Horizon Straighten Dial", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = RotOnSurface)
                    }
                    Row(modifier = Modifier.clip(RoundedCornerShape(50)).background(RotSurfaceContainer).padding(horizontal = 8.dp, vertical = 2.dp)) {
                        val safeFineAngle = if (fineAngle.isNaN()) 0f else fineAngle
                        val sign = if (safeFineAngle > 0) "+" else ""
                        val displayAngle = ((safeFineAngle * 10f).roundToInt() / 10f).toString()
                        Text("${sign}${displayAngle}°", fontSize = 11.sp, color = RotPrimary, fontWeight = FontWeight.Bold)
                    }
                }

                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(2.dp)).rotate(45f).background(RotPrimary).offset(y = 6.dp).zIndex(10f))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(RotSurfaceContainerLow),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().alpha(0.4f)) {
                            val w = size.width
                            val h = size.height
                            if (w > 0f && h > 0f) {
                                val numTicks = 30
                                val spacing = w / numTicks
                                for (i in 0..numTicks) {
                                    val x = i * spacing
                                    val isMajor = i % 5 == 0
                                    val tickH = if (isMajor) h * 0.4f else h * 0.2f
                                    drawLine(color = RotOutline, start = Offset(x, (h - tickH)/2), end = Offset(x, (h + tickH)/2), strokeWidth = if(isMajor) 1.5.dp.toPx() else 1.dp.toPx())
                                }
                            }
                        }
                        Box(modifier = Modifier.width(2.dp).fillMaxHeight(0.6f).background(RotPrimary.copy(alpha=0.4f)))

                        Slider(
                            value = fineAngle,
                            onValueChange = { fineAngle = it },
                            valueRange = -45f..45f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.Transparent,
                                activeTrackColor = Color.Transparent,
                                inactiveTrackColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("-45°", fontSize = 10.sp, color = RotOnSurfaceVariant)
                        Text("0° Center", fontSize = 10.sp, color = RotPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); fineAngle = 0f })
                        Text("+45°", fontSize = 10.sp, color = RotOnSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Format & Processing Info Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(12.dp))
                    .background(RotSurfaceContainerLowest, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(RotSurfaceContainer), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, tint = RotPrimary, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(if(imageUri != null) imageName else "No Image Selected", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = RotOnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(0.6f))
                        val safeSize = if (imageSizeMb.isNaN()) 0f else imageSizeMb
                        val displayMb = ((safeSize * 10f).roundToInt() / 10f).toString()
                        if(imageUri != null) {
                            Text("${imageWidth} × ${imageHeight} · ${displayMb} MB", fontSize = 10.sp, color = RotOnSurfaceVariant)
                        } else {
                            Text("Waiting for input", fontSize = 10.sp, color = RotOnSurfaceVariant)
                        }
                    }
                }
                Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(RotSurfaceContainer).padding(horizontal = 8.dp, vertical = 2.dp)) {
                    Text("Auto-Level", fontSize = 10.sp, color = RotOnSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(140.dp))
        }
    }
}

// Sub-components

@Composable
private fun ModeButton(label: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    val bg = if (isSelected) RotSurfaceContainerLowest else Color.Transparent
    val contentCol = if (isSelected) RotPrimary else RotOnSurfaceVariant
    val elevation = if (isSelected) 1.dp else 0.dp

    Row(
        modifier = Modifier
            .shadow(elevation, RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = contentCol, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, fontSize = 11.sp, color = contentCol)
    }
}

@Composable
private fun DirectionalButton(modifier: Modifier, icon: ImageVector, title: String, subtitle: String, iconRotation: Float = 0f, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .shadow(1.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(RotSurfaceContainerLowest)
            .clickable { onClick() }
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(RotSurfaceContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = RotPrimary, modifier = Modifier.size(22.dp).rotate(iconRotation))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(title, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = RotOnSurface)
        Text(subtitle, fontSize = 9.sp, color = RotOnSurfaceVariant)
    }
}

@Composable
private fun RotateImageTopBar(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 1.dp)
            .background(RotSurface.copy(alpha = 0.90f))
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).clickable { onNavigateBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = RotOnSurface)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text("Rotate Image", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = RotOnSurface, letterSpacing = (-0.01).sp)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(50)).background(RotSurfaceContainerHigh.copy(alpha=0.6f)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(RotPrimaryContainer))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Offline Engine Active • 100% On-device", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = RotOnSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun RotateBottomBar(
    totalRotation: Float,
    scaleX: Float,
    scaleY: Float,
    isBatchMode: Boolean,
    onSaveClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 24.dp)
            .background(RotSurface.copy(alpha = 0.90f))
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Sync, contentDescription = null, tint = RotPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))

                    val safeTotal = if (totalRotation.isNaN()) 0f else totalRotation
                    val normalizedRot = ((safeTotal.roundToInt() % 360) + 360) % 360
                    val rotDesc = if (normalizedRot == 0) "Upright" else "${normalizedRot}°"
                    val flips = mutableListOf<String>()
                    if (scaleX == -1f) flips.add("Mirrored X")
                    if (scaleY == -1f) flips.add("Mirrored Y")
                    val transformDesc = if (flips.isEmpty()) rotDesc else "$rotDesc • ${flips.joinToString(" • ")}"
                    Text("Transform: $transformDesc", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = RotOnSurfaceVariant)
                }
                Text("Dest: /Storage/DCIM/Edited", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = RotOnSurfaceVariant)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(RotPrimaryContainer)
                    .clickable { onSaveClick() }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Save, contentDescription = null, tint = RotOnPrimaryContainer, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isBatchMode) "Apply to 12 Photos & Save" else "Apply Rotation & Save", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = RotOnPrimaryContainer)
                }
            }
        }
    }
}