package com.example.myschemes.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.example.myschemes.R

class StatusSpinnerAdapter(
    context: Context,
    private val items: List<StatusItem>
) : BaseAdapter(), SpinnerAdapter {

    private val inflater = LayoutInflater.from(context)

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): StatusItem = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    // Вид для закрытого спиннера (показывает только иконку)
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: inflater.inflate(android.R.layout.simple_list_item_1, parent, false)
        val textView = view as TextView

        val item = items[position]
        textView.text = item.icon
        textView.textSize = 20f
        textView.gravity = android.view.Gravity.CENTER

        return view
    }

    // Вид для выпадающего списка (показывает иконку + текст)
    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: inflater.inflate(android.R.layout.simple_list_item_1, parent, false)
        val textView = view as TextView

        val item = items[position]
        textView.text = "${item.icon} ${item.text}"
        textView.textSize = 16f

        return view
    }
}

data class StatusItem(
    val icon: String,
    val text: String,
    val value: Int
)