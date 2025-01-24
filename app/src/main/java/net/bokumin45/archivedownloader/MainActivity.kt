package net.bokumin45.archivedownloader

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.children
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.leinardi.android.speeddial.SpeedDialActionItem
import net.bokumin45.archivedownloader.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private var isSearchActive = false

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

        setupRetrofitAndViewModel()
        setupRecyclerView()
        setupSpeedDial()
        setupFabs()
        observeViewModel()

        showHome()
    }

    private fun setupFabs() {
        binding.homeFab.setOnClickListener {
            showHome()
        }

        binding.favoriteFab.setOnClickListener {
            showFavorites()
        }

        binding.searchFab.setOnClickListener {
            showSearchDialog()
        }
    }

    private fun setupSpeedDial() {
        val speedDial = binding.speedDial

        speedDial.addActionItem(
            SpeedDialActionItem.Builder(R.id.fab_donate, R.drawable.ic_donate)
                .setLabel(getString(R.string.donate))
                .setFabBackgroundColor(ResourcesCompat.getColor(resources, R.color.teal_700, theme))
                .setFabImageTintColor(Color.WHITE)
                .create()
        )

        speedDial.addActionItem(
            SpeedDialActionItem.Builder(R.id.fab_info, R.drawable.ic_info)
                .setLabel(getString(R.string.info))
                .setFabBackgroundColor(ResourcesCompat.getColor(resources, R.color.teal_700, theme))
                .setFabImageTintColor(Color.WHITE)
                .create()
        )

        speedDial.setOnActionSelectedListener { actionItem ->
            when (actionItem.id) {
                R.id.fab_donate -> {
                    showDonateDialog()
                    speedDial.close()
                    true
                }

                R.id.fab_info -> {
                    showInfoDialog()
                    speedDial.close()
                    true
                }

                else -> false
            }
        }
    }

    private fun showInfoDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.app_info))
            .setMessage(getString(R.string.app_description))
            .setPositiveButton(android.R.string.ok, null)
            .show()
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

    private fun showDonateDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_donate, null)

        val archiveImage = view.findViewById<ImageView>(R.id.archiveImage)
        val websiteImage = view.findViewById<ImageView>(R.id.websiteImage)
        val archiveButton = view.findViewById<LinearLayout>(R.id.archiveButton)
        val websiteButton = view.findViewById<LinearLayout>(R.id.websiteButton)

        Glide.with(this)
            .load(R.drawable.archive_thumbnail)
            .centerCrop()
            .into(archiveImage)

        Glide.with(this)
            .load(R.drawable.website_thumbnail)
            .centerCrop()
            .into(websiteImage)

        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.donate))
            .setView(view)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        archiveButton.setOnClickListener {
            showConfirmationDialog("https://archive.org/donate/")
            dialog.dismiss()
        }

        websiteButton.setOnClickListener {
            showConfirmationDialog("https://bokumin45.server-on.net")
            dialog.dismiss()
        }

        dialog.show()
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
    }

    private fun showFavorites() {
        viewModel.setDisplayState(DisplayState.FAVORITES)
    }

    private fun showHome() {
        viewModel.setDisplayState(DisplayState.HOME)
        viewModel.fetchHomeCategories()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBackPressed() {
        when {
            binding.speedDial.isOpen -> {
                binding.speedDial.close()
            }

            viewModel.getLastSelectedCategory()?.parent == "latest" -> {
                viewModel.selectCategory(
                    ArchiveCategory(
                        name = "latest",
                        displayName = "Latest Updates",
                        items = emptyList(),
                        subCategories = emptyList()
                    )
                )
            }

            viewModel.getLastSelectedCategory()?.name == "latest" &&
                    viewModel.displayState.value == DisplayState.CATEGORY -> {
                showHome()
            }

            viewModel.displayState.value == DisplayState.CATEGORY -> {
                viewModel.getLastSelectedCategory()?.parent?.let { parentName ->
                    val parentCategory = viewModel.categories.value.find { it.name == parentName }
                    parentCategory?.let {
                        viewModel.selectCategory(it)
                        return
                    }
                }
                showHome()
            }

            viewModel.displayState.value != DisplayState.HOME -> {
                showHome()
            }

            viewModel.displayState.value == DisplayState.HOME &&
                    viewModel.getLastSelectedCategory()?.name == "latest" -> {
                showHome()
            }
            else -> {
                super.onBackPressed()
            }
        }
    }
}