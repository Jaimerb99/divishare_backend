package com.jrb.routes

import com.jrb.models.GenericResponse
import com.jrb.models.RegisterRequest
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.userRoutes() {
    route("/users") {

        post("/register") {
            // Reiciving the object and converting to Json
            val request = call.receive<RegisterRequest>()

            // Here we'll call the database

            call.respond(
                HttpStatusCode.Created,
                GenericResponse(success = true, message = "Usuario ${request.name} registrado con éxito")
            )
        }

        get("/"){
            call.respond(
                HttpStatusCode.OK,
                "Prueba"
            )
        }

    }
}