package app.project001.ui.calls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.project001.R
import app.project001.data.api.ApiException
import app.project001.data.api.Call
import app.project001.graph
import app.project001.ui.components.BackRow
import app.project001.ui.components.EmptyState
import app.project001.ui.components.SectionLabel

/** One call: the summary first, then the full transcript. */
@Composable
fun CallDetailScreen(callId: String, onBack: () -> Unit) {
    val repo = LocalContext.current.graph.calls
    var call by remember { mutableStateOf<Call?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(callId) {
        try {
            call = repo.get(callId)
        } catch (e: ApiException) {
            error = e.message
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        BackRow(onBack, title = call?.let { callerName(it) })
        val loaded = call
        when {
            error != null -> EmptyState(stringResource(R.string.calls_error_title), error.orEmpty())
            loaded == null -> Row(Modifier.fillMaxWidth().padding(32.dp), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() }
            else -> Column(Modifier.padding(horizontal = 16.dp).widthIn(max = 640.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard(loaded)
                SectionLabel(stringResource(R.string.call_transcript), Modifier.padding(top = 8.dp, start = 4.dp))
                Transcript(loaded.turns, Modifier.padding(bottom = 24.dp))
            }
        }
    }
}
