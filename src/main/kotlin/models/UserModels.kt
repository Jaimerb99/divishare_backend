package com.jrb.models

import kotlinx.serialization.Serializable

// What client sends
@Serializable
data class RegisterRequest(
    val email: String,
    val name: String,
    val password: String
)

// Server response
@Serializable
data class GenericResponse(
    val success: Boolean,
    val message: String
)

// Request sent by the client to log in
@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val deviceId: String,
)

// Response sent by the server upon successful login (will include JWT token later)
@Serializable
data class AuthResponse(
    val success: Boolean,
    val message: String,
    val userId: String? = null,
    val token: String? = null,
    val refreshToken: String? = null
)

// Request sent by the client to get a new JWT
@Serializable
data class RefreshTokenRequest(
    val email: String,
    val refreshToken: String,
    val deviceId: String
)

@Serializable
data class RecoverPasswordRequest(
    val email: String
)

@Serializable
data class VerifyPinRequest(
    val email: String,
    val pin: String
)

@Serializable
data class ResetPasswordRequest(
    val email: String,
    val pin: String,
    val newPassword: String
)

sealed class LoginResult {
    data class Success(val userId: String) : LoginResult()
    object InvalidCredentials : LoginResult()
    object DeactivatedReactivateNeeded : LoginResult()
    object PermanentlyDeleted : LoginResult()
    object WrongAuthProvider : LoginResult()
}