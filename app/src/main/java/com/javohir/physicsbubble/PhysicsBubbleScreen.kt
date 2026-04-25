package com.javohir.physicsbubble

import android.app.Activity
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun PhysicsBubbleScreen() {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()
        val state = rememberBubbleState(screenWidthPx, screenHeightPx)
        PhysicsBubbleContent(state, screenWidthPx, screenHeightPx)
    }
}

@Composable
private fun PhysicsBubbleContent(
    state: PhysicsBubbleState,
    screenWidthPx: Float,
    screenHeightPx: Float,
) {
    val scope = rememberCoroutineScope()
    var isDarkTheme by remember { mutableStateOf(false) }
    var previousIsDark by remember { mutableStateOf(false) }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkTheme
        }
    }

    val lightBrush = remember(screenWidthPx, screenHeightPx) {
        createRadialBrush(
            screenWidthPx, screenHeightPx,
            BubbleColors.LIGHT_CENTER, BubbleColors.LIGHT_MID1,
            BubbleColors.LIGHT_MID2, BubbleColors.LIGHT_EDGE
        )
    }
    val darkBrush = remember(screenWidthPx, screenHeightPx) {
        createRadialBrush(
            screenWidthPx, screenHeightPx,
            BubbleColors.DARK_CENTER, BubbleColors.DARK_MID,
            BubbleColors.DARK_MID, BubbleColors.DARK_EDGE
        )
    }

    val textTween = remember { tween<androidx.compose.ui.graphics.Color>(BubbleConfig.TEXT_ANIM_DURATION, easing = FastOutLinearInEasing) }
    val mainTextColor by animateColorAsState(
        if (isDarkTheme) BubbleColors.DARK_MAIN_TEXT else BubbleColors.LIGHT_MAIN_TEXT,
        animationSpec = textTween,
        label = "mainText"
    )
    val titleColor by animateColorAsState(
        if (isDarkTheme) BubbleColors.DARK_TITLE else BubbleColors.LIGHT_TITLE,
        animationSpec = textTween,
        label = "title"
    )
    val subtitleColor by animateColorAsState(
        if (isDarkTheme) BubbleColors.DARK_SUBTITLE else BubbleColors.LIGHT_SUBTITLE,
        animationSpec = textTween,
        label = "subtitle"
    )

    DeformationFrameLoop(state)

    val shader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) RuntimeShader(KINEMATIC_LENS_SHADER) else null
    }
    val revealClipPath = remember { Path() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .bubbleDragInput(state, scope)
            .bubbleTapInput(state, scope)
            .bubbleShaderLayer(state, shader)
            .drawBehind {
                drawThemeBackground(
                    isDarkTheme = isDarkTheme,
                    previousIsDark = previousIsDark,
                    revealProgress = state.themeRevealProgress.value,
                    lightBrush = lightBrush,
                    darkBrush = darkBrush,
                    reusablePath = revealClipPath,
                )
            }
    ) {
        ThemeToggleButton(
            isDarkTheme = isDarkTheme,
            onToggle = {
                if (state.themeRevealProgress.isRunning) return@ThemeToggleButton
                previousIsDark = isDarkTheme
                isDarkTheme = !isDarkTheme
                scope.launch {
                    state.themeRevealProgress.snapTo(0f)
                    state.themeRevealProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = BubbleConfig.THEME_REVEAL_DURATION,
                            easing = CubicBezierEasing(0.1f, 0.8f, 0.2f, 1.0f)
                        )
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 24.dp)
        )

        Text(
            text = "Piksellar endi\nfizik harakatda.",
            fontSize = 32.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 40.sp,
            color = mainTextColor,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-80).dp)
                .graphicsLayer { alpha = 1f - (state.progress * 4).coerceIn(0f, 1f) }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(x = 0, y = state.textYOffsetPx.roundToInt()) },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { alpha = (state.progress * 3).coerceIn(0f, 1f) }
            ) {
                Text(
                    text = "AGSL Jarayonlari",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp,
                    color = titleColor
                )
                Text(
                    text = "Real vaqtli yupqa qatlam interferensiyasi\nkinematik prujinalar bilan boshqariladi.",
                    fontSize = 24.sp,
                    lineHeight = 26.sp,
                    textAlign = TextAlign.Center,
                    color = subtitleColor,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            BubbleFallback(state)
        }
    }
}
