package com.example.myschemes.utils

import android.content.Context
import com.example.myschemes.data.model.Scheme
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object WorkReportExporter {

    private const val TEMPLATE_ASSET = "work_template.xlsx"

    private val YELLOW_COLOR = IndexedColors.YELLOW.index
    private val GREEN_COLOR = IndexedColors.LIGHT_GREEN.index

    fun export(
        context: Context,
        schemes: List<Scheme>
    ): File? {
        return try {
            val templateStream = context.assets.open(TEMPLATE_ASSET)
            val workbook = XSSFWorkbook(templateStream)
            templateStream.close()

            val sheet = workbook.getSheetAt(0)

            val schemesMap = mutableMapOf<String, Scheme>()
            schemes.forEach { scheme ->
                val key = normalizeName(scheme.equipmentName)
                if (key.isNotEmpty()) {
                    schemesMap[key] = scheme
                }
            }

            val foundSchemeKeys = mutableSetOf<String>()

            var currentTableType = TableType.ORU

            val lastRowNum = sheet.lastRowNum
            for (rowIndex in 0..lastRowNum) {
                val row = sheet.getRow(rowIndex) ?: continue

                // ===== БЕЗОПАСНОЕ ЧТЕНИЕ =====
                val firstCell = getCellStringValue(row.getCell(0))
                val thirdCell = getCellStringValue(row.getCell(2))
                val headerD = getCellStringValue(row.getCell(3))

                // Определяем тип таблицы по заголовку колонки D
                if (headerD.contains("дисп.наим. Шкафа")) {
                    currentTableType = TableType.ORU
                    continue
                } else if (headerD.contains("окраска")) {
                    currentTableType = TableType.SHAOT
                    continue
                }

                // Пропускаем заголовки разделов
                if (thirdCell == "ОРУ-500" || thirdCell == "ШАОТы" ||
                    thirdCell == "Баллонная №1" || thirdCell.isEmpty()) {
                    continue
                }

                // ===== НАЗВАНИЕ ШКАФА =====
                val equipmentName = thirdCell
                if (equipmentName.isEmpty()) continue
                if (equipmentName == "Наименование оборудования") continue
                if (equipmentName.startsWith("№")) continue
                if (equipmentName.startsWith("Компрессорная №1") &&
                    currentTableType == TableType.SHAOT) continue

                val normalizedKey = normalizeName(equipmentName)
                val scheme = schemesMap[normalizedKey]

                if (scheme != null) {
                    foundSchemeKeys.add(normalizedKey)
                    fillRow(workbook, row, scheme, currentTableType, isFound = true)
                } else {
                    fillRow(workbook, row, null, currentTableType, isFound = false)
                }
            }

            // Шкафы, которых нет в шаблоне — добавляем в конец
            val notInTemplate = schemes.filter { scheme ->
                val key = normalizeName(scheme.equipmentName)
                key.isNotEmpty() && !foundSchemeKeys.contains(key)
            }

            if (notInTemplate.isNotEmpty()) {
                var newRowIndex = lastRowNum + 2
                val headerRow = sheet.createRow(newRowIndex)
                headerRow.createCell(2).setCellValue("➕ Добавлено из отчёта (не было в шаблоне)")
                newRowIndex++

                notInTemplate.forEach { scheme ->
                    val newRow = sheet.createRow(newRowIndex)
                    fillRow(workbook, newRow, scheme, TableType.ORU, isFound = true, isNew = true)
                    newRowIndex++
                }
            }

            val timeStamp = SimpleDateFormat("dd.MM.yyyy_HH-mm", Locale.getDefault()).format(Date())
            val fileName = "Шкафы версия рабочая_$timeStamp.xlsx"
            val outputFile = File(context.cacheDir, fileName)

            FileOutputStream(outputFile).use { out ->
                workbook.write(out)
            }
            workbook.close()

            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Безопасно читает значение ячейки как строку
     */
    private fun getCellStringValue(cell: Cell?): String {
        if (cell == null) return ""
        return try {
            when (cell.cellType) {
                CellType.STRING -> cell.stringCellValue.trim()
                CellType.NUMERIC -> {
                    if (DateUtil.isCellDateFormatted(cell)) {
                        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                        dateFormat.format(cell.dateCellValue)
                    } else {
                        val num = cell.numericCellValue
                        if (num == num.toLong().toDouble()) {
                            num.toLong().toString()
                        } else {
                            num.toString()
                        }
                    }
                }
                CellType.BOOLEAN -> cell.booleanCellValue.toString()
                CellType.FORMULA -> {
                    try {
                        cell.stringCellValue.trim()
                    } catch (e: Exception) {
                        try {
                            cell.numericCellValue.toString()
                        } catch (e2: Exception) {
                            ""
                        }
                    }
                }
                CellType.BLANK -> ""
                else -> ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    private fun fillRow(
        workbook: XSSFWorkbook,
        row: Row,
        scheme: Scheme?,
        tableType: TableType,
        isFound: Boolean,
        isNew: Boolean = false
    ) {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        val notesList = mutableListOf<String>()

        if (scheme != null) {
            if (!scheme.cabinetNameNote.isNullOrBlank())
                notesList.add("Диспетчерское наименование - ${scheme.cabinetNameNote}")
            if (!scheme.switchesNameNote.isNullOrBlank())
                notesList.add("ДН автоматов, рубильников - ${scheme.switchesNameNote}")
            if (!scheme.inventoryNumberNote.isNullOrBlank())
                notesList.add("Инвентарный номер - ${scheme.inventoryNumberNote}")
            if (!scheme.lockIntegrityNote.isNullOrBlank())
                notesList.add("Целостность замков - ${scheme.lockIntegrityNote}")
            if (!scheme.sealIntegrityNote.isNullOrBlank())
                notesList.add("Уплотнение шкафа - ${scheme.sealIntegrityNote}")
            if (!scheme.cableEntriesNote.isNullOrBlank())
                notesList.add("Заходы кабелей - ${scheme.cableEntriesNote}")
            if (!scheme.noBareWiresNote.isNullOrBlank())
                notesList.add("Нет оголённых жил - ${scheme.noBareWiresNote}")
            if (!scheme.addressLabelsNote.isNullOrBlank())
                notesList.add("Адресные бирки - ${scheme.addressLabelsNote}")
            if (!scheme.terminalsIntegrityNote.isNullOrBlank())
                notesList.add("Целостность клеммников - ${scheme.terminalsIntegrityNote}")
            if (!scheme.paintingNote.isNullOrBlank())
                notesList.add("Окраска - ${scheme.paintingNote}")
            if (!scheme.heatingNote.isNullOrBlank())
                notesList.add("Обогрев - ${scheme.heatingNote}")
            if (!scheme.groundingNote.isNullOrBlank())
                notesList.add("Заземление - ${scheme.groundingNote}")
        }

        when (tableType) {
            TableType.ORU -> {
                scheme?.let {
                    setCellValue(row, 3, if (it.cabinetNameChecked) "+" else "-")
                    setCellValue(row, 4, if (it.lockIntegrity) "+" else "-")
                    setCellValue(row, 5, if (it.sealIntegrity) "+" else "-")
                    setCellValue(row, 6, if (it.cableEntries) "+" else "-")
                    setCellValue(row, 7, if (it.switchesNameChecked) "+" else "-")
                    setCellValue(row, 8, if (it.noBareWires) "+" else "-")
                    setCellValue(row, 9, if (it.addressLabels) "+" else "-")
                    setCellValue(row, 10, if (it.terminalsIntegrity) "+" else "-")
                    setCellValue(row, 11, if (it.painting) "+" else "-")
                    setCellValue(row, 12, if (it.inventoryNumberChecked) "+" else "-")
                    setCellValue(row, 13, if (it.heating) "+" else "-")
                    setCellValue(row, 14, if (it.grounding) "+" else "-")
                    setCellValue(row, 15, if (it.lastRevisionDate != 0L) dateFormat.format(Date(it.lastRevisionDate)) else "")
                    setCellValue(row, 16, if (it.nextRevisionDate != 0L) dateFormat.format(Date(it.nextRevisionDate)) else "")
                    setCellValue(row, 17, it.schemeNumber ?: "")
                }
            }
            TableType.SHAOT -> {
                scheme?.let {
                    setCellValue(row, 3, if (it.painting) "+" else "-")
                    setCellValue(row, 4, if (it.inventoryNumberChecked) "+" else "-")
                    setCellValue(row, 5, if (it.cabinetNameChecked) "+" else "-")
                    setCellValue(row, 6, if (it.cableEntries) "+" else "-")
                    setCellValue(row, 7, if (it.switchesNameChecked) "+" else "-")
                    setCellValue(row, 8, if (it.noBareWires) "+" else "-")
                    setCellValue(row, 9, if (it.terminalsIntegrity) "+" else "-")
                    setCellValue(row, 10, if (it.sealIntegrity) "+" else "-")
                    setCellValue(row, 11, if (it.lockIntegrity) "+" else "-")
                    // M (12) — пусто
                    setCellValue(row, 13, if (it.heating) "+" else "-")
                    setCellValue(row, 14, if (it.grounding) "+" else "-")
                    setCellValue(row, 15, if (it.lastRevisionDate != 0L) dateFormat.format(Date(it.lastRevisionDate)) else "")
                    setCellValue(row, 16, if (it.nextRevisionDate != 0L) dateFormat.format(Date(it.nextRevisionDate)) else "")
                    setCellValue(row, 17, it.schemeNumber ?: "")
                }
            }
        }

        // Колонка S (18) — примечания
        val notesText = when {
            !isFound -> "⚠️ Нет в отчёте"
            isNew -> {
                val baseNotes = if (notesList.isNotEmpty()) notesList.joinToString(", ") else ""
                if (baseNotes.isNotEmpty()) "➕ Добавлено из отчёта, $baseNotes"
                else "➕ Добавлено из отчёта"
            }
            else -> notesList.joinToString(", ")
        }
        setCellValue(row, 18, notesText)

        // Красим строку
        if (!isFound) {
            fillRowWithColor(workbook, row, YELLOW_COLOR)
        } else if (isNew) {
            fillRowWithColor(workbook, row, GREEN_COLOR)
        }
    }

    private fun setCellValue(row: Row, colIndex: Int, value: String) {
        val cell = row.getCell(colIndex) ?: row.createCell(colIndex)
        cell.setCellValue(value)
    }

    private fun fillRowWithColor(workbook: XSSFWorkbook, row: Row, color: Short) {
        val fillStyle = workbook.createCellStyle().apply {
            fillForegroundColor = color
            fillPattern = FillPatternType.SOLID_FOREGROUND
        }
        for (col in 0..18) {
            val cell = row.getCell(col) ?: row.createCell(col)
            val existingStyle = cell.cellStyle
            val newStyle = workbook.createCellStyle()
            if (existingStyle != null) {
                newStyle.cloneStyleFrom(existingStyle)
            }
            newStyle.fillForegroundColor = color
            newStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
            cell.cellStyle = newStyle
        }
    }

    private fun normalizeName(name: String): String {
        return name
            .lowercase()
            .replace("\\s+".toRegex(), " ")
            .replace("ё", "е")
            .replace("  ", " ")
            .trim()
    }

    enum class TableType {
        ORU,
        SHAOT
    }
}