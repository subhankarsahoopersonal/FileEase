package com.subha.fileease.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

private val PrimaryLight = Color(0xFFEAF2FF)
private val Background = Color(0xFFFFFFFF)
private val CardBorder = Color(0xFFF1F5F9)

@Composable
fun StudioScreen() {
    Scaffold(
        containerColor = Background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // Header Section
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Studio",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Advanced visual file modifications",
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = TextSecondary,
                letterSpacing = (-0.2).sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Section 1: Conversion Tools
            SectionHeader(title = "Conversion Tools")
            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StudioToolCard(
                    modifier = Modifier.weight(1f),
                    title = "Images to PDF",
                    icon = Icons.Outlined.Image
                )
                StudioToolCard(
                    modifier = Modifier.weight(1f),
                    title = "PDF to Images",
                    icon = Icons.Outlined.CollectionsBookmark
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StudioToolCard(
                    modifier = Modifier.weight(1f),
                    title = "JPG to PNG",
                    icon = Icons.Outlined.SwapHoriz
                )
                StudioToolCard(
                    modifier = Modifier.weight(1f),
                    title = "Text to PDF",
                    iconText = "A"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 2: Image Tools
            SectionHeader(title = "Image Tools")
            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StudioToolCard(
                    modifier = Modifier.weight(1f),
                    title = "Crop Image",
                    icon = Icons.Outlined.Crop
                )
                StudioToolCard(
                    modifier = Modifier.weight(1f),
                    title = "Resize Image",
                    icon = Icons.Outlined.OpenInFull
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StudioToolCard(
                    modifier = Modifier.weight(1f),
                    title = "Compress Image",
                    icon = Icons.Outlined.Compress
                )
                StudioToolCard(
                    modifier = Modifier.weight(1f),
                    title = "Rotate Image",
                    icon = Icons.Outlined.RotateRight
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
                .background(PrimaryBlue)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            letterSpacing = (-0.2).sp
        )
    }
}

@Composable
fun StudioToolCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector? = null,
    iconText: String? = null
) {
    Column(
        modifier = modifier
            .heightIn(min = 108.dp)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.03f),
                spotColor = Color.Black.copy(alpha = 0.03f)
            )
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Background)
            .clickable { }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(PrimaryLight),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(24.dp)
                )
            } else if (iconText != null) {
                Text(
                    text = iconText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            letterSpacing = (-0.2).sp,
            lineHeight = 16.sp
        )
    }
}