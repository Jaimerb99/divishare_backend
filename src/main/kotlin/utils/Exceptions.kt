package com.jrb.utils

enum class DuplicateErrorCode {
    EXPENSE_ID_ALREADY_EXISTS,
    SPLIT_ID_ALREADY_EXISTS,
    GROUP_ID_ALREADY_EXISTS,
    SETTLEMENT_ID_ALREADY_EXISTS,
    USER_ALREADY_IN_GROUP
}

class DuplicateEntityException(val errorCode: DuplicateErrorCode, message: String = errorCode.name) : Exception(message)