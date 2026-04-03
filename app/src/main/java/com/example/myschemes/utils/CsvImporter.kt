package com.example.myschemes.utils

import android.content.Context
import com.example.myschemes.data.model.Scheme
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

class CsvImporter(private val context: Context) {

    fun importFromAssets(filename: String = "schemes.csv"): List<Scheme> {
        val schemes = mutableListOf<Scheme>()
        val inputStream = context.assets.open(filename)
        val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())

        var lineNumber = 0

        reader.forEachLine { line ->
            if (line.isBlank()) return@forEachLine

            // Пропускаем заголовки (первые 3 строки)
            if (lineNumber < 3) {
                lineNumber++
                return@forEachLine
            }

            // Пропускаем пустые строки и строки-разделители
            if (line.startsWith(",,,") || line.contains("Наименование оборудования")) {
                return@forEachLine
            }

            val parts = parseCsvLine(line)
            if (parts.size < 3) return@forEachLine

            val equipmentName = parts[0].trim()
            val lastRevisionDateStr = parts[1].trim()
            val nextRevisionDateStr = parts[2].trim()
            val schemeNumber = if (parts.size > 3) parts[3].trim().takeIf { it.isNotBlank() } else null
            val cellNumber = if (parts.size > 4) parts[4].trim().takeIf { it.isNotBlank() } else null  // ← ДОБАВИЛ ЯЧЕЙКУ

            // Пропускаем строки без названия оборудования или без дат
            if (equipmentName.isEmpty() || equipmentName == "Наименование оборудования") return@forEachLine
            if (lastRevisionDateStr.isEmpty() || nextRevisionDateStr.isEmpty()) return@forEachLine

            try {
                val lastRevisionDate = dateFormat.parse(lastRevisionDateStr)?.time ?: 0L
                val nextRevisionDate = dateFormat.parse(nextRevisionDateStr)?.time ?: 0L

                if (lastRevisionDate != 0L && nextRevisionDate != 0L) {
                    schemes.add(
                        Scheme(
                            itemNumber = schemes.size + 1,
                            cellNumber = cellNumber,  // ← ТЕПЕРЬ ЧИТАЕТ ЯЧЕЙКУ
                            equipmentName = equipmentName,
                            lastRevisionDate = lastRevisionDate,
                            nextRevisionDate = nextRevisionDate,
                            schemeNumber = schemeNumber
                        )
                    )
                }
            } catch (e: Exception) {
                println("Ошибка парсинга даты: $lastRevisionDateStr, $nextRevisionDateStr")
            }
            lineNumber++
        }

        reader.close()
        println("Загружено схем: ${schemes.size}")
        return schemes
    }

    // Функция для правильного парсинга CSV с кавычками
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val char = line[i]
            when {
                char == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"')
                    i++
                }
                char == '"' -> {
                    inQuotes = !inQuotes
                }
                char == ',' && !inQuotes -> {
                    result.add(current.toString().trim())
                    current = StringBuilder()
                }
                else -> {
                    current.append(char)
                }
            }
            i++
        }
        result.add(current.toString().trim())
        return result
    }
}