package com.example.myschemes.ui

import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.viewpager2.widget.ViewPager2
import com.example.myschemes.R
import com.github.chrisbanes.photoview.PhotoView
import java.io.File
import android.widget.TextView

class PhotoViewPagerDialog(
    private val activity: AppCompatActivity,
    private val photoPaths: List<String>,
    private val startPosition: Int = 0
) {

    fun show() {
        if (photoPaths.isEmpty()) return

        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_photo_view_pager, null)
        val viewPager = view.findViewById<ViewPager2>(R.id.viewPager)
        val btnClose = view.findViewById<ImageButton>(R.id.btnClose)
        val btnShare = view.findViewById<ImageButton>(R.id.btnShare)
        val tvCounter = view.findViewById<TextView>(R.id.tvPhotoCounter)

        val adapter = PhotoPagerAdapter(photoPaths)
        viewPager.adapter = adapter
        viewPager.currentItem = startPosition

        // Обновляем счётчик
        tvCounter.text = "${startPosition + 1} / ${photoPaths.size}"
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                tvCounter.text = "${position + 1} / ${photoPaths.size}"
            }
        })

        val dialog = AlertDialog.Builder(activity)
            .setView(view)
            .setCancelable(true)
            .create()

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        // Кнопка "Поделиться"
        btnShare.setOnClickListener {
            val currentPosition = viewPager.currentItem
            if (currentPosition in photoPaths.indices) {
                sharePhoto(photoPaths[currentPosition])
            }
        }

        dialog.show()
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    private fun sharePhoto(photoPath: String) {
        try {
            val file = File(photoPath)
            if (!file.exists()) {
                android.widget.Toast.makeText(activity, "Файл не найден", android.widget.Toast.LENGTH_SHORT).show()
                return
            }

            val uri = FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            activity.startActivity(Intent.createChooser(shareIntent, "Отправить фото"))
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(activity, "Ошибка отправки: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    inner class PhotoPagerAdapter(private val paths: List<String>) :
        androidx.recyclerview.widget.RecyclerView.Adapter<PhotoPagerAdapter.PhotoViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_photo_view, parent, false)
            return PhotoViewHolder(view)
        }

        override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
            holder.bind(paths[position])
        }

        override fun getItemCount(): Int = paths.size

        inner class PhotoViewHolder(itemView: View) :
            androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
            private val photoView: PhotoView = itemView.findViewById(R.id.photoView)

            fun bind(photoPath: String) {
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
                photoView.maximumScale = 5.0f
                photoView.minimumScale = 0.8f
                photoView.mediumScale = 1.5f

                // Долгое нажатие — меню (отправить/удалить)
                photoView.setOnLongClickListener {
                    showPhotoMenu(photoPath)
                    true
                }
            }
        }
    }

    private fun showPhotoMenu(photoPath: String) {
        val options = arrayOf("📤 Отправить", "❌ Отмена")
        AlertDialog.Builder(activity)
            .setTitle("Действия с фото")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> sharePhoto(photoPath)
                }
            }
            .show()
    }
}