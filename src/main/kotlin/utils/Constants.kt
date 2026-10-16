package com.jrb.utils

object Constants {

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
        const val INVALID_USER_ID = "invalid_user_id"
        const val INVALID_REQUEST_BODY = "invalid_request_body"
        const val GROUP_CREATION_FAILED = "group_creation_failed"
        const val GROUP_FETCH_FAILED = "group_fetch_failed"
        const val EXPENSE_CREATION_FAILED = "expense_creation_failed"
        const val INVALID_GROUP_ID = "invalid_group_id"
        const val EXPENSE_FETCH_FAILED = "expense_fetch_failed"
        const val SETTLEMENT_CREATION_FAILED = "settlement_creation_failed"
        const val SETTLEMENT_FETCH_FAILED = "settlement_fetch_failed"
        const val ACTIVITY_LOG_FETCH_FAILED = "activity_log_fetch_failed"
        const val MEMBER_ADD_FAILED = "member_add_failed"
        const val MEMBER_REMOVE_FAILED = "member_remove_failed"
        const val EXPENSE_NOT_FOUND = "expense_not_found"
        const val EXPENSE_UPDATE_FAILED = "expense_update_failed"
        const val EXPENSE_DELETE_FAILED = "expense_delete_failed"

        const val GROUP_ALREADY_EXISTS = "group_already_exists"
        const val EXPENSE_ALREADY_EXISTS = "expense_already_exists"
        const val SETTLEMENT_ALREADY_EXISTS = "settlement_already_exists"
    }

    object Localization {
        const val DEFAULT_LANG_CODE = "en"
        const val SPANISH_LANG_PREFIX = "es"
        const val BUNDLE_BASE_NAME = "messages"
    }

    object User {
        const val GRACE_PERIOD_DAYS = 30
        const val DELETED_NAME = "Deleted User"
        const val DELETED_EMAIL_DOMAIN = "@deleted.divishare.local"
        const val DELETED_EMAIL_PREFIX = "deleted_"
    }

    object Security {
        const val JWT_SECRET = "divishare-super-secret-key-2026"
        const val JWT_ISSUER = "http://localhost:8080/"
        const val JWT_AUDIENCE = "divishare-users"
        const val JWT_EXPIRATION_MS = 900_000L // 15 minutes
        const val AUTH_JWT_NAME = "auth-jwt"
        const val CLAIM_EMAIL = "email"
        const val PIN_MIN = 100000
        const val PIN_MAX = 999999
        const val CLAIM_USER_ID = "userId"
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
        const val GROUP_SERVICE_TAG = "GroupService"
        const val EXPENSE_SERVICE_TAG = "ExpenseService"
        const val SETTLEMENT_SERVICE_TAG = "SettlementService"
        const val ACTIVITY_LOG_SERVICE_TAG = "ActivityLogService"

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

            const val GROUP_CREATE_SUCCESS = "Group created successfully with ID: %s"
            const val GROUP_CREATE_ERROR = "Error creating group with ID: %s"
            const val GROUP_FETCH_ERROR = "Error fetching groups for user ID: %s"

            const val EXPENSE_CREATE_SUCCESS = "Expense created successfully with ID: %s"
            const val EXPENSE_CREATE_ERROR = "Error creating expense with ID: %s"
            const val EXPENSE_FETCH_ERROR = "Error fetching expenses for group ID: %s"
            const val EXPENSE_UPDATE_SUCCESS = "Expense updated successfully: %s"
            const val EXPENSE_UPDATE_ERROR = "Error updating expense: %s"
            const val EXPENSE_DELETE_SUCCESS = "Expense soft-deleted successfully: %s"
            const val EXPENSE_DELETE_ERROR = "Error deleting expense: %s"
            const val EXPENSE_NOT_FOUND_LOG = "Expense not found: %s"

            const val UNAUTHORIZED_GROUP_ACCESS = "Security: User %s attempted to access group %s without being a member"

            const val SETTLEMENT_CREATE_SUCCESS = "Settlement created successfully with ID: %s"
            const val SETTLEMENT_CREATE_ERROR = "Error creating settlement with ID: %s"
            const val SETTLEMENT_FETCH_ERROR = "Error fetching settlements for group ID: %s"
            const val SETTLEMENT_UPDATE_SUCCESS = "Settlement updated successfully: %s"
            const val SETTLEMENT_UPDATE_ERROR = "Error updating settlement: %s"
            const val SETTLEMENT_DELETE_SUCCESS = "Settlement soft-deleted successfully: %s"
            const val SETTLEMENT_DELETE_ERROR = "Error deleting settlement: %s"
            const val SETTLEMENT_NOT_FOUND_LOG = "Settlement not found: %s"

            const val ACTIVITY_LOG_FETCH_ERROR = "Error fetching activity logs for group ID: %s"

            const val MEMBER_ADD_SUCCESS = "Members added successfully to group: %s"
            const val MEMBER_ADD_ERROR = "Error adding members to group: %s"
            const val MEMBER_REMOVE_SUCCESS = "Member %s removed from group: %s"
            const val MEMBER_REMOVE_ERROR = "Error removing member %s from group: %s"
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

    object Audit {
        const val ENTITY_GROUP = "GROUP"
        const val ENTITY_MEMBER = "MEMBER"
        const val ENTITY_EXPENSE = "EXPENSE"
        const val ENTITY_SETTLEMENT = "SETTLEMENT"

        const val ACTION_CREATE = "CREATE"
        const val ACTION_UPDATE = "UPDATE"
        const val ACTION_DELETE = "DELETE"
        const val ACTION_JOIN = "JOIN"
        const val ACTION_LEAVE = "LEAVE"
    }

    object Exceptions {
        const val UNAUTHORIZED_GROUP_ACCESS = "Unauthorized group access"
    }
}