package com.example.myschemes.utils

import android.content.Context
import com.example.myschemes.data.model.Scheme
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object DefectsExporter {

    fun export(
        context: Context,
        schemes: List<Scheme>
    ): File? {
        return try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Дефекты")

            // ===== СТИЛЬ ЗАГОЛОВКОВ =====
            val headerFont = workbook.createFont().apply {
                bold = true
                color = IndexedColors.WHITE.index
                fontHeightInPoints = 11
            }
            val headerStyle = workbook.createCellStyle().apply {
                fillForegroundColor = IndexedColors.DARK_GREEN.index
                fillPattern = FillPatternType.SOLID_FOREGROUND
                setFont(headerFont)
                alignment = HorizontalAlignment.CENTER
                verticalAlignment = VerticalAlignment.CENTER
            }

            // ===== СТИЛЬ ДЛЯ ЯЧЕЕК С ПЕРЕНОСОМ =====
            val wrapStyle = workbook.createCellStyle().apply {
                wrapText = true
                verticalAlignment = VerticalAlignment.TOP
            }

            // ===== СТИЛЬ ДЛЯ НАЗВАНИЯ ШКАФА (жирный) =====
            val equipmentNameFont = workbook.createFont().apply {
                bold = true
            }
            val equipmentNameStyle = workbook.createCellStyle().apply {
                setFont(equipmentNameFont)
                verticalAlignment = VerticalAlignment.TOP
            }

            // ===== ЗАГОЛОВКИ =====
            val headers = listOf(
                "Наименование оборудования",
                "Пункт чек-листа",
                "Примечание"
            )

            val headerRow = sheet.createRow(0)
            headers.forEachIndexed { index, header ->
                val cell = headerRow.createCell(index)
                cell.setCellValue(header)
                cell.cellStyle = headerStyle
            }

            // ===== ШИРИНА КОЛОНОК =====
            sheet.setColumnWidth(0, 9000)
            sheet.setColumnWidth(1, 6000)
            sheet.setColumnWidth(2, 12000)

            // ===== ДАННЫЕ =====
            var rowIndex = 1

            schemes.forEach { scheme ->
                // Собираем все примечания
                val defects = mutableListOf<Pair<String, String>>()

                if (!scheme.cabinetNameNote.isNullOrBlank())
                    defects.add("Диспетчерское наименование" to scheme.cabinetNameNote)
                if (!scheme.switchesNameNote.isNullOrBlank())
                    defects.add("ДН автоматов, рубильников" to scheme.switchesNameNote)
                if (!scheme.inventoryNumberNote.isNullOrBlank())
                    defects.add("Инвентарный номер" to scheme.inventoryNumberNote)
                if (!scheme.lockIntegrityNote.isNullOrBlank())
                    defects.add("Целостность замков" to scheme.lockIntegrityNote)
                if (!scheme.sealIntegrityNote.isNullOrBlank())
                    defects.add("Уплотнение шкафа" to scheme.sealIntegrityNote)
                if (!scheme.cableEntriesNote.isNullOrBlank())
                    defects.add("Заходы кабелей" to scheme.cableEntriesNote)
                if (!scheme.noBareWiresNote.isNullOrBlank())
                    defects.add("Нет оголённых жил" to scheme.noBareWiresNote)
                if (!scheme.addressLabelsNote.isNullOrBlank())
                    defects.add("Адресные бирки" to scheme.addressLabelsNote)
                if (!scheme.terminalsIntegrityNote.isNullOrBlank())
                    defects.add("Целостность клеммников" to scheme.terminalsIntegrityNote)
                if (!scheme.paintingNote.isNullOrBlank())
                    defects.add("Окраска" to scheme.paintingNote)
                if (!scheme.heatingNote.isNullOrBlank())
                    defects.add("Обогрев" to scheme.heatingNote)
                if (!scheme.groundingNote.isNullOrBlank())
                    defects.add("Заземление" to scheme.groundingNote)

                // Записываем: название шкафа только для первой строки
                defects.forEachIndexed { index, (checkpoint, note) ->
                    val row = sheet.createRow(rowIndex++)

                    // Название шкафа — только в первой строке
                    if (index == 0) {
                        val nameCell = row.createCell(0)
                        nameCell.setCellValue(scheme.equipmentName)
                        nameCell.cellStyle = equipmentNameStyle
                    } else {
                        row.createCell(0).setCellValue("")
                    }

                    row.createCell(1).setCellValue(checkpoint)

                    val noteCell = row.createCell(2)
                    noteCell.setCellValue(note)
                    noteCell.cellStyle = wrapStyle
                }
            }

            // ===== СОХРАНЕНИЕ =====
            val timeStamp = SimpleDateFormat("dd.MM.yyyy_HH-mm", Locale.getDefault()).format(Date())
            val fileName = "Дефекты_$timeStamp.xlsx"
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
}