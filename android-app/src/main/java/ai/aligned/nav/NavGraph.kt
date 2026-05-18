package ai.aligned.nav

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import ai.aligned.ui.accounts.AccountsScreen
import ai.aligned.ui.bookmarks.BookmarksScreen
import ai.aligned.ui.categories.CategoriesScreen
import ai.aligned.ui.chat.ChatScreen
import ai.aligned.ui.events.EventsScreen
import ai.aligned.ui.feed.FeedScreen
import ai.aligned.ui.history.HistoryScreen
import ai.aligned.ui.icons.IconSpec
import ai.aligned.ui.icons.MorphingIcon
import ai.aligned.ui.icons.MorphingIcons
import ai.aligned.ui.lists.BriefScreen
import ai.aligned.ui.map.MapScreen
import ai.aligned.ui.research.ResearchScreen
import ai.aligned.ui.search.SearchScreen
import ai.aligned.ui.settings.SettingsScreen
import ai.aligned.ui.story.StoryScreen
import ai.aligned.ui.summarize.SummarizeScreen
import ai.aligned.ui.summarize.SummarizeViewModel
import ai.aligned.ui.theme.AlignedTokens

object Routes {
    const val Feed = "feed"
    const val Brief = "brief"
    const val MapR = "map"
    const val Chat = "chat"
    const val Settings = "settings"
    const val Search = "search"
    const val Research = "research"
    const val Events = "events"
    const val History = "history"
    const val Bookmarks = "bookmarks"
    const val Summarize = "summarize"
    const val Categories = "categories"
    const val Accounts = "accounts"
    const val Story = "story/{id}"
    fun story(id: String) = "story/$id"
}

@Composable
fun AlignedNavHost(sharedUrl: String? = null) {
    val nav = rememberNavController()
    val back by nav.currentBackStackEntryAsState()
    val currentRoute = back?.destination?.route
    val tabRoutes = setOf(Routes.Feed, Routes.Brief, Routes.MapR, Routes.Chat, Routes.Settings)

    // If we were launched by a shared URL, jump straight to Summarize.
    LaunchedEffect(sharedUrl) {
        if (!sharedUrl.isNullOrBlank() && currentRoute != Routes.Summarize) {
            nav.navigate(Routes.Summarize)
        }
    }

    Scaffold(
        containerColor = AlignedTokens.colors.bg,
        contentColor = AlignedTokens.colors.text,
        bottomBar = {
            if (currentRoute in tabRoutes) {
                NavigationBar(
                    containerColor = AlignedTokens.colors.surface,
                    contentColor = AlignedTokens.colors.text
                ) {
                    NavItem(currentRoute, Routes.Feed,     MorphingIcons.flame,    "Feed")     { nav.tab(Routes.Feed) }
                    NavItem(currentRoute, Routes.Brief,    MorphingIcons.sparkle,  "Today")    { nav.tab(Routes.Brief) }
                    NavItem(currentRoute, Routes.MapR,     MorphingIcons.globe,    "Map")      { nav.tab(Routes.MapR) }
                    NavItem(currentRoute, Routes.Chat,     MorphingIcons.mic,      "Ask")      { nav.tab(Routes.Chat) }
                    NavItem(currentRoute, Routes.Settings, MorphingIcons.settings, "More")     { nav.tab(Routes.Settings) }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.Feed,
            modifier = Modifier.padding(padding),
            enterTransition = {
                fadeIn(tween(180)) + slideInHorizontally(tween(180)) { it / 24 }
            },
            exitTransition = { fadeOut(tween(140)) },
            popEnterTransition = { fadeIn(tween(180)) },
            popExitTransition = {
                fadeOut(tween(140)) + slideOutHorizontally(tween(180)) { it / 24 }
            }
        ) {
            composable(Routes.Feed) {
                FeedScreen(
                    onStory = { id -> nav.navigate(Routes.story(id)) },
                    onSearch = { nav.navigate(Routes.Search) }
                )
            }
            composable(Routes.Brief)     { BriefScreen() }
            composable(Routes.MapR)      { MapScreen() }
            composable(Routes.Chat)      { ChatScreen() }
            composable(Routes.Settings)  {
                SettingsScreen(
                    onOpenBookmarks  = { nav.navigate(Routes.Bookmarks) },
                    onOpenResearch   = { nav.navigate(Routes.Research) },
                    onOpenEvents     = { nav.navigate(Routes.Events) },
                    onOpenHistory    = { nav.navigate(Routes.History) },
                    onOpenSummarize  = { nav.navigate(Routes.Summarize) },
                    onOpenCategories = { nav.navigate(Routes.Categories) },
                    onOpenAccounts   = { nav.navigate(Routes.Accounts) }
                )
            }
            composable(Routes.Research)   { ResearchScreen() }
            composable(Routes.Events)     { EventsScreen() }
            composable(Routes.History)    { HistoryScreen() }
            composable(Routes.Bookmarks)  {
                BookmarksScreen(onStory = { id -> nav.navigate(Routes.story(id)) })
            }
            composable(Routes.Summarize) {
                val vm = hiltViewModel<SummarizeViewModel>()
                // Auto-prefill once if launched by share intent.
                LaunchedEffect(sharedUrl) {
                    if (!sharedUrl.isNullOrBlank() && vm.input.value.isBlank()) {
                        vm.setInput(sharedUrl)
                        vm.submit()
                    }
                }
                SummarizeScreen(
                    onBack = { nav.popBackStack() },
                    onStory = { id -> nav.navigate(Routes.story(id)) },
                    vm = vm
                )
            }
            composable(Routes.Categories) { CategoriesScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.Accounts)   { AccountsScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.Search) {
                SearchScreen(
                    onBack = { nav.popBackStack() },
                    onStory = { id -> nav.navigate(Routes.story(id)) }
                )
            }
            composable(
                Routes.Story,
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
                deepLinks = listOf(navDeepLink { uriPattern = "aligned://story/{id}" })
            ) { StoryScreen(onBack = { nav.popBackStack() }) }
        }
    }
}

private fun androidx.navigation.NavController.tab(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
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
