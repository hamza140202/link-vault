package com.momostack.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun MomoMascotView(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    animate: Boolean = true,
    holdingCard: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "momoAnimation")

    val bounceY by if (animate) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -8f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bounceY"
        )
    } else {
        remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    val earWiggle by if (animate) {
        infiniteTransition.animateFloat(
            initialValue = -2f,
            targetValue = 2f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "earWiggle"
        )
    } else {
        remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    Canvas(modifier = modifier.size(size)) {
        val scale = this.size.width / 512f

        translate(top = bounceY * scale) {
            // Left Ear
            rotate(degrees = -20f + earWiggle, pivot = Offset(170f * scale, 155f * scale)) {
                drawOval(
                    color = Color.White,
                    topLeft = Offset((170f - 36f) * scale, (155f - 52f) * scale),
                    size = Size(72f * scale, 104f * scale)
                )
                drawOval(
                    color = Color(0xFFF43F5E),
                    topLeft = Offset((170f - 22f) * scale, (155f - 36f) * scale),
                    size = Size(44f * scale, 72f * scale)
                )
            }

            // Right Ear
            rotate(degrees = 20f - earWiggle, pivot = Offset(342f * scale, 155f * scale)) {
                drawOval(
                    color = Color.White,
                    topLeft = Offset((342f - 36f) * scale, (155f - 52f) * scale),
                    size = Size(72f * scale, 104f * scale)
                )
                drawOval(
                    color = Color(0xFFF43F5E),
                    topLeft = Offset((342f - 22f) * scale, (155f - 36f) * scale),
                    size = Size(44f * scale, 72f * scale)
                )
            }

            // Momo Dumpling Body
            val bodyPath = Path().apply {
                moveTo(256f * scale, 145f * scale)
                cubicTo(366f * scale, 145f * scale, 400f * scale, 210f * scale, 400f * scale, 298f * scale)
                cubicTo(400f * scale, 390f * scale, 350f * scale, 412f * scale, 256f * scale, 412f * scale)
                cubicTo(162f * scale, 412f * scale, 112f * scale, 390f * scale, 112f * scale, 298f * scale)
                cubicTo(112f * scale, 210f * scale, 146f * scale, 145f * scale, 256f * scale, 145f * scale)
                close()
            }
            drawPath(path = bodyPath, color = Color(0xFFFFFDF9))
            drawPath(path = bodyPath, color = Color(0xFFFFE4E6), style = Stroke(width = 2f * scale))

            // Rosy Blush Cheeks
            drawCircle(
                color = Color(0x55FF4D6D),
                radius = 26f * scale,
                center = Offset(170f * scale, 310f * scale)
            )
            drawCircle(
                color = Color(0x55FF4D6D),
                radius = 26f * scale,
                center = Offset(342f * scale, 310f * scale)
            )

            // Left Eye
            drawOval(
                color = Color(0xFF18181B),
                topLeft = Offset((204f - 19f) * scale, (268f - 26f) * scale),
                size = Size(38f * scale, 52f * scale)
            )
            drawOval(
                color = Color.White,
                topLeft = Offset((212f - 8f) * scale, (256f - 11f) * scale),
                size = Size(16f * scale, 22f * scale)
            )
            drawCircle(
                color = Color.White,
                radius = 4.5f * scale,
                center = Offset(198f * scale, 280f * scale)
            )

            // Right Eye
            drawOval(
                color = Color(0xFF18181B),
                topLeft = Offset((308f - 19f) * scale, (268f - 26f) * scale),
                size = Size(38f * scale, 52f * scale)
            )
            drawOval(
                color = Color.White,
                topLeft = Offset((316f - 8f) * scale, (256f - 11f) * scale),
                size = Size(16f * scale, 22f * scale)
            )
            drawCircle(
                color = Color.White,
                radius = 4.5f * scale,
                center = Offset(302f * scale, 280f * scale)
            )

            // Sweet Smile & Pink Tongue
            val smilePath = Path().apply {
                moveTo(242f * scale, 292f * scale)
                quadraticBezierTo(256f * scale, 308f * scale, 270f * scale, 292f * scale)
            }
            drawPath(
                path = smilePath,
                color = Color(0xFF18181B),
                style = Stroke(width = 5f * scale, cap = StrokeCap.Round)
            )

            val tonguePath = Path().apply {
                moveTo(249f * scale, 299f * scale)
                quadraticBezierTo(256f * scale, 310f * scale, 263f * scale, 299f * scale)
                close()
            }
            drawPath(path = tonguePath, color = Color(0xFFFB7185))

            // Held Note Card
            if (holdingCard) {
                // Indigo Note Card
                drawRoundRect(
                    color = Color(0xFF4F46E5),
                    topLeft = Offset(176f * scale, 326f * scale),
                    size = Size(160f * scale, 92f * scale),
                    cornerRadius = CornerRadius(18f * scale, 18f * scale)
                )
                drawRoundRect(
                    color = Color(0xFF818CF8),
                    topLeft = Offset(176f * scale, 326f * scale),
                    size = Size(160f * scale, 92f * scale),
                    cornerRadius = CornerRadius(18f * scale, 18f * scale),
                    style = Stroke(width = 2.5f * scale)
                )

                // Coral Bookmark Ribbon
                val ribbonPath = Path().apply {
                    moveTo(238f * scale, 312f * scale)
                    lineTo(274f * scale, 312f * scale)
                    lineTo(274f * scale, 350f * scale)
                    lineTo(256f * scale, 338f * scale)
                    lineTo(238f * scale, 350f * scale)
                    close()
                }
                drawPath(path = ribbonPath, color = Color(0xFFFF6B8B))

                // Gold Star
                drawCircle(
                    color = Color(0xFFFBBF24),
                    radius = 3.5f * scale,
                    center = Offset(256f * scale, 324f * scale)
                )

                // Embossed Note Lines
                drawLine(
                    color = Color(0xFFE0E7FF),
                    start = Offset(198f * scale, 368f * scale),
                    end = Offset(314f * scale, 368f * scale),
                    strokeWidth = 3f * scale,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFFE0E7FF),
                    start = Offset(198f * scale, 384f * scale),
                    end = Offset(284f * scale, 384f * scale),
                    strokeWidth = 3f * scale,
                    cap = StrokeCap.Round
                )

                // Momo's Little Plush Paws
                drawOval(
                    color = Color(0xFFFFFDF9),
                    topLeft = Offset((176f - 16f) * scale, (372f - 12f) * scale),
                    size = Size(32f * scale, 24f * scale)
                )
                drawOval(
                    color = Color(0xFFFFE4E6),
                    topLeft = Offset((176f - 16f) * scale, (372f - 12f) * scale),
                    size = Size(32f * scale, 24f * scale),
                    style = Stroke(width = 1.5f * scale)
                )

                drawOval(
                    color = Color(0xFFFFFDF9),
                    topLeft = Offset((336f - 16f) * scale, (372f - 12f) * scale),
                    size = Size(32f * scale, 24f * scale)
                )
                drawOval(
                    color = Color(0xFFFFE4E6),
                    topLeft = Offset((336f - 16f) * scale, (372f - 12f) * scale),
                    size = Size(32f * scale, 24f * scale),
                    style = Stroke(width = 1.5f * scale)
                )
            }
        }
    }
}
