package com.jrb.models

import kotlinx.serialization.Serializable

/**
 * A generic wrapper for paginated API responses.
 * @param T The type of the items in the list (e.g., ActivityLogDto, ExpenseDto)
 */
@Serializable
data class PaginatedResponse<T>(
    val items: List<T>,
    val page: Int,
    val limit: Int,
    val totalItems: Long,
    val totalPages: Int,
    val hasMore: Boolean
)