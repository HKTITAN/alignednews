package ai.aligned.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ai.aligned.net.dto.StoryDto
import ai.aligned.ui.icons.MorphingIcon
import ai.aligned.ui.icons.MorphingIcons
import ai.aligned.ui.theme.AlignedTokens
import ai.aligned.ui.theme.Tokens

@Composable
fun FeedScreen(
    onStory: (String) -> Unit,
    vm: FeedViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val listState = rememberLazyListState()
    val c = AlignedTokens.colors

    Column(modifier = Modifier.fillMaxSize().background(c.bg)) {
        TopHeader(state = state)
        when (val s = state) {
            FeedState.Loading -> CenterText("Loading…")
            is FeedState.Error -> CenterText("Couldn't reach alignednews.ai\n${s.message}", isError = true)
            is FeedState.Ready -> {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (s.refreshing) item {
                        Text("Refreshing…", color = AlignedTokens.colors.textTertiary,
                             fontSize = 12.sp,
                             modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
                    }
                    items(s.stories, key = { it.id }) { story ->
                        StoryCard(story = story, onClick = { onStory(story.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TopHeader(state: FeedState) {
    val c = AlignedTokens.colors
    val count = (state as? FeedState.Ready)?.stories?.size ?: 0
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("ALIGNED",
                color = c.text, fontWeight = FontWeight.Bold, fontSize = 28.sp,
                letterSpacing = (-0.5).sp)
            Text("$count stories · realtime",
                color = c.textSecondary, fontSize = 13.sp)
        }
        MorphingIcon(spec = MorphingIcons.search, size = 22.dp, color = c.text)
    }
}

@Composable
private fun StoryCard(story: StoryDto, onClick: () -> Unit) {
    val c = AlignedTokens.colors
    val accent = Tokens.categoryColor(story.category)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Tokens.Radius.card))
            .background(c.elev1)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        // category chip
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier
                .size(width = 8.dp, height = 8.dp)
                .background(accent, RoundedCornerShape(100))
            )
            Spacer(Modifier.width(8.dp))
            Text(story.category, color = c.textSecondary, fontSize = 11.sp,
                fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
            Spacer(Modifier.weight(1f))
            Text("${story.tweetCount}",
                color = c.textTertiary, fontSize = 11.sp)
        }
        Spacer(Modifier.height(10.dp))
        Text(story.headline,
            color = c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
            lineHeight = 22.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        if (story.summary.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(story.summary,
                color = c.textSecondary, fontSize = 15.sp, lineHeight = 20.sp,
                maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun CenterText(msg: String, isError: Boolean = false) {
    val c = AlignedTokens.colors
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(msg, color = if (isError) c.destructive else c.textSecondary)
    }
}
