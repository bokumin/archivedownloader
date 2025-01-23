package net.bokumin45.archivedownloader

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.navigation.NavigationView
import net.bokumin45.archivedownloader.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import net.bokumin45.archivedownloader.repository.ArchiveRepository
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var searchView: SearchView
    private var isSearchActive = false
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toggle: ActionBarDrawerToggle

    @RequiresApi(Build.VERSION_CODES.O)
    private val categoryAdapter = CategoryAdapter { category ->
        when {
            category.name == "latest" -> {
                viewModel.fetchLatestUploads()
                supportActionBar?.title = "Latest Uploads"
            }
            category.parent == "latest" -> {
                viewModel.loadCategory(category)
                supportActionBar?.title = category.name
            }
            category.subCategories.isNotEmpty() -> {
                viewModel.setDisplayState(DisplayState.CATEGORY)
                viewModel.selectCategory(category)
                supportActionBar?.title = category.name
            }
            else -> {
                viewModel.loadCategory(category)
                showCategoryItems(category)
            }
        }
    }
    private val itemAdapter = ArchiveAdapter(
        onFavoriteClick = { item ->
            viewModel.toggleFavorite(item)
        },
        isFavorite = { identifier -> viewModel.isFavorite(identifier) }
    )

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        setupDrawer()
        setupRetrofitAndViewModel()
        setupRecyclerView()
        observeViewModel()
        setupSearchFab()

        showHome()
    }
    private fun setupSearchFab() {
        binding.searchFab.setOnClickListener {
            showSearchDialog()
        }
    }

    private fun showSearchDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_search, null)
        val editText = view.findViewById<EditText>(R.id.searchEditText)
        val chipGroup = view.findViewById<ChipGroup>(R.id.categoryChipGroup)

        lateinit var dialog: AlertDialog

        val categoriesFirstRow = listOf(
            "Texts" to "texts",
            "Movies" to "movies",
            "Audio" to "audio",
            "Software" to "software",
            "Image" to "image",
            "Web" to "web",
            "Data" to "data"
        )

        val categoriesSecondRow = listOf(
            "Education" to "education",
            "Collection" to "collection",
            "Journals" to "journals",
            "Etree" to "etree",
            "Prelinger" to "prelinger",
            "Podcasts" to "podcasts",
            "Radio" to "radio",
            "Additional Collections" to "additional_collections"
        )

        categoriesFirstRow.forEach { (displayName, value) ->
            val chip = Chip(this).apply {
                text = displayName
                isCheckable = true
                tag = value
            }
            chipGroup.addView(chip)
        }

        categoriesSecondRow.forEach { (displayName, value) ->
            val chip = Chip(this).apply {
                text = displayName
                isCheckable = true
                tag = value
            }
            chipGroup.addView(chip)
        }

        val executeSearch = {
            val query = editText.text.toString()
            if (query.isNotBlank()) {
                val selectedCategories = chipGroup.children
                    .filterIsInstance<Chip>()
                    .filter { it.isChecked }
                    .map { it.tag as String }
                    .toList()

                viewModel.setDisplayState(DisplayState.SEARCH)
                viewModel.search(query, selectedCategories)
                dialog.dismiss()
            }
        }

        dialog = AlertDialog.Builder(this)
            .setTitle("Search")
            .setView(view)
            .setPositiveButton("Search") { _, _ ->
                executeSearch()
            }
            .setNegativeButton("Cancel", null)
            .create()

        editText.setOnEditorActionListener { _, actionId, event ->
            when {
                actionId == EditorInfo.IME_ACTION_SEARCH -> {
                    executeSearch()
                    true
                }
                event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP -> {
                    executeSearch()
                    true
                }
                else -> false
            }
        }

        dialog.show()

        editText.requestFocus()
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }
    companion object {
        private const val VPN_REQUEST_CODE = 1
    }

    private fun showConfirmationDialog(url: String) {
        AlertDialog.Builder(this)
            .setTitle("Confirm")
            .setMessage("Would you like to open this page in your browser?")
            .setPositiveButton("OK") { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun setupDrawer() {
        drawerLayout = binding.drawerLayout
        navigationView = binding.navigationView

        toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_favorites -> {
                    showFavorites()
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_home -> {
                    showHome()
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_donate_archive -> {
                    showConfirmationDialog("https://archive.org/donate/")
                    true
                }
                R.id.nav_donate_website -> {
                    showConfirmationDialog("https://bokumin45.server-on.net")
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRetrofitAndViewModel() {
        val retrofit = Retrofit.Builder()
            .baseUrl(ArchiveService.BASE_URL)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val archiveService = retrofit.create(ArchiveService::class.java)
        val repository = ArchiveRepository(archiveService)
        val favoriteManager = FavoriteManager(this)
        val viewModelFactory = MainViewModelFactory(repository, favoriteManager)
        viewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                        && firstVisibleItemPosition >= 0
                    ) {
                        when (viewModel.displayState.value) {
                            DisplayState.SEARCH -> viewModel.loadNextSearchPage()
                            DisplayState.CATEGORY -> viewModel.loadNextCategoryPage()
                            else -> {}
                        }
                    }
                }
            })
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.currentItems.collect { items ->
                when (items.firstOrNull()) {
                    is ArchiveCategory -> {
                        binding.recyclerView.adapter = categoryAdapter
                        categoryAdapter.submitCategoryList(items as List<ArchiveCategory>)
                    }
                    is ArchiveItem -> {
                        binding.recyclerView.adapter = itemAdapter
                        itemAdapter.submitList(items as List<ArchiveItem>)
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let {
                    Toast.makeText(this@MainActivity, it, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showCategoryItems(category: ArchiveCategory) {
        viewModel.setDisplayState(DisplayState.CATEGORY)
        viewModel.selectCategory(category)
        supportActionBar?.title = category.name
    }

    private fun showFavorites() {
        viewModel.setDisplayState(DisplayState.FAVORITES)
        supportActionBar?.title = "Favorites"
    }

    private fun showHome() {
        viewModel.setDisplayState(DisplayState.HOME)
        viewModel.fetchHomeCategories()
        supportActionBar?.title = getString(R.string.app_name)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    viewModel.setDisplayState(DisplayState.SEARCH)
                    isSearchActive = true
                    viewModel.search(it)
                    searchView.clearFocus()
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank() && isSearchActive) {
                    isSearchActive = false
                    showHome()
                }
                return true
            }
        })

        searchView.setOnCloseListener {
            if (isSearchActive) {
                isSearchActive = false
                showHome()
            }
            false
        }
    }
}