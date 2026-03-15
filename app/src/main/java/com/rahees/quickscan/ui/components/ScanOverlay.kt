package com.rahees.quickscan.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath

@Composable
fun ScanOverlay(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan_animation")

    val scanLineProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_line"
    )

    val cornerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "corner_alpha"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val scanAreaWidth = size.width * 0.7f
        val scanAreaHeight = scanAreaWidth
        val scanAreaLeft = (size.width - scanAreaWidth) / 2f
        val scanAreaTop = (size.height - scanAreaHeight) / 2f
        val cornerRadius = 16f
        val scanRect = Rect(
            left = scanAreaLeft,
            top = scanAreaTop,
            right = scanAreaLeft + scanAreaWidth,
            bottom = scanAreaTop + scanAreaHeight
        )

        // Draw semi-transparent overlay with cutout
        val overlayPath = Path().apply {
            addRect(Rect(0f, 0f, size.width, size.height))
        }
        val cutoutPath = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = scanRect,
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )
            )
        }

        clipPath(cutoutPath, clipOp = ClipOp.Difference) {
            drawRect(
                color = Color.Black.copy(alpha = 0.6f),
                size = size
            )
        }

        // Draw corner brackets
        val bracketLength = 40f
        val bracketWidth = 4f
        val bracketColor = Color(0xFF00897B).copy(alpha = cornerAlpha)

        drawCornerBrackets(
            scanRect = scanRect,
            bracketLength = bracketLength,
            bracketWidth = bracketWidth,
            bracketColor = bracketColor,
            cornerRadius = cornerRadius
        )

        // Draw scan line
        val scanLineY = scanAreaTop + (scanAreaHeight * scanLineProgress)
        drawLine(
            color = Color(0xFF00897B).copy(alpha = 0.8f),
            start = Offset(scanAreaLeft + 16f, scanLineY),
            end = Offset(scanAreaLeft + scanAreaWidth - 16f, scanLineY),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawCornerBrackets(
    scanRect: Rect,
    bracketLength: Float,
    bracketWidth: Float,
    bracketColor: Color,
    cornerRadius: Float
) {
    val stroke = Stroke(width = bracketWidth, cap = StrokeCap.Round)

    // Top-left corner
    drawLine(bracketColor, Offset(scanRect.left, scanRect.top + cornerRadius), Offset(scanRect.left, scanRect.top + bracketLength), strokeWidth = bracketWidth, cap = StrokeCap.Round)
    drawLine(bracketColor, Offset(scanRect.left + cornerRadius, scanRect.top), Offset(scanRect.left + bracketLength, scanRect.top), strokeWidth = bracketWidth, cap = StrokeCap.Round)
    drawArc(bracketColor, startAngle = 180f, sweepAngle = 90f, useCenter = false, topLeft = Offset(scanRect.left, scanRect.top), size = Size(cornerRadius * 2, cornerRadius * 2), style = stroke)

    // Top-right corner
    drawLine(bracketColor, Offset(scanRect.right, scanRect.top + cornerRadius), Offset(scanRect.right, scanRect.top + bracketLength), strokeWidth = bracketWidth, cap = StrokeCap.Round)
    drawLine(bracketColor, Offset(scanRect.right - cornerRadius, scanRect.top), Offset(scanRect.right - bracketLength, scanRect.top), strokeWidth = bracketWidth, cap = StrokeCap.Round)
    drawArc(bracketColor, startAngle = 270f, sweepAngle = 90f, useCenter = false, topLeft = Offset(scanRect.right - cornerRadius * 2, scanRect.top), size = Size(cornerRadius * 2, cornerRadius * 2), style = stroke)

    // Bottom-left corner
    drawLine(bracketColor, Offset(scanRect.left, scanRect.bottom - cornerRadius), Offset(scanRect.left, scanRect.bottom - bracketLength), strokeWidth = bracketWidth, cap = StrokeCap.Round)
    drawLine(bracketColor, Offset(scanRect.left + cornerRadius, scanRect.bottom), Offset(scanRect.left + bracketLength, scanRect.bottom), strokeWidth = bracketWidth, cap = StrokeCap.Round)
    drawArc(bracketColor, startAngle = 90f, sweepAngle = 90f, useCenter = false, topLeft = Offset(scanRect.left, scanRect.bottom - cornerRadius * 2), size = Size(cornerRadius * 2, cornerRadius * 2), style = stroke)

    // Bottom-right corner
    drawLine(bracketColor, Offset(scanRect.right, scanRect.bottom - cornerRadius), Offset(scanRect.right, scanRect.bottom - bracketLength), strokeWidth = bracketWidth, cap = StrokeCap.Round)
    drawLine(bracketColor, Offset(scanRect.right - cornerRadius, scanRect.bottom), Offset(scanRect.right - bracketLength, scanRect.bottom), strokeWidth = bracketWidth, cap = StrokeCap.Round)
    drawArc(bracketColor, startAngle = 0f, sweepAngle = 90f, useCenter = false, topLeft = Offset(scanRect.right - cornerRadius * 2, scanRect.bottom - cornerRadius * 2), size = Size(cornerRadius * 2, cornerRadius * 2), style = stroke)
}
