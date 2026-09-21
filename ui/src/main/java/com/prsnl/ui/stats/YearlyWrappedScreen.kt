package com.prsnl.ui.stats

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.prsnl.storage.repository.YearlyWrappedData
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun YearlyWrappedScreen(
    viewModel: WritingStatsViewModel,
    year: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val wrappedData by viewModel.yearlyWrapped.collectAsState()

    LaunchedEffect(year) {
        viewModel.loadYearlyWrapped(year)
    }

    if (wrappedData == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1E1C1A)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFFC88A4B))
        }
        return
    }

    val data = wrappedData!!
    val totalSlides = 6
    val pagerState = rememberPagerState(pageCount = { totalSlides })

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            when (pageIndex) {
                0 -> SlideDistance(data = data)
                1 -> SlideTopNotebook(data = data)
                2 -> SlideRhythm(data = data)
                3 -> SlideVolume(data = data)
                4 -> SlideStreak(data = data)
                5 -> SlideShareCard(data = data, onShareClick = { shareSummaryCard(context, data) })
            }
        }

        // Top Navigation Bar (Progress Dots + Close Button)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 20.dp, end = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Pager indicator bars
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(totalSlides) { i ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (i <= pagerState.currentPage) Color(0xFFC88A4B) else Color.White.copy(alpha = 0.25f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Wrapped",
                    tint = Color(0xFFFAF8F5),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Tap to advance hint at bottom
        if (pagerState.currentPage < totalSlides - 1) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 36.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Swipe to continue ›",
                    color = Color(0xFFE2D7C5).copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                )
            }
        }
    }
}

// SLIDE 0: Total Distance Written (Centered, Warm Espresso & Amber)
@Composable
private fun SlideDistance(data: YearlyWrappedData) {
    val animatedMeters by animateFloatAsState(
        targetValue = data.totalMetersThisYear,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "WrappedMetersAnim"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1C1A))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${data.year} IN NUMBERS",
                color = Color(0xFFC88A4B),
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Your stylus traveled",
                color = Color(0xFFFAF8F5),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = String.format("%.1f", animatedMeters),
                color = Color(0xFFC88A4B),
                fontSize = 68.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Text(
                text = "meters of ink",
                color = Color(0xFFFAF8F5),
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(28.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF2D2B28),
                border = BorderStroke(1.dp, Color(0xFF47433E)),
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Text(
                    text = data.comparisonLandmark,
                    color = Color(0xFFE2D7C5),
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

// SLIDE 1: Top Notebook & Top Folder (Centered, Warm Dark Saddle)
@Composable
private fun SlideTopNotebook(data: YearlyWrappedData) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF24201D))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "YOUR SANCTUARY OF IDEAS",
                color = Color(0xFFC88A4B),
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Your #1 Notebook was",
                color = Color(0xFFFAF8F5),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF8F5)),
                border = BorderStroke(1.5.dp, Color(0xFFC88A4B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(data.topNotebook?.coverColor ?: 0xFFC88A4B.toInt()))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = data.topNotebook?.notebookTitle ?: "My Notebook",
                        color = Color(0xFF2D2B28),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Folder: ${data.topNotebook?.folderName ?: "General"}",
                        color = Color(0xFF7A756D),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "${String.format("%.1f", data.topNotebook?.metersWritten ?: 0f)} meters written",
                        color = Color(0xFFC88A4B),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// SLIDE 2: Rhythm & Busiest Month (Centered)
@Composable
private fun SlideRhythm(data: YearlyWrappedData) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF221E1C))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "CREATIVE PEAK",
                color = Color(0xFFC88A4B),
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Your most prolific month was",
                color = Color(0xFFFAF8F5),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = data.busiestMonth ?: "Active Days",
                color = Color(0xFFC88A4B),
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${String.format("%.1f", data.busiestMonthMeters)} meters penned in this month alone",
                color = Color(0xFFE2D7C5),
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(28.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF2D2B28),
                border = BorderStroke(1.dp, Color(0xFF47433E))
            ) {
                Text(
                    text = "You wrote across ${data.activeDaysCount} separate days this year.",
                    color = Color(0xFFFAF8F5),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

// SLIDE 3: Total Pages + Total Strokes (Centered)
@Composable
private fun SlideVolume(data: YearlyWrappedData) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1C1A))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "VOLUME OF WORK",
                color = Color(0xFFC88A4B),
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(28.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2B28)),
                border = BorderStroke(1.dp, Color(0xFF47433E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${data.totalStrokesThisYear}",
                        color = Color(0xFFC88A4B),
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Stylus Strokes Drawn",
                        color = Color(0xFFFAF8F5),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2B28)),
                border = BorderStroke(1.dp, Color(0xFF47433E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${data.totalPagesThisYear}",
                        color = Color(0xFFFAF8F5),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Pages Created",
                        color = Color(0xFFE2D7C5),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// SLIDE 4: Consistency & Longest Streak (Centered)
@Composable
private fun SlideStreak(data: YearlyWrappedData) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF24201D))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = null,
                tint = Color(0xFFE07A2B),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "CONSISTENCY MATTERS",
                color = Color(0xFFC88A4B),
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Longest Writing Streak",
                color = Color(0xFFFAF8F5),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "${data.longestStreakDays} Days",
                color = Color(0xFFC88A4B),
                fontSize = 60.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (data.longestStreakDays >= 7) "A full week or more of uninterrupted handwritten focus!"
                       else "Every day written builds momentum. Great effort!",
                color = Color(0xFFE2D7C5),
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

