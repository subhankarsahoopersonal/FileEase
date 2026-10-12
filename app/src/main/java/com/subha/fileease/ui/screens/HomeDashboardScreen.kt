package com.subha.fileease.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.Environment
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

// Exact colors from the Stitch HTML file
val BgColor = Color(0xFFF8FAFC)
val SurfaceColor = Color(0xFFFFFFFF)
val PrimaryBlue = Color(0xFF0066FF)
val TextPrimary = Color(0xFF0F172A)
val TextSecondary = Color(0xFF64748B)
val TextTertiary = Color(0xFF94A3B8)
val Slate100 = Color(0xFFF1F5F9)
val Blue50 = Color(0xFFEFF6FF)
val Red500 = Color(0xFFEF4444)
val Red50 = Color(0xFFFEF2F2)
val Emerald500 = Color(0xFF10B981)
val Emerald50 = Color(0xFFECFDF5)

@Composable
fun HomeDashboardScreen(
    // Added navigation callbacks to make the dashboard functional
    onMergeClick: () -> Unit = {},
    onSplitClick: () -> Unit = {},
    onCompressClick: () -> Unit = {},
    onImagesToPdfClick: () -> Unit = {},
    onViewAllClick: () -> Unit = {}
) {
    Scaffold(
        topBar = { DashboardTopBar() },
        containerColor = BgColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            GreetingSection()

            Spacer(modifier = Modifier.height(24.dp))
            QuickActionsSection(
                onMergeClick = onMergeClick,
                onSplitClick = onSplitClick,
                onCompressClick = onCompressClick,
                onImagesToPdfClick = onImagesToPdfClick,
                onViewAllClick = onViewAllClick
            )

            Spacer(modifier = Modifier.height(24.dp))
            DeviceStorageSection()

            Spacer(modifier = Modifier.height(24.dp))
            RecentActivitySection()

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun DashboardTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceColor.copy(alpha = 0.9f))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo & App Name
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PrimaryBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = "Logo",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "FileEase",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        // Notifications & Avatar
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    tint = TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
                // Red Dot Indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 6.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Red500)
                        .border(2.dp, SurfaceColor, CircleShape)
                )
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Slate100)
                    .border(2.dp, SurfaceColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = "User Avatar",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun GreetingSection() {
    Column {
        Text(
            text = "Good morning,\nSubha",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            lineHeight = 28.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "What would you like to do today?",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
        )
    }
}

@Composable
fun QuickActionsSection(
    onMergeClick: () -> Unit,
    onSplitClick: () -> Unit,
    onCompressClick: () -> Unit,
    onImagesToPdfClick: () -> Unit,
    onViewAllClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quick Actions",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            // Added clickable to "View all"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onViewAllClick() }
                    .padding(4.dp)
            ) {
                Text(
                    text = "View all",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryBlue
                )
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = "View all",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Images to PDF (Active) - Added onClick callback
            Column(
                modifier = Modifier
                    .width(84.dp)
                    .height(92.dp)
                    .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = PrimaryBlue)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PrimaryBlue)
                    .clickable { onImagesToPdfClick() }
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Image, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("Images to PDF", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1)
            }

            // 2. Merge PDF - Passed the new onClick callback
            QuickActionCard(icon = Icons.Outlined.Layers, title = "Merge PDF", onClick = onMergeClick)
            // 3. Split PDF
            QuickActionCard(icon = Icons.Outlined.ContentCut, title = "Split PDF", onClick = onSplitClick)
            // 4. Compress PDF
            QuickActionCard(icon = Icons.Outlined.ZoomInMap, title = "Compress", onClick = onCompressClick)
        }
    }
}

// Added onClick parameter to individual cards
@Composable
fun QuickActionCard(icon: ImageVector, title: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(84.dp)
            .height(92.dp)
            .border(1.dp, Slate100, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceColor)
            .clickable { onClick() }
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Blue50),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B), maxLines = 1)
    }
}

@Composable
fun DeviceStorageSection() {
    // State variables to hold the real device data
    var totalGb by remember { mutableStateOf(128f) } // Default fallback
    var usedGb by remember { mutableStateOf(0f) }
    var docsGb by remember { mutableStateOf(0f) }
    var othersGb by remember { mutableStateOf(0f) }
    var progress by remember { mutableStateOf(0f) }

    // Fetch real storage stats asynchronously when the UI loads
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                // StatFs reads the actual file system blocks on Android
                val stat = StatFs(Environment.getDataDirectory().path)
                val totalBytes = stat.totalBytes
                val availableBytes = stat.availableBytes
                val usedBytes = totalBytes - availableBytes

                // Using 1 Billion bytes = 1 GB (OEM marketing standard for storage)
                val gigabyte = 1_000_000_000f
                val tGb = totalBytes / gigabyte
                val uGb = usedBytes / gigabyte

                totalGb = tGb
                usedGb = uGb
                progress = if (tGb > 0) uGb / tGb else 0f

                // Note: Querying the exact size of specific file types (PDFs, Images)
                // across the entire OS requires MANAGE_EXTERNAL_STORAGE permissions.
                // Until we implement permissions later, we proportionally split the actual used space.
                docsGb = uGb * 0.58f
                othersGb = uGb * 0.42f
            } catch (e: Exception) {
                // Keep default state on error
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1E60F2), Color(0xFF3B82F6), Color(0xFF60A5FA))))
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Memory, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Device Storage", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.9f))
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text("${"%.1f".format(usedGb)} GB", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("of ${"%.0f".format(totalGb)} GB used", fontSize = 14.sp, fontWeight = FontWeight.Normal, color = Color.White.copy(alpha = 0.8f))
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dynamic Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        // Constrain fraction between 0.01 and 1.0 to prevent Compose rendering crashes on 0 width
                        .fillMaxWidth(progress.coerceIn(0.01f, 1f))
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Legend
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Documents ${"%.1f".format(docsGb)} GB", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.9f))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.5f)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Others ${"%.1f".format(othersGb)} GB", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.9f))
                }
            }
        }
    }
}

@Composable
fun RecentActivitySection() {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Activity",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "View all",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryBlue
                )
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = "View all",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ActivityItem(
                icon = Icons.Outlined.PictureAsPdf,
                iconBgColor = Red50,
                iconColor = Red500,
                title = "document_scan.pdf",
                subtitle = "Edited • 2 min ago",
                size = "4.2 MB"
            )
            ActivityItem(
                icon = Icons.Outlined.Image,
                iconBgColor = Emerald50,
                iconColor = Emerald500,
                title = "invoice.jpg",
                subtitle = "Converted • 18 min ago",
                size = "1.1 MB"
            )
            ActivityItem(
                icon = Icons.Outlined.Description,
                iconBgColor = Blue50,
                iconColor = PrimaryBlue,
                title = "project_brief.docx",
                subtitle = "Compressed • Yesterday",
                size = "890 KB"
            )
        }
    }
}

@Composable
fun ActivityItem(
    icon: ImageVector,
    iconBgColor: Color,
    iconColor: Color,
    title: String,
    subtitle: String,
    size: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Slate100, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceColor)
            .clickable { }
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextTertiary
                )
            }
        }
        Text(
            text = size,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}