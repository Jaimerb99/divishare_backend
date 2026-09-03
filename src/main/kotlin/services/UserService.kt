package com.jrb.services

import com.jrb.db.factory.DatabaseFactory.dbQuery
import com.jrb.db.UsersTable
import com.jrb.models.LoginRequest
import com.jrb.models.RegisterRequest
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import org.mindrot.jbcrypt.BCrypt

class UserService {

    // CREATE: Register a new user safely
    suspend fun registerUser(request: RegisterRequest): Boolean {
        return try {
            dbQuery {
                // 1. Check if the email already exists in the database
                val existingUser = UsersTable
                    .select { UsersTable.email eq request.email }
                    .singleOrNull()

                // If it exists, return false (registration failed)
                if (existingUser != null) {
                    return@dbQuery false
                }

                // 2. Encrypt the password using BCrypt
                val hashedPassword = BCrypt.hashpw(request.password, BCrypt.gensalt())

                // 3. Insert the new user into the database and capture the result
                val insertStatement = UsersTable.insert {
                    it[email] = request.email
                    it[passwordHash] = hashedPassword
                    it[name] = request.name
                    it[authProvider] = "LOCAL" // Registered via email/password
                }

                // 4. Return true only if exactly one row was successfully inserted
                insertStatement.insertedCount > 0
            }
        } catch (e: Exception) {
            // e.g., logger.error("Error inserting user: ${e.message}")
            false
        }
    }

    // READ: Authenticate a user
    suspend fun loginUser(request: LoginRequest): Boolean {
        return try {
            dbQuery {
                // 1. Find the user by email
                val userRow = UsersTable
                    .select { UsersTable.email eq request.email }
                    .singleOrNull()

                // If user doesn't exist, login fails
                if (userRow == null) {
                    return@dbQuery false
                }

                // 2. Extract the stored hash from the database row
                val storedHash = userRow[UsersTable.passwordHash] ?: return@dbQuery false // If for some reason there's no password (e.g., Google login only), fail

                // 3. Verify the plain text password against the hashed one
                BCrypt.checkpw(request.password, storedHash)
            }
        } catch (e: Exception) {
            false
        }
    }

    // UPDATE: Save the refresh token in the database
    suspend fun saveRefreshToken(email: String, token: String) {
        dbQuery {
            UsersTable.update({ UsersTable.email eq email }) {
                it[refreshToken] = token
            }
        }
    }

    // READ: Validate if the refresh token matches the one stored in the database
    suspend fun validateRefreshToken(email: String, token: String): Boolean {
        return try {
            dbQuery {
                val userRow = UsersTable
                    .select { UsersTable.email eq email }
                    .singleOrNull()

                // Extract the stored token and compare it
                val storedToken = userRow?.get(UsersTable.refreshToken)
                storedToken != null && storedToken == token
            }
        } catch (e: Exception) {
            false
        }
    }
}