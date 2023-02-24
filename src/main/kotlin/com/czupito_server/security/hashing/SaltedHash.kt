package com.czupito_server.security.hashing

data class SaltedHash(
    val hash: String,
    val salt: String
)
