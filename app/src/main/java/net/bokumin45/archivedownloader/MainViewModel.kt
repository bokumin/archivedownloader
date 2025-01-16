package net.bokumin45.archivedownloader

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.bokumin45.archivedownloader.repository.ArchiveRepository

class MainViewModel(private val repository: ArchiveRepository) : ViewModel() {
    private val _categories = MutableStateFlow<List<ArchiveCategory>>(emptyList())
    val categories: StateFlow<List<ArchiveCategory>> = _categories

    private val _selectedCategoryItems = MutableStateFlow<List<ArchiveItem>>(emptyList())
    val selectedCategoryItems: StateFlow<List<ArchiveItem>> = _selectedCategoryItems

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

                val latestCategory = ArchiveCategory("latest", items)

                val groupedItems = items.groupBy {
                    it.category.substringBefore('/')
                }

                _categories.value = listOf(latestCategory) + groupedItems.map { (category, categoryItems) ->
                    ArchiveCategory(category, categoryItems)
                }.sortedBy { it.name }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching uploads", e)
                _error.value = e.message ?: "Unknown error occurred"
                _categories.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectCategory(category: ArchiveCategory) {
        _selectedCategoryItems.value = category.items
    }
}