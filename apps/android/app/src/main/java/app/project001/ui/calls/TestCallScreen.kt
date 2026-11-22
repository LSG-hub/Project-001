package app.project001.ui.calls

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.CallEnd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import app.project001.R
import app.project001.data.calls.TestCallState.Phase
import app.project001.graph
import app.project001.ui.components.BackRow
import app.project001.ui.components.Panel
import app.project001.ui.components.SectionLabel
import app.project001.ui.theme.AppTheme
import kotlinx.coroutines.launch

/**
 * Talk to your own assistant from the phone, like a caller would. It uses your voice
 * choice and what it knows about you; the call is saved with its transcript and summary.
 */
@Composable
fun TestCallScreen(onBack: () -> Unit) {
    val c = AppTheme.colors
    val context = LocalContext.current
    val session = context.graph.testCall
    val state by session.state.collectAsState()
    val scope = rememberCoroutineScope()
    var callerNumber by rememberSaveable { mutableStateOf("") }
    var micDenied by rememberSaveable { mutableStateOf(false) }

    val askMic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        micDenied = !granted
        if (granted) scope.launch { session.start(callerNumber) }
    }
    fun begin() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            scope.launch { session.start(callerNumber) }
        } else {
            askMic.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Keep the screen on during a call; leaving the screen hangs up.
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = false
            session.hangUp()
            session.close()
        }
    }
    LaunchedEffect(Unit) { session.reset() }

    val scroll = rememberScrollState()
    LaunchedEffect(state.lines.size, state.lines.lastOrNull()?.text?.length) { scroll.animateScrollTo(scroll.maxValue) }

    Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
        BackRow(onBack, title = stringResource(R.string.test_title))
        Column(Modifier.padding(horizontal = 16.dp).widthIn(max = 640.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (state.phase) {
                Phase.Idle, Phase.Failed -> {
                    Panel {
                        Text(stringResource(R.string.test_intro), style = MaterialTheme.typography.bodyLarge)
                        OutlinedTextField(
                            value = callerNumber,
                            onValueChange = { v -> callerNumber = v.filter { it.isDigit() || it == '+' || it == ' ' }.take(18) },
                            label = { Text(stringResource(R.string.test_caller_number)) },
                            supportingText = { Text(stringResource(R.string.test_caller_number_hint)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                        )
                        Button(onClick = ::begin, modifier = Modifier.padding(top = 8.dp).fillMaxWidth().heightIn(min = 52.dp)) {
                            Text(stringResource(R.string.test_start))
                        }
                    }
                    if (micDenied) Text(stringResource(R.string.test_mic_needed), color = c.scam.ink, style = MaterialTheme.typography.bodyMedium)
                    state.error?.let { Text(it, color = c.scam.ink, style = MaterialTheme.typography.bodyMedium) }
                }
                Phase.Connecting -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
                    Text(stringResource(R.string.test_connecting), style = MaterialTheme.typography.titleMedium)
                }
                Phase.Live, Phase.Ending -> {
                    Text(
                        stringResource(if (state.phase == Phase.Live) R.string.test_live else R.string.test_ending),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                    state.lines.forEach { Bubble(it.fromAssistant, it.text, cutOff = it.cutOff) }
                    state.note?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = c.muted) }
                    if (state.phase == Phase.Live) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            FilterChip(
                                selected = state.speaker,
                                onClick = { session.setSpeaker(!state.speaker) },
                                label = { Text(stringResource(R.string.test_speaker)) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            )
                            Button(
                                onClick = session::hangUp,
                                colors = ButtonDefaults.buttonColors(containerColor = c.scam.ink),
                                modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                            ) {
                                Icon(Icons.Outlined.CallEnd, contentDescription = null)
                                Text(stringResource(R.string.test_hang_up), modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text(stringResource(R.string.test_writing_summary), color = c.muted)
                        }
                    }
                }
                Phase.Done -> {
                    val call = state.call
                    if (call != null) {
                        SummaryCard(call)
                        SectionLabel(stringResource(R.string.call_transcript), Modifier.padding(top = 4.dp, start = 4.dp))
                        Transcript(call.turns)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 24.dp)) {
                        OutlinedButton(onClick = { session.reset() }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.test_again)) }
                        Button(onClick = onBack, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.test_done)) }
                    }
                }
            }
        }
    }
}
