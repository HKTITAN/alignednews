package ai.aligned.ui.story

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ai.aligned.net.dto.TweetDto
import ai.aligned.ui.icons.MorphingIcon
import ai.aligned.ui.icons.MorphingIcons
import ai.aligned.ui.theme.AlignedTokens
import ai.aligned.ui.theme.Tokens

@Composable
fun StoryScreen(
    onBack: () -> Unit,
    vm: StoryViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val c = AlignedTokens.colors

    Column(modifier = Modifier.fillMaxSize().background(c.bg)) {
        TopBar(onBack = onBack)
        when {
            state.loading -> Center("Loading…")
            state.error != null -> Center("Couldn't load: ${state.error}", isError = true)
            state.story != null -> {
                val s = state.story!!
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(s.category, color = c.textSecondary, fontSize = 11.sp,
                            fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
                    }
                    item {
                        Text(s.headline, color = c.text, fontSize = 28.sp,
                            fontWeight = FontWeight.Bold, lineHeight = 34.sp,
                            letterSpacing = (-0.3).sp)
                    }
                    if (s.summary.isNotBlank()) item {
                        Text(s.summary, color = c.textSecondary, fontSize = 17.sp, lineHeight = 24.sp)
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            VoteChip(icon = MorphingIcons.check, label = "Useful",
                                color = c.text, onClick = { vm.vote(true) })
                            VoteChip(icon = MorphingIcons.close, label = "Not useful",
                                color = c.text, onClick = { vm.vote(false) })
                            if (state.lastVote.isNotEmpty()) {
                                Text(state.lastVote, color = c.textSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(c.separator))
                    }
                    item {
                        Text("Source posts", color = c.textSecondary, fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold)
                    }
                    items(s.tweets, key = { it.id }) { t -> TweetCard(t) }
                }
            }
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    val c = AlignedTokens.colors
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(Tokens.Radius.tap))
            .clickable(onClick = onBack),
            contentAlignment = Alignment.Center) {
            MorphingIcon(spec = MorphingIcons.chevronLeft, size = 22.dp, color = c.text)
        }
    }
}

@Composable
private fun VoteChip(
    icon: ai.aligned.ui.icons.IconSpec,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    val c = AlignedTokens.colors
    Row(modifier = Modifier
        .clip(RoundedCornerShape(100))
        .background(c.elev1)
        .clickable(onClick = onClick)
        .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        MorphingIcon(spec = icon, size = 16.dp, color = color)
        Text(label, color = color, fontSize = 14.sp)
    }
}

@Composable
private fun TweetCard(t: TweetDto) {
    val c = AlignedTokens.colors
    Column(modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(Tokens.Radius.card))
        .background(c.elev1)
        .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(t.authorName, color = c.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("@${t.authorUsername}", color = c.textSecondary, fontSize = 13.sp)
        }
        Text(t.text, color = c.text, fontSize = 15.sp, lineHeight = 20.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("♥ ${t.likes}", color = c.textTertiary, fontSize = 12.sp)
            Text("↻ ${t.retweets}", color = c.textTertiary, fontSize = 12.sp)
            Text("💬 ${t.replies}", color = c.textTertiary, fontSize = 12.sp)
            Text("👁 ${t.views}", color = c.textTertiary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun Center(msg: String, isError: Boolean = false) {
    val c = AlignedTokens.colors
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(msg, color = if (isError) c.destructive else c.textSecondary,
            modifier = Modifier.padding(32.dp))
    }
}
