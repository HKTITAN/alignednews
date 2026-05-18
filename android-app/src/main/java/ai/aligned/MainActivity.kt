package ai.aligned

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import ai.aligned.nav.AlignedNavHost
import ai.aligned.ui.theme.AlignedTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Entry point. Handles two extra-curricular intents:
 *  - `aligned://story/{id}` deep link → routed by NavHost.navDeepLink.
 *  - `Intent.ACTION_SEND text/plain` → if the shared text contains an X/Twitter
 *    URL, prefill the Summarize screen with it.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val sharedUrl: MutableState<String?> = mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sharedUrl.value = extractSharedUrl(intent)
        setContent {
            AlignedTheme {
                Surface { AlignedNavHost(sharedUrl = sharedUrl.value) }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        sharedUrl.value = extractSharedUrl(intent)
    }

    private fun extractSharedUrl(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_SEND) return null
        val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return null
        // Pick out the first URL fragment in the shared text.
        val match = Regex("""https?://\S+""").find(text)?.value ?: return null
        return match
    }
}
