package net.bokumin45.archivedownloader

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import net.bokumin45.archivedownloader.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import net.bokumin45.archivedownloader.repository.ArchiveRepository
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
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
            .build()

        val archiveService = retrofit.create(ArchiveService::class.java)
        val repository = ArchiveRepository(archiveService)
        val viewModelFactory = MainViewModelFactory(repository)
        viewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = categoryAdapter
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
}