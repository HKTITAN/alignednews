package ai.aligned.ui.settings

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ai.aligned.ui.components.AlignedPress
import ai.aligned.ui.icons.IconSpec
import ai.aligned.ui.icons.MorphingIcon
import ai.aligned.ui.icons.MorphingIcons
import ai.aligned.ui.theme.AlignedTokens
import ai.aligned.ui.theme.Tokens

@Composable
fun SettingsScreen(
    onOpenBookmarks: () -> Unit = {},
    onOpenResearch: () -> Unit = {},
    onOpenEvents: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenSummarize: () -> Unit = {},
    onOpenCategories: () -> Unit = {},
    onOpenAccounts: () -> Unit = {},
    vm: SettingsViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val c = AlignedTokens.colors

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(c.bg),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                Text("More", color = c.text, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.5).sp)
                Text("v0.1.0 · local-only preferences", color = c.textSecondary, fontSize = 13.sp)
            }
        }

        item { SectionHeader("Tools") }
        item {
            Card {
                NavRow(MorphingIcons.share,    "Summarize a link", onClick = onOpenSummarize)
                Separator()
                NavRow(MorphingIcons.sparkle,  "Research",         onClick = onOpenResearch)
            }
        }

        item { SectionHeader("Library") }
        item {
            Card {
                NavRow(MorphingIcons.bookmark, "Bookmarks", onClick = onOpenBookmarks)
                Separator()
                NavRow(MorphingIcons.history,  "History",   onClick = onOpenHistory)
                Separator()
                NavRow(MorphingIcons.calendar, "Events",    onClick = onOpenEvents)
            }
        }

        item { SectionHeader("Browse") }
        item {
            Card {
                NavRow(MorphingIcons.pin,    "Categories",   onClick = onOpenCategories)
                Separator()
                NavRow(MorphingIcons.heart,  "Sources",      onClick = onOpenAccounts)
            }
        }

        item { SectionHeader("Backend") }
        item {
            Card {
                InfoRow(label = "Origin",       value = "alignednews.ai")
                Separator()
                InfoRow(
                    label = "Health",
                    value = state.health,
                    color = if (state.health == "ok") Tokens.Palette.success else c.textSecondary
                )
                Separator()
                InfoRow(label = "Live stories", value = state.storyCount?.toString() ?: "—")
                Separator()
                InfoRow(label = "Last update",  value = state.lastUpdated ?: "—")
                Separator()
                InfoRow(label = "Cache TTL",    value = "Stories 7 days")
            }
        }

        item { SectionHeader("Notifications") }
        item {
            Card {
                ToggleRow(MorphingIcons.flame,    "Breaking",       enabled = state.breakingEnabled, onToggle = vm::toggleBreaking)
                Separator()
                ToggleRow(MorphingIcons.pin,      "My topics",      enabled = state.topicsEnabled,   onToggle = vm::toggleTopics)
                Separator()
                ToggleRow(MorphingIcons.sparkle,  "Daily brief",    enabled = state.briefEnabled,    onToggle = vm::toggleBrief)
                Separator()
                ToggleRow(MorphingIcons.bell,     "Research ready", enabled = state.researchEnabled, onToggle = vm::toggleResearch)
            }
        }

        item { SectionHeader("Sync log") }
        if (state.syncLog.isEmpty()) item {
            Card {
                Row(modifier = Modifier.padding(16.dp)) {
                    Text("No sync events yet.", color = AlignedTokens.colors.textSecondary, fontSize = 13.sp)
                }
            }
        } else items(state.syncLog) { row ->
            Card {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("${row.tier} · ${if (row.ok) "ok" else "failed"}",
                            color = c.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(row.message, color = c.textSecondary, fontSize = 12.sp, maxLines = 1)
                    }
                    Text(row.ago, color = c.textTertiary, fontSize = 12.sp)
                }
            }
        }

        item { SectionHeader("About") }
        item {
            Card {
                InfoRow(label = "Version", value = "0.1.0")
                Separator()
                InfoRow(label = "Source",  value = "github.com/...")
                Separator()
                InfoRow(label = "License", value = "MIT")
            }
        }

        item { Spacer(Modifier.height(48.dp)) }
    }
}

@Composable
private fun SectionHeader(label: String) {
    val c = AlignedTokens.colors
    Text(
        label.uppercase(),
        color = c.textSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.7.sp,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    val c = AlignedTokens.colors
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(Tokens.Radius.card)).background(c.elev1),
        content = content
    )
}

@Composable
private fun InfoRow(label: String, value: String, color: Color? = null) {
    val c = AlignedTokens.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = c.text, fontSize = 14.sp)
        Text(value, color = color ?: c.textSecondary, fontSize = 14.sp)
    }
}

@Composable
private fun NavRow(icon: IconSpec, label: String, onClick: () -> Unit) {
    val c = AlignedTokens.colors
    AlignedPress(onClick = onClick) { pressed ->
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(if (pressed) c.elev2 else Color.Transparent)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MorphingIcon(icon, size = 18.dp, color = c.text)
            Text(label, color = c.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
            MorphingIcon(MorphingIcons.chevronRight, size = 16.dp, color = c.textTertiary)
        }
    }
}

@Composable
private fun ToggleRow(icon: IconSpec, label: String, enabled: Boolean, onToggle: () -> Unit) {
    val c = AlignedTokens.colors
    AlignedPress(onClick = onToggle, haptic = true) { pressed ->
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(if (pressed) c.elev2 else Color.Transparent)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MorphingIcon(icon, size = 18.dp, color = c.text)
            Text(label, color = c.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Switch(enabled = enabled)
        }
    }
}

@Composable
private fun Switch(enabled: Boolean) {
    val c = AlignedTokens.colors
    Box(
        modifier = Modifier.size(width = 42.dp, height = 24.dp)
            .clip(RoundedCornerShape(100))
            .background(if (enabled) Tokens.Palette.success else c.elev2)
    ) {
        Box(
            modifier = Modifier.padding(2.dp).size(20.dp)
                .clip(RoundedCornerShape(100))
                .background(Color.White)
                .align(if (enabled) Alignment.CenterEnd else Alignment.CenterStart)
        )
    }
}

@Composable
private fun Separator() {
    val c = AlignedTokens.colors
    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(c.separator).padding(start = 14.dp))
}
