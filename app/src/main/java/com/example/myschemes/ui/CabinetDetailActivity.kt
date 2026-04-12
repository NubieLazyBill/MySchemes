package com.example.myschemes.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
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

class CabinetDetailActivity : AppCompatActivity() {

    // Заголовок и информация
    private lateinit var tvTitle: TextView
    private lateinit var tvEquipmentName: TextView
    private lateinit var tvCellNumber: TextView
    private lateinit var tvLastRevisionDate: TextView
    private lateinit var tvNextRevisionDate: TextView
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

    // Кнопки фото для чек-листа
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

    // Общее фото
    private lateinit var btnTakeGeneralPhoto: Button
    private lateinit var btnViewGeneralPhoto: Button
    private lateinit var ivGeneralPhotoPreview: ImageView

    // Кнопки действий
    private lateinit var btnSave: Button
    private lateinit var btnDelete: Button
    private lateinit var btnBack: Button

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
        setupPhotoButtons()

        schemeId = intent.getIntExtra("scheme_id", 0)

        val database = SchemeDatabase.getInstance(this)
        repository = SchemeRepository(database.schemeDao())

        loadSchemeData()

        btnSave.setOnClickListener { saveScheme() }
        btnDelete.setOnClickListener { confirmDelete() }
        btnBack.setOnClickListener { finish() }
    }

    private fun initViews() {
        tvTitle = findViewById(R.id.tvTitle)
        tvEquipmentName = findViewById(R.id.tvEquipmentName)
        tvCellNumber = findViewById(R.id.tvCellNumber)
        tvLastRevisionDate = findViewById(R.id.tvLastRevisionDate)
        tvNextRevisionDate = findViewById(R.id.tvNextRevisionDate)
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

        btnTakeGeneralPhoto = findViewById(R.id.btnTakeGeneralPhoto)
        btnViewGeneralPhoto = findViewById(R.id.btnViewGeneralPhoto)
        ivGeneralPhotoPreview = findViewById(R.id.ivGeneralPhotoPreview)

        btnSave = findViewById(R.id.btnSave)
        btnDelete = findViewById(R.id.btnDelete)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun initPhotosMap() {
        val keys = listOf(
            "cabinetName", "switchesName", "inventoryNumber",
            "lockIntegrity", "sealIntegrity", "cableEntries",
            "noBareWires", "addressLabels", "terminalsIntegrity",
            "painting", "heating", "grounding", "general"
        )
        keys.forEach { photosMap[it] = mutableListOf() }
    }

    private fun setupPhotoButtons() {
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
        btnTakeGeneralPhoto.setOnClickListener { showPhotoGallery("general", "Общее фото шкафа") }
        btnViewGeneralPhoto.setOnClickListener { viewPhoto(photosMap["general"]?.firstOrNull()) }
    }

    private fun showPhotoGallery(key: String, title: String) {
        val photos = photosMap[key] ?: mutableListOf()
        currentPhotoDialog = PhotoGalleryDialog(
            activity = this,
            title = title,
            photos = photos
        ) { updatedPhotos ->
            photosMap[key] = updatedPhotos.toMutableList()
            if (key == "general" && updatedPhotos.isNotEmpty()) {
                loadImagePreview(ivGeneralPhotoPreview, updatedPhotos.first())
            }
        }
        currentPhotoDialog?.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Передаём результат в PhotoHelper
        photoHelper.handleActivityResult(requestCode, resultCode, data)
        // Также передаём в диалог, если он открыт
        currentPhotoDialog?.handleActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        photoHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
        currentPhotoDialog?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun viewPhoto(path: String?) {
        if (path.isNullOrEmpty()) {
            Toast.makeText(this, "Фото не добавлено", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_VIEW)
        val uri = if (path.startsWith("content://")) {
            Uri.parse(path)
        } else {
            androidx.core.content.FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                File(path)
            )
        }
        intent.setDataAndType(uri, "image/*")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(intent)
    }

    private fun loadImagePreview(imageView: ImageView, path: String) {
        val file = if (path.startsWith("content://")) {
            return
        } else {
            File(path)
        }
        if (file.exists()) {
            val bitmap = BitmapFactory.decodeFile(path)
            imageView.setImageBitmap(bitmap)
        }
    }

    private fun loadSchemeData() {
        lifecycleScope.launch {
            val schemes = repository.getAllSchemes()
            scheme = schemes.find { it.id == schemeId }

            scheme?.let { s ->
                displayBasicInfo(s)
                displayCheckboxes(s)
                loadPhotos(s)
            }
        }
    }

    private fun displayBasicInfo(scheme: Scheme) {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        tvTitle.text = "📋 ${scheme.equipmentName}"
        tvEquipmentName.text = scheme.equipmentName
        tvCellNumber.text = scheme.cellNumber ?: "—"

        val lastDate = dateFormat.format(Date(scheme.lastRevisionDate))
        tvLastRevisionDate.text = lastDate
        etEditLastRevisionDate.setText(lastDate)

        val nextDate = dateFormat.format(Date(scheme.nextRevisionDate))
        tvNextRevisionDate.text = nextDate
        etEditNextRevisionDate.setText(nextDate)

        etSchemeNumber.setText(scheme.schemeNumber ?: "")

        val (statusText, statusColor) = getStatusInfo(scheme.nextRevisionDate)
        tvStatus.text = statusText
        tvStatus.setTextColor(statusColor)
    }

    private fun displayCheckboxes(scheme: Scheme) {
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
        photosMap["cabinetName"] = scheme.cabinetNamePhotos.toMutableList()
        photosMap["switchesName"] = scheme.switchesNamePhotos.toMutableList()
        photosMap["inventoryNumber"] = scheme.inventoryNumberPhotos.toMutableList()
        photosMap["lockIntegrity"] = scheme.lockIntegrityPhotos.toMutableList()
        photosMap["sealIntegrity"] = scheme.sealIntegrityPhotos.toMutableList()
        photosMap["cableEntries"] = scheme.cableEntriesPhotos.toMutableList()
        photosMap["noBareWires"] = scheme.noBareWiresPhotos.toMutableList()
        photosMap["addressLabels"] = scheme.addressLabelsPhotos.toMutableList()
        photosMap["terminalsIntegrity"] = scheme.terminalsIntegrityPhotos.toMutableList()
        photosMap["painting"] = scheme.paintingPhotos.toMutableList()
        photosMap["heating"] = scheme.heatingPhotos.toMutableList()
        photosMap["grounding"] = scheme.groundingPhotos.toMutableList()
        photosMap["general"] = scheme.generalPhotos.toMutableList()

        scheme.generalPhotos.firstOrNull()?.let { loadImagePreview(ivGeneralPhotoPreview, it) }
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

    private fun saveScheme() {
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
                groundingPhotos = photosMap["grounding"] ?: emptyList(),
                generalPhotos = photosMap["general"] ?: emptyList()
            )

            lifecycleScope.launch {
                val allSchemes = repository.getAllSchemes().toMutableList()
                val index = allSchemes.indexOfFirst { it.id == schemeId }
                if (index != -1) {
                    allSchemes[index] = updatedScheme
                    repository.saveSchemes(allSchemes)
                    Toast.makeText(this@CabinetDetailActivity, "Данные сохранены", Toast.LENGTH_SHORT).show()
                }
            }
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
            val allSchemes = repository.getAllSchemes().toMutableList()
            allSchemes.removeAll { it.id == schemeId }
            repository.saveSchemes(allSchemes)
            Toast.makeText(this@CabinetDetailActivity, "Схема удалена", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}