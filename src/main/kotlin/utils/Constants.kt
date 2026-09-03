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
    }

    // Configuration for localization and language defaults
    object Localization {
        const val DEFAULT_LANG_CODE = "en"
        const val SPANISH_LANG_PREFIX = "es"
        const val BUNDLE_BASE_NAME = "messages"
    }

    // You can add more categories in the future
    object Security {
        // const val JWT_SECRET = "..."
    }
}