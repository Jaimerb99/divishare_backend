package com.jrb.support

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

// Tests read responses as generic JSON on purpose: they only depend on a handful of
// field names instead of on the production response classes.
private val parser = Json { ignoreUnknownKeys = true; isLenient = true }

fun String.toJson(): JsonElement = parser.parseToJsonElement(this)

// Returns a top-level primitive field as a string, or null when it is missing
fun JsonElement.field(name: String): String? =
    ((this as? JsonObject)?.get(name) as? JsonPrimitive)?.contentOrNull

// Returns the id of a created entity or fails with a readable message that includes the body
fun String.requireId(key: String = "id"): String =
    toJson().field(key) ?: error("No '$key' field in response body: $this")

// Number of items of a list response. Works for a root array and for a paginated
// wrapper object (the first array found in the root object is used).
fun String.itemCount(): Int = when (val json = toJson()) {
    is JsonArray -> json.size
    is JsonObject -> json.values.filterIsInstance<JsonArray>().firstOrNull()?.size
        ?: error("No array found in response body: $this")
    else -> error("Unexpected response body: $this")
}

// Items of a list response: a root array, or the first array found in a wrapper object
fun JsonElement.items(): List<JsonElement> = when (this) {
    is JsonArray -> this.toList()
    is JsonObject -> values.filterIsInstance<JsonArray>().firstOrNull()?.toList() ?: emptyList()
    else -> emptyList()
}

// Elements of a named array field (empty when the field is missing or is not an array)
fun JsonElement.arrayField(name: String): List<JsonElement> =
    ((this as? JsonObject)?.get(name) as? JsonArray)?.toList() ?: emptyList()