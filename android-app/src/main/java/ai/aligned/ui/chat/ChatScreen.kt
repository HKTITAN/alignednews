package ai.aligned.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ai.aligned.ui.icons.MorphingIcon
import ai.aligned.ui.icons.MorphingIcons
import ai.aligned.ui.theme.AlignedTokens
import ai.aligned.ui.theme.Tokens

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
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(state.messages, key = { "${state.messages.indexOf(it)}-${it.role}" }) { msg ->
                MessageBubble(role = msg.role, content = msg.content)
            }
            if (state.messages.isEmpty()) item {
                Text(
                    "Ask anything about today's news. The reply streams in token-by-token.",
                    color = c.textSecondary, fontSize = 15.sp,
                    modifier = Modifier.padding(top = 80.dp, bottom = 8.dp)
                )
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
private fun MessageBubble(role: String, content: String) {
    val c = AlignedTokens.colors
    Column(modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(Tokens.Radius.card))
        .background(c.elev1)
        .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(role, color = c.textSecondary, fontSize = 11.sp,
            fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
        Text(content, color = c.text, fontSize = 15.sp, lineHeight = 22.sp)
    }
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
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 12.dp)
        .clip(RoundedCornerShape(24.dp))
        .background(c.elev1)
        .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.weight(1f)) {
            if (text.isEmpty()) {
                Text("Ask anything about today's news...",
                    color = c.textTertiary, fontSize = 15.sp)
            }
            BasicTextField(
                value = text,
                onValueChange = onText,
                textStyle = TextStyle(color = c.text, fontSize = 15.sp),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(c.accent),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Box(modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(100))
            .background(if (streaming) c.surface else c.accent)
            .clickable { if (streaming) onCancel() else onSend() },
            contentAlignment = Alignment.Center) {
            MorphingIcon(
                spec = if (streaming) MorphingIcons.pause else MorphingIcons.send,
                size = 18.dp,
                color = if (streaming) c.text else androidx.compose.ui.graphics.Color.White
            )
        }
    }
}
