package com.example.contactmanager.data.database.dao

import androidx.room.*
import com.example.contactmanager.data.model.Event
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Query("SELECT * FROM events ORDER BY date ASC")
    fun getAllEvents(): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE id = :id")
    fun getEventById(id: Long): Flow<Event?>

    @Query("SELECT * FROM events WHERE contactId = :contactId ORDER BY date DESC")
    fun getEventsByContact(contactId: Long): Flow<List<Event>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: Event): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllEvents(events: List<Event>)

    @Update
    suspend fun updateEvent(event: Event)

    @Delete
    suspend fun deleteEvent(event: Event)

    @Query("DELETE FROM events WHERE contactId = :contactId")
    suspend fun deleteEventsByContact(contactId: Long)

    @Query("DELETE FROM events")
    suspend fun deleteAllEvents()
}