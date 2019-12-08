package info.kurozeropb.azurlane

import info.kurozeropb.azurlane.controllers.*
import info.kurozeropb.azurlane.controllers.NationsController
import info.kurozeropb.azurlane.managers.DatabaseManager
import info.kurozeropb.azurlane.structures.Patron
import io.github.cdimascio.dotenv.Dotenv
import io.github.cdimascio.dotenv.dotenv
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.Javalin
import io.javalin.core.util.Header
import io.javalin.http.Context
import org.litote.kmongo.eq
import org.litote.kmongo.findOne

lateinit var dotenv: Dotenv

object API {

    @JvmStatic
    fun main(args: Array<String>) {
        dotenv = dotenv {
            ignoreIfMissing = true
        }

        val app = Javalin.create().apply {
            exception(Exception::class.java) { e, _ -> e.printStackTrace() }
            error(404) { ctx -> ctx.json("not found") }
        }.start(dotenv["PORT"]?.toInt() ?: 8080)

        DatabaseManager.initialize()

        app.routes {

            get("/") { ctx ->
                ctx.status(200).json(object {
                    val statusCode = 200
                    val statusMessage = "OK"
                    val message = "Request was successful"
                    val routes = listOf(
                        "/v1/ship [deprecated]",
                        "/v1/ships [deprecated]",
                        "/v1/build [deprecated]",
                        "/v2/ship",
                        "/v2/ships",
                        "/v2/build"
                    )
                })
            }

            post("/patreon", PatreonController::handleRequest)

            path("/v1") {
                get { ctx ->
                    ctx.status(200).json(object {
                        val statusCode = 200
                        val statusMessage = "OK"
                        val message = "Request was successful"
                        val routes = listOf(
                            "/ship [deprecated]",
                            "/ships [deprecated]",
                            "/build [deprecated]"
                        )
                    })
                }

                get("/ship", ShipController::getShip)

                get("/build", ConstructionController::getBuildInfo)

                get("/ships", ShipsController::getShips)
            }

            path("/v2") {

                get { ctx ->
                    ctx.status(200).json(object {
                        val statusCode = 200
                        val statusMessage = "OK"
                        val message = "Request was successful"
                        val routes = listOf(
                            "/ship",
                            "/ships",
                            "/build",
                            "/names"
                        )
                    })
                }

                get("/ship", ShipController::getShip)

                get("/build", ConstructionController::getBuildInfo)

                get("/ships", ShipsController::getShips)

                get("/ships/all", AllShipsController::getAllShips)

                get("/nations", NationsController::getNations)

                get("/equipment") {}

                get("/equipment/all", EquipmentsController::getEquipments)

                get("/equipment/types", EquipmentsController::getEquipmentTypes)
            }
        }
    }

    fun authorize(ctx: Context): Boolean {
        val token = ctx.header(Header.AUTHORIZATION) ?: return false
        DatabaseManager.users.findOne(Patron::token eq token) ?: return false
        return true
    }
}