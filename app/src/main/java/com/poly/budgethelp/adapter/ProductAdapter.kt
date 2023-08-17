package com.poly.budgethelp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.poly.budgethelp.R
import com.poly.budgethelp.ReceiptInfoActivity
import com.poly.budgethelp.data.Product

class ProductAdapter : ListAdapter<Product, ProductAdapter.ProductViewHolder>(ReceiptsComparator()) {

    lateinit var context: ReceiptInfoActivity

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        return ProductViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current, context)
    }

    class ProductViewHolder (itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val productNameView: TextView = itemView.findViewById(R.id.productNameTextView)
        private val productCategoryView: TextView = itemView.findViewById(R.id.productCategoryTextView)
        private val productPriceView: TextView = itemView.findViewById(R.id.productPriceTextView)
        fun bind(product: Product, context: ReceiptInfoActivity) {
            productNameView.text = product.productName
            productCategoryView.text = product.productCategory
            productPriceView.text = String.format("%.2f €", product.productPrice)
        }

        companion object {
            fun create(parent: ViewGroup): ProductViewHolder {
                val view: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_receipt_product, parent, false)
                return ProductViewHolder(view)
            }
        }
    }

    class ReceiptsComparator : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.productId == newItem.productId
        }
    }
}