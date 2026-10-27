package com.jrb.routes

import com.jrb.support.Fixtures
import com.jrb.support.Session
import com.jrb.support.api
import com.jrb.support.assertRejected
import com.jrb.support.createExpense
import com.jrb.support.createGroup
import com.jrb.support.itemCount
import com.jrb.support.newUser
import com.jrb.support.newWorld
import com.jrb.support.arrayField
import com.jrb.support.field
import com.jrb.support.requireId
import com.jrb.support.toJson
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

class ExpenseRoutesTest {

    // Year 2100 in milliseconds: later than any timestamp the server can have stored
    private val farFuture = 4_102_444_800_000L

    private suspend fun ApplicationTestBuilder.list(session: Session, groupId: String, query: String = "") =
        api(HttpMethod.Get, "/expenses/$groupId$query", session)

    private suspend fun ApplicationTestBuilder.count(session: Session, groupId: String, query: String = ""): Int {
        val res = list(session, groupId, query)
        assertEquals(HttpStatusCode.OK, res.status, "Listing expenses failed: ${res.bodyAsText()}")
        return res.bodyAsText().itemCount()
    }

    // -------------------------------------------------------------------- create

    @Test
    fun `create expense returns 201 and the new id`() = runTestApp {
        val w = newWorld()

        val res = api(
            HttpMethod.Post, "/expenses", w.owner,
            Fixtures.expenseBody(w.groupId, w.ownerRef, 30.0, "Dinner", mapOf(w.ownerRef to 15.0, w.memberRef to 15.0)),
        )

        assertEquals(HttpStatusCode.Created, res.status)
        assertTrue(res.bodyAsText().requireId().isNotBlank())
    }

    @Test
    fun `a member can create an expense and the owner sees it`() = runTestApp {
        val w = newWorld()

        createExpense(w.member, w.groupId, payer = w.member, others = listOf(w.owner), description = "Taxi")

        val body = list(w.owner, w.groupId).bodyAsText()
        assertTrue(body.contains("Taxi"))
    }

    @Test
    fun `create expense without a token returns 401`() = runTestApp {
        val res = api(HttpMethod.Post, "/expenses", body = Fixtures.expenseBody(UUID.randomUUID().toString(), "x", 1.0, "x", emptyMap()))
        assertEquals(HttpStatusCode.Unauthorized, res.status)
    }

    @Test
    fun `create expense with malformed JSON returns 400`() = runTestApp {
        val owner = newUser("owner")
        assertEquals(HttpStatusCode.BadRequest, api(HttpMethod.Post, "/expenses", owner, "{ nope").status)
    }

    @Test
    fun `create expense with an empty body returns 400`() = runTestApp {
        val owner = newUser("owner")
        assertEquals(HttpStatusCode.BadRequest, api(HttpMethod.Post, "/expenses", owner, "").status)
    }

    @Test
    fun `create expense with an invalid group id is rejected`() = runTestApp {
        // groupId is a plain String in CreateExpenseRequest, so this is checked by the service
        val owner = newUser("owner")
        val res = api(
            HttpMethod.Post, "/expenses", owner,
            Fixtures.expenseBody("not-a-uuid", owner.userId, 10.0, "x", mapOf(owner.userId to 10.0)),
        )
        assertRejected(res.status)
    }

    @Test
    fun `an outsider cannot create an expense in a group and nothing is stored`() = runTestApp {
        val w = newWorld()

        val res = api(
            HttpMethod.Post, "/expenses", w.outsider,
            Fixtures.expenseBody(w.groupId, w.outsiderRef, 10.0, "Intruder", mapOf(w.outsiderRef to 10.0)),
        )

        assertRejected(res.status)
        assertFalse(list(w.owner, w.groupId).bodyAsText().contains("Intruder"))
    }

    @Test
    fun `an expense in an unknown group is rejected`() = runTestApp {
        val owner = newUser("owner")
        val res = api(
            HttpMethod.Post, "/expenses", owner,
            Fixtures.expenseBody(UUID.randomUUID().toString(), owner.userId, 10.0, "x", mapOf(owner.userId to 10.0)),
        )
        assertRejected(res.status)
    }

    @Test
    @Ignore("Desired behaviour, not implemented yet: amounts must be greater than zero")
    fun `create expense with a zero or negative amount returns 400`() = runTestApp {
        val w = newWorld()
        for (amount in listOf(0.0, -5.0)) {
            val res = api(
                HttpMethod.Post, "/expenses", w.owner,
                Fixtures.expenseBody(w.groupId, w.ownerRef, amount, "Bad", mapOf(w.ownerRef to amount)),
            )
            assertEquals(HttpStatusCode.BadRequest, res.status)
        }
    }

