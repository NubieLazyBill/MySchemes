package com.example.myschemes.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myschemes.R
import com.example.myschemes.data.database.SchemeDatabase
import com.example.myschemes.data.model.Scheme
import com.example.myschemes.data.repository.SchemeRepository
import com.example.myschemes.utils.PhotoHelper
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.example.myschemes.utils.FileHelper

class CabinetDetailActivity : AppCompatActivity() {

    // Заголовок и статус
    private lateinit var tvTitle: TextView
    private lateinit var tvStatus: TextView

    // Редактируемые поля
    private lateinit var etSchemeNumber: TextInputEditText
    private lateinit var etEditLastRevisionDate: TextInputEditText
    private lateinit var etEditNextRevisionDate: TextInputEditText

    // Чек-боксы
    private lateinit var cbCabinetNameChecked: CheckBox
    private lateinit var cbSwitchesNameChecked: CheckBox
    private lateinit var cbInventoryNumberChecked: CheckBox
    private lateinit var cbLockIntegrity: CheckBox
    private lateinit var cbSealIntegrity: CheckBox
    private lateinit var cbCableEntries: CheckBox
    private lateinit var cbNoBareWires: CheckBox
    private lateinit var cbAddressLabels: CheckBox
    private lateinit var cbTerminalsIntegrity: CheckBox
    private lateinit var cbPainting: CheckBox
    private lateinit var cbHeating: CheckBox
    private lateinit var cbGrounding: CheckBox

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

