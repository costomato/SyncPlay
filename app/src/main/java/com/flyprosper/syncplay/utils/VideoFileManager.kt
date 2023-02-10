package com.flyprosper.syncplay.utils

import android.content.ContentResolver
import android.os.Environment
import android.provider.MediaStore
import com.flyprosper.syncplay.model.VideoDirectory
import java.io.File
import java.net.URLConnection


class VideoFileManager {

    fun getVideoFiles(path: String): List<VideoDirectory> {
        val file = File(path)
        val videoFiles = mutableListOf<VideoDirectory>()
        for (f in file.listFiles()?.sorted()?.toList() ?: emptyList()) {
            if (f.isDirectory) continue
            val mimeType = URLConnection.guessContentTypeFromName(f.name)
            if (mimeType?.startsWith("video/") == true)
                videoFiles.add(VideoDirectory(f.name, f.path, false))
        }
        return videoFiles
    }

    fun getVideoDirectories(contentResolver: ContentResolver): List<VideoDirectory> {
        val selection = "${MediaStore.Video.Media.DATA} like?"
        val selectionArgs = arrayOf("%${Environment.getExternalStorageDirectory()}%")
        val cursor = contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            null,
            selection,
            selectionArgs,
            null
        )

        val videoDirs = mutableListOf<VideoDirectory>()
        while (cursor?.moveToNext() == true) {
            val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))
            val file = File(path)
            val parent = file.parentFile
            val name = parent?.name
            val thumbnailPath = file.path

            if (parent != null) {
                if (!videoDirs.any {
                        it.path == parent.absolutePath
                    }) {
                    videoDirs.add(
                        VideoDirectory(
                            name ?: "",
                            parent.absolutePath,
                            true,
                            thumbnailPath
                        )
                    )
                }
            }
        }
        cursor?.close()

        return videoDirs
    }

}