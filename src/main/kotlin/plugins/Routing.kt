package com.jrb.plugins

import com.jrb.models.GenericResponse
import com.jrb.routes.userRoutes
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {

    // Error Handling
    install(StatusPages) {

        exception<io.ktor.server.plugins.BadRequestException> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest,
                GenericResponse(success = false, message = "Formato de datos incorrecto")
            )
        }

        // Cualquier otro error inesperado en el servidor
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                GenericResponse(success = false, message = "Error interno: ${cause.localizedMessage}")
            )
        }
    }

    // Route register
    routing {
        userRoutes()
        // We have to add next rotes groupRoutes(), expenseRoutes()...
    }
}