package ai.aligned.ui.summarize

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ai.aligned.ui.components.AlignedPress
import ai.aligned.ui.icons.MorphingIcon
import ai.aligned.ui.icons.MorphingIcons
import ai.aligned.ui.theme.AlignedTokens
import ai.aligned.ui.theme.Tokens

@Composable
fun SummarizeScreen(
    onBack: () -> Unit = {},
    onStory: (String) -> Unit = {},
    vm: SummarizeViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val c = AlignedTokens.colors

    Column(modifier = Modifier.fillMaxSize().background(c.bg)) {
        TopBar(onBack = onBack)
        Header(state = state, onClear = vm::clear)
        Composer(
            text = vm.input.value,
            loading = state.loading,
            onText = vm::setInput,
            onSubmit = vm::submit
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            state.error?.let { err ->
                item {
                    Text(err, color = c.destructive, fontSize = 13.sp,
                         modifier = Modifier.padding(horizontal = 4.dp))
                }
            }
            if (state.overview.isBlank() && !state.loading && state.error == null) {
                item { Hint() }
            }
            if (state.loading) {
                item { LoadingShimmer() }
            }
            if (state.overview.isNotBlank()) {
                item { SourceChip(state.source) }
                item { OverviewCard(state.overview) }
                if (state.perTweet.isNotEmpty()) {
                    item {
                        Text("KEY POINTS", color = c.textSecondary, fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
                    }
                    items(state.perTweet) { line -> BulletCard(line) }
                }
                state.story?.let { story ->
                    item {
                        AlignedPress(onClick = { onStory(story.id) }) { pressed ->
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(Tokens.Radius.card))
                                    .background(if (pressed) c.elev2 else c.elev1)
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Tokens.categoryColor(story.category)))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("OPEN STORY", color = c.textSecondary, fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
                                    Text(story.headline, color = c.text, fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold, maxLines = 2)
                                }
                                MorphingIcon(MorphingIcons.chevronRight, size = 16.dp, color = c.textTertiary)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    val c = AlignedTokens.colors
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        AlignedPress(onClick = onBack) { pressed ->
            Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(Tokens.Radius.tap))
                .background(if (pressed) c.surface else Color.Transparent),
                contentAlignment = Alignment.Center) {
                MorphingIcon(MorphingIcons.chevronLeft, size = 22.dp, color = c.text)
            }
        }
    }
}

@Composable
private fun Header(state: SummarizeState, onClear: () -> Unit) {
    val c = AlignedTokens.colors
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Summarize", color = c.text, fontWeight = FontWeight.Bold,
                fontSize = 28.sp, letterSpacing = (-0.5).sp)
            Text(
                when {
                    state.loading        -> "Resolving thread…"
                    state.overview.isNotBlank() -> "${state.tweets.size} tweets summarized"
                    else                 -> "Paste any X/Twitter URL"
                },
                color = c.textSecondary, fontSize = 13.sp
            )
        }
        if (state.overview.isNotBlank() || state.error != null) {
            AlignedPress(onClick = onClear) { pressed ->
                Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(Tokens.Radius.tap))
                    .background(if (pressed) c.surface else Color.Transparent),
                    contentAlignment = Alignment.Center) {
                    MorphingIcon(MorphingIcons.close, size = 18.dp, color = c.text)
                }
            }
        }
    }
}

@Composable
private fun Composer(text: String, loading: Boolean, onText: (String) -> Unit, onSubmit: () -> Unit) {
    val c = AlignedTokens.colors
    val canSubmit = text.isNotBlank() && !loading
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        .clip(RoundedCornerShape(Tokens.Radius.chip))
        .background(c.elev1)
        .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MorphingIcon(MorphingIcons.share, size = 16.dp, color = c.textSecondary)
        Box(modifier = Modifier.weight(1f)) {
            if (text.isEmpty()) Text(
                "https://x.com/…/status/… or tweet ID",
                color = c.textTertiary, fontSize = 15.sp
            )
            BasicTextField(
                value = text, onValueChange = onText,
                textStyle = TextStyle(color = c.text, fontSize = 15.sp),
                cursorBrush = SolidColor(c.accent),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        AlignedPress(onClick = { if (canSubmit) onSubmit() }) { pressed ->
            Box(modifier = Modifier.size(36.dp).clip(CircleShape)
                .background(if (canSubmit) c.accent else c.elev2),
                contentAlignment = Alignment.Center) {
                MorphingIcon(
                    spec = MorphingIcons.sparkle, size = 16.dp,
                    color = if (canSubmit) Color.White else c.textTertiary
                )
            }
        }
    }
}

@Composable
private fun Hint() {
    val c = AlignedTokens.colors
    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
        Text("Paste a tweet link (or tweet ID) to summarize the whole thread.",
            color = c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            "If the tweet is part of a story alignednews.ai already follows, we'll pull every related quote and reply. Otherwise we'll search the feed.",
            color = c.textSecondary, fontSize = 14.sp, lineHeight = 20.sp
        )
    }
}

@Composable
private fun LoadingShimmer() {
    val c = AlignedTokens.colors
    Column(modifier = Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(Tokens.Radius.card))
        .background(c.elev1)
        .padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(modifier = Modifier.fillMaxWidth(0.5f).height(12.dp)
            .clip(RoundedCornerShape(6.dp)).background(c.elev2))
        Box(modifier = Modifier.fillMaxWidth(0.9f).height(12.dp)
            .clip(RoundedCornerShape(6.dp)).background(c.elev2))
        Box(modifier = Modifier.fillMaxWidth(0.7f).height(12.dp)
            .clip(RoundedCornerShape(6.dp)).background(c.elev2))
    }
}

@Composable
private fun SourceChip(source: String) {
    val c = AlignedTokens.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(horizontal = 4.dp)) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(c.accent))
        Text(source.uppercase(), color = c.textSecondary, fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
    }
}

@Composable
private fun OverviewCard(overview: String) {
    val c = AlignedTokens.colors
    Column(modifier = Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(Tokens.Radius.card))
        .background(c.elev1)
        .padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("OVERVIEW", color = c.textSecondary, fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
        Text(overview, color = c.text, fontSize = 15.sp, lineHeight = 22.sp)
    }
}

@Composable
private fun BulletCard(line: String) {
    val c = AlignedTokens.colors
    Row(modifier = Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(Tokens.Radius.card))
        .background(c.elev1)
        .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(c.textTertiary)
            .padding(top = 8.dp))
        Text(line, color = c.text, fontSize = 14.sp, lineHeight = 20.sp)
    }
}
