package com.jrb.routes

import com.jrb.support.Session
import com.jrb.support.api
import com.jrb.support.assertRejected
import com.jrb.support.createExpense
import com.jrb.support.createGroup
import com.jrb.support.itemCount
import com.jrb.support.newUser
import com.jrb.support.newWorld
import com.jrb.support.runTestApp
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ActivityLogRoutesTest {

    private suspend fun ApplicationTestBuilder.logs(session: Session, groupId: String, query: String = "") =
        api(HttpMethod.Get, "/activity-logs/$groupId$query", session)

    private suspend fun ApplicationTestBuilder.logCount(session: Session, groupId: String, query: String = ""): Int {
        val res = logs(session, groupId, query)
        assertEquals(HttpStatusCode.OK, res.status, "Fetching logs failed: ${res.bodyAsText()}")
        return res.bodyAsText().itemCount()
    }

    @Test
    fun `a member can read the activity log of the group`() = runTestApp {
        val w = newWorld()
        assertEquals(HttpStatusCode.OK, logs(w.member, w.groupId).status)
    }

    @Test
    fun `the owner can read the activity log of the group`() = runTestApp {
        val w = newWorld()
        assertEquals(HttpStatusCode.OK, logs(w.owner, w.groupId).status)
    }

    @Test
    fun `creating an expense adds entries to the activity log`() = runTestApp {
        // ASSUMPTION: the expense service writes an activity log entry when an expense is created
        val w = newWorld()
        val before = logCount(w.owner, w.groupId, "?limit=100")

        createExpense(w.owner, w.groupId, w.owner, listOf(w.member))

        assertTrue(logCount(w.owner, w.groupId, "?limit=100") > before)
    }

    @Test
    fun `pagination limits the number of entries`() = runTestApp {
        val w = newWorld()
        repeat(3) { createExpense(w.owner, w.groupId, w.owner, listOf(w.member), description = "Expense $it") }

        assertTrue(logCount(w.owner, w.groupId, "?page=1&limit=1") <= 1)
    }

    @Test
    fun `a page beyond the last one is empty and not an error`() = runTestApp {
        val w = newWorld()
        assertEquals(0, logCount(w.owner, w.groupId, "?page=99&limit=10"))
    }

    @Test
    fun `strange pagination parameters do not crash the server`() = runTestApp {
        val w = newWorld()
        for (query in listOf("?page=0&limit=10", "?page=-1&limit=10", "?page=1&limit=0", "?page=abc&limit=xyz")) {
            val res = logs(w.owner, w.groupId, query)
            assertTrue(res.status.value < 500, "$query returned ${res.status}")
        }
    }

    @Test
    fun `an outsider cannot read the activity log`() = runTestApp {
        val w = newWorld()
        createExpense(w.owner, w.groupId, w.owner, listOf(w.member), description = "Secret dinner")

        val res = logs(w.outsider, w.groupId)

        assertRejected(res.status)
        assertFalse(res.bodyAsText().contains("Secret dinner"))
    }

    @Test
    fun `the log of one group does not mention another group`() = runTestApp {
        val w = newWorld()
        val outsiderGroup = createGroup(w.outsider, "Private")
        createExpense(w.outsider, outsiderGroup, w.outsider, description = "Private expense")

        val body = logs(w.owner, w.groupId).bodyAsText()

        assertFalse(body.contains(outsiderGroup))
        assertFalse(body.contains("Private expense"))
    }

    @Test
    fun `without a token returns 401`() = runTestApp {
        assertEquals(HttpStatusCode.Unauthorized, api(HttpMethod.Get, "/activity-logs/${UUID.randomUUID()}").status)
    }

    @Test
    fun `an invalid group id returns 400`() = runTestApp {
        val owner = newUser("owner")
        assertEquals(HttpStatusCode.BadRequest, api(HttpMethod.Get, "/activity-logs/not-a-uuid", owner).status)
    }

    @Test
    fun `an unknown group is rejected`() = runTestApp {
        val owner = newUser("owner")
        assertRejected(api(HttpMethod.Get, "/activity-logs/${UUID.randomUUID()}", owner).status)
    }
}