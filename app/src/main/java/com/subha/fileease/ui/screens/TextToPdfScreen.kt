package com.subha.fileease.ui.screens

import android.os.Vibrator
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Exact Tailwind Color Tokens from Mockup
private val Background = Color(0xFFFAF8FF)
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
private val Outline = Color(0xFF727687)
private val OutlineVariant = Color(0xFFC2C6D8)

@Composable
fun TextToPdfScreen(onNavigateBack: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Mode State
    var inputMode by remember { mutableStateOf("paste") } // "paste" or "import"

    // Editor State
    var textContent by remember { mutableStateOf("# Project Executive Summary\nDate: Oct 24, 2024\nLead: FileEase Engineering Core\n\n1. Project Scope & Milestones\n- Native client-side document processing engine\n- Sub-50ms PDF assembly via Vector Streams\n- Complete offline privacy preservation architecture\n\n2. Key Deliverables\nReady-to-sign formatted brief exported for internal distribution.") }
    val charCount = textContent.length
    val wordCount = if (textContent.isBlank()) 0 else textContent.split("\\s+".toRegex()).size
    val lineCount = textContent.lines().size

    // Typography State
    var fontFamily by remember { mutableStateOf("Sans-Serif") }
    var fontSize by remember { mutableStateOf("12pt") }
    var lineSpacing by remember { mutableStateOf("1.5x") }

    // Formatting State
    var pageNumbersEnabled by remember { mutableStateOf(true) }
    var outputFileName by remember { mutableStateOf("Executive_Summary.pdf") }
    var authorName by remember { mutableStateOf("FileEase User") }

    // Export State
    var isProcessing by remember { mutableStateOf(false) }
    var isComplete by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Background,
        topBar = { TextToPdfTopBar(onNavigateBack) },
        bottomBar = {
            TextToPdfBottomBar(
                isProcessing = isProcessing,
                isComplete = isComplete,
                onConvert = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    coroutineScope.launch {
                        isProcessing = true
                        delay(700)
                        isProcessing = false
                        isComplete = true
                        delay(2000)
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

            // Subtitle Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(12.dp))
                    .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Convert plain text, notes, and markdown into formatted PDF files", fontSize = 13.sp, color = OnSurface, lineHeight = 18.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Bolt, contentDescription = null, tint = Primary, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Zero cloud latency • Instant client-side render", fontSize = 10.sp, color = Primary, fontWeight = FontWeight.Medium)
                    }
                }
                Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceContainer), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Input Mode Segment Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(12.dp))
                    .background(SurfaceContainer, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                ModeTab(modifier = Modifier.weight(1f), title = "Type / Paste Text", icon = Icons.Outlined.EditNote, isSelected = inputMode == "paste") { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); inputMode = "paste" }
                ModeTab(modifier = Modifier.weight(1f), title = "Import .TXT / .MD", icon = Icons.Outlined.UploadFile, isSelected = inputMode == "import") { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); inputMode = "import" }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Import Dropzone
            AnimatedVisibility(visible = inputMode == "import") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(1.dp, RoundedCornerShape(12.dp))
                        .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(SurfaceContainerHigh), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.CloudUpload, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Drag file here or browse", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                    Text("Supports UTF-8 .txt, .markdown, .md notes", fontSize = 12.sp, color = OnSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(PrimaryContainer).clickable { haptics.performHapticFeedback(HapticFeedbackType.LongPress) }.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.FolderOpen, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Select from Device", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = OnPrimary)
                    }
                }
            }

            // Editor Container
            AnimatedVisibility(visible = inputMode == "paste") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(1.dp, RoundedCornerShape(12.dp))
                        .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(PrimaryContainer))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Markdown / Text Source", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                        }
                        Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(SurfaceContainerHigh).padding(horizontal = 8.dp, vertical = 2.dp)) {
                            Text("$wordCount words • $charCount chars", fontSize = 10.sp, color = OnSurfaceVariant, letterSpacing = 0.5.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Text Area with Line Numbers
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceContainerLow)
                            .padding(12.dp)
                    ) {
                        Column(modifier = Modifier.width(20.dp), horizontalAlignment = Alignment.End) {
                            for (i in 1..maxOf(8, lineCount)) {
                                Text("$i", fontSize = 10.sp, color = Outline, modifier = Modifier.padding(bottom = 5.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        BasicTextField(
                            value = textContent,
                            onValueChange = { textContent = it },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                            textStyle = TextStyle(fontSize = 12.sp, color = OnSurface, lineHeight = 18.sp),
                            cursorBrush = SolidColor(Primary)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Primary, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Auto-synced to previewer", fontSize = 10.sp, color = OnSurfaceVariant)
                        }
                        Row(
                            modifier = Modifier.clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                textContent = "# Project Executive Summary\nDate: Oct 24, 2024\nLead: FileEase Engineering Core\n\n1. Project Scope & Milestones\n- Native client-side document processing engine\n- Sub-50ms PDF assembly via Vector Streams\n- Complete offline privacy preservation architecture\n\n2. Key Deliverables\nReady-to-sign formatted brief exported for internal distribution."
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Refresh, contentDescription = null, tint = Secondary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset sample", fontSize = 10.sp, color = Secondary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Typography & Layout Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(12.dp))
                    .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.FormatPaint, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Typography & Layout", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                    }
                    Text("PDF Style Sheet", fontSize = 10.sp, color = OnSurfaceVariant)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Font Family", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillButton(modifier = Modifier.weight(1f), text = "Sans-Serif", isSelected = fontFamily == "Sans-Serif") { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); fontFamily = "Sans-Serif" }
                    PillButton(modifier = Modifier.weight(1f), text = "Serif", isSelected = fontFamily == "Serif") { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); fontFamily = "Serif" }
                    PillButton(modifier = Modifier.weight(1f), text = "Monospace", isSelected = fontFamily == "Monospace") { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); fontFamily = "Monospace" }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Font Size", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant)
                    Text(fontSize, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PillButtonSmall(modifier = Modifier.weight(1f), text = "10pt", isSelected = fontSize == "10pt") { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); fontSize = "10pt" }
                    PillButtonSmall(modifier = Modifier.weight(1f), text = "12pt", isSelected = fontSize == "12pt") { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); fontSize = "12pt" }
                    PillButtonSmall(modifier = Modifier.weight(1f), text = "14pt", isSelected = fontSize == "14pt") { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); fontSize = "14pt" }
                    PillButtonSmall(modifier = Modifier.weight(1f), text = "16pt", isSelected = fontSize == "16pt") { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); fontSize = "16pt" }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Line Spacing", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillButtonSmall(modifier = Modifier.weight(1f), text = "1.15x", isSelected = lineSpacing == "1.15x") { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); lineSpacing = "1.15x" }
                    PillButtonSmall(modifier = Modifier.weight(1f), text = "1.5x (Rec.)", isSelected = lineSpacing == "1.5x") { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); lineSpacing = "1.5x" }
                    PillButtonSmall(modifier = Modifier.weight(1f), text = "2.0x", isSelected = lineSpacing == "2.0x") { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); lineSpacing = "2.0x" }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(SurfaceContainerLow).padding(8.dp)) {
                        Text("Page Format", fontSize = 10.sp, color = OnSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("A4 Standard", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                            Icon(Icons.Outlined.Description, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                        }
                    }
                    Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(SurfaceContainerLow).padding(8.dp)) {
                        Text("Margin", fontSize = 10.sp, color = OnSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("20mm Normal", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                            Icon(Icons.Outlined.SpaceBar, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceContainerLow).padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.FormatListNumbered, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Page numbers in footer", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                    }
                    CustomSwitch(checked = pageNumbersEnabled, onCheckedChange = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); pageNumbersEnabled = it })
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Metadata Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(12.dp))
                    .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                    Icon(Icons.Outlined.Badge, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Document Metadata", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                }

                Text("Output File Name", fontSize = 10.sp, color = OnSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceContainerLow).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Description, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = outputFileName,
                        onValueChange = { outputFileName = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(fontSize = 12.sp, color = OnSurface, fontWeight = FontWeight.Medium),
                        cursorBrush = SolidColor(Primary)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Author / Signee", fontSize = 10.sp, color = OnSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceContainerLow).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Person, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = authorName,
                        onValueChange = { authorName = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(fontSize = 12.sp, color = OnSurface),
                        cursorBrush = SolidColor(Primary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ISO Notice
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainerHigh.copy(alpha = 0.6f))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceContainerLowest), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Verified, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("ISO 32000-1 Standard", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                        Text("Embedded font metrics • Crisp print ready", fontSize = 10.sp, color = OnSurfaceVariant)
                    }
                }
                Text("100% On-device", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Primary)
            }

            Spacer(modifier = Modifier.height(140.dp))
        }
    }
}

