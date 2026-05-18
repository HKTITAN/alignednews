package ai.aligned.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import ai.aligned.net.dto.HistoryEntryDto
import ai.aligned.ui.components.SkeletonCard
import ai.aligned.ui.icons.MorphingIcon
import ai.aligned.ui.icons.MorphingIcons
import ai.aligned.ui.theme.AlignedTokens
import ai.aligned.ui.theme.Tokens

@Composable
fun HistoryScreen(vm: HistoryViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val c = AlignedTokens.colors

    Column(modifier = Modifier.fillMaxSize().background(c.bg)) {
        Header(state)
        when (val s = state) {
            HistoryState.Loading -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) { items(4) { SkeletonCard(heightDp = 72) } }
            is HistoryState.Error -> Center(s.message, isError = true)
            is HistoryState.Ready -> {
                if (s.entries.isEmpty()) Center("No history yet.")
                else LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(s.entries, key = { it.id }) { entry -> HistoryCard(entry) }
                    item { Spacer(Modifier.height(40.dp)) }
                }
            }
        }
    }
}

@Composable
private fun Header(state: HistoryState) {
    val c = AlignedTokens.colors
    val count = (state as? HistoryState.Ready)?.entries?.size ?: 0
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text("History", color = c.text, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.5).sp)
        Text(
            if (count > 0) "$count past sessions" else "Past research and chat",
            color = c.textSecondary, fontSize = 13.sp
        )
    }
}

@Composable
private fun HistoryCard(e: HistoryEntryDto) {
    val c = AlignedTokens.colors
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(Tokens.Radius.card))
            .background(c.elev1)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MorphingIcon(
                spec = if (e.type == "research") MorphingIcons.sparkle else MorphingIcons.mic,
                size = 12.dp, color = c.textSecondary
            )
            Text(e.type.uppercase(), color = c.textSecondary, fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
            if (e.completedAt.isNotEmpty()) {
                Text("· ${e.completedAt}", color = c.textTertiary, fontSize = 11.sp)
            }
        }
        Text(e.query, color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp, maxLines = 3)
        if (e.insightCount > 0 || e.confidence > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (e.insightCount > 0) Text("${e.insightCount} insights", color = c.textTertiary, fontSize = 12.sp)
                if (e.confidence > 0) Text("${e.confidence}% confidence", color = c.textTertiary, fontSize = 12.sp)
            }
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
