package com.example.myschemes.ui

import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.myschemes.R
import java.io.File

class PhotoViewDialog(
    private val activity: AppCompatActivity,
    private val photoPath: String
) {

    fun show() {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_photo_view, null)
        val imageView = view.findViewById<ImageView>(R.id.ivFullPhoto)

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

        imageView.setImageBitmap(bitmap)

        // Клик по фото закрывает диалог
        imageView.setOnClickListener {
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