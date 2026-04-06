package com.example.myschemes.ui

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myschemes.R
import com.example.myschemes.data.database.SchemeDatabase
import com.example.myschemes.data.model.Scheme
import com.example.myschemes.data.repository.SchemeRepository
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class EditSchemeActivity : AppCompatActivity() {

    private lateinit var etEquipmentName: TextInputEditText
    private lateinit var etCellNumber: TextInputEditText
    private lateinit var etSchemeNumber: TextInputEditText
    private lateinit var etLastRevisionDate: TextInputEditText
    private lateinit var etNextRevisionDate: TextInputEditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    private lateinit var repository: SchemeRepository
    private var schemeId: Int = 0
    private var isEditMode = false

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val storageDateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_scheme)

        etEquipmentName = findViewById(R.id.etEquipmentName)
        etCellNumber = findViewById(R.id.etCellNumber)
        etSchemeNumber = findViewById(R.id.etSchemeNumber)
        etLastRevisionDate = findViewById(R.id.etLastRevisionDate)
        etNextRevisionDate = findViewById(R.id.etNextRevisionDate)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)

        val database = SchemeDatabase.getInstance(this)
        repository = SchemeRepository(database.schemeDao())

        // Получаем данные из Intent
        schemeId = intent.getIntExtra("scheme_id", 0)
        isEditMode = schemeId != 0

        if (isEditMode) {
            title = "✏️ Редактирование схемы"
            loadSchemeData()
        } else {
            title = "➕ Добавление схемы"
        }

        btnSave.setOnClickListener { saveScheme() }
        btnCancel.setOnClickListener { finish() }
    }

    private fun loadSchemeData() {
        lifecycleScope.launch {
            val schemes = repository.getAllSchemes()
            val scheme = schemes.find { it.id == schemeId }
            scheme?.let {
                etEquipmentName.setText(it.equipmentName)
                etCellNumber.setText(it.cellNumber ?: "")
                etSchemeNumber.setText(it.schemeNumber ?: "")
                etLastRevisionDate.setText(dateFormat.format(Date(it.lastRevisionDate)))
                etNextRevisionDate.setText(dateFormat.format(Date(it.nextRevisionDate)))
            }
        }
    }

    private fun saveScheme() {
        val equipmentName = etEquipmentName.text.toString().trim()
        val cellNumber = etCellNumber.text.toString().trim().takeIf { it.isNotEmpty() }
        val schemeNumber = etSchemeNumber.text.toString().trim().takeIf { it.isNotEmpty() }
        val lastRevisionDateStr = etLastRevisionDate.text.toString().trim()
        val nextRevisionDateStr = etNextRevisionDate.text.toString().trim()

        if (equipmentName.isEmpty()) {
            Toast.makeText(this, "Введите наименование оборудования", Toast.LENGTH_SHORT).show()
            return
        }

        if (lastRevisionDateStr.isEmpty() || nextRevisionDateStr.isEmpty()) {
            Toast.makeText(this, "Введите даты", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val lastRevisionDate = dateFormat.parse(lastRevisionDateStr)?.time ?: 0L
            val nextRevisionDate = dateFormat.parse(nextRevisionDateStr)?.time ?: 0L

            if (lastRevisionDate == 0L || nextRevisionDate == 0L) {
                Toast.makeText(this, "Неверный формат даты", Toast.LENGTH_SHORT).show()
                return
            }

            val scheme = Scheme(
                id = if (isEditMode) schemeId else 0,
                itemNumber = null,  // автонумерация
                cellNumber = cellNumber,
                equipmentName = equipmentName,
                lastRevisionDate = lastRevisionDate,
                nextRevisionDate = nextRevisionDate,
                schemeNumber = schemeNumber
            )

            lifecycleScope.launch {
                if (isEditMode) {
                    // Обновление: удаляем старую и добавляем новую
                    val allSchemes = repository.getAllSchemes()
                    val updatedSchemes = allSchemes.map {
                        if (it.id == schemeId) scheme else it
                    }
                    repository.saveSchemes(updatedSchemes)
                    Toast.makeText(this@EditSchemeActivity, "Схема обновлена", Toast.LENGTH_SHORT).show()
                } else {
                    // Добавление
                    val allSchemes = repository.getAllSchemes().toMutableList()
                    val newId = (allSchemes.maxOfOrNull { it.id } ?: 0) + 1
                    val newScheme = scheme.copy(id = newId, itemNumber = allSchemes.size + 1)
                    allSchemes.add(newScheme)
                    repository.saveSchemes(allSchemes)
                    Toast.makeText(this@EditSchemeActivity, "Схема добавлена", Toast.LENGTH_SHORT).show()
                }
                finish()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}