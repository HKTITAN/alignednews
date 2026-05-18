package ai.aligned.ui.icons

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ai.aligned.ui.theme.AlignedTokens
import ai.aligned.ui.theme.Tokens
import kotlinx.coroutines.launch

/**
 * Morphing icon system per design/ALIGNED_DESIGN.md.
 *
 * Every icon is three lines in a 14×14 logical viewBox. Unused lines collapse to
 * (7, 7) with opacity 0. Icons in the same [IconSpec.rotationGroup] share
 * coordinates and differ only by rotation — when you morph between them we
 * animate the parent rotation rather than the line endpoints.
 *
 * Cross-group morphs interpolate every endpoint independently with a single spring.
 */

data class IconLine(
    val x1: Float, val y1: Float, val x2: Float, val y2: Float,
    val visible: Boolean = true
) {
    companion object {
        val Collapsed = IconLine(7f, 7f, 7f, 7f, visible = false)
    }
}

data class IconSpec(
    val id: String,
    val lines: List<IconLine>,            // exactly 3
    val rotationGroup: String? = null,
    val rotationDeg: Float = 0f
) { init { require(lines.size == 3) { "icon $id must have exactly 3 lines" } } }

/** Catalogue of the 26 ALIGNED icons. Generated from `design/icons/`. */
object MorphingIcons {

    private fun line(x1: Int, y1: Int, x2: Int, y2: Int): IconLine =
        if (x1 == 7 && y1 == 7 && x2 == 7 && y2 == 7) IconLine.Collapsed
        else IconLine(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat())

    val arrowUp    = IconSpec("arrow-up",    listOf(line(7,12,7,2),  line(3,6,7,2),   line(7,2,11,6)),  "arrow",   0f)
    val arrowRight = IconSpec("arrow-right", listOf(line(7,12,7,2),  line(3,6,7,2),   line(7,2,11,6)),  "arrow",  90f)
    val arrowDown  = IconSpec("arrow-down",  listOf(line(7,12,7,2),  line(3,6,7,2),   line(7,2,11,6)),  "arrow", 180f)
    val arrowLeft  = IconSpec("arrow-left",  listOf(line(7,12,7,2),  line(3,6,7,2),   line(7,2,11,6)),  "arrow", 270f)

    val chevronUp    = IconSpec("chevron-up",    listOf(line(3,9,7,5),   line(7,5,11,9), line(7,7,7,7)), "chevron",   0f)
    val chevronRight = IconSpec("chevron-right", listOf(line(3,9,7,5),   line(7,5,11,9), line(7,7,7,7)), "chevron",  90f)
    val chevronDown  = IconSpec("chevron-down",  listOf(line(3,9,7,5),   line(7,5,11,9), line(7,7,7,7)), "chevron", 180f)
    val chevronLeft  = IconSpec("chevron-left",  listOf(line(3,9,7,5),   line(7,5,11,9), line(7,7,7,7)), "chevron", 270f)

    val plus  = IconSpec("plus",  listOf(line(2,7,12,7),  line(7,2,7,12),  line(7,7,7,7)), "cross",  0f)
    val close = IconSpec("close", listOf(line(2,7,12,7),  line(7,2,7,12),  line(7,7,7,7)), "cross", 45f)

    val menu     = IconSpec("menu",     listOf(line(2,4,12,4),  line(2,7,12,7),  line(2,10,12,10)))
    val check    = IconSpec("check",    listOf(line(2,8,6,12),  line(6,12,12,3), line(7,7,7,7)))
    val search   = IconSpec("search",   listOf(line(5,3,5,9),   line(3,6,7,6),   line(7,8,12,12)))
    val settings = IconSpec("settings", listOf(line(2,3,12,3),  line(2,7,12,7),  line(2,11,12,11)))
    val share    = IconSpec("share",    listOf(line(7,12,7,2),  line(3,5,7,2),   line(7,2,11,5)))
    val bookmark = IconSpec("bookmark", listOf(line(4,2,10,2),  line(4,2,4,12),  line(10,2,10,10)))
    val play     = IconSpec("play",     listOf(line(4,3,4,11),  line(4,3,11,7),  line(4,11,11,7)))
    val pause    = IconSpec("pause",    listOf(line(5,3,5,11),  line(9,3,9,11),  line(7,7,7,7)))
    val sun      = IconSpec("sun",      listOf(line(2,7,12,7),  line(7,2,7,12),  line(4,4,10,10)))
    val moon     = IconSpec("moon",     listOf(line(5,3,5,11),  line(5,3,9,4),   line(5,11,9,10)))
    val refresh  = IconSpec("refresh",  listOf(line(3,4,11,4),  line(11,4,11,10),line(11,10,7,10)))
    val sparkle  = IconSpec("sparkle",  listOf(line(7,1,7,13),  line(1,7,13,7),  line(7,7,7,7)))
    val mic      = IconSpec("mic",      listOf(line(7,2,7,9),   line(4,12,10,12),line(7,9,7,12)))
    val send     = IconSpec("send",     listOf(line(3,4,12,7),  line(3,10,12,7), line(3,7,8,7)))
    val flame    = IconSpec("flame",    listOf(line(5,12,7,3),  line(9,12,7,3),  line(5,12,9,12)))
    val globe    = IconSpec("globe",    listOf(line(7,2,7,12),  line(3,4,11,4),  line(3,10,11,10)))

