package com.prsnl.ui.stats

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prsnl.core.util.DistanceUtils
import com.prsnl.storage.repository.DailyBarStat
import com.prsnl.storage.repository.TopFolderStat
import com.prsnl.storage.repository.TopNotebookStat
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritingStatsScreen(
    viewModel: WritingStatsViewModel,
    onBackClick: () -> Unit,
    onNavigateToWrapped: (String) -> Unit
) {
    val overallStats by viewModel.overallStats.collectAsState()
    val topNotebooks by viewModel.topNotebooks.collectAsState()
    val topFolders by viewModel.topFolders.collectAsState()
    val dailyBars by viewModel.recentDailyBars.collectAsState()
    var showEstimationInfoDialog by remember { mutableStateOf(false) }

    val currentYear = remember { LocalDate.now().year.toString() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Writing Insights",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D2B28)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF2D2B28)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.recalculateStats() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Recalculate Stats",
                            tint = Color(0xFFC88A4B)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFFFBF9F4))
            )
        },
        containerColor = Color(0xFFFBF9F4)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. "Year in Review" Wrapped Entry Card (Strict Warm Stationery Theme)
            if (overallStats.totalStrokes > 0) {
                item {
                    YearInReviewCard(
                        year = currentYear,
                        totalMeters = overallStats.totalMetersThisYear,
                        onClick = { onNavigateToWrapped(currentYear) }
                    )
                }
            }

            // 2. Hero Meters Counter with Landmark comparison (Centered)
            item {
                HeroMetersCard(
                    meters = overallStats.totalMetersAllTime,
                    totalStrokes = overallStats.totalStrokes,
                    onInfoClick = { showEstimationInfoDialog = true }
                )
            }

            // 3. Streak & Consistency Card
            item {
                StreakCard(
                    currentStreak = overallStats.currentStreakDays,
                    longestStreak = overallStats.longestStreakDays
                )
            }

            // 4. Daily Ink Length Bar Chart (Custom Canvas in warm brand colors)
            item {
                CanvasDailyBarChartCard(dailyBars = dailyBars)
            }

            // 5. Top Notebooks
            if (topNotebooks.isNotEmpty()) {
                item {
                    Text(
                        text = "Most Written Notebooks",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF2D2B28),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                itemsIndexed(topNotebooks) { index, nb ->
                    TopNotebookItem(rank = index + 1, item = nb)
                }
            }

            // 6. Top Folders
            if (topFolders.isNotEmpty()) {
                item {
                    Text(
                        text = "Top Writing Folders",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF2D2B28),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                itemsIndexed(topFolders) { index, folder ->
                    TopFolderItem(rank = index + 1, item = folder)
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showEstimationInfoDialog) {
        AlertDialog(
            onDismissRequest = { showEstimationInfoDialog = false },
            title = { Text("How Distance is Estimated", fontWeight = FontWeight.Bold, color = Color(0xFF2D2B28)) },
            text = {
                Text(
                    "Digital canvas strokes are recorded in document units. To provide a tangible real-world distance, " +
                    "we scale document width to standard A4 physical sheet dimensions (21.0 cm width). " +
                    "Erased strokes and undo operations subtract from your distance in real time.",
                    color = Color(0xFF5C5850)
                )
            },
            confirmButton = {
                TextButton(onClick = { showEstimationInfoDialog = false }) {
                    Text("Got it", fontWeight = FontWeight.Bold, color = Color(0xFFC88A4B))
                }
            },
            containerColor = Color(0xFFFAF8F5)
        )
    }
}

@Composable
private fun YearInReviewCard(
    year: String,
    totalMeters: Float,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2B28)),
        border = BorderStroke(1.5.dp, Color(0xFFC88A4B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFC88A4B),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$year YEAR IN REVIEW",
                            color = Color(0xFFC88A4B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Your Notes Wrapped",
                        color = Color(0xFFFAF8F5),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Explore your personalized handwritten journey",
                        color = Color(0xFFE2D7C5),
                        fontSize = 12.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Open Wrapped",
                    tint = Color(0xFFFAF8F5),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun HeroMetersCard(
    meters: Float,
    totalStrokes: Int,
    onInfoClick: () -> Unit
) {
    val animatedMeters by animateFloatAsState(
        targetValue = meters,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "HeroMetersAnimation"
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFEAE3D2)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "TOTAL DISTANCE WRITTEN",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF7A756D),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Estimated info",
                        tint = Color(0xFF8A8275),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Big Counter (Centered)
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = String.format("%.1f", animatedMeters),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF2D2B28)
                )
                Text(
                    text = " meters",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC88A4B),
                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                )
            }

            // Friendly comparison line (Centered)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF4EFE6),
                modifier = Modifier.padding(top = 10.dp)
            ) {
                Text(
                    text = DistanceUtils.getLandmarkComparison(meters),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2D2B28),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Secondary stat line (Centered)
            Text(
                text = "${NumberFormatShort(totalStrokes)} strokes committed with your stylus",
                fontSize = 12.sp,
                color = Color(0xFF7A756D),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StreakCard(
    currentStreak: Int,
    longestStreak: Int
) {
    val infiniteTransition = rememberInfiniteTransition(label = "FlamePulse")
    val flameScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FlameScale"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFEAE3D2)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFDF4E7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Streak flame",
                        tint = Color(0xFFE07A2B),
                        modifier = Modifier
                            .size(28.dp)
                            .scale(if (currentStreak > 0) flameScale else 1f)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = if (currentStreak > 0) "$currentStreak-Day Streak" else "No active streak",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D2B28)
                    )
                    Text(
                        text = if (currentStreak > 0) "Keep writing today to maintain momentum!" else "Write a stroke today to start your streak!",
                        fontSize = 12.sp,
                        color = Color(0xFF7A756D)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "BEST",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF8A8275)
                )
                Text(
                    text = "$longestStreak days",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D2B28)
                )
            }
        }
    }
}

