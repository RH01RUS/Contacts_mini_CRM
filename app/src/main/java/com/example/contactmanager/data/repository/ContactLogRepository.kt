package com.example.contactmanager.data.repository

import android.app.Application
import android.util.Log
import com.example.contactmanager.data.database.AppDatabase
import com.example.contactmanager.data.model.ContactLog
import kotlinx.coroutines.flow.Flow

class ContactLogRepository(application: Application) {

    private val TAG = "ContactLogRepository"
    private val contactLogDao = AppDatabase.getDatabase(application).contactLogDao()

    fun getLogsByContact(contactId: Long): Flow<List<ContactLog>> {
        Log.d(TAG, "getLogsByContact($contactId)")
        return contactLogDao.getLogsByContact(contactId)
    }

    fun getAllLogs(): Flow<List<ContactLog>> {
        Log.d(TAG, "getAllLogs()")
        return contactLogDao.getAllLogs()
    }

    suspend fun insertLog(log: ContactLog) {
        Log.d(TAG, "insertLog: для контакта ${log.contactName}, тип=${log.type}, качество=${log.quality}%")
        try {
            val id = contactLogDao.insertLog(log)
            Log.d(TAG, "Лог успешно вставлен с id=$id")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка вставки лога: ${e.message}", e)
            throw e
        }
    }

    suspend fun updateLog(log: ContactLog) {
        Log.d(TAG, "updateLog: id=${log.id}")
        try {
            contactLogDao.updateLog(log)
            Log.d(TAG, "Лог успешно обновлен")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка обновления лога: ${e.message}", e)
            throw e
        }
    }

    suspend fun deleteLog(log: ContactLog) {
        Log.d(TAG, "deleteLog: id=${log.id}")
        try {
            contactLogDao.deleteLog(log)
            Log.d(TAG, "Лог успешно удален")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка удаления лога: ${e.message}", e)
            throw e
        }
    }

    suspend fun deleteLogsByContact(contactId: Long) {
        Log.d(TAG, "deleteLogsByContact($contactId)")
        try {
            contactLogDao.deleteLogsByContact(contactId)
            Log.d(TAG, "Логи для контакта $contactId успешно удалены")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка удаления логов для контакта $contactId: ${e.message}", e)
            throw e
        }
    }

    suspend fun deleteAllLogs() {
        Log.d(TAG, "deleteAllLogs()")
        try {
            contactLogDao.deleteAllLogs()
            Log.d(TAG, "Все логи успешно удалены")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка удаления всех логов: ${e.message}", e)
            throw e
        }
    }
}