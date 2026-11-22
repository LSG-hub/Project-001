package app.project001.ui.onboarding

import android.app.role.RoleManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.project001.BuildConfig
import app.project001.R
import app.project001.phone.Phones
import app.project001.ui.components.CodeInput
import app.project001.ui.theme.AppTheme
import app.project001.data.api.ApiException
import app.project001.graph
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Step { Number, Code }

/**
 * Sign-up and sign-in use the mobile number only, one account per number. The backend
 * calls the number and reads out a 6-digit code (in dev builds any 6 digits work until
 * the phone line is live); the code signs you in.
 */
@Composable
fun SignInScreen() {
    val c = AppTheme.colors
    val context = LocalContext.current
    val signIn = context.graph.signIn
    val scope = rememberCoroutineScope()
    var step by rememberSaveable { mutableStateOf(Step.Number) }
    var region by rememberSaveable { mutableStateOf(Phones.defaultRegion(context)) }
    var number by rememberSaveable { mutableStateOf("") }
    var verificationId by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val fullNumber = remember(region, number) { Phones.international(context, number, region) }
    val notSetUp = stringResource(R.string.signin_not_set_up)

    fun startVerification() {
        scope.launch {
            busy = true
            error = null
            try {
                val started = signIn.start(Phones.e164(context, number, region), Locale.getDefault().toLanguageTag())
                verificationId = started.id
                step = Step.Code
            } catch (e: ApiException) {
                error = e.message
            } finally {
                busy = false
            }
        }
    }

    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).widthIn(max = 520.dp)) {
        Spacer(Modifier.height(48.dp))
        Image(painterResource(R.drawable.logo_mark), contentDescription = null, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(28.dp))
        when (step) {
            Step.Number -> NumberStep(region, number, busy, onRegion = { region = it }, onNumber = { number = it }, onContinue = ::startVerification)
            Step.Code -> VoiceCodeStep(
                fullNumber = fullNumber,
                check = { code ->
                    try {
                        signIn.submitCode(verificationId ?: error("No verification"), code)
                        null // signed in: the app switches to the main screens by itself
                    } catch (e: ApiException) {
                        e.message
                    } catch (e: Exception) {
                        // Firebase errors read like "An internal error has occurred. [ CODE ]".
                        if (e.message.orEmpty().contains("CONFIGURATION_NOT_FOUND")) notSetUp else (e.message ?: "Couldn't sign in.")
                    }
                },
                onCallAgain = ::startVerification,
                onChangeNumber = { step = Step.Number; error = null },
            )
        }
        error?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = c.scam.ink, modifier = Modifier.padding(top = 12.dp).semantics { liveRegion = LiveRegionMode.Assertive })
        }
        Text(stringResource(R.string.signin_terms), style = MaterialTheme.typography.bodySmall, color = c.muted, modifier = Modifier.padding(top = 24.dp, bottom = 24.dp))
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = AppTheme.colors.card,
    unfocusedContainerColor = AppTheme.colors.card,
    focusedBorderColor = AppTheme.colors.accent,
    unfocusedBorderColor = AppTheme.colors.chipLine,
)

@Composable
private fun NumberStep(region: String, number: String, busy: Boolean, onRegion: (String) -> Unit, onNumber: (String) -> Unit, onContinue: () -> Unit) {
    val c = AppTheme.colors
    val context = LocalContext.current
    val country = remember(region) { Phones.country(context, region) }
    var picking by rememberSaveable { mutableStateOf(false) }
    var invalid by rememberSaveable { mutableStateOf(false) }
    val possible = remember(region, number) { !number.startsWith("+") && Phones.isPossible(context, number, region) }

    Text(stringResource(R.string.signin_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
    Text(stringResource(R.string.signin_body), style = MaterialTheme.typography.bodyLarge, color = c.ink.copy(alpha = 0.85f), modifier = Modifier.padding(top = 12.dp))
    // Same height as the number field at any text size: IntrinsicSize.Min + fillMaxHeight.
    Row(Modifier.padding(top = 28.dp).height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        // OutlinedTextField reserves 8dp above its outline for the floating label; match it.
        CountryButton(country, onClick = { picking = true }, modifier = Modifier.padding(top = 8.dp).fillMaxHeight())
        OutlinedTextField(
            value = number,
            onValueChange = { v ->
                invalid = false
                // Keep a leading "+" while someone types a full international number;
                // once it is a whole number, switch the country and keep the local part.
                val cleaned = (if (v.trimStart().startsWith("+")) "+" else "") + v.filter { it.isDigit() }.take(15)
                val split = Phones.splitInternational(context, cleaned)
                if (split != null) {
                    onRegion(split.first)
                    onNumber(split.second)
                } else {
                    onNumber(cleaned)
                }
            },
            label = { Text(stringResource(R.string.signin_number)) },
            isError = invalid,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors(),
        )
    }
    if (invalid) {
        Text(
            stringResource(R.string.signin_number_invalid, country.name),
            style = MaterialTheme.typography.bodyMedium,
            color = c.scam.ink,
            modifier = Modifier.padding(top = 8.dp).semantics { liveRegion = LiveRegionMode.Assertive },
        )
    }
    Button(
        onClick = { if (Phones.isValid(context, number, region)) onContinue() else invalid = true },
        enabled = possible && !busy,
        modifier = Modifier.padding(top = 20.dp).fillMaxWidth().heightIn(min = 56.dp),
    ) {
        Text(stringResource(R.string.signin_continue), style = MaterialTheme.typography.titleMedium)
    }

    if (picking) {
        CountryPickerSheet(
            selected = region,
            onPick = { picked -> onRegion(picked.region); invalid = false; picking = false },
            onDismiss = { picking = false },
        )
    }
}

@Composable
private fun VoiceCodeStep(fullNumber: String, check: suspend (String) -> String?, onCallAgain: () -> Unit, onChangeNumber: () -> Unit) {
    val c = AppTheme.colors
    val scope = rememberCoroutineScope()
    var code by rememberSaveable { mutableStateOf("") }
    var problem by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    Text(stringResource(R.string.voice_code_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
    Text(stringResource(R.string.voice_code_body, fullNumber), style = MaterialTheme.typography.bodyLarge, color = c.ink.copy(alpha = 0.85f), modifier = Modifier.padding(top = 12.dp))
    CodeInput(
        value = code,
        onValueChange = { code = it; problem = null },
        onComplete = { entered ->
            scope.launch {
                checking = true
                problem = check(entered)
                checking = false
                if (problem != null) code = ""
            }
        },
        label = stringResource(R.string.code_label),
        error = problem,
        enabled = !checking,
        modifier = Modifier.padding(top = 28.dp).fillMaxWidth(),
    )
    if (checking) {
        Text(stringResource(R.string.signin_checking), style = MaterialTheme.typography.bodyMedium, color = c.muted, modifier = Modifier.padding(top = 12.dp))
    }
    if (BuildConfig.DEBUG) {
        Text(
            stringResource(R.string.voice_code_test_hint),
            style = MaterialTheme.typography.bodySmall,
            color = c.caution.ink,
            modifier = Modifier.padding(top = 14.dp).clip(RoundedCornerShape(10.dp)).background(c.caution.tint).padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
    TextButton(onClick = onCallAgain, modifier = Modifier.padding(top = 12.dp).fillMaxWidth()) {
        Text(stringResource(R.string.signin_call_again), color = c.ink)
    }
    TextButton(onClick = onChangeNumber, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.signin_change_number), color = c.ink)
    }
}
