package ai.aligned.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import ai.aligned.ui.theme.AlignedTokens
import ai.aligned.ui.theme.Tokens

/**
 * Press wrapper that follows tokens.tap: 0.97 scale, ~96ms spring release, soft haptic.
 * Composable content lambda receives the current pressed state.
 */
@Composable
fun AlignedPress(
    onClick: () -> Unit,
    enabled: Boolean = true,
    haptic: Boolean = true,
    content: @Composable (pressed: Boolean) -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val hapticFb = LocalHapticFeedback.current
    Box(
        modifier = Modifier.pointerInput(enabled) {
            if (!enabled) return@pointerInput
            detectTapGestures(
                onPress = {
                    pressed = true
                    try { tryAwaitRelease() } finally { pressed = false }
                },
                onTap = {
                    if (haptic) hapticFb.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            )
        }
    ) {
        content(pressed)
    }
}

/**
 * Loading shimmer that replaces feed cards while the cache primes.
 * No animation library — a slow alpha pulse via animateFloatAsState.
 */
@Composable
fun SkeletonCard(heightDp: Int = 132) {
    val c = AlignedTokens.colors
    var on by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            on = !on
            kotlinx.coroutines.delay(900)
        }
    }
    val alpha by animateFloatAsState(
        if (on) 0.55f else 1f,
        spring(Tokens.Motion.SpringDamping, 80f),
        label = "skeletonpulse"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .clip(RoundedCornerShape(Tokens.Radius.card))
            .background(c.elev1.copy(alpha = alpha))
            .padding(16.dp)
    )
}
