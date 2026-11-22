package app.project001.ui.calls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.project001.R
import app.project001.data.api.Call
import app.project001.data.api.Turn
import app.project001.ui.components.Panel
import app.project001.ui.components.Pill
import app.project001.ui.components.SectionLabel
import app.project001.ui.components.callTone
import app.project001.ui.components.categoryLabel
import app.project001.ui.theme.AppTheme
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Who called, as best we know: the name they gave, else their number, else "Unknown caller". */
@Composable
fun callerName(call: Call): String =
    call.summary?.caller ?: call.callerNumber ?: stringResource(R.string.call_unknown_caller)

/** "11:02" today, "Yesterday", or a short date. */
@Composable
fun callTime(iso: String): String {
    val local = runCatching { OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault()) }.getOrNull() ?: return ""
    val today = LocalDate.now()
    return when (local.toLocalDate()) {
        today -> local.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
        today.minusDays(1) -> stringResource(R.string.call_yesterday)
        else -> local.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }
}

/** The summary the owner reads instead of listening: what happened, warning signs, to-dos. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SummaryCard(call: Call, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    val s = call.summary
    Panel(modifier) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Pill(categoryLabel(s?.category), callTone(s?.category, s?.scamRisk))
            if (s != null) {
                Pill(stringResource(R.string.call_scam_risk, riskLabel(s.scamRisk)), callTone(null, s.scamRisk).takeIf { s.scamRisk != "none" } ?: c.safe)
                Pill(s.language, c.neutral)
            }
            Pill(stringResource(R.string.call_duration, call.durationSeconds.toInt()), c.neutral)
            call.voice?.let { Pill(stringResource(R.string.call_voice, it), c.neutral) }
        }
        if (s == null) {
            Text(stringResource(R.string.call_no_summary), style = MaterialTheme.typography.bodyLarge, color = c.muted, modifier = Modifier.padding(top = 12.dp))
            return@Panel
        }
        Text(s.summary, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
        Text(
            stringResource(R.string.call_caller_purpose, s.caller ?: stringResource(R.string.call_not_given), s.purpose),
            style = MaterialTheme.typography.bodyMedium,
            color = c.muted,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (s.scamSignals.isNotEmpty()) Bullets(stringResource(R.string.call_warning_signs), s.scamSignals)
        if (s.actionItems.isNotEmpty()) Bullets(stringResource(R.string.call_to_do), s.actionItems)
    }
}

@Composable
private fun riskLabel(risk: String): String = stringResource(
    when (risk) {
        "high" -> R.string.risk_high
        "medium" -> R.string.risk_medium
        "low" -> R.string.risk_low
        else -> R.string.risk_none
    },
)

@Composable
private fun Bullets(title: String, items: List<String>) {
    SectionLabel(title, Modifier.padding(top = 14.dp, bottom = 4.dp))
    items.forEach { Text("•  $it", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp)) }
}

/** Chat-style transcript: the assistant on the left, the caller on the right. */
@Composable
fun Transcript(turns: List<Turn>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        turns.forEach { Bubble(it.fromAssistant, it.text, cutOff = it.interrupted, time = it.at) }
    }
}

@Composable
fun Bubble(fromAssistant: Boolean, text: String, cutOff: Boolean = false, time: Double? = null) {
    val c = AppTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (fromAssistant) Arrangement.Start else Arrangement.End, verticalAlignment = Alignment.Top) {
        Column(
            Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth(0.86f)
                .clip(RoundedCornerShape(16.dp))
                .background(if (fromAssistant) c.safe.tint else c.card)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            val who = stringResource(if (fromAssistant) R.string.call_assistant else R.string.call_caller)
            Text(
                if (time != null) "$who · ${"%.0f".format(time)}s" else who,
                style = MaterialTheme.typography.labelSmall,
                color = c.muted,
                fontWeight = FontWeight.Medium,
            )
            Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 2.dp))
            if (cutOff) Text(stringResource(R.string.call_cut_off), style = MaterialTheme.typography.labelSmall, color = c.muted)
        }
    }
}
