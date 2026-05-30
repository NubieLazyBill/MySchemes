package com.example.myschemes.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object FileHelper {

    fun savePhotoToAppStorage(
        context: Context,
        sourcePath: String,
        equipmentName: String,
        checkpointName: String
    ): String? {
        return try {
            val destinationDir = File(context.filesDir, "photos")
            if (!destinationDir.exists()) {
                destinationDir.mkdirs()
            }

            val cleanEquipmentName = equipmentName
                .replace("[^a-zA-Zа-яА-Я0-9]".toRegex(), "_")
                .replace(" ", "_")
                .take(30)

            val cleanCheckpointName = checkpointName
                .replace("[^a-zA-Zа-яА-Я0-9]".toRegex(), "_")
                .replace(" ", "_")
                .take(50)

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "${cleanEquipmentName}|${cleanCheckpointName}_$timeStamp.jpg"
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