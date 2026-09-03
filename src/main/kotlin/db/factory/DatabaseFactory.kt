package com.jrb.db.factory

import com.jrb.db.ExpenseSplitsTable
import com.jrb.db.ExpensesTable
import com.jrb.db.GroupMembersTable
import com.jrb.db.GroupsTable
import com.jrb.db.UsersTable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init() {
        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:postgresql://localhost:5432/divishare"
            driverClassName = "org.postgresql.Driver"
            username = "admin"
            password = "admin"
            maximumPoolSize = 3
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        Database.connect(HikariDataSource(config))

        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                UsersTable,
                GroupsTable,
                GroupMembersTable,
                ExpensesTable,
                ExpenseSplitsTable
            )
        }
    }

    // Make
    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}