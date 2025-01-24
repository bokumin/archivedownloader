package net.bokumin45.archivedownloader

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.R as MaterialR

class CategoryAdapter(
    private val onCategoryClick: (ArchiveCategory) -> Unit
) : ListAdapter<CategoryAdapter.CategoryListItem, CategoryAdapter.ViewHolder>(CategoryDiffCallback()) {

    private var isLatestSelected: Boolean = false
    fun updateLatestState(isLatest: Boolean) {
        isLatestSelected = isLatest
        notifyDataSetChanged()
    }
    sealed class CategoryListItem {
        data class CategoryItem(
            val category: ArchiveCategory,
            val level: Int = 0
        ) : CategoryListItem()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val cardView = MaterialCardView(parent.context).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                val margin = (8 * context.resources.displayMetrics.density).toInt()
                setMargins(margin, margin, margin, margin)
            }
            radius = (8 * context.resources.displayMetrics.density)
            cardElevation = (2 * context.resources.displayMetrics.density)
            useCompatPadding = true

            strokeWidth = (1 * context.resources.displayMetrics.density).toInt()
            val typedValue = android.util.TypedValue()
            context.theme.resolveAttribute(MaterialR.attr.colorPrimary, typedValue, true)
            strokeColor = typedValue.data
        }

        val textView = TextView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            val padding = (16 * context.resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
            textSize = 20f
        }

        cardView.addView(textView)

        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun submitCategoryList(categories: List<ArchiveCategory>) {
        val processedCategories = categories.map { category ->
            if (category.name == "hot") {
                category.copy(subCategories = emptyList())
            } else {
                category
            }
        }
        val flattenedList = flattenCategories(processedCategories)
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

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView = itemView as MaterialCardView
        private val textView = cardView.getChildAt(0) as TextView

        fun bind(item: CategoryListItem) {
            when (item) {
                is CategoryListItem.CategoryItem -> {
                    val category = item.category
                    val indent = "  ".repeat(item.level)

                    val displayName = buildString {
                        append(indent)
                        append(category.displayName)
                        if (isLatestSelected) {
                            append(" (${category.totalItemCount})")
                        }
                    }

                    textView.text = displayName

                    val indentMargin = (16 * item.level * cardView.resources.displayMetrics.density).toInt()
                    if (cardView.layoutParams is ViewGroup.MarginLayoutParams) {
                        val params = cardView.layoutParams as ViewGroup.MarginLayoutParams
                        val baseMargin = (8 * cardView.resources.displayMetrics.density).toInt()
                        params.leftMargin = baseMargin + indentMargin
                        params.topMargin = baseMargin
                        params.rightMargin = baseMargin
                        params.bottomMargin = baseMargin
                        cardView.layoutParams = params
                    }

                    cardView.apply {
                        setCardBackgroundColor(context.getColor(android.R.color.white))
                        cardElevation = (2 * resources.displayMetrics.density)
                        radius = (8 * resources.displayMetrics.density)

                        isClickable = true
                        isFocusable = true

                        setOnClickListener {
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