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

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SchemeAdapter
    private lateinit var btnImport: Button
    private lateinit var btnTestNotification: Button  // ← добавил
    private lateinit var tvEmpty: TextView
    private lateinit var repository: SchemeRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.rvSchemes)
        btnImport = findViewById(R.id.btnImport)
        btnTestNotification = findViewById(R.id.btnTestNotification)  // ← добавил
        tvEmpty = findViewById(R.id.tvEmpty)

        val database = SchemeDatabase.getInstance(this)
        repository = SchemeRepository(database.schemeDao())

        adapter = SchemeAdapter(emptyList()) { scheme ->
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra("scheme", scheme)
            startActivity(intent)
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

        setupNotifications()
    }

    private fun setupNotifications() {
        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
            1, TimeUnit.DAYS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "schemes_notification",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun loadSchemes() {
        lifecycleScope.launch {
            val schemes = repository.getAllSchemes()
            if (schemes.isEmpty()) {
                recyclerView.visibility = android.view.View.GONE
                tvEmpty.visibility = android.view.View.VISIBLE
            } else {
                recyclerView.visibility = android.view.View.VISIBLE
                tvEmpty.visibility = android.view.View.GONE
                adapter.updateData(schemes)
            }
        }
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