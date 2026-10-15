package com.example.myschemes.utils

import android.content.Context
import android.net.Uri
import com.example.myschemes.data.model.Scheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object DatabaseBackupHelper {

    private const val BACKUP_VERSION = 1
    private const val SCHEMES_JSON = "schemes.json"
    private const val PHOTOS_DIR = "photos/"
    private const val METADATA_JSON = "metadata.json"

    data class BackupMetadata(
        val version: Int,
        val timestamp: Long,
        val schemesCount: Int,
        val photosCount: Int
    )

    /**
     * Экспортирует БД + фото в ZIP-файл
     */
    fun exportBackup(
        context: Context,
        schemes: List<Scheme>,
        outputFile: File
    ): Boolean {
        return try {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(outputFile))).use { zos ->
                val gson = Gson()

                // 1. Собираем все фото из всех пунктов
                val allPhotos = mutableSetOf<String>()
                schemes.forEach { scheme ->
                    allPhotos.addAll(scheme.cabinetNamePhotos)
                    allPhotos.addAll(scheme.switchesNamePhotos)
                    allPhotos.addAll(scheme.inventoryNumberPhotos)
                    allPhotos.addAll(scheme.lockIntegrityPhotos)
                    allPhotos.addAll(scheme.sealIntegrityPhotos)
                    allPhotos.addAll(scheme.cableEntriesPhotos)
                    allPhotos.addAll(scheme.noBareWiresPhotos)
                    allPhotos.addAll(scheme.addressLabelsPhotos)
                    allPhotos.addAll(scheme.terminalsIntegrityPhotos)
                    allPhotos.addAll(scheme.paintingPhotos)
                    allPhotos.addAll(scheme.heatingPhotos)
                    allPhotos.addAll(scheme.groundingPhotos)
                }

                // 2. Метаданные
                val metadata = BackupMetadata(
                    version = BACKUP_VERSION,
                    timestamp = System.currentTimeMillis(),
                    schemesCount = schemes.size,
                    photosCount = allPhotos.size
                )
                zos.putNextEntry(ZipEntry(METADATA_JSON))
                zos.write(gson.toJson(metadata).toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // 3. Шкафы (JSON)
                zos.putNextEntry(ZipEntry(SCHEMES_JSON))
                zos.write(gson.toJson(schemes).toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // 4. Фото
                var photosAdded = 0
                allPhotos.forEach { photoPath ->
                    try {
                        val file = File(photoPath)
                        if (file.exists()) {
                            zos.putNextEntry(ZipEntry("$PHOTOS_DIR${file.name}"))
                            FileInputStream(file).use { fis ->
                                fis.copyTo(zos)
                            }
                            zos.closeEntry()
                            photosAdded++
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("Backup", "Ошибка добавления фото: $photoPath", e)
                    }
                }

                android.util.Log.d("Backup", "Экспортировано шкафов: ${schemes.size}, фото: $photosAdded")
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Импортирует БД + фото из ZIP-файла
     */
    fun importBackup(
        context: Context,
        inputUri: Uri
    ): ImportResult {
        return try {
            val photosDir = File(context.filesDir, "photos")
            if (!photosDir.exists()) photosDir.mkdirs()

            var photosRestored = 0
            var schemesJson: String? = null

            // Читаем архив
            val inputStream = context.contentResolver.openInputStream(inputUri)
                ?: return ImportResult.Error("Не удалось открыть файл")

            inputStream.use { stream ->
                ZipInputStream(BufferedInputStream(stream)).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry

                    while (entry != null) {
                        when {
                            entry.name == SCHEMES_JSON -> {
                                schemesJson = zis.readBytes().toString(Charsets.UTF_8)
                            }
                            entry.name.startsWith(PHOTOS_DIR) && !entry.isDirectory -> {
                                val fileName = entry.name.removePrefix(PHOTOS_DIR)
                                val destFile = File(photosDir, fileName)
                                FileOutputStream(destFile).use { fos ->
                                    zis.copyTo(fos)
                                }
                                photosRestored++
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }

            // Проверяем, что JSON найден
            val json = schemesJson
                ?: return ImportResult.Error("Файл schemes.json не найден в архиве")

            // Парсим JSON
            val type = object : TypeToken<List<Scheme>>() {}.type
            val schemes: List<Scheme> = Gson().fromJson(json, type)
                ?: return ImportResult.Error("Не удалось прочитать данные из архива")

            ImportResult.Success(schemes, photosRestored)
        } catch (e: Exception) {
            e.printStackTrace()
            ImportResult.Error("Ошибка импорта: ${e.message}")
        }
    }

    sealed class ImportResult {
        data class Success(val schemes: List<Scheme>, val photosRestored: Int) : ImportResult()
        data class Error(val message: String) : ImportResult()
    }
}