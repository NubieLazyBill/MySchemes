package com.example.myschemes.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.example.myschemes.data.model.Scheme
import java.io.File
import java.io.FileOutputStream

object PhotoMigrationHelper {

    /**
     * Добавляет водяные знаки на все старые фото шкафа.
     * Извлекает название шкафа и пункта из имени файла.
     * Примечание берёт из текущего состояния шкафа.
     */
    fun migratePhotosForScheme(
        context: Context,
        scheme: Scheme,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Int {
        var processedCount = 0

        // Собираем все фото с их пунктами и примечаниями
        val photosWithInfo = mutableListOf<PhotoInfo>()

        // 1. Диспетчерское наименование
        scheme.cabinetNamePhotos.forEach {
            photosWithInfo.add(PhotoInfo(it, "Диспетчерское наименование", scheme.cabinetNameNote))
        }
        // 2. ДН автоматов, рубильников
        scheme.switchesNamePhotos.forEach {
            photosWithInfo.add(PhotoInfo(it, "ДН автоматов, рубильников", scheme.switchesNameNote))
        }
        // 3. Инвентарный номер
        scheme.inventoryNumberPhotos.forEach {
            photosWithInfo.add(PhotoInfo(it, "Инвентарный номер", scheme.inventoryNumberNote))
        }
        // 4. Целостность замков
        scheme.lockIntegrityPhotos.forEach {
            photosWithInfo.add(PhotoInfo(it, "Целостность замков", scheme.lockIntegrityNote))
        }
        // 5. Уплотнение шкафа
        scheme.sealIntegrityPhotos.forEach {
            photosWithInfo.add(PhotoInfo(it, "Уплотнение шкафа", scheme.sealIntegrityNote))
        }
        // 6. Заходы кабелей
        scheme.cableEntriesPhotos.forEach {
            photosWithInfo.add(PhotoInfo(it, "Заходы кабелей", scheme.cableEntriesNote))
        }
        // 7. Нет оголённых жил
        scheme.noBareWiresPhotos.forEach {
            photosWithInfo.add(PhotoInfo(it, "Нет оголённых жил", scheme.noBareWiresNote))
        }
        // 8. Адресные бирки
        scheme.addressLabelsPhotos.forEach {
            photosWithInfo.add(PhotoInfo(it, "Адресные бирки", scheme.addressLabelsNote))
        }
        // 9. Целостность клеммников
        scheme.terminalsIntegrityPhotos.forEach {
            photosWithInfo.add(PhotoInfo(it, "Целостность клеммников", scheme.terminalsIntegrityNote))
        }
        // 10. Окраска
        scheme.paintingPhotos.forEach {
            photosWithInfo.add(PhotoInfo(it, "Окраска", scheme.paintingNote))
        }
        // 11. Обогрев
        scheme.heatingPhotos.forEach {
            photosWithInfo.add(PhotoInfo(it, "Обогрев", scheme.heatingNote))
        }
        // 12. Заземление
        scheme.groundingPhotos.forEach {
            photosWithInfo.add(PhotoInfo(it, "Заземление", scheme.groundingNote))
        }

        val total = photosWithInfo.size

        // Обрабатываем каждое фото
        photosWithInfo.forEachIndexed { index, info ->
            val file = File(info.path)
            if (file.exists()) {
                addWatermarkToPhoto(
                    info.path,
                    scheme.equipmentName,
                    info.checkpointName,
                    info.note
                )
                processedCount++
            }
            onProgress(index + 1, total)
        }

        return processedCount
    }

    /**
     * Добавляет водяной знак на одно фото (без изменения файла, только содержимое)
     */
    private fun addWatermarkToPhoto(
        photoPath: String,
        equipmentName: String,
        checkpointName: String,
        note: String?
    ): Boolean {
        return try {
            val file = File(photoPath)
            if (!file.exists()) return false

            // Проверяем, есть ли уже водяной знак (по маркеру в файле - не надёжно)
            // Лучше сделать отдельный флаг, но проще просто перезаписать

            val originalBitmap = BitmapFactory.decodeFile(photoPath)
                ?: return false

            val mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(mutableBitmap)

            val width = mutableBitmap.width
            val height = mutableBitmap.height
            val scale = width / 1000f

            val textPaint = Paint().apply {
                color = Color.WHITE
                textSize = 48f * scale
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
                setShadowLayer(6f * scale, 3f * scale, 3f * scale, Color.BLACK)
            }

            val notePaint = Paint().apply {
                color = Color.YELLOW
                textSize = 40f * scale
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
                setShadowLayer(6f * scale, 3f * scale, 3f * scale, Color.BLACK)
            }

            val bgPaint = Paint().apply {
                color = Color.parseColor("#80000000")
            }

            // Верхняя плашка
            val topPadding = 24f * scale
            val topBgHeight = 140f * scale
            canvas.drawRect(0f, 0f, width.toFloat(), topBgHeight, bgPaint)

            canvas.drawText(
                equipmentName,
                topPadding,
                topPadding + 48f * scale,
                textPaint
            )

            val smallerPaint = Paint(textPaint).apply {
                textSize = 40f * scale
                color = Color.parseColor("#B0E0B0")
            }
            canvas.drawText(
                checkpointName,
                topPadding,
                topPadding + 104f * scale,
                smallerPaint
            )

            // Нижняя плашка
            if (!note.isNullOrBlank()) {
                val bottomBgHeight = 120f * scale
                val bottomTop = height - bottomBgHeight
                canvas.drawRect(0f, bottomTop, width.toFloat(), height.toFloat(), bgPaint)

                val noteText = "📝 $note"
                val maxWidth = width - 2 * topPadding
                val lines = wrapText(noteText, notePaint, maxWidth)

                lines.forEachIndexed { index, line ->
                    canvas.drawText(
                        line,
                        topPadding,
                        bottomTop + 48f * scale + index * 48f * scale,
                        notePaint
                    )
                }
            }

            val outputStream = FileOutputStream(file)
            mutableBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
            outputStream.close()

            originalBitmap.recycle()
            mutableBitmap.recycle()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) lines.add(currentLine)
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine)

        return lines.take(2)
    }

    data class PhotoInfo(
        val path: String,
        val checkpointName: String,
        val note: String?
    )
}