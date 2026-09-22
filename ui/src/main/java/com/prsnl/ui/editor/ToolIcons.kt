package com.prsnl.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.prsnl.drawing.view.CanvasToolMode

@Composable
fun ToolIcon(
    tool: CanvasToolMode,
    tintColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    neutralColor: Color = Color(0xFFE2E8F0) // High-visibility bright platinum/silver
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        when (tool) {
            CanvasToolMode.PEN -> {
                // Ballpoint / Fountain Pen silhouette with luminous metallic barrel
                val barrelPath = Path().apply {
                    moveTo(w * 0.76f, h * 0.10f)
                    lineTo(w * 0.90f, h * 0.24f)
                    lineTo(w * 0.44f, h * 0.70f)
                    lineTo(w * 0.30f, h * 0.56f)
                    close()
                }
                drawPath(barrelPath, color = neutralColor, style = Fill)
                drawPath(barrelPath, color = Color(0xFF0F172A).copy(alpha = 0.6f), style = Stroke(width = 1.2f))

                // Polished gold metallic collar band
                drawLine(
                    color = Color(0xFFD97706),
                    start = Offset(w * 0.30f, h * 0.56f),
                    end = Offset(w * 0.44f, h * 0.70f),
                    strokeWidth = 2.4f,
                    cap = StrokeCap.Round
                )

                // Pen Nib: Fountain shape filled with dynamic ink tintColor
                val nibPath = Path().apply {
                    moveTo(w * 0.30f, h * 0.56f)
                    lineTo(w * 0.44f, h * 0.70f)
                    lineTo(w * 0.22f, h * 0.82f)
                    lineTo(w * 0.08f, h * 0.92f) // Sharp fountain point
                    lineTo(w * 0.18f, h * 0.78f)
                    close()
                }
                drawPath(nibPath, color = tintColor, style = Fill)
                drawPath(nibPath, color = Color(0xFF0F172A), style = Stroke(width = 1.2f, join = StrokeJoin.Round))

                // Ink breather line on nib
                drawLine(
                    color = Color.White.copy(alpha = 0.9f),
                    start = Offset(w * 0.24f, h * 0.74f),
                    end = Offset(w * 0.12f, h * 0.88f),
                    strokeWidth = 1.2f
                )
            }

            CanvasToolMode.PENCIL -> {
                // Tilted wooden pencil with distinct hexagonal facets & sharp lead tip
                val pencilBody = Path().apply {
                    moveTo(w * 0.72f, h * 0.10f)
                    lineTo(w * 0.90f, h * 0.28f)
                    lineTo(w * 0.46f, h * 0.72f)
                    lineTo(w * 0.28f, h * 0.54f)
                    close()
                }
                drawPath(pencilBody, color = Color(0xFFF59E0B), style = Fill)
                drawPath(pencilBody, color = Color(0xFF0F172A).copy(alpha = 0.5f), style = Stroke(width = 1.2f))

                // Pencil facet highlight line
                drawLine(
                    color = Color(0xFFFDE68A),
                    start = Offset(w * 0.81f, h * 0.19f),
                    end = Offset(w * 0.37f, h * 0.63f),
                    strokeWidth = 1.4f
                )

                // Sharpened natural wood collar
                val woodCollar = Path().apply {
                    moveTo(w * 0.28f, h * 0.54f)
                    lineTo(w * 0.46f, h * 0.72f)
                    lineTo(w * 0.22f, h * 0.78f)
                    lineTo(w * 0.16f, h * 0.84f)
                    close()
                }
                drawPath(woodCollar, color = Color(0xFFFEF3C7), style = Fill)
                drawPath(woodCollar, color = Color(0xFFB45309).copy(alpha = 0.7f), style = Stroke(width = 1f))

                // Graphite / Lead tip with tintColor accent
                val leadTip = Path().apply {
                    moveTo(w * 0.20f, h * 0.80f)
                    lineTo(w * 0.08f, h * 0.92f)
                    lineTo(w * 0.16f, h * 0.84f)
                    close()
                }
                drawPath(leadTip, color = tintColor, style = Fill)
                drawPath(leadTip, color = Color(0xFF0F172A), style = Stroke(width = 1.2f))
            }

            CanvasToolMode.HIGHLIGHTER -> {
                // Wide chisel-tip marker with bright barrel & fluorescent neon chisel cut
                val barrel = Path().apply {
                    moveTo(w * 0.65f, h * 0.12f)
                    lineTo(w * 0.88f, h * 0.35f)
                    lineTo(w * 0.46f, h * 0.77f)
                    lineTo(w * 0.23f, h * 0.54f)
                    close()
                }
                drawPath(barrel, color = Color(0xFFF1F5F9), style = Fill)
                drawPath(barrel, color = Color(0xFF0F172A).copy(alpha = 0.6f), style = Stroke(width = 1.3f))

                // High-contrast grip ring
                drawLine(
                    color = Color(0xFF334155),
                    start = Offset(w * 0.28f, h * 0.59f),
                    end = Offset(w * 0.51f, h * 0.82f),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )

                // Fluorescent Chisel Tip: Angled broad rectangular cut
                val chiselTip = Path().apply {
                    moveTo(w * 0.23f, h * 0.54f)
                    lineTo(w * 0.38f, h * 0.69f)
                    lineTo(w * 0.22f, h * 0.92f)
                    lineTo(w * 0.08f, h * 0.82f)
                    close()
                }
                drawPath(chiselTip, color = tintColor, style = Fill)
                drawPath(chiselTip, color = Color(0xFF0F172A), style = Stroke(width = 1.4f, join = StrokeJoin.Miter))

                // Highlight shine along chisel face
                drawLine(
                    color = Color.White.copy(alpha = 0.8f),
                    start = Offset(w * 0.22f, h * 0.65f),
                    end = Offset(w * 0.14f, h * 0.84f),
                    strokeWidth = 1.5f,
                    cap = StrokeCap.Round
                )
            }

            CanvasToolMode.STROKE_ERASER, CanvasToolMode.PIXEL_ERASER -> {
                // Classic block eraser with bright pink beveled head & cyan/blue paper sleeve
                val eraserTop = Path().apply {
                    moveTo(w * 0.28f, h * 0.16f)
                    lineTo(w * 0.74f, h * 0.16f)
                    lineTo(w * 0.88f, h * 0.38f)
                    lineTo(w * 0.42f, h * 0.38f)
                    close()
                }
                drawPath(eraserTop, color = Color(0xFFFB7185), style = Fill) // Vivid coral pink
                drawPath(eraserTop, color = Color(0xFFBE123C), style = Stroke(width = 1.2f))

                // Paper sleeve band
                val eraserBody = Path().apply {
                    moveTo(w * 0.42f, h * 0.38f)
                    lineTo(w * 0.88f, h * 0.38f)
                    lineTo(w * 0.72f, h * 0.80f)
                    lineTo(w * 0.26f, h * 0.80f)
                    close()
                }
                drawPath(eraserBody, color = Color(0xFF38BDF8), style = Fill) // Sky cyan
                drawPath(eraserBody, color = Color(0xFF0284C7), style = Stroke(width = 1.2f))

                // Crisp white brand strip across sleeve
                drawLine(
                    color = Color.White,
                    start = Offset(w * 0.34f, h * 0.58f),
                    end = Offset(w * 0.80f, h * 0.58f),
                    strokeWidth = 2.4f
                )

                // Eraser bottom base
                val eraserBottom = Path().apply {
                    moveTo(w * 0.26f, h * 0.80f)
                    lineTo(w * 0.72f, h * 0.80f)
                    lineTo(w * 0.64f, h * 0.92f)
                    lineTo(w * 0.18f, h * 0.92f)
                    close()
                }
                drawPath(eraserBottom, color = Color(0xFFFDA4AF), style = Fill)
                drawPath(eraserBottom, color = Color(0xFFBE123C), style = Stroke(width = 1.2f))
            }

            CanvasToolMode.LASSO -> {
                // High-visibility dashed lasso loop in golden amber with rope knot
                val lassoPath = Path().apply {
                    moveTo(w * 0.5f, h * 0.16f)
                    cubicTo(w * 0.88f, h * 0.14f, w * 0.94f, h * 0.66f, w * 0.55f, h * 0.74f)
                    cubicTo(w * 0.22f, h * 0.80f, w * 0.10f, h * 0.44f, w * 0.30f, h * 0.24f)
                    close()
                }
                drawPath(
                    lassoPath,
                    color = Color(0xFFFBBF24),
                    style = Stroke(
                        width = 2.2f,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 3f), 0f)
                    )
                )

                // Lasso loop knot & trailing cord
                drawCircle(color = Color(0xFFF59E0B), radius = 3f, center = Offset(w * 0.55f, h * 0.74f))
                val tailPath = Path().apply {
                    moveTo(w * 0.55f, h * 0.74f)
                    quadraticTo(w * 0.72f, h * 0.86f, w * 0.84f, h * 0.90f)
                }
                drawPath(tailPath, color = Color(0xFFF59E0B), style = Stroke(width = 2.2f, cap = StrokeCap.Round))
            }

            CanvasToolMode.SHAPE_PICKER -> {
                // Crisp geometric figures: Circle + Rounded Square with bright contrasting edges
                drawCircle(
                    color = Color(0xFF38BDF8),
                    radius = w * 0.28f,
                    center = Offset(w * 0.40f, h * 0.40f),
                    style = Stroke(width = 2.2f)
                )
                drawRect(
                    color = Color(0xFFF59E0B),
                    topLeft = Offset(w * 0.36f, h * 0.36f),
                    size = Size(w * 0.50f, h * 0.50f),
                    style = Stroke(width = 2.2f)
                )
            }

            CanvasToolMode.TEXT -> {
                // Crisp typography 'T' icon with prominent serifs
                val tPath = Path().apply {
                    moveTo(w * 0.20f, h * 0.20f)
                    lineTo(w * 0.80f, h * 0.20f)
                    lineTo(w * 0.80f, h * 0.35f)
                    lineTo(w * 0.58f, h * 0.35f)
                    lineTo(w * 0.58f, h * 0.78f)
                    lineTo(w * 0.68f, h * 0.78f)
                    lineTo(w * 0.68f, h * 0.90f)
                    lineTo(w * 0.32f, h * 0.90f)
                    lineTo(w * 0.32f, h * 0.78f)
                    lineTo(w * 0.42f, h * 0.78f)
                    lineTo(w * 0.42f, h * 0.35f)
                    lineTo(w * 0.20f, h * 0.35f)
                    close()
                }
                drawPath(tPath, color = Color(0xFFF8FAFC), style = Fill)
                drawPath(tPath, color = Color(0xFF0F172A).copy(alpha = 0.5f), style = Stroke(width = 1.2f))
            }

            CanvasToolMode.SELECT -> {
                // Crisp modern pointer cursor: pure white fill with dark outline
                val arrowPath = Path().apply {
                    moveTo(w * 0.20f, h * 0.12f)
                    lineTo(w * 0.20f, h * 0.85f)
                    lineTo(w * 0.44f, h * 0.63f)
                    lineTo(w * 0.64f, h * 0.90f)
                    lineTo(w * 0.76f, h * 0.80f)
                    lineTo(w * 0.56f, h * 0.54f)
                    lineTo(w * 0.82f, h * 0.54f)
                    close()
                }
                drawPath(arrowPath, color = Color.White, style = Fill)
                drawPath(arrowPath, color = Color(0xFF0F172A), style = Stroke(width = 1.8f, join = StrokeJoin.Round))

                // Bright sky-blue accent dot inside the pointer
                drawCircle(color = Color(0xFF38BDF8), radius = 2.2f, center = Offset(w * 0.36f, h * 0.42f))
            }
        }
    }
}

