package com.example.contactmanager.data.database

import androidx.room.TypeConverter
import com.example.contactmanager.data.model.LeadCategory
import com.example.contactmanager.data.model.LeadStatus
import java.util.Date

class Converters {

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromLeadStatus(status: LeadStatus): String {
        return status.name
    }

    @TypeConverter
    fun toLeadStatus(name: String): LeadStatus {
        return LeadStatus.valueOf(name)
    }

    @TypeConverter
    fun fromLeadCategory(category: LeadCategory): String {
        return category.name
    }

    @TypeConverter
    fun toLeadCategory(name: String): LeadCategory {
        return LeadCategory.valueOf(name)
    }
}