@Composable
private fun CanvasDailyBarChartCard(dailyBars: List<DailyBarStat>) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFEAE3D2)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Last 7 Days Activity",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2D2B28)
            )
            Spacer(modifier = Modifier.height(16.dp))

            val maxMeters = (dailyBars.maxOfOrNull { it.metersWritten } ?: 1f).coerceAtLeast(0.5f)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val bottomLabelHeight = 24.dp.toPx()
                val chartHeight = canvasHeight - bottomLabelHeight

                val barCount = dailyBars.size.coerceAtLeast(1)
                val slotWidth = canvasWidth / barCount
                val barWidth = (slotWidth * 0.45f).coerceAtMost(32.dp.toPx())

                // Draw baseline
                drawLine(
                    color = Color(0xFFEAE3D2),
                    start = Offset(0f, chartHeight),
                    end = Offset(canvasWidth, chartHeight),
                    strokeWidth = 2f
                )

                dailyBars.forEachIndexed { i, stat ->
                    val centerX = slotWidth * i + slotWidth / 2f
                    val normalizedHeight = (stat.metersWritten / maxMeters).coerceIn(0f, 1f)
                    val barH = (normalizedHeight * (chartHeight - 12.dp.toPx())).coerceAtLeast(4f)
                    val barTop = chartHeight - barH

                    val isToday = i == dailyBars.size - 1
                    // Warm stationery palette: active Amber vs muted sand
                    val barColor = if (isToday) Color(0xFFC88A4B) else Color(0xFFD4C7B5)

                    // Rounded top bar
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(centerX - barWidth / 2f, barTop),
                        size = Size(barWidth, barH),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )

                    // Draw day-of-week label below baseline
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = if (isToday) Color(0xFF2D2B28).toArgb() else Color(0xFF8A8275).toArgb()
                            textSize = 11.dp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = isToday
                        }
                        drawText(
                            stat.dayOfWeekLabel,
                            centerX,
                            chartHeight + 18.dp.toPx(),
                            paint
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopNotebookItem(rank: Int, item: TopNotebookStat) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFEAE3D2)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Rank number
                Text(
                    text = "#$rank",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = Color(0xFF8A8275),
                    modifier = Modifier.width(28.dp)
                )

                // Color chip
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(item.coverColor))
                )
                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = item.notebookTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF2D2B28)
                    )
                    Text(
                        text = "in ${item.folderName} • ${item.strokeCount} strokes",
                        fontSize = 11.sp,
                        color = Color(0xFF7A756D)
                    )
                }
            }

            Text(
                text = "${String.format("%.1f", item.metersWritten)}m",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFFC88A4B)
            )
        }
    }
}

@Composable
private fun TopFolderItem(rank: Int, item: TopFolderStat) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFEAE3D2)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "#$rank",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = Color(0xFF8A8275),
                    modifier = Modifier.width(28.dp)
                )
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(item.folderColor))
                )
                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = item.folderName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF2D2B28)
                    )
                    Text(
                        text = "${item.strokeCount} total strokes",
                        fontSize = 11.sp,
                        color = Color(0xFF7A756D)
                    )
                }
            }

            Text(
                text = "${String.format("%.1f", item.metersWritten)}m",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFFC88A4B)
            )
        }
    }
}

private fun NumberFormatShort(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000f)
        count >= 1_000 -> String.format("%.1fk", count / 1_000f)
        else -> count.toString()
    }
}
