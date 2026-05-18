package ai.aligned.ui.accounts

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ai.aligned.net.dto.AccountDto
import ai.aligned.ui.components.AlignedPress
import ai.aligned.ui.components.SkeletonCard
import ai.aligned.ui.icons.MorphingIcon
import ai.aligned.ui.icons.MorphingIcons
import ai.aligned.ui.theme.AlignedTokens
import ai.aligned.ui.theme.Tokens
import coil.compose.AsyncImage

@Composable
fun AccountsScreen(
    onBack: () -> Unit = {},
    vm: AccountsViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val c = AlignedTokens.colors
    val handler = LocalUriHandler.current

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
            Text("Sources", color = c.text, fontWeight = FontWeight.Bold,
                fontSize = 28.sp, letterSpacing = (-0.5).sp)
            Text(
                when (val s = state) {
                    AccountsState.Loading -> "Loading…"
                    is AccountsState.Error -> "Couldn't load"
                    is AccountsState.Ready -> "${s.accounts.size} curated accounts"
                },
                color = c.textSecondary, fontSize = 13.sp
            )
        }
        when (val s = state) {
            AccountsState.Loading -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) { items(6) { SkeletonCard(heightDp = 72) } }
            is AccountsState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(s.message, color = c.destructive, fontSize = 13.sp,
                    modifier = Modifier.padding(32.dp))
            }
            is AccountsState.Ready -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(s.accounts, key = { it.id }) { account ->
                    AccountCard(account, onClick = {
                        runCatching { handler.openUri("https://x.com/${account.username}") }
                    })
                }
                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }
}

@Composable
private fun AccountCard(a: AccountDto, onClick: () -> Unit) {
    val c = AlignedTokens.colors
    AlignedPress(onClick = onClick) { pressed ->
        Column(modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(Tokens.Radius.card))
            .background(if (pressed) c.elev2 else c.elev1)
            .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (a.profileImage.isNotBlank()) AsyncImage(
                    model = a.profileImage, contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(c.elev2)
                ) else Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(c.elev2))
                Column(modifier = Modifier.weight(1f)) {
                    Text(a.name, color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("@${a.username}  ·  ${formatFollowers(a.followers)} followers",
                        color = c.textSecondary, fontSize = 12.sp)
                }
                MorphingIcon(MorphingIcons.chevronRight, size = 14.dp, color = c.textTertiary)
            }
            if (a.description.isNotBlank()) Text(
                a.description, color = c.textSecondary, fontSize = 13.sp,
                lineHeight = 18.sp, maxLines = 3, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatFollowers(n: Long): String = when {
    n < 1_000 -> "$n"
    n < 1_000_000 -> "${n / 1_000}K"
    else -> "${(n / 100_000) / 10.0}M"
}
