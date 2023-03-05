package com.flyprosper.syncplay.model

data class Video(
    val id: Int,
    val title: String,
    val duration: String?,
    val path: String,
    var isPlaying: Boolean? = null,
    var currentTime: Long? = null
)
