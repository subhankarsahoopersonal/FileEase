package com.subha.fileease.ui.screens

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

// Exact color tokens from the Stitch HTML Tailwind config
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
private val Secondary = Color(0xFF0058BE)
private val ErrorContainer = Color(0xFFFFDAD6)
private val OnErrorContainer = Color(0xFF93000A)
private val OutlineVariant = Color(0xFFC2C6D8)
private val SurfaceVariant = Color(0xFFDAE2FD)

@Composable
fun PdfCompressScreen(onNavigateBack: () -> Unit) {
    // State Management for settings
    var selectedPreset by remember { mutableStateOf("recommended") }
    var stripThumbnails by remember { mutableStateOf(true) }
    var grayscaleToggle by remember { mutableStateOf(false) }

    var isProcessing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Dynamic calculations mirroring the HTML JS Logic
    val originalSize = 42.6

    val baseSize = when (selectedPreset) {
        "extreme" -> 6.4
        "recommended" -> 14.9
        else -> 29.8 // light
    }

    val baseReduction = when (selectedPreset) {
        "extreme" -> 85
        "recommended" -> 65
        else -> 30
    }

    // Apply grayscale modifiers if active
    val finalSize = if (grayscaleToggle) max(1.8, baseSize * 0.75) else baseSize
    val finalReduction = if (grayscaleToggle) min(95, baseReduction + 10) else baseReduction
    val savedMb = originalSize - finalSize
    val remainingPct = (finalSize / originalSize).toFloat()

    Scaffold(
        containerColor = Surface,
        topBar = { CompressTopBar(onNavigateBack) },
        bottomBar = {
            CompressBottomBar(isProcessing) {
                coroutineScope.launch {
                    isProcessing = true
                    delay(2000)
                    isProcessing = false
                    // Handle post-compression navigation/toast here
                }
            }
        }
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
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Screen Intro
                Column {
                    Text("Compress PDF", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                    Text("Reduce document weight swiftly while maintaining legibility.", fontSize = 13.sp, color = OnSurfaceVariant)
                }

                // Source Document Card
                SourceDocumentCard()

                // Compression Presets
                CompressionPresetsSection(selectedPreset) { selectedPreset = it }

                // Live Size Projection Card
                ProjectionCard(finalSize, finalReduction, savedMb, remainingPct)

                // Refinement Options
                RefinementOptions(
                    stripThumbnails = stripThumbnails,
                    onStripToggle = { stripThumbnails = !stripThumbnails },
                    grayscale = grayscaleToggle,
                    onGrayscaleToggle = { grayscaleToggle = !grayscaleToggle }
                )

                // Info Banner
                ZeroCloudBanner()

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun CompressTopBar(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceContainerLowest.copy(alpha = 0.9f))
            .shadow(elevation = 1.dp, spotColor = Color.Black.copy(alpha = 0.04f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable { onNavigateBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = OnSurface)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text("Pdf Compressor", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Primary))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Offline Engine Active • 100% On-device", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Primary, letterSpacing = 0.5.sp)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Info, contentDescription = "Info", tint = OnSurfaceVariant)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Person, contentDescription = "User", tint = OnPrimary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun SourceDocumentCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(12.dp))
            .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ErrorContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, tint = OnErrorContainer, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Client_Portfolio_HiRes.pdf", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("42.6 MB", fontSize = 11.sp, color = OnSurfaceVariant)
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(OutlineVariant))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("58 pages", fontSize = 11.sp, color = OnSurfaceVariant)
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(OutlineVariant))
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(SurfaceContainerHigh).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("Ready", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Primary)
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceContainerLow)
                .clickable { }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text("Change", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Primary)
        }
    }
}

@Composable
private fun CompressionPresetsSection(selectedPreset: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("COMPRESSION PRESETS", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurface, letterSpacing = 0.5.sp)
            Text("Offline processing", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Primary)
        }

        PresetCard(
            id = "extreme", title = "Extreme Compression", reduction = "~85%",
            desc = "Low visual weight, 72 DPI. Ideal for email limits and mobile networks.",
            isSelected = selectedPreset == "extreme", onClick = { onSelect("extreme") }
        )

        PresetCard(
            id = "recommended", title = "Recommended", reduction = "Best Balance", isBold = true,
            desc = "Balanced density, 150 DPI. Crisp vector outlines and legible text.",
            isSelected = selectedPreset == "recommended", onClick = { onSelect("recommended") }
        )

        PresetCard(
            id = "light", title = "Light Compression", reduction = "~30%",
            desc = "Retains high fidelity, 300 DPI. Perfect for commercial prints and client portfolios.",
            isSelected = selectedPreset == "light", onClick = { onSelect("light") }
        )
    }
}

