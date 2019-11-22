package info.kurozeropb.azurlane.controllers

import info.kurozeropb.azurlane.API
import info.kurozeropb.azurlane.Config
import info.kurozeropb.azurlane.structures.Construction
import info.kurozeropb.azurlane.structures.ErrorResponse
import io.javalin.http.Context
import it.skrape.core.Method
import it.skrape.core.Mode
import it.skrape.extract
import it.skrape.selects.findAll
import it.skrape.selects.html5.div
import it.skrape.selects.htmlDocument
import it.skrape.skrape

data class NamesResponse(
    val statusCode: Int,
    val statusMessage: String,
    val message: String,
    val ships: List<String>
)

object NamesController {

    fun getNames(ctx: Context) {
        val authorized = API.authorize(ctx)
        if (!authorized) {
            ctx.status(401).json(ErrorResponse(
                statusCode = 401,
                statusMessage = "Unauthorized",
                message = "Invalid authorization token, to get a valid token donate to https://patreon.com/Kurozero"
            ))
            return
        }

        val data = try {
            scrapeNames()
        } catch (e: Exception) {
            e.printStackTrace()
            ctx.status(500).json(
                ErrorResponse(
                    statusCode = 500,
                    statusMessage = "Internal Server Error",
                    message = "The server encountered an unexpected condition that prevented it from fulfilling the request",
                    error = e.stackTrace.joinToString("\n")
                )
            )
            return
        }

        ctx.status(200).json(NamesResponse(
            statusCode = 200,
            statusMessage = "OK",
            message = "The request was successful",
            ships = data
        ))
    }

    private fun scrapeNames() =
        skrape {
            url = "${Config.baseUrl}/Building"
            mode = Mode.DOM
            method = Method.GET
            followRedirects = true
            userAgent = Config.userAgent

            extract {
                htmlDocument {

                    val ships = mutableListOf<String>()
                    val items = div {
                        withClass = "azlicon-img"
                        findAll {  this }
                    }

                    items.forEach {
                        ships.add(it.child(0).attr("title"))
                    }

                    ships
                }
            }
        }

}