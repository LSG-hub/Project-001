package app.project001.ui.family

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.project001.R
import app.project001.ui.components.EmptyState
import app.project001.ui.components.ScreenTitle

/** Family protection: coming next (the backend doesn't have it yet, so no sample data here). */
@Composable
fun FamilyScreen() {
    Column(Modifier.fillMaxSize()) {
        ScreenTitle(stringResource(R.string.nav_family))
        EmptyState(stringResource(R.string.family_soon_title), stringResource(R.string.family_soon_body))
    }
}
