package com.jrb.support

import java.util.UUID
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

// Request bodies that match the models in com.jrb.models.
// The mobile app generates ids and timestamps (offline-first), so the fixtures do the same.
object Fixtures {

    private fun uuid() = UUID.randomUUID().toString()
    private fun now() = System.currentTimeMillis()
    private fun displayName(userId: String) = "user-${userId.take(6)}"

    // ------------------------------------------------------------------ users

    fun registerBody(name: String, email: String, password: String): String = buildJsonObject {
        put("name", name)
        put("email", email)
        put("password", password)
    }.toString()

    fun loginBody(email: String, password: String, deviceId: String = "device-1"): String = buildJsonObject {
        put("email", email)
        put("password", password)
        put("deviceId", deviceId)
    }.toString()

    fun refreshBody(email: String, deviceId: String, refreshToken: String): String = buildJsonObject {
        put("email", email)
        put("deviceId", deviceId)
        put("refreshToken", refreshToken)
    }.toString()

    fun recoverBody(email: String): String = buildJsonObject { put("email", email) }.toString()

    fun verifyPinBody(email: String, pin: String): String = buildJsonObject {
        put("email", email)
        put("pin", pin)
    }.toString()

    fun resetPasswordBody(email: String, pin: String, newPassword: String): String = buildJsonObject {
        put("email", email)
        put("pin", pin)
        put("newPassword", newPassword)
    }.toString()

    // ----------------------------------------------------------------- groups

    // The creator is sent inside initialMembers, as the mobile app does
    fun createGroupBody(name: String, creatorUserId: String, otherMemberUserIds: List<String> = emptyList()): String =
        buildJsonObject {
            put("id", uuid())
            put("name", name)
            put("currency", "EUR")
            put("createdAt", now())
            putJsonArray("initialMembers") {
                (listOf(creatorUserId) + otherMemberUserIds).forEach { userId ->
                    addJsonObject {
                        put("userId", userId)
                        put("name", displayName(userId))
                    }
                }
            }
        }.toString()

    fun addMembersBody(memberUserIds: List<String>): String = buildJsonObject {
        putJsonArray("members") {
            memberUserIds.forEach { userId ->
                addJsonObject {
                    put("userId", userId)
                    put("name", displayName(userId))
                }
            }
        }
    }.toString()

    // A "ghost" member has no user account (userId is null)
    fun addGhostMembersBody(vararg names: String): String = buildJsonObject {
        putJsonArray("members") {
            names.forEach { name ->
                addJsonObject {
                    put("userId", null as String?)
                    put("name", name)
                }
            }
        }
    }.toString()

    // --------------------------------------------------------------- expenses

    // Pass a groupId to build a CreateExpenseRequest, or null to build an UpdateExpenseRequest
    // (an update has no id, groupId, paidById or createdAt, so paidBy is ignored in that case).
    // The ids in paidBy and in the splits map are whatever the services expect for a participant
    // (see PARTICIPANTS_ARE_MEMBER_ROWS in Scenarios.kt).
    fun expenseBody(
        groupId: String?,
        paidBy: String,
        amount: Double,
        description: String,
        splits: Map<String, Double>,
        category: String = "FOOD",
    ): String = buildJsonObject {
        if (groupId != null) {
            put("id", uuid())
            put("groupId", groupId)
            put("paidById", paidBy)
            put("createdAt", now())
        }
        put("description", description)
        put("amount", amount)
        put("category", category)
        putJsonArray("splits") {
            splits.forEach { (owedById, share) ->
                addJsonObject {
                    put("id", uuid())
                    put("owedById", owedById)
                    put("amount", share)
                    put("percentage", if (amount == 0.0) 0.0 else share / amount * 100.0)
                }
            }
        }
    }.toString()

    // ------------------------------------------------------------ settlements

    // Pass a groupId to build a CreateSettlementRequest, or null to build an UpdateSettlementRequest
    // (an update only carries amount and method, so payer and receiver are ignored in that case)
    fun settlementBody(
        groupId: String?,
        payerId: String,
        receiverId: String,
        amount: Double,
        method: String = "CASH",
    ): String = buildJsonObject {
        if (groupId != null) {
            put("id", uuid())
            put("groupId", groupId)
            put("payerId", payerId)
            put("receiverId", receiverId)
            put("createdAt", now())
        }
        put("amount", amount)
        put("method", method)
    }.toString()

    // ------------------------------------------------------------------ media

    fun uploadReceiptBody(base64Image: String, folder: String, expenseId: String): String = buildJsonObject {
        put("base64Image", base64Image)
        put("folder", folder)
        put("expenseId", expenseId)
    }.toString()

    fun uploadAvatarBody(base64Image: String): String = buildJsonObject {
        put("base64Image", base64Image)
    }.toString()
}