package com.javohir.physicsbubble

import androidx.compose.ui.unit.dp

object BubbleConfig {
    const val BOTTOM_ORB_RATIO = 0.92f
    const val TOP_ORB_RATIO = 0.28f
    val MAX_ORB_RADIUS = 250.dp
    val MIN_ORB_RADIUS = 85.dp
    const val TEXT_Y_BOTTOM_RATIO = 0.48f
    const val TEXT_Y_TOP_RATIO = 0.42f
    const val SNAP_UNLOCK_THRESHOLD = 10f
    const val DRAG_OVERSHOOT_RATIO = 0.05f
    const val DEFORMATION_FACTOR = 0.015f
    const val DEFORMATION_CLAMP = 0.6f
    const val VELOCITY_SMOOTHING = 0.15f
    const val THEME_REVEAL_DURATION = 1100
    const val POP_DURATION = 150
    const val POP_DELAY = 2000L
    const val TEXT_ANIM_DURATION = 700
}
