package app.project001.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.project001.ui.theme.AppTheme

/**
 * A one-time code input. It is one real text field (so paste, SMS autofill,
 * backspace and screen readers all behave normally), drawn as one box per digit.
 *
 * [onComplete] fires as soon as the last digit is in, so there is no button to find.
 * When [error] is set the boxes turn red and the message is announced; typing again
 * clears it (the caller resets [error] in [onValueChange]).
 */
@Composable
fun CodeInput(
    value: String,
    onValueChange: (String) -> Unit,
    onComplete: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    length: Int = 6,
    error: String? = null,
    enabled: Boolean = true,
    autoFocus: Boolean = true,
) {
    val c = AppTheme.colors
    val focus = remember { FocusRequester() }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    if (autoFocus) LaunchedEffect(Unit) { focus.requestFocus() }

    Column(modifier) {
        BasicTextField(
            value = value,
            onValueChange = { raw ->
                val digits = raw.filter { it.isDigit() }.take(length)
                if (digits != value) {
                    onValueChange(digits)
                    if (digits.length == length) onComplete(digits)
                }
            },
            enabled = enabled,
            singleLine = true,
            interactionSource = interaction,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (value.length == length) onComplete(value) }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focus)
                .semantics {
                    contentDescription = label
                    contentType = ContentType.SmsOtpCode
                    if (error != null) error(error)
                },
            decorationBox = { _ ->
                // The real text is never drawn; the boxes below show it.
                Row(
                    Modifier.fillMaxWidth().clearAndSetSemantics { },
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    repeat(length) { i ->
                        val digit = value.getOrNull(i)
                        val active = focused && enabled && (i == value.length || (i == length - 1 && value.length == length))
                        val border = when {
                            error != null -> c.scam.ink
                            active -> c.accent
                            digit != null -> c.ink.copy(alpha = 0.35f)
                            else -> c.chipLine
                        }
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(0.82f)
                                .heightIn(min = 56.dp)
                                .background(if (error != null) c.scam.tint else c.card, RoundedCornerShape(14.dp))
                                .border(if (active || error != null) 2.dp else 1.dp, border, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (digit != null) {
                                Text(
                                    digit.toString(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = if (error != null) c.scam.ink else c.ink,
                                    textAlign = TextAlign.Center,
                                )
                            } else if (active && value.length < length) {
                                Caret()
                            }
                        }
                    }
                }
            },
        )
        if (error != null) {
            Text(
                error,
                style = MaterialTheme.typography.bodyMedium,
                color = c.scam.ink,
                modifier = Modifier.padding(top = 10.dp).semantics { liveRegion = LiveRegionMode.Assertive },
            )
        }
    }
}

@Composable
private fun Caret() {
    val blink by rememberInfiniteTransition(label = "caret").animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            keyframes { durationMillis = 1000; 1f at 0; 1f at 499; 0f at 500; 0f at 999 },
            RepeatMode.Restart,
        ),
        label = "caret",
    )
    Box(Modifier.width(2.dp).height(28.dp).alpha(blink).background(AppTheme.colors.accent))
}
