package info.kurozeropb.azurlane.controllers

import info.kurozeropb.azurlane.API
import info.kurozeropb.azurlane.Config
import info.kurozeropb.azurlane.structures.Construction
import info.kurozeropb.azurlane.structures.ConstructionResponse
import info.kurozeropb.azurlane.structures.ErrorResponse
import io.javalin.http.Context
import it.skrape.core.Method
import it.skrape.core.Mode
import it.skrape.extract
import it.skrape.selects.htmlDocument
import it.skrape.skrape

object ConstructionController {

    fun getBuildInfo(ctx: Context) {
        val authorized = API.authorize(ctx)
        if (!authorized) {
            ctx.status(401).json(ErrorResponse(
                statusCode = 401,
                statusMessage = "Unauthorized",
                message = "Invalid authorization token, to get a valid token donate to https://patreon.com/Kurozero"
            ))
            return
        }

        val time = ctx.queryParam("time")
        if (time.isNullOrBlank()) {
            ctx.status(400).json(ErrorResponse(
                statusCode = 400,
                statusMessage = "Bad Request",
                message = "Missing or invalid time query param"
            ))
            return
        }

        val data = try {
            scrapeHtmlForBuildInfo(time)
        } catch (e: Exception) {
            e.printStackTrace()
            ctx.status(500).json(ErrorResponse(
                statusCode = 500,
                statusMessage = "Internal Server Error",
                message = "The server encountered an unexpected condition that prevented it from fulfilling the request",
                error = e.stackTrace.joinToString("\n")
            ))
            return
        }

        ctx.status(200).json(ConstructionResponse(
            statusCode = 200,
            statusMessage = "OK",
            message = "The request was successful",
            construction = data
        ))
    }

    private fun scrapeHtmlForBuildInfo(time: String) =
        skrape {
            url = "${Config.baseUrl}/Building"
            mode = Mode.DOM
            method = Method.GET
            followRedirects = true
            userAgent = Config.userAgent

            extract {
                htmlDocument {
                    val parent = getElementById(time)
                    val t = parent.child(0).text()
                    val children = parent.child(1).child(0).children()

                    val ships = mutableListOf<String>()
                    children.forEach { el ->
                        ships.add(if (el.child(0).children().count() >= 3) el.child(0).child(2).text() else el.child(0).child(1).text())
                    }

                    Construction(
                        time = t,
                        wikiUrl = "${Config.baseUrl}/Building",
                        ships = ships
                    )
                }
            }
        }

}