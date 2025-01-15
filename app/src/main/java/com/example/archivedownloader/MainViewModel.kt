package com.example.archivedownloader

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.archivedownloader.repository.ArchiveRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(private val repository: ArchiveRepository) : ViewModel() {
    private val _items = MutableStateFlow<List<ArchiveItem>>(emptyList())
    val items: StateFlow<List<ArchiveItem>> = _items

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun fetchLatestUploads() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val items = repository.getLatestUploads()
                Log.d("MainViewModel", "Fetched ${items.size} items")
                _items.value = items
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching uploads", e)
                _error.value = e.message ?: "Unknown error occurred"
                _items.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}