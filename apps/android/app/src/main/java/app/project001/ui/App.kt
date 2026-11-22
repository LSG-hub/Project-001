package app.project001.ui

import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.project001.R
import app.project001.data.Languages
import app.project001.ui.assistant.AssistantScreen
import app.project001.graph
import app.project001.ui.calls.CallDetailScreen
import app.project001.ui.calls.CallsScreen
import app.project001.ui.calls.TestCallScreen
import app.project001.ui.family.FamilyScreen
import app.project001.ui.onboarding.SetupScreen
import app.project001.ui.onboarding.SignInScreen
import app.project001.ui.theme.AppTheme
import app.project001.ui.you.AboutYouScreen
import app.project001.ui.you.ExpectedCallsScreen
import app.project001.ui.you.LanguageScreen
import app.project001.ui.you.VoiceScreen
import app.project001.ui.you.YouScreen

private data class Tab(val route: String, val label: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab("calls", R.string.nav_calls, Icons.Outlined.Call),
    Tab("family", R.string.nav_family, Icons.Outlined.Groups),
    Tab("assistant", R.string.nav_assistant, Icons.AutoMirrored.Outlined.Chat),
    Tab("you", R.string.nav_you, Icons.Outlined.Person),
)

/**
 * App shell. Bottom bar on phones (< 600dp wide), navigation rail on
 * foldables and tablets. Simple mode enlarges all text by 20%.
 */
@Composable
fun App() {
    val signedIn by LocalContext.current.graph.auth.signedIn.collectAsState()
    var simpleMode by rememberSaveable { mutableStateOf(false) }
    val base = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(base.density, base.fontScale * if (simpleMode) 1.2f else 1f)) {
        if (!signedIn) {
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                SignInScreen()
            }
        } else {
            Shell(simpleMode = simpleMode, onSimpleModeChange = { simpleMode = it })
        }
    }
}

@Composable
private fun Shell(simpleMode: Boolean, onSimpleModeChange: (Boolean) -> Unit) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "calls"
    val onTab = tabs.any { it.route == route }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 600.dp
        if (wide) {
            Row(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                if (onTab) Rail(route) { go(nav, it) }
                Box(Modifier.weight(1f)) { Routes(nav, simpleMode, onSimpleModeChange) }
            }
        } else {
            Scaffold(
                containerColor = AppTheme.colors.paper,
                bottomBar = { if (onTab) BottomBar(route) { go(nav, it) } },
            ) { padding ->
                Box(Modifier.padding(padding)) { Routes(nav, simpleMode, onSimpleModeChange) }
            }
        }
    }
}

private fun go(nav: NavHostController, route: String) {
    nav.navigate(route) {
        popUpTo("calls") { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun BottomBar(current: String, onSelect: (String) -> Unit) {
    val c = AppTheme.colors
    NavigationBar(containerColor = c.card) {
        tabs.forEach { t ->
            NavigationBarItem(
                selected = current == t.route,
                onClick = { onSelect(t.route) },
                icon = { Icon(t.icon, contentDescription = null) },
                label = { Text(stringResource(t.label), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = c.safe.ink,
                    selectedTextColor = c.ink,
                    indicatorColor = c.safe.tint,
                    unselectedIconColor = c.muted,
                    unselectedTextColor = c.muted,
                ),
            )
        }
    }
}

@Composable
private fun Rail(current: String, onSelect: (String) -> Unit) {
    val c = AppTheme.colors
    NavigationRail(containerColor = c.card) {
        tabs.forEach { t ->
            NavigationRailItem(
                selected = current == t.route,
                onClick = { onSelect(t.route) },
                icon = { Icon(t.icon, contentDescription = null) },
                label = { Text(stringResource(t.label), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = c.safe.ink,
                    selectedTextColor = c.ink,
                    indicatorColor = c.safe.tint,
                    unselectedIconColor = c.muted,
                    unselectedTextColor = c.muted,
                ),
            )
        }
    }
}

@Composable
private fun Routes(nav: NavHostController, simpleMode: Boolean, onSimpleModeChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    var languageTag by rememberSaveable { mutableStateOf(currentAppLanguage(context)) }
    NavHost(nav, startDestination = "calls") {
        composable("calls") { CallsScreen(onOpenCall = { nav.navigate("call/$it") }, onTestCall = { nav.navigate("test-call") }) }
        composable("call/{id}") { e -> CallDetailScreen(e.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() }) }
        composable("test-call") { TestCallScreen(onBack = { nav.popBackStack() }) }
        composable("family") { FamilyScreen() }
        composable("assistant") { AssistantScreen() }
        composable("you") {
            val label = Languages.languages.firstOrNull { it.tag == languageTag }?.takeIf { it.tag != null }?.native
                ?: stringResource(R.string.language_use_phone)
            YouScreen(
                appLanguageLabel = label,
                simpleMode = simpleMode,
                onSimpleModeChange = onSimpleModeChange,
                onOpenLanguage = { nav.navigate("language") },
                onOpenSetup = { nav.navigate("setup") },
                onOpenAbout = { nav.navigate("about") },
                onOpenVoice = { nav.navigate("voice") },
                onOpenExpected = { nav.navigate("expected") },
            )
        }
        composable("about") { AboutYouScreen(onBack = { nav.popBackStack() }) }
        composable("voice") { VoiceScreen(onBack = { nav.popBackStack() }) }
        composable("expected") { ExpectedCallsScreen(onBack = { nav.popBackStack() }) }
        composable("language") {
            LanguageScreen(
                selectedTag = languageTag,
                onPickLanguage = { l ->
                    languageTag = l.tag
                    setAppLanguage(context, l.tag)
                },
                onBack = { nav.popBackStack() },
            )
        }
        composable("setup") { SetupScreen(onBack = { nav.popBackStack() }) }
    }
}

/** Android 13+ per-app language. On older versions the app follows the phone's language. */
private fun setAppLanguage(context: android.content.Context, tag: String?) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val lm = context.getSystemService(LocaleManager::class.java)
        lm.applicationLocales = if (tag == null) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
    }
}

private fun currentAppLanguage(context: android.content.Context): String? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
    val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
    return if (locales.isEmpty) null else locales[0].language
}
