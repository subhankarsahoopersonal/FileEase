package com.subha.fileease.ui.screens

import androidx.compose.foundation.background
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

data class PdfTool(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

@Composable
fun PdfToolsScreen(
    onMergeClick: () -> Unit,
    onSplitClick: () -> Unit,
    onCompressClick: () -> Unit,
    onRotateClick: () -> Unit,
    onPasswordClick: () -> Unit,
    onExtractClick: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val tools = remember {
        listOf(
            PdfTool("Merge PDF", "Combine multiple PDFs into one", Icons.Outlined.Layers),
            PdfTool("Split PDF", "Separate pages into individual files", Icons.Outlined.ContentCut),
            PdfTool("Compress PDF", "Reduce file size without losing quality", Icons.Outlined.Compress),
            PdfTool("Rotate PDF", "Fix orientation of your document", Icons.Outlined.RotateRight),
            PdfTool("Password", "Encrypt and protect your files", Icons.Outlined.Lock),
            PdfTool("Extract Pages", "Get specific pages from a PDF", Icons.Outlined.FileCopy)
        )
    }

    Scaffold(
        containerColor = BgColor,
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Box(
                    modifier = Modifier
                        .padding(bottom = 80.dp, start = 24.dp, end = 24.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(TextPrimary)
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
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = LocalTextStyle.current.fontFamily
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgColor)
                .padding(innerPadding)
                .statusBarsPadding() // Safely pushes the header below the system icons
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "PDF Tools",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    fontFamily = LocalTextStyle.current.fontFamily
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Manipulate documents offline",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    fontFamily = LocalTextStyle.current.fontFamily
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 130.dp)
            ) {
                items(tools, key = { it.title }) { tool ->
                    PdfToolCard(
                        tool = tool,
                        onClick = {
                            when (tool.title) {
                                "Merge PDF" -> onMergeClick()
                                "Split PDF" -> onSplitClick()
                                "Compress PDF" -> onCompressClick()
                                "Rotate PDF" -> onRotateClick()
                                "Password" -> onPasswordClick()
                                "Extract Pages" -> onExtractClick()
                                else -> coroutineScope.launch { snackbarHostState.showSnackbar("${tool.title} selected") }
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
            .heightIn(min = 152.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.03f),
                spotColor = Color.Black.copy(alpha = 0.06f)
            )
            .background(SurfaceColor, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .bounceClick { onClick() }
            .padding(16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Blue50),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = tool.title,
                tint = PrimaryBlue,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = tool.title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            lineHeight = 18.sp,
            fontFamily = LocalTextStyle.current.fontFamily
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = tool.subtitle,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = TextSecondary,
            lineHeight = 16.sp,
            fontFamily = LocalTextStyle.current.fontFamily
        )
    }
}