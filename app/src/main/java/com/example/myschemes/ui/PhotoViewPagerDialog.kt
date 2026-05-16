package com.example.myschemes.ui

import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.myschemes.R
import com.github.chrisbanes.photoview.PhotoView
import java.io.File

class PhotoViewPagerDialog(
    private val activity: AppCompatActivity,
    private val photoPaths: List<String>,
    private val startPosition: Int = 0
) {

    fun show() {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_photo_view_pager, null)
        val viewPager = view.findViewById<ViewPager2>(R.id.viewPager)
        val btnClose = view.findViewById<ImageButton>(R.id.btnClose)

        val adapter = PhotoPagerAdapter(photoPaths)
        viewPager.adapter = adapter
        viewPager.currentItem = startPosition

        btnClose.setOnClickListener {
            (view.parent as? ViewGroup)?.let {
                (it.parent as? AlertDialog)?.dismiss()
            }
        }

        val dialog = AlertDialog.Builder(activity)
            .setView(view)
            .setCancelable(true)
            .create()

        dialog.show()
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
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
            }
        }
    }
}