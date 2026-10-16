package com.jrb.services

import com.jrb.db.ActivityLogsTable
import com.jrb.db.factory.DatabaseFactory.dbQuery
import com.jrb.models.ActivityLogDto
import com.jrb.models.PaginatedResponse
import com.jrb.utils.AppLogger
import com.jrb.utils.Constants
import com.jrb.utils.SecurityUtils
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.select
import java.util.UUID
import kotlin.math.ceil

class ActivityLogService {

    private val tag = Constants.Logging.ACTIVITY_LOG_SERVICE_TAG

    suspend fun getActivityLogsForGroup(
        authenticatedUserId: UUID,
        targetGroupId: UUID,
        page: Int,
        limit: Int
    ): PaginatedResponse<ActivityLogDto>? {
        return try {
            dbQuery {
                // 1. Security Check extracted to a single reusable line
                SecurityUtils.ensureUserIsGroupMember(authenticatedUserId, targetGroupId, tag)

                // 2. Calculate pagination metrics
                val offset = ((page - 1) * limit).toLong()

                // Get the total number of logs for this group to calculate pages
                val totalItems = ActivityLogsTable
                    .select { ActivityLogsTable.groupId eq targetGroupId }
                    .count()

                val totalPages = ceil(totalItems.toDouble() / limit).toInt()

                // 3. Fetch ONLY the requested page of logs
                val logsRows = ActivityLogsTable
                    .select { ActivityLogsTable.groupId eq targetGroupId }
                    .orderBy(ActivityLogsTable.createdAt to SortOrder.DESC)
                    .limit(n = limit, offset = offset) // <-- SQL LIMIT and OFFSET applied here

                // 4. Map to DTOs
                val items = logsRows.map { row ->
                    ActivityLogDto(
                        id = row[ActivityLogsTable.id].toString(),
                        groupId = targetGroupId.toString(),
                        userId = row[ActivityLogsTable.userId].toString(),
                        entityType = row[ActivityLogsTable.entityType],
                        entityId = row[ActivityLogsTable.entityId].toString(),
                        action = row[ActivityLogsTable.action],
                        changesSummary = row[ActivityLogsTable.changesSummary],
                        createdAt = row[ActivityLogsTable.createdAt]
                    )
                }

                // 5. Return the wrapped response
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
            AppLogger.error(tag, Constants.Logging.LogMessages.ACTIVITY_LOG_FETCH_ERROR.format(targetGroupId), e)
            null
        }
    }
}