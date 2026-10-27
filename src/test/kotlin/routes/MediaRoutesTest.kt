package com.jrb.routes

import com.jrb.support.Fixtures
import com.jrb.support.api
import com.jrb.support.newUser
import com.jrb.support.newWorld
import com.jrb.support.runTestApp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import java.util.UUID
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// None of these tests reach Cloudinary: uploads are only tested with inputs that are rejected
// before the upload, and downloads only check the redirect that the server builds.
// The upload happy path needs MediaService behind an interface so a fake can replace it.
class MediaRoutesTest {

    private val notBase64 = "%%% this is not base64 %%%"

    // ------------------------------------------------------------- authentication

    @Test
    fun `every media route requires a token`() = runTestApp {
        val id = UUID.randomUUID().toString()

        assertEquals(HttpStatusCode.Unauthorized, api(HttpMethod.Post, "/media/upload", body = Fixtures.uploadReceiptBody("AAAA", "receipts", id)).status)
        assertEquals(HttpStatusCode.Unauthorized, api(HttpMethod.Post, "/media/avatar/upload", body = Fixtures.uploadAvatarBody("AAAA")).status)
        assertEquals(HttpStatusCode.Unauthorized, api(HttpMethod.Get, "/media/receipt/$id").status)
        assertEquals(HttpStatusCode.Unauthorized, api(HttpMethod.Get, "/media/avatar/$id").status)
    }

    // ------------------------------------------------------------ upload: rejects

    @Test
    fun `receipt upload with malformed JSON returns 400`() = runTestApp {
        val user = newUser("user")
        assertEquals(HttpStatusCode.BadRequest, api(HttpMethod.Post, "/media/upload", user, "{ nope").status)
    }

    @Test
    fun `receipt upload with invalid base64 returns 400`() = runTestApp {
        // NOTE: this also returns 400 when the JSON field names are wrong, so keep
        // Fixtures.uploadReceiptBody in sync with UploadReceiptImageRequest
        val user = newUser("user")
        val res = api(HttpMethod.Post, "/media/upload", user, Fixtures.uploadReceiptBody(notBase64, "receipts", UUID.randomUUID().toString()))
        assertEquals(HttpStatusCode.BadRequest, res.status)
    }

    @Test
    fun `avatar upload with malformed JSON returns 400`() = runTestApp {
        val user = newUser("user")
        assertEquals(HttpStatusCode.BadRequest, api(HttpMethod.Post, "/media/avatar/upload", user, "{ nope").status)
    }

    @Test
    fun `avatar upload with invalid base64 returns 400`() = runTestApp {
        val user = newUser("user")
        val res = api(HttpMethod.Post, "/media/avatar/upload", user, Fixtures.uploadAvatarBody(notBase64))
        assertEquals(HttpStatusCode.BadRequest, res.status)
    }

    @Test
    @Ignore("Desired behaviour, not implemented yet: reject oversized or non image payloads before uploading")
    fun `upload of a huge or non image payload is rejected with 413 or 400`() = runTestApp {
        val user = newUser("user")
        val hugeBase64 = "A".repeat(20_000_000)
        val res = api(HttpMethod.Post, "/media/avatar/upload", user, Fixtures.uploadAvatarBody(hugeBase64))
        assertTrue(res.status == HttpStatusCode.PayloadTooLarge || res.status == HttpStatusCode.BadRequest)
    }

    // ---------------------------------------------------------------- downloads

    @Test
    fun `receipt download redirects to the storage url`() = runTestApp {
        val user = newUser("user")
        val expenseId = UUID.randomUUID().toString()
        val noRedirects = createClient { followRedirects = false }

        val res = noRedirects.get("/media/receipt/$expenseId") {
            header(HttpHeaders.Authorization, user.bearer)
        }

        assertEquals(HttpStatusCode.Found, res.status)
        val location = res.headers[HttpHeaders.Location]
        assertNotNull(location)
        assertTrue(location.contains(expenseId), "The redirect should point to the file of that expense")
    }

    @Test
    fun `avatar download redirects to the storage url`() = runTestApp {
        val user = newUser("user")
        val other = newUser("other")
        val noRedirects = createClient { followRedirects = false }

        val res = noRedirects.get("/media/avatar/${other.userId}") {
            header(HttpHeaders.Authorization, user.bearer)
        }

        assertEquals(HttpStatusCode.Found, res.status)
        val location = res.headers[HttpHeaders.Location]
        assertNotNull(location)
        assertTrue(location.contains(other.userId))
    }

    @Test
    @Ignore("Desired behaviour, not implemented yet: only members of the group may fetch a receipt")
    fun `an outsider cannot fetch the receipt of an expense of another group`() = runTestApp {
        val w = newWorld()
        val noRedirects = createClient { followRedirects = false }

        val res = noRedirects.get("/media/receipt/${UUID.randomUUID()}") {
            header(HttpHeaders.Authorization, w.outsider.bearer)
        }

        assertTrue(res.status == HttpStatusCode.Forbidden || res.status == HttpStatusCode.NotFound)
    }
}