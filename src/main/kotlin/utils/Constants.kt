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
        const val JWT_EXPIRATION_MS = 900_000L // 15 minutes
    }

    // Auth related constants
    object Auth {
        const val PROVIDER_LOCAL = "LOCAL"
    }

    // Logging tags and messages
    object Logging {
        const val GLOBAL_TAG = "DivishareBackend"
        const val USER_SERVICE_TAG = "UserService"

        object LogMessages {
            const val REGISTRATION_EMAIL_EXISTS = "Registration attempt failed: Email already exists -> %s"
            const val REGISTRATION_SUCCESS = "User successfully registered -> %s"
            const val REGISTRATION_ERROR = "Critical database error while registering user: %s"

            const val LOGIN_USER_NOT_FOUND = "Login attempt failed: User not found -> %s"
            const val LOGIN_NO_HASH = "Login attempt failed: No password hash found for user -> %s"
            const val LOGIN_INVALID_PASSWORD = "Login attempt failed: Invalid password for user -> %s"
            const val LOGIN_ERROR = "Critical database error during login for user: %s"

            const val REFRESH_TOKEN_SAVED = "Refresh token updated successfully for user -> %s"
            const val REFRESH_TOKEN_SAVE_ERROR = "Failed to save refresh token for user: %s"

            const val REFRESH_TOKEN_VALIDATION_FAILED = "Refresh token validation failed for user -> %s"
            const val REFRESH_TOKEN_VALIDATION_ERROR = "Critical error validating refresh token for user: %s"
        }
    }
}