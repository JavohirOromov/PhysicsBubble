package com.javohir.physicsbubble

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun ThemeToggleButton(
    isDarkTheme: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress by animateFloatAsState(
        targetValue = if (isDarkTheme) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f),
        label = "theme_morph"
    )

    val scaleAnim = remember { Animatable(1f) }
    LaunchedEffect(isDarkTheme) {
        scaleAnim.snapTo(0.85f)
        scaleAnim.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 400f))
    }

    val mainPath = remember { Path() }
    val cutoutPath = remember { Path() }
    val finalPath = remember { Path() }

    Canvas(
        modifier = modifier
            .size(48.dp)
            .graphicsLayer {
                scaleX = scaleAnim.value
                scaleY = scaleAnim.value
            }
            .clip(CircleShape)
            .clickable(onClick = onToggle)
            .padding(6.dp)
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.width / 2f
        val currentColor = androidx.compose.ui.graphics.lerp(Color(0xFFFDB813), Color(0xFFE5E5EA), progress)

        rotate(degrees = progress * -90f, pivot = center) {
            val rayAlpha = (1f - progress * 2.5f).coerceIn(0f, 1f)
            if (rayAlpha > 0f) {
                val rayLength = maxRadius * 0.25f
                val rayOffset = maxRadius * 0.6f
                for (i in 0 until 8) {
                    rotate(degrees = i * 45f, pivot = center) {
                        drawLine(
                            color = currentColor.copy(alpha = rayAlpha),
                            start = center.copy(y = center.y - rayOffset),
                            end = center.copy(y = center.y - rayOffset - rayLength),
                            strokeWidth = maxRadius * 0.15f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            val sunRadius = maxRadius * 0.45f
            val moonRadius = maxRadius * 0.85f
            val currentRadius = sunRadius + (moonRadius - sunRadius) * progress

            mainPath.reset()
            mainPath.addOval(
                Rect(
                    left = center.x - currentRadius,
                    top = center.y - currentRadius,
                    right = center.x + currentRadius,
                    bottom = center.y + currentRadius
                )
            )

            val cutoutStartOffset = Offset(center.x + maxRadius * 2f, center.y - maxRadius * 2f)
            val cutoutEndOffset = Offset(center.x + currentRadius * 0.3f, center.y - currentRadius * 0.3f)
            val cutoutX = cutoutStartOffset.x + (cutoutEndOffset.x - cutoutStartOffset.x) * progress
            val cutoutY = cutoutStartOffset.y + (cutoutEndOffset.y - cutoutStartOffset.y) * progress
            val cutoutRadius = currentRadius * 0.95f

            cutoutPath.reset()
            cutoutPath.addOval(
                Rect(
                    left = cutoutX - cutoutRadius,
                    top = cutoutY - cutoutRadius,
                    right = cutoutX + cutoutRadius,
                    bottom = cutoutY + cutoutRadius
                )
            )

            finalPath.reset()
            finalPath.op(mainPath, cutoutPath, PathOperation.Difference)
            drawPath(path = finalPath, color = currentColor)
        }
    }
}
