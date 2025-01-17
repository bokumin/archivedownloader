package net.bokumin45.archivedownloader

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

    private val categoryAdapter = CategoryAdapter { category ->
        showCategoryItems(category)
    }

    private val itemAdapter = ArchiveAdapter(
        onFavoriteClick = { item -> viewModel.toggleFavorite(item) },
        isFavorite = { identifier -> viewModel.isFavorite(identifier) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        setupDrawer()
        setupRetrofitAndViewModel()
        setupRecyclerView()
        observeViewModel()
        viewModel.fetchLatestUploads()
    }

    private fun setupDrawer() {
        drawerLayout = binding.drawerLayout
        navigationView = binding.navigationView

        val toggle = ActionBarDrawerToggle(
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

        lifecycleScope.launch {
            viewModel.searchResults.collect { results ->
                if (isSearchActive) {
                    itemAdapter.submitList(results)
                    binding.recyclerView.adapter = itemAdapter
                }
            }
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = categoryAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (isSearchActive) {
                        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                        val visibleItemCount = layoutManager.childCount
                        val totalItemCount = layoutManager.itemCount
                        val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0) {
                            viewModel.loadNextSearchPage()
                        }
                    }
                }
            })
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.categories.collect { categories ->
                categoryAdapter.submitCategoryList(categories)
            }
        }

        lifecycleScope.launch {
            viewModel.selectedCategoryItems.collect { items ->
                itemAdapter.submitList(items)
            }
        }

        lifecycleScope.launch {
            viewModel.favoriteItems.collect { items ->
                if (!isSearchActive) {
                    itemAdapter.submitList(items)
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
                    // エラー処理をここに実装
                }
            }
        }
    }

    private fun showCategoryItems(category: ArchiveCategory) {
        binding.recyclerView.adapter = itemAdapter
        viewModel.selectCategory(category)
        supportActionBar?.apply {
            title = category.name
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun showFavorites() {
        binding.recyclerView.adapter = itemAdapter
        viewModel.loadFavorites()
        supportActionBar?.apply {
            title = "Favorites"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun showHome() {
        binding.recyclerView.adapter = categoryAdapter
        viewModel.fetchLatestUploads()
        supportActionBar?.apply {
            title = getString(R.string.app_name)
            setDisplayHomeAsUpEnabled(false)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                if (drawerLayout.isDrawerOpen(navigationView)) {
                    drawerLayout.closeDrawer(navigationView)
                } else {
                    showHome()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)

        val searchItem = menu.findItem(R.id.action_search)
        searchView = searchItem.actionView as SearchView

        setupSearchView()
        return true
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    isSearchActive = true
                    viewModel.search(it)
                    searchView.clearFocus()
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    isSearchActive = false
                    binding.recyclerView.adapter = categoryAdapter
                }
                return true
            }
        })

        searchView.setOnCloseListener {
            isSearchActive = false
            binding.recyclerView.adapter = categoryAdapter
            false
        }
    }
}