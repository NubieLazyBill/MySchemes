package com.example.myschemes.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.myschemes.R
import com.example.myschemes.data.model.Scheme
import java.text.SimpleDateFormat
import java.util.*

class DetailActivity : AppCompatActivity() {

    private lateinit var tvEquipmentName: TextView
    private lateinit var tvItemNumber: TextView
    private lateinit var tvCellNumber: TextView
    private lateinit var tvSchemeNumber: TextView
    private lateinit var tvLastRevisionDate: TextView
    private lateinit var tvNextRevisionDate: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnBack: Button          // ← Button
    private lateinit var btnInspection: Button
    private var scheme: Scheme? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        tvEquipmentName = findViewById(R.id.tvEquipmentName)
        tvItemNumber = findViewById(R.id.tvItemNumber)
        tvCellNumber = findViewById(R.id.tvCellNumber)
        tvSchemeNumber = findViewById(R.id.tvSchemeNumber)
        tvLastRevisionDate = findViewById(R.id.tvLastRevisionDate)
        tvNextRevisionDate = findViewById(R.id.tvNextRevisionDate)
        tvStatus = findViewById(R.id.tvStatus)
        btnBack = findViewById(R.id.btnBack)
        btnInspection = findViewById(R.id.btnInspection)

        scheme = intent.getSerializableExtra("scheme") as? Scheme

        scheme?.let {
            displayScheme(it)
            setupInspectionButton()
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun displayScheme(scheme: Scheme) {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        tvEquipmentName.text = scheme.equipmentName
        tvItemNumber.text = scheme.itemNumber?.toString() ?: "—"
        tvCellNumber.text = scheme.cellNumber ?: "—"
        tvSchemeNumber.text = scheme.schemeNumber ?: "—"
        tvLastRevisionDate.text = dateFormat.format(Date(scheme.lastRevisionDate))
        tvNextRevisionDate.text = dateFormat.format(Date(scheme.nextRevisionDate))

        val (statusText, statusColor) = getStatusInfo(scheme.nextRevisionDate)
        tvStatus.text = statusText
        tvStatus.setTextColor(statusColor)
    }

    private fun setupInspectionButton() {
        btnInspection.setOnClickListener {
            val intent = Intent(this, InspectionActivity::class.java)
            intent.putExtra("scheme_id", scheme?.id ?: 0)
            startActivity(intent)
        }
    }

    private fun getStatusInfo(nextRevisionDate: Long): Pair<String, Int> {
        val today = System.currentTimeMillis()
        val daysLeft = ((nextRevisionDate - today) / (1000 * 60 * 60 * 24)).toInt()
        return when {
            daysLeft < 0 -> "🔴 Просрочено" to Color.RED
            daysLeft <= 30 -> "🟡 Скоро истекает" to Color.parseColor("#FF9800")
            else -> "✅ Активна" to Color.GREEN
        }
    }
}