package com.jrb.db

import org.jetbrains.exposed.sql.Table

object ExpenseSplitsTable : Table("expense_splits") {
    val expenseId = reference("expense_id", ExpensesTable.id)
    val userId = reference("user_id", UsersTable.id)
    val amountOwed = double("amount_owed")

    override val primaryKey = PrimaryKey(expenseId, userId)
}