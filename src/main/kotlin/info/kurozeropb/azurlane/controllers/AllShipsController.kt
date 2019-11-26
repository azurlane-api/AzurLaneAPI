package info.kurozeropb.azurlane.controllers

import info.kurozeropb.azurlane.API
import info.kurozeropb.azurlane.Config
import info.kurozeropb.azurlane.structures.ErrorResponse
import io.javalin.http.Context
import it.skrape.core.Method
import it.skrape.core.Mode
import it.skrape.extract
import it.skrape.selects.findAll
import it.skrape.selects.html5.div
import it.skrape.selects.htmlDocument
import it.skrape.skrape

typealias Ships = List<AllShip>

data class AllShip(
    val name: String?,
    val id: String?,
    val type: String?,
    val nationality: String?,
    val rarity: String?
)

data class AllShipsResponse(
    val statusCode: Int,
    val statusMessage: String,
    val message: String,
    val ships: Ships
)

object AllShipsController {

    fun getAllShips(ctx: Context) {
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

        ctx.status(200).json(AllShipsResponse(
            statusCode = 200,
            statusMessage = "OK",
            message = "The request was successful",
            ships = data
        ))
    }

    private fun scrapeNames() =
        skrape {
            url = "${Config.baseUrl}/List_of_Ships_by_Image"
            mode = Mode.DOM
            method = Method.GET
            followRedirects = true
            userAgent = Config.userAgent

            extract {
                htmlDocument {
                    val ships = mutableListOf<AllShip>()

                    val divs = div {
                        withClass = "shipyardicon"
                        findAll {
                            this
                        }
                    }

                    divs.forEach {
                        if (it.children().size == 4) {
                            val id = try { it.child(0).child(1).text() } catch (e: Exception) { null }
                            val numeric = id?.matches("-?\\d+(\\.\\d+)?".toRegex()) ?: false
                            if (numeric && id?.length == 4) {
                                // Retrofit ship
                                return@forEach
                            } else {
                                val type = try { it.child(0).child(0).child(0).attr("title") } catch (e: Exception) { null }
                                val nationality = try { it.child(2).child(0).child(0).attr("title") } catch (e: Exception) { null }
                                val rarity = try { it.child(2).child(1).child(0).attr("title").replace("Category:", "").replace("ships", "").trim() } catch (e: Exception) { null }
                                val name = try { it.child(3).child(0).text() } catch (e: Exception) { null }
                                ships.add(AllShip(name, id, type, nationality, rarity))
                            }
                        }
                    }

                    ships
                }
            }
        }

}