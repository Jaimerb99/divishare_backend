package com.jrb.utils

import com.jrb.db.GroupMembersTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import java.util.UUID

object SecurityUtils {

    /**
     * Checks if a user is an active member of a group.
     * MUST be called inside a DatabaseFactory.dbQuery { ... } transaction block.
     * Throws SecurityException if the user is not a member.
     */
    fun ensureUserIsGroupMember(userId: UUID, groupId: UUID, loggerTag: String) {
        val isMember = GroupMembersTable.select {
            (GroupMembersTable.groupId eq groupId) and
                    (GroupMembersTable.userId eq userId) and
                    GroupMembersTable.deletedAt.isNull()
        }.count() > 0

        if (!isMember) {
            AppLogger.warn(loggerTag, Constants.Logging.LogMessages.UNAUTHORIZED_GROUP_ACCESS.format(userId, groupId))
            throw SecurityException(Constants.Exceptions.UNAUTHORIZED_GROUP_ACCESS)
        }
    }
}