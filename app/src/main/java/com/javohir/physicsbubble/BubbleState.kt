package com.javohir.physicsbubble

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.util.lerp

@Stable
class PhysicsBubbleState(
    private val screenHeightPx: Float,
    orbRadiusMaxPx: Float,
    orbRadiusMinPx: Float,
    val centerX: Float,
) {
    val bottomOrbCenterY = screenHeightPx * BubbleConfig.BOTTOM_ORB_RATIO
    val topOrbCenterY = screenHeightPx * BubbleConfig.TOP_ORB_RATIO
    val midPoint = (bottomOrbCenterY + topOrbCenterY) / 2f
    val maxDragY = bottomOrbCenterY + (screenHeightPx * BubbleConfig.DRAG_OVERSHOOT_RATIO)

    private val orbRadiusMax = orbRadiusMaxPx
    private val orbRadiusMin = orbRadiusMinPx

    private val orbRange = bottomOrbCenterY - topOrbCenterY
    private val textYBottom = screenHeightPx * BubbleConfig.TEXT_Y_BOTTOM_RATIO
    private val textYTop = screenHeightPx * BubbleConfig.TEXT_Y_TOP_RATIO

    val bubblePos = Animatable(Offset(centerX, bottomOrbCenterY), Offset.VectorConverter)
    val deformationAnim = Animatable(Offset.Zero, Offset.VectorConverter)
    val popAnim = Animatable(0f)
    val themeRevealProgress = Animatable(1f)
    val shaderTime = floatArrayOf(0f)

    val progress: Float
        get() = ((bottomOrbCenterY - bubblePos.value.y) / orbRange).coerceIn(0f, 1f)

    val currentOrbRadius: Float
        get() = lerp(orbRadiusMax, orbRadiusMin, progress)

    val textYOffsetPx: Float
        get() = lerp(textYBottom, textYTop, progress)

    fun isAtTop(): Boolean = bubblePos.value.y <= topOrbCenterY + BubbleConfig.SNAP_UNLOCK_THRESHOLD
}

@Composable
fun rememberBubbleState(
    screenWidthPx: Float,
    screenHeightPx: Float,
): PhysicsBubbleState {
    val density = LocalDensity.current
    val maxRadiusPx = with(density) { BubbleConfig.MAX_ORB_RADIUS.toPx() }
    val minRadiusPx = with(density) { BubbleConfig.MIN_ORB_RADIUS.toPx() }

    return remember(screenWidthPx, screenHeightPx) {
        PhysicsBubbleState(
            screenHeightPx = screenHeightPx,
            orbRadiusMaxPx = maxRadiusPx,
            orbRadiusMinPx = minRadiusPx,
            centerX = screenWidthPx / 2f,
        )
    }
}