    @Test
    @Ignore("Desired behaviour, not implemented yet: the splits must add up to the total amount")
    fun `create expense whose splits do not add up to the total returns 400`() = runTestApp {
        val w = newWorld()
        val res = api(
            HttpMethod.Post, "/expenses", w.owner,
            Fixtures.expenseBody(w.groupId, w.ownerRef, 30.0, "Bad splits", mapOf(w.ownerRef to 5.0, w.memberRef to 5.0)),
        )
        assertEquals(HttpStatusCode.BadRequest, res.status)
    }

    @Test
    @Ignore("Desired behaviour, not implemented yet: payer and split users must belong to the group")
    fun `create expense with a payer or a split user outside the group returns 400`() = runTestApp {
        val w = newWorld()
        val paidByOutsider = api(
            HttpMethod.Post, "/expenses", w.owner,
            Fixtures.expenseBody(w.groupId, w.outsiderRef, 10.0, "Payer outside", mapOf(w.ownerRef to 10.0)),
        )
        val splitWithOutsider = api(
            HttpMethod.Post, "/expenses", w.owner,
            Fixtures.expenseBody(w.groupId, w.ownerRef, 10.0, "Split outside", mapOf(w.outsiderRef to 10.0)),
        )
        assertEquals(HttpStatusCode.BadRequest, paidByOutsider.status)
        assertEquals(HttpStatusCode.BadRequest, splitWithOutsider.status)
    }

    // ---------------------------------------------------------------------- list

    @Test
    fun `list returns the expenses of the group`() = runTestApp {
        val w = newWorld()
        createExpense(w.owner, w.groupId, w.owner, listOf(w.member), description = "Groceries")

        val body = list(w.member, w.groupId).bodyAsText()

        assertEquals(1, body.itemCount())
        assertTrue(body.contains("Groceries"))
    }

    @Test
    fun `list of a group without expenses is empty`() = runTestApp {
        val w = newWorld()
        assertEquals(0, count(w.owner, w.groupId))
    }

    @Test
    fun `expenses of one group never appear in another group`() = runTestApp {
        val w = newWorld()
        val otherGroup = createGroup(w.owner, "Other")
        createExpense(w.owner, w.groupId, w.owner, listOf(w.member), description = "Only here")

        assertEquals(0, count(w.owner, otherGroup))
        assertEquals(1, count(w.owner, w.groupId))
    }

    @Test
    fun `list without a token returns 401`() = runTestApp {
        assertEquals(HttpStatusCode.Unauthorized, api(HttpMethod.Get, "/expenses/${UUID.randomUUID()}").status)
    }

    @Test
    fun `list with an invalid group id returns 400`() = runTestApp {
        val owner = newUser("owner")
        assertEquals(HttpStatusCode.BadRequest, api(HttpMethod.Get, "/expenses/not-a-uuid", owner).status)
    }

    @Test
    fun `an outsider cannot list the expenses of a group`() = runTestApp {
        val w = newWorld()
        createExpense(w.owner, w.groupId, w.owner, listOf(w.member), description = "Secret")

        val res = list(w.outsider, w.groupId)

        assertRejected(res.status)
        assertFalse(res.bodyAsText().contains("Secret"))
    }

    @Test
    @Ignore("Desired behaviour, not implemented yet: a failed authorization should be 403, not 500")
    fun `an outsider listing expenses gets 403`() = runTestApp {
        val w = newWorld()
        assertEquals(HttpStatusCode.Forbidden, list(w.outsider, w.groupId).status)
    }

    // ---------------------------------------------------------------- pagination

    @Test
    fun `pagination splits the expenses into pages`() = runTestApp {
        val w = newWorld()
        repeat(3) { createExpense(w.owner, w.groupId, w.owner, listOf(w.member), description = "Expense $it") }

        assertEquals(2, count(w.owner, w.groupId, "?page=1&limit=2"))
        assertEquals(1, count(w.owner, w.groupId, "?page=2&limit=2"))
        assertEquals(0, count(w.owner, w.groupId, "?page=3&limit=2"))
    }

