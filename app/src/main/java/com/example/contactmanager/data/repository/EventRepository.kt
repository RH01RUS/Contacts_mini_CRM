package com.example.contactmanager.data.repository

import android.app.Application
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import com.example.contactmanager.data.database.AppDatabase
import com.example.contactmanager.data.model.Event
import kotlinx.coroutines.flow.Flow
import java.util.*

class EventRepository(application: Application) {

    private val TAG = "EventRepository"
    private val eventDao = AppDatabase.getDatabase(application).eventDao()
    private val context = application.applicationContext

    fun getAllEvents(): Flow<List<Event>> {
        Log.d(TAG, "getAllEvents() вызван")
        return eventDao.getAllEvents()
    }

    fun getEventById(id: Long): Flow<Event?> {
        Log.d(TAG, "getEventById($id) вызван")
        return eventDao.getEventById(id)
    }

    fun getEventsByContact(contactId: Long): Flow<List<Event>> {
        Log.d(TAG, "getEventsByContact($contactId) вызван")
        return eventDao.getEventsByContact(contactId)
    }

    suspend fun insertEvent(event: Event) {
        Log.d(TAG, "insertEvent: ${event.type} для контакта ${event.contactId}")
        try {
            eventDao.insertEvent(event)
            Log.d(TAG, "✅ Событие успешно вставлено в БД")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка вставки события: ${e.message}", e)
            throw e
        }
    }

    suspend fun insertAllEvents(events: List<Event>) {
        Log.d(TAG, "insertAllEvents: ${events.size} событий")
        try {
            eventDao.insertAllEvents(events)
            Log.d(TAG, "✅ Все события успешно вставлены")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка вставки всех событий: ${e.message}", e)
            throw e
        }
    }

    suspend fun updateEvent(event: Event) {
        Log.d(TAG, "updateEvent: id=${event.id}")
        try {
            eventDao.updateEvent(event)
            Log.d(TAG, "✅ Событие успешно обновлено")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка обновления события: ${e.message}", e)
            throw e
        }
    }

    suspend fun deleteEvent(event: Event) {
        Log.d(TAG, "deleteEvent: id=${event.id}")
        try {
            eventDao.deleteEvent(event)
            Log.d(TAG, "✅ Событие успешно удалено")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка удаления события: ${e.message}", e)
            throw e
        }
    }

    suspend fun deleteEventsByContact(contactId: Long) {
        Log.d(TAG, "deleteEventsByContact($contactId)")
        try {
            eventDao.deleteEventsByContact(contactId)
            Log.d(TAG, "✅ События для контакта $contactId удалены")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка удаления событий для контакта $contactId: ${e.message}", e)
            throw e
        }
    }

    suspend fun deleteAllEvents() {
        Log.d(TAG, "deleteAllEvents()")
        try {
            eventDao.deleteAllEvents()
            Log.d(TAG, "✅ Все события удалены")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка удаления всех событий: ${e.message}", e)
            throw e
        }
    }

    suspend fun syncEventToCalendar(event: Event): Intent? {
        Log.d(TAG, "syncEventToCalendar: ${event.id}")

        return try {
            if (event.calendarEventId.isNullOrEmpty()) {
                createCalendarEventIntent(event)
            } else {
                updateCalendarEventIntent(event)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Нет разрешения на запись в календарь", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка синхронизации с календарем: ${e.message}", e)
            throw e
        }
    }

    private fun createCalendarEventIntent(event: Event): Intent {
        Log.d(TAG, "createCalendarEventIntent: ${event.id}")

        val calendar = Calendar.getInstance().apply {
            time = event.date
            val timeParts = event.time.split(":")
            set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
            set(Calendar.MINUTE, timeParts[1].toInt())
            set(Calendar.SECOND, 0)
        }

        // Формируем описание с номером телефона
        val description = buildString {
            append(event.note ?: "")
            if (event.note != null && event.note!!.isNotEmpty()) {
                append("\n\n")
            }
            append("📞 Телефон: ${event.contactPhone}")
            append("\n👤 Контакт: ${event.contactName}")
        }

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, "${event.type} с ${event.contactName}")
            putExtra(CalendarContract.Events.DESCRIPTION, description)
            putExtra(CalendarContract.Events.EVENT_LOCATION, "")
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, calendar.timeInMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, calendar.timeInMillis + 60 * 60 * 1000) // +1 час
            putExtra(CalendarContract.Events.ACCESS_LEVEL, CalendarContract.Events.ACCESS_DEFAULT)
            putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)

            // Добавляем дополнительные данные для контакта
            putExtra(CalendarContract.Events.CUSTOM_APP_PACKAGE, context.packageName)
        }

        return intent
    }

    private fun updateCalendarEventIntent(event: Event): Intent? {
        Log.d(TAG, "updateCalendarEventIntent: ${event.id}")

        if (event.calendarEventId.isNullOrEmpty()) {
            return null
        }

        val calendar = Calendar.getInstance().apply {
            time = event.date
            val timeParts = event.time.split(":")
            set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
            set(Calendar.MINUTE, timeParts[1].toInt())
            set(Calendar.SECOND, 0)
        }

        // Формируем описание с номером телефона
        val description = buildString {
            append(event.note ?: "")
            if (event.note != null && event.note!!.isNotEmpty()) {
                append("\n\n")
            }
            append("📞 Телефон: ${event.contactPhone}")
            append("\n👤 Контакт: ${event.contactName}")
        }

        val uri = Uri.withAppendedPath(CalendarContract.Events.CONTENT_URI, event.calendarEventId)

        val intent = Intent(Intent.ACTION_EDIT).apply {
            data = uri
            putExtra(CalendarContract.Events.TITLE, "${event.type} с ${event.contactName}")
            putExtra(CalendarContract.Events.DESCRIPTION, description)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, calendar.timeInMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, calendar.timeInMillis + 60 * 60 * 1000)
        }

        return intent
    }
}