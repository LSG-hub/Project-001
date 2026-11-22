package app.project001.ui.you

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.project001.R
import app.project001.data.ThemeChoice
import app.project001.data.api.Me
import app.project001.data.api.Profile
import app.project001.graph
import app.project001.ui.components.Panel
import app.project001.ui.components.SectionLabel
import app.project001.ui.components.SettingRow
import app.project001.ui.theme.AppTheme
import app.project001.ui.theme.DisplayFamily

/** "You": who you are to the assistant, how it sounds, and the app's own settings. */
@Composable
fun YouScreen(
    appLanguageLabel: String,
    onOpenLanguage: () -> Unit,
    onOpenSetup: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenVoice: () -> Unit,
    onOpenExpected: () -> Unit,
) {
    val c = AppTheme.colors
    val graph = LocalContext.current.graph
    var me by remember { mutableStateOf<Me?>(null) }
    var profile by remember { mutableStateOf<Profile?>(null) }
    var expectedCount by remember { mutableStateOf<Int?>(null) }
    var defaultVoice by remember { mutableStateOf<String?>(null) }
    val simpleMode by graph.settings.simpleMode.collectAsState()
    val theme by graph.settings.theme.collectAsState()
    val scope = rememberCoroutineScope()
    LifecycleResumeEffect(Unit) {
        val job = scope.launchCatching {
            me = graph.signIn.me()
            profile = graph.profile.profile()
            expectedCount = graph.profile.expected().size
            defaultVoice = graph.profile.voices().firstOrNull { it.default }?.name
        }
        onPauseOrDispose { job.cancel() }
    }
    val name = profile?.name

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(c.inverse), contentAlignment = Alignment.Center) {
                Text((name?.firstOrNull() ?: '•').uppercase(), style = MaterialTheme.typography.headlineSmall.copy(fontFamily = DisplayFamily), color = c.onInverse)
            }
            Column(Modifier.weight(1f)) {
                Text(name ?: stringResource(R.string.you_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                Text(me?.phone ?: "…", style = MaterialTheme.typography.bodyMedium, color = c.muted)
            }
        }
        Column(Modifier.padding(horizontal = 16.dp).widthIn(max = 640.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Panel(padding = 0.dp) {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    SectionLabel(stringResource(R.string.you_your_assistant), Modifier.padding(top = 14.dp, bottom = 2.dp))
                    NavRow(
                        stringResource(R.string.you_about),
                        profile?.about?.takeIf { it.isNotBlank() } ?: stringResource(R.string.you_about_empty),
                        onOpenAbout,
                    )
                    NavRow(
                        stringResource(R.string.you_voice),
                        profile?.voice ?: defaultVoice?.let { stringResource(R.string.you_voice_default, it) } ?: "…",
                        onOpenVoice,
                    )
                    NavRow(
                        stringResource(R.string.you_expected),
                        when (val n = expectedCount) { null -> "…"; 0 -> stringResource(R.string.you_expected_none); else -> stringResource(R.string.you_expected_count, n) },
                        onOpenExpected,
                        divider = false,
                    )
                }
            }
            Panel(padding = 0.dp) {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    SectionLabel(stringResource(R.string.you_app), Modifier.padding(top = 14.dp, bottom = 2.dp))
                    NavRow(stringResource(R.string.you_app_language), appLanguageLabel, onOpenLanguage)
                    NavRow(stringResource(R.string.you_check_setup), stringResource(R.string.you_check_setup_value), onOpenSetup)
                    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                        Text(stringResource(R.string.you_theme), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal))
                        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                ThemeChoice.Light to R.string.theme_light,
                                ThemeChoice.Dark to R.string.theme_dark,
                                ThemeChoice.System to R.string.theme_system,
                            ).forEach { (choice, label) ->
                                FilterChip(selected = theme == choice, onClick = { graph.settings.setTheme(choice) }, label = { Text(stringResource(label)) })
                            }
                        }
                    }
                    HorizontalDivider(color = c.line)
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(role = Role.Switch) { graph.settings.setSimpleMode(!simpleMode) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.you_simple_mode), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal))
                            Text(stringResource(R.string.you_simple_mode_detail), style = MaterialTheme.typography.bodySmall, color = c.muted)
                        }
                        Switch(checked = simpleMode, onCheckedChange = null, colors = SwitchDefaults.colors(checkedTrackColor = c.accent))
                    }
                }
            }
            Panel(padding = 0.dp) {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    SettingRow(stringResource(R.string.you_sign_out), color = c.scam.ink, divider = false, onClick = { graph.signIn.signOut() })
                }
            }
            Box(Modifier.heightIn(min = 16.dp))
        }
    }
}

@Composable
internal fun NavRow(label: String, value: String, onClick: () -> Unit, divider: Boolean = true) {
    val c = AppTheme.colors
    Column {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(onClick = onClick, role = Role.Button),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal), modifier = Modifier.padding(vertical = 8.dp))
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = c.muted,
                textAlign = TextAlign.End,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = c.muted)
        }
        if (divider) HorizontalDivider(color = c.line)
    }
}
