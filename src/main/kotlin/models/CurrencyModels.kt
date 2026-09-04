package com.jrb.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExternalApiRatesResponse(
    @SerialName("rates") val rates: Map<String, Double>
)

@Serializable
data class CurrencyRatesSyncResponse(
    val success: Boolean,
    val message: String? = null,
    val lastUpdatedAt: Long = 0L,
    val rates: Map<String, Double> = emptyMap()
)