    @Test
    fun `pages do not overlap`() = runTestApp {
        val w = newWorld()
        repeat(2) { createExpense(w.owner, w.groupId, w.owner, listOf(w.member), description = "Unique-$it") }

        val first = list(w.owner, w.groupId, "?page=1&limit=1").bodyAsText()
        val second = list(w.owner, w.groupId, "?page=2&limit=1").bodyAsText()

        val firstHas0 = first.contains("Unique-0")
        val secondHas0 = second.contains("Unique-0")
        assertTrue(firstHas0 != secondHas0, "The same expense must not be returned on both pages")
    }

    @Test
    fun `without pagination parameters all expenses up to the default limit are returned`() = runTestApp {
        val w = newWorld()
        repeat(3) { createExpense(w.owner, w.groupId, w.owner, listOf(w.member), description = "Expense $it") }

        assertEquals(3, count(w.owner, w.groupId))
    }

    @Test
    fun `a page beyond the last one is empty and not an error`() = runTestApp {
        val w = newWorld()
        createExpense(w.owner, w.groupId, w.owner, listOf(w.member))

        assertEquals(0, count(w.owner, w.groupId, "?page=99&limit=10"))
    }

    @Test
    fun `non numeric pagination parameters do not crash the server`() = runTestApp {
        val w = newWorld()
        val res = list(w.owner, w.groupId, "?page=abc&limit=xyz")
        assertTrue(res.status.value < 500, "Got ${res.status}")
    }

    @Test
    fun `zero or negative pagination parameters do not crash the server`() = runTestApp {
        // If one of these fails with 500, it is a real finding: getPaginationParams needs validation
        val w = newWorld()
        for (query in listOf("?page=0&limit=10", "?page=-1&limit=10", "?page=1&limit=0", "?page=1&limit=-5")) {
            val res = list(w.owner, w.groupId, query)
            assertTrue(res.status.value < 500, "$query returned ${res.status}")
        }
    }

    @Test
    fun `a huge limit does not crash the server`() = runTestApp {
        val w = newWorld()
        val res = list(w.owner, w.groupId, "?page=1&limit=1000000")
        assertTrue(res.status.value < 500, "Got ${res.status}")
    }

    // ------------------------------------------------------- sync (since, deleted)

    @Test
    fun `since in the far future returns nothing and since 0 returns everything`() = runTestApp {
        val w = newWorld()
        createExpense(w.owner, w.groupId, w.owner, listOf(w.member))

        assertEquals(0, count(w.owner, w.groupId, "?since=$farFuture"))
        assertEquals(1, count(w.owner, w.groupId, "?since=0"))
    }

    @Test
    fun `a non numeric since is treated as zero`() = runTestApp {
        val w = newWorld()
        createExpense(w.owner, w.groupId, w.owner, listOf(w.member))

        assertEquals(1, count(w.owner, w.groupId, "?since=yesterday"))
    }

    // -------------------------------------------------------------------- update

    @Test
    fun `update changes the expense and the list shows the new data`() = runTestApp {
        val w = newWorld()
        val expenseId = createExpense(w.owner, w.groupId, w.owner, listOf(w.member), description = "Old name")

        val res = api(
            HttpMethod.Put, "/expenses/$expenseId", w.owner,
            Fixtures.expenseBody(null, w.ownerRef, 40.0, "New name", mapOf(w.ownerRef to 20.0, w.memberRef to 20.0)),
        )

        assertEquals(HttpStatusCode.OK, res.status)
        assertTrue(res.bodyAsText().contains("New name"))
        val listing = list(w.owner, w.groupId).bodyAsText()
        assertTrue(listing.contains("New name"))
        assertFalse(listing.contains("Old name"))
    }

    @Test
    fun `update replaces the splits instead of adding to them`() = runTestApp {
        val w = newWorld()
        val expenseId = createExpense(w.owner, w.groupId, w.owner, listOf(w.member))

        val res = api(
            HttpMethod.Put, "/expenses/$expenseId", w.owner,
            Fixtures.expenseBody(null, w.ownerRef, 30.0, "Solo", mapOf(w.ownerRef to 30.0)),
        )

        assertEquals(HttpStatusCode.OK, res.status)
        // The old split may still be returned as a soft deleted one (deletedAt set), but never as active
        val activeMemberSplits = res.bodyAsText().toJson().arrayField("splits")
            .filter { it.field("owedById") == w.memberRef && it.field("deletedAt") == null }
        assertTrue(activeMemberSplits.isEmpty(), "The old split of the member must not be active")
    }

    @Test
    fun `update of an unknown expense is rejected`() = runTestApp {
        val w = newWorld()
        val res = api(
            HttpMethod.Put, "/expenses/${UUID.randomUUID()}", w.owner,
            Fixtures.expenseBody(null, w.ownerRef, 1.0, "Ghost", mapOf(w.ownerRef to 1.0)),
        )
        assertRejected(res.status)
    }

