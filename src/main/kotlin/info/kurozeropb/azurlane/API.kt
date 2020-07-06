package info.kurozeropb.azurlane

import info.kurozeropb.azurlane.controllers.*
import info.kurozeropb.azurlane.controllers.NationsController
import info.kurozeropb.azurlane.managers.DatabaseManager
import info.kurozeropb.azurlane.structures.BaseResponse
import info.kurozeropb.azurlane.structures.Patron
import info.kurozeropb.azurlane.structures.IndexResponse
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
        }.start(dotenv["PORT"]?.toInt() ?: 8081)

        DatabaseManager.initialize()

        app.routes {

            path("/azurlane") {

                get("/") { ctx ->
                    ctx.status(200).json(IndexResponse(
                        statusCode = 200,
                        statusMessage = "OK",
                        message = "Request was successful",
                        routes = listOf(
                            "/v2/ship",
                            "/v2/ships",
                            "/v2/build",
                            "/v2/names"
                        )
                    ))
                }

                post("/patreon", PatreonController::handleRequest)

                path("/v1") {
                    get { ctx ->
                        ctx.status(200).json(BaseResponse(
                            statusCode = 410,
                            statusMessage = "Gone",
                            message = "v1 is no longer available and has been permanently deleted, please switch all api requests to /v2"
                        ))
                    }
                }

                path("/v2") {

                    get { ctx ->
                        ctx.status(200).json(IndexResponse(
                            statusCode = 200,
                            statusMessage = "OK",
                            message = "Request was successful",
                            routes = listOf(
                                "/ship",
                                "/ships",
                                "/build",
                                "/names"
                            )
                        ))
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
    }

    fun authorize(ctx: Context): Boolean {
        val token = ctx.header(Header.AUTHORIZATION) ?: return false
        DatabaseManager.users.findOne(Patron::token eq token) ?: return false
        return true
    }
}