@Composable
private fun TextToPdfTopBar(onNavigateBack: () -> Unit) {
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
                Text("Text To PDF", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OnSurface, letterSpacing = (-0.01).sp)
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
private fun ModeTab(modifier: Modifier, title: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) SurfaceContainerLowest else Color.Transparent
    val contentColor = if (isSelected) Primary else OnSurfaceVariant
    val elevation = if (isSelected) 1.dp else 0.dp

    Row(
        modifier = modifier
            .shadow(elevation, RoundedCornerShape(8.dp))
            .background(bgColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(title, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, color = contentColor)
    }
}

@Composable
private fun PillButton(modifier: Modifier, text: String, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) Primary else SurfaceContainerHigh
    val textColor = if (isSelected) OnPrimary else OnSurface

    Box(
        modifier = modifier
            .shadow(if(isSelected) 1.dp else 0.dp, RoundedCornerShape(8.dp))
            .background(bgColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 11.sp, fontWeight = if(isSelected) FontWeight.SemiBold else FontWeight.Normal, color = textColor)
    }
}

@Composable
private fun PillButtonSmall(modifier: Modifier, text: String, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) Primary else SurfaceContainerHigh
    val textColor = if (isSelected) OnPrimary else OnSurface

    Box(
        modifier = modifier
            .shadow(if(isSelected) 1.dp else 0.dp, RoundedCornerShape(8.dp))
            .background(bgColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 10.sp, fontWeight = if(isSelected) FontWeight.SemiBold else FontWeight.Normal, color = textColor)
    }
}

