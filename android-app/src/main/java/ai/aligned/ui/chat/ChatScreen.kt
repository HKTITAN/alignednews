package ai.aligned.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import kotlinx.coroutines.delay

@Composable
fun ChatScreen(vm: ChatViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val c = AlignedTokens.colors
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size, state.messages.lastOrNull()?.content) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(c.bg)) {
        ChatHeader(streaming = state.streaming, onClear = vm::clear, canClear = state.messages.isNotEmpty())

        if (state.messages.isEmpty()) {
            EmptyChat(onSuggest = vm::setInput)
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.messages, key = { "${state.messages.indexOf(it)}-${it.role}" }) { msg ->
                    MessageBubble(role = msg.role, content = msg.content, streaming = state.streaming && msg === state.messages.lastOrNull() && msg.role == "assistant")
                }
                if (state.error != null) item {
                    Text(state.error!!, color = c.destructive, fontSize = 13.sp,
                         modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }

        Composer(
            text = state.input,
            streaming = state.streaming,
            onText = vm::setInput,
            onSend = vm::send,
            onCancel = vm::cancel
        )
    }
}

@Composable
private fun ChatHeader(streaming: Boolean, onClear: () -> Unit, canClear: Boolean) {
    val c = AlignedTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Ask", color = c.text, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
            Text(
                if (streaming) "Streaming reply…" else "Ground-truth grounded in today's stories",
                color = c.textSecondary, fontSize = 13.sp
            )
        }
        if (canClear) AlignedPress(onClick = onClear) { pressed ->
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(Tokens.Radius.tap))
                    .background(if (pressed) c.surface else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                MorphingIcon(spec = MorphingIcons.close, size = 18.dp, color = c.text)
            }
        }
    }
}

@Composable
private fun ColumnScope.EmptyChat(onSuggest: (String) -> Unit) {
    val c = AlignedTokens.colors
    val prompts = remember {
        listOf(
            "What's the biggest AI safety story today?",
            "Summarize today's xAI news",
            "Which companies announced new models?",
            "What's happening in robotics?",
            "Any major research papers worth reading?"
        )
    }
    Column(
        modifier = Modifier.weight(1f, fill = true).fillMaxWidth().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            "Ask anything about today's news.",
            color = c.text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp,
            letterSpacing = (-0.2).sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Answers stream token-by-token from the live feed.",
            color = c.textSecondary, fontSize = 15.sp, lineHeight = 20.sp
        )
        Spacer(Modifier.height(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            prompts.forEach { p ->
                AlignedPress(onClick = { onSuggest(p) }) { pressed ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Tokens.Radius.card))
                            .background(if (pressed) c.elev2 else c.elev1)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MorphingIcon(spec = MorphingIcons.sparkle, size = 16.dp, color = c.textSecondary)
                        Text(p, color = c.text, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(role: String, content: String, streaming: Boolean) {
    val c = AlignedTokens.colors
    val isUser = role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp, topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp
                    )
                )
                .background(if (isUser) c.accent else c.elev1)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val textColor = if (isUser) Color.White else c.text
            if (content.isEmpty() && streaming) {
                TypingIndicator(color = c.textSecondary)
            } else {
                Text(content, color = textColor, fontSize = 15.sp, lineHeight = 22.sp)
                if (streaming) TypingIndicator(color = c.textTertiary)
            }
        }
    }
}

@Composable
private fun TypingIndicator(color: Color) {
    val infinite = rememberInfiniteTransitionValues()
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 0..2) {
            val phase = (infinite + i * 0.18f) % 1f
            val alpha = 0.3f + 0.7f * androidx.compose.ui.util.lerp(0f, 1f, kotlin.math.abs(1f - 2f * phase))
            Box(
                modifier = Modifier.size(5.dp).clip(CircleShape).background(color.copy(alpha = alpha))
            )
        }
    }
}

@Composable
private fun rememberInfiniteTransitionValues(): Float {
    var t by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            t = (t + 0.02f) % 1f
            delay(40)
        }
    }
    return t
}

@Composable
private fun Composer(
    text: String,
    streaming: Boolean,
    onText: (String) -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit
) {
    val c = AlignedTokens.colors
    val canSend = text.isNotBlank() && !streaming
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(c.elev1)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (text.isEmpty()) {
                Text(
                    "Ask anything about today's news…",
                    color = c.textTertiary, fontSize = 15.sp
                )
            }
            BasicTextField(
                value = text,
                onValueChange = onText,
                textStyle = TextStyle(color = c.text, fontSize = 15.sp),
                cursorBrush = SolidColor(c.accent),
                modifier = Modifier.fillMaxWidth()
            )
        }
        val sendActive = canSend || streaming
        AlignedPress(onClick = { if (streaming) onCancel() else if (canSend) onSend() }) { pressed ->
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            streaming -> c.text
                            canSend -> c.accent
                            else -> c.surface
                        }
                    )
                    .scale(if (pressed && sendActive) 0.95f else 1f),
                contentAlignment = Alignment.Center
            ) {
                MorphingIcon(
                    spec = if (streaming) MorphingIcons.pause else MorphingIcons.send,
                    size = 18.dp,
                    color = when {
                        streaming -> c.bg
                        canSend -> Color.White
                        else -> c.textTertiary
                    }
                )
            }
        }
    }
}
