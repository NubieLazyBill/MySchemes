package com.example.myschemes.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object FileHelper {

    fun savePhotoToAppStorage(context: Context, sourcePath: String): String? {
        return try {
            val fileName = "photo_${System.currentTimeMillis()}.jpg"
            val destinationFile = File(context.filesDir, "photos/$fileName")

            // Создаём директорию если не существует
            destinationFile.parentFile?.mkdirs()

            // Копируем файл
            val sourceFile = File(sourcePath)
            if (sourceFile.exists()) {
                sourceFile.copyTo(destinationFile, overwrite = true)
            } else {
                // Если это URI из галереи
                val inputStream = context.contentResolver.openInputStream(Uri.parse(sourcePath))
                inputStream?.use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        input.copyTo(output)
                    }
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
            if (file.exists() && file.absolutePath.contains(context.filesDir.absolutePath)) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}