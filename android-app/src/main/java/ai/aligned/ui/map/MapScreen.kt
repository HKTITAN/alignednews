package ai.aligned.ui.map

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
fun MapScreen(vm: MapViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val c = AlignedTokens.colors
    Column(modifier = Modifier.fillMaxSize().background(c.bg)) {
        Header(state)
        when (val s = state) {
            MapState.Loading -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) { items(5) { SkeletonCard(heightDp = 80) } }
            is MapState.Error -> ErrorView(s.message, vm::refresh)
            is MapState.Ready -> Markers(s)
        }
    }
}

@Composable
private fun Header(state: MapState) {
    val c = AlignedTokens.colors
    val count = (state as? MapState.Ready)?.markers?.size ?: 0
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text("Map", color = c.text, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.5).sp)
        Text(
            if (count > 0) "$count locations · today" else "Where today's news is happening",
            color = c.textSecondary, fontSize = 13.sp
        )
    }
}

@Composable
private fun Markers(s: MapState.Ready) {
    val c = AlignedTokens.colors
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(s.markers, key = { "${it.city}/${it.groupId}" }) { m ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Tokens.Radius.card))
                    .background(c.elev1)
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(m.color))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${m.city}${if (m.country.isNotBlank()) ", ${m.country}" else ""}",
                        color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.weight(1f))
                    Text(m.groupName, color = c.textSecondary, fontSize = 11.sp,
                        fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
                }
                if (m.headlines.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    m.headlines.take(2).forEach { h ->
                        Text("• $h", color = c.textSecondary, fontSize = 13.sp, lineHeight = 18.sp, maxLines = 2)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun ErrorView(msg: String, onRetry: () -> Unit) {
    val c = AlignedTokens.colors
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MorphingIcon(spec = MorphingIcons.globe, size = 28.dp, color = c.destructive)
            Spacer(Modifier.height(8.dp))
            Text("Map unavailable", color = c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(msg, color = c.textSecondary, fontSize = 13.sp)
        }
    }
}
