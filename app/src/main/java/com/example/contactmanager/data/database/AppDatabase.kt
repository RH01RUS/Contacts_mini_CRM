package com.example.contactmanager.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.contactmanager.data.database.dao.ContactDao
import com.example.contactmanager.data.database.dao.ContactLogDao
import com.example.contactmanager.data.database.dao.EventDao
import com.example.contactmanager.data.model.Contact
import com.example.contactmanager.data.model.ContactLog
import com.example.contactmanager.data.model.Event

@Database(
    entities = [Contact::class, Event::class, ContactLog::class],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun contactDao(): ContactDao
    abstract fun eventDao(): EventDao
    abstract fun contactLogDao(): ContactLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Миграция с версии 1 на 2 - добавление поля category в contacts
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE contacts ADD COLUMN category TEXT NOT NULL DEFAULT 'COLD'")
            }
        }

        // Миграция с версии 2 на 3 - добавление поля contactEmail в events
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE events ADD COLUMN contactEmail TEXT NOT NULL DEFAULT ''")
            }
        }

        // Миграция с версии 3 на 4 - добавление полей status и category в events
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE events ADD COLUMN status TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE events ADD COLUMN category TEXT NOT NULL DEFAULT ''")
            }
        }

        // Миграция с версии 4 на 5 - пустая (на будущее)
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Пустая миграция
            }
        }

        // Миграция с версии 5 на 6 - создание таблицы contact_logs
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `contact_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `contactId` INTEGER NOT NULL,
                        `contactName` TEXT NOT NULL,
                        `contactPhone` TEXT NOT NULL,
                        `date` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `quality` INTEGER NOT NULL,
                        `note` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`contactId`) REFERENCES `contacts`(`id`) ON UPDATE CASCADE ON DELETE CASCADE
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_contact_logs_contactId` ON `contact_logs` (`contactId`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}