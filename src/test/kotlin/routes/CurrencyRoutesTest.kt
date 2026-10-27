package com.jrb.routes

import com.jrb.db.ExchangeRatesTable
import com.jrb.models.CurrencyRatesSyncResponse
import com.jrb.support.jsonClient
import com.jrb.support.runTestApp
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

class CurrencyRoutesTest {

    @Test
    fun `sync returns 503 when there are no rates`() = runTestApp {
        val res = jsonClient().get("/currencies/sync")

        assertEquals(HttpStatusCode.ServiceUnavailable, res.status)
        assertFalse(res.body<CurrencyRatesSyncResponse>().success)
    }

    @Test
    fun `sync returns stored rates`() = runTestApp {
        // The app must be running (database initialised) before using Exposed directly
        startApplication()
        transaction {
            ExchangeRatesTable.insert {
                it[currencyCode] = "EUR"
                it[rateToUsd] = 0.92
                it[updatedAt] = 1_700_000_000L
            }
        }

        val res = jsonClient().get("/currencies/sync")
        val body = res.body<CurrencyRatesSyncResponse>()

        assertEquals(HttpStatusCode.OK, res.status)
        assertTrue(body.success)
        assertEquals(0.92, body.rates["EUR"])
        assertEquals(1_700_000_000L, body.lastUpdatedAt)
    }
}