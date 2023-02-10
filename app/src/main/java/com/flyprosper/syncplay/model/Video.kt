package com.flyprosper.syncplay.model

data class Video(
    val id: Int,
    val title: String,
    val duration: String?,
    val path: String,
    val isPlaying: Boolean? = null,
    var currentTime: Int? = null
)
