package com.example.contactmanager.ui.events

import android.app.Application
import android.content.Intent  // Добавлен импорт
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.contactmanager.data.model.Event
import com.example.contactmanager.data.repository.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class EventViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "EventViewModel"
    private val repository = EventRepository(application)

    private val _allEvents = MutableLiveData<List<Event>>()
    val allEvents: LiveData<List<Event>> = _allEvents

    private val _filteredEvents = MutableLiveData<List<Event>>()
    val filteredEvents: LiveData<List<Event>> = _filteredEvents

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _contactEvents = MutableLiveData<List<Event>>()
    val contactEvents: LiveData<List<Event>> = _contactEvents

    private val _syncIntent = MutableLiveData<Intent?>()
    val syncIntent: LiveData<Intent?> = _syncIntent

    // Поля для поиска и фильтрации
    private val searchQuery = MutableStateFlow("")
    private val selectedEventTypes = MutableStateFlow<Set<String>>(emptySet())

    init {
        Log.d(TAG, "EventViewModel инициализирован")
        setupSearchAndFilter()
        loadAllEvents()
    }

    fun loadAllEvents() {
        Log.d(TAG, "loadAllEvents() вызван")
        viewModelScope.launch {
            _isLoading.postValue(true)
            try {
                repository.getAllEvents().collect { eventList ->
                    Log.d(TAG, "loadAllEvents: получено ${eventList.size} событий")
                    _allEvents.postValue(eventList)
                    applyFilters()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка загрузки событий: ${e.message}", e)
                _allEvents.postValue(emptyList())
                _filteredEvents.postValue(emptyList())
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    private fun setupSearchAndFilter() {
        combine(searchQuery, selectedEventTypes) { query, types ->
            Pair(query, types)
        }
            .debounce(300)
            .distinctUntilChanged()
            .onEach { (query, types) ->
                applyFilters(query, types)
            }
            .launchIn(viewModelScope)
    }

    private fun applyFilters(query: String = searchQuery.value, types: Set<String> = selectedEventTypes.value) {
        val allEvents = _allEvents.value ?: emptyList()

        val filtered = allEvents.filter { event ->
            var matches = true

            // Поиск по имени контакта или телефону
            if (query.isNotEmpty() && query.length >= 2) {
                matches = event.contactName.contains(query, ignoreCase = true) ||
                        event.contactPhone.contains(query, ignoreCase = true)
            }

            // Фильтр по типу мероприятия
            if (matches && types.isNotEmpty()) {
                matches = types.contains(event.type)
            }

            matches
        }

        _filteredEvents.postValue(filtered)
        Log.d(TAG, "Применены фильтры: найдено ${filtered.size} событий")
    }

    fun onSearchQueryChanged(query: String) {
        Log.d(TAG, "onSearchQueryChanged: $query")
        searchQuery.value = query
    }

    fun toggleEventTypeFilter(type: String, isSelected: Boolean) {
        Log.d(TAG, "toggleEventTypeFilter: $type, isSelected=$isSelected")
        val currentTypes = selectedEventTypes.value.toMutableSet()
        if (isSelected) {
            currentTypes.add(type)
        } else {
            currentTypes.remove(type)
        }
        selectedEventTypes.value = currentTypes
    }

    fun clearFilters() {
        selectedEventTypes.value = emptySet()
        searchQuery.value = ""
    }

    fun getEventsByContact(contactId: Long): LiveData<List<Event>> {
        Log.d(TAG, "getEventsByContact($contactId) вызван")
        viewModelScope.launch {
            _isLoading.postValue(true)
            try {
                repository.getEventsByContact(contactId).collect { eventList ->
                    _contactEvents.postValue(eventList)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка загрузки событий для контакта $contactId: ${e.message}", e)
                _contactEvents.postValue(emptyList())
            } finally {
                _isLoading.postValue(false)
            }
        }
        return contactEvents
    }

    fun saveEvent(event: Event) {
        viewModelScope.launch {
            _isLoading.postValue(true)
            try {
                if (event.id == 0L) {
                    repository.insertEvent(event)
                } else {
                    repository.updateEvent(event)
                }
                loadAllEvents()
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка сохранения события: ${e.message}", e)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun deleteEvent(event: Event) {
        viewModelScope.launch {
            _isLoading.postValue(true)
            try {
                repository.deleteEvent(event)
                loadAllEvents()
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка удаления события: ${e.message}", e)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun syncEventToCalendar(event: Event) {
        viewModelScope.launch {
            _isLoading.postValue(true)
            try {
                val intent = repository.syncEventToCalendar(event)
                _syncIntent.postValue(intent)
                loadAllEvents()
            } catch (e: SecurityException) {
                Log.e(TAG, "Ошибка безопасности при синхронизации", e)
                _syncIntent.postValue(null)
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка синхронизации: ${e.message}", e)
                _syncIntent.postValue(null)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun clearSyncIntent() {
        _syncIntent.postValue(null)
    }
}