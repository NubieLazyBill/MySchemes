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
    private var equipmentName: String = ""
    private var checkpointTitle: String = ""

    fun show(equipmentName: String, checkpointTitle: String) {
        this.equipmentName = equipmentName
        this.checkpointTitle = checkpointTitle
        photoHelper = PhotoHelper(activity)

        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_photo_gallery, null)
        listView = view.findViewById(R.id.listViewPhotos)
        val btnAdd = view.findViewById<Button>(R.id.btnAddPhoto)

        adapter = PhotoListAdapter(activity, photos)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            PhotoViewPagerDialog(activity, photos, position).show()
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
        photoHelper.takePhoto(equipmentName, checkpointTitle) { path ->
            android.util.Log.d("PhotoGallery", "Фото получено: $path")
            photos.add(path)
            adapter.notifyDataSetChanged()
            onPhotosChanged(photos)
        }
    }

    private fun pickFromGallery() {
        photoHelper.pickFromGallery(equipmentName, checkpointTitle) { path ->
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

    // ТОЛЬКО ОДИН КЛАСС PhotoListAdapter (правильная версия)
    inner class PhotoListAdapter(
        private val context: android.content.Context,
        private val photoList: List<String>
    ) : BaseAdapter() {

        override fun getCount(): Int = photoList.size

        override fun getItem(position: Int): Any = photoList[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)

            val text1 = view.findViewById<TextView>(android.R.id.text1)
            val photoPath = photoList[position]
            val file = File(photoPath)

            // Получаем имя файла без расширения
            val fileName = file.nameWithoutExtension

            // Ищем дату в конце имени файла (8 цифр подряд)
            val datePattern = Regex("(\\d{8})(?:_\\d{6})?$")
            val dateMatch = datePattern.find(fileName)

            val displayText = if (dateMatch != null) {
                val datePart = dateMatch.groupValues[1]
                val beforeDate = fileName.substring(0, dateMatch.range.first).trimEnd('_')

                // Ищем разделитель |
                val separatorIndex = beforeDate.indexOf('|')

                val checkpointName = if (separatorIndex != -1) {
                    // Берём всё ПОСЛЕ | и заменяем _ на пробелы
                    beforeDate.substring(separatorIndex + 1).replace("_", " ")
                } else {
                    // Для старых файлов (без |) - пробуем старый способ
                    val segments = beforeDate.split("_")
                    if (segments.size > 1) {
                        segments.drop(1).joinToString(" ")
                    } else {
                        beforeDate.replace("_", " ")
                    }
                }

                val formattedDate = if (datePart.length == 8) {
                    "${datePart.take(4)}.${datePart.substring(4, 6)}.${datePart.takeLast(2)}"
                } else {
                    datePart
                }

                "$checkpointName\n$formattedDate"
            } else {
                // Для старых фото - только дата из файла
                val lastModified = Date(file.lastModified())
                val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
                dateFormat.format(lastModified)
            }

            text1.text = displayText
            text1.setSingleLine(false)
            text1.maxLines = 2
            text1.textSize = 14f

            // Загружаем миниатюру
            loadThumbnail(photoPath) { bitmap ->
                val drawable = android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
                drawable.setBounds(0, 0, 280, 200)
                text1.setCompoundDrawables(drawable, null, null, null)
                text1.compoundDrawablePadding = 16
                text1.minHeight = 220
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
                        options.inSampleSize = 2
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