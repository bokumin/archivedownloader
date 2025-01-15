package com.example.archivedownloader

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.archivedownloader.databinding.ItemArchiveBinding

class ArchiveAdapter : ListAdapter<ArchiveItem, ArchiveAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemArchiveBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemArchiveBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ArchiveItem) {
            binding.titleTextView.text = item.title
            binding.categoryTextView.text = item.category

            Glide.with(binding.root.context)
                .load(item.thumbnailUrl)
                .centerCrop()
                .into(binding.thumbnailImageView)
            itemView.setOnClickListener {
                val intent = Intent(itemView.context, DetailActivity::class.java).apply {
                    putExtra(DetailActivity.EXTRA_IDENTIFIER, item.identifier)
                }
                itemView.context.startActivity(intent)
            }
        }

    }

    private class DiffCallback : DiffUtil.ItemCallback<ArchiveItem>() {
        override fun areItemsTheSame(oldItem: ArchiveItem, newItem: ArchiveItem): Boolean {
            return oldItem.identifier == newItem.identifier
        }

        override fun areContentsTheSame(oldItem: ArchiveItem, newItem: ArchiveItem): Boolean {
            return oldItem == newItem
        }
    }
}