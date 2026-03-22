package com.example.contactmanager.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import kotlinx.coroutines.flow.Flow

fun <T> Flow<T>.asLiveData(): LiveData<T> = liveData {
    collect { value ->
        emit(value)
    }
}