package net.bokumin45.archivedownloader

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

    private val categoryAdapter = CategoryAdapter { category ->
        showCategoryItems(category)
    }
    private val itemAdapter = ArchiveAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRetrofitAndViewModel()
        setupRecyclerView()
        observeViewModel()
        viewModel.fetchLatestUploads()
    }

    private fun setupRetrofitAndViewModel() {
        val retrofit = Retrofit.Builder()
            .baseUrl(ArchiveService.BASE_URL)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create()) // JSONパース用に追加
            .build()

        val archiveService = retrofit.create(ArchiveService::class.java)
        val repository = ArchiveRepository(archiveService)
        val viewModelFactory = MainViewModelFactory(repository)
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
                categoryAdapter.submitCategoryList(categories)  // submitList()から変更
            }
        }

        lifecycleScope.launch {
            viewModel.selectedCategoryItems.collect { items ->
                itemAdapter.submitList(items)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                binding.recyclerView.adapter = categoryAdapter
                supportActionBar?.apply {
                    title = getString(R.string.app_name)
                    setDisplayHomeAsUpEnabled(false)
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