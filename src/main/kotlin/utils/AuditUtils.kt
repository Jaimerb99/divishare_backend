package com.jrb.utils

import com.jrb.db.ActivityLogsTable
import org.jetbrains.exposed.sql.insert
import java.util.UUID

object AuditUtils {

    /**
     * Centralized helper to insert audit logs.
     * MUST be called inside a DatabaseFactory.dbQuery { ... } transaction block.
     */
    fun logAction(
        groupId: UUID,
        userId: UUID,
        entityType: String,
        entityId: UUID,
        action: String,
        timestamp: Long,
        changesSummary: String? = null
    ) {
        ActivityLogsTable.insert {
            it[this.groupId] = groupId
            it[this.userId] = userId
            it[this.entityType] = entityType
            it[this.entityId] = entityId
            it[this.action] = action
            it[this.changesSummary] = changesSummary
            it[this.createdAt] = timestamp
        }
    }
}