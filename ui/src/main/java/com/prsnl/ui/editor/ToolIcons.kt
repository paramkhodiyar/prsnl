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
    neutralColor: Color = Color(0xFF64748B) // Slate gray for barrels
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        when (tool) {
            CanvasToolMode.PEN -> {
                // Ballpoint / Fountain Pen silhouette
                // Barrel: upper-right to mid-lower-left
                val barrelPath = Path().apply {
                    moveTo(w * 0.75f, h * 0.12f)
                    lineTo(w * 0.88f, h * 0.25f)
                    lineTo(w * 0.42f, h * 0.71f)
                    lineTo(w * 0.29f, h * 0.58f)
                    close()
                }
                drawPath(barrelPath, color = neutralColor, style = Fill)
                drawPath(barrelPath, color = neutralColor.copy(alpha = 0.5f), style = Stroke(width = 1.2f))

                // Metallic collar band
                drawLine(
                    color = Color(0xFFCBD5E1),
                    start = Offset(w * 0.29f, h * 0.58f),
                    end = Offset(w * 0.42f, h * 0.71f),
                    strokeWidth = 2.2f,
                    cap = StrokeCap.Round
                )

                // Pen Nib (Last ~20% of the icon shape, colored dynamically with tintColor)
                val nibPath = Path().apply {
                    moveTo(w * 0.29f, h * 0.58f)
                    lineTo(w * 0.42f, h * 0.71f)
                    lineTo(w * 0.18f, h * 0.82f)
                    lineTo(w * 0.10f, h * 0.90f) // Sharp fountain point
                    lineTo(w * 0.18f, h * 0.82f)
                    close()
                }
                drawPath(nibPath, color = tintColor, style = Fill)
                drawPath(nibPath, color = Color(0xFF334155), style = Stroke(width = 1.2f, join = StrokeJoin.Round))

                // Fine ink breather hole / split on nib
                drawLine(
                    color = Color.White.copy(alpha = 0.8f),
                    start = Offset(w * 0.23f, h * 0.77f),
                    end = Offset(w * 0.12f, h * 0.88f),
                    strokeWidth = 1f
                )
            }

            CanvasToolMode.PENCIL -> {
                // Tilted wooden pencil with visible graphite tip
                // Upper body: Hexagonal yellow/neutral barrel
                val pencilBody = Path().apply {
                    moveTo(w * 0.72f, h * 0.10f)
                    lineTo(w * 0.90f, h * 0.28f)
                    lineTo(w * 0.46f, h * 0.72f)
                    lineTo(w * 0.28f, h * 0.54f)
                    close()
                }
                drawPath(pencilBody, color = Color(0xFFF59E0B), style = Fill)
                drawPath(pencilBody, color = neutralColor, style = Stroke(width = 1.2f))

                // Inner facet line
                drawLine(
                    color = Color(0xFFD97706),
                    start = Offset(w * 0.81f, h * 0.19f),
                    end = Offset(w * 0.37f, h * 0.63f),
                    strokeWidth = 1.2f
                )

                // Wooden sharpened collar
                val woodCollar = Path().apply {
                    moveTo(w * 0.28f, h * 0.54f)
                    lineTo(w * 0.46f, h * 0.72f)
                    lineTo(w * 0.22f, h * 0.78f)
                    lineTo(w * 0.16f, h * 0.84f)
                    close()
                }
                drawPath(woodCollar, color = Color(0xFFFDE68A), style = Fill)

                // Graphite / Lead Tip (colored with tintColor)
                val leadTip = Path().apply {
                    moveTo(w * 0.20f, h * 0.80f)
                    lineTo(w * 0.10f, h * 0.90f) // Sharp point
                    lineTo(w * 0.16f, h * 0.84f)
                    close()
                }
                drawPath(leadTip, color = tintColor, style = Fill)
                drawPath(leadTip, color = Color(0xFF1E293B), style = Stroke(width = 1f))
            }

            CanvasToolMode.HIGHLIGHTER -> {
                // Chisel-tip marker body
                // Rectangular wide marker barrel
                val barrel = Path().apply {
                    moveTo(w * 0.65f, h * 0.12f)
                    lineTo(w * 0.88f, h * 0.35f)
                    lineTo(w * 0.46f, h * 0.77f)
                    lineTo(w * 0.23f, h * 0.54f)
                    close()
                }
                drawPath(barrel, color = neutralColor, style = Fill)
                drawPath(barrel, color = Color(0xFF334155), style = Stroke(width = 1.3f))

                // Cap grip ring
                drawLine(
                    color = Color(0xFF94A3B8),
                    start = Offset(w * 0.30f, h * 0.61f),
                    end = Offset(w * 0.53f, h * 0.84f),
                    strokeWidth = 2.5f
                )

                // Chisel Tip (Slanted rectangular cut, colored dynamically with tintColor)
                val chiselTip = Path().apply {
                    moveTo(w * 0.23f, h * 0.54f)
                    lineTo(w * 0.35f, h * 0.66f)
                    lineTo(w * 0.20f, h * 0.88f)
                    lineTo(w * 0.10f, h * 0.85f)
                    close()
                }
                drawPath(chiselTip, color = tintColor, style = Fill)
                drawPath(chiselTip, color = tintColor.copy(alpha = 0.7f), style = Stroke(width = 1.2f, join = StrokeJoin.Miter))
            }

            CanvasToolMode.STROKE_ERASER, CanvasToolMode.PIXEL_ERASER -> {
                // Classic block eraser
                val eraserTop = Path().apply {
                    moveTo(w * 0.30f, h * 0.18f)
                    lineTo(w * 0.75f, h * 0.18f)
                    lineTo(w * 0.88f, h * 0.38f)
                    lineTo(w * 0.43f, h * 0.38f)
                    close()
                }
                drawPath(eraserTop, color = Color(0xFFFDA4AF), style = Fill) // Pink eraser head

                val eraserBody = Path().apply {
                    moveTo(w * 0.43f, h * 0.38f)
                    lineTo(w * 0.88f, h * 0.38f)
                    lineTo(w * 0.72f, h * 0.82f)
                    lineTo(w * 0.27f, h * 0.82f)
                    close()
                }
                drawPath(eraserBody, color = Color(0xFF38BDF8), style = Fill) // Blue sleeve band
                drawPath(eraserBody, color = Color(0xFF0284C7), style = Stroke(width = 1.2f))

                // Eraser bottom rubber
                val eraserBottom = Path().apply {
                    moveTo(w * 0.27f, h * 0.82f)
                    lineTo(w * 0.72f, h * 0.82f)
                    lineTo(w * 0.64f, h * 0.92f)
                    lineTo(w * 0.19f, h * 0.92f)
                    close()
                }
                drawPath(eraserBottom, color = Color(0xFFFDA4AF), style = Fill)
                drawPath(eraserBottom, color = Color(0xFFE11D48), style = Stroke(width = 1.2f))
            }

            CanvasToolMode.LASSO -> {
                // Dashed lasso rope loop
                val lassoPath = Path().apply {
                    moveTo(w * 0.5f, h * 0.18f)
                    cubicTo(w * 0.85f, h * 0.15f, w * 0.92f, h * 0.65f, w * 0.55f, h * 0.72f)
                    cubicTo(w * 0.25f, h * 0.78f, w * 0.12f, h * 0.45f, w * 0.30f, h * 0.25f)
                    close()
                }
                drawPath(
                    lassoPath,
                    color = neutralColor,
                    style = Stroke(
                        width = 1.8f,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4f, 3f), 0f)
                    )
                )

                // Lasso loop knot & trailing tail
                drawCircle(color = Color(0xFFD97706), radius = 2.5f, center = Offset(w * 0.55f, h * 0.72f))
                val tailPath = Path().apply {
                    moveTo(w * 0.55f, h * 0.72f)
                    quadraticTo(w * 0.70f, h * 0.85f, w * 0.82f, h * 0.88f)
                }
                drawPath(tailPath, color = Color(0xFFD97706), style = Stroke(width = 2f, cap = StrokeCap.Round))
            }

            CanvasToolMode.SHAPE_PICKER -> {
                // Geometric shapes icon: Circle + Square
                drawCircle(
                    color = neutralColor,
                    radius = w * 0.26f,
                    center = Offset(w * 0.40f, h * 0.40f),
                    style = Stroke(width = 1.8f)
                )
                drawRect(
                    color = Color(0xFFC88A4B),
                    topLeft = Offset(w * 0.35f, h * 0.35f),
                    size = Size(w * 0.48f, h * 0.48f),
                    style = Stroke(width = 1.8f)
                )
            }

            CanvasToolMode.TEXT -> {
                // Typography 'T' icon
                val tPath = Path().apply {
                    moveTo(w * 0.22f, h * 0.22f)
                    lineTo(w * 0.78f, h * 0.22f)
                    lineTo(w * 0.78f, h * 0.36f)
                    lineTo(w * 0.57f, h * 0.36f)
                    lineTo(w * 0.57f, h * 0.78f)
                    lineTo(w * 0.65f, h * 0.78f)
                    lineTo(w * 0.65f, h * 0.88f)
                    lineTo(w * 0.35f, h * 0.88f)
                    lineTo(w * 0.35f, h * 0.78f)
                    lineTo(w * 0.43f, h * 0.78f)
                    lineTo(w * 0.43f, h * 0.36f)
                    lineTo(w * 0.22f, h * 0.36f)
                    close()
                }
                drawPath(tPath, color = neutralColor, style = Fill)
            }

            CanvasToolMode.SELECT -> {
                // Selection pointer / pan hand
                val arrowPath = Path().apply {
                    moveTo(w * 0.22f, h * 0.15f)
                    lineTo(w * 0.22f, h * 0.82f)
                    lineTo(w * 0.42f, h * 0.62f)
                    lineTo(w * 0.60f, h * 0.85f)
                    lineTo(w * 0.72f, h * 0.76f)
                    lineTo(w * 0.54f, h * 0.54f)
                    lineTo(w * 0.78f, h * 0.54f)
                    close()
                }
                drawPath(arrowPath, color = neutralColor, style = Fill)
                drawPath(arrowPath, color = Color.White, style = Stroke(width = 1.5f))
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

        // Coordinate axes
        drawLine(
            color = Color(0xFF64748B),
            start = Offset(w * 0.18f, h * 0.82f),
            end = Offset(w * 0.88f, h * 0.82f),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFF64748B),
            start = Offset(w * 0.18f, h * 0.82f),
            end = Offset(w * 0.18f, h * 0.14f),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )

        // Arrowheads
        val arrowX = Path().apply {
            moveTo(w * 0.88f, h * 0.82f)
            lineTo(w * 0.78f, h * 0.74f)
            moveTo(w * 0.88f, h * 0.82f)
            lineTo(w * 0.78f, h * 0.90f)
        }
        drawPath(arrowX, color = Color(0xFF64748B), style = Stroke(width = 1.8f, cap = StrokeCap.Round))

        val arrowY = Path().apply {
            moveTo(w * 0.18f, h * 0.14f)
            lineTo(w * 0.10f, h * 0.24f)
            moveTo(w * 0.18f, h * 0.14f)
            lineTo(w * 0.26f, h * 0.24f)
        }
        drawPath(arrowY, color = Color(0xFF64748B), style = Stroke(width = 1.8f, cap = StrokeCap.Round))

        // Vibrant intersecting function curve (parabola / curve in tintColor)
        val curvePath = Path().apply {
            moveTo(w * 0.24f, h * 0.76f)
            cubicTo(
                w * 0.40f, h * 0.74f,
                w * 0.50f, h * 0.35f,
                w * 0.80f, h * 0.28f
            )
        }
        drawPath(curvePath, color = tintColor, style = Stroke(width = 2.2f, cap = StrokeCap.Round))
    }
}
