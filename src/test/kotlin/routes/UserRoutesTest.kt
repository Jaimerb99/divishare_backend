package com.jrb.routes

import com.jrb.support.Fixtures
import com.jrb.support.TestDb
import com.jrb.support.api
import com.jrb.support.assertRejected
import com.jrb.support.field
import com.jrb.support.registerAndLogin
import com.jrb.support.runTestApp
import com.jrb.support.toJson
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UserRoutesTest {

    private val password = "Passw0rd!"

    private suspend fun ApplicationTestBuilder.register(email: String, pw: String = password): HttpResponse =
        api(HttpMethod.Post, "/users/register", body = Fixtures.registerBody("Test User", email, pw))

    private suspend fun ApplicationTestBuilder.login(
        email: String,
        pw: String = password,
        deviceId: String = "device-1",
        lang: String? = null,
    ): HttpResponse =
        api(HttpMethod.Post, "/users/login", body = Fixtures.loginBody(email, pw, deviceId), lang = lang)

    private suspend fun ApplicationTestBuilder.refresh(email: String, deviceId: String, token: String): HttpResponse =
        api(HttpMethod.Post, "/users/refresh", body = Fixtures.refreshBody(email, deviceId, token))

    // ------------------------------------------------------------------ register

    @Test
    fun `register creates a user and returns 201`() = runTestApp {
        TestDb.clean()
        assertEquals(HttpStatusCode.Created, register("ana@test.com").status)
    }

    @Test
    fun `register with an existing email returns 409`() = runTestApp {
        register("dup@test.com")
        assertEquals(HttpStatusCode.Conflict, register("dup@test.com").status)
    }

    @Test
    fun `register does not overwrite the password of an existing account`() = runTestApp {
        register("keep@test.com", pw = password)
        register("keep@test.com", pw = "AnotherPassw0rd!")

        assertEquals(HttpStatusCode.OK, login("keep@test.com", pw = password).status)
        assertEquals(HttpStatusCode.Unauthorized, login("keep@test.com", pw = "AnotherPassw0rd!").status)
    }

    @Test
    fun `register with malformed JSON returns 400`() = runTestApp {
        val res = api(HttpMethod.Post, "/users/register", body = "{ this is not json")
        assertEquals(HttpStatusCode.BadRequest, res.status)
    }

    @Test
    fun `register with an empty body returns 400`() = runTestApp {
        val res = api(HttpMethod.Post, "/users/register", body = "")
        assertEquals(HttpStatusCode.BadRequest, res.status)
    }

    @Test
    fun `register with a missing field returns 400`() = runTestApp {
        val res = api(HttpMethod.Post, "/users/register", body = """{"email":"nopass@test.com"}""")
        assertEquals(HttpStatusCode.BadRequest, res.status)
    }

    @Test
    @Ignore("Desired behaviour, not implemented yet: emails should be compared case-insensitively")
    fun `register with the same email in a different case returns 409`() = runTestApp {
        register("case@test.com")
        assertEquals(HttpStatusCode.Conflict, register("CASE@test.com").status)
    }

    @Test
    @Ignore("Desired behaviour, not implemented yet: invalid emails and weak passwords should return 400")
    fun `register with an invalid email or a weak password returns 400`() = runTestApp {
        assertEquals(HttpStatusCode.BadRequest, register("not-an-email").status)
        assertEquals(HttpStatusCode.BadRequest, register("weak@test.com", pw = "1").status)
    }

    // --------------------------------------------------------------------- login

    @Test
    fun `login returns a user id, an access token and a refresh token`() = runTestApp {
        register("login@test.com")

        val res = login("login@test.com")
        val json = res.bodyAsText().toJson()

        assertEquals(HttpStatusCode.OK, res.status)
        assertNotNull(json.field("token"))
        assertNotNull(json.field("refreshToken"))
        assertNotNull(json.field("userId"))
    }

    @Test
    fun `login with a wrong password returns 401`() = runTestApp {
        register("wrongpw@test.com")
        assertEquals(HttpStatusCode.Unauthorized, login("wrongpw@test.com", pw = "wrong").status)
    }

    @Test
    fun `login with an unknown email returns 401`() = runTestApp {
        assertEquals(HttpStatusCode.Unauthorized, login("ghost@test.com").status)
    }

    @Test
    fun `login does not reveal whether the email exists`() = runTestApp {
        register("known@test.com")

        val wrongPassword = login("known@test.com", pw = "wrong").bodyAsText().toJson()
        val unknownEmail = login("unknown@test.com").bodyAsText().toJson()

        assertEquals(wrongPassword.field("message"), unknownEmail.field("message"))
    }

    @Test
    fun `login with a missing field returns 400`() = runTestApp {
        val res = api(HttpMethod.Post, "/users/login", body = """{"email":"x@test.com"}""")
        assertEquals(HttpStatusCode.BadRequest, res.status)
    }

    @Test
    fun `login with malformed JSON returns 400`() = runTestApp {
        assertEquals(HttpStatusCode.BadRequest, api(HttpMethod.Post, "/users/login", body = "nope").status)
    }

    @Test
    fun `error messages follow the Accept-Language header`() = runTestApp {
        // ASSUMPTION: Messages has both an English and a Spanish text for the login error
        register("i18n@test.com")

        val en = login("i18n@test.com", pw = "wrong", lang = "en").bodyAsText().toJson().field("message")
        val es = login("i18n@test.com", pw = "wrong", lang = "es").bodyAsText().toJson().field("message")

        assertNotEquals(en, es)
    }

    // ------------------------------------------------------------------- refresh

    @Test
    fun `refresh returns a new access token and a new refresh token`() = runTestApp {
        register("refresh@test.com")
        val first = login("refresh@test.com").bodyAsText().toJson()

        val res = refresh("refresh@test.com", "device-1", first.field("refreshToken")!!)
        val second = res.bodyAsText().toJson()

        assertEquals(HttpStatusCode.OK, res.status)
        assertNotEquals(first.field("refreshToken"), second.field("refreshToken"))
        assertNotNull(second.field("token"))
    }

    @Test
    fun `the new access token obtained by refreshing is accepted`() = runTestApp {
        register("newtoken@test.com")
        val first = login("newtoken@test.com").bodyAsText().toJson()
        val second = refresh("newtoken@test.com", "device-1", first.field("refreshToken")!!).bodyAsText().toJson()

        val res = api(HttpMethod.Get, "/groups") {
            headers.append("Authorization", "Bearer ${second.field("token")}")
        }

        assertEquals(HttpStatusCode.OK, res.status)
    }

    @Test
    fun `a refresh token can only be used once`() = runTestApp {
        register("rotate@test.com")
        val old = login("rotate@test.com").bodyAsText().toJson().field("refreshToken")!!

        assertEquals(HttpStatusCode.OK, refresh("rotate@test.com", "device-1", old).status)
        assertEquals(HttpStatusCode.Unauthorized, refresh("rotate@test.com", "device-1", old).status)
    }

    @Test
    fun `refresh with a wrong device id returns 401`() = runTestApp {
        register("device@test.com")
        val token = login("device@test.com", deviceId = "device-1").bodyAsText().toJson().field("refreshToken")!!

        assertEquals(HttpStatusCode.Unauthorized, refresh("device@test.com", "another-device", token).status)
    }

    @Test
    fun `refresh with an unknown email or a garbage token returns 401`() = runTestApp {
        register("garbage@test.com")
        login("garbage@test.com")

        assertEquals(HttpStatusCode.Unauthorized, refresh("ghost@test.com", "device-1", "abc").status)
        assertEquals(HttpStatusCode.Unauthorized, refresh("garbage@test.com", "device-1", "abc").status)
    }

    @Test
    fun `a refresh token of one user cannot be used for another user`() = runTestApp {
        register("victim@test.com")
        register("attacker@test.com")
        val victimToken = login("victim@test.com").bodyAsText().toJson().field("refreshToken")!!
        login("attacker@test.com")

        assertEquals(HttpStatusCode.Unauthorized, refresh("attacker@test.com", "device-1", victimToken).status)
    }

    @Test
    fun `logging in on a second device keeps the first device refresh token valid`() = runTestApp {
        // ASSUMPTION: refresh tokens are stored per (email, deviceId)
        register("multi@test.com")
        val device1 = login("multi@test.com", deviceId = "device-1").bodyAsText().toJson().field("refreshToken")!!
        login("multi@test.com", deviceId = "device-2")

        assertEquals(HttpStatusCode.OK, refresh("multi@test.com", "device-1", device1).status)
    }

    @Test
    fun `refresh with a missing field returns 400`() = runTestApp {
        val res = api(HttpMethod.Post, "/users/refresh", body = """{"email":"x@test.com"}""")
        assertEquals(HttpStatusCode.BadRequest, res.status)
    }

    // ----------------------------------------------------------------- JWT guard

    @Test
    fun `a valid token gives access to a protected route`() = runTestApp {
        val session = registerAndLogin()
        assertEquals(HttpStatusCode.OK, api(HttpMethod.Get, "/groups", session).status)
    }

    @Test
    fun `a protected route without a token returns 401`() = runTestApp {
        assertEquals(HttpStatusCode.Unauthorized, api(HttpMethod.Get, "/groups").status)
    }

    @Test
    fun `a garbage token returns 401`() = runTestApp {
        val res = api(HttpMethod.Get, "/groups") { headers.append("Authorization", "Bearer not.a.token") }
        assertEquals(HttpStatusCode.Unauthorized, res.status)
    }

    @Test
    fun `a token without the Bearer scheme returns 401`() = runTestApp {
        val session = registerAndLogin()
        val res = api(HttpMethod.Get, "/groups") { headers.append("Authorization", session.token) }
        assertEquals(HttpStatusCode.Unauthorized, res.status)
    }

    @Test
    fun `a token with a tampered signature returns 401`() = runTestApp {
        val session = registerAndLogin()
        val (header, payload, signature) = session.token.split(".")
        // Change a character in the middle of the signature (the last character can carry
        // unused bits, so flipping it may leave the decoded signature untouched)
        val mid = signature.length / 2
        val flipped = if (signature[mid] == 'A') 'B' else 'A'
        val tampered = "$header.$payload.${signature.substring(0, mid)}$flipped${signature.substring(mid + 1)}"

        val res = api(HttpMethod.Get, "/groups") { headers.append("Authorization", "Bearer $tampered") }

        assertEquals(HttpStatusCode.Unauthorized, res.status)
    }

    @Test
    @Ignore("Desired behaviour, to be decided: an access token of a deleted account should stop working")
    fun `an access token of a deleted account is rejected`() = runTestApp {
        val session = registerAndLogin(email = "gone@test.com")
        api(HttpMethod.Delete, "/users", session)

        assertEquals(HttpStatusCode.Unauthorized, api(HttpMethod.Get, "/groups", session).status)
    }

    // ------------------------------------------------------------ delete account

    @Test
    fun `delete account returns 200 and blocks the next login`() = runTestApp {
        // NOTE: if this returns 404, change the path to "/users/" (the route is declared as delete("/"))
        val session = registerAndLogin(email = "delete@test.com")

        assertEquals(HttpStatusCode.OK, api(HttpMethod.Delete, "/users", session).status)
        assertEquals(HttpStatusCode.Forbidden, login("delete@test.com").status)
    }

    @Test
    fun `delete account without a token returns 401`() = runTestApp {
        assertEquals(HttpStatusCode.Unauthorized, api(HttpMethod.Delete, "/users").status)
    }

    // ----------------------------------------------------------- password reset
    // The happy path needs the PIN that is sent by email. It is covered once the email
    // sending is behind an interface (EmailSender) that tests can replace with a fake.

    @Test
    fun `recover password for an unknown email returns 400`() = runTestApp {
        val res = api(HttpMethod.Post, "/users/recover-password", body = Fixtures.recoverBody("ghost@test.com"))
        assertEquals(HttpStatusCode.BadRequest, res.status)
    }

    @Test
    fun `recover password with malformed JSON returns 400`() = runTestApp {
        assertEquals(HttpStatusCode.BadRequest, api(HttpMethod.Post, "/users/recover-password", body = "{").status)
    }

    @Test
    fun `verify pin for an unknown email returns 401`() = runTestApp {
        val res = api(HttpMethod.Post, "/users/verify-pin", body = Fixtures.verifyPinBody("ghost@test.com", "123456"))
        assertEquals(HttpStatusCode.Unauthorized, res.status)
    }

    @Test
    fun `verify pin with a wrong pin returns 401`() = runTestApp {
        register("pin@test.com")
        val res = api(HttpMethod.Post, "/users/verify-pin", body = Fixtures.verifyPinBody("pin@test.com", "000000"))
        assertEquals(HttpStatusCode.Unauthorized, res.status)
    }

    @Test
    fun `reset password without a valid pin returns 400 and keeps the old password`() = runTestApp {
        register("reset@test.com")

        val res = api(
            HttpMethod.Post, "/users/reset-password",
            body = Fixtures.resetPasswordBody("reset@test.com", "000000", "BrandNewPassw0rd!"),
        )

        assertRejected(res.status)
        assertEquals(HttpStatusCode.OK, login("reset@test.com", pw = password).status)
        assertEquals(HttpStatusCode.Unauthorized, login("reset@test.com", pw = "BrandNewPassw0rd!").status)
    }

    @Test
    @Ignore("Desired behaviour, not implemented yet: verify-pin must lock after repeated failures")
    fun `verify pin is rate limited after repeated failures`() = runTestApp {
        // A 6 digit pin has only 1,000,000 combinations, so without a limit it can be brute forced
        register("brute@test.com")
        var lastStatus = HttpStatusCode.Unauthorized
        repeat(20) {
            lastStatus = api(
                HttpMethod.Post, "/users/verify-pin",
                body = Fixtures.verifyPinBody("brute@test.com", "%06d".format(it)),
            ).status
        }
        assertTrue(lastStatus == HttpStatusCode.TooManyRequests || lastStatus == HttpStatusCode.Locked)
    }
}