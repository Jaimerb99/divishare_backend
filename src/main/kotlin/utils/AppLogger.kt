package com.jrb.utils

import org.slf4j.LoggerFactory

object AppLogger {
    // A single, centralized logger for the entire Ktor application
    private val logger = LoggerFactory.getLogger(Constants.Logging.GLOBAL_TAG)

    fun info(tag: String, message: String) {
        logger.info("[$tag] $message")
    }

    fun warn(tag: String, message: String) {
        logger.warn("[$tag] $message")
    }

    fun error(tag: String, message: String, exception: Throwable? = null) {
        if (exception != null) {
            logger.error("[$tag] $message", exception)
        } else {
            logger.error("[$tag] $message")
        }
    }
}