package com.flyprosper.syncplay.network.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val name: String,
    val isAdmin: Boolean
)