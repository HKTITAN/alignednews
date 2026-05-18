package ai.aligned.ui.categories

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
import ai.aligned.ui.components.AlignedPress
import ai.aligned.ui.icons.MorphingIcon
import ai.aligned.ui.icons.MorphingIcons
import ai.aligned.ui.theme.AlignedTokens
import ai.aligned.ui.theme.Tokens

@Composable
fun CategoriesScreen(
    onBack: () -> Unit = {},
    vm: CategoriesViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val pins by vm.pinnedIds.collectAsState()
    val c = AlignedTokens.colors

    Column(modifier = Modifier.fillMaxSize().background(c.bg)) {
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
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
            Text("Categories", color = c.text, fontWeight = FontWeight.Bold,
                fontSize = 28.sp, letterSpacing = (-0.5).sp)
            Text(
                if (pins.isEmpty()) "Pin topics to get story notifications for them."
                else "${pins.size} pinned · stories in these get pushed to you",
                color = c.textSecondary, fontSize = 13.sp
            )
        }
        when (val s = state) {
            CategoriesState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading…", color = c.textSecondary, fontSize = 13.sp)
            }
            is CategoriesState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(s.message, color = c.destructive, fontSize = 13.sp)
            }
            is CategoriesState.Ready -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(s.categories, key = { it.id }) { cat ->
                    val pinned = cat.id in pins
                    AlignedPress(onClick = { vm.togglePin(cat.id) }) { pressed ->
                        Row(modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(Tokens.Radius.card))
                            .background(if (pressed) c.elev2 else c.elev1)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(cat.color))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(cat.label, color = c.text, fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium)
                                Text(cat.id, color = c.textTertiary, fontSize = 11.sp,
                                    letterSpacing = 0.4.sp)
                            }
                            MorphingIcon(
                                spec = MorphingIcons.pin,
                                size = 18.dp,
                                color = if (pinned) c.accent else c.textTertiary
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }
}
