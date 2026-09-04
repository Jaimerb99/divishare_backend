package com.jrb.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.jrb.models.GenericResponse
import com.jrb.utils.Constants
import com.jrb.utils.Messages
import com.jrb.utils.getLanguage
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*

fun Application.configureSecurity() {
    authentication {
        // IMPORTANT: We set the configuration name using our constant so the routes know which auth scheme to use
        jwt(Constants.Security.AUTH_JWT_NAME) {

            // Replace default generated literals with our Constants
            realm = Constants.Security.JWT_AUDIENCE

            verifier(
                JWT
                    .require(Algorithm.HMAC256(Constants.Security.JWT_SECRET))
                    .withAudience(Constants.Security.JWT_AUDIENCE)
                    .withIssuer(Constants.Security.JWT_ISSUER)
                    .build()
            )

            // Keep original audience validation but also ensure the email claim exists and is not empty
            validate { credential ->
                if (credential.payload.audience.contains(Constants.Security.JWT_AUDIENCE) &&
                    credential.payload.getClaim(Constants.Security.CLAIM_EMAIL).asString().isNotEmpty()
                ) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }

            // Add the challenge block to prevent Ktor from returning a default plain-text error,
            // responding instead with a structured and localized JSON response
            challenge { defaultScheme, realm ->
                val lang = call.getLanguage()
                call.respond(
                    HttpStatusCode.Unauthorized,
                    GenericResponse(
                        success = false,
                        message = Messages.get(Constants.Messages.UNAUTHORIZED_ACCESS, lang)
                    )
                )
            }
        }
    }
}