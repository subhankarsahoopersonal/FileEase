package com.subha.fileease.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// Data class to hold tool info
data class PdfTool(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

@Composable
fun PdfToolsScreen(onMergeClick: () -> Unit, onSplitClick: () -> Unit, onCompressClick: () -> Unit, onRotateClick: () -> Unit, onPasswordClick: () -> Unit) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val tools = listOf(
        PdfTool("Merge PDF", "Combine multiple PDFs into one", Icons.Outlined.Layers),
        PdfTool("Split PDF", "Separate pages into individual files", Icons.Outlined.ContentCut),
        PdfTool("Compress PDF", "Reduce file size without losing quality", Icons.Outlined.Compress),
        PdfTool("Rotate PDF", "Fix orientation of your document", Icons.Outlined.RotateRight),
        PdfTool("Password", "Encrypt and protect your files", Icons.Outlined.Lock),
        PdfTool("Extract Pages", "Get specific pages from a PDF", Icons.Outlined.DriveFileMove)
    )

    Scaffold(
        containerColor = SurfaceColor,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                // Custom Snackbar matching your dark HTML toast
                Box(
                    modifier = Modifier
                        .padding(bottom = 80.dp, start = 24.dp, end = 24.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(TextPrimary) // Slate-900
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = data.visuals.message,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SurfaceColor)
        ) {
            // Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "PDF Tools",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Manipulate documents offline",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextSecondary
                )
            }

            // Grid Section
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 112.dp) // Extra bottom padding for scroll clearance
            ) {
                items(tools) { tool ->
                    PdfToolCard(
                        tool = tool,
                        onClick = {
                            if (tool.title == "Merge PDF") {
                                onMergeClick()
                            } else if (tool.title == "Split PDF") {
                                onSplitClick()
                            } else if (tool.title == "Compress PDF") {
                                onCompressClick()
                            } else if (tool.title == "Rotate PDF") {
                                onRotateClick()
                            } else if (tool.title == "Password") {
                                onPasswordClick()
                            } else {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(message = "${tool.title} selected")
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PdfToolCard(tool: PdfTool, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 148.dp)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.04f)
            )
            .border(1.dp, Slate100, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceColor)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        // Icon Box
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Blue50),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = tool.title,
                tint = PrimaryBlue,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Text Content
        Text(
            text = tool.title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = tool.subtitle,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            color = TextSecondary,
            lineHeight = 16.sp
        )
    }
}