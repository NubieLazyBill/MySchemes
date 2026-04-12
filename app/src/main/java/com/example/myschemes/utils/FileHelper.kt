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

            android.util.Log.d("FileHelper", "Сохраняем фото: $sourcePath -> ${destinationFile.absolutePath}")

            destinationFile.parentFile?.mkdirs()

            val sourceFile = File(sourcePath)
            if (sourceFile.exists()) {
                sourceFile.copyTo(destinationFile, overwrite = true)
                android.util.Log.d("FileHelper", "Фото скопировано из временного файла")
            } else {
                val inputStream = context.contentResolver.openInputStream(Uri.parse(sourcePath))
                inputStream?.use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        input.copyTo(output)
                    }
                    android.util.Log.d("FileHelper", "Фото скопировано из URI")
                }
            }
            destinationFile.absolutePath
        } catch (e: Exception) {
            android.util.Log.e("FileHelper", "Ошибка сохранения фото: ${e.message}")
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