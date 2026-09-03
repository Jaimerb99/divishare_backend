package com.jrb.routes

import com.jrb.models.AuthResponse
import com.jrb.models.GenericResponse
import com.jrb.models.LoginRequest
import com.jrb.models.RegisterRequest
import com.jrb.services.UserService
import com.jrb.utils.Constants
import com.jrb.utils.Messages
import com.jrb.utils.getLanguage
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.userRoutes(userService: UserService) {
    route("/users") {

        post("/register") {
            val request = call.receive<RegisterRequest>()

            // 1. Detect the requested language from headers
            val lang = call.getLanguage()

            val isRegistered = userService.registerUser(request)

            if (isRegistered) {
                call.respond(
                    HttpStatusCode.Created,
                    GenericResponse(
                        success = true,
                        // 2. Fetch the translated success message using the Constant
                        message = Messages.get(Constants.Messages.REGISTER_SUCCESS, lang, request.name)
                    )
                )
            } else {
                call.respond(
                    HttpStatusCode.Conflict,
                    GenericResponse(
                        success = false,
                        message = Messages.get(Constants.Messages.REGISTER_CONFLICT, lang)
                    )
                )
            }
        }

        post("/login") {
            val request = call.receive<LoginRequest>()
            val lang = call.getLanguage()

            val isValidUser = userService.loginUser(request)

            if (isValidUser) {
                // TODO: Generate JWT token here in the next step
                call.respond(
                    HttpStatusCode.OK,
                    AuthResponse(
                        success = true,
                        message = Messages.get(Constants.Messages.LOGIN_SUCCESS, lang)
                    )
                )
            } else {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    AuthResponse(
                        success = false,
                        message = Messages.get(Constants.Messages.LOGIN_ERROR, lang)
                    )
                )
            }
        }
    }
}