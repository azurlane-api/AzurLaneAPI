package info.kurozeropb.azurlane.controllers

import info.kurozeropb.azurlane.Config
import info.kurozeropb.azurlane.structures.*
import io.javalin.http.Context
import it.skrape.core.Method
import it.skrape.core.Mode
import it.skrape.exceptions.ElementNotFoundException
import it.skrape.extract
import it.skrape.selects.findByIndex
import it.skrape.selects.findFirst
import it.skrape.selects.html5.*
import it.skrape.selects.htmlDocument
import it.skrape.skrape
import java.lang.Exception

object ShipController {

    fun getShip(ctx: Context) {
        var name = ctx.queryParam("name")
        if (name == null) {
            var id = ctx.queryParam("id")
            if (id == null) {
                ctx.status(400).json(ErrorResponse(
                    statusCode = 400,
                    statusMessage = "Bad Request",
                    message = "Missing or invalid name/id query param"
                ))
                return
            }
            id = id.capitalize()
            name = getShipNameFromId(id)
        }

        if (name.isNullOrBlank()) {
            ctx.status(400).json(ErrorResponse(
                statusCode = 400,
                statusMessage = "Bad Request",
                message = "Missing or invalid name/id query param"
            ))
            return
        }

        name = name.toLowerCase()
            .replace(Regex(" ", setOf(RegexOption.IGNORE_CASE)), "_")
            .split("_")
            .map { if (Config.skipCapitalization.contains(it)) it else it.capitalize() }
            .joinToString("_") { if (Config.capitalizeAll.contains(it)) it.toUpperCase() else it }

        if (name.startsWith("Jeanne")) {
            name = "Jeanne d'Arc"
        }

        val isValid = try {
            validateShip(name.split("_").joinToString(" "))
        } catch(e: Exception) {
            e.printStackTrace()
            ctx.status(500).json(ErrorResponse(
                statusCode = 500,
                statusMessage = "Internal Server Error",
                message = "The server encountered an unexpected condition that prevented it from fulfilling the request",
                error = e.stackTrace.joinToString("\n")
            ))
            return
        }

        if (!isValid) {
            ctx.status(400).json(ErrorResponse(
                statusCode = 400,
                statusMessage = "Bad Request",
                message = "Invalid ship name"
            ))
            return
        }

        val data = try {
            findShip(name)
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

        ctx.status(200).json(ShipResponse(
            statusCode = 200,
            statusMessage = "OK",
            message = "The request was successful",
            ship = data
        ))
    }

    private fun getShipNameFromId(id: String): String =
        skrape {
            url = "${Config.baseUrl}/List_of_Ships"
            mode = Mode.DOM
            method = Method.GET
            followRedirects = true
            userAgent = Config.userAgent

            extract {
                htmlDocument {
                    td {
                        withAttribute = Pair("data-sort-value", id)
                        findFirst {
                            child(0).attr("title")
                        }
                    }
                }
            }
        }

    private fun validateShip(name: String): Boolean =
        skrape {
            url = "${Config.baseUrl}/List_of_Ships"
            mode = Mode.DOM
            method = Method.GET
            followRedirects = true
            userAgent = Config.userAgent

            extract {
                htmlDocument {
                    val elements = getElementsContainingOwnText(name)
                    if (elements.isNullOrEmpty()) {
                        false
                    } else {
                        val element = elements.first()
                        val index = element.parent().parent().children().indexOf(element.parent())
                        val table = element.parent().parent().parent()
                        val title = table.child(0).child(if (index > 3) index + 1 else index)
                        if (title.text() != "Name") {
                            false
                        } else {
                            element.text().trim() == name && element.attr("title").trim() == name
                        }
                    }
                }
            }
        }

    private fun findShip(ship: String): Ship =
        skrape {
            url = "${Config.baseUrl}/$ship"
            mode = Mode.DOM
            method = Method.GET
            followRedirects = true
            userAgent = Config.userAgent

            extract {
                htmlDocument {
                    val shipId = getElementsContainingOwnText("ID").first()?.nextElementSibling()?.text()?.trim()

                    val skins: MutableList<Skin> = mutableListOf()
                    val chibis = getElementsByAttributeValueContaining("alt", "Chibi")
                    val list = getElementsByClass("tabbertab")
                    list.forEachIndexed { index, element ->
                        val child = element.children().find { c -> c.className().contains("adaptiveratioimg") }
                        if (child != null) {
                            var skinChibi: String? = null
                            var chibi = if (chibis.isNullOrEmpty()) null else chibis[index].attr("src").split("/")
                            if (chibi.isNullOrEmpty().not()) {
                                chibi = chibi!!.dropLast(1)
                                skinChibi = Config.baseUrl + chibi.joinToString("/").replace("/thumb", "")
                            }

                            skins.add(Skin(
                                title = element.attr("title"),
                                image = Config.baseUrl + child.child(0)?.child(0)?.attr("src"),
                                chibi = skinChibi
                            ))
                        }
                    }

                    var short = getElementsByAttributeValue("style", "border-style:solid; border-width:1px 1px 0px 1px; border-color:#a2a9b1; width:100%; background-color:#eaecf0; text-align:center; font-weight:bold").text()
                    Config.nations.forEach {
                        if (short.contains(it)) {
                            short = it
                        }
                    }

                    val baseStats: MutableList<Stat> = mutableListOf()
                    val bsElements = getElementsByAttributeValue("title", "Base Stats")
                    val bsBody = bsElements[0].child(1).child(0)
                    val bsFiltered = bsBody.children().filter { el -> el.tagName() == "tr" }
                    bsFiltered.forEach { el ->
                        val tds = el.children().filter { e -> e.tagName() === "td" }
                        val ths = el.children().filter { e -> e.tagName() === "th" }
                        tds.forEachIndexed { i, _ ->
                            val value = tds[i].text().replace("\n", "")
                            val name = if (ths[i].child(0).attr("alt").isNullOrBlank()) null else ths[i].child(0).attr("alt")
                            val image = "${Config.baseUrl}${ths[i].child(0).attr("src")}"
                            baseStats.add(Stat(name, image, value))
                        }
                    }

                    val _100: MutableList<Stat> = mutableListOf()
                    val _100Elements = getElementsByAttributeValue("title", "Level 100")
                    val _100Body = _100Elements[0].child(1).child(0)
                    val _100Filtered = _100Body.children().filter { el -> el.tagName() == "tr" }
                    _100Filtered.forEach { el ->
                        val tds = el.children().filter { e -> e.tagName() === "td" }
                        val ths = el.children().filter { e -> e.tagName() === "th" }
                        tds.forEachIndexed { i, _ ->
                            val value = tds[i].text().replace("\n", "")
                            val name = if (ths[i].child(0).attr("alt").isNullOrBlank()) null else ths[i].child(0).attr("alt")
                            val image = "${Config.baseUrl}${ths[i].child(0).attr("src")}"
                            _100.add(Stat(name, image, value))
                        }
                    }

                    val _120: MutableList<Stat> = mutableListOf()
                    val _120Elements = getElementsByAttributeValue("title", "Level 120")
                    val _120Body = _120Elements[0].child(1).child(0)
                    val _120Filtered = _120Body.children().filter { el -> el.tagName() == "tr" }
                    _120Filtered.forEach { el ->
                        val tds = el.children().filter { e -> e.tagName() === "td" }
                        val ths = el.children().filter { e -> e.tagName() === "th" }
                        tds.forEachIndexed { i, _ ->
                            val value = tds[i].text().replace("\n", "")
                            val name = if (ths[i].child(0).attr("alt").isNullOrBlank()) null else ths[i].child(0).attr("alt")
                            val image = "${Config.baseUrl}${ths[i].child(0).attr("src")}"
                            _120.add(Stat(name, image, value))
                        }
                    }

                    val _100Retrofit: MutableList<Stat> = mutableListOf()
                    val _100RetrofitElements = getElementsByAttributeValue("title", "Level 100 Retrofit")
                    val _100RetrofitBody = if (_100RetrofitElements.isNotEmpty()) _100RetrofitElements[0]?.child(1)?.child(0) else null
                    if (_100RetrofitBody != null) {
                        val _100RetrofitFiltered = _100RetrofitBody.children().filter { el -> el.tagName() == "tr" }
                        _100RetrofitFiltered.forEach { el ->
                            val tds = el.children().filter { e -> e.tagName() === "td" }
                            val ths = el.children().filter { e -> e.tagName() === "th" }
                            tds.forEachIndexed { i, _ ->
                                val value = tds[i].text().replace("\n", "")
                                val name = if (ths[i].child(0).attr("alt").isNullOrBlank()) null else ths[i].child(0).attr("alt")
                                val image = "${Config.baseUrl}${ths[i].child(0).attr("src")}"
                                _100Retrofit.add(Stat(name, image, value))
                            }
                        }
                    }

                    val _120Retrofit: MutableList<Stat> = mutableListOf()
                    val _120RetrofitElements = getElementsByAttributeValue("title", "Level 120 Retrofit")
                    val _120RetrofitBody = if (_120RetrofitElements.isNotEmpty()) _120RetrofitElements[0]?.child(1)?.child(0) else null
                    if (_120RetrofitBody != null) {
                        val _120RetrofitFiltered = _120RetrofitBody.children().filter { el -> el.tagName() == "tr" }
                        _120RetrofitFiltered.forEach { el ->
                            val tds = el.children().filter { e -> e.tagName() === "td" }
                            val ths = el.children().filter { e -> e.tagName() === "th" }
                            tds.forEachIndexed { i, _ ->
                                val value = tds[i].text().replace("\n", "")
                                val name = if (ths[i].child(0).attr("alt").isNullOrBlank()) null else ths[i].child(0).attr("alt")
                                val image = "${Config.baseUrl}${ths[i].child(0).attr("src")}"
                                _120Retrofit.add(Stat(name, image, value))
                            }
                        }
                    }

                    val miscellaneous = Miscellaneous()
                    val mList = getElementsContainingOwnText("Miscellaneous Info").parents()[1].children()
                    val mFiltered = mList.filter { el -> el.tagName() == "tr" }
                    mFiltered.forEach { el ->
                        val children = el.children().filter { e -> e.tagName() == "td" }
                        if (children.count() >= 2) {
                            val title = children[0].text().replace("\n", "")
                            var link: String? = null
                            var name: String? = null

                            if (children[1].children().count() >= 1 ) {
                                link = if (children[1].child(0).attr("href").startsWith("/Artist")) "${Config.baseUrl}${children[1].child(0).attr("href")}" else children[1].child(0).attr("href")
                                name = children[1].child(0).text()
                            }

                            when (title.toLowerCase()) {
                                "artist" -> miscellaneous.artist = MiscellaneousData(link, name)
                                "web" -> miscellaneous.web = MiscellaneousData(link, name)
                                "pixiv" -> miscellaneous.pixiv = MiscellaneousData(link, name)
                                "twitter" -> miscellaneous.twitter = MiscellaneousData(link, name)
                                "voice actress" -> {
                                    val actress = try {
                                        a { withClass = "extiw"; findFirst { this } }
                                    } catch (error: ElementNotFoundException) {
                                        try {
                                            a { withClasses = listOf("external", "text"); withAttribute = Pair("rel", "nofollow"); findByIndex(3) { this } }
                                        } catch (e: ElementNotFoundException) {
                                            null
                                        }
                                    }

                                    miscellaneous.voiceActress = MiscellaneousData(actress?.attr("href"), actress?.text())
                                }
                            }
                        }
                    }

                    Ship(
                        wikiUrl = "${Config.baseUrl}/${ship}",
                        id = shipId,
                        names = Names(
                            en = try { div { withClass = "azl_box_title"; findFirst { text() } } } catch (e: Exception) { null },
                            cn = try { span { withAttribute = Pair("lang", "zh"); findFirst { text() } } } catch (e: Exception) { null },
                            jp = try { span { withAttribute = Pair("lang", "ja"); findFirst { text() } } } catch (e: Exception) { null },
                            kr = try { span { withAttribute = Pair("lang", "ko"); findFirst { text() } } } catch (e: Exception) { null }
                        ),
                        thumbnail = "${Config.baseUrl}/" + try { a { withAttribute = Pair("href", "/File:${ship}Icon.png"); findFirst { child(0).attr("src") } } } catch (e: Exception) { null },
                        skins = skins,
                        buildTime = try { a { withAttribute = Pair("href", "/${ship}#Construction"); findFirst { text() } } } catch (e: Exception) { null },
                        rarity = getElementsContainingOwnText("☆").first()?.child(0)?.attr("title"),
                        stars = Stars(
                            value = getElementsContainingOwnText("☆").first()?.text(),
                            count = getElementsContainingOwnText("☆").first()?.text()?.count { it == '★' } ?: 0
                        ),
                        `class` = getElementsContainingOwnText("Class").first()?.nextElementSibling()?.text(),
                        nationality = getElementsContainingOwnText("Nationality").first()?.nextElementSibling()?.child(1)?.text(),
                        nationalityShort = short,
                        hullType = getElementsContainingOwnText("Classification").first()?.nextElementSibling()?.text(),
                        stats = Stats(_100, _120, baseStats, if (_100Retrofit.isEmpty()) null else _100Retrofit, if (_120Retrofit.isEmpty()) null else _120Retrofit),
                        miscellaneous = miscellaneous
                    )
                }
            }
        }

}