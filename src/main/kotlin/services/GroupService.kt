package com.jrb.services

import com.jrb.db.GroupMembersTable
import com.jrb.db.GroupsTable
import com.jrb.db.UsersTable
import com.jrb.db.factory.DatabaseFactory.dbQuery
import com.jrb.models.AddGroupMembersRequest
import com.jrb.models.CreateGroupRequest
import com.jrb.models.CreateMemberRequest
import com.jrb.models.GroupDto
import com.jrb.models.GroupMemberDto
import com.jrb.utils.AppLogger
import com.jrb.utils.AuditUtils
import com.jrb.utils.Constants
import com.jrb.utils.DuplicateErrorCode
import com.jrb.utils.DuplicateEntityException
import com.jrb.utils.SecurityUtils
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import java.util.UUID

class GroupService {

    private val tag = Constants.Logging.GROUP_SERVICE_TAG

    suspend fun createGroup(authenticatedUserId: UUID, request: CreateGroupRequest): GroupDto? {
        return try {
            dbQuery {
                val now = System.currentTimeMillis()
                val newGroupId = UUID.fromString(request.id)

                //SecurityUtils.ensureUserIsGroupMember(authenticatedUserId, newGroupId, tag)

                // Verify if group ID already exists
                val exists = GroupsTable.select { GroupsTable.id eq newGroupId }.count() > 0
                if (exists) {
                    throw DuplicateEntityException(DuplicateErrorCode.GROUP_ID_ALREADY_EXISTS)
                }

                GroupsTable.insert {
                    it[id] = newGroupId
                    it[name] = request.name
                    it[currency] = request.currency
                    it[createdById] = authenticatedUserId
                    it[createdAt] = request.createdAt
                    it[updatedAt] = now
                }

                val memberDtos = request.initialMembers.map { memberReq ->
                    insertMemberAndGetDto(newGroupId, memberReq, now)
                }

                AuditUtils.logAction(
                    groupId = newGroupId,
                    userId = authenticatedUserId,
                    entityType = Constants.Audit.ENTITY_GROUP,
                    entityId = newGroupId,
                    action = Constants.Audit.ACTION_CREATE,
                    timestamp = now
                )

                AppLogger.info(tag, Constants.Logging.LogMessages.GROUP_CREATE_SUCCESS.format(request.id))

                GroupDto(
                    id = request.id,
                    name = request.name,
                    currency = request.currency,
                    createdById = authenticatedUserId.toString(),
                    createdAt = request.createdAt,
                    updatedAt = now,
                    members = memberDtos
                )
            }
        } catch (e: DuplicateEntityException) {
            throw e
        } catch (e: Exception) {
            AppLogger.error(tag, Constants.Logging.LogMessages.GROUP_CREATE_ERROR.format(request.id), e)
            null
        }
    }

    suspend fun getGroupsForUser(
        authenticatedUserId: UUID,
        includeDeleted: Boolean = false,
        since: Long = 0L
    ): List<GroupDto>? {
        return try {
            dbQuery {
                val userGroupIds = GroupMembersTable
                    .select {
                        var condition = (GroupMembersTable.userId eq authenticatedUserId)
                        if (!includeDeleted) {
                            condition = condition and GroupMembersTable.deletedAt.isNull()
                        }
                        condition
                    }
                    .map { it[GroupMembersTable.groupId] }
                    .distinct()

                if (userGroupIds.isEmpty()) {
                    return@dbQuery emptyList()
                }

                val groupsRows = GroupsTable
                    .select {
                        var condition = (GroupsTable.id inList userGroupIds) and (GroupsTable.updatedAt greaterEq since)
                        if (!includeDeleted) {
                            condition = condition and GroupsTable.deletedAt.isNull()
                        }
                        condition
                    }

                val allMembersRows = GroupMembersTable
                    .select {
                        var condition = (GroupMembersTable.groupId inList userGroupIds) and (GroupMembersTable.updatedAt greaterEq since)
                        if (!includeDeleted) {
                            condition = condition and GroupMembersTable.deletedAt.isNull()
                        }
                        condition
                    }

                val membersByGroup = allMembersRows.groupBy { it[GroupMembersTable.groupId] }

                groupsRows.map { groupRow ->
                    val groupId = groupRow[GroupsTable.id]

                    val members = membersByGroup[groupId]?.map { memberRow ->
                        GroupMemberDto(
                            id = memberRow[GroupMembersTable.id].toString(),
                            groupId = groupId.toString(),
                            userId = memberRow[GroupMembersTable.userId]?.toString(),
                            name = memberRow[GroupMembersTable.name],
                            updatedAt = memberRow[GroupMembersTable.updatedAt],
                            deletedAt = memberRow[GroupMembersTable.deletedAt]
                        )
                    } ?: emptyList()

                    GroupDto(
                        id = groupId.toString(),
                        name = groupRow[GroupsTable.name],
                        currency = groupRow[GroupsTable.currency],
                        createdById = groupRow[GroupsTable.createdById].toString(),
                        createdAt = groupRow[GroupsTable.createdAt],
                        updatedAt = groupRow[GroupsTable.updatedAt],
                        deletedAt = groupRow[GroupsTable.deletedAt],
                        members = members
                    )
                }
            }
        } catch (e: Exception) {
            AppLogger.error(tag, Constants.Logging.LogMessages.GROUP_FETCH_ERROR.format(authenticatedUserId), e)
            null
        }
    }

