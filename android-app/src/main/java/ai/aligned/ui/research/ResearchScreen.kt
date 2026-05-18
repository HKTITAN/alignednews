package ai.aligned.ui.research

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LinearProgressIndicator
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
fun ResearchScreen(vm: ResearchViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val c = AlignedTokens.colors

    Column(modifier = Modifier.fillMaxSize().background(c.bg)) {
        Header(state)
        Composer(
            query = vm.query.value,
            running = state.status == "running",
            onQuery = vm::setQuery,
            onStart = vm::start
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.error != null) item {
                Text(state.error!!, color = c.destructive, fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
            }
            if (state.steps.isEmpty() && state.status == "idle") item {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        MorphingIcon(MorphingIcons.sparkle, size = 28.dp, color = c.textTertiary)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Ask a deep question to kick off a multi-step research session.",
                            color = c.textSecondary, fontSize = 14.sp
                        )
                    }
                }
            }
            if (state.steps.isNotEmpty() || state.status == "running") item {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(Tokens.Radius.card))
                        .background(c.elev1)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(8.dp).clip(CircleShape)
                                .background(
                                    when (state.status) {
                                        "running"  -> c.accent
                                        "complete" -> Tokens.Palette.success
                                        else       -> c.textTertiary
                                    }
                                )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when (state.status) {
                                "running"  -> "Step ${state.steps.count { it.status == "complete" }} / ${state.steps.size.coerceAtLeast(10)}"
                                "complete" -> "All steps complete"
                                else       -> "Awaiting query"
                            },
                            color = c.textSecondary, fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp
                        )
                    }
                    val pct = if (state.steps.isEmpty()) 0f
                              else state.steps.count { it.status == "complete" }.toFloat() / state.steps.size.coerceAtLeast(1)
                    LinearProgressIndicator(
                        progress = { pct },
                        modifier = Modifier.fillMaxWidth(),
                        color = c.accent,
                        trackColor = c.elev2
                    )
                    state.steps.forEach { step ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                when (step.status) {
                                    "complete" -> "●"
                                    "running"  -> "◐"
                                    else       -> "○"
                                },
                                color = when (step.status) {
                                    "complete" -> Tokens.Palette.success
                                    "running"  -> c.accent
                                    else       -> c.textTertiary
                                },
                                fontSize = 14.sp
                            )
                            Text(step.name, color = c.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            if (!step.detail.isNullOrBlank()) {
                                Text(step.detail!!, color = c.textTertiary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            state.answer?.takeIf { it.isNotBlank() }?.let { answer ->
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(Tokens.Radius.card))
                            .background(c.elev1)
                            .padding(16.dp)
                    ) {
                        Text(
                            "ANSWER",
                            color = c.textSecondary, fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(answer, color = c.text, fontSize = 15.sp, lineHeight = 22.sp)
                    }
                }
            }
            items(state.insights, key = { it.title }) { insight ->
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(Tokens.Radius.card))
                        .background(c.elev1)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(insight.title, color = c.text, fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1).sp)
                    Text(insight.summary, color = c.textSecondary, fontSize = 14.sp, lineHeight = 20.sp)
                    if (insight.confidence > 0) Text(
                        "Confidence ${insight.confidence}%",
                        color = c.textTertiary, fontSize = 11.sp
                    )
                }
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun Header(state: ResearchState) {
    val c = AlignedTokens.colors
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text("Research", color = c.text, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.5).sp)
        Text(
            when (state.status) {
                "running"  -> "Researching…"
                "complete" -> "Complete"
                "error"    -> "Failed"
                else       -> "Multi-step deep research"
            },
            color = c.textSecondary, fontSize = 13.sp
        )
    }
}

@Composable
private fun Composer(query: String, running: Boolean, onQuery: (String) -> Unit, onStart: () -> Unit) {
    val c = AlignedTokens.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(Tokens.Radius.chip))
            .background(c.elev1)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) Text(
                "Research a topic in depth…",
                color = c.textTertiary, fontSize = 15.sp
            )
            BasicTextField(
                value = query, onValueChange = onQuery,
                textStyle = TextStyle(color = c.text, fontSize = 15.sp),
                cursorBrush = SolidColor(c.accent),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        val canStart = query.isNotBlank() && !running
        AlignedPress(onClick = { if (canStart) onStart() }) { pressed ->
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape)
                    .background(if (canStart) c.accent else c.elev2),
                contentAlignment = Alignment.Center
            ) {
                MorphingIcon(
                    spec = MorphingIcons.sparkle, size = 16.dp,
                    color = if (canStart) Color.White else c.textTertiary
                )
            }
        }
    }
}
