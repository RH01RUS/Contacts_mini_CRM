package com.example.contactmanager.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "events",
    foreignKeys = [
        ForeignKey(
            entity = Contact::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["contactId"])]
)
data class Event(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contactId: Long,
    val contactName: String,
    val contactPhone: String,
    val contactEmail: String = "",
    val date: Date,
    val time: String,
    val type: String,
    val note: String? = null,
    val calendarEventId: String? = null,
    // Добавляем поля статуса и категории для отображения
    val status: String = "",  // Будет хранить название статуса
    val category: String = ""  // Будет хранить название категории
)