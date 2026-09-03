package com.jrb.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object ExpensesTable : Table("expenses") {
    val id = uuid("id").autoGenerate()
    val groupId = reference("group_id", GroupsTable.id)
    val paidById = reference("paid_by_id", UsersTable.id)
    val description = varchar("description", 255)
    val category = varchar("category", 50)
    val totalAmount = double("total_amount")
    val date = datetime("date")

    override val primaryKey = PrimaryKey(id)
}