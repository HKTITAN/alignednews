package ai.aligned.ui.events

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ai.aligned.net.dto.EventDto
import ai.aligned.ui.components.SkeletonCard
import ai.aligned.ui.icons.MorphingIcon
import ai.aligned.ui.icons.MorphingIcons
import ai.aligned.ui.theme.AlignedTokens
import ai.aligned.ui.theme.Tokens

@Composable
fun EventsScreen(vm: EventsViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val c = AlignedTokens.colors
    val handler = LocalUriHandler.current

    Column(modifier = Modifier.fillMaxSize().background(c.bg)) {
        Header(state)
        when (val s = state) {
            EventsState.Loading -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) { items(5) { SkeletonCard(heightDp = 80) } }
            is EventsState.Error -> Center(s.message, isError = true)
            is EventsState.Ready -> {
                if (s.events.isEmpty()) Center("No upcoming events.")
                else LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(s.events, key = { it.id }) { e ->
                        EventCard(e) {
                            e.source?.tweetUrl?.takeIf { it.isNotEmpty() }?.let {
                                runCatching { handler.openUri(it) }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(40.dp)) }
                }
            }
        }
    }
}

@Composable
private fun Header(state: EventsState) {
    val c = AlignedTokens.colors
    val count = (state as? EventsState.Ready)?.events?.size ?: 0
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text("Events", color = c.text, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.5).sp)
        Text(
            if (count > 0) "$count upcoming · live" else "Conferences, launches, deadlines",
            color = c.textSecondary, fontSize = 13.sp
        )
    }
}

@Composable
private fun EventCard(e: EventDto, onClick: () -> Unit) {
    val c = AlignedTokens.colors
    val accent = Tokens.categoryColor(e.category)
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(Tokens.Radius.card))
            .background(c.elev1)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(accent))
            Text(e.date, color = c.textSecondary, fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
            if (e.location.isNotBlank()) {
                Text("· ${e.location}", color = c.textTertiary, fontSize = 11.sp)
            }
        }
        Text(e.name, color = c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp)
        if (e.description.isNotBlank()) {
            Text(e.description, color = c.textSecondary, fontSize = 14.sp, lineHeight = 20.sp, maxLines = 3)
        }
    }
}

@Composable
private fun Center(msg: String, isError: Boolean = false) {
    val c = AlignedTokens.colors
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(msg, color = if (isError) c.destructive else c.textSecondary, modifier = Modifier.padding(32.dp))
    }
}
