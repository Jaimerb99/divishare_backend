package com.jrb.utils

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import java.util.UUID

/**
 * Helper extension function to extract and validate the User ID from the JWT Token.
 * If the validation fails, it automatically responds with the correct HTTP error
 * and returns null, allowing the route handler to exit early.
 */
suspend fun ApplicationCall.getAuthenticatedUserIdOrRespond(lang: AppLanguage): UUID? {
    val principal = this.principal<JWTPrincipal>()
    val userIdString = principal?.payload?.getClaim(Constants.Security.CLAIM_USER_ID)?.asString()

    if (userIdString == null) {
        this.respond(HttpStatusCode.Unauthorized, Messages.get(Constants.Messages.UNAUTHORIZED_ACCESS, lang))
        return null
    }

    return try {
        UUID.fromString(userIdString)
    } catch (e: Exception) {
        this.respond(HttpStatusCode.BadRequest, Messages.get(Constants.Messages.INVALID_USER_ID, lang))
        null
    }
}

/**
 * Helper extension function to extract and validate a UUID path parameter from the URL.
 * If validation fails or the parameter is missing, it responds with a BadRequest
 * and the localized error message, returning null to stop execution.
 */
suspend fun ApplicationCall.getUuidPathParameterOrRespond(
    paramName: String,
    errorMessageKey: String,
    lang: AppLanguage
): UUID? {
    val paramString = this.parameters[paramName]

    if (paramString == null) {
        this.respond(HttpStatusCode.BadRequest, Messages.get(errorMessageKey, lang))
        return null
    }

    return try {
        UUID.fromString(paramString)
    } catch (e: Exception) {
        this.respond(HttpStatusCode.BadRequest, Messages.get(errorMessageKey, lang))
        null
    }
}


/**
 * Extracts 'page' and 'limit' query parameters from the URL.
 * Defaults to page 1 and limit 20. Restricts limit to a maximum of 100 for safety.
 */
fun ApplicationCall.getPaginationParams(defaultLimit: Int = 20): Pair<Int, Int> {
    val page = this.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
    val limit = this.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: defaultLimit
    return Pair(page, limit)
}