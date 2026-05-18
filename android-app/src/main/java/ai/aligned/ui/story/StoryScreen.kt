package ai.aligned.ui.story

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ai.aligned.net.dto.TweetDto
import ai.aligned.net.dto.TweetMedia
import ai.aligned.ui.components.AlignedPress
import ai.aligned.ui.icons.IconSpec
import ai.aligned.ui.icons.MorphingIcon
import ai.aligned.ui.icons.MorphingIcons
import ai.aligned.ui.theme.AlignedTokens
import ai.aligned.ui.theme.Tokens
import coil.compose.AsyncImage

@Composable
fun StoryScreen(
    onBack: () -> Unit,
    vm: StoryViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val c = AlignedTokens.colors
    val ctx = LocalContext.current
    val handler = LocalUriHandler.current

    Column(modifier = Modifier.fillMaxSize().background(c.bg)) {
        TopBar(
            onBack = onBack,
            onShare = {
                state.story?.let { s ->
                    val url = vm.shareUrl(s.id)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, s.headline)
                        putExtra(Intent.EXTRA_TEXT, "${s.headline}\n$url")
                    }
                    ctx.startActivity(Intent.createChooser(intent, "Share story"))
                }
            },
            onBookmark = { vm.toggleBookmark() },
            bookmarked = state.bookmarked,
            canShare = state.story != null
        )
        when {
            state.loading -> Center("Loading…")
            state.error != null -> Center("Couldn't load this story.\n${state.error}", isError = true)
            state.story != null -> {
                val s = state.story!!
                val accent = Tokens.categoryColor(s.category)
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(accent))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                prettyCategory(s.category).uppercase(),
                                color = c.textSecondary, fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp
                            )
                        }
                    }
                    item {
                        Text(
                            s.headline,
                            color = c.text, fontSize = 32.sp,
                            fontWeight = FontWeight.Bold, lineHeight = 38.sp,
                            letterSpacing = (-0.4).sp
                        )
                    }
                    if (s.summary.isNotBlank()) item {
                        Text(s.summary, color = c.textSecondary, fontSize = 17.sp, lineHeight = 24.sp)
                    }
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VoteChip(
                                icon = MorphingIcons.check, label = "Useful",
                                tint = state.voteHighlight == Vote.UP, onClick = { vm.vote(true) }
                            )
                            VoteChip(
                                icon = MorphingIcons.close, label = "Not useful",
                                tint = state.voteHighlight == Vote.DOWN, onClick = { vm.vote(false) }
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "${s.tweetCount} sources",
                                color = c.textTertiary, fontSize = 12.sp
                            )
                        }
                    }
                    item {
                        AlignedPress(onClick = {
                            runCatching { handler.openUri(vm.infographicUrl(s.id)) }
                        }) { pressed ->
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(Tokens.Radius.card))
                                    .background(if (pressed) c.elev2 else c.elev1)
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(modifier = Modifier.size(36.dp)
                                    .clip(RoundedCornerShape(Tokens.Radius.tap))
                                    .background(accent.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center) {
                                    MorphingIcon(MorphingIcons.sparkle, size = 18.dp, color = accent)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("View infographic", color = c.text, fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold)
                                    Text("AI-rendered visual summary", color = c.textSecondary, fontSize = 12.sp)
                                }
                                MorphingIcon(MorphingIcons.chevronRight, size = 16.dp, color = c.textTertiary)
                            }
                        }
                    }
                    item { Divider() }
                    item {
                        Text(
                            "Source posts",
                            color = c.textSecondary, fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp
                        )
                    }
                    items(s.tweets, key = { it.id }) { t -> TweetCard(t) }
                    item { Spacer(Modifier.height(64.dp)) }
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    onBack: () -> Unit,
    onShare: () -> Unit,
    onBookmark: () -> Unit,
    bookmarked: Boolean,
    canShare: Boolean
) {
    val c = AlignedTokens.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlignedPress(onClick = onBack) { pressed ->
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(Tokens.Radius.tap))
                    .background(if (pressed) c.surface else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                MorphingIcon(spec = MorphingIcons.chevronLeft, size = 22.dp, color = c.text)
            }
        }
        Spacer(Modifier.weight(1f))
        if (canShare) {
            AlignedPress(onClick = onBookmark) { pressed ->
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(Tokens.Radius.tap))
                        .background(if (pressed) c.surface else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    MorphingIcon(
                        spec = MorphingIcons.bookmark, size = 20.dp,
                        color = if (bookmarked) c.accent else c.text
                    )
                }
            }
            AlignedPress(onClick = onShare) { pressed ->
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(Tokens.Radius.tap))
                        .background(if (pressed) c.surface else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    MorphingIcon(spec = MorphingIcons.share, size = 20.dp, color = c.text)
                }
            }
        }
    }
}

