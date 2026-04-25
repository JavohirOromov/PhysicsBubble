package com.javohir.physicsbubble

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun BubbleFallback(state: PhysicsBubbleState) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.6f), Color.Transparent),
                center = state.bubblePos.value,
                radius = state.currentOrbRadius
            )
        )
    }
}
