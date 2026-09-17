package com.akshatdjain.ultron.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun ColorWheel(
    hue: Float,
    saturation: Float,
    onColorChange: (hue: Float, saturation: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var radiusPx by remember { mutableStateOf(0f) }
    var centerPx by remember { mutableStateOf(Offset.Zero) }

    fun updateFromOffset(offset: Offset) {
        if (radiusPx <= 0f) return
        val dx = offset.x - centerPx.x
        val dy = offset.y - centerPx.y
        val dist = hypot(dx, dy).coerceAtMost(radiusPx)
        val angleDeg = (Math.toDegrees(atan2(dy, dx).toDouble()) + 360.0) % 360.0
        onColorChange(angleDeg.toFloat(), (dist / radiusPx).coerceIn(0f, 1f))
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .pointerInput(Unit) { detectTapGestures { updateFromOffset(it) } }
            .pointerInput(Unit) { detectDragGestures { change, _ -> updateFromOffset(change.position) } },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            radiusPx = radius
            centerPx = Offset(size.width / 2f, size.height / 2f)

            drawCircle(
                brush = Brush.sweepGradient(
                    colors = (0..360 step 20).map { Color.hsv(it.toFloat() % 360f, 1f, 1f) }
                ),
                radius = radius,
                center = center
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color.Transparent),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.3f),
                radius = radius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            val angleRad = Math.toRadians(hue.toDouble())
            val thumbDist = radius * saturation
            val thumbCenter = Offset(
                x = center.x + (cos(angleRad) * thumbDist).toFloat(),
                y = center.y + (sin(angleRad) * thumbDist).toFloat()
            )
            drawCircle(color = Color.hsv(hue, saturation, 1f), radius = 14.dp.toPx(), center = thumbCenter)
            drawCircle(color = Color.White, radius = 14.dp.toPx(), center = thumbCenter, style = Stroke(width = 3.dp.toPx()))
        }
    }
}
