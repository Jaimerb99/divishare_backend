package com.jrb.plugins

import com.jrb.models.GenericResponse
import com.jrb.routes.currencyRoutes
import com.jrb.routes.userRoutes
import com.jrb.services.UserService
import com.jrb.utils.Constants
import com.jrb.utils.Messages
import com.jrb.utils.getLanguage
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    val userService = UserService()

    // Error Handling
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

        // Any other unexpected server error
        exception<Throwable> { call, cause ->
            val lang = call.getLanguage()
            val errorMessage = cause.localizedMessage ?: "Unknown"

            call.respond(
                HttpStatusCode.InternalServerError,
                GenericResponse(
                    success = false,
                    message = Messages.get(Constants.Messages.ERROR_INTERNAL_SERVER, lang, errorMessage)
                )
            )
        }
    }

    // Route register
    routing {
        userRoutes(userService)
        currencyRoutes()
        // We have to add next routes groupRoutes(), expenseRoutes()...
    }
}