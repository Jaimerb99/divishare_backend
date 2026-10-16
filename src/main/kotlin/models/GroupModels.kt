package com.jrb.models

import kotlinx.serialization.Serializable

// ==========================================
// DTOs (Data Transfer Objects) - What we send to the app
// ==========================================

@Serializable
data class GroupDto(
    val id: String,
    val name: String,
    val currency: String,
    val createdById: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    // Optional list of members to send everything in a single request
    val members: List<GroupMemberDto> = emptyList()
)

@Serializable
data class GroupMemberDto(
    val id: String,
    val groupId: String,
    val userId: String?, // Null if it's a ghost user
    val name: String,
    val updatedAt: Long,
    val deletedAt: Long? = null
)

// ==========================================
// REQUESTS - What the mobile app sends to us
// ==========================================

@Serializable
data class CreateGroupRequest(
    val id: String, // Mobile generates the UUID for offline-first support
    val name: String,
    val currency: String,
    val createdAt: Long,
    // The members that are added at the time of creation (e.g., the creator + friends)
    val initialMembers: List<CreateMemberRequest>
)

@Serializable
data class CreateMemberRequest(
    val userId: String?,
    val name: String
)

@Serializable
data class AddGroupMembersRequest(
    val members: List<CreateMemberRequest>
)