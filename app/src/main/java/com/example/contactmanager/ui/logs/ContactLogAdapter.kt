package com.example.contactmanager.ui.logs

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.contactmanager.R
import com.example.contactmanager.data.model.ContactLog
import java.text.SimpleDateFormat
import java.util.*

class ContactLogAdapter(
    private val onItemClick: (ContactLog) -> Unit
) : RecyclerView.Adapter<ContactLogAdapter.ContactLogViewHolder>() {

    private var logs = listOf<ContactLog>()
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    fun submitList(newList: List<ContactLog>) {
        logs = newList
        notifyDataSetChanged()
    }

    fun getItem(position: Int): ContactLog {
        return logs[position]
    }

    fun getItemPosition(log: ContactLog): Int {
        return logs.indexOfFirst { it.id == log.id }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactLogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact_log, parent, false)
        return ContactLogViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: ContactLogViewHolder, position: Int) {
        holder.bind(logs[position])
    }

    override fun getItemCount(): Int = logs.size

    class ContactLogViewHolder(
        itemView: View,
        private val onItemClick: (ContactLog) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvContactName: TextView = itemView.findViewById(R.id.tvContactName)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvType: TextView = itemView.findViewById(R.id.tvType)
        private val progressQuality: ProgressBar = itemView.findViewById(R.id.progressQuality)
        private val tvQuality: TextView = itemView.findViewById(R.id.tvQuality)
        private val tvNote: TextView = itemView.findViewById(R.id.tvNote)
        private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        private var currentLog: ContactLog? = null

        init {
            itemView.setOnClickListener {
                currentLog?.let { onItemClick(it) }
            }
        }

        fun bind(log: ContactLog) {
            currentLog = log

            tvContactName.text = log.contactName
            tvDate.text = dateFormat.format(log.date)
            tvType.text = log.type
            progressQuality.progress = log.quality
            tvQuality.text = "${log.quality}%"

            if (!log.note.isNullOrEmpty()) {
                tvNote.text = log.note
                tvNote.visibility = View.VISIBLE
            } else {
                tvNote.visibility = View.GONE
            }
        }
    }
}