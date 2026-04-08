package com.example.myschemes.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.*
import com.example.myschemes.R
import com.example.myschemes.data.database.SchemeDatabase
import com.example.myschemes.data.repository.SchemeRepository
import com.example.myschemes.utils.CsvImporter
import com.example.myschemes.worker.NotificationWorker
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.Context
import android.widget.Toast
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SchemeAdapter
    private lateinit var btnImport: Button
    private lateinit var btnTestNotification: Button
    private lateinit var btnAddScheme: Button  // ← добавили
    private lateinit var tvEmpty: TextView
    private lateinit var repository: SchemeRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.rvSchemes)
        btnImport = findViewById(R.id.btnImport)
        btnTestNotification = findViewById(R.id.btnTestNotification)
        btnAddScheme = findViewById(R.id.btnAddScheme)  // ← добавили
        tvEmpty = findViewById(R.id.tvEmpty)

        val database = SchemeDatabase.getInstance(this)
        repository = SchemeRepository(database.schemeDao())

        adapter = SchemeAdapter(emptyList()) { scheme ->
            val intent = Intent(this, CabinetDetailActivity::class.java)
            intent.putExtra("scheme_id", scheme.id)
            startActivity(intent)
        }

        btnAddScheme.setOnClickListener {
            // TODO: создание новой схемы
            Toast.makeText(this, "Добавление схемы в разработке", Toast.LENGTH_SHORT).show()
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        loadSchemes()

        btnImport.setOnClickListener {
            importSchemes()
        }

        btnTestNotification.setOnClickListener {
            val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                .build()
            WorkManager.getInstance(this).enqueue(workRequest)
        }

        btnAddScheme.setOnClickListener {
            val intent = Intent(this, EditSchemeActivity::class.java)
            startActivity(intent)
        }

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

        // Запускаем проверку при первом запуске (опционально)
        if (isFirstRun()) {
            val immediateWork = OneTimeWorkRequestBuilder<NotificationWorker>()
                .build()
            WorkManager.getInstance(this).enqueue(immediateWork)
        }
    }

    private fun setupNotifications() {
        val startTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
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
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    private fun loadSchemes() {
        lifecycleScope.launch {
            val schemes = repository.getAllSchemes()
            if (schemes.isEmpty()) {
                recyclerView.visibility = android.view.View.GONE
                tvEmpty.visibility = android.view.View.VISIBLE
                findViewById<TextView>(R.id.tvCounter).text = "Всего схем: 0"
            } else {
                recyclerView.visibility = android.view.View.VISIBLE
                tvEmpty.visibility = android.view.View.GONE
                adapter.updateData(schemes)
                findViewById<TextView>(R.id.tvCounter).text = "Всего схем: ${schemes.size}"
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
        lifecycleScope.launch {
            try {
                val importer = CsvImporter(applicationContext)
                val schemes = importer.importFromAssets()
                if (schemes.isNotEmpty()) {
                    repository.saveSchemes(schemes)
                    loadSchemes()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}