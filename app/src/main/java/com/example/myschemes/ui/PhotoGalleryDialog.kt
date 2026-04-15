package com.example.myschemes.ui

import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.myschemes.R
import com.example.myschemes.utils.FileHelper
import com.example.myschemes.utils.PhotoHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PhotoGalleryDialog(
    private val activity: AppCompatActivity,
    private val title: String,
    private val photos: MutableList<String>,
    private val onPhotosChanged: (List<String>) -> Unit
) {

    private lateinit var photoHelper: PhotoHelper
    private lateinit var listView: ListView
    private lateinit var adapter: PhotoListAdapter

    fun show() {
        photoHelper = PhotoHelper(activity)

        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_photo_gallery, null)
        listView = view.findViewById(R.id.listViewPhotos)
        val btnAdd = view.findViewById<Button>(R.id.btnAddPhoto)

        adapter = PhotoListAdapter(activity, photos)
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
            android.util.Log.d("PhotoGallery", "Фото получено: $path")
            photos.add(path)
            adapter.notifyDataSetChanged()
            onPhotosChanged(photos)
        }
    }

    private fun pickFromGallery() {
        photoHelper.pickFromGallery { path ->
            android.util.Log.d("PhotoGallery", "Фото из галереи сохранено: $path")
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
                val photoPath = photos[position]
                FileHelper.deletePhotoFromAppStorage(activity, photoPath)
                photos.removeAt(position)
                adapter.notifyDataSetChanged()
                onPhotosChanged(photos)
            }
            .setNegativeButton("Нет", null)
            .show()
    }

    // Внутренний адаптер с миниатюрами
    inner class PhotoListAdapter(
        private val context: android.content.Context,
        private val photoList: List<String>
    ) : BaseAdapter() {

        // Формат только даты (без времени)
        private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        override fun getCount(): Int = photoList.size

        override fun getItem(position: Int): Any = photoList[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)

            val text1 = view.findViewById<TextView>(android.R.id.text1)
            val photoPath = photoList[position]

            // Название = дата создания файла (только дата, без времени)
            val fileName = if (photoPath.startsWith("content://")) {
                "Фото из галереи"
            } else {
                val file = File(photoPath)
                if (file.exists()) {
                    val lastModified = Date(file.lastModified())
                    dateFormat.format(lastModified)  // только дата
                } else {
                    "Фото"
                }
            }

            text1.text = fileName

            // Загружаем миниатюру (увеличенная в 2 раза: 320x240 вместо 160x120)
            loadThumbnail(photoPath) { bitmap ->
                val drawable = android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
                drawable.setBounds(0, 0, 320, 240)  // увеличенная миниатюра 320x240 (в 2 раза больше)
                text1.setCompoundDrawables(drawable, null, null, null)
                text1.compoundDrawablePadding = 20
                // Увеличиваем высоту строки, чтобы поместилась миниатюра
                text1.minHeight = 260
            }

            return view
        }

        private fun loadThumbnail(photoPath: String, onResult: (android.graphics.Bitmap) -> Unit) {
            Thread {
                val bitmap = if (photoPath.startsWith("content://")) {
                    val uri = Uri.parse(photoPath)
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                } else {
                    val file = File(photoPath)
                    if (file.exists()) {
                        val options = BitmapFactory.Options()
                        options.inSampleSize = 2  // уменьшаем меньше для лучшего качества
                        BitmapFactory.decodeFile(photoPath, options)
                    } else null
                }
                bitmap?.let {
                    (activity as? AppCompatActivity)?.runOnUiThread {
                        onResult(it)
                    }
                }
            }.start()
        }
    }
}