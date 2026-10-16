package com.jrb.routes

import com.jrb.models.CreateExpenseRequest
import com.jrb.models.UpdateExpenseRequest
import com.jrb.services.ExpenseService
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

fun Route.expenseRoutes(expenseService: ExpenseService) {

    authenticate(Constants.Security.AUTH_JWT_NAME) {

        route("/expenses") {

            post {
                val lang = call.getLanguage()
                val authenticatedUserId = call.getAuthenticatedUserIdOrRespond(lang) ?: return@post
                val request = try {
                    call.receive<CreateExpenseRequest>()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, Messages.get(Constants.Messages.INVALID_REQUEST_BODY, lang))
                    return@post
                }
                val createdExpense = expenseService.createExpense(authenticatedUserId, request)
                if (createdExpense != null) {
                    call.respond(HttpStatusCode.Created, createdExpense)
                } else {
                    call.respond(HttpStatusCode.InternalServerError, Messages.get(Constants.Messages.EXPENSE_CREATION_FAILED, lang))
                }
            }

            // GET: Retrieves a paginated list of expenses for a specific group
            get("/{groupId}") {
                val lang = call.getLanguage()

                // 1. Extract IDs
                val authenticatedUserId = call.getAuthenticatedUserIdOrRespond(lang) ?: return@get
                val targetGroupId = call.getUuidPathParameterOrRespond("groupId", Constants.Messages.INVALID_GROUP_ID, lang) ?: return@get

                // 2. Extract pagination params (e.g., ?page=1&limit=50)
                val (page, limit) = call.getPaginationParams(defaultLimit = 50)

                // 3. Fetch paginated data
                val includeDeleted = call.request.queryParameters["includeDeleted"]?.toBoolean() ?: false
                val since = call.request.queryParameters["since"]?.toLongOrNull() ?: 0L

                val paginatedExpenses = expenseService.getExpensesForGroup(
                    authenticatedUserId,
                    targetGroupId,
                    page,
                    limit,
                    includeDeleted,
                    since
                )

                if (paginatedExpenses != null) {
                    call.respond(HttpStatusCode.OK, paginatedExpenses)
                } else {
                    call.respond(HttpStatusCode.InternalServerError, Messages.get(Constants.Messages.EXPENSE_FETCH_FAILED, lang))
                }
            }

            // PUT: Updates an existing expense and completely replaces its splits
            put("/{expenseId}") {
                val lang = call.getLanguage()
                val authenticatedUserId = call.getAuthenticatedUserIdOrRespond(lang) ?: return@put
                val targetExpenseId = call.getUuidPathParameterOrRespond("expenseId", Constants.Messages.ERROR_BAD_REQUEST, lang) ?: return@put

                val request = try {
                    call.receive<UpdateExpenseRequest>()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, Messages.get(Constants.Messages.INVALID_REQUEST_BODY, lang))
                    return@put
                }

                val updatedExpense = expenseService.updateExpense(authenticatedUserId, targetExpenseId, request)

                if (updatedExpense != null) {
                    call.respond(HttpStatusCode.OK, updatedExpense)
                } else {
                    call.respond(HttpStatusCode.InternalServerError, Messages.get(Constants.Messages.EXPENSE_UPDATE_FAILED, lang))
                }
            }

            // DELETE: Soft deletes an expense and all its splits
            delete("/{expenseId}") {
                val lang = call.getLanguage()
                val authenticatedUserId = call.getAuthenticatedUserIdOrRespond(lang) ?: return@delete
                val targetExpenseId = call.getUuidPathParameterOrRespond("expenseId", Constants.Messages.ERROR_BAD_REQUEST, lang) ?: return@delete

                val success = expenseService.deleteExpense(authenticatedUserId, targetExpenseId)

                if (success) {
                    call.respond(HttpStatusCode.NoContent) // HTTP 204: Success, no body
                } else {
                    call.respond(HttpStatusCode.NotFound, Messages.get(Constants.Messages.EXPENSE_NOT_FOUND, lang))
                }
            }
        }
    }
}