package net.bokumin45.archivedownloader

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
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

    private val _searchResults = MutableStateFlow<List<ArchiveItem>>(emptyList())
    val searchResults: StateFlow<List<ArchiveItem>> = _searchResults

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

                // メインカテゴリーでグループ化
                val categoryGroups = items.groupBy { it.mainCategory }

                // 各メインカテゴリーのサブカテゴリーを作成
                val mainCategories = categoryGroups.map { (categoryName, categoryItems) ->
                    // サブカテゴリーでグループ化
                    val subCategoryGroups = categoryItems
                        .filter { it.subCategory.isNotEmpty() }
                        .groupBy { it.subCategory }

                    // サブカテゴリーを作成
                    val subCategories = subCategoryGroups.map { (subName, subItems) ->
                        ArchiveCategory(
                            name = "$categoryName/$subName",
                            items = subItems,
                            parent = categoryName
                        )
                    }.sortedBy { it.name }

                    // メインカテゴリーを作成（サブカテゴリーがないアイテムも含める）
                    ArchiveCategory(
                        name = categoryName,
                        items = categoryItems.filter { it.subCategory.isEmpty() },
                        subCategories = subCategories,
                        parent = "latest"
                    )
                }.sortedBy { it.name }

                // Latest カテゴリーを作成
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
        _selectedCategoryItems.value = category.items
    }
}