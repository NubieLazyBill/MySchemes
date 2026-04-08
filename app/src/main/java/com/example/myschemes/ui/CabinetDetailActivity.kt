package com.example.myschemes.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.myschemes.R
import com.example.myschemes.data.database.SchemeDatabase
import com.example.myschemes.data.model.Scheme
import com.example.myschemes.data.model.SchemeStatus
import com.example.myschemes.data.repository.SchemeRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CabinetDetailActivity : AppCompatActivity() {

    // Основные поля
    private lateinit var tvTitle: TextView
    private lateinit var tvEquipmentName: TextView
    private lateinit var tvCellNumber: TextView
    private lateinit var tvSchemeNumber: TextView
    private lateinit var tvLastRevisionDate: TextView
    private lateinit var tvNextRevisionDate: TextView
    private lateinit var tvStatus: TextView

    // Поля для редактирования
    private lateinit var etCabinetName: TextInputEditText
    private lateinit var etSwitchesName: TextInputEditText
    private lateinit var etInventoryNumber: TextInputEditText

    // Чек-боксы
    private lateinit var cbLockIntegrity: CheckBox
    private lateinit var cbSealIntegrity: CheckBox
    private lateinit var cbCableEntries: CheckBox
    private lateinit var cbNoBareWires: CheckBox
    private lateinit var cbAddressLabels: CheckBox
    private lateinit var cbTerminalsIntegrity: CheckBox
    private lateinit var cbPainting: CheckBox
    private lateinit var cbHeating: CheckBox
    private lateinit var cbGrounding: CheckBox

    // Фото
    private lateinit var btnTakePhoto: MaterialButton
    private lateinit var btnViewPhoto: MaterialButton
    private lateinit var ivPhotoPreview: ImageView

    // Кнопки действий
    private lateinit var btnSave: MaterialButton
    private lateinit var btnDelete: MaterialButton
    private lateinit var btnBack: MaterialButton

    private lateinit var repository: SchemeRepository
    private var scheme: Scheme? = null
    private var schemeId: Int = 0
    private var currentPhotoPath: String? = null

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val REQUEST_CAMERA_PERMISSION = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cabinet_detail)

        initViews()

        schemeId = intent.getIntExtra("scheme_id", 0)

        val database = SchemeDatabase.getInstance(this)
        repository = SchemeRepository(database.schemeDao())

        loadSchemeData()

        btnTakePhoto.setOnClickListener { takePhoto() }
        btnViewPhoto.setOnClickListener { viewPhoto() }
        btnSave.setOnClickListener { saveScheme() }
        btnDelete.setOnClickListener { confirmDelete() }
        btnBack.setOnClickListener { finish() }
    }

    private fun initViews() {
        tvTitle = findViewById(R.id.tvTitle)
        tvEquipmentName = findViewById(R.id.tvEquipmentName)
        tvCellNumber = findViewById(R.id.tvCellNumber)
        tvSchemeNumber = findViewById(R.id.tvSchemeNumber)
        tvLastRevisionDate = findViewById(R.id.tvLastRevisionDate)
        tvNextRevisionDate = findViewById(R.id.tvNextRevisionDate)
        tvStatus = findViewById(R.id.tvStatus)

        etCabinetName = findViewById(R.id.etCabinetName)
        etSwitchesName = findViewById(R.id.etSwitchesName)
        etInventoryNumber = findViewById(R.id.etInventoryNumber)

        cbLockIntegrity = findViewById(R.id.cbLockIntegrity)
        cbSealIntegrity = findViewById(R.id.cbSealIntegrity)
        cbCableEntries = findViewById(R.id.cbCableEntries)
        cbNoBareWires = findViewById(R.id.cbNoBareWires)
        cbAddressLabels = findViewById(R.id.cbAddressLabels)
        cbTerminalsIntegrity = findViewById(R.id.cbTerminalsIntegrity)
        cbPainting = findViewById(R.id.cbPainting)
        cbHeating = findViewById(R.id.cbHeating)
        cbGrounding = findViewById(R.id.cbGrounding)

        btnTakePhoto = findViewById(R.id.btnTakePhoto)
        btnViewPhoto = findViewById(R.id.btnViewPhoto)
        ivPhotoPreview = findViewById(R.id.ivPhotoPreview)

        btnSave = findViewById(R.id.btnSave)
        btnDelete = findViewById(R.id.btnDelete)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun loadSchemeData() {
        lifecycleScope.launch {
            val schemes = repository.getAllSchemes()
            scheme = schemes.find { it.id == schemeId }

            scheme?.let { s ->
                displayBasicInfo(s)
                displayInspectionData(s)
                currentPhotoPath = s.photoPath
                if (!currentPhotoPath.isNullOrEmpty()) {
                    loadPhotoPreview()
                }
            }
        }
    }

    private fun displayBasicInfo(scheme: Scheme) {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        tvTitle.text = "📋 ${scheme.equipmentName}"
        tvEquipmentName.text = scheme.equipmentName
        tvCellNumber.text = scheme.cellNumber ?: "—"
        tvSchemeNumber.text = scheme.schemeNumber ?: "—"
        tvLastRevisionDate.text = dateFormat.format(Date(scheme.lastRevisionDate))
        tvNextRevisionDate.text = dateFormat.format(Date(scheme.nextRevisionDate))

        val (statusText, statusColor) = getStatusInfo(scheme.nextRevisionDate)
        tvStatus.text = statusText
        tvStatus.setTextColor(statusColor)
    }

    private fun displayInspectionData(scheme: Scheme) {
        etCabinetName.setText(scheme.cabinetName ?: "")
        etSwitchesName.setText(scheme.switchesName ?: "")
        etInventoryNumber.setText(scheme.inventoryNumber ?: "")

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

    private fun getStatusInfo(nextRevisionDate: Long): Pair<String, Int> {
        val today = System.currentTimeMillis()
        val daysLeft = ((nextRevisionDate - today) / (1000 * 60 * 60 * 24)).toInt()
        return when {
            daysLeft < 0 -> "🔴 Просрочено" to Color.RED
            daysLeft <= 30 -> "🟡 Скоро истекает" to Color.parseColor("#FF9800")
            else -> "✅ Активна" to Color.GREEN
        }
    }

    private fun takePhoto() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    REQUEST_CAMERA_PERMISSION
                )
                return
            }
        }

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            val photoFile = createImageFile()
            photoFile?.let {
                val photoURI = FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    it
                )
                currentPhotoPath = it.absolutePath
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    private fun createImageFile(): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir("photos")
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            loadPhotoPreview()
        }
    }

    private fun loadPhotoPreview() {
        currentPhotoPath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(path)
                ivPhotoPreview.setImageBitmap(bitmap)
                ivPhotoPreview.visibility = ImageView.VISIBLE
            }
        }
    }

    private fun viewPhoto() {
        currentPhotoPath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                val intent = Intent(Intent.ACTION_VIEW)
                val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
                intent.setDataAndType(uri, "image/*")
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Фото не найдено", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveScheme() {
        scheme?.let { s ->
            val updatedScheme = s.copy(
                cabinetName = etCabinetName.text.toString().takeIf { it.isNotEmpty() },
                lockIntegrity = cbLockIntegrity.isChecked,
                sealIntegrity = cbSealIntegrity.isChecked,
                cableEntries = cbCableEntries.isChecked,
                switchesName = etSwitchesName.text.toString().takeIf { it.isNotEmpty() },
                noBareWires = cbNoBareWires.isChecked,
                addressLabels = cbAddressLabels.isChecked,
                terminalsIntegrity = cbTerminalsIntegrity.isChecked,
                painting = cbPainting.isChecked,
                inventoryNumber = etInventoryNumber.text.toString().takeIf { it.isNotEmpty() },
                heating = cbHeating.isChecked,
                grounding = cbGrounding.isChecked,
                photoPath = currentPhotoPath
            )

            lifecycleScope.launch {
                val allSchemes = repository.getAllSchemes().toMutableList()
                val index = allSchemes.indexOfFirst { it.id == schemeId }
                if (index != -1) {
                    allSchemes[index] = updatedScheme
                    repository.saveSchemes(allSchemes)
                    Toast.makeText(this@CabinetDetailActivity, "Данные сохранены", Toast.LENGTH_SHORT).show()
                    loadSchemeData() // обновляем отображение
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