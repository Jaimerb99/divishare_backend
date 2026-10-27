package com.jrb.support

import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder

// ASSUMPTION: paidById, owedById, payerId and receiverId in expenses and settlements refer to the
// id of a row in the group members table (so that "ghost" members without an account can take part).
// If your services expect the user id instead, set this to false and nothing else has to change.
const val PARTICIPANTS_ARE_MEMBER_ROWS = true

// A ready-made scenario: a group owned by `owner` that `member` has joined, plus an `outsider`
// who is not part of the group. The *Ref values are the ids to use as expense and settlement
// participants (the outsider has no member row, so its ref is its user id).
class World(
    val owner: Session,
    val member: Session,
    val outsider: Session,
    val groupId: String,
    val ownerRef: String,
    val memberRef: String,
    val outsiderRef: String,
)

suspend fun ApplicationTestBuilder.newUser(tag: String): Session =
    registerAndLogin(email = "$tag@test.com", name = tag, deviceId = "device-$tag")

suspend fun ApplicationTestBuilder.createGroup(owner: Session, name: String = "Trip"): String {
    val res = api(HttpMethod.Post, "/groups", owner, Fixtures.createGroupBody(name, owner.userId))
    check(res.status == HttpStatusCode.Created) {
        "Setup failed: POST /groups -> ${res.status} ${res.bodyAsText()}. Check Fixtures.createGroupBody"
    }
    return res.bodyAsText().requireId()
}

suspend fun ApplicationTestBuilder.addMembers(owner: Session, groupId: String, vararg members: Session) {
    val res = api(
        HttpMethod.Post, "/groups/$groupId/members", owner,
        Fixtures.addMembersBody(members.map { it.userId }),
    )
    check(res.status == HttpStatusCode.Created) {
        "Setup failed: POST /groups/{id}/members -> ${res.status} ${res.bodyAsText()}. Check Fixtures.addMembersBody"
    }
}

// Looks up the id of the member row of `userId` in `groupId`, reading GET /groups as `viewer`
suspend fun ApplicationTestBuilder.memberRowId(viewer: Session, groupId: String, userId: String): String {
    val res = api(HttpMethod.Get, "/groups", viewer)
    val body = res.bodyAsText()
    check(res.status == HttpStatusCode.OK) { "Setup failed: GET /groups -> ${res.status} $body" }

    val group = body.toJson().items().firstOrNull { it.field("id") == groupId }
        ?: error("Setup failed: group $groupId not found in GET /groups: $body")
    val member = group.arrayField("members").firstOrNull { it.field("userId") == userId }
        ?: error("Setup failed: user $userId not found in the members of group $groupId. Does GET /groups include the members? Body: $body")
    return member.field("id") ?: error("Setup failed: the member has no 'id' field: $member")
}

// The id to use for `who` as a participant of expenses and settlements of `groupId`
suspend fun ApplicationTestBuilder.participantRef(viewer: Session, groupId: String, who: Session): String =
    if (PARTICIPANTS_ARE_MEMBER_ROWS) memberRowId(viewer, groupId, who.userId) else who.userId

suspend fun ApplicationTestBuilder.newWorld(): World {
    val owner = newUser("owner")
    val member = newUser("member")
    val outsider = newUser("outsider")
    val groupId = createGroup(owner)
    addMembers(owner, groupId, member)
    return World(
        owner = owner,
        member = member,
        outsider = outsider,
        groupId = groupId,
        ownerRef = participantRef(owner, groupId, owner),
        memberRef = participantRef(owner, groupId, member),
        outsiderRef = outsider.userId,
    )
}

// The payer and the listed participants split the amount equally
suspend fun ApplicationTestBuilder.createExpense(
    session: Session,
    groupId: String,
    payer: Session,
    others: List<Session> = emptyList(),
    amount: Double = 30.0,
    description: String = "Dinner",
): String {
    val participants = listOf(payer) + others
    val share = amount / participants.size
    val refs = participants.associateWith { participantRef(session, groupId, it) }
    val res = api(
        HttpMethod.Post, "/expenses", session,
        Fixtures.expenseBody(groupId, refs.getValue(payer), amount, description, refs.values.associateWith { share }),
    )
    check(res.status == HttpStatusCode.Created) {
        "Setup failed: POST /expenses -> ${res.status} ${res.bodyAsText()}. Check Fixtures.expenseBody"
    }
    return res.bodyAsText().requireId()
}

suspend fun ApplicationTestBuilder.createSettlement(
    session: Session,
    groupId: String,
    from: Session,
    to: Session,
    amount: Double = 10.0,
): String {
    val res = api(
        HttpMethod.Post, "/settlements", session,
        Fixtures.settlementBody(
            groupId,
            participantRef(session, groupId, from),
            participantRef(session, groupId, to),
            amount,
        ),
    )
    check(res.status == HttpStatusCode.Created) {
        "Setup failed: POST /settlements -> ${res.status} ${res.bodyAsText()}. Check Fixtures.settlementBody"
    }
    return res.bodyAsText().requireId()
}