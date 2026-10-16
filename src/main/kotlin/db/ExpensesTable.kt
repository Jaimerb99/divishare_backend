package com.jrb.db

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object ExpensesTable : Table("expenses") {
    val id = uuid("id").autoGenerate()

    // Relationships
    val groupId = reference("group_id", GroupsTable.id, onDelete = ReferenceOption.CASCADE)
    val paidById = reference("paid_by_id", GroupMembersTable.id, onDelete = ReferenceOption.RESTRICT)

    // Expense details
    val description = varchar("description", 255)
    val amount = double("amount")
    val category = varchar("category", 50)
    val receiptUrl = varchar("receipt_url", 512).nullable()

    // Sync metadata
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")
    val deletedAt = long("deleted_at").nullable()

    override val primaryKey = PrimaryKey(id)

}