    @Test
    fun `update without a token returns 401`() = runTestApp {
        val res = api(
            HttpMethod.Put, "/expenses/${UUID.randomUUID()}",
            body = Fixtures.expenseBody(null, "x", 1.0, "x", emptyMap()),
        )
        assertEquals(HttpStatusCode.Unauthorized, res.status)
    }

    @Test
    fun `update with an invalid id returns 400`() = runTestApp {
        val owner = newUser("owner")
        val res = api(HttpMethod.Put, "/expenses/not-a-uuid", owner, Fixtures.expenseBody(null, owner.userId, 1.0, "x", emptyMap()))
        assertEquals(HttpStatusCode.BadRequest, res.status)
    }

    @Test
    fun `update with malformed JSON returns 400`() = runTestApp {
        val w = newWorld()
        val expenseId = createExpense(w.owner, w.groupId, w.owner, listOf(w.member))
        assertEquals(HttpStatusCode.BadRequest, api(HttpMethod.Put, "/expenses/$expenseId", w.owner, "{ nope").status)
    }

    @Test
    fun `an outsider cannot update an expense and the data stays unchanged`() = runTestApp {
        val w = newWorld()
        val expenseId = createExpense(w.owner, w.groupId, w.owner, listOf(w.member), description = "Original")

        val res = api(
            HttpMethod.Put, "/expenses/$expenseId", w.outsider,
            Fixtures.expenseBody(null, w.outsiderRef, 1.0, "Hacked", mapOf(w.outsiderRef to 1.0)),
        )

        assertRejected(res.status)
        val listing = list(w.owner, w.groupId).bodyAsText()
        assertTrue(listing.contains("Original"))
        assertFalse(listing.contains("Hacked"))
    }

    // -------------------------------------------------------------------- delete

    @Test
    fun `delete returns 204 and the expense leaves the default list`() = runTestApp {
        val w = newWorld()
        val expenseId = createExpense(w.owner, w.groupId, w.owner, listOf(w.member))

        assertEquals(HttpStatusCode.NoContent, api(HttpMethod.Delete, "/expenses/$expenseId", w.owner).status)

        assertEquals(0, count(w.owner, w.groupId))
    }

    @Test
    fun `a deleted expense is still returned when includeDeleted is true`() = runTestApp {
        // Soft delete: clients that sync need the tombstone to remove the expense locally
        val w = newWorld()
        val expenseId = createExpense(w.owner, w.groupId, w.owner, listOf(w.member))
        api(HttpMethod.Delete, "/expenses/$expenseId", w.owner)

        assertEquals(1, count(w.owner, w.groupId, "?includeDeleted=true"))
        assertEquals(0, count(w.owner, w.groupId, "?includeDeleted=false"))
    }

    @Test
    fun `deleting twice returns 404 the second time`() = runTestApp {
        val w = newWorld()
        val expenseId = createExpense(w.owner, w.groupId, w.owner, listOf(w.member))

        assertEquals(HttpStatusCode.NoContent, api(HttpMethod.Delete, "/expenses/$expenseId", w.owner).status)
        assertEquals(HttpStatusCode.NotFound, api(HttpMethod.Delete, "/expenses/$expenseId", w.owner).status)
    }

    @Test
    fun `deleting an unknown expense returns 404`() = runTestApp {
        val owner = newUser("owner")
        assertEquals(HttpStatusCode.NotFound, api(HttpMethod.Delete, "/expenses/${UUID.randomUUID()}", owner).status)
    }

    @Test
    fun `delete without a token returns 401`() = runTestApp {
        assertEquals(HttpStatusCode.Unauthorized, api(HttpMethod.Delete, "/expenses/${UUID.randomUUID()}").status)
    }

    @Test
    fun `delete with an invalid id returns 400`() = runTestApp {
        val owner = newUser("owner")
        assertEquals(HttpStatusCode.BadRequest, api(HttpMethod.Delete, "/expenses/not-a-uuid", owner).status)
    }

    @Test
    fun `an outsider cannot delete an expense`() = runTestApp {
        val w = newWorld()
        val expenseId = createExpense(w.owner, w.groupId, w.owner, listOf(w.member))

        val res = api(HttpMethod.Delete, "/expenses/$expenseId", w.outsider)

        assertEquals(HttpStatusCode.NotFound, res.status)
        assertEquals(1, count(w.owner, w.groupId))
    }
}