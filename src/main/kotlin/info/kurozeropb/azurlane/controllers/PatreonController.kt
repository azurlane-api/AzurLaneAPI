package info.kurozeropb.azurlane.controllers

import info.kurozeropb.azurlane.dotenv
import info.kurozeropb.azurlane.structures.ErrorResponse
import io.javalin.http.Context
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.and

object PatreonController {

    private fun createToken() {

    }

    private fun deleteToken() {

    }

    fun handleRequest(ctx: Context) {
        val trigger = ctx.header("X-Patreon-Event")
        val signature = ctx.header("X-Patreon-Signature")

        val algorithm = "HmacMD5"
        val key = dotenv["patreon"] ?: ""
        val text = ctx.bodyAsBytes()

        val keySpec = SecretKeySpec(key.toByteArray(), algorithm)
        val mac = Mac.getInstance(algorithm)
        mac.init(keySpec)
        // val sign = mac.doFinal(text).joinToString("") { String.format("%02x", it and 255.toByte()) }
        val digest = MessageDigest.getInstance("hex").digest(mac.doFinal(text)).joinToString("")

        println("signature: $signature")
        println("digest: $digest")

        if (signature != digest) {
            ctx.status(403).json(ErrorResponse(
                statusCode = 403,
                statusMessage = "Forbidden",
                message = "Invalid credentials sent"
            ))
            return
        }

        println(trigger)
        println(ctx.body())
        ctx.status(200).json(ErrorResponse(
            statusCode = 200,
            statusMessage = "OK",
            message = "Test"
        ))
    }

}
