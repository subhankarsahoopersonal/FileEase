package com.subha.fileease.ui.screens

import android.os.Vibrator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.roundToInt

// Exact Tailwind Color Tokens from Mockup
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

@Composable
fun CropImageScreen(onNavigateBack: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Engine & Output State
    var hasImage by remember { mutableStateOf(true) }
    var isProcessing by remember { mutableStateOf(false) }
    var isComplete by remember { mutableStateOf(false) }

    // Tuning State
    var targetQuality by remember { mutableFloatStateOf(82f) }
    var stripExif by remember { mutableStateOf(true) }
    var convertWebp by remember { mutableStateOf(false) }

    // Pure Kotlin Float Math
    val factor = maxOf(0.08f, (targetQuality / 100f).pow(1.8f))
    val estimatedMb = 8.4f * factor
    val savedMb = 8.4f - estimatedMb
    val percentSavings = ((savedMb / 8.4f) * 100f).roundToInt()
    val ratioWidth = maxOf(0.08f, minOf(1.0f, estimatedMb / 8.4f))

    val progressWidth by animateFloatAsState(targetValue = ratioWidth, label = "savings")

    Scaffold(
        containerColor = Background,
        topBar = { CropImageTopBar(onNavigateBack) },
        bottomBar = {
            CropImageBottomBar(
                isProcessing = isProcessing,
                isComplete = isComplete,
                isEnabled = hasImage,
                onConvert = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    coroutineScope.launch {
                        isProcessing = true
                        delay(1100)
                        isProcessing = false
                        isComplete = true
                        delay(1800)
                        isComplete = false
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
                    Icon(Icons.Outlined.AutoFixHigh, contentDescription = null, tint = Primary, modifier = Modifier.size(17.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Precision Compression", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                    Text("Reduce photo file weight while preserving optical visual clarity on-device.", fontSize = 12.sp, color = OnSurfaceVariant)
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
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(SurfaceContainer).padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SecondaryContainer))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ready", fontSize = 10.sp, color = OnSurfaceVariant)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceContainer)) {
                        // Dummy image for Phase 1
                        Box(modifier = Modifier.fillMaxSize().background(PrimaryContainer.copy(alpha = 0.2f)))
                        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).clip(RoundedCornerShape(4.dp)).background(InverseSurface.copy(alpha = 0.85f)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                            Text("JPG", fontSize = 9.sp, fontWeight = FontWeight.Medium, color = InverseOnSurface)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("HighRes_Landscape_Camera.jpg", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface, maxLines = 1)
                        Text("4000 × 3000 px", fontSize = 12.sp, color = OnSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                        Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(SurfaceContainer).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text("8.4 MB Original", fontSize = 11.sp, color = OnSurface)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("24-bit sRGB", fontSize = 10.sp, color = OnSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceContainer)
                            .clickable { }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("Change", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Primary)
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
                        Text("Original (8.4 MB)", fontSize = 10.sp, color = OnPrimary.copy(alpha = 0.8f))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(SurfaceContainerLowest.copy(alpha = 0.3f))) {
                        Box(modifier = Modifier.fillMaxWidth(progressWidth).fillMaxHeight().clip(RoundedCornerShape(50)).background(SurfaceContainerLowest))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Compression Presets
            // FIXED: Using start and end instead of horizontal to fix compiler error
            Row(modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Compression Presets", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                Text("Pick one or customize below", fontSize = 10.sp, color = OnSurfaceVariant)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PresetCard(
                    title = "Smart Compress",
                    sizeText = "~1.8 MB",
                    desc = "80% Quality · Balanced for web & messaging apps",
                    badge1 = "-78% size",
                    badge2 = "• Imperceptible loss",
                    isSelected = targetQuality == 80f,
                    isRecommended = true,
                    onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); targetQuality = 80f }
                )
                PresetCard(
                    title = "Maximum Shrink",
                    sizeText = "~820 KB",
                    desc = "50% Quality · Ultra compact for strict email attachment caps",
                    badge1 = "-90% size",
                    badge2 = "• Noticeable compression",
                    isSelected = targetQuality == 50f,
                    onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); targetQuality = 50f }
                )
                PresetCard(
                    title = "Minimal Compression",
                    sizeText = "~5.1 MB",
                    desc = "95% Quality · Near-lossless archival for physical photo printing",
                    badge1 = "-39% size",
                    badge2 = "• Pure studio fidelity",
                    isSelected = targetQuality == 95f,
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
                    CustomSwitch(checked = stripExif, onCheckedChange = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); stripExif = it })
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
                    CustomSwitch(checked = convertWebp, onCheckedChange = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); convertWebp = it })
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
private fun PresetCard(
    title: String,
    sizeText: String,
    desc: String,
    badge1: String,
    badge2: String,
    isSelected: Boolean,
    isRecommended: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(12.dp))
            .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
            .clickable { onClick() }
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
private fun CustomSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val bg by animateColorAsState(if (checked) PrimaryContainer else SurfaceContainerHigh, label = "switchBg")
    val offset by animateDpAsState(if (checked) 24.dp else 0.dp, label = "switchOffset")

    Box(
        modifier = Modifier
            .width(48.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable { onCheckedChange(!checked) }
            .padding(2.dp),
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
private fun CropImageBottomBar(isProcessing: Boolean, isComplete: Boolean, isEnabled: Boolean, onConvert: () -> Unit) {
    val cornerRadius by animateDpAsState(targetValue = if (isProcessing) 50.dp else 12.dp, label = "buttonShape")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface.copy(alpha = 0.90f))
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
                    .background(PrimaryContainer)
                    .clickable(enabled = isEnabled && !isProcessing && !isComplete) { onConvert() },
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(targetState = Pair(isProcessing, isComplete), label = "btn") { state ->
                    val (processing, complete) = state
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (processing) {
                            CircularProgressIndicator(color = OnPrimaryContainer, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Optimizing Pixels...", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnPrimaryContainer)
                        } else if (complete) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = OnPrimaryContainer, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Compressed & Saved!", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnPrimaryContainer)
                        } else {
                            Icon(Icons.Outlined.Compress, contentDescription = null, tint = OnPrimaryContainer, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Compress & Save Image", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnPrimaryContainer)
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