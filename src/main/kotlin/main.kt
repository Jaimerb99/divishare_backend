package com.jrb

import com.jrb.db.factory.DatabaseFactory
import com.jrb.plugins.configureRouting
import com.jrb.plugins.configureSecurity
import com.jrb.plugins.configureSerialization
import com.jrb.workers.CurrencyWorker
import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    // Init database
    DatabaseFactory.init()

    // Workers
    CurrencyWorker.start()

    // Load the rest of the conf ktor generated
    configureSerialization()
    configureSecurity()
    configureRouting()
}