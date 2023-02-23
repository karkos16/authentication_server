package com.czupito_server

import com.czupito_server.data.user.MongoUserDataSource
import com.czupito_server.data.user.User
import io.ktor.server.application.*
import com.czupito_server.plugins.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    val mongoPW = System.getenv("MONGO_PW")
    val dbName = "czupito-auth"
    val db = KMongo.createClient(
        connectionString = "mongodb+srv://karkos:$mongoPW@cluster0.50ysj6w.mongodb.net/$dbName?retryWrites=true&w=majority"
    ).coroutine
        .getDatabase(dbName)

    val userDataSource = MongoUserDataSource(db)

    GlobalScope.launch {
        val user = User (
            username = "test",
            password = "test123",
            salt = "123"
        )
        userDataSource.insertUser(user)
    }

    configureSecurity()
    configureMonitoring()
    configureSerialization()
    configureRouting()
}
