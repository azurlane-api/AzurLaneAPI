package info.kurozeropb.azurlane.controllers

import info.kurozeropb.azurlane.dotenv
import info.kurozeropb.azurlane.structures.ErrorResponse
import io.javalin.http.Context
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

        val body = ctx.body()

        val algorithm = "HmacMD5"
        val key = dotenv["patreon"] ?: ""
        val text = body.toByteArray()

        val keySpec = SecretKeySpec(key.toByteArray(), algorithm)
        val mac = Mac.getInstance(algorithm)
        mac.init(keySpec)
        val sign = mac.doFinal(text).joinToString("") { String.format("%02x", it and 255.toByte()) }

        println("signature: $signature")
        println("digest: $sign")

        if (signature != sign) {
            ctx.status(403).json(ErrorResponse(
                statusCode = 403,
                statusMessage = "Forbidden",
                message = "Invalid credentials sent"
            ))
            return
        }

        /** @TODO
         * - Generate token and save to database
         * - Send webhook with generated token and user info
         */

        when (trigger) {
            "members:pledge:create" -> createToken()
            "members:pledge:delete" -> deleteToken()
        }

        println(trigger)
        println(body)
        ctx.status(200).json(ErrorResponse(
            statusCode = 200,
            statusMessage = "OK",
            message = "Test"
        ))
    }

}
