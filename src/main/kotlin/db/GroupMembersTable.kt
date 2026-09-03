package com.jrb.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object GroupMembersTable : Table("group_members") {
    val groupId = reference("group_id", GroupsTable.id)
    val userId = reference("user_id", UsersTable.id)
    val joinedAt = datetime("joined_at")

    override val primaryKey = PrimaryKey(groupId, userId)
}