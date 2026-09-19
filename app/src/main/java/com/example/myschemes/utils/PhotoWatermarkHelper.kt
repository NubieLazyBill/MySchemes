package com.example.myschemes.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import java.io.File
import java.io.FileOutputStream

object PhotoWatermarkHelper {

    fun addWatermark(
        context: Context,
        photoPath: String,
        equipmentName: String,
        checkpointName: String,
        note: String?
    ): String? {
        return try {
            val file = File(photoPath)
            if (!file.exists()) return null

            val originalBitmap = BitmapFactory.decodeFile(photoPath)
                ?: return null

            val mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(mutableBitmap)

            val width = mutableBitmap.width
            val height = mutableBitmap.height
            val scale = width / 1000f

            // ===== УВЕЛИЧЕННЫЙ ШРИФТ В 2 РАЗА =====
            val textPaint = Paint().apply {
                color = Color.WHITE
                textSize = 48f * scale          // ← было 24f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
                setShadowLayer(6f * scale, 3f * scale, 3f * scale, Color.BLACK)
            }

            val notePaint = Paint().apply {
                color = Color.YELLOW
                textSize = 40f * scale          // ← было 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
                setShadowLayer(6f * scale, 3f * scale, 3f * scale, Color.BLACK)
            }

            val bgPaint = Paint().apply {
                color = Color.parseColor("#80000000")
            }

            // ===== ВЕРХНЯЯ ПЛАШКА =====
            val topPadding = 24f * scale
            val topBgHeight = 140f * scale      // ← было 70f

            canvas.drawRect(0f, 0f, width.toFloat(), topBgHeight, bgPaint)

            // Название шкафа (сверху)
            canvas.drawText(
                equipmentName,
                topPadding,
                topPadding + 48f * scale,       // ← было 24f
                textPaint
            )

            // Пункт осмотра (под названием шкафа)
            val smallerPaint = Paint(textPaint).apply {
                textSize = 40f * scale          // ← было 20f
                color = Color.parseColor("#B0E0B0")
            }
            canvas.drawText(
                checkpointName,
                topPadding,
                topPadding + 104f * scale,      // ← было 52f
                smallerPaint
            )

            // ===== НИЖНЯЯ ПЛАШКА =====
            if (!note.isNullOrBlank()) {
                val bottomBgHeight = 120f * scale   // ← было 60f
                val bottomTop = height - bottomBgHeight
                canvas.drawRect(0f, bottomTop, width.toFloat(), height.toFloat(), bgPaint)

                val noteText = "📝 $note"
                val maxWidth = width - 2 * topPadding
                val lines = wrapText(noteText, notePaint, maxWidth)

                lines.forEachIndexed { index, line ->
                    canvas.drawText(
                        line,
                        topPadding,
                        bottomTop + 48f * scale + index * 48f * scale,  // ← было 24f
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

            photoPath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val textWidth = paint.measureText(testLine)
            if (textWidth <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                }
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        return lines.take(2)
    }
}