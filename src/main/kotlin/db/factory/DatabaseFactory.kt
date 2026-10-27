package com.jrb.db.factory

import com.jrb.db.ActivityLogsTable
import com.jrb.db.ExchangeRatesTable
import com.jrb.db.ExpenseSplitsTable
import com.jrb.db.ExpensesTable
import com.jrb.db.GroupMembersTable
import com.jrb.db.GroupsTable
import com.jrb.db.RefreshTokensTable
import com.jrb.db.SettlementsTable
import com.jrb.db.UsersTable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.ApplicationConfig
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {

    private var connectedUrl: String? = null

    // Connects once per JVM. Exposed keeps the "default database" in global static state, so
    // closing and reconnecting it between Ktor test applications leaves stale references behind
    // (errors like "Database ... does not have any transaction manager").
    @Synchronized
    fun init(appConfig: ApplicationConfig) {
        fun setting(key: String, default: String) =
            appConfig.propertyOrNull(key)?.getString() ?: default

        val url = setting("database.url", "jdbc:postgresql://localhost:5432/divishare")

        // Already connected to this database in this JVM (every test after the first one)
        if (connectedUrl == url) return

        check(connectedUrl == null) {
            "DatabaseFactory is already connected to '$connectedUrl' and cannot switch to '$url'"
        }

        val config = HikariConfig().apply {
            jdbcUrl = url
            driverClassName = "org.postgresql.Driver"
            username = setting("database.user", "admin")
            password = setting("database.password", "admin")
            maximumPoolSize = 3
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        val dataSource = HikariDataSource(config)
        Database.connect(dataSource)

        // The pool is released when the JVM exits (production stop or end of the test run)
        Runtime.getRuntime().addShutdownHook(Thread { dataSource.close() })

        transaction {
            SchemaUtils.create(
                UsersTable,
                GroupsTable,
                GroupMembersTable,
                ExpensesTable,
                ExpenseSplitsTable,
                RefreshTokensTable,
                ExchangeRatesTable,
                SettlementsTable,
                ActivityLogsTable,
            )
        }

        connectedUrl = url
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}