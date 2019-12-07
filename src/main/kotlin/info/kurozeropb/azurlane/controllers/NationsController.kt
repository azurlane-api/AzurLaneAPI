package info.kurozeropb.azurlane.controllers

import info.kurozeropb.azurlane.API
import info.kurozeropb.azurlane.Config
import info.kurozeropb.azurlane.structures.ErrorResponse
import info.kurozeropb.azurlane.structures.Nation
import info.kurozeropb.azurlane.structures.NationsResponse
import io.javalin.http.Context
import it.skrape.core.Method
import it.skrape.core.Mode
import it.skrape.extract
import it.skrape.selects.htmlDocument
import it.skrape.skrape

object NationsController {

    fun getNations(ctx: Context) {
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
            scrapeNations()
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

        ctx.status(200).json(NationsResponse(
            statusCode = 200,
            statusMessage = "OK",
            message = "The request was successful",
            nations = data
        ))
    }

    private fun scrapeNations() =
        skrape {
            url = "${Config.baseUrl}/Nations"
            mode = Mode.DOM
            method = Method.GET
            followRedirects = true
            userAgent = Config.userAgent

            extract {
                htmlDocument {
                    val nations = mutableListOf<Nation>()

                    val elements = getElementsContainingOwnText("In-game Nation")
                    val basic = elements[0].parent().parent()
                    val collab = elements[1].parent().parent()

                    val basicChildren = basic.children().subList(1, basic.children().size)
                    basicChildren.forEach { basicElements ->
                        val name = try { basicElements.child(0).child(0).text() } catch (e: Exception) { null }
                        var prefix = try { basicElements.child(1).text() } catch (e: Exception) { null }
                        prefix = if (prefix.isNullOrBlank()) null else prefix
                        var iconList = try { basicElements.child(3).child(0).child(0).attr("src").split("/") } catch (e: Exception) { null }

                        val icon = if (iconList != null) {
                            iconList = iconList.subList(0, iconList.size - 1)
                            "${Config.baseUrl}${iconList.joinToString("/").replace("/thumb", "")}"
                        } else {
                            null
                        }

                        nations.add(Nation(name, prefix, icon))
                    }

                    val collabChildren = collab.children().subList(1, collab.children().size)
                    collabChildren.forEach { collabElements ->
                        val name = try { collabElements.child(0).child(0).text() } catch (e: Exception) { null }
                        var prefix = try { collabElements.child(1).text() } catch (e: Exception) { null }
                        prefix = if (prefix.isNullOrBlank()) null else prefix
                        var iconList2 = try { collabElements.child(3).child(0).child(0).attr("src").split("/") } catch (e: Exception) { null }

                        val icon = if (iconList2 != null) {
                            iconList2 = iconList2.subList(0, iconList2.size - 1)
                            "${Config.baseUrl}${iconList2.joinToString("/").replace("/thumb", "")}"
                        } else {
                            null
                        }

                        nations.add(Nation(name, prefix, icon))
                    }

                    nations
                }
            }
        }

}