package com.jrb.support

import org.testcontainers.postgresql.PostgreSQLContainer

object TestDb {
    // Started once and shared by the whole test suite
    private val container = PostgreSQLContainer("postgres:16-alpine").apply { start() }

    val jdbcUrl: String get() = container.jdbcUrl
    val user: String get() = container.username
    val password: String get() = container.password
    val port: Int get() = container.getMappedPort(5432)

    // Truncates every table in the public schema so each test starts from a clean state.
    // Table names are discovered dynamically, so nothing is hardcoded and it is safe to
    // call before the app has created its tables (the first test run).
    @Suppress("SqlNoDataSourceInspection", "SqlResolve")
    fun clean() {
        container.createConnection("").use { conn ->
            conn.createStatement().use { statement ->
                val tables = mutableListOf<String>()
                statement.executeQuery(
                    "SELECT tablename FROM pg_tables WHERE schemaname = 'public'"
                ).use { rs ->
                    while (rs.next()) tables += "\"${rs.getString(1)}\""
                }
                if (tables.isNotEmpty()) {
                    statement.execute(
                        "TRUNCATE TABLE ${tables.joinToString()} RESTART IDENTITY CASCADE"
                    )
                }
            }
        }
    }
}