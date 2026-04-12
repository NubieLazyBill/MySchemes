package com.example.myschemes.ui

import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import com.example.myschemes.R
import com.github.chrisbanes.photoview.PhotoView
import java.io.File

class PhotoViewDialog(
    private val activity: AppCompatActivity,
    private val photoPath: String
) {

    fun show() {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_photo_view, null)
        val photoView = view.findViewById<PhotoView>(R.id.photoView)

        // Загружаем фото
        val bitmap = if (photoPath.startsWith("content://")) {
            val uri = Uri.parse(photoPath)
            activity.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } else {
            val file = File(photoPath)
            if (file.exists()) BitmapFactory.decodeFile(photoPath) else null
        }

        photoView.setImageBitmap(bitmap)

        // Настройка зума (в коде)
        photoView.maximumScale = 5.0f
        photoView.minimumScale = 0.8f
        photoView.mediumScale = 1.5f

        // Клик по фото закрывает диалог
        photoView.setOnClickListener {
            (view.parent as? android.view.ViewGroup)?.let {
                (it.parent as? AlertDialog)?.dismiss()
            }
        }

        AlertDialog.Builder(activity)
            .setView(view)
            .setPositiveButton("Закрыть") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}