@Composable
private fun VoteChip(
    icon: IconSpec, label: String, tint: Boolean, onClick: () -> Unit
) {
    val c = AlignedTokens.colors
    val bg = if (tint) c.text else c.elev1
    val fg = if (tint) c.bg else c.text
    AlignedPress(onClick = onClick) { pressed ->
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(Tokens.Radius.chip))
                .background(bg)
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MorphingIcon(spec = icon, size = 14.dp, color = fg)
            Text(label, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun Divider() {
    val c = AlignedTokens.colors
    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(c.separator))
}

@Composable
private fun TweetCard(t: TweetDto) {
    val c = AlignedTokens.colors
    val handler = LocalUriHandler.current
    AlignedPress(
        onClick = { if (t.url.isNotEmpty()) runCatching { handler.openUri(t.url) } },
        haptic = false
    ) { pressed ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Tokens.Radius.card))
                .background(if (pressed) c.elev2 else c.elev1)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (t.authorProfileImage.isNotBlank()) AsyncImage(
                    model = t.authorProfileImage,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(c.elev2)
                )
                Column {
                    Text(t.authorName, color = c.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("@${t.authorUsername}", color = c.textTertiary, fontSize = 12.sp)
                }
            }
            Text(t.text, color = c.text, fontSize = 15.sp, lineHeight = 21.sp)
            val photos = t.media.filter { it.type == "photo" }
            if (photos.isNotEmpty()) {
                if (photos.size == 1) MediaSingle(photos.first())
                else MediaRow(photos)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                StatGlyph(MorphingIcons.heart,   t.likes)
                StatGlyph(MorphingIcons.retweet, t.retweets)
                StatGlyph(MorphingIcons.reply,   t.replies)
                StatGlyph(MorphingIcons.eye,     t.views)
            }
        }
    }
}

@Composable
private fun MediaSingle(m: TweetMedia) {
    val c = AlignedTokens.colors
    AsyncImage(
        model = m.url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(Tokens.Radius.tap))
            .background(c.elev2)
    )
}

@Composable
private fun MediaRow(items: List<TweetMedia>) {
    val c = AlignedTokens.colors
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(items) { m ->
            AsyncImage(
                model = m.url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 180.dp, height = 140.dp)
                    .clip(RoundedCornerShape(Tokens.Radius.tap))
                    .background(c.elev2)
            )
        }
    }
}

@Composable
private fun StatGlyph(icon: IconSpec, value: Long) {
    val c = AlignedTokens.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        MorphingIcon(spec = icon, size = 12.dp, color = c.textTertiary)
        Text(format(value), color = c.textTertiary, fontSize = 12.sp)
    }
}

@Composable
private fun Center(msg: String, isError: Boolean = false) {
    val c = AlignedTokens.colors
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            msg,
            color = if (isError) c.destructive else c.textSecondary,
            modifier = Modifier.padding(32.dp)
        )
    }
}

private fun prettyCategory(id: String): String =
    id.split('-').joinToString(" ") { it.replaceFirstChar(Char::uppercase) }

private fun format(v: Long): String = when {
    v < 1_000 -> "$v"
    v < 1_000_000 -> "${v / 1_000}K"
    else -> "${(v / 100_000) / 10.0}M"
}
