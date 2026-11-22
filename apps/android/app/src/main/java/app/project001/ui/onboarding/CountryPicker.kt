package app.project001.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.project001.R
import app.project001.phone.Country
import app.project001.phone.Phones
import app.project001.phone.matches
import app.project001.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The country part of the number field: flag on the left, code on the right. Tap to change. */
@Composable
fun CountryButton(country: Country, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    val description = stringResource(R.string.signin_country_button, country.name, country.plusCode)
    val change = stringResource(R.string.signin_country_change)
    Row(
        modifier
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(c.card)
            .border(1.dp, c.chipLine, RoundedCornerShape(14.dp))
            .clickable(onClickLabel = change, role = Role.Button, onClick = onClick)
            .clearAndSetSemantics { contentDescription = description }
            .padding(start = 14.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(country.flag, fontSize = 24.sp)
        Text(country.plusCode, style = MaterialTheme.typography.bodyLarge, color = c.ink)
        Icon(Icons.Outlined.ExpandMore, contentDescription = null, tint = c.muted, modifier = Modifier.size(20.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryPickerSheet(selected: String, onPick: (Country) -> Unit, onDismiss: () -> Unit) {
    val c = AppTheme.colors
    val context = LocalContext.current
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by rememberSaveable { mutableStateOf("") }
    val all by produceState(initialValue = emptyList<Country>()) {
        value = withContext(Dispatchers.Default) { Phones.countries(context) }
    }
    val sim = remember { Phones.simRegion(context) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = c.paper) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.92f).imePadding().padding(horizontal = 20.dp)) {
            Text(
                stringResource(R.string.country_picker_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.country_search)) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = c.muted) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = c.card,
                    unfocusedContainerColor = c.card,
                    focusedBorderColor = c.accent,
                    unfocusedBorderColor = c.chipLine,
                ),
                modifier = Modifier.padding(top = 14.dp, bottom = 8.dp).fillMaxWidth(),
            )

            val results = remember(all, query) { all.filter { it.matches(query) } }
            val suggested = remember(all, sim, selected) {
                listOfNotNull(sim, selected).distinct().mapNotNull { r -> all.firstOrNull { it.region == r } }
            }
            LazyColumn(Modifier.fillMaxWidth()) {
                if (query.isBlank() && suggested.isNotEmpty()) {
                    item(key = "h-suggested") { SheetLabel(stringResource(R.string.country_suggested)) }
                    items(suggested, key = { "s-" + it.region }) { CountryRow(it, it.region == selected) { onPick(it) } }
                    item(key = "h-all") { SheetLabel(stringResource(R.string.country_all)) }
                }
                items(results, key = { it.region }) { CountryRow(it, it.region == selected) { onPick(it) } }
                if (all.isNotEmpty() && results.isEmpty()) {
                    item(key = "none") {
                        Text(stringResource(R.string.country_none), color = c.muted, modifier = Modifier.padding(vertical = 24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = AppTheme.colors.muted,
        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp).semantics { heading() },
    )
}

@Composable
private fun CountryRow(country: Country, isSelected: Boolean, onClick: () -> Unit) {
    val c = AppTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) c.safe.tint else c.paper)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics(mergeDescendants = true) { selected = isSelected }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(country.flag, fontSize = 24.sp)
        Text(
            country.name,
            style = MaterialTheme.typography.bodyLarge,
            color = c.ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(country.plusCode, style = MaterialTheme.typography.bodyMedium, color = c.muted)
        Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            if (isSelected) Icon(Icons.Outlined.Check, contentDescription = null, tint = c.accent, modifier = Modifier.size(20.dp))
        }
    }
}
