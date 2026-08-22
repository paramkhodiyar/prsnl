package com.prsnl.drawing.shape

data class ShapeRecognitionConfig(
    val holdDurationMs: Long = 400L,
    val velocityStillThreshold: Float = 0.08f, // px/ms
    val closeShapeThreshold: Float = 60f, // px gap
    val lineResidualThreshold: Float = 15f,
    val cornerAngleThreshold: Float = 55f, // degrees
    val ellipseDeviationThreshold: Float = 25f
)
