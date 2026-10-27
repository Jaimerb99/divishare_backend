package com.jrb.routes

import com.jrb.support.Fixtures
import com.jrb.support.Session
import com.jrb.support.addMembers
import com.jrb.support.api
import com.jrb.support.assertRejected
import com.jrb.support.createGroup
import com.jrb.support.itemCount
import com.jrb.support.memberRowId
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

class GroupRoutesTest {

    private suspend fun ApplicationTestBuilder.myGroupsBody(session: Session): String {
        val res = api(HttpMethod.Get, "/groups", session)
        assertEquals(HttpStatusCode.OK, res.status)
        return res.bodyAsText()
    }

    // -------------------------------------------------------------------- create

    @Test
    fun `create group returns 201 and the new id`() = runTestApp {
        val owner = newUser("owner")

        val res = api(HttpMethod.Post, "/groups", owner, Fixtures.createGroupBody("Holidays", owner.userId))

        assertEquals(HttpStatusCode.Created, res.status)
        assertTrue(res.bodyAsText().requireId().isNotBlank())
    }

    @Test
    fun `create group without a token returns 401`() = runTestApp {
        assertEquals(HttpStatusCode.Unauthorized, api(HttpMethod.Post, "/groups", body = Fixtures.createGroupBody("x", "any-user")).status)
    }

    @Test
    fun `create group with malformed JSON returns 400`() = runTestApp {
        val owner = newUser("owner")
        assertEquals(HttpStatusCode.BadRequest, api(HttpMethod.Post, "/groups", owner, "{ nope").status)
    }

    @Test
    fun `create group with an empty body returns 400`() = runTestApp {
        val owner = newUser("owner")
        assertEquals(HttpStatusCode.BadRequest, api(HttpMethod.Post, "/groups", owner, "").status)
    }

    @Test
    @Ignore("Desired behaviour, not implemented yet: a blank group name should return 400")
    fun `create group with a blank name returns 400`() = runTestApp {
        val owner = newUser("owner")
        assertEquals(HttpStatusCode.BadRequest, api(HttpMethod.Post, "/groups", owner, Fixtures.createGroupBody("   ", owner.userId)).status)
    }

    // ---------------------------------------------------------------------- list

    @Test
    fun `a new user has no groups`() = runTestApp {
        val user = newUser("fresh")
        assertEquals(0, myGroupsBody(user).itemCount())
    }

    @Test
    fun `the creator sees the new group in their list`() = runTestApp {
        val owner = newUser("owner")
        val groupId = createGroup(owner)

        assertTrue(myGroupsBody(owner).contains(groupId))
    }

    @Test
    fun `a user only sees their own groups`() = runTestApp {
        val alice = newUser("alice")
        val bob = newUser("bob")
        val aliceGroup = createGroup(alice, "Alice group")
        val bobGroup = createGroup(bob, "Bob group")

        val aliceList = myGroupsBody(alice)
        val bobList = myGroupsBody(bob)

        assertTrue(aliceList.contains(aliceGroup) && !aliceList.contains(bobGroup))
        assertTrue(bobList.contains(bobGroup) && !bobList.contains(aliceGroup))
    }

    @Test
    fun `a user with several groups sees all of them`() = runTestApp {
        val owner = newUser("owner")
        createGroup(owner, "One")
        createGroup(owner, "Two")
        createGroup(owner, "Three")

        assertEquals(3, myGroupsBody(owner).itemCount())
    }

    @Test
    fun `list groups without a token returns 401`() = runTestApp {
        assertEquals(HttpStatusCode.Unauthorized, api(HttpMethod.Get, "/groups").status)
    }

    // --------------------------------------------------------------- add members

    @Test
    fun `the owner can add a member and the member sees the group`() = runTestApp {
        val owner = newUser("owner")
        val member = newUser("member")
        val groupId = createGroup(owner)

        val res = api(HttpMethod.Post, "/groups/$groupId/members", owner, Fixtures.addMembersBody(listOf(member.userId)))

        assertEquals(HttpStatusCode.Created, res.status)
        assertTrue(myGroupsBody(member).contains(groupId))
    }

    @Test
    fun `several members can be added in one request`() = runTestApp {
        val owner = newUser("owner")
        val one = newUser("one")
        val two = newUser("two")
        val groupId = createGroup(owner)

        addMembers(owner, groupId, one, two)

        assertTrue(myGroupsBody(one).contains(groupId))
        assertTrue(myGroupsBody(two).contains(groupId))
    }

    @Test
    fun `adding the same member twice does not duplicate the group`() = runTestApp {
        val owner = newUser("owner")
        val member = newUser("member")
        val groupId = createGroup(owner)

        api(HttpMethod.Post, "/groups/$groupId/members", owner, Fixtures.addMembersBody(listOf(member.userId)))
        api(HttpMethod.Post, "/groups/$groupId/members", owner, Fixtures.addMembersBody(listOf(member.userId)))

        // The group id also appears inside every member, so count groups instead of occurrences
        assertEquals(1, myGroupsBody(member).itemCount())
    }

    @Test
    fun `a ghost member without an account can be added`() = runTestApp {
        val owner = newUser("owner")
        val groupId = createGroup(owner)

        val res = api(HttpMethod.Post, "/groups/$groupId/members", owner, Fixtures.addGhostMembersBody("Aunt Rosa"))

        assertEquals(HttpStatusCode.Created, res.status)
        assertTrue(myGroupsBody(owner).contains("Aunt Rosa"))
    }

