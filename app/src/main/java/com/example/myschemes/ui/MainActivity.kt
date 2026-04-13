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
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*
import androidx.appcompat.app.AlertDialog

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SchemeAdapter
    private lateinit var tvCounter: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var repository: SchemeRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setTitleTextColor(Color.BLACK)
        supportActionBar?.title = "📋 Мои схемы"

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
            .setTitle("➕ Добавление схемы")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val equipmentName = etEquipmentName.text.toString().trim()
                val cellNumber = etCellNumber.text.toString().trim().takeIf { it.isNotEmpty() }
                val schemeNumber = etSchemeNumber.text.toString().trim().takeIf { it.isNotEmpty() }
                val lastRevisionDateStr = etLastRevisionDate.text.toString().trim()

                if (equipmentName.isEmpty()) {
                    Toast.makeText(this, "Введите наименование оборудования", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (lastRevisionDateStr.isEmpty()) {
                    Toast.makeText(this, "Введите дату схемы", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                try {
                    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    val lastRevisionDate = dateFormat.parse(lastRevisionDateStr)?.time ?: 0L

                    if (lastRevisionDate == 0L) {
                        Toast.makeText(this, "Неверный формат даты", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    // Автоматически вычисляем дату следующего пересмотра (+3 года)
                    val calendar = Calendar.getInstance().apply {
                        timeInMillis = lastRevisionDate
                        add(Calendar.YEAR, 3)
                    }
                    val nextRevisionDate = calendar.timeInMillis

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
                        Toast.makeText(this@MainActivity, "Схема добавлена", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
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
            if (schemes.isEmpty()) {
                recyclerView.visibility = android.view.View.GONE
                tvEmpty.visibility = android.view.View.VISIBLE
                tvCounter.text = "Всего схем: 0"
            } else {
                recyclerView.visibility = android.view.View.VISIBLE
                tvEmpty.visibility = android.view.View.GONE
                adapter.updateData(schemes)
                tvCounter.text = "Всего схем: ${schemes.size}"
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
}