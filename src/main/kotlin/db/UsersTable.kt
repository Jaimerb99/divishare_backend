package com.jrb.db

import org.jetbrains.exposed.sql.Table

object UsersTable : Table("users") {
    val id = uuid("id").autoGenerate()
    val email = varchar("email", 128)
    val passwordHash = varchar("password_hash", 128).nullable()
    val name = varchar("name", 128)
    val authProvider = varchar("auth_provider", 50)

    val resetPin = varchar("reset_pin", 6).nullable()
    val isActive = bool("is_active").default(true)
    val deletedAt = long("deleted_at").nullable()

    override val primaryKey = PrimaryKey(email)
}

object RefreshTokensTable : Table("refresh_tokens") {
    val email = varchar("email", 128).references(UsersTable.email)
    val deviceId = varchar("device_id", 128)
    val token = varchar("token", 512)

    override val primaryKey = PrimaryKey(email, deviceId)
}
