package com.example.contactmanager

import android.app.Application
import android.util.Log

class ContactManagerApplication : Application() {

    companion object {
        private const val TAG = "ContactManagerApp"
        lateinit var instance: ContactManagerApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "Application создан")

        // Перехват необработанных исключений
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Необработанное исключение в потоке ${thread.name}", throwable)
            throwable.printStackTrace()
        }
    }
}