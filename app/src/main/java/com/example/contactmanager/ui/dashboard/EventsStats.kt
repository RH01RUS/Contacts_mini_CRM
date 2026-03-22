package com.example.contactmanager.ui.dashboard

data class EventsStats(
    val totalEvents: Int,
    val upcomingEvents: Int,
    val pastEvents: Int,
    val eventsByType: Map<String, Int>,
    val eventsToday: Int,
    val eventsThisWeek: Int,
    val eventsThisMonth: Int
)