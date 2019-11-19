package info.kurozeropb.azurlane.managers

import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import info.kurozeropb.azurlane.dotenv
import info.kurozeropb.azurlane.structures.Patron
import org.litote.kmongo.KMongo
import kotlin.system.measureTimeMillis

object DatabaseManager {
    private lateinit var client: MongoClient
    private lateinit var db: MongoDatabase
    lateinit var users: MongoCollection<Patron>

    fun initialize() {
        println("Connecting to the database... ")
        val milli = measureTimeMillis {
            client = KMongo.createClient(MongoClientURI("mongodb+srv://${dotenv["user"]}:${dotenv["password"]}@${dotenv["url"]}/${dotenv["database"]}?retryWrites=true&w=majority"))
            db = client.getDatabase(dotenv["database"] ?: "")
            users = db.getCollection("users", Patron::class.java)
        }
        println("Done! (${milli}ms)")
    }
}