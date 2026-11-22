package app.project001.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.project001.R
import app.project001.ui.components.BackRow
import app.project001.ui.theme.AppTheme

/** Setup checklist. Forwarding uses the carrier's standard codes; this screen only shows the flow. */
@Composable
fun SetupScreen(onBack: () -> Unit) {
    val c = AppTheme.colors
    var done by rememberSaveable { mutableIntStateOf(2) }
    val total = 4
    Column(Modifier.fillMaxSize()) {
        BackRow(onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).widthIn(max = 640.dp)) {
            LinearProgressIndicator(progress = { done / total.toFloat() }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape), color = c.accent, trackColor = c.chipLine, drawStopIndicator = {})
            Text(stringResource(R.string.setup_step, (done + 1).coerceAtMost(total), total), style = MaterialTheme.typography.bodyMedium, color = c.muted, modifier = Modifier.padding(top = 20.dp))
            Text(stringResource(R.string.setup_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 6.dp).semantics { heading() })
            Text(stringResource(R.string.setup_body, "Jio"), style = MaterialTheme.typography.bodyLarge, color = c.ink.copy(alpha = 0.85f), modifier = Modifier.padding(top = 12.dp))
            Spacer(Modifier.height(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Step(1, stringResource(R.string.setup_signed_in), null, state(1, done))
                Step(2, stringResource(R.string.setup_screening), null, state(2, done))
                Step(3, stringResource(R.string.setup_forwarding), stringResource(R.string.setup_forwarding_detail), state(3, done))
                Step(4, stringResource(R.string.setup_test), null, state(4, done))
            }
            if (done >= 3) {
                Text(stringResource(R.string.setup_done), style = MaterialTheme.typography.bodyMedium, color = c.safe.ink, modifier = Modifier.padding(top = 16.dp))
            }
            Spacer(Modifier.height(16.dp))
        }
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp).widthIn(max = 640.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { done = (done + 1).coerceAtMost(total) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text(stringResource(R.string.setup_turn_on), style = MaterialTheme.typography.titleMedium)
            }
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.setup_myself), color = c.ink, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium))
            }
        }
    }
}

private enum class StepState { Done, Current, Todo }

private fun state(n: Int, done: Int) = when {
    n <= done -> StepState.Done
    n == done + 1 -> StepState.Current
    else -> StepState.Todo
}

@Composable
private fun Step(n: Int, title: String, detail: String?, s: StepState) {
    val c = AppTheme.colors
    val border = if (s == StepState.Current) Modifier.border(BorderStroke(2.dp, c.accent), RoundedCornerShape(18.dp)) else Modifier
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.card).then(border).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(32.dp).clip(CircleShape).then(
                    if (s == StepState.Done) Modifier.background(c.accent) else Modifier.border(2.dp, if (s == StepState.Current) c.accent else c.chipLine, CircleShape),
                ),
                contentAlignment = Alignment.Center,
            ) {
                if (s == StepState.Done) Icon(Icons.Outlined.Check, contentDescription = null, tint = c.onAccent, modifier = Modifier.size(16.dp))
                else Text(n.toString(), style = MaterialTheme.typography.labelLarge, color = if (s == StepState.Current) c.accent else c.muted)
            }
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = if (s == StepState.Current) FontWeight.SemiBold else FontWeight.Normal), color = if (s == StepState.Todo) c.muted else c.ink)
        }
        if (detail != null && s == StepState.Current) {
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = c.muted, modifier = Modifier.padding(start = 44.dp, top = 8.dp))
        }
    }
}
