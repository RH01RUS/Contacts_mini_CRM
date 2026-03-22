package com.example.contactmanager.ui.contacts

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.contactmanager.R
import com.example.contactmanager.data.model.Contact
import com.example.contactmanager.data.model.LeadCategory
import com.example.contactmanager.data.model.LeadStatus

class ContactAdapter(
    private val onItemClick: (Contact) -> Unit
) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    private var contacts = listOf<Contact>()

    fun submitList(newList: List<Contact>) {
        contacts = newList
        notifyDataSetChanged()
    }

    fun getItem(position: Int): Contact = contacts[position]

    fun getItemPosition(contact: Contact): Int {
        return contacts.indexOfFirst { it.id == contact.id }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact, parent, false)
        return ContactViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(contacts[position])
    }

    override fun getItemCount(): Int = contacts.size

    class ContactViewHolder(
        itemView: View,
        private val onItemClick: (Contact) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val textName: TextView = itemView.findViewById(R.id.textName)
        private val textPhone: TextView = itemView.findViewById(R.id.textPhone)
        private val textCompany: TextView = itemView.findViewById(R.id.textCompany)
        private val textStatus: TextView = itemView.findViewById(R.id.textStatus)
        private val textCategory: TextView = itemView.findViewById(R.id.textCategory)
        private var currentContact: Contact? = null

        init {
            itemView.setOnClickListener {
                currentContact?.let { onItemClick(it) }
            }
        }

        fun bind(contact: Contact) {
            currentContact = contact
            textName.text = contact.name
            textPhone.text = contact.phone
            textCompany.text = contact.company ?: ""
            textCompany.visibility = if (contact.company.isNullOrEmpty()) View.GONE else View.VISIBLE

            // Отображение статуса
            when(contact.status) {
                LeadStatus.NEW -> {
                    textStatus.text = "Новый"
                    textStatus.setBackgroundResource(R.drawable.badge_new)
                }
                LeadStatus.IN_PROGRESS -> {
                    textStatus.text = "В работе"
                    textStatus.setBackgroundResource(R.drawable.badge_progress)
                }
                LeadStatus.NEGOTIATION -> {
                    textStatus.text = "Переговоры"
                    textStatus.setBackgroundResource(R.drawable.badge_negotiation)
                }
                LeadStatus.CONVERTED -> {
                    textStatus.text = "Конвертирован"
                    textStatus.setBackgroundResource(R.drawable.badge_converted)
                }
                LeadStatus.LOST -> {
                    textStatus.text = "Потерян"
                    textStatus.setBackgroundResource(R.drawable.badge_lost)
                }
            }
            textStatus.visibility = View.VISIBLE

            // Отображение категории
            when(contact.category) {
                LeadCategory.HOT -> {
                    textCategory.text = "🔥 Горячий"
                    textCategory.setBackgroundColor(Color.parseColor("#FF4444"))
                }
                LeadCategory.WARM -> {
                    textCategory.text = "⭐ Теплый"
                    textCategory.setBackgroundColor(Color.parseColor("#FFA500"))
                }
                LeadCategory.COLD -> {
                    textCategory.text = "❄️ Холодный"
                    textCategory.setBackgroundColor(Color.parseColor("#4CAF50"))
                }
            }
            textCategory.visibility = View.VISIBLE
        }
    }
}