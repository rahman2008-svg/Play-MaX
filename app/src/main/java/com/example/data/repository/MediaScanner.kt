package com.example.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.example.data.model.MediaFile
import com.example.data.model.MediaFolder
import java.io.File

object MediaScanner {
    private const val TAG = "MediaScanner"

    fun scanVideos(context: Context): List<MediaFile> {
        val videoList = mutableListOf<MediaFile>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.RESOLUTION,
            MediaStore.Video.Media.DATE_MODIFIED
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
        val queryUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        try {
            context.contentResolver.query(
                queryUri,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val resolutionColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.RESOLUTION)
                val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Video_$id"
                    val duration = cursor.getLong(durationColumn)
                    val size = cursor.getLong(sizeColumn)
                    val path = cursor.getString(dataColumn) ?: ""
                    val resolution = cursor.getString(resolutionColumn)
                    val dateModified = cursor.getLong(dateModifiedColumn)

                    // Get containing folder name
                    val parentFile = File(path).parentFile
                    val folderName = parentFile?.name ?: "Unknown"

                    if (File(path).exists() || path.isNotEmpty()) {
                        videoList.add(
                            MediaFile(
                                id = id,
                                path = path,
                                title = name,
                                duration = duration,
                                size = size,
                                isVideo = true,
                                folderName = folderName,
                                resolution = resolution,
                                dateModified = dateModified
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning videos", e)
        }
        return videoList
    }

    fun scanAudios(context: Context): List<MediaFile> {
        val audioList = mutableListOf<MediaFile>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATE_MODIFIED
        )

        val sortOrder = "${MediaStore.Audio.Media.DATE_MODIFIED} DESC"
        val queryUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        try {
            context.contentResolver.query(
                queryUri,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Audio_$id"
                    val title = cursor.getString(titleColumn) ?: name
                    val duration = cursor.getLong(durationColumn)
                    val size = cursor.getLong(sizeColumn)
                    val path = cursor.getString(dataColumn) ?: ""
                    val album = cursor.getString(albumColumn)
                    val artist = cursor.getString(artistColumn)
                    val dateModified = cursor.getLong(dateModifiedColumn)

                    val parentFile = File(path).parentFile
                    val folderName = parentFile?.name ?: "Unknown"

                    if (File(path).exists() || path.isNotEmpty()) {
                        audioList.add(
                            MediaFile(
                                id = id,
                                path = path,
                                title = title,
                                duration = duration,
                                size = size,
                                album = album,
                                artist = artist,
                                isVideo = false,
                                folderName = folderName,
                                dateModified = dateModified
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning audio files", e)
        }
        return audioList
    }

    fun getFolders(files: List<MediaFile>): List<MediaFolder> {
        val folderMap = mutableMapOf<String, MutableList<MediaFile>>()
        for (file in files) {
            val parentPath = File(file.path).parent ?: "Root"
            folderMap.getOrPut(parentPath) { mutableListOf() }.add(file)
        }

        return folderMap.map { (path, folderFiles) ->
            val name = File(path).name
            val isVideo = folderFiles.any { it.isVideo }
            val videoCount = folderFiles.count { it.isVideo }
            val audioCount = folderFiles.count { !it.isVideo }
            MediaFolder(
                name = name,
                path = path,
                isVideo = isVideo,
                videoCount = videoCount,
                audioCount = audioCount
            )
        }.sortedBy { it.name }
    }
}
