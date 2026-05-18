package ai.aligned.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ai.aligned.ui.chat.ChatScreen
import ai.aligned.ui.feed.FeedScreen
import ai.aligned.ui.icons.IconSpec
import ai.aligned.ui.story.StoryScreen
import ai.aligned.ui.icons.MorphingIcon
import ai.aligned.ui.icons.MorphingIcons
import ai.aligned.ui.theme.AlignedTokens

@Composable
fun AlignedNavHost() {
    val nav = rememberNavController()
    val back by nav.currentBackStackEntryAsState()
    val currentRoute = back?.destination?.route

    Scaffold(
        containerColor = AlignedTokens.colors.bg,
        contentColor = AlignedTokens.colors.text,
        bottomBar = {
            NavigationBar(
                containerColor = AlignedTokens.colors.surface,
                contentColor = AlignedTokens.colors.text
            ) {
                NavItem(currentRoute, "feed",     MorphingIcons.flame,    "Feed")     { nav.navigate("feed")     { popUpTo("feed"){inclusive=true} } }
                NavItem(currentRoute, "brief",    MorphingIcons.sparkle,  "Today")    { nav.navigate("brief")    }
                NavItem(currentRoute, "map",      MorphingIcons.globe,    "Map")      { nav.navigate("map")      }
                NavItem(currentRoute, "chat",     MorphingIcons.mic,      "Ask")      { nav.navigate("chat")     }
                NavItem(currentRoute, "settings", MorphingIcons.settings, "Settings") { nav.navigate("settings") }
            }
        }
    ) { padding ->
        NavHost(navController = nav, startDestination = "feed", modifier = Modifier.padding(padding)) {
            composable("feed")     { FeedScreen(onStory = { id -> nav.navigate("story/$id") }) }
            composable("brief")    { Placeholder("Today's Brief — wired in M4") }
            composable("map")      { Placeholder("Map — wired in M7") }
            composable("chat")     { ChatScreen() }
            composable("settings") { Placeholder("Settings — wired in M10") }
            composable(
                "story/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { StoryScreen(onBack = { nav.popBackStack() }) }
        }
    }
}

@Composable
private fun RowScope.NavItem(
    current: String?, route: String, icon: IconSpec, label: String, onClick: () -> Unit
) {
    NavigationBarItem(
        selected = current == route,
        onClick = onClick,
        icon = { MorphingIcon(spec = icon, size = 22.dp, color = AlignedTokens.colors.text) },
        label = { Text(label) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = AlignedTokens.colors.accent,
            selectedTextColor = AlignedTokens.colors.accent,
            unselectedIconColor = AlignedTokens.colors.textSecondary,
            unselectedTextColor = AlignedTokens.colors.textSecondary,
            indicatorColor = AlignedTokens.colors.elev2
        )
    )
}

@Composable
private fun Placeholder(msg: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(msg, color = AlignedTokens.colors.textSecondary)
    }
}
