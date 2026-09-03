package com.jrb.utils

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import java.util.ResourceBundle
import java.util.Locale

enum class AppLanguage(val code: String) {
    EN(Constants.Localization.DEFAULT_LANG_CODE),
    ES(Constants.Localization.SPANISH_LANG_PREFIX)
}

// Extension function to extract the language directly from the HTTP call
fun ApplicationCall.getLanguage(): AppLanguage {
    // Read the "Accept-Language" header sent
    val header = request.header(HttpHeaders.AcceptLanguage) ?: Constants.Localization.DEFAULT_LANG_CODE

    return when {
        header.lowercase().startsWith(Constants.Localization.SPANISH_LANG_PREFIX) -> AppLanguage.ES
        else -> AppLanguage.EN // Default fallback to English if no header or unsupported language is provided
    }
}

object Messages {
    fun get(key: String, lang: AppLanguage, vararg args: Any): String {
        // Fetch the corresponding .properties file based on the constant name
        val locale = Locale(lang.code)
        val bundle = ResourceBundle.getBundle(Constants.Localization.BUNDLE_BASE_NAME, locale)

        val text = if (bundle.containsKey(key)) bundle.getString(key) else key
        return String.format(text, *args)
    }
}