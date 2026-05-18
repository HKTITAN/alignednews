package ai.aligned.ui.lists

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ai.aligned.ui.components.SkeletonCard
import ai.aligned.ui.icons.MorphingIcon
import ai.aligned.ui.icons.MorphingIcons
import ai.aligned.ui.theme.AlignedTokens
import ai.aligned.ui.theme.Tokens

@Composable
fun BriefScreen(vm: BriefViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val c = AlignedTokens.colors

    Column(modifier = Modifier.fillMaxSize().background(c.bg)) {
        Header(state)
        when (val s = state) {
            BriefState.Loading -> Loading()
            is BriefState.Error -> ErrorView(s.message, vm::refresh)
            is BriefState.Ready -> Content(s)
        }
    }
}

@Composable
private fun Header(state: BriefState) {
    val c = AlignedTokens.colors
    val date = (state as? BriefState.Ready)?.dateLabel ?: "Today"
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text("Today's Brief", color = c.text, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.5).sp)
        Text(date, color = c.textSecondary, fontSize = 13.sp)
    }
}

@Composable
private fun Loading() {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) { items(4) { SkeletonCard(heightDp = 96) } }
}

@Composable
private fun ErrorView(msg: String, onRetry: () -> Unit) {
    val c = AlignedTokens.colors
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MorphingIcon(spec = MorphingIcons.flame, size = 28.dp, color = c.destructive)
            Spacer(Modifier.height(8.dp))
            Text("Brief unavailable", color = c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(msg, color = c.textSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            Text("Try again", color = c.accent,
                fontSize = 14.sp,
                modifier = Modifier.clip(RoundedCornerShape(Tokens.Radius.chip))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun Content(s: BriefState.Ready) {
    val c = AlignedTokens.colors
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        s.execSummary?.takeIf { it.isNotBlank() }?.let { execBody ->
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Tokens.Radius.card))
                        .background(c.elev1)
                        .padding(16.dp)
                ) {
                    Text(
                        "EXECUTIVE SUMMARY",
                        color = c.textSecondary, fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(execBody, color = c.text, fontSize = 15.sp, lineHeight = 22.sp)
                }
            }
        }
        items(s.groups, key = { it.id }) { g -> GroupCard(g) }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun GroupCard(g: BriefGroup) {
    val c = AlignedTokens.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Tokens.Radius.card))
            .background(c.elev1)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(g.color))
            Spacer(Modifier.width(8.dp))
            Text(g.name, color = c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1).sp)
            Spacer(Modifier.weight(1f))
            Text("${g.storyCount}", color = c.textTertiary, fontSize = 12.sp)
        }
        if (g.summary.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(g.summary, color = c.textSecondary, fontSize = 14.sp, lineHeight = 20.sp, maxLines = 4)
        }
    }
}
