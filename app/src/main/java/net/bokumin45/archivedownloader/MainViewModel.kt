package net.bokumin45.archivedownloader

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
    private val _displayState = MutableStateFlow(DisplayState.HOME)
    val displayState: StateFlow<DisplayState> = _displayState

    private val _currentItems = MutableStateFlow<List<Any>>(emptyList())
    val currentItems: StateFlow<List<Any>> = _currentItems

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

    private val _hotPeriod = MutableStateFlow<HotPeriod>(HotPeriod.WEEK)
    val hotPeriod: StateFlow<HotPeriod> = _hotPeriod

    private var lastSelectedCategory: ArchiveCategory? = null
    private var currentSearchJob: Job? = null
    private var currentPage = 1
    private var currentCategoryPage = 1
    private var isCategoryLastPage = false
    private var currentCategoryName: String? = null
    private var isLastPage = false
    private var currentQuery = ""

    fun loadCategory(category: ArchiveCategory) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                if (category.parent == "latest") {
                    if (category.name == "latest") {
                        val items = repository.getLatestUploads()
                        _selectedCategoryItems.value = items
                        if (_displayState.value == DisplayState.CATEGORY) {
                            _currentItems.value = items
                        }
                    } else {
                        loadLatestCategoryItems(category)
                    }
                } else {
                    loadNormalCategoryItems(category)
                }
            } catch (e: Exception) {
                _error.value = "Failed to load category items: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadLatestCategoryItems(category: ArchiveCategory) {
        val latestItems = repository.getLatestUploads()
        val filteredItems = when {
            category.name.contains("/") -> {
                val (main, sub) = category.name.split("/")
                latestItems.filter { it.mainCategory == main && it.subCategory == sub }
            }
            else -> {
                latestItems.filter { it.mainCategory == category.name }
            }
        }

        _selectedCategoryItems.value = filteredItems
        lastSelectedCategory = category.copy(items = filteredItems)

        if (_displayState.value == DisplayState.CATEGORY) {
            _currentItems.value = filteredItems
        }
    }

    private suspend fun loadNormalCategoryItems(category: ArchiveCategory) {
        currentCategoryPage = 1
        isCategoryLastPage = false
        currentCategoryName = category.name

        val items = repository.getCategoryItems(category.name, currentCategoryPage)
        val updatedCategory = category.copy(items = items)

        _selectedCategoryItems.value = items
        lastSelectedCategory = updatedCategory

        if (_displayState.value == DisplayState.CATEGORY) {
            _currentItems.value = items
        }
    }
    fun loadNextCategoryPage() {
        if (!isCategoryLastPage && !_isLoading.value && currentCategoryName != null) {
            viewModelScope.launch {
                try {
                    _isLoading.value = true
                    currentCategoryPage++

                    val newItems = repository.getCategoryItems(currentCategoryName!!, currentCategoryPage)

                    if (newItems.isEmpty()) {
                        isCategoryLastPage = true
                    } else {
                        _selectedCategoryItems.value = _selectedCategoryItems.value + newItems
                        if (_displayState.value == DisplayState.CATEGORY) {
                            _currentItems.value = _selectedCategoryItems.value
                        }
                    }
                } catch (e: Exception) {
                    _error.value = "Failed to load more items: ${e.message}"
                    currentCategoryPage--
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }

    fun getLastSelectedCategory(): ArchiveCategory? = lastSelectedCategory

    fun setDisplayState(newState: DisplayState) {
        viewModelScope.launch {
            _displayState.value = newState
            when (newState) {
                DisplayState.HOME -> {
                    _currentItems.value = _categories.value
                }
                DisplayState.FAVORITES -> {
                    loadFavorites()
                    _currentItems.value = _favoriteItems.value
                }
                DisplayState.CATEGORY -> {
                    _currentItems.value = _selectedCategoryItems.value
                }
                DisplayState.SEARCH -> {
                    _currentItems.value = _searchResults.value
                }
            }
        }
    }

    fun toggleFavorite(item: ArchiveItem) {
        if (favoriteManager.isFavorite(item.identifier)) {
            favoriteManager.removeFavorite(item)
        } else {
            favoriteManager.addFavorite(item)
        }
        loadFavorites()
    }

    fun loadFavorites() {
        val favorites = favoriteManager.getFavorites().toList()
        _favoriteItems.value = favorites
        if (_displayState.value == DisplayState.FAVORITES) {
            _currentItems.value = favorites
        }
    }

    fun isFavorite(identifier: String): Boolean {
        return favoriteManager.isFavorite(identifier)
    }

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

                if (_displayState.value == DisplayState.SEARCH) {
                    _currentItems.value = _searchResults.value
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
                if (_displayState.value == DisplayState.HOME) {
                    _currentItems.value = _categories.value
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error occurred"
                _categories.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchHomeCategories() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val latestCategory = ArchiveCategory(
                    name = "latest",
                    items = emptyList(),
                    subCategories = emptyList(),
                    parent = null
                )

                val hotCategory = ArchiveCategory(
                    name = "hot",
                    items = emptyList(),
                    subCategories = listOf(
                        ArchiveCategory(name = "day", displayName = "Last 24 Hours", items = emptyList(), parent = "hot"),
                        ArchiveCategory(name = "week", displayName = "Last Week", items = emptyList(), parent = "hot"),
                        ArchiveCategory(name = "month", displayName = "Last Month", items = emptyList(), parent = "hot"),
                        ArchiveCategory(name = "year", displayName = "Last Year", items = emptyList(), parent = "hot")
                    ),
                    parent = null
                )

                val categoriesCategory = ArchiveCategory(
                    name = "categories",
                    items = emptyList(),
                    subCategories = emptyList(),
                    parent = null
                )

                _categories.value = listOf(latestCategory, hotCategory, categoriesCategory)

                if (_displayState.value == DisplayState.HOME) {
                    _currentItems.value = _categories.value
                }

            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error occurred"
                _categories.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchCategories() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val mainCategories = listOf(
                    ArchiveCategory(name = "texts", items = emptyList(), subCategories = emptyList(), parent = null),
                    ArchiveCategory(name = "movies", items = emptyList(), subCategories = emptyList(), parent = null),
                    ArchiveCategory(name = "audio", items = emptyList(), subCategories = emptyList(), parent = null),
                    ArchiveCategory(name = "software", items = emptyList(), subCategories = emptyList(), parent = null),
                    ArchiveCategory(name = "image", items = emptyList(), subCategories = emptyList(), parent = null),
                    ArchiveCategory(name = "web", items = emptyList(), subCategories = emptyList(), parent = null),
                    ArchiveCategory(name = "data", items = emptyList(), subCategories = emptyList(), parent = null),
                    ArchiveCategory(name = "education", items = emptyList(), subCategories = emptyList(), parent = null),
                    ArchiveCategory(name = "collection", items = emptyList(), subCategories = emptyList(), parent = null),
                    ArchiveCategory(name = "journals", items = emptyList(), subCategories = emptyList(), parent = null),
                    ArchiveCategory(name = "etree", items = emptyList(), subCategories = emptyList(), parent = null),
                    ArchiveCategory(name = "prelinger", items = emptyList(), subCategories = emptyList(), parent = null),
                    ArchiveCategory(name = "podcasts", items = emptyList(), subCategories = emptyList(), parent = null),
                    ArchiveCategory(name = "radio", items = emptyList(), subCategories = emptyList(), parent = null),
                    ArchiveCategory(name = "additional_collections", items = emptyList(), subCategories = emptyList(), parent = null)
                )


                if (_displayState.value == DisplayState.HOME) {
                    _currentItems.value = _categories.value
                }

            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error occurred"
                _categories.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun selectCategory(category: ArchiveCategory) {
        lastSelectedCategory = category
        when (category.name) {
            "latest" -> {
                viewModelScope.launch {
                    try {
                        _isLoading.value = true
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

                        val updatedCategory = category.copy(
                            items = items,
                            subCategories = mainCategories
                        )

                        _selectedCategoryItems.value = items
                        if (_displayState.value == DisplayState.CATEGORY) {
                            _currentItems.value = updatedCategory.subCategories
                        }
                    } finally {
                        _isLoading.value = false
                    }
                }
            }
            "categories" -> {
                viewModelScope.launch {
                    try {
                        _isLoading.value = true

                        val mainCategories = listOf(
                            ArchiveCategory(name = "texts", items = emptyList(), subCategories = emptyList(), parent = null),
                            ArchiveCategory(name = "movies", items = emptyList(), subCategories = emptyList(), parent = null),
                            ArchiveCategory(name = "audio", items = emptyList(), subCategories = emptyList(), parent = null),
                            ArchiveCategory(name = "software", items = emptyList(), subCategories = emptyList(), parent = null),
                            ArchiveCategory(name = "image", items = emptyList(), subCategories = emptyList(), parent = null),
                            ArchiveCategory(name = "web", items = emptyList(), subCategories = emptyList(), parent = null),
                            ArchiveCategory(name = "data", items = emptyList(), subCategories = emptyList(), parent = null),
                            ArchiveCategory(name = "education", items = emptyList(), subCategories = emptyList(), parent = null),
                            ArchiveCategory(name = "collection", items = emptyList(), subCategories = emptyList(), parent = null),
                            ArchiveCategory(name = "journals", items = emptyList(), subCategories = emptyList(), parent = null),
                            ArchiveCategory(name = "etree", items = emptyList(), subCategories = emptyList(), parent = null),
                            ArchiveCategory(name = "prelinger", items = emptyList(), subCategories = emptyList(), parent = null),
                            ArchiveCategory(name = "podcasts", items = emptyList(), subCategories = emptyList(), parent = null),
                            ArchiveCategory(name = "radio", items = emptyList(), subCategories = emptyList(), parent = null),
                            ArchiveCategory(name = "additional_collections", items = emptyList(), subCategories = emptyList(), parent = null)
                        )

                        if (_displayState.value == DisplayState.CATEGORY) {
                            _currentItems.value = mainCategories
                        }
                    } finally {
                        _isLoading.value = false
                    }
                }
            }
            "hot" -> {
                handleHotCategory(category)
            }
            else -> {
                if (category.parent == "latest") {
                    loadCategory(category)
                }
                if(category.parent == "hot") {
                    handleHotPeriodCategory(category)
                }
                    else {
                    currentCategoryName = category.name
                    currentCategoryPage = 1
                    isCategoryLastPage = false
                    loadCategory(category)
                }
            }
        }
    }
    private fun handleHotCategory(category: ArchiveCategory) {
        if (_displayState.value == DisplayState.CATEGORY) {
            _currentItems.value = category.subCategories
        }
    }

    private fun handleHotPeriodCategory(category: ArchiveCategory) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val period = when (category.name) {
                    "day" -> HotPeriod.DAY
                    "week" -> HotPeriod.WEEK
                    "month" -> HotPeriod.MONTH
                    "year" -> HotPeriod.YEAR
                    else -> HotPeriod.WEEK
                }
                _hotPeriod.value = period

                val items = repository.getHotItems(period)
                _selectedCategoryItems.value = items

                if (_displayState.value == DisplayState.CATEGORY) {
                    _currentItems.value = items
                }
            } catch (e: Exception) {
                _error.value = "Failed to load hot items: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}