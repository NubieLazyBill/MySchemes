package com.example.myschemes.utils

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PhotoHelper(private val activity: FragmentActivity) {

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 100
        private const val REQUEST_IMAGE_CAPTURE = 101
    }

    private var currentPhotoPath: String? = null
    private var currentCallback: ((String) -> Unit)? = null

    // Регистрируем ланчер для выбора фото из галереи (без разрешений!)
    private lateinit var galleryLauncher: ActivityResultLauncher<String>

    init {
        galleryLauncher = activity.registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                currentCallback?.invoke(it.toString())
            }
            currentCallback = null
        }
    }

    fun takePhoto(onPhotoTaken: (String) -> Unit) {
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
            val photoFile = createImageFile()
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

    fun pickFromGallery(onPhotoSelected: (String) -> Unit) {
        currentCallback = onPhotoSelected
        galleryLauncher.launch("image/*")
    }

    private fun createImageFile(): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = activity.getExternalFilesDir(null)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            currentPhotoPath?.let {
                currentCallback?.invoke(it)
            }
            currentCallback = null
        }
    }
}