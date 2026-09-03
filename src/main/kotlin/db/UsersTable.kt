package com.jrb.db

import org.jetbrains.exposed.sql.Table

object UsersTable : Table("users") {
    val id = uuid("id").autoGenerate()
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255).nullable()
    val googleId = varchar("google_id", 255).nullable().uniqueIndex()
    val name = varchar("name", 100)
    val avatarUrl = varchar("avatar_url", 500).nullable()
    val authProvider = varchar("auth_provider", 20)
    val refreshToken = varchar("refresh_token", 512).nullable()

    override val primaryKey = PrimaryKey(id)
}
