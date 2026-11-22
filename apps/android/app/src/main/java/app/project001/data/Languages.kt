package app.project001.data

/** A language the app's own screens can be shown in (the assistant speaks every caller's language). */
data class Language(
    val tag: String?,
    val native: String,
    val english: String,
    val rtl: Boolean = false,
    val searchAlso: String = "",
)

object Languages {
    val languages = listOf(
        Language(null, "", "", searchAlso = "device system default phone"),
        Language("en", "English", "English"),
        Language("hi", "हिन्दी", "Hindi", searchAlso = "hindi"),
        Language("te", "తెలుగు", "Telugu", searchAlso = "telugu"),
        Language("ta", "தமிழ்", "Tamil", searchAlso = "tamil"),
        Language("kn", "ಕನ್ನಡ", "Kannada", searchAlso = "kannada"),
        Language("ml", "മലയാളം", "Malayalam", searchAlso = "malayalam"),
        Language("mr", "मराठी", "Marathi", searchAlso = "marathi"),
        Language("bn", "বাংলা", "Bengali", searchAlso = "bangla bengali"),
        Language("es", "Español", "Spanish", searchAlso = "espanol"),
        Language("fr", "Français", "French", searchAlso = "francais"),
        Language("pt", "Português", "Portuguese", searchAlso = "portugues"),
        Language("id", "Bahasa Indonesia", "Indonesian"),
        Language("ar", "العربية", "Arabic", rtl = true, searchAlso = "arabic"),
    )
}
