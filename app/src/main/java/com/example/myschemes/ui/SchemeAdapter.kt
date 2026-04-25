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

            // Показываем дату только если есть схема и дата не 0
            if (scheme.hasScheme && scheme.nextRevisionDate != 0L) {
                tvNextRevisionDate.text = dateFormat.format(Date(scheme.nextRevisionDate))
            } else {
                tvNextRevisionDate.text = "—"
            }
            tvNextRevisionDate.visibility = if (scheme.hasScheme) View.VISIBLE else View.GONE

            val (statusText, bgColor, textColor) = getStatusInfo(scheme)
            tvStatus.text = statusText
            tvStatus.setBackgroundColor(bgColor)
            tvStatus.setTextColor(textColor)

            cardView.setOnClickListener { onItemClick(scheme) }
        }

        private fun getStatusInfo(scheme: Scheme): Triple<String, Int, Int> {
            // Если схема не требуется
            if (!scheme.hasScheme) {
                return Triple(
                    "📄 Не требуется",
                    Color.parseColor("#EEEEEE"),
                    Color.parseColor("#999999")
                )
            }
            // Если схема есть, но даты не заполнены
            if (scheme.lastRevisionDate == 0L || scheme.nextRevisionDate == 0L) {
                return Triple(
                    "📄 Нет схемы",
                    Color.parseColor("#EEEEEE"),
                    Color.parseColor("#999999")
                )
            }

            val today = System.currentTimeMillis()
            val daysLeft = ((scheme.nextRevisionDate - today) / (1000 * 60 * 60 * 24)).toInt()
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