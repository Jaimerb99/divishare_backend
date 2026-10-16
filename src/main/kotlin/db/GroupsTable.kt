package com.jrb.db

import org.jetbrains.exposed.sql.Table

object GroupsTable : Table("groups") {
    val id = uuid("id").autoGenerate()
    val name = varchar("name", 128)
    val currency = varchar("currency", 3)
    val createdById = reference("created_by_id", UsersTable.id)

    // Sync & Audit metadata
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")
    val deletedAt = long("deleted_at").nullable()

    override val primaryKey = PrimaryKey(id)
}