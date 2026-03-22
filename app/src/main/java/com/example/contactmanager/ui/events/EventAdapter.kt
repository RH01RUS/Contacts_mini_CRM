package com.example.contactmanager.ui.events

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.contactmanager.R
import com.example.contactmanager.data.model.Event
import java.text.SimpleDateFormat
import java.util.*

class EventAdapter(
    private val onItemClick: (Event) -> Unit,
    private val onCallClick: (Event) -> Unit,
    private val onSmsClick: (Event) -> Unit,
    private val onEmailClick: (Event) -> Unit,
    private val onWhatsappClick: (Event) -> Unit,
    private val onTelegramClick: (Event) -> Unit,
    private val onSyncClick: (Event) -> Unit
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    private val TAG = "EventAdapter"
    private var events = listOf<Event>()
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun submitList(newList: List<Event>) {
        events = newList
        notifyDataSetChanged()
    }

    fun getItem(position: Int): Event = events[position]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
        return EventViewHolder(
            view,
            onItemClick,
            onCallClick,
            onSmsClick,
            onEmailClick,
            onWhatsappClick,
            onTelegramClick,
            onSyncClick
        )
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(events[position])
    }
    fun getItemPosition(event: Event): Int {
        return events.indexOfFirst { it.id == event.id }
    }
    override fun getItemCount(): Int = events.size

    class EventViewHolder(
        itemView: View,
        private val onItemClick: (Event) -> Unit,
        private val onCallClick: (Event) -> Unit,
        private val onSmsClick: (Event) -> Unit,
        private val onEmailClick: (Event) -> Unit,
        private val onWhatsappClick: (Event) -> Unit,
        private val onTelegramClick: (Event) -> Unit,
        private val onSyncClick: (Event) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val textContactName: TextView = itemView.findViewById(R.id.textContactName)
        private val textContactPhone: TextView = itemView.findViewById(R.id.textContactPhone)
        private val textDateTime: TextView = itemView.findViewById(R.id.textDateTime)
        private val textTimeStatus: TextView = itemView.findViewById(R.id.textTimeStatus)
        private val textEventType: TextView = itemView.findViewById(R.id.textEventType)
        private val textNote: TextView = itemView.findViewById(R.id.textNote)
        private val textStatus: TextView = itemView.findViewById(R.id.textStatus)
        private val textCategory: TextView = itemView.findViewById(R.id.textCategory)
        private val btnCallContact: ImageButton = itemView.findViewById(R.id.btnCallContact)
        private val btnSmsContact: ImageButton = itemView.findViewById(R.id.btnSmsContact)
        private val btnEmailContact: ImageButton = itemView.findViewById(R.id.btnEmailContact)
        private val btnWhatsappContact: ImageButton = itemView.findViewById(R.id.btnWhatsappContact)
        private val btnTelegramContact: ImageButton = itemView.findViewById(R.id.btnTelegramContact)
        private val btnSync: ImageButton = itemView.findViewById(R.id.btnSync)
        private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        private var currentEvent: Event? = null

        init {
            itemView.setOnClickListener {
                currentEvent?.let { onItemClick(it) }
            }

            btnCallContact.setOnClickListener {
                currentEvent?.let { onCallClick(it) }
            }

            btnSmsContact.setOnClickListener {
                currentEvent?.let { onSmsClick(it) }
            }

            btnEmailContact.setOnClickListener {
                currentEvent?.let { onEmailClick(it) }
            }

            btnWhatsappContact.setOnClickListener {
                currentEvent?.let { onWhatsappClick(it) }
            }

            btnTelegramContact.setOnClickListener {
                currentEvent?.let { onTelegramClick(it) }
            }

            btnSync.setOnClickListener {
                currentEvent?.let { onSyncClick(it) }
            }
        }

        private fun getTimeStatusText(eventDateTime: Date): Pair<String, Int> {
            val now = Date()
            val calendarNow = Calendar.getInstance()
            val calendarEvent = Calendar.getInstance().apply { time = eventDateTime }

            // Сравниваем только дату и время
            calendarNow.set(Calendar.SECOND, 0)
            calendarNow.set(Calendar.MILLISECOND, 0)

            val diffMillis = calendarEvent.timeInMillis - calendarNow.timeInMillis
            val diffMinutes = diffMillis / (60 * 1000)

            return when {
                diffMillis < 0 -> {
                    // Просрочено
                    val minutesAgo = -diffMinutes
                    val text = when {
                        minutesAgo < 60 -> "${minutesAgo} мин. назад"
                        minutesAgo < 1440 -> "${minutesAgo / 60} ч. ${minutesAgo % 60} мин. назад"
                        else -> "${minutesAgo / 1440} дн. ${(minutesAgo % 1440) / 60} ч. назад"
                    }
                    Pair("⏰ $text", Color.parseColor("#F44336")) // Красный
                }
                diffMinutes == 0L -> {
                    Pair("🟢 Сейчас", Color.parseColor("#4CAF50")) // Зеленый
                }
                diffMinutes <= 60 -> {
                    Pair("🟡 Через $diffMinutes мин.", Color.parseColor("#FF9800")) // Оранжевый
                }
                diffMinutes <= 1440 -> {
                    val hours = diffMinutes / 60
                    val minutes = diffMinutes % 60
                    Pair("🟢 Через ${hours} ч. ${minutes} мин.", Color.parseColor("#2196F3")) // Синий
                }
                else -> {
                    val days = diffMinutes / 1440
                    val hours = (diffMinutes % 1440) / 60
                    Pair("🔵 Через ${days} дн. ${hours} ч.", Color.parseColor("#9C27B0")) // Фиолетовый
                }
            }
        }

        fun bind(event: Event) {
            currentEvent = event

            textContactName.text = event.contactName
            textContactPhone.text = event.contactPhone

            // Формируем дату и время
            val dateTimeStr = "${dateFormat.format(event.date)} ${event.time}"
            textDateTime.text = dateTimeStr

            // Рассчитываем статус времени
            try {
                // Парсим дату и время события
                val eventCalendar = Calendar.getInstance()
                eventCalendar.time = event.date
                val timeParts = event.time.split(":")
                eventCalendar.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                eventCalendar.set(Calendar.MINUTE, timeParts[1].toInt())
                eventCalendar.set(Calendar.SECOND, 0)
                eventCalendar.set(Calendar.MILLISECOND, 0)

                val (statusText, color) = getTimeStatusText(eventCalendar.time)
                textTimeStatus.text = statusText
                textTimeStatus.setTextColor(color)
                textTimeStatus.setBackgroundColor(Color.parseColor("#20" + Integer.toHexString(color).substring(2)))
                textTimeStatus.visibility = View.VISIBLE
            } catch (e: Exception) {
                textTimeStatus.visibility = View.GONE
            }

            textEventType.text = event.type

            // Отображаем статус контакта
            if (event.status.isNotEmpty()) {
                textStatus.text = event.status
                when {
                    event.status.contains("Новый") -> textStatus.setBackgroundResource(R.drawable.badge_new)
                    event.status.contains("В работе") -> textStatus.setBackgroundResource(R.drawable.badge_progress)
                    event.status.contains("Переговоры") -> textStatus.setBackgroundResource(R.drawable.badge_negotiation)
                    event.status.contains("Конвертирован") -> textStatus.setBackgroundResource(R.drawable.badge_converted)
                    event.status.contains("Потерян") -> textStatus.setBackgroundResource(R.drawable.badge_lost)
                    else -> textStatus.setBackgroundColor(Color.parseColor("#888888"))
                }
                textStatus.visibility = View.VISIBLE
            } else {
                textStatus.visibility = View.GONE
            }

            // Отображаем категорию контакта
            if (event.category.isNotEmpty()) {
                textCategory.text = event.category
                when {
                    event.category.contains("Горячий") -> textCategory.setBackgroundColor(Color.parseColor("#FF4444"))
                    event.category.contains("Теплый") -> textCategory.setBackgroundColor(Color.parseColor("#FFA500"))
                    event.category.contains("Холодный") -> textCategory.setBackgroundColor(Color.parseColor("#4CAF50"))
                    else -> textCategory.setBackgroundColor(Color.parseColor("#888888"))
                }
                textCategory.visibility = View.VISIBLE
            } else {
                textCategory.visibility = View.GONE
            }

            val hasPhone = event.contactPhone.isNotEmpty()
            btnCallContact.visibility = if (hasPhone) View.VISIBLE else View.GONE
            btnSmsContact.visibility = if (hasPhone) View.VISIBLE else View.GONE
            btnWhatsappContact.visibility = if (hasPhone) View.VISIBLE else View.GONE
            btnTelegramContact.visibility = if (hasPhone) View.VISIBLE else View.GONE

            val hasEmail = event.contactEmail.isNotEmpty()
            btnEmailContact.visibility = if (hasEmail) View.VISIBLE else View.GONE

            if (!event.note.isNullOrEmpty()) {
                textNote.text = event.note
                textNote.visibility = View.VISIBLE
            } else {
                textNote.visibility = View.GONE
            }

            btnSync.visibility = if (event.calendarEventId.isNullOrEmpty()) View.VISIBLE else View.GONE
        }
    }
}