package com.example.archivedownloader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.archivedownloader.repository.ArchiveRepository

class MainViewModelFactory(
    private val repository: ArchiveRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}