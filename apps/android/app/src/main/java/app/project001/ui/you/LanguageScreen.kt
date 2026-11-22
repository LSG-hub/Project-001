package app.project001.ui.you

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import app.project001.R
import app.project001.data.Language
import app.project001.data.Languages
import app.project001.ui.components.BackRow
import app.project001.ui.components.Panel
import app.project001.ui.components.SectionLabel
import app.project001.ui.theme.AppTheme

/**
 * App language and the assistant's speaking language. Picking an app language
 * applies Android's per-app language (the same list appears in system Settings).
 */
@Composable
fun LanguageScreen(selectedTag: String?, onPickLanguage: (Language) -> Unit, onBack: () -> Unit) {
    val c = AppTheme.colors
    var query by rememberSaveable { mutableStateOf("") }
    var matchCaller by rememberSaveable { mutableStateOf(true) }
    val q = query.trim().lowercase()
    val phoneLabel = stringResource(R.string.language_use_phone)
    val shown = Languages.languages.filter { l ->
        q.isEmpty() || "${if (l.tag == null) phoneLabel else l.native} ${l.english} ${l.searchAlso}".lowercase().contains(q)
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        BackRow(onBack = onBack, title = stringResource(R.string.language_title))
        Column(Modifier.padding(horizontal = 16.dp).widthIn(max = 640.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Panel {
                SectionLabel(stringResource(R.string.you_app_language), Modifier.padding(bottom = 10.dp))
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.language_search)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = c.paper,
                        unfocusedContainerColor = c.paper,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                )
                Column(Modifier.padding(top = 6.dp).selectableGroup()) {
                    if (shown.isEmpty()) {
                        Text(stringResource(R.string.language_no_match, query), color = c.muted, modifier = Modifier.padding(vertical = 18.dp))
                    }
                    shown.forEachIndexed { i, l ->
                        val selected = l.tag == selectedTag
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 56.dp).selectable(selected = selected, role = Role.RadioButton) { onPickLanguage(l) },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            RadioButton(selected = selected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = c.accent))
                            Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                                Text(
                                    if (l.tag == null) phoneLabel else l.native,
                                    style = MaterialTheme.typography.titleMedium.copy(textDirection = if (l.rtl) TextDirection.Rtl else TextDirection.Content),
                                )
                                Text(if (l.tag == null) "System default" else l.english, style = MaterialTheme.typography.bodySmall, color = c.muted)
                            }
                        }
                        if (i < shown.lastIndex) HorizontalDivider(color = c.line)
                    }
                }
            }

            Panel {
                SectionLabel(stringResource(R.string.language_voice_heading))
                Column(Modifier.selectableGroup()) {
                    VoiceOption(stringResource(R.string.language_voice_match), stringResource(R.string.language_voice_match_detail), matchCaller) { matchCaller = true }
                    HorizontalDivider(color = c.line)
                    VoiceOption(stringResource(R.string.language_voice_fixed), stringResource(R.string.language_voice_fixed_detail), !matchCaller) { matchCaller = false }
                }
                Text(
                    stringResource(R.string.language_summaries_note),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 6.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.paper).padding(12.dp),
                )
            }
            Column(Modifier.heightIn(min = 16.dp)) {}
        }
    }
}

@Composable
private fun VoiceOption(title: String, detail: String, selected: Boolean, onSelect: () -> Unit) {
    val c = AppTheme.colors
    Row(
        Modifier.fillMaxWidth().selectable(selected = selected, role = Role.RadioButton, onClick = onSelect).padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = selected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = c.accent), modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium))
            Text(detail, style = MaterialTheme.typography.bodySmall, color = c.muted)
        }
    }
}
