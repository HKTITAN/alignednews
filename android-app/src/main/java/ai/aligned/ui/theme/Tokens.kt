package ai.aligned.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Hand-mirrored from design/tokens.json. Keep in sync — the JSON is canonical.
 */
object Tokens {
    object Color {
        // Light
        val lightBg            = Color(0xFFFFFFFF)
        val lightSurface       = Color(0xFFF5F5F7)
        val lightElev1         = Color(0xFFFFFFFF)
        val lightElev2         = Color(0xFFFBFBFD)
        val lightText          = Color(0xFF1D1D1F)
        val lightTextSecondary = Color(0xFF6E6E73)
        val lightTextTertiary  = Color(0xFF86868B)
        val lightSeparator     = Color(0xFFD2D2D7)

        // Dark
        val darkBg             = Color(0xFF000000)
        val darkSurface        = Color(0xFF1C1C1E)
        val darkElev1          = Color(0xFF2C2C2E)
        val darkElev2          = Color(0xFF3A3A3C)
        val darkText           = Color(0xFFF5F5F7)
        val darkTextSecondary  = Color(0xFF98989D)
        val darkTextTertiary   = Color(0xFF6E6E73)
        val darkSeparator      = Color(0xFF38383A)

        val accent      = Color(0xFF0A84FF)
        val destructive = Color(0xFFFF3B30)
        val success     = Color(0xFF34C759)
        val warning     = Color(0xFFFF9500)
    }

    object Space {
        val s1 = 4.dp;  val s2 = 8.dp;   val s3 = 12.dp;  val s4 = 16.dp
        val s5 = 20.dp; val s6 = 24.dp;  val s7 = 32.dp;  val s8 = 40.dp
        val s9 = 48.dp; val s10 = 64.dp; val s11 = 80.dp
    }

    object Radius { val tap = 12.dp; val card = 16.dp; val sheet = 24.dp; val chip = 100.dp; val icon = 6.dp }

    object Type {
        data class Spec(val sizeSp: Int, val lineSp: Int, val weight: FontWeight, val trackingEm: Float)
        val displayLg = Spec(34, 41, FontWeight.Bold,     -0.012f)
        val displayMd = Spec(28, 34, FontWeight.Bold,     -0.011f)
        val title     = Spec(22, 28, FontWeight.SemiBold, -0.009f)
        val titleSm   = Spec(17, 22, FontWeight.SemiBold, -0.006f)
        val body      = Spec(17, 22, FontWeight.Normal,   -0.006f)
        val bodyEmph  = Spec(17, 22, FontWeight.SemiBold, -0.006f)
        val callout   = Spec(16, 21, FontWeight.Normal,    0f)
        val subhead   = Spec(15, 20, FontWeight.Normal,    0f)
        val footnote  = Spec(13, 18, FontWeight.Normal,    0f)
        val caption   = Spec(12, 16, FontWeight.Normal,    0f)
    }

    object Motion {
        const val SpringStiffness = 380f
        const val SpringDamping = 30f
        const val SpringSnappyStiffness = 600f
        const val SpringSnappyDamping = 36f
        const val IconSpringStiffness = 420f
        const val IconSpringDamping = 32f
        const val IconRotationStiffness = 500f
        const val IconRotationDamping = 30f
    }

    /** Category colors — fallback only. The live values come from /api/categories. */
    val CategoryFallback: Map<String, Color> = mapOf(
        "ai-headlines" to Color(0xFF3B82F6), "ai-companies" to Color(0xFF6366F1),
        "ai-papers"    to Color(0xFFA78BFA), "ai-models"    to Color(0xFF818CF8),
        "ai-events"    to Color(0xFF0EA5E9), "ai-scoble"    to Color(0xFFF59E0B),
        "ai-conversations" to Color(0xFF10B981), "ai-safety" to Color(0xFFF87171),
        "ai-robotics"  to Color(0xFFFB923C), "ai-vehicles"  to Color(0xFF16A34A),
        "creative-ai"  to Color(0xFFF472B6), "ai-decentralized" to Color(0xFFEAB308),
        "ai-spatial"   to Color(0xFF14B8A6), "ai-videos"    to Color(0xFF8B5CF6),
        "ai-agents"    to Color(0xFF06B6D4), "ai-entrepreneurs" to Color(0xFFA855F7),
        "ai-investors" to Color(0xFF34D399), "ai-neuro"     to Color(0xFFEC4899),
        "world-models" to Color(0xFF2DD4BF), "xai-news"     to Color(0xFF1D4ED8),
        "quantum"      to Color(0xFF7C3AED), "dev-tools"    to Color(0xFF0891B2)
    )
    fun categoryColor(id: String): Color = CategoryFallback[id] ?: Color.accent
}

private val androidx.compose.ui.graphics.Color.Companion.accent get() = Color(0xFF0A84FF)
