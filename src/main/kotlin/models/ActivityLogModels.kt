package com.jrb.models

import kotlinx.serialization.Serializable

@Serializable
data class ActivityLogDto(
    val id: String,
    val groupId: String,
    val userId: String,
    val entityType: String,
    val entityId: String,
    val action: String,
    val changesSummary: String?,
    val createdAt: Long
)