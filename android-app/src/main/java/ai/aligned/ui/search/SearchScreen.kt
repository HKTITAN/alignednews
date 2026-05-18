package ai.aligned.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ai.aligned.ui.components.AlignedPress
import ai.aligned.ui.icons.MorphingIcon
import ai.aligned.ui.icons.MorphingIcons
import ai.aligned.ui.theme.AlignedTokens
import ai.aligned.ui.theme.Tokens

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onStory: (String) -> Unit,
    vm: SearchViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val c = AlignedTokens.colors
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(modifier = Modifier.fillMaxSize().background(c.bg)) {
        SearchBar(
            text = vm.query.value,
            onText = vm::onQuery,
            onBack = onBack,
            onClear = { vm.onQuery("") },
            focusRequester = focusRequester
        )
        when (val s = state) {
            SearchState.Idle -> EmptyHint()
            SearchState.Searching -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Searching…", color = c.textSecondary, fontSize = 14.sp)
            }
            is SearchState.Results -> {
                if (s.stories.isEmpty()) NoResults(s.query)
                else LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(s.stories, key = { it.id }) { story ->
                        AlignedPress(onClick = { onStory(story.id) }) { pressed ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(Tokens.Radius.card))
                                    .background(if (pressed) c.elev2 else c.elev1)
                                    .padding(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp).clip(CircleShape)
                                            .background(Tokens.categoryColor(story.category))
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        story.category.uppercase(),
                                        color = c.textSecondary, fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(story.headline, color = c.text, fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold, lineHeight = 20.sp, maxLines = 3)
                            }
                        }
                    }
                }
            }
            is SearchState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(s.message, color = c.destructive, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun SearchBar(
    text: String,
    onText: (String) -> Unit,
    onBack: () -> Unit,
    onClear: () -> Unit,
    focusRequester: FocusRequester
) {
    val c = AlignedTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AlignedPress(onClick = onBack) { pressed ->
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(Tokens.Radius.tap))
                    .background(if (pressed) c.surface else Color.Transparent),
                contentAlignment = Alignment.Center
            ) { MorphingIcon(MorphingIcons.chevronLeft, size = 22.dp, color = c.text) }
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(Tokens.Radius.chip))
                .background(c.elev1)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MorphingIcon(MorphingIcons.search, size = 16.dp, color = c.textSecondary)
            Box(modifier = Modifier.weight(1f)) {
                if (text.isEmpty()) Text(
                    "Search stories…", color = c.textTertiary, fontSize = 15.sp
                )
                BasicTextField(
                    value = text,
                    onValueChange = onText,
                    textStyle = TextStyle(color = c.text, fontSize = 15.sp),
                    singleLine = true,
                    cursorBrush = SolidColor(c.accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                )
            }
            if (text.isNotEmpty()) AlignedPress(onClick = onClear, haptic = false) { _ ->
                Box(
                    modifier = Modifier.size(20.dp).clip(CircleShape).background(c.elev2),
                    contentAlignment = Alignment.Center
                ) { MorphingIcon(MorphingIcons.close, size = 10.dp, color = c.textSecondary) }
            }
        }
    }
}

@Composable
private fun EmptyHint() {
    val c = AlignedTokens.colors
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MorphingIcon(spec = MorphingIcons.search, size = 28.dp, color = c.textTertiary)
            Spacer(Modifier.height(8.dp))
            Text("Search the live AI news feed.", color = c.textSecondary, fontSize = 14.sp)
        }
    }
}

@Composable
private fun NoResults(query: String) {
    val c = AlignedTokens.colors
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MorphingIcon(spec = MorphingIcons.sparkle, size = 28.dp, color = c.textTertiary)
            Spacer(Modifier.height(8.dp))
            Text("No matches for \"$query\".", color = c.textSecondary, fontSize = 14.sp)
        }
    }
}
