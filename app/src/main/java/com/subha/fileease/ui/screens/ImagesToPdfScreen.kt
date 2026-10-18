package com.subha.fileease.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Exact Tailwind Color Tokens
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
private val PrimaryFixed = Color(0xFFDAE1FF)
private val OnPrimaryFixed = Color(0xFF001849)
private val Error = Color(0xFFBA1A1A)
private val ErrorContainer = Color(0xFFFFDAD6)
private val Outline = Color(0xFF727687)
private val OutlineVariant = Color(0xFFC2C6D8)
private val Secondary = Color(0xFF0058BE)

// Data class representing queued images
data class QueuedImage(
    val id: String,
    val name: String,
    val dimensions: String,
    val sizeMb: String,
    var rotation: Float = 0f
)

@Composable
fun ImagesToPdfScreen(onNavigateBack: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    // State Management
    var fileName by remember { mutableStateOf("Scan_Bundle_2024") }
    var isProcessing by remember { mutableStateOf(false) }
    var isComplete by remember { mutableStateOf(false) }

    val imageQueue = remember {
        mutableStateListOf(
            QueuedImage("1", "IMG_2024_01.jpg", "3024 × 4032", "2.4 MB"),
            QueuedImage("2", "Receipt_Scan.jpg", "1820 × 2960", "1.1 MB"),
            QueuedImage("3", "Whiteboard_Notes.png", "2400 × 1600", "3.2 MB"),
            QueuedImage("4", "ID_Card.jpg", "1080 × 1920", "980 KB")
        )
    }

    var selectedPageSize by remember { mutableStateOf("A4") }
    var selectedOrientation by remember { mutableStateOf("Auto") }
    var selectedMargins by remember { mutableStateOf("Compact (5mm)") }
    var selectedFit by remember { mutableStateOf("Fit Entire") }
    var selectedDpi by remember { mutableStateOf(150) } // 150 or 300

    Scaffold(
        containerColor = Surface,
        topBar = { ImagesToPdfTopBar(onNavigateBack) },
        bottomBar = {
            ImagesToPdfBottomBar(
                imageCount = imageQueue.size,
                isProcessing = isProcessing,
                isComplete = isComplete,
                onConvert = {
                    if (imageQueue.isNotEmpty()) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        coroutineScope.launch {
                            isProcessing = true
                            delay(1500) // Simulated build time
                            isProcessing = false
                            isComplete = true
                            delay(2000)
                            isComplete = false
                        }
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
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Informational Context & Offline Assurance
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Convert photo gallery & scans into a structured PDF document",
                        fontSize = 13.sp,
                        color = OnSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(SurfaceContainerHigh.copy(alpha = 0.7f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.VerifiedUser, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("100% On-Device Processing · Zero Cloud Upload", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                    }
                }

                // Document Filename Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(1.dp, RoundedCornerShape(12.dp), spotColor = Color.Black.copy(alpha = 0.04f))
                        .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("FILE OUTPUT NAME", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant, letterSpacing = 0.5.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BasicTextField(
                                    value = fileName,
                                    onValueChange = { fileName = it },
                                    textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface),
                                    modifier = Modifier.weight(1f, fill = false),
                                    singleLine = true
                                )
                                Text(".pdf", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Outline)
                            }
                        }
                    }
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).clickable { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) }.padding(4.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }

                // Source Queue Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Source Queue", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(PrimaryFixed).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                Text("${imageQueue.size} files", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = OnPrimaryFixed)
                            }
                        }
                        Text("Clear All", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Error, modifier = Modifier.clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            imageQueue.clear()
                        }.padding(4.dp))
                    }

                    AnimatedVisibility(
                        visible = imageQueue.isEmpty(),
                        enter = fadeIn() + slideInVertically { -it / 2 },
                        exit = fadeOut()
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceContainer).padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Queue is empty. Tap \"+ Add More Images\" below to start.", fontSize = 12.sp, color = OnSurfaceVariant, textAlign = TextAlign.Center)
                        }
                    }

                    imageQueue.forEachIndexed { index, img ->
                        QueueItemRow(
                            item = img,
                            index = index + 1,
                            onRotate = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                img.rotation = (img.rotation + 90f) % 360f
                            },
                            onDelete = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                imageQueue.remove(img)
                            }
                        )
                    }
                }

                // Add More Images Action Tile
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceContainer)
                        .clickable { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
                        .padding(vertical = 14.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add More Images", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                }

                // Page Layout Settings Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(1.dp, RoundedCornerShape(12.dp), spotColor = Color.Black.copy(alpha = 0.04f))
                        .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.AutoStories, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Page Layout Settings", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                        }
                        Text("Sheet & Frame", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = OnSurfaceVariant)
                    }

                    SegmentControlRow("PAGE SIZE", listOf("A4", "US Letter", "Fit Image", "Original"), selectedPageSize) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedPageSize = it
                    }

                    // Orientation with Icons
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("ORIENTATION", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant, letterSpacing = 0.5.sp)
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceContainer).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val options = listOf(
                                Triple("Auto", Icons.Outlined.Sync, "Auto"),
                                Triple("Portrait", Icons.Outlined.Smartphone, "Portrait"),
                                Triple("Landscape", Icons.Outlined.StayCurrentLandscape, "Landscape")
                            )
                            options.forEach { (label, icon, value) ->
                                val isSelected = selectedOrientation == value
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) Primary else Color.Transparent)
                                        .clickable {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedOrientation = value
                                        }
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(icon, contentDescription = null, tint = if(isSelected) OnPrimary else OnSurfaceVariant, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(label, fontSize = 11.sp, fontWeight = if(isSelected) FontWeight.SemiBold else FontWeight.Medium, color = if(isSelected) OnPrimary else OnSurfaceVariant)
                                }
                            }
                        }
                    }

                    SegmentControlRow("MARGINS", listOf("None (0mm)", "Compact (5mm)", "Std (12mm)"), selectedMargins) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedMargins = it
                    }
                    SegmentControlRow("IMAGE FIT & SCALING", listOf("Fit Entire", "Fill Page", "Center"), selectedFit) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedFit = it
                    }
                }

                // Quality & Compression Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(1.dp, RoundedCornerShape(12.dp), spotColor = Color.Black.copy(alpha = 0.04f))
                        .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Tune, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Quality & Output Size", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                        }
                        Text("DPI Profile", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = OnSurfaceVariant)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QualityCard(
                            modifier = Modifier.weight(1f),
                            dpi = "300 DPI", title = "Print Quality", subtitle = "Lossless detail",
                            isSelected = selectedDpi == 300,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedDpi = 300
                            }
                        )
                        QualityCard(
                            modifier = Modifier.weight(1f),
                            dpi = "150 DPI", title = "Balanced Web", subtitle = "Fast sharing",
                            isSelected = selectedDpi == 150,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedDpi = 150
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(112.dp))
            }
        }
    }
}

