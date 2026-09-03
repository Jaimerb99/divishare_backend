package com.jrb.models

import kotlinx.serialization.Serializable

// Lo que el cliente envía
@Serializable
data class RegisterRequest(
    val email: String,
    val name: String,
    val password: String
)

// Lo que el servidor responde siempre (éxito o error)
@Serializable
data class GenericResponse(
    val success: Boolean,
    val message: String
)