package aanibrothers.tracker.io.extension

import aanibrothers.tracker.io.*

data class LanguageInfo(val languageCode: String, val languageName: String, val icon: Int)

fun getLanguages(): MutableList<LanguageInfo> {
    val languages = mutableListOf<LanguageInfo>()
    languages.add(LanguageInfo(languageName = "English", languageCode = "en", icon = R.drawable.ic_language_english))
    languages.add(LanguageInfo(languageName = "Hindi / हिंदी", languageCode = "hi", icon = R.drawable.ic_language_india))
    languages.add(LanguageInfo(languageName = "German / Deutsch", languageCode = "de", icon = R.drawable.ic_language_german))
    languages.add(LanguageInfo(languageName = "French / Français", languageCode = "fr", icon = R.drawable.ic_language_france))
    languages.add(LanguageInfo(languageName = "Arabic / العربية", languageCode = "ar", icon = R.drawable.ic_language_arab))
    languages.add(LanguageInfo(languageName = "Japanese / 日本語", languageCode = "ja", icon = R.drawable.ic_language_japan))
    languages.add(LanguageInfo(languageName = "Spanish / Española", languageCode = "es", icon = R.drawable.ic_language_spanish))
    languages.add(LanguageInfo(languageName = "Indonesian / bahasa Indonesia", languageCode = "in", icon = R.drawable.ic_language_indonesian))
    languages.add(LanguageInfo(languageName = "African / Afrikaans", languageCode = "af", icon = R.drawable.ic_language_african))
    languages.add(LanguageInfo(languageName = "Portuguese / Português", languageCode = "pt", icon = R.drawable.ic_language_portugal))
    return languages
}