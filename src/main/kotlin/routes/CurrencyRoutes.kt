package com.jrb.routes

import com.jrb.db.ExchangeRatesTable
import com.jrb.models.CurrencyRatesSyncResponse
import com.jrb.utils.Constants
import com.jrb.utils.Messages
import com.jrb.utils.getLanguage
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.currencyRoutes() {
    route("/currencies") {
        get("/sync") {
            val lang = call.getLanguage()
            val ratesMap = mutableMapOf<String, Double>()
            var lastUpdated = 0L

            transaction {
                ExchangeRatesTable.selectAll().forEach { row ->
                    ratesMap[row[ExchangeRatesTable.currencyCode]] = row[ExchangeRatesTable.rateToUsd]
                    lastUpdated = row[ExchangeRatesTable.updatedAt]
                }
            }

            if (ratesMap.isEmpty()) {
                call.respond(
                    HttpStatusCode.ServiceUnavailable,
                    CurrencyRatesSyncResponse(
                        success = false,
                        message = Messages.get(Constants.Messages.CURRENCY_UNAVAILABLE, lang)
                    )
                )
                return@get
            }

            call.respond(
                HttpStatusCode.OK,
                CurrencyRatesSyncResponse(
                    success = true,
                    lastUpdatedAt = lastUpdated,
                    rates = ratesMap
                )
            )
        }
    }
}