@Composable
private fun CustomSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val bg by animateColorAsState(if (checked) PrimaryContainer else OutlineVariant, label = "switchBg")
    val offset by animateDpAsState(if (checked) 16.dp else 0.dp, label = "switchOffset")

    Box(
        modifier = Modifier
            .width(40.dp)
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
private fun TextToPdfBottomBar(isProcessing: Boolean, isComplete: Boolean, onConvert: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface.copy(alpha = 0.90f))
            .shadow(elevation = 24.dp, spotColor = OnSurface.copy(alpha = 0.06f))
            .navigationBarsPadding()
    ) {
        // FIXED: Using individual padding sides to prevent compile error
        Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Primary))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Targeting ~24 KB output size", fontSize = 10.sp, color = OnSurfaceVariant)
                }
                Text("1 Page document preview", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Primary)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .shadow(4.dp, RoundedCornerShape(12.dp), spotColor = PrimaryContainer.copy(alpha = 0.5f))
                    .clip(RoundedCornerShape(12.dp))
                    .background(PrimaryContainer)
                    .clickable(enabled = !isProcessing && !isComplete) { onConvert() },
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = Pair(isProcessing, isComplete),
                    label = "buttonContent"
                ) { states ->
                    val (processing, complete) = states
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (processing) {
                            CircularProgressIndicator(color = OnPrimaryContainer, modifier = Modifier.size(20.dp), strokeWidth = 2.5.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Compiling Vector PDF...", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OnPrimaryContainer)
                        } else if (complete) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = OnPrimaryContainer, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PDF Ready • Saved", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OnPrimaryContainer)
                        } else {
                            Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, tint = OnPrimaryContainer, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate PDF from Text", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OnPrimaryContainer)
                        }
                    }
                }
            }
        }
    }
}