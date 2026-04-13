package com.example.myschemes.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object FileHelper {

    fun savePhotoToAppStorage(context: Context, sourcePath: String): String? {
        return try {
            val destinationDir = File(context.filesDir, "photos")
            if (!destinationDir.exists()) {
                destinationDir.mkdirs()
            }

            val fileName = "photo_${System.currentTimeMillis()}.jpg"
            val destinationFile = File(destinationDir, fileName)

            val inputStream = if (sourcePath.startsWith("content://")) {
                val uri = Uri.parse(sourcePath)
                context.contentResolver.openInputStream(uri)
            } else {
                File(sourcePath).inputStream()
            }

            inputStream?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }

            destinationFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deletePhotoFromAppStorage(context: Context, photoPath: String) {
        try {
            val file = File(photoPath)
            if (file.exists()) {
                val deleted = file.delete()
                android.util.Log.d("FileHelper", "Удаление фото $photoPath: ${if (deleted) "успешно" else "не удалось"}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}