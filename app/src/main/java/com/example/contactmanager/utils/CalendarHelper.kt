package com.example.contactmanager.utils

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.contactmanager.data.model.Event
import java.text.SimpleDateFormat
import java.util.*

class CalendarHelper {

    private val TAG = "CalendarHelper"

    /**
     * Проверяет наличие обоих разрешений
     */
    fun hasCalendarPermissions(context: Context): Boolean {
        val readGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        val writeGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "Read permission: $readGranted, Write permission: $writeGranted")
        return readGranted && writeGranted
    }

    /**
     * Находит ID основного календаря Google (только если есть READ permission)
     */
    private fun getPrimaryCalendarId(context: Context): Long? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No READ permission, using fallback ID=1")
            return 1L
        }

        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE
        )

        // Сначала ищем Google календарь
        val selection = "${CalendarContract.Calendars.ACCOUNT_TYPE} = ?"
        val selectionArgs = arrayOf("com.google")

        try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIndex = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                    val nameIndex = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)

                    Log.d(TAG, "Found Google calendar: ${cursor.getString(nameIndex)} (ID: ${cursor.getLong(idIndex)})")
                    return cursor.getLong(idIndex)
                }
            }

            // Если Google календарь не найден, берем любой доступный
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIndex = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                    val nameIndex = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)

                    Log.d(TAG, "Found fallback calendar: ${cursor.getString(nameIndex)} (ID: ${cursor.getLong(idIndex)})")
                    return cursor.getLong(idIndex)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading calendars", e)
        }

        return 1L // Возвращаем 1 как запасной вариант
    }

    fun createCalendarEvent(event: Event, context: Context): String? {
        Log.d(TAG, "=== Attempting to create calendar event ===")

        if (!hasCalendarPermissions(context)) {
            Log.e(TAG, "❌ Missing calendar permissions!")
            return null
        }

        val calendarId = getPrimaryCalendarId(context) ?: 1L
        Log.d(TAG, "Using calendar ID: $calendarId")

        val calendar = Calendar.getInstance().apply {
            time = event.date
            val timeParts = event.time.split(":")
            if (timeParts.size == 2) {
                set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                set(Calendar.MINUTE, timeParts[1].toInt())
            }
        }

        val endCalendar = calendar.clone() as Calendar
        endCalendar.add(Calendar.HOUR_OF_DAY, 1)

        val description = buildString {
            append("Контакт: ${event.contactName}\n")
            append("Телефон: ${event.contactPhone}\n")
            append("Тип: ${event.type}\n")
            event.note?.let { append("Заметка: $it") }
        }

        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, calendar.timeInMillis)
            put(CalendarContract.Events.DTEND, endCalendar.timeInMillis)
            put(CalendarContract.Events.TITLE, "${event.type}: ${event.contactName}")
            put(CalendarContract.Events.DESCRIPTION, description)
            put(CalendarContract.Events.EVENT_LOCATION, "")
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }

        return try {
            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            Log.d(TAG, "Insert result URI: $uri")
            uri?.lastPathSegment
        } catch (e: Exception) {
            Log.e(TAG, "Error creating event", e)
            null
        }
    }
}