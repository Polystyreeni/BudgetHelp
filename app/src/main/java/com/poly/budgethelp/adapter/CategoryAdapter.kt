package com.poly.budgethelp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.ui.graphics.Color
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.poly.budgethelp.CategoryActivity
import com.poly.budgethelp.R
import com.poly.budgethelp.data.Category
import com.poly.budgethelp.utility.ActivityUtils

class CategoryAdapter : ListAdapter<Category, CategoryAdapter.CategoryViewHolder>(ReceiptsComparator()) {

    lateinit var baseContext: CategoryActivity

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        return CategoryViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current, baseContext, position)
    }

    class CategoryViewHolder (itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(category: Category, baseContext: CategoryActivity, position: Int) {
            val categoryNameView: TextView = itemView.findViewById(R.id.categoryNameTextView)
            val editButton: View = itemView.findViewById(R.id.categoryEditButton)
            val deleteButton: View = itemView.findViewById(R.id.categoryDeleteButton)

            if (ActivityUtils.isUsingNightModeResources(baseContext)) {
                editButton.setBackgroundColor(android.graphics.Color.WHITE)
                deleteButton.setBackgroundColor(android.graphics.Color.WHITE)
            }

            categoryNameView.text = category.categoryName
            editButton.setOnClickListener { view ->
                baseContext.createEditCategoryPopup(category, position)
            }
            deleteButton.setOnClickListener { view ->
                baseContext.requestCategoryDelete(category, position)
            }
        }

        companion object {
            fun create(parent: ViewGroup): CategoryViewHolder {
                val view: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_category_with_delete, parent, false)
                return CategoryViewHolder(view)
            }
        }
    }

    class ReceiptsComparator : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem.categoryName == newItem.categoryName
        }
    }
}