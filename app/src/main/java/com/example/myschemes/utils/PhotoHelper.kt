package com.example.myschemes.utils

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PhotoHelper(private val activity: AppCompatActivity) {

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 100
        private const val REQUEST_IMAGE_CAPTURE = 101
        private const val REQUEST_GALLERY = 102
    }

    private var currentPhotoPath: String? = null
    private var currentCallback: ((String) -> Unit)? = null
    private var currentEquipmentName: String = ""
    private var currentCheckpointName: String = ""

    fun takePhoto(equipmentName: String, checkpointName: String, onPhotoTaken: (String) -> Unit) {
        currentEquipmentName = equipmentName
        currentCheckpointName = checkpointName
        currentCallback = onPhotoTaken

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.CAMERA),
                    REQUEST_CAMERA_PERMISSION
                )
                return
            }
        }

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(activity.packageManager) != null) {
            val photoFile = createPermanentImageFile(equipmentName, checkpointName)
            photoFile?.let {
                val photoURI = FileProvider.getUriForFile(
                    activity,
                    "${activity.packageName}.fileprovider",
                    it
                )
                currentPhotoPath = it.absolutePath
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                activity.startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    fun pickFromGallery(equipmentName: String, checkpointName: String, onPhotoSelected: (String) -> Unit) {
        currentEquipmentName = equipmentName
        currentCheckpointName = checkpointName
        currentCallback = onPhotoSelected
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        activity.startActivityForResult(intent, REQUEST_GALLERY)
    }

    private fun createPermanentImageFile(equipmentName: String, checkpointName: String): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

        // Очищаем название шкафа от недопустимых символов, пробелы заменяем на _
        val cleanEquipmentName = equipmentName
            .replace("[^a-zA-Zа-яА-Я0-9]".toRegex(), "_")
            .replace(" ", "_")
            .take(30)

        // Очищаем название пункта, пробелы заменяем на _
        val cleanCheckpointName = checkpointName
            .replace("[^a-zA-Zа-яА-Я0-9]".toRegex(), "_")
            .replace(" ", "_")
            .take(50)

        // Разделитель | между шкафом и пунктом
        val fileName = "${cleanEquipmentName}|${cleanCheckpointName}_$timeStamp.jpg"

        val storageDir = File(activity.filesDir, "photos")
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        return File(storageDir, fileName)
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        android.util.Log.d("PhotoHelper", "handleActivityResult: requestCode=$requestCode, resultCode=$resultCode")
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    android.util.Log.d("PhotoHelper", "Фото с камеры: $currentPhotoPath")
                    currentPhotoPath?.let {
                        currentCallback?.invoke(it)
                    }
                }
                REQUEST_GALLERY -> {
                    val uri = data?.data
                    android.util.Log.d("PhotoHelper", "URI из галереи: $uri")
                    uri?.let {
                        val savedPath = FileHelper.savePhotoToAppStorage(
                            activity,
                            it.toString(),
                            currentEquipmentName,
                            currentCheckpointName
                        )
                        savedPath?.let { path ->
                            currentCallback?.invoke(path)
                        }
                    }
                }
            }
        }
        currentCallback = null
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                currentCallback?.let {
                    takePhoto(currentEquipmentName, currentCheckpointName, it)
                }
            } else {
                currentCallback = null
            }
        }
    }
}