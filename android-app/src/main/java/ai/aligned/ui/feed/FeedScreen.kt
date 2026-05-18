package ai.aligned.ui.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ai.aligned.net.dto.StoryDto
import ai.aligned.ui.components.AlignedPress
import ai.aligned.ui.components.SkeletonCard
import ai.aligned.ui.icons.MorphingIcon
import ai.aligned.ui.icons.MorphingIcons
import ai.aligned.ui.theme.AlignedTokens
import ai.aligned.ui.theme.Tokens
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onStory: (String) -> Unit,
    onSearch: () -> Unit = {},
    vm: FeedViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val listState = rememberLazyListState()
    val refreshState = rememberPullToRefreshState()
    val c = AlignedTokens.colors

    val refreshing = (state as? FeedState.Ready)?.refreshing == true

    Column(modifier = Modifier.fillMaxSize().background(c.bg)) {
        TopHeader(state = state, onSearch = onSearch, onRefresh = vm::refresh, refreshing = refreshing)
        CategoryFilterRow(
            categories = vm.knownCategories,
            selected = vm.selectedCategory.value,
            onSelected = vm::selectCategory,
            label = vm::categoryLabel,
            color = vm::categoryColor
        )

        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = vm::refresh,
            state = refreshState,
            modifier = Modifier.fillMaxSize()
        ) {
            when (val s = state) {
                FeedState.Loading -> SkeletonList()
                is FeedState.Error -> ErrorView(message = s.message, onRetry = vm::refresh)
                is FeedState.Ready -> {
                    val stories = s.stories.let { all ->
                        val cat = vm.selectedCategory.value
                        if (cat == null) all else all.filter { it.category == cat }
                    }
                    if (stories.isEmpty()) EmptyView()
                    else LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(stories, key = { it.id }) { story ->
                            StoryCard(story = story, onClick = { onStory(story.id) })
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopHeader(
    state: FeedState,
    onSearch: () -> Unit,
    onRefresh: () -> Unit,
    refreshing: Boolean
) {
    val c = AlignedTokens.colors
    val count = (state as? FeedState.Ready)?.stories?.size ?: 0
    val statusText = when {
        refreshing -> "Refreshing…"
        state is FeedState.Error -> "Offline · showing cache"
        count > 0 -> "$count stories · live"
        else -> "Loading"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "ALIGNED",
                color = c.text, fontWeight = FontWeight.Bold, fontSize = 28.sp,
                letterSpacing = (-0.5).sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                LiveDot(active = !refreshing && state is FeedState.Ready)
                Spacer(Modifier.width(6.dp))
                Text(statusText, color = c.textSecondary, fontSize = 13.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HeaderIconButton(MorphingIcons.search, onSearch)
            HeaderIconButton(
                if (refreshing) MorphingIcons.sparkle else MorphingIcons.refresh,
                onRefresh
            )
        }
    }
}

@Composable
private fun LiveDot(active: Boolean) {
    val c = AlignedTokens.colors
    val alpha by animateFloatAsState(
        if (active) 1f else 0.4f,
        spring(Tokens.Motion.SpringDamping, Tokens.Motion.SpringStiffness),
        label = "livedot"
    )
    Box(
        modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background((if (active) Tokens.Palette.success else c.textTertiary).copy(alpha = alpha))
    )
}

@Composable
private fun HeaderIconButton(spec: ai.aligned.ui.icons.IconSpec, onClick: () -> Unit) {
    val c = AlignedTokens.colors
    AlignedPress(onClick = onClick) { pressed ->
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(Tokens.Radius.tap))
                .background(if (pressed) c.surface else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            MorphingIcon(spec = spec, size = 22.dp, color = c.text)
        }
    }
}

@Composable
private fun CategoryFilterRow(
    categories: List<String>,
    selected: String?,
    onSelected: (String?) -> Unit,
    label: (String) -> String,
    color: (String) -> Color
) {
    if (categories.isEmpty()) return
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item("__all__") {
            Chip(label = "All", selected = selected == null) { onSelected(null) }
        }
        items(categories, key = { it }) { cat ->
            Chip(
                label = label(cat),
                dotColor = color(cat),
                selected = selected == cat
            ) { onSelected(if (selected == cat) null else cat) }
        }
    }
}

@Composable
private fun Chip(
    label: String,
    selected: Boolean,
    dotColor: Color? = null,
    onClick: () -> Unit
) {
    val c = AlignedTokens.colors
    val bg = if (selected) c.text else c.elev1
    val fg = if (selected) c.bg else c.text
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(Tokens.Radius.chip))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (dotColor != null) Box(
            modifier = Modifier.size(7.dp).clip(CircleShape).background(dotColor)
        )
        Text(label, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StoryCard(story: StoryDto, onClick: () -> Unit) {
    val c = AlignedTokens.colors
    val accent = Tokens.categoryColor(story.category)
    val image = story.tweets.firstOrNull { t -> t.media.any { it.type == "photo" } }
        ?.media?.firstOrNull { it.type == "photo" }?.url

    AlignedPress(onClick = onClick) { pressed ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .scale(if (pressed) 0.985f else 1f)
                .clip(RoundedCornerShape(Tokens.Radius.card))
                .background(c.elev1)
                .padding(16.dp)
        ) {
            // category strip
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    prettyCategory(story.category).uppercase(),
                    color = c.textSecondary, fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${story.tweetCount} posts · ${formatEngagement(story.totalEngagement)}",
                    color = c.textTertiary, fontSize = 11.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                story.headline,
                color = c.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                lineHeight = 23.sp, maxLines = 3, overflow = TextOverflow.Ellipsis,
                letterSpacing = (-0.2).sp
            )
            if (story.summary.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    story.summary,
                    color = c.textSecondary, fontSize = 14.sp, lineHeight = 19.sp,
                    maxLines = 3, overflow = TextOverflow.Ellipsis
                )
            }
            if (image != null) {
                Spacer(Modifier.height(12.dp))
                AsyncImage(
                    model = image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(Tokens.Radius.tap))
                        .background(c.elev2)
                )
            }
        }
    }
}

@Composable
private fun SkeletonList() {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(6) { SkeletonCard() }
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    val c = AlignedTokens.colors
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MorphingIcon(spec = MorphingIcons.flame, size = 32.dp, color = c.destructive)
            Spacer(Modifier.height(12.dp))
            Text("Couldn't reach alignednews.ai", color = c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(message, color = c.textSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(Tokens.Radius.chip))
                    .background(c.accent)
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MorphingIcon(spec = MorphingIcons.refresh, size = 14.dp, color = Color.White)
                Text("Try again", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun EmptyView() {
    val c = AlignedTokens.colors
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MorphingIcon(spec = MorphingIcons.sparkle, size = 28.dp, color = c.textTertiary)
            Spacer(Modifier.height(8.dp))
            Text("Nothing here yet.", color = c.textSecondary, fontSize = 15.sp)
        }
    }
}

private fun prettyCategory(id: String): String =
    id.split('-').joinToString(" ") { it.replaceFirstChar(Char::uppercase) }

private fun formatEngagement(total: Long): String = when {
    total < 1_000 -> "$total"
    total < 1_000_000 -> "${total / 1_000}K"
    else -> "${(total / 100_000) / 10.0}M"
}
