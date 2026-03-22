package com.example.contactmanager.utils

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactPickerHelper(private val context: Context) {

    data class PhoneContact(
        val name: String,
        val phoneNumber: String,
        val normalizedNumber: String
    )

    private val contentResolver: ContentResolver = context.contentResolver
    private val uri: Uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
    private val projection = arrayOf(
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER,
        ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER
    )

    private var cachedContacts: List<PhoneContact>? = null

    /**
     * Поиск контактов с кэшированием для оптимизации
     */
    suspend fun searchContacts(query: String): List<PhoneContact> = withContext(Dispatchers.IO) {
        if (query.length < 2) return@withContext emptyList()

        // Загружаем все контакты в кэш при первом запросе
        if (cachedContacts == null) {
            cachedContacts = loadAllContacts()
        }

        val searchQuery = query.lowercase().trim()

        // Фильтруем кэшированные контакты
        cachedContacts?.filter { contact ->
            contact.name.lowercase().contains(searchQuery) ||
                    contact.phoneNumber.replace(Regex("[\\s\\-()+]"), "").contains(searchQuery.replace(Regex("[\\s\\-()+]"), "")) ||
                    (contact.normalizedNumber.isNotEmpty() && contact.normalizedNumber.contains(searchQuery))
        } ?: emptyList()
    }

    /**
     * Загрузка всех контактов (выполняется один раз)
     */
    private fun loadAllContacts(): List<PhoneContact> {
        val contacts = mutableListOf<PhoneContact>()

        try {
            val cursor: Cursor? = contentResolver.query(
                uri,
                projection,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )

            cursor?.use { c ->
                val nameIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val normalizedIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER)

                while (c.moveToNext()) {
                    val name = if (nameIndex >= 0) c.getString(nameIndex) else null ?: continue
                    val phoneNumber = if (numberIndex >= 0) c.getString(numberIndex) else null ?: continue
                    val normalizedNumber = if (normalizedIndex >= 0) c.getString(normalizedIndex) else ""

                    contacts.add(PhoneContact(name, phoneNumber, normalizedNumber ?: ""))
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        return contacts
    }

    /**
     * Очистка кэша (можно вызвать при обновлении контактов)
     */
    fun clearCache() {
        cachedContacts = null
    }

    /**
     * Принудительное обновление кэша контактов (синхронное)
     */
    fun refreshContactsSync() {
        clearCache()
        loadAllContacts()
    }

    /**
     * Преобразование PhoneContact в Contact приложения
     */
    fun toAppContact(phoneContact: PhoneContact): com.example.contactmanager.data.model.Contact {
        return com.example.contactmanager.data.model.Contact(
            name = phoneContact.name,
            phone = phoneContact.phoneNumber,
            email = "",
            company = null,
            position = null,
            note = null,
            status = com.example.contactmanager.data.model.LeadStatus.NEW,
            category = com.example.contactmanager.data.model.LeadCategory.COLD  // Добавляем категорию по умолчанию
        )
    }
}