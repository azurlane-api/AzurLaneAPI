package info.kurozeropb.azurlane

import info.kurozeropb.azurlane.controllers.ConstructionController
import info.kurozeropb.azurlane.controllers.PatreonController
import info.kurozeropb.azurlane.controllers.ShipController
import info.kurozeropb.azurlane.controllers.ShipsController
import info.kurozeropb.azurlane.managers.DatabaseManager
import io.github.cdimascio.dotenv.Dotenv
import io.github.cdimascio.dotenv.dotenv
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.Javalin

lateinit var dotenv: Dotenv

object API {

    @JvmStatic
    fun main(args: Array<String>) {
        dotenv = dotenv()

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

            get("/patreon", PatreonController::handleRequest)

            path("v1") {
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
                            "/build"
                        )
                    })
                }

                get("/ship", ShipController::getShip)

                get("/build", ConstructionController::getBuildInfo)

                get("/ships", ShipsController::getShips)
            }
        }
    }
}