package com.jrb.services

import com.jrb.db.SettlementsTable
import com.jrb.db.factory.DatabaseFactory.dbQuery
import com.jrb.models.CreateSettlementRequest
import com.jrb.models.PaginatedResponse
import com.jrb.models.SettlementDto
import com.jrb.models.UpdateSettlementRequest
import com.jrb.utils.AppLogger
import com.jrb.utils.AuditUtils
import com.jrb.utils.Constants
import com.jrb.utils.DuplicateErrorCode
import com.jrb.utils.DuplicateEntityException
import com.jrb.utils.SecurityUtils
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import java.util.UUID
import kotlin.math.ceil

class SettlementService {

    private val tag = Constants.Logging.SETTLEMENT_SERVICE_TAG

    suspend fun createSettlement(authenticatedUserId: UUID, request: CreateSettlementRequest): SettlementDto? {
        return try {
            dbQuery {
                val now = System.currentTimeMillis()
                val newSettlementId = UUID.fromString(request.id)
                val targetGroupId = UUID.fromString(request.groupId)
                val payerId = UUID.fromString(request.payerId)
                val receiverId = UUID.fromString(request.receiverId)

                // Verify if settlement ID already exists
                val exists = SettlementsTable.select { SettlementsTable.id eq newSettlementId }.count() > 0
                if (exists) {
                    throw DuplicateEntityException(DuplicateErrorCode.SETTLEMENT_ID_ALREADY_EXISTS)
                }

                SettlementsTable.insert {
                    it[id] = newSettlementId
                    it[groupId] = targetGroupId
                    it[this.payerId] = payerId
                    it[this.receiverId] = receiverId
                    it[amount] = request.amount
                    it[method] = request.method
                    it[createdAt] = request.createdAt
                    it[updatedAt] = now
                }

                AuditUtils.logAction(
                    groupId = targetGroupId,
                    userId = authenticatedUserId,
                    entityType = Constants.Audit.ENTITY_SETTLEMENT,
                    entityId = newSettlementId,
                    action = Constants.Audit.ACTION_CREATE,
                    timestamp = now
                )

                AppLogger.info(tag, Constants.Logging.LogMessages.SETTLEMENT_CREATE_SUCCESS.format(request.id))

                SettlementDto(
                    id = request.id,
                    groupId = request.groupId,
                    payerId = request.payerId,
                    receiverId = request.receiverId,
                    amount = request.amount,
                    method = request.method,
                    createdAt = request.createdAt,
                    updatedAt = now,
                    deletedAt = null
                )
            }
        } catch (e: DuplicateEntityException) {
            throw e
        } catch (e: Exception) {
            AppLogger.error(tag, Constants.Logging.LogMessages.SETTLEMENT_CREATE_ERROR.format(request.id), e)
            null
        }
    }

    suspend fun getSettlementsForGroup(
        authenticatedUserId: UUID,
        targetGroupId: UUID,
        page: Int,
        limit: Int
    ): PaginatedResponse<SettlementDto>? {
        return try {
            dbQuery {
                SecurityUtils.ensureUserIsGroupMember(authenticatedUserId, targetGroupId, tag)

                val offset = ((page - 1) * limit).toLong()

                val totalItems = SettlementsTable.select {
                    (SettlementsTable.groupId eq targetGroupId) and SettlementsTable.deletedAt.isNull()
                }.count()

                val totalPages = ceil(totalItems.toDouble() / limit).toInt()

                if (totalItems == 0L) {
                    return@dbQuery PaginatedResponse(emptyList(), page, limit, 0, 0, false)
                }

                val settlementsRows = SettlementsTable.select {
                    (SettlementsTable.groupId eq targetGroupId) and SettlementsTable.deletedAt.isNull()
                }
                    .orderBy(SettlementsTable.createdAt to SortOrder.DESC)
                    .limit(n = limit, offset = offset)

                val items = settlementsRows.map { row ->
                    SettlementDto(
                        id = row[SettlementsTable.id].toString(),
                        groupId = targetGroupId.toString(),
                        payerId = row[SettlementsTable.payerId].toString(),
                        receiverId = row[SettlementsTable.receiverId].toString(),
                        amount = row[SettlementsTable.amount],
                        method = row[SettlementsTable.method],
                        createdAt = row[SettlementsTable.createdAt],
                        updatedAt = row[SettlementsTable.updatedAt],
                        deletedAt = row[SettlementsTable.deletedAt]
                    )
                }

                PaginatedResponse(
                    items = items,
                    page = page,
                    limit = limit,
                    totalItems = totalItems,
                    totalPages = totalPages,
                    hasMore = page < totalPages
                )
            }
        } catch (e: Exception) {
            AppLogger.error(tag, Constants.Logging.LogMessages.SETTLEMENT_FETCH_ERROR.format(targetGroupId), e)
            null
        }
    }

