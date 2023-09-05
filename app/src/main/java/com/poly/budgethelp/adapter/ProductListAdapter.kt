package com.poly.budgethelp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.poly.budgethelp.ProductListActivity
import com.poly.budgethelp.R
import com.poly.budgethelp.ReceiptInfoActivity
import com.poly.budgethelp.config.UserConfig
import com.poly.budgethelp.data.Product
import com.poly.budgethelp.utility.ActivityUtils

class ProductListAdapter : ListAdapter<Product, ProductListAdapter.ProductListViewHolder>(ReceiptsComparator()) {

    lateinit var baseContext: ProductListActivity

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductListViewHolder {
        return ProductListViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: ProductListViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current, baseContext)
    }

    class ProductListViewHolder (itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(product: Product, baseContext: ProductListActivity) {
            val nameTextView: TextView = itemView.findViewById(R.id.productNameTextView)
            val categoryTextView: TextView = itemView.findViewById(R.id.productCategoryTextView)
            val priceTextView: TextView = itemView.findViewById(R.id.productPriceTextView)
            val editButton: View = itemView.findViewById(R.id.productEditButton)
            val deleteButton: View = itemView.findViewById(R.id.productDeleteButton)

            if (ActivityUtils.isUsingNightModeResources(baseContext)) {
                editButton.setBackgroundColor(android.graphics.Color.WHITE)
                deleteButton.setBackgroundColor(android.graphics.Color.WHITE)
            }

            nameTextView.text = product.productName
            categoryTextView.text = product.productCategory
            priceTextView.text = String.format("%.2f %s", product.productPrice, UserConfig.currency)

            deleteButton.setOnClickListener { _ ->
                baseContext.requestDeleteProduct(product)
            }

            editButton.setOnClickListener { view ->
                val popupData = ActivityUtils.createPopup(R.layout.popup_add_item, baseContext)
                popupData.second.isFocusable = true
                baseContext.activePopup = popupData.second

                val nameEdit: EditText = popupData.first.findViewById(R.id.addProductNameEditText)
                val categorySpinner: Spinner = popupData.first.findViewById(R.id.addProductCategorySpinner)
                val priceEdit: EditText = popupData.first.findViewById(R.id.addProductPriceEditText)
                val confirmButton: Button = popupData.first.findViewById(R.id.addProductConfirmButton)

                if (ActivityUtils.isUsingNightModeResources(baseContext)) {
                    nameEdit.setTextColor(android.graphics.Color.BLACK)
                    priceEdit.setTextColor(android.graphics.Color.BLACK)
                }

                nameEdit.setText(product.productName)
                priceEdit.setText(product.productPrice.toString())

                baseContext.categoryAdapter.setDropDownViewResource(R.layout.spinner_item)
                categorySpinner.adapter = baseContext.categoryAdapter
                categorySpinner.setSelection(baseContext.categoryAdapter.getPosition(product.productCategory))

                popupData.second.update()

                confirmButton.setOnClickListener {_ ->
                    if (nameEdit.text.isBlank()) {
                        Toast.makeText(baseContext,
                            baseContext.resources.getString(R.string.error_empty_product),
                            Toast.LENGTH_SHORT).show()
                    }
                    else if (priceEdit.text.toString().toFloatOrNull() == null) {
                        Toast.makeText(baseContext,
                            baseContext.resources.getString(R.string.error_no_price),
                            Toast.LENGTH_SHORT).show()
                    }

                    else if (categorySpinner.selectedItemPosition < 0
                        || baseContext.categoryAdapter.getItem(categorySpinner.selectedItemPosition) == null) {
                        Toast.makeText(baseContext,
                            baseContext.resources.getString(R.string.error_invalid_category),
                            Toast.LENGTH_SHORT).show()
                    }

                    // Valid product data
                    else {
                        val id = product.productId
                        val newName = nameEdit.text.toString()
                        val category = baseContext.categoryAdapter.getItem(categorySpinner.selectedItemPosition)
                        val price = priceEdit.text.toString().toFloat()

                        val newProduct = Product(id, newName, category!!, price)
                        baseContext.saveProduct(newProduct)

                        popupData.second.dismiss()
                        baseContext.activePopup = null
                    }
                }

                popupData.second.setOnDismissListener { baseContext.activePopup = null }
            }
        }

        companion object {
            fun create(parent: ViewGroup): ProductListViewHolder {
                val view: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.product_list_item_with_delete, parent, false)
                return ProductListViewHolder(view)
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