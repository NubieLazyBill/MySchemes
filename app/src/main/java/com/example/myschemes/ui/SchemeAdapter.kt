package com.example.myschemes.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.myschemes.R
import com.example.myschemes.data.model.Scheme
import java.text.SimpleDateFormat
import java.util.*

class SchemeAdapter(
    private var schemes: List<Scheme>,
    private val onItemClick: (Scheme) -> Unit
) : RecyclerView.Adapter<SchemeAdapter.SchemeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SchemeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_scheme, parent, false)
        return SchemeViewHolder(view)
    }

    override fun onBindViewHolder(holder: SchemeViewHolder, position: Int) {
        holder.bind(schemes[position], onItemClick)
    }

    override fun getItemCount() = schemes.size

    fun updateData(newSchemes: List<Scheme>) {
        schemes = newSchemes
        notifyDataSetChanged()
    }

    class SchemeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvEquipmentName: TextView = itemView.findViewById(R.id.tvEquipmentName)
        private val tvCellNumber: TextView = itemView.findViewById(R.id.tvCellNumber)
        private val tvNextRevisionDate: TextView = itemView.findViewById(R.id.tvNextRevisionDate)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val cardView: CardView = itemView as CardView

        fun bind(scheme: Scheme, onItemClick: (Scheme) -> Unit) {
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

            tvEquipmentName.text = scheme.equipmentName
            tvCellNumber.text = "Ячейка: ${scheme.cellNumber ?: "—"}"
            tvNextRevisionDate.text = dateFormat.format(Date(scheme.nextRevisionDate))

            val (statusText, bgColor, textColor) = getStatusInfo(scheme.nextRevisionDate)
            tvStatus.text = statusText
            tvStatus.setBackgroundColor(bgColor)
            tvStatus.setTextColor(textColor)  // ← применяем цвет текста

            cardView.setOnClickListener { onItemClick(scheme) }
        }

        private fun getStatusInfo(nextRevisionDate: Long): Triple<String, Int, Int> {
            val today = System.currentTimeMillis()
            val daysLeft = ((nextRevisionDate - today) / (1000 * 60 * 60 * 24)).toInt()
            return when {
                daysLeft < 0 -> Triple(
                    "🔴 Просрочено",
                    Color.parseColor("#FFEBEE"),
                    Color.parseColor("#D32F2F")
                )
                daysLeft <= 30 -> Triple(
                    "🟡 Скоро истекает",
                    Color.parseColor("#FFF3E0"),
                    Color.parseColor("#F57C00")
                )
                else -> Triple(
                    "✅ Активна",
                    Color.parseColor("#E8F5E9"),
                    Color.parseColor("#388E3C")
                )
            }
        }
    }
}