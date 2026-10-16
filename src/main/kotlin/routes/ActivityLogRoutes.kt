package com.jrb.routes

import com.jrb.services.ActivityLogService
import com.jrb.utils.Constants
import com.jrb.utils.Messages
import com.jrb.utils.getAuthenticatedUserIdOrRespond
import com.jrb.utils.getLanguage
import com.jrb.utils.getPaginationParams
import com.jrb.utils.getUuidPathParameterOrRespond
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.activityLogRoutes(activityLogService: ActivityLogService) {

    // Protect all routes inside this block with JWT authentication
    authenticate(Constants.Security.AUTH_JWT_NAME) {

        // Literal route for maximum readability
        route("/activity-logs") {

            /// GET: Retrieves a paginated list of history logs for a specific group
            // Example: GET /activity-logs/123e4567...?page=2&limit=50
            get("/{groupId}") {
                val lang = call.getLanguage()

                // 1. Extract & validate IDs
                val authenticatedUserId = call.getAuthenticatedUserIdOrRespond(lang) ?: return@get
                val targetGroupId = call.getUuidPathParameterOrRespond("groupId", Constants.Messages.INVALID_GROUP_ID, lang) ?: return@get

                // 2. Extract pagination parameters
                val (page, limit) = call.getPaginationParams(defaultLimit = 30)

                // 3. Fetch the paginated logs from the Service layer
                val paginatedLogs = activityLogService.getActivityLogsForGroup(authenticatedUserId, targetGroupId, page, limit)

                // 4. Return the appropriate HTTP response
                if (paginatedLogs != null) {
                    call.respond(HttpStatusCode.OK, paginatedLogs) // Ktor automatically serializes the PaginatedResponse wrapper
                } else {
                    call.respond(HttpStatusCode.InternalServerError, Messages.get(Constants.Messages.ACTIVITY_LOG_FETCH_FAILED, lang))
                }
            }
        }
    }
}