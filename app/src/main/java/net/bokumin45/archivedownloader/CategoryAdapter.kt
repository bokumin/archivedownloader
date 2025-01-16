package net.bokumin45.archivedownloader

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class CategoryAdapter(
    private val onCategoryClick: (ArchiveCategory) -> Unit
) : ListAdapter<ArchiveCategory, CategoryAdapter.ViewHolder>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = getItem(position)
        holder.bind(category)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val textView: TextView = view.findViewById(android.R.id.text1)

        fun bind(category: ArchiveCategory) {
            val displayName = if (category.name == "latest") {
                "latest (${category.items.size})"
            } else {
                "${category.displayName} (${category.items.size})"
            }
            textView.text = displayName
            itemView.setOnClickListener { onCategoryClick(category) }
        }
    }

    private class CategoryDiffCallback : DiffUtil.ItemCallback<ArchiveCategory>() {
        override fun areItemsTheSame(oldItem: ArchiveCategory, newItem: ArchiveCategory) =
            oldItem.name == newItem.name
        override fun areContentsTheSame(oldItem: ArchiveCategory, newItem: ArchiveCategory) =
            oldItem == newItem
    }
}