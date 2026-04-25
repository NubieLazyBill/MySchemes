package com.example.myschemes.ui

import android.content.Intent
import android.os.Bundle
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
import com.example.myschemes.utils.CsvImporter
import com.example.myschemes.worker.NotificationWorker
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import android.Manifest
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
import androidx.activity.result.contract.ActivityResultContracts
import org.apache.poi.ss.usermodel.CellType

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SchemeAdapter
    private lateinit var tvCounter: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var repository: SchemeRepository
    private var currentSortMode = 0

    companion object {
        private const val REQUEST_IMPORT_EXCEL = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
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

        adapter = SchemeAdapter(emptyList()) { scheme ->
            val intent = Intent(this, CabinetDetailActivity::class.java)
            intent.putExtra("scheme_id", scheme.id)
            startActivity(intent)
        }

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
        loadSchemes()  // ← добавляем эту строку
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
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
            R.id.action_add_scheme -> {
                showAddSchemeDialog()
                true
            }
            R.id.action_import_csv -> {
                importSchemes()  // ← вызывает диалог подтверждения
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
            else -> super.onOptionsItemSelected(item)
        }
    }

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

                // Дата схемы теперь необязательная
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

                // Если дата схемы не указана, дата пересмотра тоже будет 0
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
                tvCounter.text = "Всего шкафов: 0"
            } else {
                recyclerView.visibility = android.view.View.VISIBLE
                tvEmpty.visibility = android.view.View.GONE
                adapter.updateData(sortedSchemes)
                tvCounter.text = "Всего шкафов: ${sortedSchemes.size}"
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

    private fun importSchemes() {
        AlertDialog.Builder(this)
            .setTitle("📥 Импорт из CSV")
            .setMessage("Внимание! Импорт заменит все текущие схемы данными из файла schemes.csv. Продолжить?")
            .setPositiveButton("Да, импортировать") { _, _ ->
                performImport()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun performImport() {
        lifecycleScope.launch {
            try {
                val importer = CsvImporter(applicationContext)
                val schemes = importer.importFromAssets()
                if (schemes.isNotEmpty()) {
                    repository.saveSchemes(schemes)
                    loadSchemes()
                    Toast.makeText(this@MainActivity, "Импортировано ${schemes.size} схем", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Нет данных для импорта", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, "Ошибка импорта: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

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
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())  // ← объявляем в самом начале
            val timeStamp = SimpleDateFormat("dd.MM.yyyy_HH-mm", Locale.getDefault()).format(Date())
            val fileName = "Осмотр ШКС_$timeStamp.xlsx"
            val file = File(cacheDir, fileName)

            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Шкафы")

            // Стиль для заголовков
            val headerFont = workbook.createFont().apply {
                bold = true
                color = IndexedColors.WHITE.index
            }
            val headerStyle = workbook.createCellStyle().apply {
                fillForegroundColor = IndexedColors.LIGHT_GREEN.index
                fillPattern = FillPatternType.SOLID_FOREGROUND
                setFont(headerFont)
            }

            // Стиль для статусов
            val plusFont = workbook.createFont().apply {
                bold = true
                color = IndexedColors.GREEN.index
            }
            val plusStyle = workbook.createCellStyle().apply {
                setFont(plusFont)
            }

            val minusFont = workbook.createFont().apply {
                bold = true
                color = IndexedColors.RED.index
            }
            val minusStyle = workbook.createCellStyle().apply {
                setFont(minusFont)
            }

            // Заголовки
            val headers = listOf(
                "Наименование оборудования",
                "Ячейка",
                "Диспетчерское наименование",
                "ДН автоматов, рубильников",
                "Инвентарный номер",
                "Целостность замков",
                "Уплотнение шкафа",
                "Заходы кабелей",
                "Нет оголённых жил",
                "Адресные бирки",
                "Целостность клеммников",
                "Окраска",
                "Обогрев",
                "Заземление",
                "Примечание",
                "Номер схемы",
                "Дата схемы",
                "Дата пересмотра"
            )

            val headerRow = sheet.createRow(0)
            headers.forEachIndexed { index, header ->
                val cell = headerRow.createCell(index)
                cell.setCellValue(header)
                cell.cellStyle = headerStyle
                sheet.setColumnWidth(index, (header.length * 256).coerceIn(3000, 15000))
            }

            // Данные
            schemes.forEachIndexed { rowIndex, scheme ->
                val row = sheet.createRow(rowIndex + 1)
                var colIndex = 0

                // Основные поля
                row.createCell(colIndex++).setCellValue(scheme.equipmentName)
                row.createCell(colIndex++).setCellValue(scheme.cellNumber ?: "")

                // Пункты чек-листа
                listOf(
                    scheme.cabinetNameChecked,
                    scheme.switchesNameChecked,
                    scheme.inventoryNumberChecked,
                    scheme.lockIntegrity,
                    scheme.sealIntegrity,
                    scheme.cableEntries,
                    scheme.noBareWires,
                    scheme.addressLabels,
                    scheme.terminalsIntegrity,
                    scheme.painting,
                    scheme.heating,
                    scheme.grounding
                ).forEach { checked ->
                    val cell = row.createCell(colIndex++)
                    cell.setCellValue(if (checked) "+" else "-")
                    cell.cellStyle = if (checked) plusStyle else minusStyle
                }

                // Примечание
                val notes = listOfNotNull(
                    scheme.cabinetNameNote, scheme.switchesNameNote, scheme.inventoryNumberNote,
                    scheme.lockIntegrityNote, scheme.sealIntegrityNote, scheme.cableEntriesNote,
                    scheme.noBareWiresNote, scheme.addressLabelsNote, scheme.terminalsIntegrityNote,
                    scheme.paintingNote, scheme.heatingNote, scheme.groundingNote
                ).filter { it.isNotBlank() }
                row.createCell(colIndex++).setCellValue(notes.joinToString("; "))

                // Поля схемы (в конец)
                row.createCell(colIndex++).setCellValue(scheme.schemeNumber ?: "")
                row.createCell(colIndex++).setCellValue(if (scheme.lastRevisionDate != 0L) dateFormat.format(Date(scheme.lastRevisionDate)) else "")
                row.createCell(colIndex).setCellValue(if (scheme.nextRevisionDate != 0L) dateFormat.format(Date(scheme.nextRevisionDate)) else "")
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
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Сохранить отчёт"))
    }

    //Иморт из Excell
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
                var nextId = (existingSchemes.maxOfOrNull { it.id } ?: 0) + 1  // ← var вместо val
                var nextItemNumber = (existingSchemes.maxOfOrNull { it.itemNumber ?: 0 } ?: 0) + 1  // ← var вместо val

                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val workbook = XSSFWorkbook(inputStream)
                    val sheet = workbook.getSheetAt(0)

                    var rowIndex = 0
                    for (row in sheet) {
                        rowIndex++
                        //Пропускаем первую строку (заголовки)
                        //if (rowIndex == 1) continue // пропускаем заголовки

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
        if (requestCode == REQUEST_IMPORT_EXCEL && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                processExcelImport(uri)
            }
        }
    }
    private fun showSortDialog() {
        val sortOptions = arrayOf(
            "📛 По наименованию (А-Я)",
            "📅 По дате пересмотра (сначала срочные)",
            "🔴 По статусу (просрочены → скоро → активны → нет схемы)",
            "🗄️ По ячейке"
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
        return when (currentSortMode) {
            0 -> schemes.sortedBy { it.equipmentName.lowercase() }
            1 -> schemes.sortedBy {
                if (it.nextRevisionDate == 0L) Long.MAX_VALUE else it.nextRevisionDate
            }
            2 -> schemes.sortedWith(compareBy(
                { it.getStatus().ordinal },
                { it.equipmentName.lowercase() }
            ))
            3 -> schemes.sortedWith(compareBy(
                { it.cellNumber ?: "" },
                { it.equipmentName.lowercase() }
            ))
            else -> schemes
        }
    }



}