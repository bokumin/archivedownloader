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
) : ListAdapter<CategoryAdapter.CategoryListItem, CategoryAdapter.ViewHolder>(CategoryDiffCallback()) {

    sealed class CategoryListItem {
        data class CategoryItem(
            val category: ArchiveCategory,
            val level: Int = 0
        ) : CategoryListItem()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun submitCategoryList(categories: List<ArchiveCategory>) {
        val flattenedList = flattenCategories(categories)
        submitList(flattenedList)
    }

    private fun flattenCategories(
        categories: List<ArchiveCategory>,
        level: Int = 0
    ): List<CategoryListItem> {
        return categories.flatMap { category ->
            listOf(CategoryListItem.CategoryItem(category, level)) +
                    flattenCategories(category.subCategories, level + 1)
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val textView: TextView = view.findViewById(android.R.id.text1)

        fun bind(item: CategoryListItem) {
            when (item) {
                is CategoryListItem.CategoryItem -> {
                    val category = item.category
                    val indent = "  ".repeat(item.level)
                    val displayName = buildString {
                        append(indent)
                        append(category.displayName)
                        append(" (${category.totalItemCount})")
                    }
                    textView.text = displayName
                    // カテゴリーに子カテゴリーがある場合は、子カテゴリーを含むアイテムすべてを表示
                    itemView.setOnClickListener {
                        if (category.subCategories.isNotEmpty()) {
                            // サブカテゴリーを含むすべてのアイテムを表示
                            val allItems = category.items +
                                    category.subCategories.flatMap { it.items }
                            onCategoryClick(category.copy(items = allItems))
                        } else {
                            onCategoryClick(category)
                        }
                    }
                }
            }
        }
    }

    private class CategoryDiffCallback : DiffUtil.ItemCallback<CategoryListItem>() {
        override fun areItemsTheSame(oldItem: CategoryListItem, newItem: CategoryListItem): Boolean {
            return when {
                oldItem is CategoryListItem.CategoryItem && newItem is CategoryListItem.CategoryItem ->
                    oldItem.category.name == newItem.category.name &&
                            oldItem.level == newItem.level
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: CategoryListItem, newItem: CategoryListItem): Boolean {
            return oldItem == newItem
        }
    }
}