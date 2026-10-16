package com.jrb.routes

import com.jrb.models.CreateSettlementRequest
import com.jrb.models.UpdateSettlementRequest
import com.jrb.services.SettlementService
import com.jrb.utils.Constants
import com.jrb.utils.Messages
import com.jrb.utils.getAuthenticatedUserIdOrRespond
import com.jrb.utils.getLanguage
import com.jrb.utils.getPaginationParams
import com.jrb.utils.getUuidPathParameterOrRespond
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.settlementRoutes(settlementService: SettlementService) {

    authenticate(Constants.Security.AUTH_JWT_NAME) {

        route("/settlements") {

            post {
                val lang = call.getLanguage()
                val authenticatedUserId = call.getAuthenticatedUserIdOrRespond(lang) ?: return@post
                val request = try {
                    call.receive<CreateSettlementRequest>()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, Messages.get(Constants.Messages.INVALID_REQUEST_BODY, lang))
                    return@post
                }
                val createdSettlement = settlementService.createSettlement(authenticatedUserId, request)
                if (createdSettlement != null) {
                    call.respond(HttpStatusCode.Created, createdSettlement)
                } else {
                    call.respond(HttpStatusCode.InternalServerError, Messages.get(Constants.Messages.SETTLEMENT_CREATION_FAILED, lang))
                }
            }

            // GET: Retrieves a paginated list of settlements for a specific group
            get("/{groupId}") {
                val lang = call.getLanguage()

                // 1. Extract IDs
                val authenticatedUserId = call.getAuthenticatedUserIdOrRespond(lang) ?: return@get
                val targetGroupId = call.getUuidPathParameterOrRespond("groupId", Constants.Messages.INVALID_GROUP_ID, lang) ?: return@get

                // 2. Extract pagination params (e.g., ?page=1&limit=20)
                val (page, limit) = call.getPaginationParams(defaultLimit = 20)

                // 3. Fetch paginated data
                val paginatedSettlements = settlementService.getSettlementsForGroup(authenticatedUserId, targetGroupId, page, limit)

                if (paginatedSettlements != null) {
                    call.respond(HttpStatusCode.OK, paginatedSettlements)
                } else {
                    call.respond(HttpStatusCode.InternalServerError, Messages.get(Constants.Messages.SETTLEMENT_FETCH_FAILED, lang))
                }
            }

            // PUT: Updates an existing settlement
            put("/{settlementId}") {
                val lang = call.getLanguage()
                val authenticatedUserId = call.getAuthenticatedUserIdOrRespond(lang) ?: return@put
                val targetSettlementId = call.getUuidPathParameterOrRespond("settlementId", Constants.Messages.ERROR_BAD_REQUEST, lang) ?: return@put

                val request = try {
                    call.receive<UpdateSettlementRequest>()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, Messages.get(Constants.Messages.INVALID_REQUEST_BODY, lang))
                    return@put
                }

                val updatedSettlement = settlementService.updateSettlement(authenticatedUserId, targetSettlementId, request)

                if (updatedSettlement != null) {
                    call.respond(HttpStatusCode.OK, updatedSettlement)
                } else {
                    call.respond(HttpStatusCode.InternalServerError, Messages.get(Constants.Messages.ERROR_INTERNAL_SERVER, lang))
                }
            }

            // DELETE: Soft deletes a settlement
            delete("/{settlementId}") {
                val lang = call.getLanguage()
                val authenticatedUserId = call.getAuthenticatedUserIdOrRespond(lang) ?: return@delete
                val targetSettlementId = call.getUuidPathParameterOrRespond("settlementId", Constants.Messages.ERROR_BAD_REQUEST, lang) ?: return@delete

                val success = settlementService.deleteSettlement(authenticatedUserId, targetSettlementId)

                if (success) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound, Messages.get(Constants.Messages.ERROR_BAD_REQUEST, lang))
                }
            }
        }
    }
}