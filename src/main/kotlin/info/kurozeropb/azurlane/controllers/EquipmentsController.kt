package info.kurozeropb.azurlane.controllers

import info.kurozeropb.azurlane.Config
import info.kurozeropb.azurlane.structures.*
import info.kurozeropb.azurlane.utils.capitalizeName
import io.javalin.http.Context
import it.skrape.core.Method
import it.skrape.core.Mode
import it.skrape.extract
import it.skrape.selects.htmlDocument
import it.skrape.skrape
import org.jsoup.select.Elements

val rarities = mapOf(
    "" to "Normal",
    "rarity-1" to "Normal",
    "rarity-2" to "Normal",
    "rarity-3" to "Rare",
    "rarity-4" to "Epic",
    "rarity-5" to "Super Rare",
    "rarity-6" to "Ultra Rare"
)

object EquipmentsController {

    fun getEquipments(ctx: Context) {
        var type = ctx.queryParam("type")
        if (type == null) {
            ctx.status(400).json(ErrorResponse(
                statusCode = 400,
                statusMessage = "Bad Request",
                message = "Missing type query param"
            ))
            return
        }

        type = type.toLowerCase()
            .split(" ")
            .map { if (Config.skipCapitalization.contains(it)) it else it.capitalizeName() }
            .joinToString(" ") { if (Config.capitalizeAll.contains(it)) it.toUpperCase() else it }
            .split("-")
            .joinToString("-") { it.capitalize() }

        val equipments = try {
            scrapeEquipmentTypes()
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

        val types = equipments.map { it.name }
        if (!types.contains(type)) {
            ctx.status(400).json(ErrorResponse(
                statusCode = 400,
                statusMessage = "Bad Request",
                message = "Invalid type query param"
            ))
            return
        }

        val equipment = equipments.find { it.name == type }
        val data = if (equipment != null) {
            if (equipment.url != null) {
                scrapeEquipments(equipment.name ?: "", equipment.url)
            } else {
                ctx.status(400).json(ErrorResponse(
                    statusCode = 400,
                    statusMessage = "Bad Request",
                    message = "Invalid type query param"
                ))
                return
            }
        } else {
            ctx.status(400).json(ErrorResponse(
                statusCode = 400,
                statusMessage = "Bad Request",
                message = "Invalid type query param"
            ))
            return
        }

        ctx.status(200).json(EquipmentResponse(
            statusCode = 200,
            statusMessage = "OK",
            message = "The request was successful",
            equipments = data
        ))
        return
    }

    fun getEquipmentTypes(ctx: Context) {
        val data = try {
            scrapeEquipmentTypes()
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

        ctx.status(200).json(EquipmentsResponse(
                statusCode = 200,
                statusMessage = "OK",
                message = "The request was successful",
                equipments = data
        ))
    }

    private fun scrapeEquipments(name: String, equipmentsUrl: String) =
        skrape {
            url = equipmentsUrl
            mode = Mode.DOM
            method = Method.GET
            followRedirects = true
            userAgent = Config.userAgent

            extract {
                htmlDocument {
                    val equipments = mutableListOf<Equipment>()

                    val stupid = listOf(
                        "Auxiliary Equipment",
                        "Anti-Submarine Equipment"
                    )

                    val tabber = getElementsByAttributeValueStarting("id", "tabber-")[0]
                    if (stupid.contains(name)) {
                        var body = tabber.child(1).child(1).child(1).children()
                        body = Elements(body.subList(1, body.size))
                        body.forEach {
                            val eqName = try { it.child(0).child(0).text() } catch (e: Exception) { null }
                            var iconList = try { it.child(1).child(0).child(0).attr("src").split("/") } catch (e: Exception) { null }
                            val icon = if (iconList != null) {
                                iconList = iconList.subList(0, iconList.size - 1)
                                "${Config.baseUrl}${iconList.joinToString("/").replace("/thumb", "")}"
                            } else {
                                null
                            }

                            val rarity = when (it.child(2).text().trim().count { star -> star == '★' }) {
                                1, 2 -> "Normal"
                                3 -> "Rare"
                                4 -> "Epic"
                                5 -> "Super Rare"
                                6 -> "Ultra Rare"
                                else -> null
                            }

                            equipments.add(Equipment(eqName, icon, rarity))
                        }
                    } else {
                        var body = tabber.child(0).child(1).child(0).children()
                        body = Elements(body.subList(2, body.size))
                        body.forEach {
                            val eqName = try { it.child(0).child(0).text() } catch (e: Exception) { null }
                            var iconList2 = try { it.child(1).child(0).child(0).child(0).child(0).child(0).attr("src").split("/") } catch (e: Exception) { null }
                            val icon = if (iconList2 != null) {
                                iconList2 = iconList2.subList(0, iconList2.size - 1)
                                "${Config.baseUrl}${iconList2.joinToString("/").replace("/thumb", "")}"
                            } else {
                                null
                            }
                            val rarityclass = try { it.child(1).child(0).child(0).classNames().last() } catch (e: Exception) { null }
                            val rarity = if (rarityclass != null) rarities[rarityclass] else null
                            equipments.add(Equipment(eqName, icon, rarity))
                        }
                    }

                    equipments
                }
            }
        }

    private fun scrapeEquipmentTypes() =
        skrape {
            url = "${Config.baseUrl}/Equipment_List"
            mode = Mode.DOM
            method = Method.GET
            followRedirects = true
            userAgent = Config.userAgent

            extract {
                htmlDocument {
                    val equipments = mutableListOf<SmallEquipment>()
                    val header = getElementById("Equipment_Lists")
                    val equipmentsList = header.parent().nextElementSibling().children()
                    equipmentsList.forEach {
                        val name = it.child(0).text()
                        val url = "${Config.baseUrl}${it.child(0).attr("href")}"
                        equipments.add(SmallEquipment(name, url))
                    }

                    equipments
                }
            }
        }

}