package com.jrb.support

import com.jrb.models.AuthResponse
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder

data class Session(val userId: String, val token: String, val bearer: String = "Bearer $token")

// Registers a user through the real API and logs in to obtain a valid JWT
suspend fun ApplicationTestBuilder.registerAndLogin(
    email: String = "user@test.com",
    password: String = "Passw0rd!",
    name: String = "Test User",
    deviceId: String = "device-1",
): Session {
    val client = jsonClient()

    client.post("/users/register") {
        contentType(ContentType.Application.Json)
        // Adjust the fields to match your RegisterRequest
        setBody("""{"name":"$name","email":"$email","password":"$password"}""")
    }

    val auth = client.post("/users/login") {
        contentType(ContentType.Application.Json)
        // Adjust the fields to match your LoginRequest
        setBody("""{"email":"$email","password":"$password","deviceId":"$deviceId"}""")
    }.body<AuthResponse>()

    return Session(userId = auth.userId.toString(), token = auth.token!!)
}