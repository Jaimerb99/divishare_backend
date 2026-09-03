package com.jrb.utils

object Constants {

    // Keys that match exactly the ones in your .properties files
    object Messages {
        const val REGISTER_SUCCESS = "register_success"
        const val REGISTER_CONFLICT = "register_conflict"
        const val LOGIN_SUCCESS = "login_success"
        const val LOGIN_ERROR = "login_error"
        const val ERROR_BAD_REQUEST = "error_bad_request"
        const val ERROR_INTERNAL_SERVER = "error_internal_server"
        const val REFRESH_SUCCESS = "refresh_success"
        const val REFRESH_ERROR = "refresh_error"
    }

    // Configuration for localization and language defaults
    object Localization {
        const val DEFAULT_LANG_CODE = "en"
        const val SPANISH_LANG_PREFIX = "es"
        const val BUNDLE_BASE_NAME = "messages"
    }

    object Security {
        // In a real production app, NEVER hardcode the secret here. Use environment variables.
        // For development, this is fine.
        const val JWT_SECRET = "divishare-super-secret-key-2026"
        const val JWT_ISSUER = "http://localhost:8080/"
        const val JWT_AUDIENCE = "divishare-users"
        const val JWT_EXPIRATION_MS = 86400000L // 24 hours in milliseconds        900_000L // 15 minutes
    }
}