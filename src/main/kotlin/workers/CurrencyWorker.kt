package com.jrb.workers

import com.jrb.db.ExchangeRatesTable
import com.jrb.models.ExternalApiRatesResponse
import com.jrb.utils.AppLogger
import com.jrb.utils.Constants
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object CurrencyWorker {
    private const val TAG = "CurrencyWorker"
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private val jsonParser = Json { ignoreUnknownKeys = true }
    private val httpClient = HttpClient.newHttpClient()

    fun start() {
        scheduler.scheduleAtFixedRate(
            { fetchAndSaveRates() },
            0,
            24,
            TimeUnit.HOURS
        )
    }

    private fun fetchAndSaveRates() {
        try {
            AppLogger.info(TAG, Constants.Logging.LogMessages.CURRENCY_FETCH_START)

            val request = HttpRequest.newBuilder()
                .uri(URI.create(Constants.ExternalApis.EXCHANGE_RATES_URL))
                .GET()
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() == 200) {
                val apiResponse = jsonParser.decodeFromString<ExternalApiRatesResponse>(response.body())
                val currentTime = System.currentTimeMillis()

                transaction {
                    ExchangeRatesTable.deleteAll()

                    ExchangeRatesTable.batchInsert(apiResponse.rates.entries) { entry ->
                        this[ExchangeRatesTable.currencyCode] = entry.key
                        this[ExchangeRatesTable.rateToUsd] = entry.value
                        this[ExchangeRatesTable.updatedAt] = currentTime
                    }
                }
                AppLogger.info(TAG, Constants.Logging.LogMessages.CURRENCY_FETCH_SUCCESS.format(apiResponse.rates.size))
            } else {
                AppLogger.error(TAG, Constants.Logging.LogMessages.CURRENCY_FETCH_FAILED_HTTP.format(response.statusCode()), null)
            }
        } catch (e: Exception) {
            AppLogger.error(TAG, Constants.Logging.LogMessages.CURRENCY_FETCH_EXCEPTION, e)
        }
    }
}