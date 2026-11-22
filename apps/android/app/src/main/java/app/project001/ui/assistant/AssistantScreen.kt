package app.project001.ui.assistant

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.project001.R
import app.project001.ui.components.EmptyState
import app.project001.ui.components.ScreenTitle

/** Talking to your own assistant to get things done: coming next. */
@Composable
fun AssistantScreen() {
    Column(Modifier.fillMaxSize()) {
        ScreenTitle(stringResource(R.string.nav_assistant))
        EmptyState(stringResource(R.string.assistant_soon_title), stringResource(R.string.assistant_soon_body))
    }
}
