package com.jrb.routes

import com.jrb.support.Fixtures
import com.jrb.support.Session
import com.jrb.support.api
import com.jrb.support.assertRejected
import com.jrb.support.createGroup
import com.jrb.support.createSettlement
import com.jrb.support.itemCount
import com.jrb.support.newUser
import com.jrb.support.newWorld
import com.jrb.support.requireId
import com.jrb.support.runTestApp
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import java.util.UUID
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettlementRoutesTest {

    private suspend fun ApplicationTestBuilder.list(session: Session, groupId: String, query: String = "") =
        api(HttpMethod.Get, "/settlements/$groupId$query", session)

    private suspend fun ApplicationTestBuilder.count(session: Session, groupId: String, query: String = ""): Int {
        val res = list(session, groupId, query)
        assertEquals(HttpStatusCode.OK, res.status, "Listing settlements failed: ${res.bodyAsText()}")
        return res.bodyAsText().itemCount()
    }

    // -------------------------------------------------------------------- create

    @Test
    fun `create settlement returns 201 and the new id`() = runTestApp {
        val w = newWorld()

        val res = api(
            HttpMethod.Post, "/settlements", w.member,
            Fixtures.settlementBody(w.groupId, w.memberRef, w.ownerRef, 12.5),
        )

        assertEquals(HttpStatusCode.Created, res.status)
        assertTrue(res.bodyAsText().requireId().isNotBlank())
    }

    @Test
    fun `create settlement without a token returns 401`() = runTestApp {
        val res = api(HttpMethod.Post, "/settlements", body = Fixtures.settlementBody(UUID.randomUUID().toString(), "a", "b", 1.0))
        assertEquals(HttpStatusCode.Unauthorized, res.status)
    }

    @Test
    fun `create settlement with malformed JSON returns 400`() = runTestApp {
        val owner = newUser("owner")
        assertEquals(HttpStatusCode.BadRequest, api(HttpMethod.Post, "/settlements", owner, "{ nope").status)
    }

    @Test
    fun `create settlement with an empty body returns 400`() = runTestApp {
        val owner = newUser("owner")
        assertEquals(HttpStatusCode.BadRequest, api(HttpMethod.Post, "/settlements", owner, "").status)
    }

    @Test
    fun `create settlement with an invalid group id is rejected`() = runTestApp {
        // groupId is a plain String in CreateSettlementRequest, so this is checked by the service
        val w = newWorld()
        val res = api(
            HttpMethod.Post, "/settlements", w.owner,
            Fixtures.settlementBody("not-a-uuid", w.ownerRef, w.memberRef, 5.0),
        )
        assertRejected(res.status)
    }

    @Test
    fun `an outsider cannot create a settlement in a group`() = runTestApp {
        val w = newWorld()

        val res = api(
            HttpMethod.Post, "/settlements", w.outsider,
            Fixtures.settlementBody(w.groupId, w.outsiderRef, w.ownerRef, 5.0),
        )

        assertRejected(res.status)
        assertEquals(0, count(w.owner, w.groupId))
    }

    @Test
    fun `a settlement in an unknown group is rejected`() = runTestApp {
        val w = newWorld()
        val res = api(
            HttpMethod.Post, "/settlements", w.owner,
            Fixtures.settlementBody(UUID.randomUUID().toString(), w.ownerRef, w.memberRef, 5.0),
        )
        assertRejected(res.status)
    }

    @Test
    @Ignore("Desired behaviour, not implemented yet: amounts must be greater than zero")
    fun `create settlement with a zero or negative amount returns 400`() = runTestApp {
        val w = newWorld()
        for (amount in listOf(0.0, -3.0)) {
            val res = api(
                HttpMethod.Post, "/settlements", w.owner,
                Fixtures.settlementBody(w.groupId, w.ownerRef, w.memberRef, amount),
            )
            assertEquals(HttpStatusCode.BadRequest, res.status)
        }
    }

    @Test
    @Ignore("Desired behaviour, not implemented yet: payer and receiver must differ and belong to the group")
    fun `create settlement to yourself or with a user outside the group returns 400`() = runTestApp {
        val w = newWorld()
        val toSelf = api(
            HttpMethod.Post, "/settlements", w.owner,
            Fixtures.settlementBody(w.groupId, w.ownerRef, w.ownerRef, 5.0),
        )
        val toOutsider = api(
            HttpMethod.Post, "/settlements", w.owner,
            Fixtures.settlementBody(w.groupId, w.ownerRef, w.outsiderRef, 5.0),
        )
        assertEquals(HttpStatusCode.BadRequest, toSelf.status)
        assertEquals(HttpStatusCode.BadRequest, toOutsider.status)
    }

    // ---------------------------------------------------------------------- list

    @Test
    fun `list returns the settlements of the group`() = runTestApp {
        val w = newWorld()
        createSettlement(w.member, w.groupId, from = w.member, to = w.owner)

        assertEquals(1, count(w.owner, w.groupId))
    }

    @Test
    fun `list of a group without settlements is empty`() = runTestApp {
        val w = newWorld()
        assertEquals(0, count(w.owner, w.groupId))
    }

    @Test
    fun `settlements of one group never appear in another group`() = runTestApp {
        val w = newWorld()
        val otherGroup = createGroup(w.owner, "Other")
        createSettlement(w.owner, w.groupId, from = w.owner, to = w.member)

        assertEquals(0, count(w.owner, otherGroup))
    }

    @Test
    fun `list without a token returns 401`() = runTestApp {
        assertEquals(HttpStatusCode.Unauthorized, api(HttpMethod.Get, "/settlements/${UUID.randomUUID()}").status)
    }

    @Test
    fun `list with an invalid group id returns 400`() = runTestApp {
        val owner = newUser("owner")
        assertEquals(HttpStatusCode.BadRequest, api(HttpMethod.Get, "/settlements/not-a-uuid", owner).status)
    }

    @Test
    fun `an outsider cannot list the settlements of a group`() = runTestApp {
        val w = newWorld()
        createSettlement(w.owner, w.groupId, from = w.owner, to = w.member)
        assertRejected(list(w.outsider, w.groupId).status)
    }

    @Test
    fun `pagination splits the settlements into pages`() = runTestApp {
        val w = newWorld()
        repeat(3) { createSettlement(w.owner, w.groupId, from = w.owner, to = w.member, amount = 1.0 + it) }

        assertEquals(2, count(w.owner, w.groupId, "?page=1&limit=2"))
        assertEquals(1, count(w.owner, w.groupId, "?page=2&limit=2"))
        assertEquals(0, count(w.owner, w.groupId, "?page=3&limit=2"))
    }

    @Test
    fun `zero or negative pagination parameters do not crash the server`() = runTestApp {
        val w = newWorld()
        for (query in listOf("?page=0&limit=10", "?page=-1&limit=10", "?page=1&limit=0", "?page=abc&limit=xyz")) {
            val res = list(w.owner, w.groupId, query)
            assertTrue(res.status.value < 500, "$query returned ${res.status}")
        }
    }

    // -------------------------------------------------------------------- update

    @Test
    fun `update changes the settlement`() = runTestApp {
        val w = newWorld()
        val id = createSettlement(w.owner, w.groupId, from = w.owner, to = w.member, amount = 10.0)

        val res = api(
            HttpMethod.Put, "/settlements/$id", w.owner,
            Fixtures.settlementBody(null, w.ownerRef, w.memberRef, 77.0),
        )

        assertEquals(HttpStatusCode.OK, res.status)
        assertTrue(res.bodyAsText().contains("77"))
    }

    @Test
    fun `update of an unknown settlement is rejected`() = runTestApp {
        val w = newWorld()
        val res = api(
            HttpMethod.Put, "/settlements/${UUID.randomUUID()}", w.owner,
            Fixtures.settlementBody(null, w.ownerRef, w.memberRef, 1.0),
        )
        assertRejected(res.status)
    }

    @Test
    fun `update without a token returns 401`() = runTestApp {
        val res = api(HttpMethod.Put, "/settlements/${UUID.randomUUID()}", body = Fixtures.settlementBody(null, "a", "b", 1.0))
        assertEquals(HttpStatusCode.Unauthorized, res.status)
    }

    @Test
    fun `update with an invalid id returns 400`() = runTestApp {
        val owner = newUser("owner")
        val res = api(HttpMethod.Put, "/settlements/not-a-uuid", owner, Fixtures.settlementBody(null, "a", "b", 1.0))
        assertEquals(HttpStatusCode.BadRequest, res.status)
    }

    @Test
    fun `update with malformed JSON returns 400`() = runTestApp {
        val w = newWorld()
        val id = createSettlement(w.owner, w.groupId, from = w.owner, to = w.member)
        assertEquals(HttpStatusCode.BadRequest, api(HttpMethod.Put, "/settlements/$id", w.owner, "{ nope").status)
    }

    @Test
    fun `an outsider cannot update a settlement and the amount stays the same`() = runTestApp {
        val w = newWorld()
        val id = createSettlement(w.owner, w.groupId, from = w.owner, to = w.member, amount = 10.0)

        val res = api(
            HttpMethod.Put, "/settlements/$id", w.outsider,
            Fixtures.settlementBody(null, w.outsiderRef, w.ownerRef, 999.0),
        )

        assertRejected(res.status)
        assertFalse(list(w.owner, w.groupId).bodyAsText().contains("999"))
    }

    @Test
    @Ignore("Desired behaviour, not implemented yet: updating a missing settlement should be 404, not 500")
    fun `update of an unknown settlement returns 404`() = runTestApp {
        val w = newWorld()
        val res = api(
            HttpMethod.Put, "/settlements/${UUID.randomUUID()}", w.owner,
            Fixtures.settlementBody(null, w.ownerRef, w.memberRef, 1.0),
        )
        assertEquals(HttpStatusCode.NotFound, res.status)
    }

    // -------------------------------------------------------------------- delete

    @Test
    fun `delete returns 204 and the settlement leaves the list`() = runTestApp {
        val w = newWorld()
        val id = createSettlement(w.owner, w.groupId, from = w.owner, to = w.member)

        assertEquals(HttpStatusCode.NoContent, api(HttpMethod.Delete, "/settlements/$id", w.owner).status)

        assertEquals(0, count(w.owner, w.groupId))
    }

    @Test
    fun `deleting twice returns 404 the second time`() = runTestApp {
        val w = newWorld()
        val id = createSettlement(w.owner, w.groupId, from = w.owner, to = w.member)

        assertEquals(HttpStatusCode.NoContent, api(HttpMethod.Delete, "/settlements/$id", w.owner).status)
        assertEquals(HttpStatusCode.NotFound, api(HttpMethod.Delete, "/settlements/$id", w.owner).status)
    }

    @Test
    fun `deleting an unknown settlement returns 404`() = runTestApp {
        val owner = newUser("owner")
        assertEquals(HttpStatusCode.NotFound, api(HttpMethod.Delete, "/settlements/${UUID.randomUUID()}", owner).status)
    }

    @Test
    fun `delete without a token returns 401`() = runTestApp {
        assertEquals(HttpStatusCode.Unauthorized, api(HttpMethod.Delete, "/settlements/${UUID.randomUUID()}").status)
    }

    @Test
    fun `delete with an invalid id returns 400`() = runTestApp {
        val owner = newUser("owner")
        assertEquals(HttpStatusCode.BadRequest, api(HttpMethod.Delete, "/settlements/not-a-uuid", owner).status)
    }

    @Test
    fun `an outsider cannot delete a settlement`() = runTestApp {
        val w = newWorld()
        val id = createSettlement(w.owner, w.groupId, from = w.owner, to = w.member)

        assertEquals(HttpStatusCode.NotFound, api(HttpMethod.Delete, "/settlements/$id", w.outsider).status)
        assertEquals(1, count(w.owner, w.groupId))
    }
}