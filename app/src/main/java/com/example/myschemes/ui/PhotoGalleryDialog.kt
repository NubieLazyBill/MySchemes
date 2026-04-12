package com.example.myschemes.ui

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.myschemes.R
import com.example.myschemes.utils.PhotoHelper
import java.io.File

class PhotoGalleryDialog(
    private val activity: AppCompatActivity,
    private val title: String,
    private val photos: MutableList<String>,
    private val onPhotosChanged: (List<String>) -> Unit
) {

    private lateinit var photoHelper: PhotoHelper
    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>

    fun show() {
        photoHelper = PhotoHelper(activity)

        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_photo_gallery, null)
        listView = view.findViewById(R.id.listViewPhotos)
        val btnAdd = view.findViewById<Button>(R.id.btnAddPhoto)

        adapter = object : ArrayAdapter<String>(activity, android.R.layout.simple_list_item_1, photos) {
            override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                val itemView = super.getView(position, convertView, parent)
                val photoPath = photos[position]
                val tv = itemView as TextView

                val fileName = if (photoPath.startsWith("content://")) {
                    "Фото из галереи"
                } else {
                    File(photoPath).name
                }
                tv.text = "📸 ${fileName.take(30)}"
                tv.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.ic_menu_gallery, 0)
                tv.compoundDrawablePadding = 16
                return itemView
            }
        }
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            PhotoViewDialog(activity, photos[position]).show()
        }

        listView.setOnItemLongClickListener { _, _, position, _ ->
            showDeleteConfirm(position)
            true
        }

        btnAdd.setOnClickListener {
            showAddPhotoOptions()
        }

        AlertDialog.Builder(activity)
            .setTitle("📸 $title")
            .setView(view)
            .setPositiveButton("Закрыть") { _, _ -> }
            .show()
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        photoHelper.handleActivityResult(requestCode, resultCode, data)
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        photoHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun showAddPhotoOptions() {
        val options = arrayOf("📷 Сделать фото", "🖼️ Выбрать из галереи", "❌ Отмена")
        AlertDialog.Builder(activity)
            .setTitle("Добавить фото")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> takePhoto()
                    1 -> pickFromGallery()
                }
            }
            .show()
    }

    private fun takePhoto() {
        photoHelper.takePhoto { path ->
            android.util.Log.d("PhotoGallery", "Добавляем фото: $path")
            photos.add(path)
            adapter.notifyDataSetChanged()
            onPhotosChanged(photos)
        }
    }

    private fun pickFromGallery() {
        photoHelper.pickFromGallery { path ->
            android.util.Log.d("PhotoGallery", "Фото из галереи: $path")
            photos.add(path)
            adapter.notifyDataSetChanged()
            onPhotosChanged(photos)
        }
    }

    private fun showDeleteConfirm(position: Int) {
        AlertDialog.Builder(activity)
            .setTitle("Удалить фото")
            .setMessage("Вы уверены?")
            .setPositiveButton("Да") { _, _ ->
                photos.removeAt(position)
                adapter.notifyDataSetChanged()
                onPhotosChanged(photos)
            }
            .setNegativeButton("Нет", null)
            .show()
    }
}