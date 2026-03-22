package com.example.contactmanager.data.database.dao

import androidx.room.*
import com.example.contactmanager.data.model.ContactLog
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactLogDao {

    @Query("SELECT * FROM contact_logs WHERE contactId = :contactId ORDER BY date DESC")
    fun getLogsByContact(contactId: Long): Flow<List<ContactLog>>

    @Query("SELECT * FROM contact_logs ORDER BY date DESC")
    fun getAllLogs(): Flow<List<ContactLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ContactLog)

    @Update
    suspend fun updateLog(log: ContactLog)

    @Delete
    suspend fun deleteLog(log: ContactLog)

    @Query("DELETE FROM contact_logs WHERE contactId = :contactId")
    suspend fun deleteLogsByContact(contactId: Long)

    @Query("DELETE FROM contact_logs")
    suspend fun deleteAllLogs()
}