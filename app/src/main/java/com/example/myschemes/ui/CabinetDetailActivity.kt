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

class CabinetDetailActivity : AppCompatActivity() {  // ← changed to AppCompatActivity

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

    // Хранилище путей к фото
    private val photoPaths = mutableMapOf<String, String?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cabinet_detail)

        photoHelper = PhotoHelper(this)
        initViews()
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

    private fun setupPhotoButtons() {
        btnCabinetNamePhoto.setOnClickListener { showPhotoOptions("cabinetName") }
        btnSwitchesNamePhoto.setOnClickListener { showPhotoOptions("switchesName") }
        btnInventoryNumberPhoto.setOnClickListener { showPhotoOptions("inventoryNumber") }
        btnLockIntegrityPhoto.setOnClickListener { showPhotoOptions("lockIntegrity") }
        btnSealIntegrityPhoto.setOnClickListener { showPhotoOptions("sealIntegrity") }
        btnCableEntriesPhoto.setOnClickListener { showPhotoOptions("cableEntries") }
        btnNoBareWiresPhoto.setOnClickListener { showPhotoOptions("noBareWires") }
        btnAddressLabelsPhoto.setOnClickListener { showPhotoOptions("addressLabels") }
        btnTerminalsIntegrityPhoto.setOnClickListener { showPhotoOptions("terminalsIntegrity") }
        btnPaintingPhoto.setOnClickListener { showPhotoOptions("painting") }
        btnHeatingPhoto.setOnClickListener { showPhotoOptions("heating") }
        btnGroundingPhoto.setOnClickListener { showPhotoOptions("grounding") }
        btnTakeGeneralPhoto.setOnClickListener { showPhotoOptions("general") }
        btnViewGeneralPhoto.setOnClickListener { viewPhoto(photoPaths["general"]) }
    }

    private fun showPhotoOptions(key: String) {
        val options = arrayOf("📷 Сделать фото", "🖼️ Выбрать из галереи", "❌ Отмена")
        AlertDialog.Builder(this)
            .setTitle("Добавить фото")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> photoHelper.takePhoto { path -> onPhotoTaken(key, path) }
                    1 -> photoHelper.pickFromGallery { path -> onPhotoTaken(key, path) }
                }
            }
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,  // ← changed to Array<String>
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        photoHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun onPhotoTaken(key: String, path: String) {
        photoPaths[key] = path
        if (key == "general") {
            loadImagePreview(ivGeneralPhotoPreview, path)
        }
        Toast.makeText(this, "Фото добавлено", Toast.LENGTH_SHORT).show()
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
        photoPaths["cabinetName"] = scheme.cabinetNamePhoto
        photoPaths["switchesName"] = scheme.switchesNamePhoto
        photoPaths["inventoryNumber"] = scheme.inventoryNumberPhoto
        photoPaths["lockIntegrity"] = scheme.lockIntegrityPhoto
        photoPaths["sealIntegrity"] = scheme.sealIntegrityPhoto
        photoPaths["cableEntries"] = scheme.cableEntriesPhoto
        photoPaths["noBareWires"] = scheme.noBareWiresPhoto
        photoPaths["addressLabels"] = scheme.addressLabelsPhoto
        photoPaths["terminalsIntegrity"] = scheme.terminalsIntegrityPhoto
        photoPaths["painting"] = scheme.paintingPhoto
        photoPaths["heating"] = scheme.heatingPhoto
        photoPaths["grounding"] = scheme.groundingPhoto
        photoPaths["general"] = scheme.generalPhoto

        scheme.generalPhoto?.let { loadImagePreview(ivGeneralPhotoPreview, it) }
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
                cabinetNamePhoto = photoPaths["cabinetName"],
                switchesNamePhoto = photoPaths["switchesName"],
                inventoryNumberPhoto = photoPaths["inventoryNumber"],
                lockIntegrityPhoto = photoPaths["lockIntegrity"],
                sealIntegrityPhoto = photoPaths["sealIntegrity"],
                cableEntriesPhoto = photoPaths["cableEntries"],
                noBareWiresPhoto = photoPaths["noBareWires"],
                addressLabelsPhoto = photoPaths["addressLabels"],
                terminalsIntegrityPhoto = photoPaths["terminalsIntegrity"],
                paintingPhoto = photoPaths["painting"],
                heatingPhoto = photoPaths["heating"],
                groundingPhoto = photoPaths["grounding"],
                generalPhoto = photoPaths["general"]
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        photoHelper.handleActivityResult(requestCode, resultCode, data)  // ← добавить
    }
}