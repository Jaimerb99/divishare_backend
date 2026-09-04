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
        const val DELETE_SUCCESS = "delete_success"
        const val DELETE_ERROR = "delete_error"
        const val RECOVERY_INVALID = "recovery_invalid"
        const val EMAIL_RECOVERY_SUBJECT = "email_recovery_subject"
        const val EMAIL_RECOVERY_BODY = "email_recovery_body"
        const val RECOVERY_EMAIL_SENT = "recovery_email_sent"
        const val PIN_VALID = "pin_valid"
        const val PIN_INVALID = "pin_invalid"
        const val PASSWORD_RESET_SUCCESS = "password_reset_success"
        const val PASSWORD_RESET_ERROR = "password_reset_error"
        const val UNAUTHORIZED_ACCESS = "unauthorized_access"
        const val LOGIN_ERROR_PERMANENTLY_DELETED = "login_error_permanently_deleted"
        const val LOGIN_ERROR_REACTIVATE = "login_error_reactivate"
        const val LOGIN_ERROR_WRONG_PROVIDER = "login_error_wrong_provider"
        const val CURRENCY_UNAVAILABLE = "currency_unavailable"
    }

    // Configuration for localization and language defaults
    object Localization {
        const val DEFAULT_LANG_CODE = "en"
        const val SPANISH_LANG_PREFIX = "es"
        const val BUNDLE_BASE_NAME = "messages"
    }

    object User {
        const val GRACE_PERIOD_DAYS = 30
        const val DELETED_NAME = "Deleted User" // Constants not used for future cron job
        const val DELETED_EMAIL_DOMAIN = "@deleted.divishare.local"
        const val DELETED_EMAIL_PREFIX = "deleted_"
    }

    object Security {
        // In a real production app, NEVER hardcode the secret here. Use environment variables.
        // For development, this is fine.
        const val JWT_SECRET = "divishare-super-secret-key-2026"
        const val JWT_ISSUER = "http://localhost:8080/"
        const val JWT_AUDIENCE = "divishare-users"
        const val JWT_EXPIRATION_MS = 900_000L // 15 minutes
        const val AUTH_JWT_NAME = "auth-jwt"
        const val CLAIM_EMAIL = "email"
        const val PIN_MIN = 100000
        const val PIN_MAX = 999999
    }

    // Auth related constants
    object Auth {
        const val PROVIDER_LOCAL = "LOCAL"
    }

    // Logging tags and messages
    object Logging {
        const val GLOBAL_TAG = "DivishareBackend"
        const val USER_SERVICE_TAG = "UserService"
        const val EMAIL_SERVICE_TAG = "EmailService"

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
            const val DELETE_USER_SUCCESS = "User deleted successfully -> %s"
            const val DELETE_USER_NOT_FOUND = "Delete failed: User not found -> %s"
            const val DELETE_USER_ERROR = "Critical error deleting user: %s"

            const val RECOVERY_REQUESTED = "Password recovery requested for -> %s"
            const val RECOVERY_UNSUPPORTED_PROVIDER = "Recovery denied: User is not LOCAL -> %s"
            const val RECOVERY_USER_NOT_FOUND = "Recovery denied: User not found -> %s"
            const val RECOVERY_ERROR = "Critical error during password recovery for: %s"
            const val EMAIL_SENT_SUCCESS = "Recovery email successfully sent to -> %s"
            const val EMAIL_SENT_ERROR = "Critical error sending email to: %s"

            const val PIN_GENERATED = "Generated recovery PIN for user -> %s"
            const val PIN_VERIFICATION_SUCCESS = "PIN verified successfully for user -> %s"
            const val PIN_VERIFICATION_FAILED = "Invalid PIN attempt for user -> %s"
            const val PASSWORD_RESET_SUCCESSFUL = "Password successfully reset for user -> %s"
            const val PASSWORD_RESET_FAILED = "Failed to reset password for user -> %s"
            const val LOGIN_USER_INACTIVE = "Login attempt failed: User account is deactivated -> %s"
            const val LOGIN_USER_PERMANENTLY_DELETED = "Login attempt failed: Account permanently deleted -> %s"
            const val LOGIN_REACTIVATION_REQUIRED = "Login blocked: User must reset password to reactivate -> %s"
            const val LOGIN_WRONG_PROVIDER = "Login attempt failed: User must use %s -> %s"

            const val CURRENCY_FETCH_START = "Fetching latest currency rates from external API..."
            const val CURRENCY_FETCH_SUCCESS = "Successfully updated %d exchange rates."
            const val CURRENCY_FETCH_FAILED_HTTP = "Failed to fetch rates. HTTP Status: %d"
            const val CURRENCY_FETCH_EXCEPTION = "Exception while fetching currency rates"
        }
    }

    // Mail server conf
    object Email {
        const val SMTP_HOST_KEY = "mail.smtp.host"
        const val SMTP_PORT_KEY = "mail.smtp.port"
        const val SMTP_AUTH_KEY = "mail.smtp.auth"
        const val SMTP_STARTTLS_KEY = "mail.smtp.starttls.enable"
        const val TRUE_VALUE = "true"

        // IMPORTANT: In production, these values MUST come from environment variables, never hardcoded.
        // Use your real email and an "App Password" if you are using Gmail with 2-Step Verification.
        const val HOST = "smtp.gmail.com"
        const val PORT = "587"
        const val SENDER = "divishareapp@gmail.com"
        const val PASSWORD = "sehuziombgoaamjj" // Fixme Only for develop -----------
    }

    object ExternalApis {
        const val EXCHANGE_RATES_URL = "https://open.er-api.com/v6/latest/USD"
    }
}