    val all = listOf(
        arrowUp, arrowRight, arrowDown, arrowLeft,
        chevronUp, chevronRight, chevronDown, chevronLeft,
        plus, close,
        menu, check, search, settings, share, bookmark, play, pause,
        sun, moon, refresh, sparkle, mic, send, flame, globe
    )
    fun byId(id: String): IconSpec = all.first { it.id == id }
}

/**
 * Animated icon. Pass any [IconSpec] in; the composable will morph from the
 * previously-displayed spec to the new one with a spring.
 *
 * Rotation-group fast path: if old.rotationGroup == new.rotationGroup, only the
 * rotation animates, lines stay still.
 */
@Composable
fun MorphingIcon(
    spec: IconSpec,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = AlignedTokens.colors.text,
    strokeWidth: Dp = 2.dp
) {
    // Animatables for each of 3 lines × 4 coords, plus rotation
    val x1 = remember(spec.id) { spec.lines.map { Animatable(it.x1) } }
    val y1 = remember(spec.id) { spec.lines.map { Animatable(it.y1) } }
    val x2 = remember(spec.id) { spec.lines.map { Animatable(it.x2) } }
    val y2 = remember(spec.id) { spec.lines.map { Animatable(it.y2) } }
    val opacity = remember(spec.id) { spec.lines.map { Animatable(if (it.visible) 1f else 0f) } }
    val rotation = remember { Animatable(spec.rotationDeg) }
    var previous by remember { mutableStateOf<IconSpec?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(spec) {
        val prev = previous
        val sameGroup = prev != null && prev.rotationGroup != null && prev.rotationGroup == spec.rotationGroup
        if (sameGroup) {
            // Rotate by shortest arc
            val current = rotation.value
            val target  = shortestRotation(current, spec.rotationDeg)
            scope.launch {
                rotation.animateTo(
                    target,
                    spring(Tokens.Motion.IconRotationDamping, Tokens.Motion.IconRotationStiffness)
                )
            }
        } else {
            // Cross-group morph: animate every endpoint + opacity, snap rotation
            rotation.snapTo(spec.rotationDeg)
            val springSpec = spring<Float>(Tokens.Motion.IconSpringDamping, Tokens.Motion.IconSpringStiffness)
            spec.lines.forEachIndexed { i, line ->
                scope.launch { x1[i].animateTo(line.x1, springSpec) }
                scope.launch { y1[i].animateTo(line.y1, springSpec) }
                scope.launch { x2[i].animateTo(line.x2, springSpec) }
                scope.launch { y2[i].animateTo(line.y2, springSpec) }
                scope.launch { opacity[i].animateTo(if (line.visible) 1f else 0f, springSpec) }
            }
        }
        previous = spec
    }

    Canvas(modifier = modifier.size(size)) {
        val px = this.size.minDimension / 14f
        val strokePx = strokeWidth.toPx()
        rotate(rotation.value, pivot = center) {
            translate(
                left = (this.size.width  - 14f * px) / 2f,
                top  = (this.size.height - 14f * px) / 2f
            ) {
                for (i in 0..2) {
                    val a = opacity[i].value
                    if (a <= 0f) continue
                    drawLine(
                        color = color.copy(alpha = a * color.alpha),
                        start = Offset(x1[i].value * px, y1[i].value * px),
                        end   = Offset(x2[i].value * px, y2[i].value * px),
                        strokeWidth = strokePx,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

/** Return target in degrees so that rotating from [from] to [target] takes the shortest arc. */
private fun shortestRotation(from: Float, targetRaw: Float): Float {
    var delta = ((targetRaw - from) % 360f + 540f) % 360f - 180f
    return from + delta
}
