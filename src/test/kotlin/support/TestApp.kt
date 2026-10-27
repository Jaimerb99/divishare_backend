package com.jrb.support

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.config.mergeWith
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication

import org.jetbrains.exposed.sql.transactions.TransactionManager

fun runTestApp(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
    TestDb.clean()
    environment {
        config = ApplicationConfig("application.yaml").mergeWith(
            MapApplicationConfig(
                "database.url" to TestDb.jdbcUrl,
                "database.user" to TestDb.user,
                "database.password" to TestDb.password,
                "workers.enabled" to "false",
            )
        )
    }
    startApplication()

    // Safety net: never run a test against a database that is not the Testcontainers one
    val url = TransactionManager.defaultDatabase?.url
    check(url != null && url.contains(":${TestDb.port}/")) {
        "SAFETY CHECK FAILED: the app is connected to '$url' instead of the test container"
    }

    block()
}

fun ApplicationTestBuilder.jsonClient(): HttpClient =
    createClient { install(ContentNegotiation) { json() } }