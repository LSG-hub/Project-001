package app.project001.ui.you

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.project001.R
import app.project001.data.api.ApiException
import app.project001.data.api.ExpectedCall
import app.project001.data.api.Voice
import app.project001.graph
import app.project001.ui.components.BackRow
import app.project001.ui.components.Panel
import app.project001.ui.components.SectionLabel
import app.project001.ui.theme.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Runs [block], swallowing API errors (screens show what loaded). */
internal fun CoroutineScope.launchCatching(block: suspend () -> Unit): Job = launch {
    try {
        block()
    } catch (_: ApiException) {
    }
}

/** Your name and what the assistant should know about you. Your words are instructions to it. */
@Composable
fun AboutYouScreen(onBack: () -> Unit) {
    val c = AppTheme.colors
    val repo = LocalContext.current.graph.profile
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }
    var about by rememberSaveable { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    val saved = stringResource(R.string.about_saved)
    LaunchedEffect(Unit) {
        runCatching { repo.profile() }.getOrNull()?.let { name = it.name.orEmpty(); about = it.about }
    }
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())) {
        BackRow(onBack, title = stringResource(R.string.you_about))
        Column(Modifier.padding(horizontal = 16.dp).widthIn(max = 640.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.about_intro), style = MaterialTheme.typography.bodyLarge, color = c.ink.copy(alpha = 0.85f))
            OutlinedTextField(
                value = name, onValueChange = { name = it.take(60) },
                label = { Text(stringResource(R.string.about_name)) }, singleLine = true,
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = about, onValueChange = { about = it.take(2000) },
                label = { Text(stringResource(R.string.you_about)) },
                placeholder = { Text(stringResource(R.string.about_placeholder)) },
                minLines = 6, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    scope.launch {
                        status = try {
                            repo.update(name = name, about = about); saved
                        } catch (e: ApiException) {
                            e.message
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) { Text(stringResource(R.string.about_save)) }
            status?.let { Text(it, color = c.muted, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

/** The assistant's voice on your calls. */
@Composable
fun VoiceScreen(onBack: () -> Unit) {
    val c = AppTheme.colors
    val repo = LocalContext.current.graph.profile
    val scope = rememberCoroutineScope()
    var voices by remember { mutableStateOf<List<Voice>>(emptyList()) }
    var chosen by remember { mutableStateOf<String?>(null) }
    var women by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        scope.launchCatching {
            voices = repo.voices()
            chosen = repo.profile().voice ?: voices.firstOrNull { it.default }?.name
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        BackRow(onBack, title = stringResource(R.string.you_voice))
        Column(Modifier.padding(horizontal = 16.dp).widthIn(max = 640.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.voice_intro), style = MaterialTheme.typography.bodyLarge, color = c.ink.copy(alpha = 0.85f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = women, onClick = { women = true }, label = { Text(stringResource(R.string.voice_women)) })
                FilterChip(selected = !women, onClick = { women = false }, label = { Text(stringResource(R.string.voice_men)) })
            }
            Panel(padding = 0.dp) {
                voices.filter { (it.gender == "female") == women }.forEach { v ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 56.dp)
                            .clickable(role = Role.RadioButton) {
                                chosen = v.name
                                scope.launchCatching { repo.update(voice = v.name) }
                            }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = chosen == v.name, onClick = null)
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(v.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (v.default) stringResource(R.string.voice_style_default, v.style) else v.style,
                                style = MaterialTheme.typography.bodySmall, color = c.muted,
                            )
                        }
                    }
                }
            }
            Text(stringResource(R.string.voice_try_hint), style = MaterialTheme.typography.bodySmall, color = c.muted, modifier = Modifier.padding(bottom = 24.dp))
        }
    }
}

/** Calls you're expecting, so the assistant knows what to do when they ring. */
@Composable
fun ExpectedCallsScreen(onBack: () -> Unit) {
    val c = AppTheme.colors
    val repo = LocalContext.current.graph.profile
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<ExpectedCall>?>(null) }
    var text by rememberSaveable { mutableStateOf("") }
    var hours by rememberSaveable { mutableStateOf(24.0) }
    var error by remember { mutableStateOf<String?>(null) }
    fun reload() = scope.launchCatching { items = repo.expected() }
    LaunchedEffect(Unit) { reload() }
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())) {
        BackRow(onBack, title = stringResource(R.string.you_expected))
        Column(Modifier.padding(horizontal = 16.dp).widthIn(max = 640.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.expected_intro), style = MaterialTheme.typography.bodyLarge, color = c.ink.copy(alpha = 0.85f))
            OutlinedTextField(
                value = text, onValueChange = { text = it.take(500) },
                label = { Text(stringResource(R.string.expected_new)) },
                placeholder = { Text(stringResource(R.string.expected_placeholder)) },
                minLines = 2, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(12.0 to R.string.expected_today, 48.0 to R.string.expected_two_days, 168.0 to R.string.expected_week).forEach { (h, label) ->
                    FilterChip(selected = hours == h, onClick = { hours = h }, label = { Text(stringResource(label)) })
                }
            }
            Button(
                enabled = text.trim().length >= 3,
                onClick = {
                    scope.launch {
                        try {
                            repo.addExpected(text, hours); text = ""; error = null; reload()
                        } catch (e: ApiException) {
                            error = e.message
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) { Text(stringResource(R.string.expected_add)) }
            error?.let { Text(it, color = c.scam.ink) }
            SectionLabel(stringResource(R.string.expected_waiting), Modifier.padding(top = 8.dp))
            val list = items
            if (list != null && list.isEmpty()) Text(stringResource(R.string.you_expected_none), color = c.muted)
            list?.forEach { e ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.card).padding(start = 14.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(e.text, style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(R.string.expected_until, until(e.expiresAt)), style = MaterialTheme.typography.bodySmall, color = c.muted)
                    }
                    IconButton(onClick = { scope.launchCatching { repo.removeExpected(e.id); reload() } }) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.expected_remove), tint = c.muted)
                    }
                }
            }
        }
    }
}

private fun until(iso: String): String =
    runCatching {
        OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
    }.getOrDefault(iso)
