package net.bokumin45.archivedownloader

import ArchiveFileAdapter
import android.app.DownloadManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import net.bokumin45.archivedownloader.databinding.ActivityDetailBinding
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class DetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailBinding
    private lateinit var archiveService: ArchiveService
    private lateinit var fileAdapter: ArchiveFileAdapter
    private var identifier: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)


        identifier = intent.getStringExtra(EXTRA_IDENTIFIER) ?: return
        val thumbnailUrl = ArchiveService.getThumbnailUrl(identifier)

        val thumbnailImageView: ImageView = findViewById(R.id.thumbnailImageView)
        Glide.with(this)
            .load(thumbnailUrl)
            .into(thumbnailImageView)

        setupRetrofit()
        setupRecyclerView()
        loadMetadata()

    }

    private fun setupRetrofit() {
        val retrofit = Retrofit.Builder()
            .baseUrl(ArchiveService.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        archiveService = retrofit.create(ArchiveService::class.java)
    }

    private fun setupRecyclerView() {
        fileAdapter = ArchiveFileAdapter { file ->
            showDownloadConfirmationDialog(file)
        }

        binding.fileList.apply {
            layoutManager = GridLayoutManager(this@DetailActivity, 2)
            adapter = fileAdapter
        }
    }

    private fun showDownloadConfirmationDialog(file: ArchiveFile) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.download_confirmation_title))
            .setMessage(getString(R.string.download_confirmation_message, file.name))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                downloadFile(file)
            }
            .setNegativeButton(getString(R.string.no)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun loadMetadata() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                val response = archiveService.getMetadata(identifier)
                fileAdapter.submitList(response.files)

            } catch (e: Exception) {
                Toast.makeText(this@DetailActivity, "Error loading metadata: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun downloadFile(file: ArchiveFile) {
        val downloadUrl = "${ArchiveService.BASE_URL}download/$identifier/${Uri.encode(file.name)}"
        val request = DownloadManager.Request(Uri.parse(downloadUrl))
            .setTitle(file.name)
            .setDescription("Downloading from Internet Archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, file.name)
            .addRequestHeader("User-Agent", "Mozilla/5.0")

        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
        Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_IDENTIFIER = "extra_identifier"
    }
}