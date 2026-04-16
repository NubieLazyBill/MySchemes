package com.example.myschemes.ui

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myschemes.R
import com.example.myschemes.data.database.SchemeDatabase
import com.example.myschemes.data.model.Scheme
import com.example.myschemes.data.repository.SchemeRepository
import com.example.myschemes.utils.FileHelper
import com.example.myschemes.utils.PhotoHelper
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CabinetDetailActivity : AppCompatActivity() {

    private var isLoading = false

    // Заголовок и статус
    private lateinit var tvTitle: TextView
    private lateinit var tvStatus: TextView

    // Редактируемые поля
    private lateinit var etSchemeNumber: TextInputEditText
    private lateinit var etEditLastRevisionDate: TextInputEditText
    private lateinit var etEditNextRevisionDate: TextInputEditText

    // Spinner'ы для чек-листа
    private lateinit var spinnerCabinetName: Spinner
    private lateinit var spinnerSwitchesName: Spinner
    private lateinit var spinnerInventoryNumber: Spinner
    private lateinit var spinnerLockIntegrity: Spinner
    private lateinit var spinnerSealIntegrity: Spinner
    private lateinit var spinnerCableEntries: Spinner
    private lateinit var spinnerNoBareWires: Spinner
    private lateinit var spinnerAddressLabels: Spinner
    private lateinit var spinnerTerminalsIntegrity: Spinner
    private lateinit var spinnerPainting: Spinner
    private lateinit var spinnerHeating: Spinner
    private lateinit var spinnerGrounding: Spinner

    // Кнопки примечаний
    private lateinit var btnCabinetNameNote: ImageButton
    private lateinit var btnSwitchesNameNote: ImageButton
    private lateinit var btnInventoryNumberNote: ImageButton
    private lateinit var btnLockIntegrityNote: ImageButton
    private lateinit var btnSealIntegrityNote: ImageButton
    private lateinit var btnCableEntriesNote: ImageButton
    private lateinit var btnNoBareWiresNote: ImageButton
    private lateinit var btnAddressLabelsNote: ImageButton
    private lateinit var btnTerminalsIntegrityNote: ImageButton
    private lateinit var btnPaintingNote: ImageButton
    private lateinit var btnHeatingNote: ImageButton
    private lateinit var btnGroundingNote: ImageButton

    // Кнопки фото
    private lateinit var btnCabinetNamePhoto: ImageButton
    private lateinit var btnSwitchesNamePhoto: ImageButton
    private lateinit var btnInventoryNumberPhoto: ImageButton
    private lateinit var btnLockIntegrityPhoto: ImageButton
    private lateinit var btnSealIntegrityPhoto: ImageButton
    private lateinit var btnCableEntriesPhoto: ImageButton
    private lateinit var btnNoBareWiresPhoto: ImageButton
    private lateinit var btnAddressLabelsPhoto: ImageButton
    private lateinit var btnTerminalsIntegrityPhoto: ImageButton
    private lateinit var btnPaintingPhoto: ImageButton
    private lateinit var btnHeatingPhoto: ImageButton
    private lateinit var btnGroundingPhoto: ImageButton

    // Кнопка удаления
    private lateinit var btnDelete: Button

    private lateinit var repository: SchemeRepository
    private lateinit var photoHelper: PhotoHelper
    private var scheme: Scheme? = null
    private var schemeId: Int = 0

    // Хранилище списков фото для каждого пункта
    private val photosMap = mutableMapOf<String, MutableList<String>>()

    // Хранилище статусов для каждого пункта (0=Норма, 1=Замечания, 2=Не соответствует)
    private val statusMap = mutableMapOf<String, Int>()

    // Хранилище примечаний для каждого пункта
    private val notesMap = mutableMapOf<String, String?>()

    private var currentPhotoDialog: PhotoGalleryDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cabinet_detail)

        photoHelper = PhotoHelper(this)
        initViews()
        initMaps()
        setupSpinners()
        setupListeners()

        schemeId = intent.getIntExtra("scheme_id", 0)

        val database = SchemeDatabase.getInstance(this)
        repository = SchemeRepository(database.schemeDao())

        loadSchemeData()
    }

    private fun initViews() {
        tvTitle = findViewById(R.id.tvTitle)
        tvStatus = findViewById(R.id.tvStatus)

        etSchemeNumber = findViewById(R.id.etSchemeNumber)
        etEditLastRevisionDate = findViewById(R.id.etLastRevisionDate)
        etEditNextRevisionDate = findViewById(R.id.etNextRevisionDate)

        // Spinner'ы
        spinnerCabinetName = findViewById(R.id.spinnerCabinetName)
        spinnerSwitchesName = findViewById(R.id.spinnerSwitchesName)
        spinnerInventoryNumber = findViewById(R.id.spinnerInventoryNumber)
        spinnerLockIntegrity = findViewById(R.id.spinnerLockIntegrity)
        spinnerSealIntegrity = findViewById(R.id.spinnerSealIntegrity)
        spinnerCableEntries = findViewById(R.id.spinnerCableEntries)
        spinnerNoBareWires = findViewById(R.id.spinnerNoBareWires)
        spinnerAddressLabels = findViewById(R.id.spinnerAddressLabels)
        spinnerTerminalsIntegrity = findViewById(R.id.spinnerTerminalsIntegrity)
        spinnerPainting = findViewById(R.id.spinnerPainting)
        spinnerHeating = findViewById(R.id.spinnerHeating)
        spinnerGrounding = findViewById(R.id.spinnerGrounding)

        // Кнопки примечаний
        btnCabinetNameNote = findViewById(R.id.btnCabinetNameNote)
        btnSwitchesNameNote = findViewById(R.id.btnSwitchesNameNote)
        btnInventoryNumberNote = findViewById(R.id.btnInventoryNumberNote)
        btnLockIntegrityNote = findViewById(R.id.btnLockIntegrityNote)
        btnSealIntegrityNote = findViewById(R.id.btnSealIntegrityNote)
        btnCableEntriesNote = findViewById(R.id.btnCableEntriesNote)
        btnNoBareWiresNote = findViewById(R.id.btnNoBareWiresNote)
        btnAddressLabelsNote = findViewById(R.id.btnAddressLabelsNote)
        btnTerminalsIntegrityNote = findViewById(R.id.btnTerminalsIntegrityNote)
        btnPaintingNote = findViewById(R.id.btnPaintingNote)
        btnHeatingNote = findViewById(R.id.btnHeatingNote)
        btnGroundingNote = findViewById(R.id.btnGroundingNote)

        // Кнопки фото
        btnCabinetNamePhoto = findViewById(R.id.btnCabinetNamePhoto)
        btnSwitchesNamePhoto = findViewById(R.id.btnSwitchesNamePhoto)
        btnInventoryNumberPhoto = findViewById(R.id.btnInventoryNumberPhoto)
        btnLockIntegrityPhoto = findViewById(R.id.btnLockIntegrityPhoto)
        btnSealIntegrityPhoto = findViewById(R.id.btnSealIntegrityPhoto)
        btnCableEntriesPhoto = findViewById(R.id.btnCableEntriesPhoto)
        btnNoBareWiresPhoto = findViewById(R.id.btnNoBareWiresPhoto)
        btnAddressLabelsPhoto = findViewById(R.id.btnAddressLabelsPhoto)
        btnTerminalsIntegrityPhoto = findViewById(R.id.btnTerminalsIntegrityPhoto)
        btnPaintingPhoto = findViewById(R.id.btnPaintingPhoto)
        btnHeatingPhoto = findViewById(R.id.btnHeatingPhoto)
        btnGroundingPhoto = findViewById(R.id.btnGroundingPhoto)

        btnDelete = findViewById(R.id.btnDelete)
    }

    private fun initMaps() {
        val keys = listOf(
            "cabinetName", "switchesName", "inventoryNumber",
            "lockIntegrity", "sealIntegrity", "cableEntries",
            "noBareWires", "addressLabels", "terminalsIntegrity",
            "painting", "heating", "grounding"
        )
        keys.forEach {
            photosMap[it] = mutableListOf()
            statusMap[it] = 0
            notesMap[it] = null
        }
    }

    private fun setupSpinners() {
        val statusItems = listOf(
            StatusItem("✅", "Норма", 0),
            StatusItem("⚠️", "Есть замечания", 1),
            StatusItem("❌", "Не соответствует", 2)
        )

        val adapter = StatusSpinnerAdapter(this, statusItems)

        val spinners = listOf(
            spinnerCabinetName, spinnerSwitchesName, spinnerInventoryNumber,
            spinnerLockIntegrity, spinnerSealIntegrity, spinnerCableEntries,
            spinnerNoBareWires, spinnerAddressLabels, spinnerTerminalsIntegrity,
            spinnerPainting, spinnerHeating, spinnerGrounding
        )

        spinners.forEach { spinner ->
            spinner.adapter = adapter
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (!isLoading) {
                        val key = when (spinner.id) {
                            R.id.spinnerCabinetName -> "cabinetName"
                            R.id.spinnerSwitchesName -> "switchesName"
                            R.id.spinnerInventoryNumber -> "inventoryNumber"
                            R.id.spinnerLockIntegrity -> "lockIntegrity"
                            R.id.spinnerSealIntegrity -> "sealIntegrity"
                            R.id.spinnerCableEntries -> "cableEntries"
                            R.id.spinnerNoBareWires -> "noBareWires"
                            R.id.spinnerAddressLabels -> "addressLabels"
                            R.id.spinnerTerminalsIntegrity -> "terminalsIntegrity"
                            R.id.spinnerPainting -> "painting"
                            R.id.spinnerHeating -> "heating"
                            else -> "grounding"
                        }
                        statusMap[key] = position
                        autoSave()
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    private fun setupListeners() {
        val textWatcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (!isLoading) autoSave()
            }
        }

        etSchemeNumber.addTextChangedListener(textWatcher)
        etEditLastRevisionDate.addTextChangedListener(textWatcher)
        etEditNextRevisionDate.addTextChangedListener(textWatcher)

        // Кнопки примечаний
        btnCabinetNameNote.setOnClickListener { showNoteDialog("cabinetName", "Диспетчерское наименование") }
        btnSwitchesNameNote.setOnClickListener { showNoteDialog("switchesName", "ДН автоматов, рубильников") }
        btnInventoryNumberNote.setOnClickListener { showNoteDialog("inventoryNumber", "Инвентарный номер") }
        btnLockIntegrityNote.setOnClickListener { showNoteDialog("lockIntegrity", "Целостность замков") }
        btnSealIntegrityNote.setOnClickListener { showNoteDialog("sealIntegrity", "Уплотнение шкафа") }
        btnCableEntriesNote.setOnClickListener { showNoteDialog("cableEntries", "Заходы кабелей") }
        btnNoBareWiresNote.setOnClickListener { showNoteDialog("noBareWires", "Оголённые жилы") }
        btnAddressLabelsNote.setOnClickListener { showNoteDialog("addressLabels", "Адресные бирки") }
        btnTerminalsIntegrityNote.setOnClickListener { showNoteDialog("terminalsIntegrity", "Клеммники") }
        btnPaintingNote.setOnClickListener { showNoteDialog("painting", "Окраска") }
        btnHeatingNote.setOnClickListener { showNoteDialog("heating", "Обогрев") }
        btnGroundingNote.setOnClickListener { showNoteDialog("grounding", "Заземление") }

        // Кнопки фото
        btnCabinetNamePhoto.setOnClickListener { showPhotoGallery("cabinetName", "Диспетчерское наименование") }
        btnSwitchesNamePhoto.setOnClickListener { showPhotoGallery("switchesName", "ДН автоматов, рубильников") }
        btnInventoryNumberPhoto.setOnClickListener { showPhotoGallery("inventoryNumber", "Инвентарный номер") }
        btnLockIntegrityPhoto.setOnClickListener { showPhotoGallery("lockIntegrity", "Целостность замков") }
        btnSealIntegrityPhoto.setOnClickListener { showPhotoGallery("sealIntegrity", "Уплотнение шкафа") }
        btnCableEntriesPhoto.setOnClickListener { showPhotoGallery("cableEntries", "Заходы кабелей") }
        btnNoBareWiresPhoto.setOnClickListener { showPhotoGallery("noBareWires", "Оголённые жилы") }
        btnAddressLabelsPhoto.setOnClickListener { showPhotoGallery("addressLabels", "Адресные бирки") }
        btnTerminalsIntegrityPhoto.setOnClickListener { showPhotoGallery("terminalsIntegrity", "Клеммники") }
        btnPaintingPhoto.setOnClickListener { showPhotoGallery("painting", "Окраска") }
        btnHeatingPhoto.setOnClickListener { showPhotoGallery("heating", "Обогрев") }
        btnGroundingPhoto.setOnClickListener { showPhotoGallery("grounding", "Заземление") }

        btnDelete.setOnClickListener { confirmDelete() }

        // Автообновление даты следующего пересмотра
        etEditLastRevisionDate.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val dateStr = s.toString()
                if (dateStr.isNotEmpty()) {
                    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    try {
                        val parsedDate = dateFormat.parse(dateStr)
                        parsedDate?.let {
                            val calendar = Calendar.getInstance().apply {
                                time = it
                                add(Calendar.YEAR, 3)
                            }
                            val newNextDate = dateFormat.format(calendar.time)
                            if (etEditNextRevisionDate.text.toString() != newNextDate) {
                                etEditNextRevisionDate.setText(newNextDate)
                            }
                        }
                    } catch (e: Exception) { }
                }
            }
        })
    }

    private fun showNoteDialog(key: String, title: String) {
        val etNote = EditText(this)
        etNote.setText(notesMap[key] ?: "")
        etNote.minLines = 3
        etNote.gravity = android.view.Gravity.TOP

        AlertDialog.Builder(this)
            .setTitle("📝 Примечание: $title")
            .setView(etNote)
            .setPositiveButton("Сохранить") { _, _ ->
                notesMap[key] = etNote.text.toString().takeIf { it.isNotBlank() }
                autoSave()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showPhotoGallery(key: String, title: String) {
        val photos = photosMap[key] ?: mutableListOf()
        currentPhotoDialog = PhotoGalleryDialog(
            activity = this,
            title = title,
            photos = photos
        ) { updatedPhotos ->
            photosMap[key] = updatedPhotos.toMutableList()
            autoSave()
        }
        currentPhotoDialog?.show()
    }

    private fun autoSave() {
        if (isLoading) return

        scheme?.let { s ->
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

            val schemeNumber = etSchemeNumber.text.toString().takeIf { it.isNotEmpty() }
            val lastRevisionDateStr = etEditLastRevisionDate.text.toString()
            val nextRevisionDateStr = etEditNextRevisionDate.text.toString()

            val lastRevisionDate = try {
                dateFormat.parse(lastRevisionDateStr)?.time ?: s.lastRevisionDate
            } catch (e: Exception) { s.lastRevisionDate }

            val nextRevisionDate = try {
                dateFormat.parse(nextRevisionDateStr)?.time ?: s.nextRevisionDate
            } catch (e: Exception) { s.nextRevisionDate }

            val updatedScheme = s.copy(
                schemeNumber = schemeNumber,
                lastRevisionDate = lastRevisionDate,
                nextRevisionDate = nextRevisionDate,
                cabinetNameChecked = statusMap["cabinetName"] == 0,
                switchesNameChecked = statusMap["switchesName"] == 0,
                inventoryNumberChecked = statusMap["inventoryNumber"] == 0,
                lockIntegrity = statusMap["lockIntegrity"] == 0,
                sealIntegrity = statusMap["sealIntegrity"] == 0,
                cableEntries = statusMap["cableEntries"] == 0,
                noBareWires = statusMap["noBareWires"] == 0,
                addressLabels = statusMap["addressLabels"] == 0,
                terminalsIntegrity = statusMap["terminalsIntegrity"] == 0,
                painting = statusMap["painting"] == 0,
                heating = statusMap["heating"] == 0,
                grounding = statusMap["grounding"] == 0,
                cabinetNamePhotos = photosMap["cabinetName"] ?: emptyList(),
                switchesNamePhotos = photosMap["switchesName"] ?: emptyList(),
                inventoryNumberPhotos = photosMap["inventoryNumber"] ?: emptyList(),
                lockIntegrityPhotos = photosMap["lockIntegrity"] ?: emptyList(),
                sealIntegrityPhotos = photosMap["sealIntegrity"] ?: emptyList(),
                cableEntriesPhotos = photosMap["cableEntries"] ?: emptyList(),
                noBareWiresPhotos = photosMap["noBareWires"] ?: emptyList(),
                addressLabelsPhotos = photosMap["addressLabels"] ?: emptyList(),
                terminalsIntegrityPhotos = photosMap["terminalsIntegrity"] ?: emptyList(),
                paintingPhotos = photosMap["painting"] ?: emptyList(),
                heatingPhotos = photosMap["heating"] ?: emptyList(),
                groundingPhotos = photosMap["grounding"] ?: emptyList(),
                cabinetNameNote = notesMap["cabinetName"],
                switchesNameNote = notesMap["switchesName"],
                inventoryNumberNote = notesMap["inventoryNumber"],
                lockIntegrityNote = notesMap["lockIntegrity"],
                sealIntegrityNote = notesMap["sealIntegrity"],
                cableEntriesNote = notesMap["cableEntries"],
                noBareWiresNote = notesMap["noBareWires"],
                addressLabelsNote = notesMap["addressLabels"],
                terminalsIntegrityNote = notesMap["terminalsIntegrity"],
                paintingNote = notesMap["painting"],
                heatingNote = notesMap["heating"],
                groundingNote = notesMap["grounding"]
            )

            lifecycleScope.launch {
                val allSchemes = repository.getAllSchemes().toMutableList()
                val index = allSchemes.indexOfFirst { it.id == schemeId }
                if (index != -1) {
                    allSchemes[index] = updatedScheme
                    repository.saveSchemes(allSchemes)
                    updateStatusDisplay(updatedScheme)
                }
            }
        }
    }

    private fun updateStatusDisplay(scheme: Scheme) {
        val (statusText, statusColor) = getStatusInfo(scheme.nextRevisionDate)
        tvStatus.text = statusText
        tvStatus.setTextColor(statusColor)
    }

    private fun loadSchemeData() {
        lifecycleScope.launch {
            isLoading = true
            val schemes = repository.getAllSchemes()
            scheme = schemes.find { it.id == schemeId }

            scheme?.let { s ->
                displayData(s)
                loadPhotos(s)
                loadStatuses(s)
                loadNotes(s)
                updateStatusDisplay(s)
            }
            isLoading = false
        }
    }

    private fun loadNotes(scheme: Scheme) {
        notesMap["cabinetName"] = scheme.cabinetNameNote
        notesMap["switchesName"] = scheme.switchesNameNote
        notesMap["inventoryNumber"] = scheme.inventoryNumberNote
        notesMap["lockIntegrity"] = scheme.lockIntegrityNote
        notesMap["sealIntegrity"] = scheme.sealIntegrityNote
        notesMap["cableEntries"] = scheme.cableEntriesNote
        notesMap["noBareWires"] = scheme.noBareWiresNote
        notesMap["addressLabels"] = scheme.addressLabelsNote
        notesMap["terminalsIntegrity"] = scheme.terminalsIntegrityNote
        notesMap["painting"] = scheme.paintingNote
        notesMap["heating"] = scheme.heatingNote
        notesMap["grounding"] = scheme.groundingNote
    }

    private fun displayData(scheme: Scheme) {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        tvTitle.text = "📋 ${scheme.equipmentName}"

        etSchemeNumber.setText(scheme.schemeNumber ?: "")
        etEditLastRevisionDate.setText(dateFormat.format(Date(scheme.lastRevisionDate)))
        etEditNextRevisionDate.setText(dateFormat.format(Date(scheme.nextRevisionDate)))
    }

    private fun loadStatuses(scheme: Scheme) {
        statusMap["cabinetName"] = if (scheme.cabinetNameChecked) 0 else 1
        statusMap["switchesName"] = if (scheme.switchesNameChecked) 0 else 1
        statusMap["inventoryNumber"] = if (scheme.inventoryNumberChecked) 0 else 1
        statusMap["lockIntegrity"] = if (scheme.lockIntegrity) 0 else 1
        statusMap["sealIntegrity"] = if (scheme.sealIntegrity) 0 else 1
        statusMap["cableEntries"] = if (scheme.cableEntries) 0 else 1
        statusMap["noBareWires"] = if (scheme.noBareWires) 0 else 1
        statusMap["addressLabels"] = if (scheme.addressLabels) 0 else 1
        statusMap["terminalsIntegrity"] = if (scheme.terminalsIntegrity) 0 else 1
        statusMap["painting"] = if (scheme.painting) 0 else 1
        statusMap["heating"] = if (scheme.heating) 0 else 1
        statusMap["grounding"] = if (scheme.grounding) 0 else 1

        // Устанавливаем значения в спиннеры без вызова onItemSelected
        setSpinnerSelectionWithoutCallback(spinnerCabinetName, statusMap["cabinetName"] ?: 0)
        setSpinnerSelectionWithoutCallback(spinnerSwitchesName, statusMap["switchesName"] ?: 0)
        setSpinnerSelectionWithoutCallback(spinnerInventoryNumber, statusMap["inventoryNumber"] ?: 0)
        setSpinnerSelectionWithoutCallback(spinnerLockIntegrity, statusMap["lockIntegrity"] ?: 0)
        setSpinnerSelectionWithoutCallback(spinnerSealIntegrity, statusMap["sealIntegrity"] ?: 0)
        setSpinnerSelectionWithoutCallback(spinnerCableEntries, statusMap["cableEntries"] ?: 0)
        setSpinnerSelectionWithoutCallback(spinnerNoBareWires, statusMap["noBareWires"] ?: 0)
        setSpinnerSelectionWithoutCallback(spinnerAddressLabels, statusMap["addressLabels"] ?: 0)
        setSpinnerSelectionWithoutCallback(spinnerTerminalsIntegrity, statusMap["terminalsIntegrity"] ?: 0)
        setSpinnerSelectionWithoutCallback(spinnerPainting, statusMap["painting"] ?: 0)
        setSpinnerSelectionWithoutCallback(spinnerHeating, statusMap["heating"] ?: 0)
        setSpinnerSelectionWithoutCallback(spinnerGrounding, statusMap["grounding"] ?: 0)
    }

    private fun setSpinnerSelectionWithoutCallback(spinner: Spinner, position: Int) {
        spinner.setSelection(position, false)
    }

    private fun loadPhotos(scheme: Scheme) {
        photosMap["cabinetName"] = scheme.cabinetNamePhotos.filter { isPhotoExists(it) }.toMutableList()
        photosMap["switchesName"] = scheme.switchesNamePhotos.filter { isPhotoExists(it) }.toMutableList()
        photosMap["inventoryNumber"] = scheme.inventoryNumberPhotos.filter { isPhotoExists(it) }.toMutableList()
        photosMap["lockIntegrity"] = scheme.lockIntegrityPhotos.filter { isPhotoExists(it) }.toMutableList()
        photosMap["sealIntegrity"] = scheme.sealIntegrityPhotos.filter { isPhotoExists(it) }.toMutableList()
        photosMap["cableEntries"] = scheme.cableEntriesPhotos.filter { isPhotoExists(it) }.toMutableList()
        photosMap["noBareWires"] = scheme.noBareWiresPhotos.filter { isPhotoExists(it) }.toMutableList()
        photosMap["addressLabels"] = scheme.addressLabelsPhotos.filter { isPhotoExists(it) }.toMutableList()
        photosMap["terminalsIntegrity"] = scheme.terminalsIntegrityPhotos.filter { isPhotoExists(it) }.toMutableList()
        photosMap["painting"] = scheme.paintingPhotos.filter { isPhotoExists(it) }.toMutableList()
        photosMap["heating"] = scheme.heatingPhotos.filter { isPhotoExists(it) }.toMutableList()
        photosMap["grounding"] = scheme.groundingPhotos.filter { isPhotoExists(it) }.toMutableList()
    }

    private fun isPhotoExists(path: String): Boolean {
        return if (path.startsWith("content://")) {
            try {
                contentResolver.openInputStream(Uri.parse(path))?.close()
                true
            } catch (e: Exception) {
                false
            }
        } else {
            File(path).exists()
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

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("Удаление схемы")
            .setMessage("Вы уверены, что хотите удалить эту схему?")
            .setPositiveButton("Да") { _, _ ->
                deleteScheme()
            }
            .setNegativeButton("Нет", null)
            .show()
    }

    private fun deleteScheme() {
        lifecycleScope.launch {
            photosMap.values.forEach { photoList ->
                photoList.forEach { photoPath ->
                    FileHelper.deletePhotoFromAppStorage(this@CabinetDetailActivity, photoPath)
                }
            }

            val allSchemes = repository.getAllSchemes().toMutableList()
            allSchemes.removeAll { it.id == schemeId }
            repository.saveSchemes(allSchemes)
            Toast.makeText(this@CabinetDetailActivity, "Схема удалена", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        photoHelper.handleActivityResult(requestCode, resultCode, data)
        currentPhotoDialog?.handleActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        photoHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}