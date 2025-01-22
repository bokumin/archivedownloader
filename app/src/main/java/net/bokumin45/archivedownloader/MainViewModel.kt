package net.bokumin45.archivedownloader

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.bokumin45.archivedownloader.repository.ArchiveRepository

class MainViewModel(
    private val repository: ArchiveRepository,
    private val favoriteManager: FavoriteManager
) : ViewModel() {
    private val _categories = MutableStateFlow<List<ArchiveCategory>>(emptyList())
    val categories: StateFlow<List<ArchiveCategory>> = _categories

    private val _selectedCategoryItems = MutableStateFlow<List<ArchiveItem>>(emptyList())
    val selectedCategoryItems: StateFlow<List<ArchiveItem>> = _selectedCategoryItems

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _searchResults = MutableStateFlow<List<ArchiveItem>>(emptyList())
    val searchResults: StateFlow<List<ArchiveItem>> = _searchResults

    private val _favoriteItems = MutableStateFlow<List<ArchiveItem>>(emptyList())
    val favoriteItems: StateFlow<List<ArchiveItem>> = _favoriteItems

    private var lastSelectedCategory: ArchiveCategory? = null
    fun getLastSelectedCategory(): ArchiveCategory? = lastSelectedCategory

    fun toggleFavorite(item: ArchiveItem) {
        if (favoriteManager.isFavorite(item.identifier)) {
            favoriteManager.removeFavorite(item)
        } else {
            favoriteManager.addFavorite(item)
        }
        if (_favoriteItems.value.isNotEmpty()) {
            loadFavorites()
        }
    }

    fun loadFavorites() {
        _favoriteItems.value = favoriteManager.getFavorites().toList()
    }

    fun isFavorite(identifier: String): Boolean {
        return favoriteManager.isFavorite(identifier)
    }

    private var currentSearchJob: Job? = null
    private var currentPage = 1
    private var isLastPage = false
    private var currentQuery = ""

    fun search(query: String, resetPage: Boolean = true) {
        if (resetPage) {
            currentPage = 1
            isLastPage = false
            _searchResults.value = emptyList()
        }

        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        currentQuery = query
        currentSearchJob?.cancel()
        currentSearchJob = viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = repository.searchItems(query, currentPage)

                val newItems = response.response.docs.map { it.toArchiveItem() }
                if (resetPage) {
                    _searchResults.value = newItems
                } else {
                    _searchResults.value = _searchResults.value + newItems
                }

                isLastPage = newItems.isEmpty() ||
                        response.response.start + response.response.docs.size >= response.response.numFound

            } catch (e: Exception) {
                _error.value = "Search failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadNextSearchPage() {
        if (!isLastPage && !_isLoading.value) {
            currentPage++
            search(currentQuery, false)
        }
    }

    fun fetchLatestUploads() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val items = repository.getLatestUploads()

                val categoryGroups = items.groupBy { it.mainCategory }

                val mainCategories = categoryGroups.map { (categoryName, categoryItems) ->
                    val subCategoryGroups = categoryItems
                        .filter { it.subCategory.isNotEmpty() }
                        .groupBy { it.subCategory }

                    val subCategories = subCategoryGroups.map { (subName, subItems) ->
                        ArchiveCategory(
                            name = "$categoryName/$subName",
                            items = subItems,
                            parent = categoryName
                        )
                    }.sortedBy { it.name }

                    ArchiveCategory(
                        name = categoryName,
                        items = categoryItems.filter { it.subCategory.isEmpty() },
                        subCategories = subCategories,
                        parent = "latest"
                    )
                }.sortedBy { it.name }

                val latestCategory = ArchiveCategory(
                    name = "latest",
                    items = items,
                    subCategories = mainCategories,
                    parent = null
                )

                _categories.value = listOf(latestCategory)
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
        lastSelectedCategory = category
        _selectedCategoryItems.value = category.items
    }
}