    suspend fun updateSettlement(
        authenticatedUserId: UUID,
        targetSettlementId: UUID,
        request: UpdateSettlementRequest
    ): SettlementDto? {
        return try {
            dbQuery {
                val now = System.currentTimeMillis()

                val settlementRow = SettlementsTable.select {
                    (SettlementsTable.id eq targetSettlementId) and SettlementsTable.deletedAt.isNull()
                }.singleOrNull()

                if (settlementRow == null) {
                    AppLogger.warn(tag, Constants.Logging.LogMessages.SETTLEMENT_NOT_FOUND_LOG.format(targetSettlementId))
                    return@dbQuery null
                }

                val targetGroupId = settlementRow[SettlementsTable.groupId]

                SecurityUtils.ensureUserIsGroupMember(authenticatedUserId, targetGroupId, tag)

                SettlementsTable.update({ SettlementsTable.id eq targetSettlementId }) {
                    it[amount] = request.amount
                    it[method] = request.method
                    it[updatedAt] = now
                }

                AuditUtils.logAction(
                    groupId = targetGroupId,
                    userId = authenticatedUserId,
                    entityType = Constants.Audit.ENTITY_SETTLEMENT,
                    entityId = targetSettlementId,
                    action = Constants.Audit.ACTION_UPDATE,
                    timestamp = now
                )

                AppLogger.info(tag, Constants.Logging.LogMessages.SETTLEMENT_UPDATE_SUCCESS.format(targetSettlementId))

                SettlementDto(
                    id = targetSettlementId.toString(),
                    groupId = targetGroupId.toString(),
                    payerId = settlementRow[SettlementsTable.payerId].toString(),
                    receiverId = settlementRow[SettlementsTable.receiverId].toString(),
                    amount = request.amount,
                    method = request.method,
                    createdAt = settlementRow[SettlementsTable.createdAt],
                    updatedAt = now,
                    deletedAt = null
                )
            }
        } catch (e: Exception) {
            AppLogger.error(tag, Constants.Logging.LogMessages.SETTLEMENT_UPDATE_ERROR.format(targetSettlementId), e)
            null
        }
    }

    suspend fun deleteSettlement(authenticatedUserId: UUID, targetSettlementId: UUID): Boolean {
        return try {
            dbQuery {
                val now = System.currentTimeMillis()

                val settlementRow = SettlementsTable.select {
                    (SettlementsTable.id eq targetSettlementId) and SettlementsTable.deletedAt.isNull()
                }.singleOrNull()

                if (settlementRow == null) {
                    AppLogger.warn(tag, Constants.Logging.LogMessages.SETTLEMENT_NOT_FOUND_LOG.format(targetSettlementId))
                    return@dbQuery false
                }

                val targetGroupId = settlementRow[SettlementsTable.groupId]

                SecurityUtils.ensureUserIsGroupMember(authenticatedUserId, targetGroupId, tag)

                val updatedRows = SettlementsTable.update({ SettlementsTable.id eq targetSettlementId }) {
                    it[deletedAt] = now
                    it[updatedAt] = now
                }

                if (updatedRows > 0) {
                    AuditUtils.logAction(
                        groupId = targetGroupId,
                        userId = authenticatedUserId,
                        entityType = Constants.Audit.ENTITY_SETTLEMENT,
                        entityId = targetSettlementId,
                        action = Constants.Audit.ACTION_DELETE,
                        timestamp = now
                    )

                    AppLogger.info(tag, Constants.Logging.LogMessages.SETTLEMENT_DELETE_SUCCESS.format(targetSettlementId))
                    true
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            AppLogger.error(tag, Constants.Logging.LogMessages.SETTLEMENT_DELETE_ERROR.format(targetSettlementId), e)
            false
        }
    }
}