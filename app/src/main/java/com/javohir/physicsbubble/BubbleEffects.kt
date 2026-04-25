package com.javohir.physicsbubble

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot

val SnapBackSpring = spring<Offset>(
    dampingRatio = 0.65f,
    stiffness = Spring.StiffnessLow
)

val UnlockedSnapSpring = spring<Offset>(
    dampingRatio = 0.45f,
    stiffness = Spring.StiffnessLow
)

@Composable
fun DeformationFrameLoop(state: PhysicsBubbleState) {
    LaunchedEffect(state) {
        var previousActualPos = state.bubblePos.value
        val startTime = androidx.compose.runtime.withFrameNanos { it }
        var lastFrameTime = startTime
        var smoothedVelocity = Offset.Zero
        var defVelocity = Offset.Zero

        val stiffness = 1500f
        val damping = 34.8f

        while (true) {
            val frameTime = androidx.compose.runtime.withFrameNanos { it }
            val dt = ((frameTime - lastFrameTime) / 1_000_000_000f).coerceAtMost(0.032f)
            lastFrameTime = frameTime

            state.shaderTime[0] = (frameTime - startTime) / 1_000_000_000f

            val currentActualPos = state.bubblePos.value
            val rawVelocity = currentActualPos - previousActualPos

            smoothedVelocity = Offset(
                x = smoothedVelocity.x + (rawVelocity.x - smoothedVelocity.x) * BubbleConfig.VELOCITY_SMOOTHING,
                y = smoothedVelocity.y + (rawVelocity.y - smoothedVelocity.y) * BubbleConfig.VELOCITY_SMOOTHING
            )

            if (state.popAnim.value == 0f) {
                val targetDeformation = Offset(
                    x = (smoothedVelocity.x * BubbleConfig.DEFORMATION_FACTOR).coerceIn(
                        -BubbleConfig.DEFORMATION_CLAMP, BubbleConfig.DEFORMATION_CLAMP
                    ),
                    y = (smoothedVelocity.y * BubbleConfig.DEFORMATION_FACTOR).coerceIn(
                        -BubbleConfig.DEFORMATION_CLAMP, BubbleConfig.DEFORMATION_CLAMP
                    )
                )

                val currentDef = state.deformationAnim.value
                val forceX = (targetDeformation.x - currentDef.x) * stiffness - defVelocity.x * damping
                val forceY = (targetDeformation.y - currentDef.y) * stiffness - defVelocity.y * damping
                defVelocity = Offset(defVelocity.x + forceX * dt, defVelocity.y + forceY * dt)
                val nextDef = Offset(currentDef.x + defVelocity.x * dt, currentDef.y + defVelocity.y * dt)
                state.deformationAnim.snapTo(nextDef)
            } else {
                state.deformationAnim.snapTo(Offset.Zero)
                defVelocity = Offset.Zero
            }

            previousActualPos = currentActualPos
        }
    }
}

fun Modifier.bubbleDragInput(
    state: PhysicsBubbleState,
    scope: CoroutineScope,
): Modifier = pointerInput(Unit) {
    var isUnlocked = false
    detectDragGestures(
        onDragStart = { isUnlocked = state.isAtTop() },
        onDragEnd = {
            scope.launch {
                if (isUnlocked) {
                    if (state.bubblePos.value.y < state.midPoint) {
                        state.bubblePos.animateTo(Offset(state.centerX, state.topOrbCenterY), UnlockedSnapSpring)
                    } else {
                        state.bubblePos.animateTo(Offset(state.centerX, state.bottomOrbCenterY), SnapBackSpring)
                    }
                } else {
                    val targetY = if (state.bubblePos.value.y < state.midPoint) state.topOrbCenterY else state.bottomOrbCenterY
                    state.bubblePos.animateTo(Offset(state.centerX, targetY), SnapBackSpring)
                }
            }
        }
    ) { change, dragAmount ->
        if (state.popAnim.value > 0f) return@detectDragGestures
        change.consume()
        val proposedY = state.bubblePos.value.y + dragAmount.y
        if (!isUnlocked && proposedY <= state.topOrbCenterY) isUnlocked = true
        if (isUnlocked) {
            scope.launch {
                state.bubblePos.snapTo(Offset(state.bubblePos.value.x + dragAmount.x, proposedY))
            }
        } else {
            val clampedY = proposedY.coerceAtMost(state.maxDragY)
            scope.launch { state.bubblePos.snapTo(Offset(state.centerX, clampedY)) }
        }
    }
}

fun Modifier.bubbleTapInput(
    state: PhysicsBubbleState,
    scope: CoroutineScope,
): Modifier = pointerInput(Unit) {
    detectTapGestures(
        onTap = {
            if (state.popAnim.value == 0f) {
                scope.launch {
                    state.popAnim.animateTo(1f, tween(BubbleConfig.POP_DURATION, easing = FastOutLinearInEasing))
                    delay(BubbleConfig.POP_DELAY)
                    state.popAnim.snapTo(0f)
                    state.bubblePos.snapTo(Offset(state.centerX, state.bottomOrbCenterY))
                }
            }
        }
    )
}

fun Modifier.bubbleShaderLayer(
    state: PhysicsBubbleState,
    shader: RuntimeShader?,
): Modifier = graphicsLayer {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && shader != null) {
        shader.setFloatUniform("touchCenter", state.bubblePos.value.x, state.bubblePos.value.y)
        shader.setFloatUniform("radius", state.currentOrbRadius)
        shader.setFloatUniform("progress", state.progress)
        shader.setFloatUniform("deformation", state.deformationAnim.value.x, state.deformationAnim.value.y)
        shader.setFloatUniform("popProgress", state.popAnim.value)
        shader.setFloatUniform("sysTime", state.shaderTime[0])
        renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "composable").asComposeRenderEffect()
    }
}

fun createRadialBrush(
    screenWidthPx: Float,
    screenHeightPx: Float,
    center: Color,
    mid1: Color,
    mid2: Color,
    edge: Color,
): Brush = Brush.radialGradient(
    0.0f to center,
    0.3f to mid1,
    0.7f to mid2,
    1.0f to edge,
    center = Offset(screenWidthPx / 2f, screenHeightPx * 0.4f)
)

fun DrawScope.drawThemeBackground(
    isDarkTheme: Boolean,
    previousIsDark: Boolean,
    revealProgress: Float,
    lightBrush: Brush,
    darkBrush: Brush,
    reusablePath: Path,
) {
    val currentBrush = if (isDarkTheme) darkBrush else lightBrush
    val prevBrush = if (previousIsDark) darkBrush else lightBrush

    drawRect(brush = prevBrush)

    if (revealProgress < 1f) {
        val maxRadius = hypot(size.width, size.height)
        val currentRevealRadius = revealProgress * maxRadius
        val epicenter = Offset(size.width - 100f, 150f)

        reusablePath.reset()
        reusablePath.addOval(
            Rect(
                left = epicenter.x - currentRevealRadius,
                top = epicenter.y - currentRevealRadius,
                right = epicenter.x + currentRevealRadius,
                bottom = epicenter.y + currentRevealRadius
            )
        )

        clipPath(reusablePath) {
            drawRect(brush = currentBrush)
        }
    } else {
        drawRect(brush = currentBrush)
    }
}
