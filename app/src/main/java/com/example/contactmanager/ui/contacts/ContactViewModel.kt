package com.example.contactmanager.ui.contacts

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.contactmanager.data.model.Contact
import com.example.contactmanager.data.model.LeadCategory
import com.example.contactmanager.data.model.LeadStatus
import com.example.contactmanager.data.repository.ContactRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ContactViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "ContactViewModel"
    private val repository = ContactRepository(application)

    private val _allContacts = MutableLiveData<List<Contact>>()
    val allContacts: LiveData<List<Contact>> = _allContacts

    private val _filteredContacts = MutableLiveData<List<Contact>>()
    val filteredContacts: LiveData<List<Contact>> = _filteredContacts

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val searchQuery = MutableStateFlow("")
    private val selectedStatuses = MutableStateFlow<Set<LeadStatus>>(emptySet())
    private val selectedCategories = MutableStateFlow<Set<LeadCategory>>(emptySet())

    init {
        Log.d(TAG, "ContactViewModel инициализирован")
        setupSearchAndFilter()
        loadAllContacts()
    }

    fun loadAllContacts() {
        viewModelScope.launch {
            _isLoading.postValue(true)
            try {
                repository.getAllContacts().collect { contacts ->
                    Log.d(TAG, "Загружено контактов: ${contacts.size}")
                    _allContacts.postValue(contacts)
                    applyFilters()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка загрузки контактов: ${e.message}", e)
                _allContacts.postValue(emptyList())
                _filteredContacts.postValue(emptyList())
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    private fun setupSearchAndFilter() {
        combine(searchQuery, selectedStatuses, selectedCategories) { query, statuses, categories ->
            Triple(query, statuses, categories)
        }
            .debounce(300)
            .distinctUntilChanged()
            .onEach { (query, statuses, categories) ->
                applyFilters(query, statuses, categories)
            }
            .launchIn(viewModelScope)
    }

    private fun applyFilters(
        query: String = searchQuery.value,
        statuses: Set<LeadStatus> = selectedStatuses.value,
        categories: Set<LeadCategory> = selectedCategories.value
    ) {
        val allContacts = _allContacts.value ?: emptyList()

        val filtered = allContacts.filter { contact ->
            var matches = true

            if (query.isNotEmpty() && query.length >= 2) {
                matches = contact.name.contains(query, ignoreCase = true) ||
                        contact.phone.contains(query, ignoreCase = true) ||
                        (contact.email?.contains(query, ignoreCase = true) ?: false) ||
                        (contact.company?.contains(query, ignoreCase = true) ?: false)
            }

            if (matches && statuses.isNotEmpty()) {
                matches = statuses.contains(contact.status)
            }

            if (matches && categories.isNotEmpty()) {
                matches = categories.contains(contact.category)
            }

            matches
        }

        _filteredContacts.postValue(filtered)
        Log.d(TAG, "Применены фильтры: найдено ${filtered.size} контактов")
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun toggleStatusFilter(status: LeadStatus, isSelected: Boolean) {
        val currentStatuses = selectedStatuses.value.toMutableSet()
        if (isSelected) {
            currentStatuses.add(status)
        } else {
            currentStatuses.remove(status)
        }
        selectedStatuses.value = currentStatuses
    }

    fun toggleCategoryFilter(category: LeadCategory, isSelected: Boolean) {
        val currentCategories = selectedCategories.value.toMutableSet()
        if (isSelected) {
            currentCategories.add(category)
        } else {
            currentCategories.remove(category)
        }
        selectedCategories.value = currentCategories
    }

    fun clearFilters() {
        selectedStatuses.value = emptySet()
        selectedCategories.value = emptySet()
        searchQuery.value = ""
    }

    fun clearCategoryFilters() {
        selectedCategories.value = emptySet()
    }

    fun clearStatusFilters() {
        selectedStatuses.value = emptySet()
    }

    fun saveContact(contact: Contact) {
        viewModelScope.launch {
            _isLoading.postValue(true)
            try {
                if (contact.id == 0L) {
                    Log.d(TAG, "Вставка нового контакта: ${contact.name}")
                    repository.insertContact(contact)
                } else {
                    Log.d(TAG, "Обновление контакта: id=${contact.id}")
                    repository.updateContact(contact)
                }
                loadAllContacts()
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка сохранения: ${e.message}", e)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch {
            _isLoading.postValue(true)
            try {
                Log.d(TAG, "Удаление контакта: id=${contact.id}")
                repository.deleteContact(contact)
                loadAllContacts()
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка удаления: ${e.message}", e)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}