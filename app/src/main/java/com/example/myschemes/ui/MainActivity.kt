package com.example.myschemes.ui

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.*
import com.example.myschemes.R
import com.example.myschemes.data.database.SchemeDatabase
import com.example.myschemes.data.model.Scheme
import com.example.myschemes.data.repository.SchemeRepository
import com.example.myschemes.utils.WorkReportExporter
import com.example.myschemes.worker.NotificationWorker
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*
import androidx.appcompat.app.AlertDialog
import java.io.File
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.VerticalAlignment
import com.example.myschemes.utils.DatabaseBackupHelper
import kotlinx.coroutines.withContext
import com.example.myschemes.utils.DefectsExporter

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SchemeAdapter
    private lateinit var tvCounter: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var repository: SchemeRepository
    private var currentSortMode = 0

    private var searchQuery: String = ""

    private var currentCellFilter: String? = null

    companion object {
        private const val REQUEST_IMPORT_EXCEL = 1002
        private const val REQUEST_IMPORT_DB = 1003
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setTitleTextColor(Color.BLACK)
        supportActionBar?.title = "📋 Мои шкафы"

        recyclerView = findViewById(R.id.rvSchemes)
        tvCounter = findViewById(R.id.tvCounter)
        tvEmpty = findViewById(R.id.tvEmpty)

        val database = SchemeDatabase.getInstance(this)
        repository = SchemeRepository(database.schemeDao())

        adapter = SchemeAdapter(
            schemes = emptyList(),
            onItemClick = { scheme ->
                val intent = Intent(this, CabinetDetailActivity::class.java)
                intent.putExtra("scheme_id", scheme.id)
                startActivity(intent)
            },
            onAllPhotosClick = { scheme ->
                showAllPhotosForScheme(scheme)
            },
            onInspectedClick = { scheme, newState ->
                updateInspectedStatus(scheme, newState)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        loadSchemes()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        setupNotifications()

        if (isFirstRun()) {
            val immediateWork = OneTimeWorkRequestBuilder<NotificationWorker>()
                .build()
            WorkManager.getInstance(this).enqueue(immediateWork)
        }
    }

    override fun onResume() {
        super.onResume()
        loadSchemes()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)

        val searchItem = menu?.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? androidx.appcompat.widget.SearchView

        searchView?.queryHint = "Поиск по шкафу, ячейке, схеме..."
        searchView?.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchQuery = query ?: ""
                loadSchemes()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchQuery = newText ?: ""
                loadSchemes()
                return true
            }
        })

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_export_defects -> {
                exportDefects()
                true
            }
            R.id.action_export_db -> {
                exportDatabase()
                true
            }
            R.id.action_import_db -> {
                importDatabase()
                true
            }
            R.id.action_import_excel -> {
                importFromExcel()
                true
            }
            R.id.action_sort -> {
                showSortDialog()
                true
            }
            R.id.action_export_report -> {
                exportReport()
                true
            }
            R.id.action_export_work -> {
                exportWorkReport()
                true
            }
            R.id.action_add_scheme -> {
                showAddSchemeDialog()
                true
            }
            R.id.action_reset_inspected -> {
                showResetInspectedDialog()
                true
            }
            R.id.action_test_notification -> {
                android.util.Log.d("MainActivity", "Нажата кнопка теста уведомления")
                val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                    .setInitialDelay(1, TimeUnit.SECONDS)
                    .build()
                WorkManager.getInstance(this).enqueue(workRequest)
                Toast.makeText(this, "Отправка тестового уведомления...", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_notification_settings -> {
                showNotificationSettingsDialog()
                true
            }
            R.id.action_filter_by_cell -> {
                showCellFilterDialog()
                true
            }
            R.id.action_clear_filter -> {
                clearCellFilter()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // ==================== ЭКСПОРТ В РАБОЧИЙ ФОРМАТ ====================

    private fun exportWorkReport() {
        lifecycleScope.launch {
            try {
                val schemes = repository.getAllSchemes()
                if (schemes.isEmpty()) {
                    Toast.makeText(this@MainActivity, "Нет данных для экспорта", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                Toast.makeText(this@MainActivity, "Формирую отчёт...", Toast.LENGTH_SHORT).show()

                val reportFile = WorkReportExporter.export(this@MainActivity, schemes)

                if (reportFile != null) {
                    saveToDownloads(reportFile)
                } else {
                    Toast.makeText(this@MainActivity, "Ошибка создания отчёта", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveToDownloads(sourceFile: File) {
        try {
            val fileName = sourceFile.name

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, getMimeType(fileName))  // ← динамически
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                )

                if (uri != null) {
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        sourceFile.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    Toast.makeText(
                        this,
                        "✅ Сохранено в Загрузки: $fileName",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(this, "Не удалось сохранить файл", Toast.LENGTH_LONG).show()
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                )
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val destFile = File(downloadsDir, fileName)
                sourceFile.copyTo(destFile, overwrite = true)
                Toast.makeText(
                    this,
                    "✅ Сохранено в Загрузки: $fileName",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Ошибка сохранения: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Вспомогательный метод
    private fun getMimeType(fileName: String): String {
        return when {
            fileName.endsWith(".zip", ignoreCase = true) -> "application/zip"
            fileName.endsWith(".xlsx", ignoreCase = true) ->
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            fileName.endsWith(".xls", ignoreCase = true) -> "application/vnd.ms-excel"
            else -> "application/octet-stream"
        }
    }

    // ==================== ДИАЛОГИ И МЕНЮ ====================

    private fun showAddSchemeDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_scheme, null)
        val etEquipmentName = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etEquipmentName)
        val etCellNumber = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etCellNumber)
        val etSchemeNumber = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etSchemeNumber)
        val etLastRevisionDate = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etLastRevisionDate)

        AlertDialog.Builder(this)
            .setTitle("➕ Добавить шкаф")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val equipmentName = etEquipmentName.text.toString().trim()
                val cellNumber = etCellNumber.text.toString().trim().takeIf { it.isNotEmpty() }
                val schemeNumber = etSchemeNumber.text.toString().trim().takeIf { it.isNotEmpty() }
                val lastRevisionDateStr = etLastRevisionDate.text.toString().trim()

                if (equipmentName.isEmpty()) {
                    Toast.makeText(this, "Введите наименование шкафа", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val lastRevisionDate = if (lastRevisionDateStr.isNotEmpty()) {
                    try {
                        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                        dateFormat.parse(lastRevisionDateStr)?.time ?: 0L
                    } catch (e: Exception) {
                        0L
                    }
                } else {
                    0L
                }

                val nextRevisionDate = if (lastRevisionDate != 0L) {
                    val calendar = Calendar.getInstance().apply {
                        timeInMillis = lastRevisionDate
                        add(Calendar.YEAR, 3)
                    }
                    calendar.timeInMillis
                } else {
                    0L
                }

                lifecycleScope.launch {
                    val allSchemes = repository.getAllSchemes().toMutableList()
                    val newId = (allSchemes.maxOfOrNull { it.id } ?: 0) + 1
                    val newScheme = Scheme(
                        id = newId,
                        itemNumber = allSchemes.size + 1,
                        cellNumber = cellNumber,
                        equipmentName = equipmentName,
                        lastRevisionDate = lastRevisionDate,
                        nextRevisionDate = nextRevisionDate,
                        schemeNumber = schemeNumber
                    )
                    allSchemes.add(newScheme)
                    repository.saveSchemes(allSchemes)
                    loadSchemes()
                    Toast.makeText(this@MainActivity, "Шкаф добавлен", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showNotificationSettingsDialog() {
        val hours = arrayOf("9:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00")
        AlertDialog.Builder(this)
            .setTitle("Настройка времени уведомлений")
            .setItems(hours) { _, which ->
                val selectedHour = which + 9
                saveNotificationTime(selectedHour)
                Toast.makeText(this, "Уведомления будут приходить в ${selectedHour}:00", Toast.LENGTH_SHORT).show()
                setupNotifications()
            }
            .show()
    }

    private fun saveNotificationTime(hour: Int) {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        prefs.edit().putInt("notification_hour", hour).apply()
    }

    private fun getNotificationHour(): Int {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        return prefs.getInt("notification_hour", 9)
    }

    private fun setupNotifications() {
        val notificationHour = getNotificationHour()
        val startTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, notificationHour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val initialDelay = startTime.timeInMillis - System.currentTimeMillis()

        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
            1, TimeUnit.DAYS
        ).setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "schemes_notification",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    private fun loadSchemes() {
        lifecycleScope.launch {
            val schemes = repository.getAllSchemes()
            val sortedSchemes = sortSchemes(schemes)

            if (sortedSchemes.isEmpty()) {
                recyclerView.visibility = android.view.View.GONE
                tvEmpty.visibility = android.view.View.VISIBLE

                if (searchQuery.isNotBlank()) {
                    tvEmpty.text = "🔍 Ничего не найдено по запросу \"$searchQuery\""
                } else {
                    tvEmpty.text = "📋 Список шкафов пуст"
                }
                tvCounter.text = "Всего шкафов: 0"
            } else {
                recyclerView.visibility = android.view.View.VISIBLE
                tvEmpty.visibility = android.view.View.GONE
                adapter.updateData(sortedSchemes)

                val filterParts = mutableListOf<String>()
                if (searchQuery.isNotBlank()) filterParts.add("поиск: \"$searchQuery\"")
                if (currentCellFilter != null) filterParts.add("ячейка: $currentCellFilter")

                val filterText = if (filterParts.isNotEmpty()) {
                    " (${filterParts.joinToString(", ")})"
                } else ""

                tvCounter.text = "Всего шкафов: ${sortedSchemes.size}$filterText"
            }
        }
    }

    private fun isFirstRun(): Boolean {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isFirst = prefs.getBoolean("is_first_run", true)
        if (isFirst) {
            prefs.edit().putBoolean("is_first_run", false).apply()
        }
        return isFirst
    }

    // ==================== СТАНДАРТНЫЙ ЭКСПОРТ ОТЧЁТА ====================

    private fun exportReport() {
        lifecycleScope.launch {
            val schemes = repository.getAllSchemes()
            if (schemes.isEmpty()) {
                Toast.makeText(this@MainActivity, "Нет данных для экспорта", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val excelFile = createExcelFile(schemes)
            if (excelFile != null) {
                shareFile(excelFile)
            } else {
                Toast.makeText(this@MainActivity, "Ошибка создания файла отчёта", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createExcelFile(schemes: List<Scheme>): File? {
        return try {
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val timeStamp = SimpleDateFormat("dd.MM.yyyy_HH-mm", Locale.getDefault()).format(Date())
            val fileName = "Осмотр ШКС_$timeStamp.xlsx"
            val file = File(cacheDir, fileName)

            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Шкафы")

            val headerFont = workbook.createFont().apply {
                bold = true
                color = IndexedColors.WHITE.index
                fontHeightInPoints = 10
            }
            val headerStyle = workbook.createCellStyle().apply {
                fillForegroundColor = IndexedColors.DARK_GREEN.index
                fillPattern = FillPatternType.SOLID_FOREGROUND
                setFont(headerFont)
                alignment = HorizontalAlignment.CENTER
                verticalAlignment = VerticalAlignment.BOTTOM
                wrapText = true
                rotation = 90.toShort()
            }

            val plusFont = workbook.createFont().apply {
                bold = true
                color = IndexedColors.GREEN.index
            }
            val plusStyle = workbook.createCellStyle().apply {
                setFont(plusFont)
                alignment = HorizontalAlignment.CENTER
                verticalAlignment = VerticalAlignment.CENTER
            }

            val minusFont = workbook.createFont().apply {
                bold = true
                color = IndexedColors.RED.index
            }
            val minusStyle = workbook.createCellStyle().apply {
                setFont(minusFont)
                alignment = HorizontalAlignment.CENTER
                verticalAlignment = VerticalAlignment.CENTER
            }

            val wrapStyle = workbook.createCellStyle().apply {
                wrapText = true
                verticalAlignment = VerticalAlignment.TOP
            }

            val headers = listOf(
                "Наименование оборудования",
                "Ячейка",
                "Диспетчерское наименование", "Прим.",
                "ДН автоматов, рубильников", "Прим.",
                "Инвентарный номер", "Прим.",
                "Целостность замков", "Прим.",
                "Уплотнение шкафа", "Прим.",
                "Заходы кабелей", "Прим.",
                "Нет оголённых жил", "Прим.",
                "Адресные бирки", "Прим.",
                "Целостность клеммников", "Прим.",
                "Окраска", "Прим.",
                "Обогрев", "Прим.",
                "Заземление", "Прим.",
                "Номер схемы",
                "Дата схемы",
                "Дата пересмотра"
            )

            val headerRow = sheet.createRow(0)
            headerRow.heightInPoints = 150f

            headers.forEachIndexed { index, header ->
                val cell = headerRow.createCell(index)
                cell.setCellValue(header)
                cell.cellStyle = headerStyle
            }

            headers.forEachIndexed { index, header ->
                val width = when {
                    header == "Наименование оборудования" -> 9000
                    header == "Ячейка" -> 4500
                    header == "Прим." -> 6000
                    header == "Номер схемы" -> 4000
                    header == "Дата схемы" || header == "Дата пересмотра" -> 4000
                    else -> 1800
                }
                sheet.setColumnWidth(index, width)
            }

            schemes.forEachIndexed { rowIndex, scheme ->
                val row = sheet.createRow(rowIndex + 1)
                var colIndex = 0

                row.createCell(colIndex++).setCellValue(scheme.equipmentName)
                row.createCell(colIndex++).setCellValue(scheme.cellNumber ?: "")

                fun addStatusAndNote(checked: Boolean, note: String?) {
                    val statusCell = row.createCell(colIndex++)
                    statusCell.setCellValue(if (checked) "+" else "-")
                    statusCell.cellStyle = if (checked) plusStyle else minusStyle

                    val noteCell = row.createCell(colIndex++)
                    noteCell.setCellValue(note ?: "")
                    noteCell.cellStyle = wrapStyle
                }

                addStatusAndNote(scheme.cabinetNameChecked, scheme.cabinetNameNote)
                addStatusAndNote(scheme.switchesNameChecked, scheme.switchesNameNote)
                addStatusAndNote(scheme.inventoryNumberChecked, scheme.inventoryNumberNote)
                addStatusAndNote(scheme.lockIntegrity, scheme.lockIntegrityNote)
                addStatusAndNote(scheme.sealIntegrity, scheme.sealIntegrityNote)
                addStatusAndNote(scheme.cableEntries, scheme.cableEntriesNote)
                addStatusAndNote(scheme.noBareWires, scheme.noBareWiresNote)
                addStatusAndNote(scheme.addressLabels, scheme.addressLabelsNote)
                addStatusAndNote(scheme.terminalsIntegrity, scheme.terminalsIntegrityNote)
                addStatusAndNote(scheme.painting, scheme.paintingNote)
                addStatusAndNote(scheme.heating, scheme.heatingNote)
                addStatusAndNote(scheme.grounding, scheme.groundingNote)

                row.createCell(colIndex++).setCellValue(scheme.schemeNumber ?: "")
                row.createCell(colIndex++).setCellValue(
                    if (scheme.lastRevisionDate != 0L) dateFormat.format(Date(scheme.lastRevisionDate)) else ""
                )
                row.createCell(colIndex).setCellValue(
                    if (scheme.nextRevisionDate != 0L) dateFormat.format(Date(scheme.nextRevisionDate)) else ""
                )
            }

            workbook.write(file.outputStream())
            workbook.close()

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun shareFile(file: File) {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        )

        // Определяем тип по расширению
        val mimeType = getMimeType(file.name)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Сохранить отчёт"))
    }

    // ==================== ИМПОРТ ИЗ EXCEL ====================

    private fun importFromExcel() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(Intent.createChooser(intent, "Выберите Excel-файл"), REQUEST_IMPORT_EXCEL)
    }

    private fun processExcelImport(uri: Uri) {
        lifecycleScope.launch {
            try {
                val existingSchemes = repository.getAllSchemes()
                val existingNames = existingSchemes.map { it.equipmentName }.toSet()

                val newSchemes = mutableListOf<Scheme>()
                var nextId = (existingSchemes.maxOfOrNull { it.id } ?: 0) + 1
                var nextItemNumber = (existingSchemes.maxOfOrNull { it.itemNumber ?: 0 } ?: 0) + 1

                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val workbook = XSSFWorkbook(inputStream)
                    val sheet = workbook.getSheetAt(0)

                    var rowIndex = 0
                    for (row in sheet) {
                        rowIndex++

                        val cellNumberCell = row.getCell(0)
                        val equipmentNameCell = row.getCell(1)

                        val cellNumber = when (cellNumberCell?.cellType) {
                            CellType.STRING -> cellNumberCell.stringCellValue.trim().takeIf { it.isNotBlank() }
                            else -> null
                        }

                        val equipmentName = when (equipmentNameCell?.cellType) {
                            CellType.STRING -> equipmentNameCell.stringCellValue.trim()
                            else -> continue
                        }

                        if (equipmentName.isEmpty()) continue
                        if (existingNames.contains(equipmentName)) continue

                        newSchemes.add(
                            Scheme(
                                id = nextId,
                                itemNumber = nextItemNumber,
                                cellNumber = cellNumber,
                                equipmentName = equipmentName,
                                lastRevisionDate = 0L,
                                nextRevisionDate = 0L,
                                schemeNumber = null
                            )
                        )
                        nextId++
                        nextItemNumber++
                    }
                    workbook.close()
                }

                if (newSchemes.isNotEmpty()) {
                    val allSchemes = existingSchemes.toMutableList()
                    allSchemes.addAll(newSchemes)
                    repository.saveSchemes(allSchemes)
                    loadSchemes()
                    Toast.makeText(this@MainActivity, "Добавлено ${newSchemes.size} новых шкафов", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@MainActivity, "Нет новых шкафов для добавления", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, "Ошибка импорта: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_IMPORT_EXCEL -> {
                    data?.data?.let { uri -> processExcelImport(uri) }
                }
                REQUEST_IMPORT_DB -> {
                    data?.data?.let { uri -> processDatabaseImport(uri) }
                }
            }
        }
    }

    // ==================== СОРТИРОВКА ====================

    private fun showSortDialog() {
        val sortOptions = arrayOf(
            "📛 По наименованию (А-Я)",
            "📅 По дате пересмотра (сначала срочные)",
            "🔴 По статусу (просрочены → скоро → активны → нет схемы)",
            "🗄️ По ячейке",
            "✅ Сначала неосмотренные"
        )

        AlertDialog.Builder(this)
            .setTitle("Сортировка")
            .setItems(sortOptions) { _, which ->
                currentSortMode = which
                loadSchemes()
            }
            .show()
    }

    private fun sortSchemes(schemes: List<Scheme>): List<Scheme> {
        val searched = if (searchQuery.isNotBlank()) {
            val q = searchQuery.lowercase().trim()
            schemes.filter { scheme ->
                scheme.equipmentName.lowercase().contains(q) ||
                        (scheme.cellNumber?.lowercase()?.contains(q) == true) ||
                        (scheme.schemeNumber?.lowercase()?.contains(q) == true)
            }
        } else {
            schemes
        }

        val filtered = if (currentCellFilter != null) {
            searched.filter { it.cellNumber == currentCellFilter }
        } else {
            searched
        }

        return when (currentSortMode) {
            0 -> filtered.sortedBy { it.equipmentName.lowercase() }
            1 -> filtered.sortedBy {
                if (it.nextRevisionDate == 0L) Long.MAX_VALUE else it.nextRevisionDate
            }
            2 -> filtered.sortedWith(compareBy(
                { it.getStatus().ordinal },
                { it.equipmentName.lowercase() }
            ))
            3 -> filtered.sortedWith(compareBy(
                { it.cellNumber ?: "" },
                { it.equipmentName.lowercase() }
            ))
            4 -> filtered.sortedWith(compareBy(
                { if (it.isInspected) 1 else 0 },
                { it.equipmentName.lowercase() }
            ))
            else -> filtered
        }
    }

    // ==================== ФОТО ====================

    private fun showAllPhotosForScheme(scheme: Scheme) {
        val allPhotos = getAllPhotosFromScheme(scheme).toMutableList()
        if (allPhotos.isNotEmpty()) {
            val dialog = PhotoGalleryDialog(
                activity = this,
                title = "Все фото: ${scheme.equipmentName}",
                photos = allPhotos,
                onPhotosChanged = { }
            )
            dialog.show(scheme.equipmentName, "Все фото")
        } else {
            Toast.makeText(this, "Нет фото для этого шкафа", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getAllPhotosFromScheme(scheme: Scheme): List<String> {
        val allPhotos = mutableListOf<String>()
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
        return allPhotos
    }

    // ==================== ОТМЕТКИ ОСМОТРА ====================

    private fun updateInspectedStatus(scheme: Scheme, isInspected: Boolean) {
        lifecycleScope.launch {
            val allSchemes = repository.getAllSchemes().toMutableList()
            val index = allSchemes.indexOfFirst { it.id == scheme.id }
            if (index != -1) {
                val updated = allSchemes[index].copy(isInspected = isInspected)
                allSchemes[index] = updated
                repository.saveSchemes(allSchemes)
                loadSchemes()
                val message = if (isInspected) "✓ осмотрено" else "✗ не осмотрено"
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showResetInspectedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Сбросить отметки")
            .setMessage("Вы уверены, что хотите снять все отметки \"Осмотрено\" со всех шкафов?")
            .setPositiveButton("Да, сбросить") { _, _ ->
                resetAllInspected()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun resetAllInspected() {
        lifecycleScope.launch {
            val allSchemes = repository.getAllSchemes().toMutableList()
            val updatedSchemes = allSchemes.map { it.copy(isInspected = false) }
            repository.saveSchemes(updatedSchemes)
            loadSchemes()
            Toast.makeText(this@MainActivity, "Все отметки сброшены", Toast.LENGTH_SHORT).show()
        }
    }

    // ==================== ФИЛЬТР ПО ЯЧЕЙКЕ ====================

    private fun showCellFilterDialog() {
        lifecycleScope.launch {
            val allSchemes = repository.getAllSchemes()
            val cells = allSchemes.mapNotNull { it.cellNumber?.takeIf { it.isNotBlank() } }.distinct().sorted()

            if (cells.isEmpty()) {
                Toast.makeText(this@MainActivity, "Нет шкафов с указанными ячейками", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val cellOptions = listOf("Все ячейки") + cells
            AlertDialog.Builder(this@MainActivity)
                .setTitle("Выберите ячейку")
                .setItems(cellOptions.toTypedArray()) { _, which ->
                    if (which == 0) {
                        clearCellFilter()
                    } else {
                        currentCellFilter = cellOptions[which]
                        loadSchemes()
                        supportActionBar?.subtitle = "Ячейка: ${currentCellFilter}"
                    }
                }
                .show()
        }
    }

    private fun clearCellFilter() {
        currentCellFilter = null
        loadSchemes()
        supportActionBar?.subtitle = null
    }

    // ==================== ЭКСПОРТ БД ====================

    private fun exportDatabase() {
        lifecycleScope.launch {
            try {
                val schemes = repository.getAllSchemes()
                if (schemes.isEmpty()) {
                    Toast.makeText(this@MainActivity, "Нет данных для экспорта", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                Toast.makeText(this@MainActivity, "Формирую архив...", Toast.LENGTH_SHORT).show()

                val timeStamp = SimpleDateFormat("dd.MM.yyyy_HH-mm", Locale.getDefault()).format(Date())
                val fileName = "MySchemes_backup_$timeStamp.zip"
                val tempFile = File(cacheDir, fileName)

                val success = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    DatabaseBackupHelper.exportBackup(this@MainActivity, schemes, tempFile)
                }

                if (success) {
                    saveToDownloads(tempFile)
                } else {
                    Toast.makeText(this@MainActivity, "Ошибка создания архива", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

// ==================== ИМПОРТ БД ====================

    private fun importDatabase() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Импорт базы данных")
            .setMessage("Все текущие шкафы и фото будут ЗАМЕНЕНЫ данными из архива.\n\nПродолжить?")
            .setPositiveButton("Да, заменить") { _, _ ->
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "application/zip"
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                startActivityForResult(
                    Intent.createChooser(intent, "Выберите архив"),
                    REQUEST_IMPORT_DB
                )
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun processDatabaseImport(uri: Uri) {
        lifecycleScope.launch {
            try {
                Toast.makeText(this@MainActivity, "Восстанавливаю...", Toast.LENGTH_SHORT).show()

                val result = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    DatabaseBackupHelper.importBackup(this@MainActivity, uri)
                }

                when (result) {
                    is DatabaseBackupHelper.ImportResult.Success -> {
                        // Очищаем текущую БД и загружаем импортированные данные
                        repository.saveSchemes(result.schemes)
                        loadSchemes()
                        Toast.makeText(
                            this@MainActivity,
                            "✅ Восстановлено шкафов: ${result.schemes.size}, фото: ${result.photosRestored}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    is DatabaseBackupHelper.ImportResult.Error -> {
                        Toast.makeText(
                            this@MainActivity,
                            "❌ ${result.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ==================== ВЫГРУЗКА ДЕФЕКТОВ ====================

    private fun exportDefects() {
        lifecycleScope.launch {
            try {
                val schemes = repository.getAllSchemes()
                if (schemes.isEmpty()) {
                    Toast.makeText(this@MainActivity, "Нет данных для экспорта", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                Toast.makeText(this@MainActivity, "Формирую список дефектов...", Toast.LENGTH_SHORT).show()

                val reportFile = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    DefectsExporter.export(this@MainActivity, schemes)
                }

                if (reportFile != null) {
                    shareFile(reportFile)   // ← теперь через "Поделиться"
                } else {
                    Toast.makeText(this@MainActivity, "Ошибка создания файла", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}