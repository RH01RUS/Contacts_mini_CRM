package com.example.contactmanager.ui.dashboard

data class ContactsStats(
    val totalContacts: Int,
    val newLeads: Int,
    val inProgressLeads: Int,
    val negotiationLeads: Int,
    val convertedLeads: Int,
    val lostLeads: Int,
    val conversionRate: Double
)