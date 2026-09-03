package com.jrb.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.security.SecureRandom
import java.util.Base64
import java.util.Date

object TokenManager {

    // Generates a short-lived JWT token (Access Token)
    fun generateToken(email: String): String {
        return JWT.create()
            .withAudience(Constants.Security.JWT_AUDIENCE)
            .withIssuer(Constants.Security.JWT_ISSUER)
            .withClaim("email", email)
            .withExpiresAt(Date(System.currentTimeMillis() + Constants.Security.JWT_EXPIRATION_MS))
            .sign(Algorithm.HMAC256(Constants.Security.JWT_SECRET))
    }

    // Generates a long-lived secure random string (Refresh Token)
    fun generateRefreshToken(): String {
        val random = ByteArray(32)
        SecureRandom().nextBytes(random)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(random)
    }
}