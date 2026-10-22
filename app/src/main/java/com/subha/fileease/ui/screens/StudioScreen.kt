package com.subha.fileease.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Surface = Color(0xFFFAF8FF)
private val SurfaceContainerLowest = Color(0xFFFFFFFF)
private val SurfaceContainerLow = Color(0xFFF2F3FF)
private val OnSurface = Color(0xFF131B2E)
private val OnSurfaceVariant = Color(0xFF424656)
private val Primary = Color(0xFF0050CB)

@Composable
fun StudioScreen(
    onImagesToPdfClick: () -> Unit = {},
    onPdfToImagesClick: () -> Unit = {},
    onJpgToPngClick: () -> Unit = {},
    onTextToPdfClick: () -> Unit = {},
    onCropImageClick: () -> Unit = {},
    onResizeImageClick: () -> Unit = {},
    onCompressImageClick: () -> Unit = {},
    onRotateImageClick: () -> Unit = {} // <-- ADDED ROTATE TRIGGER
) {
    Scaffold(
        containerColor = Surface,
        contentWindowInsets = WindowInsets(0.dp)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Studio",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = OnSurface,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Advanced visual file modifications",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = OnSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            SectionHeader(title = "Conversion Tools")
            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StudioToolCard(
                    modifier = Modifier.weight(1f),
                    title = "Images to PDF",
                    icon = Icons.Outlined.Image,
                    onClick = onImagesToPdfClick
                )
                StudioToolCard(
                    modifier = Modifier.weight(1f),
                    title = "PDF to Images",
                    icon = Icons.Outlined.CollectionsBookmark,
                    onClick = onPdfToImagesClick
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StudioToolCard(
                    modifier = Modifier.weight(1f),
                    title = "JPG to PNG",
                    icon = Icons.Outlined.SwapHoriz,
                    onClick = onJpgToPngClick
                )
                StudioToolCard(
                    modifier = Modifier.weight(1f),
                    title = "Text to PDF",
                    iconText = "A",
                    onClick = onTextToPdfClick
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SectionHeader(title = "Image Tools")
            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StudioToolCard(
                    modifier = Modifier.weight(1f),
                    title = "Crop Image",
                    icon = Icons.Outlined.Crop,
                    onClick = onCropImageClick
                )
                StudioToolCard(
                    modifier = Modifier.weight(1f),
                    title = "Resize Image",
                    icon = Icons.Outlined.OpenInFull,
                    onClick = onResizeImageClick
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StudioToolCard(
                    modifier = Modifier.weight(1f),
                    title = "Compress Image",
                    icon = Icons.Outlined.Compress,
                    onClick = onCompressImageClick
                )
                StudioToolCard(
                    modifier = Modifier.weight(1f),
                    title = "Rotate Image",
                    icon = Icons.Outlined.RotateRight,
                    onClick = onRotateImageClick // <-- CONNECTED TO CARD
                )
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(50))
                .background(Primary)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = OnSurface
        )
    }
}

@Composable
fun StudioToolCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector? = null,
    iconText: String? = null,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .heightIn(min = 108.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.08f))
            .background(SurfaceContainerLowest, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceContainerLow),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Primary,
                    modifier = Modifier.size(24.dp)
                )
            } else if (iconText != null) {
                Text(
                    text = iconText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = OnSurface,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
    }
}