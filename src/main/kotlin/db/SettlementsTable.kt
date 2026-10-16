package com.jrb.db

import org.jetbrains.exposed.sql.Table

object SettlementsTable : Table("settlements") {
    val id = uuid("id").autoGenerate()

    // Relationships
    val groupId = reference("group_id", GroupsTable.id)
    val payerId = reference("payer_id", GroupMembersTable.id)     // The person paying the debt
    val receiverId = reference("receiver_id", GroupMembersTable.id) // The person receiving the money

    val amount = double("amount")

    // E.g., "CASH", "TRANSFER" (Bizum), "WAIVED" (Invitado / Forgiven)
    val method = varchar("method", 50)

    // Sync metadata
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")
    val deletedAt = long("deleted_at").nullable()

    override val primaryKey = PrimaryKey(id)
}