// SLIDE 5: Final Shareable Summary Card (Centered, Warm Moleskine Stationery Theme)
@Composable
private fun SlideShareCard(
    data: YearlyWrappedData,
    onShareClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1C1A))
            .padding(horizontal = 24.dp, vertical = 64.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Main Shareable Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2B28)),
                border = BorderStroke(2.dp, Color(0xFFC88A4B)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "PRSNL ${data.year} WRAPPED",
                        color = Color(0xFFC88A4B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "${String.format("%.1f", data.totalMetersThisYear)}m Written",
                        color = Color(0xFFFAF8F5),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = data.comparisonLandmark,
                        color = Color(0xFFE2D7C5),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF22201D),
                        border = BorderStroke(1.dp, Color(0xFF47433E)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Top Notebook", color = Color(0xFF8A8275), fontSize = 12.sp)
                                Text(data.topNotebook?.notebookTitle ?: "—", color = Color(0xFFFAF8F5), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Longest Streak", color = Color(0xFF8A8275), fontSize = 12.sp)
                                Text("${data.longestStreakDays} days", color = Color(0xFFC88A4B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Strokes Drawn", color = Color(0xFF8A8275), fontSize = 12.sp)
                                Text("${data.totalStrokesThisYear}", color = Color(0xFFFAF8F5), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Share Button
            Button(
                onClick = onShareClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC88A4B)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color(0xFF2D2B28))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Share Your Wrapped",
                    color = Color(0xFF2D2B28),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Bitmap Generation & Sharing Utility (Strict brand warm palette)
private fun shareSummaryCard(context: Context, data: YearlyWrappedData) {
    try {
        val width = 800
        val height = 1100
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background dark stationery
        val bgPaint = Paint().apply {
            color = android.graphics.Color.rgb(30, 28, 26) // #1E1C1A
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Warm Card
        val cardPaint = Paint().apply {
            color = android.graphics.Color.rgb(45, 43, 40) // #2D2B28
            isAntiAlias = true
        }
        val cardRect = RectF(50f, 80f, width - 50f, height - 120f)
        canvas.drawRoundRect(cardRect, 40f, 40f, cardPaint)

        // Card Border in Amber
        val borderPaint = Paint().apply {
            color = android.graphics.Color.rgb(200, 138, 75) // #C88A4B
            style = Paint.Style.STROKE
            strokeWidth = 6f
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, 40f, 40f, borderPaint)

        // Title Paint
        val accentPaint = Paint().apply {
            color = android.graphics.Color.rgb(200, 138, 75) // #C88A4B
            textSize = 28f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("PRSNL ${data.year} WRAPPED", width / 2f, 160f, accentPaint)

        // Big Meters
        val bigMetersPaint = Paint().apply {
            color = android.graphics.Color.rgb(250, 248, 245) // #FAF8F5
            textSize = 72f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("${String.format("%.1f", data.totalMetersThisYear)}m Written", width / 2f, 270f, bigMetersPaint)

        // Comparison line
        val subPaint = Paint().apply {
            color = android.graphics.Color.rgb(226, 215, 197) // #E2D7C5
            textSize = 24f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(data.comparisonLandmark, width / 2f, 330f, subPaint)

        // Inner stats box
        val innerBoxPaint = Paint().apply {
            color = android.graphics.Color.rgb(34, 32, 29) // #22201D
            isAntiAlias = true
        }
        val innerBox = RectF(100f, 400f, width - 100f, 850f)
        canvas.drawRoundRect(innerBox, 30f, 30f, innerBoxPaint)

        val statKeyPaint = Paint().apply {
            color = android.graphics.Color.rgb(180, 172, 160)
            textSize = 26f
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
        }
        val statValPaint = Paint().apply {
            color = android.graphics.Color.rgb(250, 248, 245)
            textSize = 28f
            isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }

        // Row 1: Top Notebook
        canvas.drawText("Top Notebook", 140f, 480f, statKeyPaint)
        canvas.drawText(data.topNotebook?.notebookTitle ?: "—", width - 140f, 480f, statValPaint)

        // Row 2: Top Folder
        canvas.drawText("Top Folder", 140f, 570f, statKeyPaint)
        canvas.drawText(data.topFolder?.folderName ?: "—", width - 140f, 570f, statValPaint)

        // Row 3: Longest Streak
        canvas.drawText("Longest Streak", 140f, 660f, statKeyPaint)
        val flameValPaint = Paint(statValPaint).apply { color = android.graphics.Color.rgb(200, 138, 75) }
        canvas.drawText("${data.longestStreakDays} days", width - 140f, 660f, flameValPaint)

        // Row 4: Total Strokes
        canvas.drawText("Strokes Committed", 140f, 750f, statKeyPaint)
        canvas.drawText("${data.totalStrokesThisYear}", width - 140f, 750f, statValPaint)

        // Branding watermark
        val brandingPaint = Paint().apply {
            color = android.graphics.Color.rgb(138, 130, 117)
            textSize = 22f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Crafted with PRSNL Notes", width / 2f, height - 60f, brandingPaint)

        // Save to cache directory
        val cacheFolder = File(context.cacheDir, "shared_images").apply { mkdirs() }
        val shareFile = File(cacheFolder, "wrapped_${data.year}.png")
        FileOutputStream(shareFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            shareFile
        )

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "My PRSNL ${data.year} Notes Wrapped")
            putExtra(Intent.EXTRA_TEXT, "I wrote ${String.format("%.1f", data.totalMetersThisYear)} meters with my stylus in PRSNL Notes this year! #NotesWrapped")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Your Wrapped"))

    } catch (e: Exception) {
        android.util.Log.e("YearlyWrapped", "Failed to share image, falling back to text", e)
        try {
            val textIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "My PRSNL ${data.year} Notes Wrapped")
                putExtra(Intent.EXTRA_TEXT, "In ${data.year}, I wrote ${String.format("%.1f", data.totalMetersThisYear)} meters of stylus notes in PRSNL Notes! Top Notebook: ${data.topNotebook?.notebookTitle ?: "—"}. Longest Streak: ${data.longestStreakDays} days.")
            }
            context.startActivity(Intent.createChooser(textIntent, "Share Your Wrapped"))
        } catch (inner: Exception) {
            Toast.makeText(context, "Could not share wrapped summary", Toast.LENGTH_SHORT).show()
        }
    }
}
