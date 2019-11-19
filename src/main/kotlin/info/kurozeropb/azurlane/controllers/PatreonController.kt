package info.kurozeropb.azurlane.controllers

import com.google.gson.Gson
import info.kurozeropb.azurlane.dotenv
import info.kurozeropb.azurlane.managers.DatabaseManager
import info.kurozeropb.azurlane.structures.ErrorResponse
import info.kurozeropb.azurlane.structures.Patreon
import info.kurozeropb.azurlane.structures.Patron
import info.kurozeropb.azurlane.structures.User
import io.javalin.http.Context
import org.litote.kmongo.*
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import kotlin.experimental.and
import kotlin.random.Random


object PatreonController {
    private const val tokenLength = 50
    private val charPool : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

    private fun createToken(): String {
        return (1..tokenLength)
            .map { _ -> Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }

    private fun mailToken(email: String, token: String) {
        val prop = Properties()
        prop["mail.smtp.auth"] = true
        prop["mail.transport.protocol"] = "smtp"
        prop["mail.host"] = "mail.kurozeropb.info"
        prop["mail.smtp.starttls.enable"] = "true"
        prop["mail.smtp.host"] = "mail.kurozeropb.info"
        prop["mail.smtp.port"] = "587"
        prop["mail.smtp.ssl.trust"] = "mail.kurozeropb.info"

        val session = Session.getInstance(prop, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication? {
                return PasswordAuthentication(dotenv["mail_address"], dotenv["mail_password"])
            }
        })

        val message = MimeMessage(session)
        message.setFrom(InternetAddress(dotenv["mail_address"]))
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email))
        message.subject = "Azur Lane API authentication token"

//        val msg = "Thank you for donating, here is your api token: $token"
        val msg = """
            <html>
                <head>
                    <title>Azur Lane API authentication token</title>
                </head>
                <body>
                    <p>
                        <b>Thank you for donating, here is your api token</b>
                    </p>
                    <table style="border-collapse: collapse; border: 1px solid grey;">
                        <tr>
                            <th style="border: 1px solid grey; padding: 15px; text-align: left;">Token</th>
                        </tr>
                        <tr>
                            <td style="border: 1px solid grey; padding: 15px; text-align: left;">${token}</td>
                        </tr>
                    </table>
                </body>
            </html>
        """.trimIndent()

        val mimeBodyPart = MimeBodyPart()
        mimeBodyPart.setContent(msg, "text/html")

        val multipart: Multipart = MimeMultipart()
        multipart.addBodyPart(mimeBodyPart)

        message.setContent(multipart)

        Transport.send(message)
    }

    fun handleRequest(ctx: Context) {
        val trigger = ctx.header("X-Patreon-Event")
        val signature = ctx.header("X-Patreon-Signature")

        val body = ctx.body()

        val algorithm = "HmacMD5"
        val key = dotenv["patreon"] ?: ""

        val keySpec = SecretKeySpec(key.toByteArray(), algorithm)
        val mac = Mac.getInstance(algorithm)
        mac.init(keySpec)
        val sign = mac.doFinal(body.toByteArray()).joinToString("") { String.format("%02x", it and 255.toByte()) }

        if (signature != sign) {
            ctx.status(403).json(ErrorResponse(
                statusCode = 403,
                statusMessage = "Forbidden",
                message = "Invalid credentials sent"
            ))
            return
        }

        val response = Gson().fromJson(body, Patreon::class.java);
        val user = if (response.included.count() >= 2) Gson().fromJson(Gson().toJson(response.included[1]), User::class.java) else null

        if (user == null) {
            ctx.status(500).json(ErrorResponse(
                statusCode = 500,
                statusMessage = "Internal Server Error",
                message = "Could not get user from request"
            ))
            return
        }

        when (trigger) {
            "members:create" -> {
                // Add member to database
                val patron = DatabaseManager.users.findOne(Patron::id eq user.id)
                if (patron == null) {
                    DatabaseManager.users.insertOne(Patron(
                        id = user.id,
                        email = user.attributes.email,
                        token = "",
                        enabled = false
                    ))
                }
            }
            "members:update" -> {
                // Check if data changed
                DatabaseManager.users.findOneAndUpdate(Patron::id eq user.id, setValue(Patron::email, user.attributes.email))
            }
            "members:delete" -> {
                // Disable/Delete member (not sure yet)
                DatabaseManager.users.findOneAndDelete(Patron::id eq user.id)
            }
            "members:pledge:create" -> {
                // Create token for member
                val patron = DatabaseManager.users.findOne(Patron::id eq user.id)
                val token = createToken()
                if (patron != null) {
                    DatabaseManager.users.updateOne(Patron::id eq user.id, set(
                        SetTo(Patron::token, token),
                        SetTo(Patron::enabled, true)
                    ))
                } else {
                    DatabaseManager.users.insertOne(Patron(
                        id = user.id,
                        email = user.attributes.email,
                        token = token,
                        enabled = true
                    ))
                }

                try {
                    mailToken(user.attributes.email, token)
                } catch (e: Exception) {
                    e.printStackTrace()
                    ctx.status(500).json(ErrorResponse(
                        statusCode = 500,
                        statusMessage = "Internal Server Error",
                        message = "Failed to send email",
                        error = e.stackTrace.joinToString("\n")
                    ))
                    return
                }
            }
            "members:pledge:update" -> {
                // Not sure yet
            }
            "members:pledge:delete" -> {
                // Delete token
                DatabaseManager.users.findOneAndUpdate(Patron::id eq user.id, set(
                    SetTo(Patron::token, ""),
                    SetTo(Patron::enabled, false)
                ))
            }
        }

        ctx.status(200).json(ErrorResponse(
            statusCode = 200,
            statusMessage = "OK",
            message = "Request handled successfully"
        ))
    }

}