@Composable
fun GraphToolIcon(
    modifier: Modifier = Modifier,
    tintColor: Color = Color(0xFFC88A4B),
    size: Dp = 22.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Coordinate axes with high-visibility bright line
        drawLine(
            color = Color(0xFFE2E8F0),
            start = Offset(w * 0.16f, h * 0.84f),
            end = Offset(w * 0.88f, h * 0.84f),
            strokeWidth = 2.2f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFFE2E8F0),
            start = Offset(w * 0.16f, h * 0.84f),
            end = Offset(w * 0.16f, h * 0.14f),
            strokeWidth = 2.2f,
            cap = StrokeCap.Round
        )

        // Axis arrow tips
        val xArrow = Path().apply {
            moveTo(w * 0.88f, h * 0.79f)
            lineTo(w * 0.96f, h * 0.84f)
            lineTo(w * 0.88f, h * 0.89f)
        }
        drawPath(xArrow, color = Color(0xFFE2E8F0), style = Stroke(width = 2.2f, cap = StrokeCap.Round))

        val yArrow = Path().apply {
            moveTo(w * 0.11f, h * 0.20f)
            lineTo(w * 0.16f, h * 0.10f)
            lineTo(w * 0.21f, h * 0.20f)
        }
        drawPath(yArrow, color = Color(0xFFE2E8F0), style = Stroke(width = 2.2f, cap = StrokeCap.Round))

        // Parabolic curve with vibrant gold highlight
        val curve = Path().apply {
            moveTo(w * 0.22f, h * 0.78f)
            quadraticTo(w * 0.50f, h * 0.24f, w * 0.84f, h * 0.70f)
        }
        drawPath(curve, color = tintColor, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
    }
}