@Composable
private fun PresetCard(id: String, title: String, reduction: String, desc: String, isBold: Boolean = false, isSelected: Boolean, onClick: () -> Unit) {
    val shadowColor = if (isSelected) PrimaryContainer.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.05f)
    val shadowElevation = if (isSelected) 8.dp else 1.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(shadowElevation, RoundedCornerShape(12.dp), spotColor = shadowColor)
            .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Radio Button
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(if (isSelected) Primary else SurfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(OnPrimary))
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontSize = 15.sp, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium, color = OnSurface)
                Spacer(modifier = Modifier.width(6.dp))
                Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(if (isBold) PrimaryContainer else SurfaceContainerHigh).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(reduction, fontSize = 10.sp, fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal, color = if (isBold) OnPrimaryContainer else OnSurfaceVariant)
                }
            }
            Text(desc, fontSize = 12.sp, color = OnSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun ProjectionCard(finalSize: Double, finalReduction: Int, savedMb: Double, remainingPct: Float) {
    // Animate the progress bar when parameters change
    val animatedProgress by animateFloatAsState(
        targetValue = remainingPct,
        animationSpec = tween(durationMillis = 500)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(12.dp), spotColor = PrimaryContainer.copy(alpha = 0.3f))
            .background(
                Brush.linearGradient(listOf(PrimaryContainer, Secondary, Primary)),
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        // Ambient blur blob
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 40.dp, y = 40.dp)
                .size(160.dp)
                .blur(24.dp)
                .background(SurfaceContainerLowest.copy(alpha = 0.1f), CircleShape)
        )

        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Analytics, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PROJECTED ESTIMATE", fontSize = 11.sp, color = OnPrimary.copy(alpha = 0.8f))
                }
                Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(SurfaceContainerLowest.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                    Text("-$finalReduction% Reduction", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OnPrimary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Original Size", fontSize = 10.sp, color = OnPrimary.copy(alpha = 0.75f))
                    Text("42.6 MB", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnPrimary)
                }
                Icon(Icons.Outlined.ArrowForward, contentDescription = null, tint = OnPrimary.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text("New Estimated Size", fontSize = 10.sp, color = OnPrimary.copy(alpha = 0.75f))
                    Text("~${"%.1f".format(finalSize)} MB", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = OnPrimary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column {
                Box(modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(SurfaceContainerLowest.copy(alpha = 0.25f)).padding(2.dp)) {
                    Box(modifier = Modifier.fillMaxWidth(animatedProgress).fillMaxHeight().clip(RoundedCornerShape(50)).background(SurfaceContainerLowest))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Saves approx. ${"%.1f".format(savedMb)} MB", fontSize = 10.sp, color = OnPrimary.copy(alpha = 0.9f))
                    Text("100% Private local engine", fontSize = 10.sp, color = OnPrimary.copy(alpha = 0.9f))
                }
            }
        }
    }
}

@Composable
private fun RefinementOptions(
    stripThumbnails: Boolean, onStripToggle: () -> Unit,
    grayscale: Boolean, onGrayscaleToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(12.dp))
            .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("REFINEMENT OPTIONS", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurface, letterSpacing = 0.5.sp, modifier = Modifier.padding(horizontal = 4.dp).padding(bottom = 4.dp))

        CustomToggleRow(
            icon = Icons.Outlined.BurstMode,
            title = "Strip embedded thumbnails",
            subtitle = "Saves extra ~1.2 MB without data loss",
            isChecked = stripThumbnails,
            onToggle = onStripToggle
        )

        CustomToggleRow(
            icon = Icons.Outlined.FilterBAndW,
            title = "Convert color to grayscale",
            subtitle = "Maximized shrinkage for documents & notes",
            isChecked = grayscale,
            onToggle = onGrayscaleToggle
        )
    }
}

@Composable
private fun CustomToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onToggle() }.padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceContainer), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                Text(subtitle, fontSize = 12.sp, color = OnSurfaceVariant)
            }
        }

        val offset by animateFloatAsState(targetValue = if (isChecked) 20f else 0f)
        val bgColor = if (isChecked) Primary else SurfaceVariant

        Box(
            modifier = Modifier.width(44.dp).height(24.dp).clip(RoundedCornerShape(50)).background(bgColor).padding(2.dp)
        ) {
            Box(
                modifier = Modifier.size(20.dp).offset(x = offset.dp).clip(CircleShape).background(SurfaceContainerLowest).shadow(1.dp, CircleShape)
            )
        }
    }
}

@Composable
private fun ZeroCloudBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerHigh.copy(alpha = 0.6f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Shield, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Your documents never leave this device. Zero cloud uploads, zero latency.", fontSize = 12.sp, color = OnSurfaceVariant, lineHeight = 16.sp)
    }
}

@Composable
private fun CompressBottomBar(isProcessing: Boolean, onCompressClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceContainerLowest.copy(alpha = 0.95f))
            .shadow(elevation = 16.dp, spotColor = OnSurface.copy(alpha = 0.06f))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = PrimaryContainer.copy(alpha = 0.35f))
                .clip(RoundedCornerShape(12.dp))
                .background(PrimaryContainer)
                .clickable(enabled = !isProcessing) { onCompressClick() },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isProcessing) {
                    CircularProgressIndicator(color = OnPrimaryContainer, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Compressing...", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnPrimaryContainer)
                } else {
                    Icon(Icons.Outlined.Compress, contentDescription = null, tint = OnPrimaryContainer, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Compress Document", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnPrimaryContainer)
                }
            }
        }
    }
}