package com.flyprosper.syncplay.model

import java.time.Duration

data class VideoDirectory(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val thumbnailPath: String? = null
)
