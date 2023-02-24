package com.czupito_server.plugins

import com.czupito_server.authenticate
import com.czupito_server.data.user.UserDataSource
import com.czupito_server.getSecretInfo
import com.czupito_server.security.hashing.HashingService
import com.czupito_server.security.token.TokenConfig
import com.czupito_server.security.token.TokenService
import com.czupito_server.signIn
import com.czupito_server.signUp
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.application.*

fun Application.configureRouting(
    userDataSource: UserDataSource,
    hashingService: HashingService,
    tokenService: TokenService,
    tokenConfig: TokenConfig
) {
    routing {
        signIn(userDataSource, hashingService, tokenService, tokenConfig)
        signUp(hashingService, userDataSource)
        authenticate()
        getSecretInfo()
    }
}
