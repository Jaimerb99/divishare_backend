package com.jrb.db

import org.jetbrains.exposed.sql.Table

object ActivityLogsTable : Table("activity_logs") {
    val id = uuid("id").autoGenerate()

    // Relationships
    val groupId = reference("group_id", GroupsTable.id)

    // Who performed the action? (The actual registered User making the API request)
    val userId = reference("user_id", UsersTable.id)

    // What entity was affected: "EXPENSE", "SETTLEMENT", "MEMBER", "GROUP"
    val entityType = varchar("entity_type", 50)

    // The UUID of the specific expense, settlement, or member
    val entityId = uuid("entity_id")

    // The action performed: "CREATE", "UPDATE", "DELETE", "JOIN"
    val action = varchar("action", 50)

    // JSON string with the exact changes.
    // Example: {"field": "amount", "old": 50.0, "new": 80.0} or {"description": "Dinner"}
    val changesSummary = text("changes_summary").nullable()

    // When did this happen
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}