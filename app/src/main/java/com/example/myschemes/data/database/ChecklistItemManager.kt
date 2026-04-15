package com.example.myschemes.ui

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.*
import com.example.myschemes.R
import com.example.myschemes.data.model.ChecklistStatus

class ChecklistItemManager(
    private val context: Context,
    private val title: String,
    private val currentStatus: ChecklistStatus,
    private val currentNote: String?,
    private val onSave: (status: ChecklistStatus, note: String?) -> Unit
) {

    fun showDialog() {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_checklist_item, null)
        val spinner = view.findViewById<Spinner>(R.id.spinnerStatus)
        val etNote = view.findViewById<EditText>(R.id.etNote)

        val statuses = listOf(
            Triple(ChecklistStatus.NORMAL, "✅ Норма", "#4CAF50"),
            Triple(ChecklistStatus.WARNING, "⚠️ Есть замечания", "#FF9800"),
            Triple(ChecklistStatus.FAIL, "❌ Не соответствует", "#F44336")
        )

        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, statuses.map { it.second })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        val currentIndex = statuses.indexOfFirst { it.first == currentStatus }
        if (currentIndex >= 0) spinner.setSelection(currentIndex)

        etNote.setText(currentNote ?: "")

        AlertDialog.Builder(context)
            .setTitle(title)
            .setView(view)
            .setPositiveButton("Сохранить") { _, _ ->
                val selectedStatus = statuses[spinner.selectedItemPosition].first
                val note = etNote.text.toString().takeIf { it.isNotBlank() }
                onSave(selectedStatus, note)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}