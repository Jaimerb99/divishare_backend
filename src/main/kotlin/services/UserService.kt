package com.jrb.services

import com.jrb.db.factory.DatabaseFactory.dbQuery
import com.jrb.db.UsersTable
import com.jrb.models.LoginRequest
import com.jrb.models.RegisterRequest
import com.jrb.utils.AppLogger
import com.jrb.utils.Constants
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import org.mindrot.jbcrypt.BCrypt

class UserService {

    private val tag = Constants.Logging.USER_SERVICE_TAG

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
                    AppLogger.warn(tag, Constants.Logging.LogMessages.REGISTRATION_EMAIL_EXISTS.format(request.email))
                    return@dbQuery false
                }

                // 2. Encrypt the password using BCrypt
                val hashedPassword = BCrypt.hashpw(request.password, BCrypt.gensalt())

                // 3. Insert the new user into the database and capture the result
                val insertStatement = UsersTable.insert {
                    it[email] = request.email
                    it[passwordHash] = hashedPassword
                    it[name] = request.name
                    it[authProvider] = Constants.Auth.PROVIDER_LOCAL
                }

                val isSuccess = insertStatement.insertedCount > 0
                if (isSuccess) {
                    AppLogger.info(tag, Constants.Logging.LogMessages.REGISTRATION_SUCCESS.format(request.email))
                }
                isSuccess
            }
        } catch (e: Exception) {
            AppLogger.error(tag, Constants.Logging.LogMessages.REGISTRATION_ERROR.format(request.email), e)
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
                    AppLogger.warn(tag, Constants.Logging.LogMessages.LOGIN_USER_NOT_FOUND.format(request.email))
                    return@dbQuery false
                }

                // 2. Extract the stored hash from the database row
                val storedHash = userRow[UsersTable.passwordHash] ?: run {
                    AppLogger.warn(tag, Constants.Logging.LogMessages.LOGIN_NO_HASH.format(request.email))
                    return@dbQuery false
                }

                // 3. Verify the plain text password against the hashed one
                val isPasswordValid = BCrypt.checkpw(request.password, storedHash)
                if (!isPasswordValid) {
                    AppLogger.warn(tag, Constants.Logging.LogMessages.LOGIN_INVALID_PASSWORD.format(request.email))
                }
                isPasswordValid
            }
        } catch (e: Exception) {
            AppLogger.error(tag, Constants.Logging.LogMessages.LOGIN_ERROR.format(request.email), e)
            false
        }
    }

    // UPDATE: Save the refresh token in the database
    suspend fun saveRefreshToken(email: String, token: String) {
        try {
            dbQuery {
                UsersTable.update({ UsersTable.email eq email }) {
                    it[refreshToken] = token
                }
            }
            AppLogger.info(tag, Constants.Logging.LogMessages.REFRESH_TOKEN_SAVED.format(email))
        } catch (e: Exception) {
            AppLogger.error(tag, Constants.Logging.LogMessages.REFRESH_TOKEN_SAVE_ERROR.format(email), e)
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
                val isValid = storedToken != null && storedToken == token

                if (!isValid) {
                    AppLogger.warn(tag, Constants.Logging.LogMessages.REFRESH_TOKEN_VALIDATION_FAILED.format(email))
                }
                isValid
            }
        } catch (e: Exception) {
            AppLogger.error(tag, Constants.Logging.LogMessages.REFRESH_TOKEN_VALIDATION_ERROR.format(email), e)
            false
        }
    }
}