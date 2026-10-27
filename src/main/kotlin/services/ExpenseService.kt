package com.jrb.services

import com.jrb.db.ExpenseSplitsTable
import com.jrb.db.ExpensesTable
import com.jrb.db.factory.DatabaseFactory.dbQuery
import com.jrb.models.CreateExpenseRequest
import com.jrb.models.CreateExpenseSplitRequest
import com.jrb.models.ExpenseDto
import com.jrb.models.ExpenseSplitDto
import com.jrb.models.PaginatedResponse
import com.jrb.models.UpdateExpenseRequest
import com.jrb.utils.AppLogger
import com.jrb.utils.AuditUtils
import com.jrb.utils.Constants
import com.jrb.utils.DuplicateErrorCode
import com.jrb.utils.DuplicateEntityException
import com.jrb.utils.SecurityUtils
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import java.util.UUID
import kotlin.math.ceil

class ExpenseService {

    private val tag = Constants.Logging.EXPENSE_SERVICE_TAG

    suspend fun createExpense(authenticatedUserId: UUID, request: CreateExpenseRequest): ExpenseDto? {
        return try {
            dbQuery {
                val now = System.currentTimeMillis()
                val newExpenseId = UUID.fromString(request.id)
                val targetGroupId = UUID.fromString(request.groupId)
                val payerId = UUID.fromString(request.paidById)

                // Verify if the expense ID already exists
                val exists = ExpensesTable.select { ExpensesTable.id eq newExpenseId }.count() > 0
                if (exists) {
                    throw DuplicateEntityException(DuplicateErrorCode.EXPENSE_ID_ALREADY_EXISTS)
                }

                // Extract all split IDs and verify if any of them already exists
                val splitIds = request.splits.map { UUID.fromString(it.id) }
                val existingSplitsCount = ExpenseSplitsTable.select {
                    ExpenseSplitsTable.id inList splitIds
                }.count()

                if (existingSplitsCount > 0) {
                    throw DuplicateEntityException(DuplicateErrorCode.SPLIT_ID_ALREADY_EXISTS)
                }

                ExpensesTable.insert {
                    it[id] = newExpenseId
                    it[groupId] = targetGroupId
                    it[paidById] = payerId
                    it[description] = request.description
                    it[amount] = request.amount
                    it[category] = request.category
                    it[receiptUrl] = request.receiptUrl
                    it[createdAt] = request.createdAt
                    it[updatedAt] = now
                }

                val splitDtos = insertSplitsAndGetDtos(newExpenseId, request.splits, now)

                AuditUtils.logAction(
                    groupId = targetGroupId,
                    userId = authenticatedUserId,
                    entityType = Constants.Audit.ENTITY_EXPENSE,
                    entityId = newExpenseId,
                    action = Constants.Audit.ACTION_CREATE,
                    timestamp = now
                )

                AppLogger.info(tag, Constants.Logging.LogMessages.EXPENSE_CREATE_SUCCESS.format(request.id))

                ExpenseDto(
                    id = request.id,
                    groupId = request.groupId,
                    paidById = request.paidById,
                    description = request.description,
                    amount = request.amount,
                    category = request.category,
                    receiptUrl = request.receiptUrl,
                    createdAt = request.createdAt,
                    updatedAt = now,
                    splits = splitDtos
                )
            }
        } catch (e: DuplicateEntityException) {
            throw e
        } catch (e: Exception) {
            AppLogger.error(tag, Constants.Logging.LogMessages.EXPENSE_CREATE_ERROR.format(request.id), e)
            null
        }
    }

