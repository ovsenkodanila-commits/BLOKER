package com.example.ui.components

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.hypot

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PatternLockView(
    modifier: Modifier = Modifier,
    dotRadius: Dp = 8.dp,
    hitRadius: Dp = 32.dp,
    gridSize: Int = 3,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    errorColor: Color = MaterialTheme.colorScheme.error,
    isError: Boolean = false,
    onPatternCompleted: (List<Int>) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var selectedDots by remember { mutableStateOf<List<Int>>(emptyList()) }
    var currentTouchPoint by remember { mutableStateOf<Offset?>(null) }
    
    val hitRadiusPx = with(LocalDensity.current) { hitRadius.toPx() }
    val dotRadiusPx = with(LocalDensity.current) { dotRadius.toPx() }

    // Keep grid points in state for hit checks
    var gridPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

    // Reset selected pattern if isError is set to false from true
    LaunchedEffect(isError) {
        if (!isError) {
            selectedDots = emptyList()
            currentTouchPoint = null
        }
    }

    val standbyColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInteropFilter { event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        if (isError) {
                            // If there was an error, clear it on new touch
                            selectedDots = emptyList()
                        }
                        currentTouchPoint = Offset(event.x, event.y)
                        if (gridPoints.isNotEmpty()) {
                            checkHit(event.x, event.y, gridPoints, hitRadiusPx) { hitDot ->
                                if (hitDot !in selectedDots) {
                                    selectedDots = selectedDots + hitDot
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                        }
                    }
                    MotionEvent.ACTION_MOVE -> {
                        currentTouchPoint = Offset(event.x, event.y)
                        if (gridPoints.isNotEmpty()) {
                            checkHit(event.x, event.y, gridPoints, hitRadiusPx) { hitDot ->
                                if (hitDot !in selectedDots) {
                                    selectedDots = selectedDots + hitDot
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        if (selectedDots.isNotEmpty()) {
                            onPatternCompleted(selectedDots)
                        }
                        currentTouchPoint = null
                    }
                    else -> return@pointerInteropFilter false
                }
                true
            }
    ) {
        val width = size.width
        val height = size.height
        
        // Compute 3x3 grid point positions centered in the canvas
        val cellWidth = width / (gridSize + 1)
        val cellHeight = height / (gridSize + 1)
        
        val points = mutableListOf<Offset>()
        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                val x = cellWidth * (col + 1)
                val y = cellHeight * (row + 1)
                points.add(Offset(x, y))
            }
        }
        gridPoints = points

        val activeColor = if (isError) errorColor else lineColor

        // Draw connections
        if (selectedDots.isNotEmpty()) {
            for (i in 0 until selectedDots.size - 1) {
                val startIdx = selectedDots[i]
                val endIdx = selectedDots[i + 1]
                if (startIdx < gridPoints.size && endIdx < gridPoints.size) {
                    val start = gridPoints[startIdx]
                    val end = gridPoints[endIdx]
                    drawLine(
                        color = activeColor,
                        start = start,
                        end = end,
                        strokeWidth = 6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
            
            // Draw live connection to finger drag
            currentTouchPoint?.let { touch ->
                val lastIdx = selectedDots.last()
                if (lastIdx < gridPoints.size) {
                    val lastDot = gridPoints[lastIdx]
                    drawLine(
                        color = activeColor,
                        start = lastDot,
                        end = touch,
                        strokeWidth = 6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        // Draw individual grid dots
        gridPoints.forEachIndexed { index, point ->
            val isSelected = index in selectedDots
            if (isSelected) {
                // outer visual ripple ring
                drawCircle(
                    color = activeColor.copy(alpha = 0.25f),
                    radius = hitRadiusPx,
                    center = point
                )
                // inner selected core dot
                drawCircle(
                    color = activeColor,
                    radius = dotRadiusPx * 1.5f,
                    center = point
                )
            } else {
                // normal standby dot
                drawCircle(
                    color = standbyColor,
                    radius = dotRadiusPx,
                    center = point
                )
            }
        }
    }
}

private inline fun checkHit(
    x: Float,
    y: Float,
    points: List<Offset>,
    hitRadius: Float,
    onHit: (Int) -> Unit
) {
    points.forEachIndexed { index, offset ->
        if (hypot(x - offset.x, y - offset.y) < hitRadius) {
            onHit(index)
        }
    }
}
