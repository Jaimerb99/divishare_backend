package com.jrb.db

import org.jetbrains.exposed.sql.Table

object ExchangeRatesTable : Table("exchange_rates") {
    val currencyCode = varchar("currency_code", 3)
    val rateToUsd = double("rate_to_usd")
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(currencyCode)
}