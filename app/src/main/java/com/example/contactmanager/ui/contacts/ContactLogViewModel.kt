package com.example.contactmanager.ui.contacts

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.contactmanager.data.model.ContactLog
import com.example.contactmanager.data.repository.ContactLogRepository
import kotlinx.coroutines.launch

class ContactLogViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "ContactLogViewModel"
    private val repository = ContactLogRepository(application)

    private val _logs = MutableLiveData<List<ContactLog>>()
    val logs: LiveData<List<ContactLog>> = _logs

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _operationResult = MutableLiveData<Boolean>()
    val operationResult: LiveData<Boolean> = _operationResult

    private var allLogsCache: List<ContactLog> = emptyList()

    fun loadLogsForContact(contactId: Long) {
        Log.d(TAG, "loadLogsForContact($contactId)")
        viewModelScope.launch {
            _isLoading.postValue(true)
            try {
                repository.getLogsByContact(contactId).collect { logList ->
                    Log.d(TAG, "loadLogsForContact: получено ${logList.size} записей")
                    _logs.postValue(logList)
                    allLogsCache = logList
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка загрузки логов: ${e.message}", e)
                _logs.postValue(emptyList())
                allLogsCache = emptyList()
                _operationResult.postValue(false)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun loadAllLogs() {
        Log.d(TAG, "loadAllLogs()")
        viewModelScope.launch {
            _isLoading.postValue(true)
            try {
                repository.getAllLogs().collect { logList ->
                    Log.d(TAG, "loadAllLogs: получено ${logList.size} записей")
                    _logs.postValue(logList)
                    allLogsCache = logList
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка загрузки всех логов: ${e.message}", e)
                _logs.postValue(emptyList())
                allLogsCache = emptyList()
                _operationResult.postValue(false)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun filterLogs(query: String) {
        Log.d(TAG, "filterLogs: $query")
        viewModelScope.launch {
            _isLoading.postValue(true)
            try {
                if (query.isEmpty()) {
                    _logs.postValue(allLogsCache)
                    Log.d(TAG, "filterLogs: показано ${allLogsCache.size} записей")
                } else {
                    val filtered = allLogsCache.filter { log ->
                        log.contactName.contains(query, ignoreCase = true) ||
                                log.contactPhone.contains(query, ignoreCase = true) ||
                                log.type.contains(query, ignoreCase = true) ||
                                (log.note?.contains(query, ignoreCase = true) ?: false)
                    }
                    _logs.postValue(filtered)
                    Log.d(TAG, "filterLogs: найдено ${filtered.size} записей по запросу '$query'")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка фильтрации: ${e.message}", e)
                _logs.postValue(emptyList())
                _operationResult.postValue(false)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun saveLog(log: ContactLog) {
        Log.d(TAG, "saveLog: ${log.type} для ${log.contactName}, качество=${log.quality}%")
        viewModelScope.launch {
            _isLoading.postValue(true)
            try {
                repository.insertLog(log)
                Log.d(TAG, "Лог успешно сохранен в репозитории")

                // Обновляем кэш и списки
                loadAllLogs()
                loadLogsForContact(log.contactId)

                _operationResult.postValue(true)
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка сохранения лога: ${e.message}", e)
                _operationResult.postValue(false)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun deleteLog(log: ContactLog) {
        Log.d(TAG, "deleteLog: id=${log.id}")
        viewModelScope.launch {
            _isLoading.postValue(true)
            try {
                repository.deleteLog(log)
                Log.d(TAG, "Лог успешно удален")
                loadAllLogs()
                loadLogsForContact(log.contactId)
                _operationResult.postValue(true)
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка удаления лога: ${e.message}", e)
                _operationResult.postValue(false)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}