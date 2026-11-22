package app.project001.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.project001.R
import app.project001.ui.theme.AppTheme
import app.project001.ui.theme.Tone

/** Screen title in Bricolage, used at the top of each tab. */
@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier, subtitle: String? = null) {
    Column(modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp)) {
        Text(text, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.muted, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun BackRow(onBack: () -> Unit, title: String? = null, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
        }
        if (title != null) {
            Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f).semantics { heading() })
        } else {
            Box(Modifier.weight(1f))
        }
        trailing()
    }
}

/** White rounded card used for every group on the paper background. */
@Composable
fun Panel(modifier: Modifier = Modifier, padding: Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AppTheme.colors.card)
            .padding(padding),
        content = content,
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.muted, modifier = modifier.semantics { heading() })
}

@Composable
fun Pill(text: String, tone: Tone, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = tone.ink,
        modifier = modifier.clip(CircleShape).background(tone.tint).padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

@Composable
fun IconBadge(icon: ImageVector, tone: Tone, size: Dp = 40.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(tone.tint), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tone.ink, modifier = Modifier.size(size / 2))
    }
}

@Composable
fun SettingRow(label: String, value: String? = null, onClick: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null, color: androidx.compose.ui.graphics.Color? = null, divider: Boolean = true) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .then(if (onClick != null) Modifier.clickableRow(onClick) else Modifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize), color = color ?: AppTheme.colors.ink, modifier = Modifier.weight(1f).padding(vertical = 8.dp))
            if (value != null) Text(value, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.muted, textAlign = androidx.compose.ui.text.style.TextAlign.End, modifier = Modifier.weight(1f, fill = false))
            trailing?.invoke()
        }
        if (divider) HorizontalDivider(color = AppTheme.colors.line)
    }
}

private fun Modifier.clickableRow(onClick: () -> Unit) = this.clickable(onClick = onClick)

/** Colour for a call, from its summary: scams red, warnings amber, deliveries and people green. */
@Composable
fun callTone(category: String?, scamRisk: String?): Tone = when {
    category == "scam" || scamRisk == "high" -> AppTheme.colors.scam
    scamRisk == "medium" -> AppTheme.colors.caution
    category in setOf("spam", "sales") -> AppTheme.colors.neutral
    category in setOf("personal", "work", "delivery") -> AppTheme.colors.safe
    category == null -> AppTheme.colors.neutral
    else -> AppTheme.colors.info
}

@Composable
fun categoryLabel(category: String?): String = stringResource(
    when (category) {
        "scam" -> R.string.category_scam
        "spam" -> R.string.category_spam
        "sales" -> R.string.category_sales
        "delivery" -> R.string.category_delivery
        "personal" -> R.string.category_personal
        "work" -> R.string.category_work
        "service" -> R.string.category_service
        null -> R.string.category_no_summary
        else -> R.string.category_other
    },
)

/** A simple centred message for empty or not-yet-built screens. */
@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        action?.invoke()
    }
}
