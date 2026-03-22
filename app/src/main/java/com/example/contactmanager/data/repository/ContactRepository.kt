package com.example.contactmanager.data.repository

import android.app.Application
import android.util.Log
import com.example.contactmanager.data.database.AppDatabase
import com.example.contactmanager.data.model.Contact
import com.example.contactmanager.data.model.LeadStatus
import kotlinx.coroutines.flow.Flow

class ContactRepository(application: Application) {

    private val TAG = "ContactRepository"
    private val contactDao = AppDatabase.getDatabase(application).contactDao()

    fun getAllContacts(): Flow<List<Contact>> {
        Log.d(TAG, "getAllContacts() вызван")
        return contactDao.getAllContacts()
    }

    fun getContactById(id: Long): Flow<Contact?> {
        Log.d(TAG, "getContactById($id) вызван")
        return contactDao.getContactById(id)
    }

    suspend fun insertContact(contact: Contact) {
        Log.d(TAG, "insertContact: ${contact.name}")
        contactDao.insertContact(contact)
    }

    suspend fun insertAllContacts(contacts: List<Contact>) {
        Log.d(TAG, "insertAllContacts: ${contacts.size} контактов")
        contactDao.insertAllContacts(contacts)
    }

    suspend fun updateContact(contact: Contact) {
        Log.d(TAG, "updateContact: id=${contact.id}")
        contactDao.updateContact(contact)
    }

    suspend fun deleteContact(contact: Contact) {
        Log.d(TAG, "deleteContact: id=${contact.id}")
        contactDao.deleteContact(contact)
    }

    fun searchContacts(query: String): Flow<List<Contact>> {
        Log.d(TAG, "searchContacts: $query")
        return contactDao.searchContacts("%$query%")
    }

    suspend fun deleteAllContacts() {
        Log.d(TAG, "deleteAllContacts()")
        contactDao.deleteAllContacts()
    }
}