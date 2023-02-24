package com.czupito_server

import com.czupito_server.data.requests.AuthRequest
import com.czupito_server.data.responses.AuthResponse
import com.czupito_server.data.user.User
import com.czupito_server.data.user.UserDataSource
import com.czupito_server.security.hashing.HashingService
import com.czupito_server.security.hashing.SHA256HashingService
import com.czupito_server.security.hashing.SaltedHash
import com.czupito_server.security.token.TokenClaim
import com.czupito_server.security.token.TokenConfig
import com.czupito_server.security.token.TokenService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.signUp(
    hashingService: HashingService,
    userDataSource: UserDataSource
) {
    post("signup") {
        val result = kotlin.runCatching { call.receive<AuthRequest>() }
        result.onSuccess { request -> 
            val receivedRequest = request
        }.onFailure { exception ->
            call.respond(HttpStatusCode.BadRequest)
            return@post
        }

        if (result.isSuccess) {
            val recivedRequest = result.getOrNull()
            if (recivedRequest != null) {

                val areFieldsBlank = recivedRequest.username.isBlank() or recivedRequest.password.isBlank()
                val isPasswordTooShort = recivedRequest.password.length < 8
                if(areFieldsBlank or isPasswordTooShort) {
                    call.respond(HttpStatusCode.Conflict)
                    return@post
                }

                val saltedHash = hashingService.generateSaltedHash(recivedRequest.password)
                val user = User(
                    username = recivedRequest.username,
                    password = saltedHash.hash,
                    salt = saltedHash.salt
                )
                val wasAcknowledged = userDataSource.insertUser(user)
                if (!wasAcknowledged) {
                    call.respond(HttpStatusCode.Conflict)
                    return@post
                }

                call.respond(HttpStatusCode.OK)

            } else {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }
        } else {
            call.respond(HttpStatusCode.BadRequest)
            return@post
        }
    }

}

fun Route.signIn(
    userDataSource: UserDataSource,
    hashingService: HashingService,
    tokenService: TokenService,
    tokenConfig: TokenConfig
) {
    post("signin") {
        val result = kotlin.runCatching { call.receive<AuthRequest>() }
        result.onSuccess { request ->
            val receivedRequest = request
        }.onFailure { exception ->
            call.respond(HttpStatusCode.BadRequest)
            return@post
        }

        if (result.isSuccess) {
            val receivedRequest = result.getOrNull()
            if (receivedRequest != null) {

                val user = userDataSource.getUserByUsername(receivedRequest.username)
                if (user == null) {
                    call.respond(HttpStatusCode.Conflict, "User is null")
                    return@post
                }

                val isValidPassword = hashingService.verify(
                    value = receivedRequest.password,
                    saltedHash = SaltedHash(
                        hash = user.password,
                        salt = user.salt
                    )
                )

                if (!isValidPassword) {
                    call.respond(HttpStatusCode.Conflict, "Inccorrect username or password")
                    return@post
                }

                val token = tokenService.generate(
                    config = tokenConfig,
                    TokenClaim(
                        name = "userId",
                        value = user.id.toString()
                    )
                )

                call.respond(
                    status = HttpStatusCode.OK,
                    message = AuthResponse(
                        token = token
                    )
                )

            } else {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }
        } else {
            call.respond(HttpStatusCode.BadRequest)
            return@post
        }

    }
}

fun Route.authenticate() {
    authenticate {
        get("authenticate") {
            call.respond(HttpStatusCode.OK)
        }
    }
}

fun Route.getSecretInfo() {
    authenticate {
        get("secret") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.getClaim("userId", String::class)
            call.respond(HttpStatusCode.OK, "Your userId is $userId")
        }
    }
}