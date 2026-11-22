package app.project001.ui.calls

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.project001.R
import app.project001.data.api.ApiException
import app.project001.data.api.Call
import app.project001.graph
import app.project001.ui.components.EmptyState
import app.project001.ui.components.IconBadge
import app.project001.ui.components.Panel
import app.project001.ui.components.Pill
import app.project001.ui.components.ScreenTitle
import app.project001.ui.components.callTone
import app.project001.ui.components.categoryLabel
import app.project001.ui.theme.AppTheme
import kotlinx.coroutines.launch

/** Your calls, newest first, as the backend saved them (transcript and summary). */
@Composable
fun CallsScreen(onOpenCall: (String) -> Unit, onTestCall: () -> Unit) {
    val repo = LocalContext.current.graph.calls
    val scope = rememberCoroutineScope()
    var calls by remember { mutableStateOf<List<Call>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    fun load() {
        scope.launch {
            loading = true
            try {
                calls = repo.recent()
                error = null
            } catch (e: ApiException) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }
    LifecycleResumeEffect(Unit) {
        load()
        onPauseOrDispose { }
    }

    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ScreenTitle(stringResource(R.string.calls_title), Modifier.weight(1f), subtitle = stringResource(R.string.calls_subtitle))
                IconButton(onClick = ::load, modifier = Modifier.padding(end = 8.dp)) {
                    if (loading) CircularProgressIndicator(Modifier.padding(4.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.calls_refresh))
                }
            }
        }
        item {
            Panel(Modifier.padding(horizontal = 16.dp).widthIn(max = 640.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBadge(Icons.Outlined.Mic, AppTheme.colors.safe)
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.calls_test_title), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.calls_test_body), style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.muted)
                    }
                }
                Button(onClick = onTestCall, modifier = Modifier.padding(top = 12.dp).fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.calls_test_button))
                }
            }
        }
        when {
            error != null && calls == null -> item {
                EmptyState(stringResource(R.string.calls_error_title), error.orEmpty()) {
                    TextButton(onClick = ::load) { Text(stringResource(R.string.calls_retry)) }
                }
            }
            calls == null -> item {
                Row(Modifier.fillMaxWidth().padding(32.dp), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() }
            }
            calls!!.isEmpty() -> item {
                EmptyState(stringResource(R.string.calls_empty_title), stringResource(R.string.calls_empty_body))
            }
            else -> items(calls!!, key = { it.id }) { call -> CallRow(call, onClick = { onOpenCall(call.id) }) }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun CallRow(call: Call, onClick: () -> Unit) {
    val c = AppTheme.colors
    val s = call.summary
    val scam = s?.category == "scam" || s?.scamRisk == "high"
    Panel(Modifier.padding(horizontal = 16.dp).widthIn(max = 640.dp).clickable(onClick = onClick)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            IconBadge(if (scam) Icons.Outlined.Warning else Icons.Outlined.Call, callTone(s?.category, s?.scamRisk))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(callerName(call), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(callTime(call.startedAt), style = MaterialTheme.typography.labelMedium, color = c.muted)
                }
                Text(
                    s?.summary ?: stringResource(R.string.call_no_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.ink.copy(alpha = 0.8f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Pill(categoryLabel(s?.category), callTone(s?.category, s?.scamRisk), Modifier.padding(top = 8.dp))
            }
        }
    }
}
