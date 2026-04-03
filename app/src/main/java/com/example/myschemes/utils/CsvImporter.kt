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
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        var lineNumber = 0
        reader.forEachLine { line ->
            if (lineNumber == 0) {
                lineNumber++
                return@forEachLine
            }

            val parts = line.split(",")
            if (parts.size < 5) return@forEachLine

            val itemNumber = parts[0].toIntOrNull()
            val cellNumber = parts[1].trim().takeIf { it.isNotBlank() && it != "null" }
            val equipmentName = parts[2].trim()
            val lastRevisionDate = dateFormat.parse(parts[3].trim())?.time ?: 0L
            val nextRevisionDate = dateFormat.parse(parts[4].trim())?.time ?: 0L
            val schemeNumber = parts.getOrNull(5)?.trim()?.takeIf { it.isNotBlank() && it != "null" }

            if (equipmentName.isNotBlank() && lastRevisionDate != 0L && nextRevisionDate != 0L) {
                schemes.add(
                    Scheme(
                        itemNumber = itemNumber,
                        cellNumber = cellNumber,
                        equipmentName = equipmentName,
                        lastRevisionDate = lastRevisionDate,
                        nextRevisionDate = nextRevisionDate,
                        schemeNumber = schemeNumber
                    )
                )
            }
            lineNumber++
        }

        reader.close()
        return schemes
    }
}