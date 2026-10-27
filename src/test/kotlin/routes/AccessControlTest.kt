package com.jrb.routes

import com.jrb.support.Fixtures
import com.jrb.support.addMembers
import com.jrb.support.api
import com.jrb.support.assertRejected
import com.jrb.support.createExpense
import com.jrb.support.createSettlement
import com.jrb.support.itemCount
import com.jrb.support.newWorld
import com.jrb.support.runTestApp
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Cross-user authorization: one scenario, every endpoint, the same rule.
// A user that is not in the group must not be able to read or change anything in it.
class AccessControlTest {

    private class Attempt(val name: String, val method: HttpMethod, val path: String, val body: String? = null)

    @Test
    fun `an outsider is rejected on every endpoint that touches the group and nothing changes`() = runTestApp {
        val w = newWorld()
        val expenseId = createExpense(w.owner, w.groupId, w.owner, listOf(w.member), description = "Original expense")
        val settlementId = createSettlement(w.owner, w.groupId, from = w.member, to = w.owner, amount = 10.0)
        val outsiderId = w.outsiderRef

        val attempts = listOf(
            Attempt("list expenses", HttpMethod.Get, "/expenses/${w.groupId}"),
            Attempt("create expense", HttpMethod.Post, "/expenses",
                Fixtures.expenseBody(w.groupId, outsiderId, 5.0, "Hacked expense", mapOf(outsiderId to 5.0))),
            Attempt("update expense", HttpMethod.Put, "/expenses/$expenseId",
                Fixtures.expenseBody(null, outsiderId, 5.0, "Hacked expense", mapOf(outsiderId to 5.0))),
            Attempt("delete expense", HttpMethod.Delete, "/expenses/$expenseId"),
            Attempt("list settlements", HttpMethod.Get, "/settlements/${w.groupId}"),
            Attempt("create settlement", HttpMethod.Post, "/settlements",
                Fixtures.settlementBody(w.groupId, outsiderId, w.ownerRef, 555.0)),
            Attempt("update settlement", HttpMethod.Put, "/settlements/$settlementId",
                Fixtures.settlementBody(null, outsiderId, w.ownerRef, 555.0)),
            Attempt("delete settlement", HttpMethod.Delete, "/settlements/$settlementId"),
            Attempt("read activity log", HttpMethod.Get, "/activity-logs/${w.groupId}"),
            Attempt("add members", HttpMethod.Post, "/groups/${w.groupId}/members",
                Fixtures.addMembersBody(listOf(outsiderId))),
            Attempt("remove a member", HttpMethod.Delete, "/groups/${w.groupId}/members/${w.memberRef}"),
        )

        for (attempt in attempts) {
            val res = api(attempt.method, attempt.path, w.outsider, attempt.body)
            assertRejected(res.status, "Endpoint '${attempt.name}' accepted an outsider")
        }

        // Nothing was created, changed or deleted by the attempts above
        val expenses = api(HttpMethod.Get, "/expenses/${w.groupId}", w.owner).bodyAsText()
        assertEquals(1, expenses.itemCount())
        assertTrue(expenses.contains("Original expense"))
        assertFalse(expenses.contains("Hacked expense"))

        val settlements = api(HttpMethod.Get, "/settlements/${w.groupId}", w.owner).bodyAsText()
        assertEquals(1, settlements.itemCount())
        assertFalse(settlements.contains("555"))

        assertEquals(HttpStatusCode.OK, api(HttpMethod.Get, "/expenses/${w.groupId}", w.member).status)
    }

    @Test
    fun `a removed member loses access to every endpoint of the group`() = runTestApp {
        val w = newWorld()
        val expenseId = createExpense(w.owner, w.groupId, w.owner, listOf(w.member))
        assertEquals(HttpStatusCode.OK, api(HttpMethod.Get, "/expenses/${w.groupId}", w.member).status)

        api(HttpMethod.Delete, "/groups/${w.groupId}/members/${w.memberRef}", w.owner)

        assertRejected(api(HttpMethod.Get, "/expenses/${w.groupId}", w.member).status)
        assertRejected(api(HttpMethod.Get, "/settlements/${w.groupId}", w.member).status)
        assertRejected(api(HttpMethod.Get, "/activity-logs/${w.groupId}", w.member).status)
        assertRejected(
            api(
                HttpMethod.Post, "/expenses", w.member,
                Fixtures.expenseBody(w.groupId, w.memberRef, 5.0, "After removal", mapOf(w.memberRef to 5.0)),
            ).status,
        )
        assertEquals(HttpStatusCode.NotFound, api(HttpMethod.Delete, "/expenses/$expenseId", w.member).status)
    }

    @Test
    fun `a member added later can see the expenses created before they joined`() = runTestApp {
        // Documents the current behaviour (history is visible to new members); change the
        // assertion if you decide that new members must only see what happens after joining
        val w = newWorld()
        createExpense(w.owner, w.groupId, w.owner, description = "Before joining")
        addMembers(w.owner, w.groupId, w.outsider)

        val body = api(HttpMethod.Get, "/expenses/${w.groupId}", w.outsider).bodyAsText()

        assertTrue(body.contains("Before joining"))
    }

    @Test
    fun `an unknown route returns 404 for authenticated and anonymous callers`() = runTestApp {
        val w = newWorld()
        assertEquals(HttpStatusCode.NotFound, api(HttpMethod.Get, "/does-not-exist", w.owner).status)
        assertEquals(HttpStatusCode.NotFound, api(HttpMethod.Get, "/does-not-exist").status)
        assertEquals(HttpStatusCode.NotFound, api(HttpMethod.Get, "/expenses/${UUID.randomUUID()}/extra", w.owner).status)
    }
}