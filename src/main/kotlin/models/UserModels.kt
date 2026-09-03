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
    val password: String
)

// Response sent by the server upon successful login (will include JWT token later)
@Serializable
data class AuthResponse(
    val success: Boolean,
    val message: String,
    val token: String? = null,
    val refreshToken: String? = null
)

// Request sent by the client to get a new JWT
@Serializable
data class RefreshTokenRequest(
    val email: String,
    val refreshToken: String
)