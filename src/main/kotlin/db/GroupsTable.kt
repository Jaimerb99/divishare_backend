package com.jrb.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object GroupsTable : Table("groups") {
    val id = uuid("id").autoGenerate()
    val name = varchar("name", 100)
    val currency = varchar("currency", 10)
    val inviteCode = varchar("invite_code", 20).uniqueIndex().nullable()
    val createdAt = datetime("created_at")
    val createdById = reference("created_by_id", UsersTable.id)

    override val primaryKey = PrimaryKey(id)
}