@Composable
private fun ImagesToPdfTopBar(onNavigateBack: () -> Unit) {
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
                Text("Images To Pdf", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OnSurface, letterSpacing = (-0.01).sp)
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
private fun QueueItemRow(item: QueuedImage, index: Int, onRotate: () -> Unit, onDelete: () -> Unit) {
    val rotation by animateFloatAsState(targetValue = item.rotation, label = "rotateAnim")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(12.dp), spotColor = Color.Black.copy(alpha = 0.04f))
            .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(28.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.DragIndicator, contentDescription = "Drag", tint = Outline, modifier = Modifier.size(20.dp))
        }

        Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceContainerHigh)) {
            Box(modifier = Modifier.fillMaxSize().rotate(rotation), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Image, contentDescription = null, tint = OutlineVariant, modifier = Modifier.size(28.dp))
            }
            Box(
                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).clip(RoundedCornerShape(4.dp)).background(OnSurface.copy(alpha = 0.75f)).padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(index.toString(), fontSize = 9.sp, color = SurfaceContainerLowest)
            }
        }

        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${item.dimensions} • ${item.sizeMb}", fontSize = 12.sp, color = OnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).clickable { onRotate() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.RotateRight, contentDescription = "Rotate", tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
            }
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).clickable { onDelete() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete", tint = Error, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun SegmentControlRow(title: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant, letterSpacing = 0.5.sp)
        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceContainer).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            options.forEach { option ->
                val isSelected = selected == option
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) Primary else Color.Transparent)
                        .clickable { onSelect(option) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        fontSize = 11.sp,
                        fontWeight = if(isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if(isSelected) OnPrimary else OnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun QualityCard(modifier: Modifier, dpi: String, title: String, subtitle: String, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) Primary else SurfaceContainer
    val textColor = if (isSelected) OnPrimary else OnSurface
    val subTextColor = if (isSelected) OnPrimary.copy(alpha = 0.8f) else OnSurfaceVariant

    Column(
        modifier = modifier
            .shadow(if (isSelected) 1.dp else 0.dp, RoundedCornerShape(12.dp))
            .background(bgColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp)
            .height(96.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(dpi, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = textColor)
            if (isSelected) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(18.dp))
            } else {
                Icon(Icons.Outlined.RadioButtonUnchecked, contentDescription = null, tint = Outline, modifier = Modifier.size(18.dp))
            }
        }
        Column {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = textColor)
            Text(subtitle, fontSize = 10.sp, color = subTextColor)
        }
    }
}

@Composable
private fun ImagesToPdfBottomBar(imageCount: Int, isProcessing: Boolean, isComplete: Boolean, onConvert: () -> Unit) {
    val estimatedSize = if (imageCount > 0) "~${String.format("%.1f", imageCount * 0.95)} MB" else "0 MB"

    val buttonText = when {
        isProcessing -> "Generating PDF..."
        isComplete -> "Saved to Downloads!"
        imageCount == 0 -> "Add images to proceed"
        else -> "Convert $imageCount Images to PDF"
    }

    val buttonIcon = when {
        isProcessing -> Icons.Outlined.Refresh
        isComplete -> Icons.Filled.CheckCircle
        else -> Icons.Outlined.PictureAsPdf
    }

    val buttonColor = if (isComplete) Secondary else if (imageCount > 0) PrimaryContainer else SurfaceContainerHigh
    val contentColor = if (imageCount > 0 || isComplete) OnPrimaryContainer else OutlineVariant
    val cornerRadius by animateDpAsState(targetValue = if (isProcessing) 50.dp else 12.dp, label = "buttonShape")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface.copy(alpha = 0.95f))
            .shadow(elevation = 24.dp, spotColor = OnSurface.copy(alpha = 0.1f))
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(if (!isComplete && imageCount > 0 && !isProcessing) 6.dp else 0.dp, RoundedCornerShape(cornerRadius), spotColor = PrimaryContainer.copy(alpha = 0.4f))
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(if(imageCount == 0) buttonColor.copy(alpha = 0.5f) else buttonColor)
                    .clickable(enabled = imageCount > 0 && !isProcessing && !isComplete) { onConvert() },
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.SdCard, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Estimated size: $estimatedSize", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = OnSurfaceVariant)
            }
        }
    }
}