    // Храним текущий открытый диалог для передачи результатов
    private var currentPhotoDialog: PhotoGalleryDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cabinet_detail)

        photoHelper = PhotoHelper(this)
        initViews()
        initPhotosMap()
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

        cbCabinetNameChecked = findViewById(R.id.cbCabinetNameChecked)
        cbSwitchesNameChecked = findViewById(R.id.cbSwitchesNameChecked)
        cbInventoryNumberChecked = findViewById(R.id.cbInventoryNumberChecked)
        cbLockIntegrity = findViewById(R.id.cbLockIntegrity)
        cbSealIntegrity = findViewById(R.id.cbSealIntegrity)
        cbCableEntries = findViewById(R.id.cbCableEntries)
        cbNoBareWires = findViewById(R.id.cbNoBareWires)
        cbAddressLabels = findViewById(R.id.cbAddressLabels)
        cbTerminalsIntegrity = findViewById(R.id.cbTerminalsIntegrity)
        cbPainting = findViewById(R.id.cbPainting)
        cbHeating = findViewById(R.id.cbHeating)
        cbGrounding = findViewById(R.id.cbGrounding)

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

    private fun initPhotosMap() {
        val keys = listOf(
            "cabinetName", "switchesName", "inventoryNumber",
            "lockIntegrity", "sealIntegrity", "cableEntries",
            "noBareWires", "addressLabels", "terminalsIntegrity",
            "painting", "heating", "grounding"
        )
        keys.forEach { photosMap[it] = mutableListOf() }
    }

    private fun setupListeners() {
        // Автосохранение при изменении текстовых полей
        val textWatcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                autoSave()
            }
        }

        etSchemeNumber.addTextChangedListener(textWatcher)
        etEditLastRevisionDate.addTextChangedListener(textWatcher)
        etEditNextRevisionDate.addTextChangedListener(textWatcher)

        // Автосохранение при изменении чек-боксов
        val checkBoxListener = android.widget.CompoundButton.OnCheckedChangeListener { _, _ ->
            autoSave()
        }

        cbCabinetNameChecked.setOnCheckedChangeListener(checkBoxListener)
        cbSwitchesNameChecked.setOnCheckedChangeListener(checkBoxListener)
        cbInventoryNumberChecked.setOnCheckedChangeListener(checkBoxListener)
        cbLockIntegrity.setOnCheckedChangeListener(checkBoxListener)
        cbSealIntegrity.setOnCheckedChangeListener(checkBoxListener)
        cbCableEntries.setOnCheckedChangeListener(checkBoxListener)
        cbNoBareWires.setOnCheckedChangeListener(checkBoxListener)
        cbAddressLabels.setOnCheckedChangeListener(checkBoxListener)
        cbTerminalsIntegrity.setOnCheckedChangeListener(checkBoxListener)
        cbPainting.setOnCheckedChangeListener(checkBoxListener)
        cbHeating.setOnCheckedChangeListener(checkBoxListener)
        cbGrounding.setOnCheckedChangeListener(checkBoxListener)

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

        // Кнопка удаления
        btnDelete.setOnClickListener { confirmDelete() }

        // Автообновление даты следующего пересмотра при изменении даты схемы
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
                            // Обновляем поле, только если оно не совпадает с текущим
                            if (etEditNextRevisionDate.text.toString() != newNextDate) {
                                etEditNextRevisionDate.setText(newNextDate)
                            }
                        }
                    } catch (e: Exception) {
                        // Невалидная дата, игнорируем
                    }
                }
            }
        })
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
                cabinetNameChecked = cbCabinetNameChecked.isChecked,
                switchesNameChecked = cbSwitchesNameChecked.isChecked,
                inventoryNumberChecked = cbInventoryNumberChecked.isChecked,
                lockIntegrity = cbLockIntegrity.isChecked,
                sealIntegrity = cbSealIntegrity.isChecked,
                cableEntries = cbCableEntries.isChecked,
                noBareWires = cbNoBareWires.isChecked,
                addressLabels = cbAddressLabels.isChecked,
                terminalsIntegrity = cbTerminalsIntegrity.isChecked,
                painting = cbPainting.isChecked,
                heating = cbHeating.isChecked,
                grounding = cbGrounding.isChecked,
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
                groundingPhotos = photosMap["grounding"] ?: emptyList()
            )

            lifecycleScope.launch {
                val allSchemes = repository.getAllSchemes().toMutableList()
                val index = allSchemes.indexOfFirst { it.id == schemeId }
                if (index != -1) {
                    allSchemes[index] = updatedScheme
                    repository.saveSchemes(allSchemes)
                    // Обновляем статус
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
            val schemes = repository.getAllSchemes()
            scheme = schemes.find { it.id == schemeId }

            scheme?.let { s ->
                displayData(s)
                loadPhotos(s)
                updateStatusDisplay(s)
            }
        }
    }

    private fun displayData(scheme: Scheme) {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        tvTitle.text = "📋 ${scheme.equipmentName}"

        etSchemeNumber.setText(scheme.schemeNumber ?: "")
        etEditLastRevisionDate.setText(dateFormat.format(Date(scheme.lastRevisionDate)))
        etEditNextRevisionDate.setText(dateFormat.format(Date(scheme.nextRevisionDate)))

        cbCabinetNameChecked.isChecked = scheme.cabinetNameChecked
        cbSwitchesNameChecked.isChecked = scheme.switchesNameChecked
        cbInventoryNumberChecked.isChecked = scheme.inventoryNumberChecked
        cbLockIntegrity.isChecked = scheme.lockIntegrity
        cbSealIntegrity.isChecked = scheme.sealIntegrity
        cbCableEntries.isChecked = scheme.cableEntries
        cbNoBareWires.isChecked = scheme.noBareWires
        cbAddressLabels.isChecked = scheme.addressLabels
        cbTerminalsIntegrity.isChecked = scheme.terminalsIntegrity
        cbPainting.isChecked = scheme.painting
        cbHeating.isChecked = scheme.heating
        cbGrounding.isChecked = scheme.grounding
    }

    private fun loadPhotos(scheme: Scheme) {
        photosMap["cabinetName"] = scheme.cabinetNamePhotos.filter { File(it).exists() }.toMutableList()
        photosMap["switchesName"] = scheme.switchesNamePhotos.filter { File(it).exists() }.toMutableList()
        photosMap["inventoryNumber"] = scheme.inventoryNumberPhotos.filter { File(it).exists() }.toMutableList()
        photosMap["lockIntegrity"] = scheme.lockIntegrityPhotos.filter { File(it).exists() }.toMutableList()
        photosMap["sealIntegrity"] = scheme.sealIntegrityPhotos.filter { File(it).exists() }.toMutableList()
        photosMap["cableEntries"] = scheme.cableEntriesPhotos.filter { File(it).exists() }.toMutableList()
        photosMap["noBareWires"] = scheme.noBareWiresPhotos.filter { File(it).exists() }.toMutableList()
        photosMap["addressLabels"] = scheme.addressLabelsPhotos.filter { File(it).exists() }.toMutableList()
        photosMap["terminalsIntegrity"] = scheme.terminalsIntegrityPhotos.filter { File(it).exists() }.toMutableList()
        photosMap["painting"] = scheme.paintingPhotos.filter { File(it).exists() }.toMutableList()
        photosMap["heating"] = scheme.heatingPhotos.filter { File(it).exists() }.toMutableList()
        photosMap["grounding"] = scheme.groundingPhotos.filter { File(it).exists() }.toMutableList()
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
            // Удаляем все фото из хранилища
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
        android.util.Log.d("CabinetDetail", "onActivityResult: requestCode=$requestCode, resultCode=$resultCode")
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