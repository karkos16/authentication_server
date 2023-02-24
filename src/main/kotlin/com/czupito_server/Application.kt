package com.czupito_server

import com.czupito_server.data.user.MongoUserDataSource
import com.czupito_server.data.user.User
import io.ktor.server.application.*
import com.czupito_server.plugins.*
import com.czupito_server.security.hashing.SHA256HashingService
import com.czupito_server.security.token.JwtTokenService
import com.czupito_server.security.token.TokenConfig
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

    val tokenService = JwtTokenService()
    val tokenConfig = TokenConfig(
        issuer = environment.config.property("jwt.issuer").getString(),
        audience = environment.config.property("jwt.audience").getString(),
        expiresIn = 1000L * 60L * 60L * 24L * 365L,
        secret = System.getenv("JWT_SECRET")
    )
    val hashingService =  SHA256HashingService()

    configureSecurity(tokenConfig)
    configureMonitoring()
    configureSerialization()
    configureRouting(userDataSource, hashingService, tokenService, tokenConfig)
}
