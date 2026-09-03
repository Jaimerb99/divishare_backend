package com.jrb.routes

import com.jrb.models.AuthResponse
import com.jrb.models.GenericResponse
import com.jrb.models.LoginRequest
import com.jrb.models.RefreshTokenRequest
import com.jrb.models.RegisterRequest
import com.jrb.services.UserService
import com.jrb.utils.Constants
import com.jrb.utils.Messages
import com.jrb.utils.TokenManager
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
                // 1. Generate both tokens
                val accessToken = TokenManager.generateToken(request.email)
                val refreshToken = TokenManager.generateRefreshToken()

                // 2. Save the refresh token to the database
                userService.saveRefreshToken(request.email, refreshToken)

                call.respond(
                    HttpStatusCode.OK,
                    AuthResponse(
                        success = true,
                        message = Messages.get(Constants.Messages.LOGIN_SUCCESS, lang),
                        token = accessToken,
                        refreshToken = refreshToken
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

        post("/refresh") {
            val request = call.receive<RefreshTokenRequest>()
            val lang = call.getLanguage()

            // 1. Verify if the token matches the database
            val isValid = userService.validateRefreshToken(request.email, request.refreshToken)

            if (isValid) {
                // 2. Generate a fresh pair of tokens (Refresh Token Rotation)
                val newAccessToken = TokenManager.generateToken(request.email)
                val newRefreshToken = TokenManager.generateRefreshToken()

                userService.saveRefreshToken(request.email, newRefreshToken)

                call.respond(
                    HttpStatusCode.OK,
                    AuthResponse(
                        success = true,
                        message = Messages.get(Constants.Messages.REFRESH_SUCCESS, lang),
                        token = newAccessToken,
                        refreshToken = newRefreshToken
                    )
                )
            } else {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    AuthResponse(
                        success = false,
                        message = Messages.get(Constants.Messages.REFRESH_ERROR, lang)
                    )
                )
            }
        }
    }
}