package com.jrb.routes

import com.jrb.models.AuthResponse
import com.jrb.models.GenericResponse
import com.jrb.models.LoginRequest
import com.jrb.models.LoginResult
import com.jrb.models.RefreshTokenRequest
import com.jrb.models.RegisterRequest
import com.jrb.models.RecoverPasswordRequest
import com.jrb.models.VerifyPinRequest
import com.jrb.models.ResetPasswordRequest
import com.jrb.services.UserService
import com.jrb.utils.Constants
import com.jrb.utils.Messages
import com.jrb.utils.TokenManager
import com.jrb.utils.getLanguage
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.userRoutes(userService: UserService) {
    route("/users") {

        post("/register") {
            val request = call.receive<RegisterRequest>()
            val lang = call.getLanguage()
            val isRegistered = userService.registerUser(request)

            if (isRegistered) {
                call.respond(
                    HttpStatusCode.Created,
                    GenericResponse(
                        success = true,
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

            when (val result = userService.loginUser(request)) {
                is LoginResult.Success -> {
                    val accessToken = TokenManager.generateToken(request.email)
                    val refreshToken = TokenManager.generateRefreshToken()

                    userService.saveRefreshToken(request.email, request.deviceId, refreshToken)

                    call.respond(
                        HttpStatusCode.OK,
                        AuthResponse(
                            success = true,
                            message = Messages.get(Constants.Messages.LOGIN_SUCCESS, lang),
                            userId = result.userId,
                            token = accessToken,
                            refreshToken = refreshToken
                        )
                    )
                }
                LoginResult.DeactivatedReactivateNeeded -> {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        AuthResponse(
                            success = false,
                            message = Messages.get(Constants.Messages.LOGIN_ERROR_REACTIVATE, lang)
                        )
                    )
                }
                LoginResult.PermanentlyDeleted -> {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        AuthResponse(
                            success = false,
                            message = Messages.get(Constants.Messages.LOGIN_ERROR_PERMANENTLY_DELETED, lang)
                        )
                    )
                }
                LoginResult.InvalidCredentials -> {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        AuthResponse(
                            success = false,
                            message = Messages.get(Constants.Messages.LOGIN_ERROR, lang)
                        )
                    )
                }
                LoginResult.WrongAuthProvider -> {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        AuthResponse(
                            success = false,
                            // Inform the user to use their original authentication provider (e.g., Google)
                            message = Messages.get(Constants.Messages.LOGIN_ERROR_WRONG_PROVIDER, lang)
                        )
                    )
                }
            }
        }

        post("/refresh") {
            val request = call.receive<RefreshTokenRequest>()
            val lang = call.getLanguage()
            val isValid = userService.validateRefreshToken(request.email, request.deviceId, request.refreshToken)

            if (isValid) {
                val newAccessToken = TokenManager.generateToken(request.email)
                val newRefreshToken = TokenManager.generateRefreshToken()

                userService.saveRefreshToken(request.email, request.deviceId, newRefreshToken)

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

        // 1. Request Recovery (Generates PIN and sends email)
        post("/recover-password") {
            val request = call.receive<RecoverPasswordRequest>()
            val lang = call.getLanguage()

            val isValidForRecovery = userService.recoverPassword(request.email, lang)

            if (isValidForRecovery) {
                call.respond(
                    HttpStatusCode.OK,
                    GenericResponse(
                        success = true,
                        message = Messages.get(Constants.Messages.RECOVERY_EMAIL_SENT, lang)
                    )
                )
            } else {
                call.respond(
                    HttpStatusCode.BadRequest,
                    GenericResponse(
                        success = false,
                        message = Messages.get(Constants.Messages.RECOVERY_INVALID, lang)
                    )
                )
            }
        }

        // 2. Verify PIN (Client app calls this to validate the PIN before showing the new password screen)
        post("/verify-pin") {
            val request = call.receive<VerifyPinRequest>()
            val lang = call.getLanguage()

            val isPinValid = userService.verifyPin(request.email, request.pin)

            if (isPinValid) {
                call.respond(
                    HttpStatusCode.OK,
                    GenericResponse(
                        success = true,
                        message = Messages.get(Constants.Messages.PIN_VALID, lang)
                    )
                )
            } else {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    GenericResponse(
                        success = false,
                        message = Messages.get(Constants.Messages.PIN_INVALID, lang)
                    )
                )
            }
        }

        // 3. Reset Password (Saves the new encrypted password and clears the PIN from DB)
        post("/reset-password") {
            val request = call.receive<ResetPasswordRequest>()
            val lang = call.getLanguage()

            val isResetSuccessful = userService.resetPassword(request)

            if (isResetSuccessful) {
                call.respond(
                    HttpStatusCode.OK,
                    GenericResponse(
                        success = true,
                        message = Messages.get(Constants.Messages.PASSWORD_RESET_SUCCESS, lang)
                    )
                )
            } else {
                call.respond(
                    HttpStatusCode.BadRequest,
                    GenericResponse(
                        success = false,
                        message = Messages.get(Constants.Messages.PASSWORD_RESET_ERROR, lang)
                    )
                )
            }
        }

        // PROTECTED ROUTE: Only users with a valid JWT token
        authenticate(Constants.Security.AUTH_JWT_NAME) {
            delete("/") {
                val lang = call.getLanguage()

                // Extract the email directly from the verified JWT Token using constants
                val principal = call.principal<JWTPrincipal>()
                val userEmail = principal?.payload?.getClaim(Constants.Security.CLAIM_EMAIL)?.asString()

                if (userEmail != null) {
                    val isDeleted = userService.deleteUser(userEmail)

                    if (isDeleted) {
                        call.respond(
                            HttpStatusCode.OK,
                            GenericResponse(
                                success = true,
                                message = Messages.get(Constants.Messages.DELETE_SUCCESS, lang)
                            )
                        )
                        return@delete
                    }
                }

                call.respond(
                    HttpStatusCode.InternalServerError,
                    GenericResponse(
                        success = false,
                        message = Messages.get(Constants.Messages.DELETE_ERROR, lang)
                    )
                )
            }
        }
    }
}