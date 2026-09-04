package com.jrb.services

import com.jrb.utils.AppLogger
import com.jrb.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class EmailService {

    private val tag = Constants.Logging.EMAIL_SERVICE_TAG

    suspend fun sendEmail(toEmail: String, subject: String, body: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val props = Properties().apply {
                    put(Constants.Email.SMTP_AUTH_KEY, Constants.Email.TRUE_VALUE)
                    put(Constants.Email.SMTP_STARTTLS_KEY, Constants.Email.TRUE_VALUE)
                    put(Constants.Email.SMTP_HOST_KEY, Constants.Email.HOST)
                    put(Constants.Email.SMTP_PORT_KEY, Constants.Email.PORT)
                }

                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(Constants.Email.SENDER, Constants.Email.PASSWORD)
                    }
                })

                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(Constants.Email.SENDER))
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
                    this.subject = subject
                    setText(body)
                }

                Transport.send(message)
                AppLogger.info(tag, Constants.Logging.LogMessages.EMAIL_SENT_SUCCESS.format(toEmail))
                true
            } catch (e: Exception) {
                AppLogger.error(tag, Constants.Logging.LogMessages.EMAIL_SENT_ERROR.format(toEmail), e)
                false
            }
        }
    }
}