package com.example.archivedownloader

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.archivedownloader.databinding.ActivityMainBinding
import com.example.archivedownloader.repository.ArchiveRepository
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private val adapter = ArchiveAdapter()

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
            adapter = this@MainActivity.adapter
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.items.collect { items ->
                adapter.submitList(items)
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
                    // エラーメッセージを表示する処理を追加
                }
            }
        }
    }
}