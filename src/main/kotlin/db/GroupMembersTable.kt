package com.jrb.db

import org.jetbrains.exposed.sql.Table

object GroupMembersTable : Table("group_members") {
    val id = uuid("id").autoGenerate()
    val groupId = reference("group_id", GroupsTable.id)

    // Nullable to support "ghost users" (unregistered participants)
    val userId = reference("user_id", UsersTable.id).nullable()

    // Display name (e.g., "Carlos"). Useful for ghost users or overriding real names locally
    val name = varchar("name", 128)

    // Sync metadata
    val updatedAt = long("updated_at")
    val deletedAt = long("deleted_at").nullable()

    override val primaryKey = PrimaryKey(id, name = "pk_group_members_id")

    init {
        uniqueIndex("uq_group_user", groupId, userId)
    }
}