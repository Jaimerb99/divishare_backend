package com.jrb.plugins

import com.jrb.models.GenericResponse
import com.jrb.routes.activityLogRoutes
import com.jrb.routes.currencyRoutes
import com.jrb.routes.expenseRoutes
import com.jrb.routes.groupRoutes
import com.jrb.routes.settlementRoutes
import com.jrb.routes.userRoutes
import com.jrb.services.ActivityLogService
import com.jrb.services.ExpenseService
import com.jrb.services.GroupService
import com.jrb.services.SettlementService
import com.jrb.services.UserService
import com.jrb.utils.Constants
import com.jrb.utils.DuplicateEntityException
import com.jrb.utils.Messages
import com.jrb.utils.getLanguage
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    val userService = UserService()
    val expenseService = ExpenseService()
    val groupService = GroupService()
    val settlementService = SettlementService()
    val activityLogService = ActivityLogService()

    install(StatusPages) {

        exception<io.ktor.server.plugins.BadRequestException> { call, _ ->
            val lang = call.getLanguage()
            call.respond(
                HttpStatusCode.BadRequest,
                GenericResponse(
                    success = false,
                    message = Messages.get(Constants.Messages.ERROR_BAD_REQUEST, lang)
                )
            )
        }

        exception<DuplicateEntityException> { call, cause ->
            val lang = call.getLanguage()
            call.respond(
                HttpStatusCode.Conflict,
                GenericResponse(
                    success = false,
                    message = Messages.get(cause.errorCode.name, lang)
                )
            )
        }

        exception<Throwable> { call, cause ->
            val lang = call.getLanguage()
            val errorMessage = cause.localizedMessage ?: ""

            call.respond(
                HttpStatusCode.InternalServerError,
                GenericResponse(
                    success = false,
                    message = Messages.get(Constants.Messages.ERROR_INTERNAL_SERVER, lang, errorMessage)
                )
            )
        }
    }

    routing {
        userRoutes(userService)
        currencyRoutes()
        expenseRoutes(expenseService)
        groupRoutes(groupService)
        settlementRoutes(settlementService)
        activityLogRoutes(activityLogService)
    }
}