    suspend fun addMembersToGroup(authenticatedUserId: UUID, targetGroupId: UUID, request: AddGroupMembersRequest): List<GroupMemberDto>? {
        return try {
            dbQuery {
                SecurityUtils.ensureUserIsGroupMember(authenticatedUserId, targetGroupId, tag)
                val now = System.currentTimeMillis()

                val newMembers = request.members.map { memberReq ->
                    // Check if the registered user is already a member of the group
                    if (memberReq.userId != null) {
                        val userUuid = UUID.fromString(memberReq.userId)
                        val userAlreadyInGroup = GroupMembersTable.select {
                            (GroupMembersTable.groupId eq targetGroupId) and
                                    (GroupMembersTable.userId eq userUuid) and
                                    GroupMembersTable.deletedAt.isNull()
                        }.count() > 0

                        if (userAlreadyInGroup) {
                            throw DuplicateEntityException(DuplicateErrorCode.USER_ALREADY_IN_GROUP)
                        }
                    }

                    // Backend generates the unique ID for the new member
                    val memberId = UUID.randomUUID()

                    val dto = insertMemberAndGetDto(targetGroupId, memberReq, now, memberId)

                    AuditUtils.logAction(
                        groupId = targetGroupId,
                        userId = authenticatedUserId,
                        entityType = Constants.Audit.ENTITY_MEMBER,
                        entityId = memberId,
                        action = Constants.Audit.ACTION_JOIN,
                        timestamp = now
                    )

                    dto
                }

                AppLogger.info(tag, Constants.Logging.LogMessages.MEMBER_ADD_SUCCESS.format(targetGroupId))
                newMembers
            }
        } catch (e: DuplicateEntityException) {
            throw e
        } catch (e: Exception) {
            AppLogger.error(tag, Constants.Logging.LogMessages.MEMBER_ADD_ERROR.format(targetGroupId), e)
            null
        }
    }

    suspend fun removeMemberFromGroup(authenticatedUserId: UUID, targetGroupId: UUID, memberIdToRemove: UUID): Boolean {
        return try {
            dbQuery {
                SecurityUtils.ensureUserIsGroupMember(authenticatedUserId, targetGroupId, tag)
                val now = System.currentTimeMillis()

                val updatedRows = GroupMembersTable.update({
                    (GroupMembersTable.id eq memberIdToRemove) and (GroupMembersTable.groupId eq targetGroupId)
                }) {
                    it[deletedAt] = now
                    it[updatedAt] = now
                }

                if (updatedRows > 0) {
                    AuditUtils.logAction(
                        groupId = targetGroupId,
                        userId = authenticatedUserId,
                        entityType = Constants.Audit.ENTITY_MEMBER,
                        entityId = memberIdToRemove,
                        action = Constants.Audit.ACTION_LEAVE,
                        timestamp = now
                    )
                    AppLogger.info(tag, Constants.Logging.LogMessages.MEMBER_REMOVE_SUCCESS.format(memberIdToRemove, targetGroupId))
                    true
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            AppLogger.error(tag, Constants.Logging.LogMessages.MEMBER_REMOVE_ERROR.format(memberIdToRemove, targetGroupId), e)
            false
        }
    }

    private fun insertMemberAndGetDto(
        groupId: UUID,
        memberReq: CreateMemberRequest,
        now: Long,
        memberId: UUID = UUID.randomUUID()
    ): GroupMemberDto {
        val userUuid = memberReq.userId?.let { uid ->
            try {
                UUID.fromString(uid)
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("invalid_user_id_format")
            }
        }

        if (userUuid != null) {
            val userExists = UsersTable.select { UsersTable.id eq userUuid }.count() > 0
            if (!userExists) {
                throw IllegalArgumentException("user_not_found")
            }
        }

        GroupMembersTable.insert {
            it[id] = memberId
            it[this.groupId] = groupId
            it[userId] = userUuid
            it[name] = memberReq.name
            it[updatedAt] = now
        }

        return GroupMemberDto(
            id = memberId.toString(),
            groupId = groupId.toString(),
            userId = memberReq.userId,
            name = memberReq.name,
            updatedAt = now
        )
    }
}