    @Test
    fun `adding an unknown user is rejected`() = runTestApp {
        val owner = newUser("owner")
        val groupId = createGroup(owner)

        val res = api(HttpMethod.Post, "/groups/$groupId/members", owner, Fixtures.addMembersBody(listOf(UUID.randomUUID().toString())))

        assertRejected(res.status)
    }

    @Test
    fun `adding members to an unknown group is rejected`() = runTestApp {
        val owner = newUser("owner")
        val member = newUser("member")

        val res = api(HttpMethod.Post, "/groups/${UUID.randomUUID()}/members", owner, Fixtures.addMembersBody(listOf(member.userId)))

        assertRejected(res.status)
    }

    @Test
    fun `add members without a token returns 401`() = runTestApp {
        val res = api(HttpMethod.Post, "/groups/${UUID.randomUUID()}/members", body = Fixtures.addMembersBody(listOf("x")))
        assertEquals(HttpStatusCode.Unauthorized, res.status)
    }

    @Test
    fun `add members with an invalid group id returns 400`() = runTestApp {
        val owner = newUser("owner")
        val res = api(HttpMethod.Post, "/groups/not-a-uuid/members", owner, Fixtures.addMembersBody(listOf(owner.userId)))
        assertEquals(HttpStatusCode.BadRequest, res.status)
    }

    @Test
    fun `add members with malformed JSON returns 400`() = runTestApp {
        val owner = newUser("owner")
        val groupId = createGroup(owner)
        assertEquals(HttpStatusCode.BadRequest, api(HttpMethod.Post, "/groups/$groupId/members", owner, "{ nope").status)
    }

    @Test
    fun `an outsider cannot add members and gains no access`() = runTestApp {
        val w = newWorld()

        val res = api(HttpMethod.Post, "/groups/${w.groupId}/members", w.outsider, Fixtures.addMembersBody(listOf(w.outsider.userId)))

        assertRejected(res.status)
        assertFalse(myGroupsBody(w.outsider).contains(w.groupId))
    }

    @Test
    @Ignore("Desired behaviour, not implemented yet: a failed authorization should be 403, not 500")
    fun `an outsider adding members gets 403`() = runTestApp {
        val w = newWorld()
        val res = api(HttpMethod.Post, "/groups/${w.groupId}/members", w.outsider, Fixtures.addMembersBody(listOf(w.outsider.userId)))
        assertEquals(HttpStatusCode.Forbidden, res.status)
    }

    // ------------------------------------------------------------ remove members

    @Test
    fun `the owner can remove a member and the group disappears from their list`() = runTestApp {
        val w = newWorld()

        val res = api(HttpMethod.Delete, "/groups/${w.groupId}/members/${w.memberRef}", w.owner)

        assertEquals(HttpStatusCode.NoContent, res.status)
        assertFalse(myGroupsBody(w.member).contains(w.groupId))
    }

    @Test
    fun `removing a member does not affect the other members`() = runTestApp {
        val owner = newUser("owner")
        val one = newUser("one")
        val two = newUser("two")
        val groupId = createGroup(owner)
        addMembers(owner, groupId, one, two)

        val oneRowId = memberRowId(owner, groupId, one.userId)
        api(HttpMethod.Delete, "/groups/$groupId/members/$oneRowId", owner)

        assertTrue(myGroupsBody(two).contains(groupId))
        assertTrue(myGroupsBody(owner).contains(groupId))
    }

    @Test
    fun `removing someone who is not a member is rejected`() = runTestApp {
        val w = newWorld()
        val res = api(HttpMethod.Delete, "/groups/${w.groupId}/members/${w.outsiderRef}", w.owner)
        assertRejected(res.status)
    }

    @Test
    fun `an outsider cannot remove members`() = runTestApp {
        val w = newWorld()

        val res = api(HttpMethod.Delete, "/groups/${w.groupId}/members/${w.memberRef}", w.outsider)

        assertRejected(res.status)
        assertTrue(myGroupsBody(w.member).contains(w.groupId))
    }

    @Test
    fun `remove member without a token returns 401`() = runTestApp {
        val res = api(HttpMethod.Delete, "/groups/${UUID.randomUUID()}/members/${UUID.randomUUID()}")
        assertEquals(HttpStatusCode.Unauthorized, res.status)
    }

    @Test
    fun `remove member with invalid ids returns 400`() = runTestApp {
        val owner = newUser("owner")
        assertEquals(HttpStatusCode.BadRequest, api(HttpMethod.Delete, "/groups/not-a-uuid/members/${UUID.randomUUID()}", owner).status)
        assertEquals(HttpStatusCode.BadRequest, api(HttpMethod.Delete, "/groups/${UUID.randomUUID()}/members/not-a-uuid", owner).status)
    }

    @Test
    @Ignore("Desired behaviour, not implemented yet: removing a non member should be 404, not 500")
    fun `removing someone who is not a member returns 404`() = runTestApp {
        val w = newWorld()
        val res = api(HttpMethod.Delete, "/groups/${w.groupId}/members/${w.outsiderRef}", w.owner)
        assertEquals(HttpStatusCode.NotFound, res.status)
    }
}