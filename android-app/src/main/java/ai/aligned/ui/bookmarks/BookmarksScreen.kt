package ai.aligned.ui.bookmarks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ai.aligned.net.dto.StoryDto
import ai.aligned.ui.components.AlignedPress
import ai.aligned.ui.icons.MorphingIcon
import ai.aligned.ui.icons.MorphingIcons
import ai.aligned.ui.theme.AlignedTokens
import ai.aligned.ui.theme.Tokens

@Composable
fun BookmarksScreen(
    onStory: (String) -> Unit,
    vm: BookmarksViewModel = hiltViewModel()
) {
    val stories by vm.bookmarks.collectAsState()
    val c = AlignedTokens.colors

    Column(modifier = Modifier.fillMaxSize().background(c.bg)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text("Bookmarks", color = c.text, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.5).sp)
            Text(
                if (stories.isEmpty()) "Tap the bookmark icon on any story to save it here."
                else "${stories.size} saved",
                color = c.textSecondary, fontSize = 13.sp
            )
        }
        if (stories.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                MorphingIcon(MorphingIcons.bookmark, size = 32.dp, color = c.textTertiary)
            }
        } else LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(stories, key = { it.id }) { story -> Card(story = story, onClick = { onStory(story.id) }) }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun Card(story: StoryDto, onClick: () -> Unit) {
    val c = AlignedTokens.colors
    val accent = Tokens.categoryColor(story.category)
    AlignedPress(onClick = onClick) { pressed ->
        Column(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(Tokens.Radius.card))
                .background(if (pressed) c.elev2 else c.elev1)
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(accent))
                Spacer(Modifier.width(6.dp))
                Text(story.category.uppercase(), color = c.textSecondary, fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text(story.headline, color = c.text, fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold, lineHeight = 21.sp,
                maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}
