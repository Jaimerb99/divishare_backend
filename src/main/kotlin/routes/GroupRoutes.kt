package com.jrb.routes

import com.jrb.models.AddGroupMembersRequest
import com.jrb.models.CreateGroupRequest
import com.jrb.services.GroupService
import com.jrb.utils.Constants
import com.jrb.utils.Messages
import com.jrb.utils.getAuthenticatedUserIdOrRespond
import com.jrb.utils.getLanguage
import com.jrb.utils.getUuidPathParameterOrRespond
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.groupRoutes(groupService: GroupService) {

    authenticate(Constants.Security.AUTH_JWT_NAME) {

        route("/groups") {

            post {
                val lang = call.getLanguage()

                val authenticatedUserId = call.getAuthenticatedUserIdOrRespond(lang) ?: return@post

                val request = try {
                    call.receive<CreateGroupRequest>()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, Messages.get(Constants.Messages.INVALID_REQUEST_BODY, lang))
                    return@post
                }

                val createdGroup = groupService.createGroup(authenticatedUserId, request)

                if (createdGroup != null) {
                    call.respond(HttpStatusCode.Created, createdGroup)
                } else {
                    call.respond(HttpStatusCode.InternalServerError, Messages.get(Constants.Messages.GROUP_CREATION_FAILED, lang))
                }
            }

            get {
                val lang = call.getLanguage()

                val authenticatedUserId = call.getAuthenticatedUserIdOrRespond(lang) ?: return@get

                val groups = groupService.getGroupsForUser(authenticatedUserId)

                if (groups != null) {
                    call.respond(HttpStatusCode.OK, groups)
                } else {
                    call.respond(HttpStatusCode.InternalServerError, Messages.get(Constants.Messages.GROUP_FETCH_FAILED, lang))
                }
            }

            route("/{groupId}/members") {

                post {
                    val lang = call.getLanguage()

                    val authenticatedUserId = call.getAuthenticatedUserIdOrRespond(lang) ?: return@post
                    val targetGroupId = call.getUuidPathParameterOrRespond("groupId", Constants.Messages.INVALID_GROUP_ID, lang) ?: return@post

                    val request = try {
                        call.receive<AddGroupMembersRequest>()
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, Messages.get(Constants.Messages.INVALID_REQUEST_BODY, lang))
                        return@post
                    }

                    val addedMembers = groupService.addMembersToGroup(authenticatedUserId, targetGroupId, request)

                    if (addedMembers != null) {
                        call.respond(HttpStatusCode.Created, addedMembers)
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, Messages.get(Constants.Messages.MEMBER_ADD_FAILED, lang))
                    }
                }

                delete("/{memberId}") {
                    val lang = call.getLanguage()

                    val authenticatedUserId = call.getAuthenticatedUserIdOrRespond(lang) ?: return@delete
                    val targetGroupId = call.getUuidPathParameterOrRespond("groupId", Constants.Messages.INVALID_GROUP_ID, lang) ?: return@delete
                    val memberIdToRemove = call.getUuidPathParameterOrRespond("memberId", Constants.Messages.ERROR_BAD_REQUEST, lang) ?: return@delete

                    val success = groupService.removeMemberFromGroup(authenticatedUserId, targetGroupId, memberIdToRemove)

                    if (success) {
                        call.respond(HttpStatusCode.NoContent)
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, Messages.get(Constants.Messages.MEMBER_REMOVE_FAILED, lang))
                    }
                }
            }
        }
    }
}