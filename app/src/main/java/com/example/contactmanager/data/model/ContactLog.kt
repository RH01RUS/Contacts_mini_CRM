package com.example.contactmanager.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "contact_logs",
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
data class ContactLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contactId: Long,
    val contactName: String,
    val contactPhone: String,
    val date: Date,           // Дата и время контакта
    val type: String,         // Тип: "Звонок", "SMS", "WhatsApp", "Telegram", "Email", "Личная встреча"
    val quality: Int,         // Качество контакта 0-100%
    val note: String? = null, // Заметка
    val createdAt: Date = Date() // Дата создания записи
)
