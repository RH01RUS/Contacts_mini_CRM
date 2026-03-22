package com.example.contactmanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String,
    val email: String = "",
    val company: String? = null,
    val position: String? = null,
    val note: String? = null,
    val status: LeadStatus = LeadStatus.NEW,
    val category: LeadCategory = LeadCategory.COLD  // Новое поле с значением по умолчанию
)