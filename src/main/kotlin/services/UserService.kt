package com.jrb.services

import com.jrb.db.RefreshTokensTable
import com.jrb.db.factory.DatabaseFactory.dbQuery
import com.jrb.db.UsersTable
import com.jrb.models.LoginRequest
import com.jrb.models.LoginResult
import com.jrb.models.RegisterRequest
import com.jrb.models.ResetPasswordRequest
import com.jrb.utils.AppLanguage
import com.jrb.utils.AppLogger
import com.jrb.utils.Constants
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import org.mindrot.jbcrypt.BCrypt

class UserService {

    private val tag = Constants.Logging.USER_SERVICE_TAG
    private val emailService = EmailService()

    // ==========================================
    //  AUTHENTICATION (Register & Login)
    // ==========================================

    suspend fun registerUser(request: RegisterRequest): Boolean {
        return try {
            dbQuery {
                val existingUser = UsersTable
                    .select { UsersTable.email eq request.email }
                    .singleOrNull()

                if (existingUser != null) {
                    AppLogger.warn(tag, Constants.Logging.LogMessages.REGISTRATION_EMAIL_EXISTS.format(request.email))
                    return@dbQuery false
                }

                val hashedPassword = BCrypt.hashpw(request.password, BCrypt.gensalt())

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

    suspend fun loginUser(request: LoginRequest): LoginResult {
        return try {
            dbQuery {
                val userRow = UsersTable.select { UsersTable.email eq request.email }.singleOrNull()

                if (userRow == null) {
                    AppLogger.warn(tag, Constants.Logging.LogMessages.LOGIN_USER_NOT_FOUND.format(request.email))
                    return@dbQuery LoginResult.InvalidCredentials
                }

                if (userRow[UsersTable.authProvider] != Constants.Auth.PROVIDER_LOCAL) {
                    AppLogger.warn(tag, Constants.Logging.LogMessages.LOGIN_WRONG_PROVIDER.format(UsersTable.authProvider, request.email))
                    return@dbQuery LoginResult.WrongAuthProvider
                }

                val storedHash = userRow[UsersTable.passwordHash] ?: run {
                    AppLogger.warn(tag, Constants.Logging.LogMessages.LOGIN_NO_HASH.format(request.email))
                    return@dbQuery LoginResult.InvalidCredentials
                }

                val isPasswordValid = BCrypt.checkpw(request.password, storedHash)
                if (!isPasswordValid) {
                    AppLogger.warn(tag, Constants.Logging.LogMessages.LOGIN_INVALID_PASSWORD.format(request.email))
                    return@dbQuery LoginResult.InvalidCredentials
                }

                val deletedAt = userRow[UsersTable.deletedAt]
                if (deletedAt != null) {
                    /* val currentTime = System.currentTimeMillis()
                    val daysPassed = (currentTime - deletedAt) / (1000 * 60 * 60 * 24)

                   if (daysPassed > Constants.User.GRACE_PERIOD_DAYS) {
                        AppLogger.warn(tag, Constants.Logging.LogMessages.LOGIN_USER_PERMANENTLY_DELETED.format(request.email))
                        return@dbQuery LoginResult.PERMANENTLY_DELETED
                    } else {                        // Acivate when future cron job is made to delete accounts
                        */AppLogger.info(tag, Constants.Logging.LogMessages.LOGIN_REACTIVATION_REQUIRED.format(request.email))
                        return@dbQuery LoginResult.DeactivatedReactivateNeeded/*
                    }*/
                } else {
                    val isActive = userRow[UsersTable.isActive]
                    if (!isActive) {
                        AppLogger.warn(tag, Constants.Logging.LogMessages.LOGIN_USER_INACTIVE.format(request.email))
                        return@dbQuery LoginResult.InvalidCredentials
                    }
                }

                val userId = userRow[UsersTable.id].toString()
                LoginResult.Success(userId)
            }
        } catch (e: Exception) {
            AppLogger.error(tag, Constants.Logging.LogMessages.LOGIN_ERROR.format(request.email), e)
            LoginResult.InvalidCredentials
        }
    }

// ==========================================
    // 2. SESSION MANAGEMENT (Tokens)
    // ==========================================

    suspend fun saveRefreshToken(email: String, deviceId: String, token: String) {
        try {
            dbQuery {
                RefreshTokensTable.deleteWhere {
                    (RefreshTokensTable.email eq email) and (RefreshTokensTable.deviceId eq deviceId)
                }
                RefreshTokensTable.insert {
                    it[this.email] = email
                    it[this.deviceId] = deviceId
                    it[this.token] = token
                }
            }
            AppLogger.info(tag, Constants.Logging.LogMessages.REFRESH_TOKEN_SAVED.format(email))
        } catch (e: Exception) {
            AppLogger.error(tag, Constants.Logging.LogMessages.REFRESH_TOKEN_SAVE_ERROR.format(email), e)
        }
    }

    suspend fun validateRefreshToken(email: String, deviceId: String, token: String): Boolean {
        return try {
            dbQuery {
                val row = RefreshTokensTable.select {
                    (RefreshTokensTable.email eq email) and (RefreshTokensTable.deviceId eq deviceId)
                }.singleOrNull()

                val storedToken = row?.get(RefreshTokensTable.token)
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

    // ==========================================
    //  PASSWORD RECOVERY FLOW (PIN)
    // ==========================================

    suspend fun recoverPassword(email: String, lang: AppLanguage): Boolean {
        return try {
            AppLogger.info(tag, Constants.Logging.LogMessages.RECOVERY_REQUESTED.format(email))
            dbQuery {
                val userRow = UsersTable.select { UsersTable.email eq email }.singleOrNull()

                if (userRow == null) {
                    AppLogger.warn(tag, Constants.Logging.LogMessages.RECOVERY_USER_NOT_FOUND.format(email))
                    return@dbQuery false
                }

                if (userRow[UsersTable.authProvider] != Constants.Auth.PROVIDER_LOCAL) {
                    AppLogger.warn(tag, Constants.Logging.LogMessages.RECOVERY_UNSUPPORTED_PROVIDER.format(email))
                    return@dbQuery false
                }

                val numericPin = (Constants.Security.PIN_MIN..Constants.Security.PIN_MAX).random().toString()

                UsersTable.update({ UsersTable.email eq email }) {
                    it[resetPin] = numericPin
                }
                AppLogger.info(tag, Constants.Logging.LogMessages.PIN_GENERATED.format(email))

                val subject = com.jrb.utils.Messages.get(Constants.Messages.EMAIL_RECOVERY_SUBJECT, lang)
                val body = com.jrb.utils.Messages.get(Constants.Messages.EMAIL_RECOVERY_BODY, lang, numericPin)

                emailService.sendEmail(email, subject, body)
            }
        } catch (e: Exception) {
            AppLogger.error(tag, Constants.Logging.LogMessages.RECOVERY_ERROR.format(email), e)
            false
        }
    }

    suspend fun verifyPin(email: String, pin: String): Boolean {
        return try {
            dbQuery {
                val userRow = UsersTable.select { UsersTable.email eq email }.singleOrNull()
                val storedPin = userRow?.get(UsersTable.resetPin)

                val isValid = storedPin != null && storedPin == pin

                if (isValid) {
                    AppLogger.info(tag, Constants.Logging.LogMessages.PIN_VERIFICATION_SUCCESS.format(email))
                } else {
                    AppLogger.warn(tag, Constants.Logging.LogMessages.PIN_VERIFICATION_FAILED.format(email))
                }
                isValid
            }
        } catch (e: Exception) {
            AppLogger.error(tag, Constants.Logging.LogMessages.PIN_VERIFICATION_FAILED.format(email), e)
            false
        }
    }

    suspend fun resetPassword(request: ResetPasswordRequest): Boolean {
        return try {
            dbQuery {
                val userRow = UsersTable.select { UsersTable.email eq request.email }.singleOrNull()
                val storedPin = userRow?.get(UsersTable.resetPin)

                if (storedPin == null || storedPin != request.pin) {
                    AppLogger.warn(tag, Constants.Logging.LogMessages.PASSWORD_RESET_FAILED.format(request.email))
                    return@dbQuery false
                }

                val hashedPassword = BCrypt.hashpw(request.newPassword, BCrypt.gensalt())

                val updatedCount = UsersTable.update({ UsersTable.email eq request.email }) {
                    it[passwordHash] = hashedPassword
                    it[resetPin] = null
                    it[isActive] = true
                    it[deletedAt] = null
                }
                RefreshTokensTable.deleteWhere { RefreshTokensTable.email eq request.email }


                val isSuccess = updatedCount > 0
                if (isSuccess) {
                    AppLogger.info(tag, Constants.Logging.LogMessages.PASSWORD_RESET_SUCCESSFUL.format(request.email))
                }
                isSuccess
            }
        } catch (e: Exception) {
            AppLogger.error(tag, Constants.Logging.LogMessages.PASSWORD_RESET_FAILED.format(request.email), e)
            false
        }
    }

    // ==========================================
    //  ACCOUNT MANAGEMENT
    // ==========================================

    suspend fun deleteUser(email: String): Boolean {
        return try {
            dbQuery {
                val updatedCount = UsersTable.update({ UsersTable.email eq email }) {
                    it[isActive] = false
                    it[deletedAt] = System.currentTimeMillis()
                }

                RefreshTokensTable.deleteWhere { RefreshTokensTable.email eq email }

                val isSuccess = updatedCount > 0
                if (isSuccess) {
                    AppLogger.info(tag, Constants.Logging.LogMessages.DELETE_USER_SUCCESS.format(email))
                } else{
                    AppLogger.warn(tag, Constants.Logging.LogMessages.DELETE_USER_NOT_FOUND.format(email))
                }
                isSuccess
            }
        } catch (e: Exception) {
            AppLogger.error(tag, Constants.Logging.LogMessages.DELETE_USER_ERROR.format(email), e)
            false
        }
    }
}