    suspend fun getExpensesForGroup(
        authenticatedUserId: UUID,
        targetGroupId: UUID,
        page: Int,
        limit: Int,
        includeDeleted: Boolean = false,
        since: Long = 0L
    ): PaginatedResponse<ExpenseDto>? {
        return try {
            dbQuery {
                SecurityUtils.ensureUserIsGroupMember(authenticatedUserId, targetGroupId, tag)

                val offset = ((page - 1) * limit).toLong()

                val buildCondition = {
                    var condition = (ExpensesTable.groupId eq targetGroupId) and (ExpensesTable.updatedAt greaterEq since)
                    if (!includeDeleted) {
                        condition = condition and ExpensesTable.deletedAt.isNull()
                    }
                    condition
                }

                val totalItems = ExpensesTable.select { buildCondition() }.count()

                val totalPages = ceil(totalItems.toDouble() / limit).toInt()

                if (totalItems == 0L) {
                    return@dbQuery PaginatedResponse(emptyList(), page, limit, 0, 0, false)
                }

                val expensesRows = ExpensesTable.select { buildCondition() }
                    .orderBy(ExpensesTable.createdAt to SortOrder.DESC)
                    .limit(n = limit, offset = offset)

                val expenseIds = expensesRows.map { it[ExpensesTable.id] }

                val allSplitsRows = ExpenseSplitsTable.select {
                    var splitCondition = (ExpenseSplitsTable.expenseId inList expenseIds) and (ExpenseSplitsTable.updatedAt greaterEq since)
                    if (!includeDeleted) {
                        splitCondition = splitCondition and ExpenseSplitsTable.deletedAt.isNull()
                    }
                    splitCondition
                }
                val splitsByExpense = allSplitsRows.groupBy { it[ExpenseSplitsTable.expenseId] }

                val items = expensesRows.map { expenseRow ->
                    val expenseId = expenseRow[ExpensesTable.id]

                    val splits = splitsByExpense[expenseId]?.map { splitRow ->
                        ExpenseSplitDto(
                            id = splitRow[ExpenseSplitsTable.id].toString(),
                            expenseId = expenseId.toString(),
                            owedById = splitRow[ExpenseSplitsTable.owedById].toString(),
                            amount = splitRow[ExpenseSplitsTable.amount],
                            percentage = splitRow[ExpenseSplitsTable.percentage],
                            updatedAt = splitRow[ExpenseSplitsTable.updatedAt],
                            deletedAt = splitRow[ExpenseSplitsTable.deletedAt]
                        )
                    } ?: emptyList()

                    ExpenseDto(
                        id = expenseId.toString(),
                        groupId = targetGroupId.toString(),
                        paidById = expenseRow[ExpensesTable.paidById].toString(),
                        description = expenseRow[ExpensesTable.description],
                        amount = expenseRow[ExpensesTable.amount],
                        category = expenseRow[ExpensesTable.category],
                        receiptUrl = expenseRow[ExpensesTable.receiptUrl],
                        createdAt = expenseRow[ExpensesTable.createdAt],
                        updatedAt = expenseRow[ExpensesTable.updatedAt],
                        deletedAt = expenseRow[ExpensesTable.deletedAt],
                        splits = splits
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
            AppLogger.error(tag, Constants.Logging.LogMessages.EXPENSE_FETCH_ERROR.format(targetGroupId), e)
            null
        }
    }

    suspend fun updateExpense(
        authenticatedUserId: UUID,
        targetExpenseId: UUID,
        request: UpdateExpenseRequest
    ): ExpenseDto? {
        return try {
            dbQuery {
                val now = System.currentTimeMillis()

                val expenseRow = ExpensesTable.select {
                    (ExpensesTable.id eq targetExpenseId) and ExpensesTable.deletedAt.isNull()
                }.singleOrNull()

                if (expenseRow == null) {
                    AppLogger.warn(tag, Constants.Logging.LogMessages.EXPENSE_NOT_FOUND_LOG.format(targetExpenseId))
                    return@dbQuery null
                }

                val targetGroupId = expenseRow[ExpensesTable.groupId]
                val paidById = expenseRow[ExpensesTable.paidById]

                SecurityUtils.ensureUserIsGroupMember(authenticatedUserId, targetGroupId, tag)

                ExpensesTable.update({ ExpensesTable.id eq targetExpenseId }) {
                    it[description] = request.description
                    it[amount] = request.amount
                    it[category] = request.category
                    it[receiptUrl] = request.receiptUrl
                    it[updatedAt] = now
                }

                ExpenseSplitsTable.update({ ExpenseSplitsTable.expenseId eq targetExpenseId }) {
                    it[deletedAt] = now
                    it[updatedAt] = now
                }

                val splitDtos = insertSplitsAndGetDtos(targetExpenseId, request.splits, now)

                AuditUtils.logAction(
                    groupId = targetGroupId,
                    userId = authenticatedUserId,
                    entityType = Constants.Audit.ENTITY_EXPENSE,
                    entityId = targetExpenseId,
                    action = Constants.Audit.ACTION_UPDATE,
                    timestamp = now
                )

                AppLogger.info(tag, Constants.Logging.LogMessages.EXPENSE_UPDATE_SUCCESS.format(targetExpenseId))

                ExpenseDto(
                    id = targetExpenseId.toString(),
                    groupId = targetGroupId.toString(),
                    paidById = paidById.toString(),
                    description = request.description,
                    amount = request.amount,
                    category = request.category,
                    receiptUrl = request.receiptUrl,
                    createdAt = expenseRow[ExpensesTable.createdAt],
                    updatedAt = now,
                    splits = splitDtos
                )
            }
        } catch (e: Exception) {
            AppLogger.error(tag, Constants.Logging.LogMessages.EXPENSE_UPDATE_ERROR.format(targetExpenseId), e)
            null
        }
    }

    suspend fun deleteExpense(authenticatedUserId: UUID, targetExpenseId: UUID): Boolean {
        return try {
            dbQuery {
                val now = System.currentTimeMillis()

                val expenseRow = ExpensesTable.select {
                    (ExpensesTable.id eq targetExpenseId) and ExpensesTable.deletedAt.isNull()
                }.singleOrNull()

                if (expenseRow == null) {
                    AppLogger.warn(tag, Constants.Logging.LogMessages.EXPENSE_NOT_FOUND_LOG.format(targetExpenseId))
                    return@dbQuery false
                }

                val targetGroupId = expenseRow[ExpensesTable.groupId]

                SecurityUtils.ensureUserIsGroupMember(authenticatedUserId, targetGroupId, tag)

                val updatedRows = ExpensesTable.update({ ExpensesTable.id eq targetExpenseId }) {
                    it[deletedAt] = now
                    it[updatedAt] = now
                }

                if (updatedRows > 0) {
                    ExpenseSplitsTable.update({ ExpenseSplitsTable.expenseId eq targetExpenseId }) {
                        it[deletedAt] = now
                        it[updatedAt] = now
                    }

                    AuditUtils.logAction(
                        groupId = targetGroupId,
                        userId = authenticatedUserId,
                        entityType = Constants.Audit.ENTITY_EXPENSE,
                        entityId = targetExpenseId,
                        action = Constants.Audit.ACTION_DELETE,
                        timestamp = now
                    )

                    AppLogger.info(tag, Constants.Logging.LogMessages.EXPENSE_DELETE_SUCCESS.format(targetExpenseId))
                    true
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            AppLogger.error(tag, Constants.Logging.LogMessages.EXPENSE_DELETE_ERROR.format(targetExpenseId), e)
            false
        }
    }

    private fun insertSplitsAndGetDtos(
        expenseId: UUID,
        splitRequests: List<CreateExpenseSplitRequest>,
        now: Long
    ): List<ExpenseSplitDto> {
        return splitRequests.map { splitReq ->
            val newSplitId = UUID.fromString(splitReq.id)
            val owedById = UUID.fromString(splitReq.owedById)

            ExpenseSplitsTable.insert {
                it[id] = newSplitId
                it[this.expenseId] = expenseId
                it[this.owedById] = owedById
                it[amount] = splitReq.amount
                it[percentage] = splitReq.percentage
                it[updatedAt] = now
            }

            ExpenseSplitDto(
                id = splitReq.id,
                expenseId = expenseId.toString(),
                owedById = splitReq.owedById,
                amount = splitReq.amount,
                percentage = splitReq.percentage,
                updatedAt = now
            )
        }
    }
}