package com.jrb.db

import org.jetbrains.exposed.sql.Table

object ExpenseSplitsTable : Table("expense_splits") {
    val id = uuid("id").autoGenerate()

    // Relationships
    val expenseId = reference("expense_id", ExpensesTable.id)
    val owedById = reference("owed_by_id", GroupMembersTable.id) // Participant who owes this share

    // Exact amount (to avoid missing cents issues) and the % configured by the user
    val amount = double("amount")
    val percentage = double("percentage")

    // Sync metadata
    val updatedAt = long("updated_at")
    val deletedAt = long("deleted_at").nullable()

    override val primaryKey = PrimaryKey(id)
}