package app.project001.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeChoice { Light, Dark, System }

/** The app's own settings, kept on this phone. Light (the design's look) is the default. */
class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(
        runCatching { ThemeChoice.valueOf(prefs.getString(KEY_THEME, null) ?: "") }.getOrDefault(ThemeChoice.Light),
    )
    val theme: StateFlow<ThemeChoice> = _theme.asStateFlow()

    private val _simpleMode = MutableStateFlow(prefs.getBoolean(KEY_SIMPLE, false))
    val simpleMode: StateFlow<Boolean> = _simpleMode.asStateFlow()

    fun setTheme(choice: ThemeChoice) {
        prefs.edit().putString(KEY_THEME, choice.name).apply()
        _theme.value = choice
    }

    fun setSimpleMode(on: Boolean) {
        prefs.edit().putBoolean(KEY_SIMPLE, on).apply()
        _simpleMode.value = on
    }

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_SIMPLE = "simple_mode"
    }
}
