package com.subha.fileease.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Exact Tailwind Color Tokens
private val Surface = Color(0xFFFAF8FF)
private val SurfaceContainerLowest = Color(0xFFFFFFFF)
private val SurfaceContainerLow = Color(0xFFF2F3FF)
private val SurfaceContainer = Color(0xFFEAEDFF)
private val SurfaceContainerHigh = Color(0xFFE2E7FF)
private val SurfaceVariant = Color(0xFFDAE2FD)
private val OnSurface = Color(0xFF131B2E)
private val OnSurfaceVariant = Color(0xFF424656)
private val Primary = Color(0xFF0050CB)
private val OnPrimary = Color(0xFFFFFFFF)
private val PrimaryContainer = Color(0xFF0066FF)
private val OnPrimaryContainer = Color(0xFFF8F7FF)
private val PrimaryFixed = Color(0xFFDAE1FF)
private val Error = Color(0xFFBA1A1A)
private val ErrorContainer = Color(0xFFFFDAD6)
private val Outline = Color(0xFF727687)
private val OutlineVariant = Color(0xFFC2C6D8)

@Composable
fun PdfToImagesScreen(onNavigateBack: () -> Unit) {
    // State Management
    var selectedScope by remember { mutableStateOf("All Pages (18)") }
    var customRange by remember { mutableStateOf("1-6, 12, 18") }
    val selectedPages = remember { mutableStateListOf(1, 2, 3, 4, 5, 6) }

    var selectedFormat by remember { mutableStateOf("PNG") }
    var selectedDpi by remember { mutableStateOf(300) }

    var saveAsZip by remember { mutableStateOf(true) }
    var saveToGallery by remember { mutableStateOf(false) }

    val dummyPages = listOf(
        Pair(1, "Cover"), Pair(2, "Index"), Pair(3, "Specs"),
        Pair(4, "Diagram"), Pair(5, "Tiers"), Pair(6, "Back")
    )

    Scaffold(
        containerColor = Surface,
        topBar = { PdfToImagesTopBar(onNavigateBack) },
        bottomBar = {
            PdfToImagesBottomBar(
                selectedCount = selectedPages.size,
                format = selectedFormat,
                dpi = selectedDpi,
                onExtract = { /* Handle extraction */ }
            )
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Extract high-resolution image pages from PDF document",
                fontSize = 13.sp,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // PDF File Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(12.dp))
                    .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(ErrorContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, tint = Error, modifier = Modifier.size(26.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Product_Catalog_2024.pdf", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                Text("18 pages", fontSize = 11.sp, color = OnSurfaceVariant)
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(OutlineVariant))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("14.2 MB", fontSize = 11.sp, color = OnSurfaceVariant)
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(OutlineVariant))
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(PrimaryFixed).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                    Text("Ready", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                                }
                            }
                        }
                    }
                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(SurfaceContainer).clickable { }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text("Change", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.VerifiedUser, contentDescription = null, tint = Primary, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Local Engine • 0 KB network used", fontSize = 10.sp, color = OnSurfaceVariant)
                    }
                    Text("RGB · sRGB Profile", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Extraction Scope Section
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Extraction Scope", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                Text("Select All (18)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PrimaryContainer, modifier = Modifier.clickable {
                    selectedScope = "All Pages (18)"
                    selectedPages.clear()
                    selectedPages.addAll(1..18)
                })
            }
            Spacer(modifier = Modifier.height(8.dp))

            SegmentControlRow(
                options = listOf("All Pages (18)", "Page Range", "Selected Only"),
                selected = selectedScope,
                onSelect = { selectedScope = it }
            )

            AnimatedVisibility(visible = selectedScope == "Page Range") {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp).shadow(1.dp, RoundedCornerShape(12.dp)).background(SurfaceContainerLowest, RoundedCornerShape(12.dp)).padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Specify custom pages", fontSize = 11.sp, color = OnSurfaceVariant)
                        Text("e.g. 1-5, 8, 12", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceContainerLow).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.EditNote, contentDescription = null, tint = Outline, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = customRange,
                            onValueChange = { customRange = it },
                            textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Visual Page Selector
            Column(modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(12.dp)).background(SurfaceContainerLowest, RoundedCornerShape(12.dp)).padding(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Visual Page Selector", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                    Text("${selectedPages.size} of 18 selected", fontSize = 10.sp, color = OnSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(12.dp))

                dummyPages.chunked(3).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { page ->
                            val isSelected = selectedPages.contains(page.first)
                            Column(
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(SurfaceContainerLow).clickable {
                                    if (isSelected) selectedPages.remove(page.first) else selectedPages.add(page.first)
                                }.padding(6.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().aspectRatio(0.75f).clip(RoundedCornerShape(4.dp)).background(SurfaceContainer)) {
                                    Icon(Icons.Outlined.Description, contentDescription = null, tint = OutlineVariant, modifier = Modifier.align(Alignment.Center).size(24.dp))
                                    if (isSelected) {
                                        Box(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(20.dp).clip(CircleShape).background(PrimaryContainer).shadow(1.dp), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Filled.Check, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Page ${page.first}", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                                    Text(page.second, fontSize = 10.sp, color = Outline)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceContainerHigh.copy(alpha = 0.6f)).clickable { }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Show all 18 thumbnail previews", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Outlined.ExpandMore, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Export Image Format
            Text("Export Image Format", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface, modifier = Modifier.padding(bottom = 8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormatCard(modifier = Modifier.weight(1f), format = "PNG", subtitle = "Lossless · Alpha", icon = Icons.Filled.Verified, isSelected = selectedFormat == "PNG") { selectedFormat = "PNG" }
                FormatCard(modifier = Modifier.weight(1f), format = "JPG", subtitle = "Compact size", icon = Icons.Outlined.Image, isSelected = selectedFormat == "JPG") { selectedFormat = "JPG" }
                FormatCard(modifier = Modifier.weight(1f), format = "WEBP", subtitle = "Web Optimized", icon = Icons.Outlined.Public, isSelected = selectedFormat == "WEBP") { selectedFormat = "WEBP" }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // DPI Settings
            Column(modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(12.dp)).background(SurfaceContainerLowest, RoundedCornerShape(12.dp)).padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.GridOn, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Resolution & DPI Preset", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                    }
                    Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(PrimaryFixed).padding(horizontal = 8.dp, vertical = 2.dp)) {
                        Text("$selectedDpi DPI", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                SegmentControlRow(
                    options = listOf("Screen (72)", "Standard (150)", "Ultra (300)"),
                    selected = when(selectedDpi) { 300 -> "Ultra (300)"; 150 -> "Standard (150)"; else -> "Screen (72)" },
                    onSelect = {
                        selectedDpi = when(it) {
                            "Ultra (300)" -> 300
                            "Standard (150)" -> 150
                            else -> 72
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                val dimSummary = when(selectedDpi) { 300 -> "2480 × 3508 px / page"; 150 -> "1240 × 1754 px / page"; else -> "595 × 842 px / page" }
                val dpiProgress by animateFloatAsState(targetValue = when(selectedDpi) { 300 -> 1f; 150 -> 0.6f; else -> 0.25f }, label = "progress")

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Target Render Dimensions", fontSize = 10.sp, color = OnSurfaceVariant)
                    Text(dimSummary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(SurfaceContainer)) {
                    Box(modifier = Modifier.fillMaxWidth(dpiProgress).fillMaxHeight().clip(RoundedCornerShape(50)).background(PrimaryContainer))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Packaging & Destination
            Text("Packaging & Destination", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface, modifier = Modifier.padding(bottom = 8.dp))
            Column(modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(12.dp)).background(SurfaceContainerLowest, RoundedCornerShape(12.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PackagingRow(
                    title = "Save as single ZIP archive",
                    subtitle = "Bundles all pages into catalog_images.zip",
                    icon = Icons.Outlined.FolderZip,
                    iconTint = Primary,
                    isChecked = saveAsZip,
                    onCheckedChange = { saveAsZip = it }
                )
                PackagingRow(
                    title = "Save individually to Photos",
                    subtitle = "Write extracted frames to gallery album",
                    icon = Icons.Outlined.PhotoLibrary,
                    iconTint = OnSurfaceVariant,
                    isChecked = saveToGallery,
                    onCheckedChange = { saveToGallery = it }
                )
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
private fun PdfToImagesTopBar(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface.copy(alpha = 0.90f))
            .shadow(elevation = 1.dp, spotColor = Color.Black.copy(alpha = 0.04f))
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).clickable { onNavigateBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = OnSurface)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text("PDF To Images", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = OnSurface, letterSpacing = (-0.01).sp)
    }
}

@Composable
private fun SegmentControlRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceContainer).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { option ->
            val isSelected = selected == option
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) SurfaceContainerLowest else Color.Transparent)
                    .shadow(if (isSelected) 1.dp else 0.dp, RoundedCornerShape(6.dp))
                    .clickable { onSelect(option) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    fontSize = 13.sp,
                    fontWeight = if(isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if(isSelected) PrimaryContainer else OnSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun FormatCard(modifier: Modifier, format: String, subtitle: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) PrimaryContainer else SurfaceContainerLowest
    val contentColor = if (isSelected) OnPrimaryContainer else OnSurface
    val subColor = if (isSelected) OnPrimaryContainer.copy(alpha = 0.9f) else OnSurfaceVariant
    val iconColor = if (isSelected) OnPrimaryContainer else Outline

    Column(
        modifier = modifier
            .shadow(1.dp, RoundedCornerShape(12.dp))
            .background(bgColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(format, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = contentColor)
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(subtitle, fontSize = 10.sp, color = subColor)
    }
}

@Composable
private fun PackagingRow(title: String, subtitle: String, icon: ImageVector, iconTint: Color, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onCheckedChange(!isChecked) }.padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceContainer), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                Text(subtitle, fontSize = 10.sp, color = OnSurfaceVariant)
            }
        }
        CustomSwitch(checked = isChecked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun CustomSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val bg by animateColorAsState(if (checked) PrimaryContainer else SurfaceVariant, label = "switchBg")
    val offset by animateDpAsState(if (checked) 20.dp else 0.dp, label = "switchOffset")

    Box(
        modifier = Modifier
            .width(48.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable { onCheckedChange(!checked) }
            .padding(4.dp),
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
private fun PdfToImagesBottomBar(selectedCount: Int, format: String, dpi: Int, onExtract: () -> Unit) {
    val estSize = when (format) {
        "PNG" -> if (dpi == 300) 1.4 else if (dpi == 150) 0.7 else 0.25
        "JPG" -> if (dpi == 300) 0.6 else if (dpi == 150) 0.35 else 0.15
        else -> if (dpi == 300) 0.45 else if (dpi == 150) 0.25 else 0.12
    }
    val totalMB = Math.max(1, (selectedCount * estSize).toInt())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceContainerLowest.copy(alpha = 0.95f))
            .shadow(elevation = 24.dp, spotColor = OnSurface.copy(alpha = 0.08f))
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Ready to extract", fontSize = 10.sp, color = OnSurfaceVariant)
                Text("$selectedCount images · ~$totalMB MB estimated", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Primary)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .shadow(if (selectedCount > 0) 4.dp else 0.dp, RoundedCornerShape(12.dp), spotColor = PrimaryContainer.copy(alpha = 0.5f))
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selectedCount > 0) PrimaryContainer else SurfaceContainerHigh)
                    .clickable(enabled = selectedCount > 0) { onExtract() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, tint = OnPrimaryContainer, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (selectedCount > 0) "Extract ${if(selectedCount < 18) "$selectedCount " else ""}Pages as $format" else "Select pages to extract",
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OnPrimaryContainer
                    )
                }
            }
        }
    }
}