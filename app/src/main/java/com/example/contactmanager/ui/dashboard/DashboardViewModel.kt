package com.example.contactmanager.ui.dashboard

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.contactmanager.data.model.Contact
import com.example.contactmanager.data.model.Event
import com.example.contactmanager.data.model.LeadStatus
import com.example.contactmanager.data.repository.ContactRepository
import com.example.contactmanager.data.repository.EventRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "DashboardViewModel"
    private val contactRepository = ContactRepository(application)
    private val eventRepository = EventRepository(application)

    private val _contactsStats = MutableLiveData<ContactsStats>()
    val contactsStats: LiveData<ContactsStats> = _contactsStats

    private val _eventsStats = MutableLiveData<EventsStats>()
    val eventsStats: LiveData<EventsStats> = _eventsStats

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        Log.d(TAG, "DashboardViewModel инициализирован")
        loadStats()
    }

    fun loadStats() {
        Log.d(TAG, "loadStats() вызван")
        viewModelScope.launch {
            _isLoading.postValue(true)
            _error.postValue(null)
            try {
                // Загружаем все данные одновременно
                loadContactsStats()
                loadEventsStats()
                Log.d(TAG, "Все статистики загружены")
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке статистики: ${e.message}", e)
                _error.postValue("Ошибка загрузки данных: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun refreshStats() {
        Log.d(TAG, "refreshStats() вызван")
        loadStats()
    }

    private suspend fun loadContactsStats() {
        Log.d(TAG, "loadContactsStats: начало загрузки")

        try {
            val contacts = contactRepository.getAllContacts().first()
            Log.d(TAG, "loadContactsStats: получено ${contacts.size} контактов")

            val newLeads = contacts.count { it.status == LeadStatus.NEW }
            val inProgress = contacts.count { it.status == LeadStatus.IN_PROGRESS }
            val negotiation = contacts.count { it.status == LeadStatus.NEGOTIATION }
            val converted = contacts.count { it.status == LeadStatus.CONVERTED }
            val lost = contacts.count { it.status == LeadStatus.LOST }

            Log.d(TAG, "Статистика контактов: new=$newLeads, inProgress=$inProgress, negotiation=$negotiation, converted=$converted, lost=$lost")

            val conversionRate = if (contacts.isNotEmpty()) {
                (converted.toDouble() / contacts.size) * 100
            } else 0.0

            val stats = ContactsStats(
                totalContacts = contacts.size,
                newLeads = newLeads,
                inProgressLeads = inProgress,
                negotiationLeads = negotiation,
                convertedLeads = converted,
                lostLeads = lost,
                conversionRate = conversionRate
            )

            Log.d(TAG, "loadContactsStats: отправка статистики, total=${stats.totalContacts}")
            _contactsStats.postValue(stats)

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка в loadContactsStats: ${e.message}", e)
            _error.postValue("Ошибка загрузки контактов: ${e.message}")
            _contactsStats.postValue(ContactsStats(
                totalContacts = 0,
                newLeads = 0,
                inProgressLeads = 0,
                negotiationLeads = 0,
                convertedLeads = 0,
                lostLeads = 0,
                conversionRate = 0.0
            ))
        }
    }

    private suspend fun loadEventsStats() {
        Log.d(TAG, "loadEventsStats: начало загрузки")

        try {
            val allEvents = eventRepository.getAllEvents().first()
            Log.d(TAG, "loadEventsStats: получено ${allEvents.size} событий")

            val now = Date()

            val upcomingEvents = allEvents.count { it.date.after(now) }
            val pastEvents = allEvents.count { it.date.before(now) }

            val eventsByType = allEvents.groupBy { it.type }
                .mapValues { it.value.size }

            // События за сегодня
            val eventsToday = allEvents.count {
                val cal1 = Calendar.getInstance().apply { time = it.date }
                val cal2 = Calendar.getInstance().apply { time = now }
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                        cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
            }

            // События за неделю
            val weekAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }.time
            val eventsThisWeek = allEvents.count { it.date.after(weekAgo) }

            // События за месяц
            val monthAgo = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }.time
            val eventsThisMonth = allEvents.count { it.date.after(monthAgo) }

            val stats = EventsStats(
                totalEvents = allEvents.size,
                upcomingEvents = upcomingEvents,
                pastEvents = pastEvents,
                eventsByType = eventsByType,
                eventsToday = eventsToday,
                eventsThisWeek = eventsThisWeek,
                eventsThisMonth = eventsThisMonth
            )

            Log.d(TAG, "loadEventsStats: отправка статистики, total=${stats.totalEvents}")
            _eventsStats.postValue(stats)

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка в loadEventsStats: ${e.message}", e)
            _error.postValue("Ошибка загрузки событий: ${e.message}")
            _eventsStats.postValue(EventsStats(
                totalEvents = 0,
                upcomingEvents = 0,
                pastEvents = 0,
                eventsByType = emptyMap(),
                eventsToday = 0,
                eventsThisWeek = 0,
                eventsThisMonth = 0
            ))
        }
    }
}